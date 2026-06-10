package com.campushelp.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 支付宝沙箱支付服务
 * 
 * 使用步骤：
 * 1. 去 https://open.alipay.com 申请沙箱应用
 * 2. 获取 AppID、应用私钥、支付宝公钥
 * 3. 填入 application.yml 的 alipay 配置段
 * 4. 用沙箱买家账号测试支付
 */
@Slf4j
@Service
public class AlipayService {

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key}")
    private String privateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.gateway-url}")
    private String gatewayUrl;

    @Value("${alipay.notify-url}")
    private String notifyUrl;

    @Value("${alipay.sign-type}")
    private String signType;

    /**
     * 创建支付订单，返回用于唤起支付宝支付的 OrderString
     *
     * @param orderId    订单ID
     * @param amount     支付金额
     * @param subject    商品描述（如"校园帮-代取快递"）
     * @return 支付宝 OrderString（前端用此字符串唤起支付）
     */
    public String createPayOrder(Long orderId, BigDecimal amount, String subject) {
        try {
            // 1. 初始化 AlipayClient
            AlipayClient alipayClient = new DefaultAlipayClient(
                    gatewayUrl, appId, privateKey, "json", "UTF-8",
                    alipayPublicKey, signType);

            // 2. 构造 APP 支付请求
            AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();

            // 异步通知地址（支付宝支付成功后回调此地址）
            request.setNotifyUrl(notifyUrl);

            // 业务参数
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"}",
                    "ORDER_" + orderId,
                    amount.setScale(2, RoundingMode.HALF_UP).toString(),
                    subject
            );
            request.setBizContent(bizContent);

            // 3. SDK 签名生成 OrderString
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
            if (response.isSuccess()) {
                String orderString = response.getBody();
                log.info("支付宝支付订单创建成功: orderId={}, orderString={}", orderId, orderString);
                return orderString;
            } else {
                log.error("支付宝支付订单创建失败: orderId={}, msg={}", orderId, response.getMsg());
                throw new RuntimeException("支付宝支付创建失败: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝SDK异常", e);
            throw new RuntimeException("支付宝支付创建失败", e);
        }
    }

    /**
     * 验证支付宝异步通知签名
     *
     * @param params 支付宝 callback 的参数 Map
     * @return true=验签通过, false=验签失败
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            // 支付宝公钥验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayPublicKey, "UTF-8", signType);

            if (!signVerified) {
                log.warn("支付宝回调验签失败: params={}", params);
                return false;
            }

            // 验签通过后校验业务数据
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            log.info("支付宝回调验签成功: outTradeNo={}, status={}, amount={}",
                    outTradeNo, tradeStatus, totalAmount);

            // 只有交易完成才处理
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return false;
        }
    }
}
