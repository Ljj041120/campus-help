package com.campushelp.controller.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushelp.common.Result;
import com.campushelp.dto.request.AcceptOrderRequest;
import com.campushelp.dto.request.CreateOrderRequest;
import com.campushelp.dto.response.OrderDetailResponse;
import com.campushelp.entity.Order;
import com.campushelp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<Order> createOrder(@RequestAttribute Long userId,
                                      @Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(userId, request);
        return Result.success(order);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailResponse> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/my")
    public Result<Page<Order>> getMyOrders(@RequestAttribute Long userId,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getUserOrders(userId, pageNum, pageSize, status));
    }

    /**
     * 订单大厅（可接的订单）
     */
    @GetMapping("/hall")
    public Result<Page<Order>> getOrderHall(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) Integer orderType) {
        return Result.success(orderService.getOrderHall(pageNum, pageSize, orderType));
    }

    /**
     * 抢单
     */
    @PostMapping("/accept")
    public Result<Void> acceptOrder(@RequestAttribute Long userId,
                                     @Valid @RequestBody AcceptOrderRequest request) {
        orderService.acceptOrder(userId, request);
        return Result.success();
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id,
                                           @RequestParam Integer status) {
        orderService.updateOrderStatus(id, status);
        return Result.success();
    }

    /**
     * Redis GEO 智能匹配
     */
    @GetMapping("/ge-match")
    public Result<Object> geoMatchRunners(@RequestParam("lng") java.math.BigDecimal lng,
                                            @RequestParam("lat") java.math.BigDecimal lat) {
        return Result.success(orderService.geMatchRunners(lng, lat));
    }

    /**
     * 支付订单（模拟支付宝回调）
     */
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id,
                                  @RequestParam String alipayTradeNo) {
        orderService.payOrder(id, alipayTradeNo);
        return Result.success();
    }
}
