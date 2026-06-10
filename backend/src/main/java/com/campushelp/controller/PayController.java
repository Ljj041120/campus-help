package com.campushelp.controller;

import com.campushelp.common.Result;
import com.campushelp.service.AlipayService;
import com.campushelp.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.util.*;

/**
 * 支付宝支付控制器
 * 
 * 接口说明：
 * 1. POST /api/pay/create — 创建支付订单，返回 OrderString
 * 2. POST /api/pay/alipay/notify — 支付宝异步回调（无需JWT）
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final AlipayService alipayService;
    private final OrderService orderService;

    /**
     * 创建支付宝支付订单
     * 
     * @param userId 用户ID（从JWT获取）
     * @param orderId 订单ID
     * @return OrderString（前端用此唤起支付宝）
     */
    @PostMapping("/create")
    public Result<Map<String, String>> createPayOrder(
            @RequestAttribute Long userId,
            @RequestParam Long orderId) {

        // 获取订单信息
        var detail = orderService.getOrderDetail(orderId);
        if (detail == null) {
            return Result.error("订单不存在");
        }
        if (!detail.getPublisherId().equals(userId)) {
            return Result.error("无权操作此订单");
        }

        // 构建商品描述
        String[] typeNames = {"", "代取快递", "代买物品", "代送物品", "其他"};
        String subject = "校园帮-" + (detail.getOrderType() != null && detail.getOrderType() < typeNames.length
                ? typeNames[detail.getOrderType()] : "跑腿");

        // 调用支付宝SDK生成支付参数
        String orderString = alipayService.createPayOrder(orderId, detail.getAmount(), subject);

        return Result.success(Map.of("orderString", orderString));
    }

    /**
     * 支付宝异步通知回调
     * 
     * 支付宝在支付成功后，会 POST 请求此地址
     * 必须返回 "success" 给支付宝，否则支付宝会持续重试
     */
    @PostMapping("/alipay/notify")
    public void alipayNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 获取支付宝回调参数
            Map<String, String> params = getAlipayParams(request);
            log.info("收到支付宝回调: params={}", params);

            // 2. 验签
            if (!alipayService.verifyNotify(params)) {
                response.getWriter().write("failure");
                return;
            }

            // 3. 解析业务参数
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");

            // out_trade_no 格式为 "ORDER_{orderId}"
            if (outTradeNo != null && outTradeNo.startsWith("ORDER_")) {
                Long orderId = Long.valueOf(outTradeNo.substring(6));

                // 幂等处理：已支付的订单不再处理
                var order = orderService.getById(orderId);
                if (order != null && order.getStatus() == 0) { // 待支付
                    orderService.payOrder(orderId, tradeNo);
                    log.info("订单支付成功: orderId={}, tradeNo={}", orderId, tradeNo);
                } else {
                    log.info("订单已处理，忽略重复回调: orderId={}", orderId);
                }
            }

            // 4. 返回 success 给支付宝
            response.getWriter().write("success");

        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            try {
                response.getWriter().write("failure");
            } catch (Exception ignored) {}
        }
    }

    /**
     * 从 HttpServletRequest 提取支付宝回调参数
     */
    private Map<String, String> getAlipayParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}
