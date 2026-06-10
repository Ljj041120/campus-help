package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushelp.entity.Order;
import com.campushelp.enums.OrderStatus;
import com.campushelp.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;

    @Scheduled(fixedDelay = 60000)
    public void cancelTimeoutOrders() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Order> paymentTimeout = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getValue())
                    .lt(Order::getCreatedAt, now.minusMinutes(30))
            );
            for (Order o : paymentTimeout) {
                o.setStatus(OrderStatus.CANCELLED.getValue());
                orderMapper.updateById(o);
                log.info("超时自动取消(未支付): orderId={}", o.getId());
            }

            List<Order> acceptTimeout = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, OrderStatus.WAITING_FOR_ACCEPT.getValue())
                    .lt(Order::getCreatedAt, now.minusHours(2))
            );
            for (Order o : acceptTimeout) {
                o.setStatus(OrderStatus.CANCELLED.getValue());
                orderMapper.updateById(o);
                log.info("超时自动取消(无人接单): orderId={}", o.getId());
            }
        } catch (Exception e) {
            log.error("超时取消任务异常", e);
        }
    }
}
