package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.common.BusinessException;
import com.campushelp.dto.request.AcceptOrderRequest;
import com.campushelp.dto.request.CreateOrderRequest;
import com.campushelp.dto.response.OrderDetailResponse;
import com.campushelp.entity.Order;
import com.campushelp.entity.User;
import com.campushelp.enums.OrderStatus;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final UserService userService;
    private final WalletService walletService;
    private final MessagePushService messagePushService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysConfigService sysConfigService;

    @Value("${jwt.secret}")
    private String secret;

    private static final String GEO_KEY = "runner_location";
    private static final double GEO_RANGE_KM = 3.0;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long publisherId, CreateOrderRequest request) {
        // 验证金额
        if (request.getAmount().compareTo(new BigDecimal("2")) < 0) {
            throw new BusinessException("报酬金额不能低于2元");
        }
        if (request.getAmount().compareTo(new BigDecimal("50")) > 0) {
            throw new BusinessException("报酬金额不能超过50元");
        }

        // 信用分校验
        User publisher = userService.getUserById(publisherId);
        int minCredit = sysConfigService.getConfigInt("credit_score_publish_min", 60);
        if (publisher.getCreditScore() < minCredit) {
            throw new BusinessException("信用分不足" + minCredit + "，无法发布订单");
        }
        if (publisher.getIsRealname() != 1) {
            throw new BusinessException("请先完成实名认证");
        }

        // 计算平台服务费和跑腿员收入
        BigDecimal platformFeeRate = getPlatformFeeRate();
        BigDecimal platformFee = request.getAmount().multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal runnerIncome = request.getAmount().subtract(platformFee);

        Order order = new Order();
        order.setPublisherId(publisherId);
        order.setOrderType(request.getOrderType());
        order.setPickupAddress(request.getPickupAddress());
        order.setPickupLng(request.getPickupLng());
        order.setPickupLat(request.getPickupLat());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryLng(request.getDeliveryLng());
        order.setDeliveryLat(request.getDeliveryLat());
        order.setAmount(request.getAmount());
        order.setPlatformFee(platformFee);
        order.setRunnerIncome(runnerIncome);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
        order.setExpectedTime(request.getExpectedTime());
        order.setDescription(request.getDescription());
        order.setRemark(request.getRemark());
        order.setImages(request.getImages());

        this.save(order);
        return order;
    }

    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId, String alipayTradeNo) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getValue()) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        order.setStatus(OrderStatus.WAITING_FOR_ACCEPT.getValue());
        order.setAlipayTradeNo(alipayTradeNo);
        this.updateById(order);
        // 支付时冻结跑腿员收入（待结算）
        walletService.freezeCommission(order.getPublisherId(), order.getRunnerIncome());
    }

    // ==================== Redis GEO ====================

    /**
     * 注册跑腿员位置（跑腿员上线/更新位置时调用）
     */
    public void registerRunnerLocation(Long runnerId, double lng, double lat) {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(lng, lat), runnerId.toString());
    }

    /**
     * 移除跑腿员位置（跑腿员下线时调用）
     */
    public void removeRunnerLocation(Long runnerId) {
        redisTemplate.opsForGeo().remove(GEO_KEY, runnerId.toString());
    }

    /**
     * Redis GEO 智能匹配：3公里范围 + 加权排序
     */
    public List<Map<String, Object>> geMatchRunners(BigDecimal orderLng, BigDecimal orderLat) {
        try {
            // 1. GEO 范围查询：3公里内所有在线跑腿员
            Circle circle = new Circle(new Point(orderLng.doubleValue(), orderLat.doubleValue()), new Distance(GEO_RANGE_KM, Metrics.KILOMETERS));
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance().includeCoordinates().sortAscending().limit(20);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                    .radius(GEO_KEY, circle, args);

            if (results == null || results.getContent().isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 批量查数据库（消除 N+1 查询）
            List<Long> runnerIds = results.getContent().stream()
                    .map(r -> Long.valueOf(r.getContent().getName().toString()))
                    .collect(Collectors.toList());
            Map<Long, User> userMap = userService.listByIds(runnerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            // 3. 计算综合评分
            List<Map<String, Object>> scored = new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
                String runnerIdStr = result.getContent().getName().toString();
                Long runnerId = Long.valueOf(runnerIdStr);

                User runner = userMap.get(runnerId);
                if (runner == null || runner.getStatus() != 1 || runner.getIsRealname() != 1) continue;
                if (runner.getCreditScore() < 60) continue;

                double distanceKm = result.getDistance().getValue();
                int creditScore = runner.getCreditScore();

                // 3. 多维加权评分
                double distanceScore = 0.5 * (1.0 / (distanceKm + 0.1));
                double creditScoreWeight = 0.3 * (creditScore / 100.0);
                double totalScore = distanceScore + creditScoreWeight + 0.2; // acceptanceRate 默认0.2

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("runnerId", runnerId);
                item.put("nickname", runner.getNickname());
                item.put("avatar", runner.getAvatar());
                item.put("creditScore", creditScore);
                item.put("distance", Math.round(distanceKm * 1000) + "m");
                item.put("score", Math.round(totalScore * 100) / 100.0);
                scored.add(item);
            }

            // 按综合评分降序排序，取TOP10
            scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
            return scored.stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("GEO匹配异常（Redis可能未启动）: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 抢单（分布式锁） ====================

    /**
     * 抢单（Redis分布式锁）
     */
    @Transactional
    public void acceptOrder(Long runnerId, AcceptOrderRequest request) {
        Long orderId = request.getOrderId();
        String lockKey = "lock:order:" + orderId;

        try {
            // 1. 尝试获取分布式锁（30秒过期）
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, runnerId.toString(), 30, TimeUnit.SECONDS);

            if (locked == null || !locked) {
                throw new BusinessException("抢单失败，该订单已被其他人接单");
            }

            // 2. 校验订单状态
            Order order = this.getById(orderId);
            if (order == null) throw new BusinessException("订单不存在");
            if (order.getStatus() != OrderStatus.WAITING_FOR_ACCEPT.getValue()) {
                throw new BusinessException("订单状态异常，无法接单");
            }
            if (order.getPublisherId().equals(runnerId)) {
                throw new BusinessException("不能接自己的订单");
            }

            // 3. 信用分校验
            User runner = userService.getUserById(runnerId);
            if (runner.getCreditScore() < 60) {
                throw new BusinessException("信用分不足60，无法接单");
            }
            if (runner.getIsRealname() != 1) {
                throw new BusinessException("请先完成实名认证才能接单");
            }

            // 4. 执行接单
            order.setRunnerId(runnerId);
            order.setStatus(OrderStatus.ACCEPTED.getValue());
            this.updateById(order);

            // 5. 冻结跑腿员收入
            walletService.freezeCommission(runnerId, order.getRunnerIncome());

            // 6. 推送接单通知给发布者
            try {
                String[] typeNames = {"", "代取快递", "代买物品", "代送物品", "其他"};
                String typeName = order.getOrderType() != null && order.getOrderType() < typeNames.length
                        ? typeNames[order.getOrderType()] : "跑腿";
                messagePushService.pushOrderAccepted(order.getPublisherId(), typeName, order.getPickupAddress());
            } catch (Exception e) {
                log.warn("推送接单通知失败: {}", e.getMessage());
            }

        } finally {
            // 6. 释放锁（Lua 脚本保证原子性：仅持有者可释放）
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                runnerId.toString()
            );
        }
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Integer newStatus, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        // 权限校验：仅订单发布者或跑腿员可修改
        if (!order.getPublisherId().equals(userId) &&
            (order.getRunnerId() == null || !order.getRunnerId().equals(userId))) {
            throw new BusinessException("无权操作此订单");
        }
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new BusinessException("非法的状态流转: " + order.getStatus() + " -> " + newStatus);
        }
        // 订单完成时结算佣金
        if (newStatus == OrderStatus.COMPLETED.getValue() && order.getRunnerId() != null) {
            walletService.settleOrderCommission(order.getRunnerId(), order.getRunnerIncome());
        }
        order.setStatus(newStatus);
        this.updateById(order);

        // 状态变更推送消息
        try {
            if (newStatus == OrderStatus.IN_PROGRESS.getValue()) {
                messagePushService.pushOrderDelivering(order.getPublisherId());
            } else if (newStatus == OrderStatus.PENDING_CONFIRM.getValue()) {
                messagePushService.pushOrderDelivered(order.getPublisherId());
            } else if (newStatus == OrderStatus.COMPLETED.getValue() && order.getRunnerId() != null) {
                messagePushService.pushOrderSettled(order.getRunnerId(), order.getRunnerIncome().toString());
            }
        } catch (Exception e) {
            log.warn("推送状态通知失败: {}", e.getMessage());
        }
    }

    /**
     * 验证状态流转是否合法
     */
    private boolean isValidStatusTransition(int from, int to) {
        switch (from) {
            case 0: return to == 1 || to == 6;
            case 1: return to == 2 || to == 6 || to == 7;
            case 2: return to == 3 || to == 6 || to == 7;
            case 3: return to == 4 || to == 7;
            case 4: return to == 5 || to == 7;
            default: return false;
        }
    }

    /**
     * 获取订单详情
     */
    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");

        OrderDetailResponse response = new OrderDetailResponse();
        BeanUtils.copyProperties(order, response);

        User publisher = userService.getById(order.getPublisherId());
        if (publisher != null) response.setPublisherNickname(publisher.getNickname());
        if (order.getRunnerId() != null) {
            User runner = userService.getById(order.getRunnerId());
            if (runner != null) response.setRunnerNickname(runner.getNickname());
        }
        return response;
    }

    /**
     * 获取用户的订单列表
     */
    public Page<Order> getUserOrders(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Order::getPublisherId, userId).or().eq(Order::getRunnerId, userId));
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreatedAt);
        return this.page(page, wrapper);
    }

    /**
     * 获取订单大厅（待接单的订单列表）
     */
    public Page<Order> getOrderHall(Integer pageNum, Integer pageSize, Integer orderType) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, OrderStatus.WAITING_FOR_ACCEPT.getValue());
        if (orderType != null) wrapper.eq(Order::getOrderType, orderType);
        wrapper.orderByDesc(Order::getCreatedAt);
        return this.page(page, wrapper);
    }

    private BigDecimal getPlatformFeeRate() {
        return sysConfigService.getConfigDecimal("platform_fee_rate", new BigDecimal("0.05"));
    }
}
