package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushelp.entity.Order;
import com.campushelp.enums.OrderStatus;
import com.campushelp.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String JOB_LOCK_KEY = "lock:job:timeout";
    private static final long JOB_LOCK_TTL = 55; // 比 fixedDelay 略短

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelTimeoutOrders() {
        // 分布式锁：多实例部署时仅一个执行
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(JOB_LOCK_KEY, "1", JOB_LOCK_TTL, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            return;
        }
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
            // 逐条更新（兼容 MyBatis-Plus 基础 Mapper）
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
