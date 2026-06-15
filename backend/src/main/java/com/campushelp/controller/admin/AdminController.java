package com.campushelp.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushelp.common.Result;
import com.campushelp.config.JwtUtil;
import com.campushelp.entity.Order;
import com.campushelp.entity.User;
import com.campushelp.mapper.RealNameAuthMapper;
import com.campushelp.entity.RealNameAuth;
import com.campushelp.enums.AuthStatus;
import com.campushelp.service.OrderService;
import com.campushelp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RealNameAuthMapper realNameAuthMapper;
    private final com.campushelp.mapper.OrderMapper orderMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 初始化管理员账号（首次启动时）
     */
    @jakarta.annotation.PostConstruct
    public void initAdmin() {
        var admin = userService.lambdaQuery()
                .eq(User::getOpenid, "admin_test")
                .eq(User::getRoles, "admin")
                .one();
        if (admin != null) {
            // 确保密码字段有 BCrypt 加密值
            String rawPassword = "admin123";
            if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(rawPassword));
                userService.updateById(admin);
            }
        }
    }

    /**
     * 管理员登录（BCrypt 校验）
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 查数据库获取管理员用户
        var admin = userService.lambdaQuery()
                .eq(User::getOpenid, username)
                .eq(User::getRoles, "admin")
                .one();

        if (admin != null && passwordEncoder.matches(password, admin.getPassword())) {
            String token = jwtUtil.generateToken(admin.getId(), "admin");
            return Result.success(Map.of("token", token, "username", username));
        }
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 数据大屏统计
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        long totalUsers = userService.count();
        long totalOrders = orderService.count();

        // 今日订单数 & 成交额
        String today = java.time.LocalDate.now().toString();
        var todayWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, today + " 00:00:00")
                .le(Order::getCreatedAt, today + " 23:59:59");
        long todayOrders = orderService.count(todayWrapper);
        BigDecimal todayGMV = orderService.lambdaQuery()
                .ge(Order::getCreatedAt, today + " 00:00:00")
                .le(Order::getCreatedAt, today + " 23:59:59")
                .eq(Order::getStatus, 5)
                .list().stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 各状态订单统计（单条 SQL GROUP BY）
        Map<String, Long> statusCount = new LinkedHashMap<>();
        List<Map<String, Object>> statusRows = orderMapper.countByStatus();
        for (Map<String, Object> row : statusRows) {
            Object statusObj = row.get("status");
            Object cntObj = row.get("cnt");
            if (statusObj != null && cntObj != null) {
                int st = ((Number) statusObj).intValue();
                long cnt = ((Number) cntObj).longValue();
                if (st == 1) statusCount.put("waiting", cnt);
                else if (st == 3) statusCount.put("inProgress", cnt);
                else if (st == 5) statusCount.put("completed", cnt);
            }
        }
        // 确保默认值
        statusCount.putIfAbsent("waiting", 0L);
        statusCount.putIfAbsent("inProgress", 0L);
        statusCount.putIfAbsent("completed", 0L);

        // 服务类型分布（单条 SQL GROUP BY）
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        List<Map<String, Object>> typeRows = orderMapper.countByOrderType();
        for (Map<String, Object> row : typeRows) {
            Object typeObj = row.get("order_type");
            Object cntObj = row.get("cnt");
            if (typeObj != null && cntObj != null) {
                int ot = ((Number) typeObj).intValue();
                long cnt = ((Number) cntObj).longValue();
                if (ot == 1) typeDistribution.put("代取快递", cnt);
                else if (ot == 2) typeDistribution.put("代买物品", cnt);
                else if (ot == 3) typeDistribution.put("代送物品", cnt);
                else if (ot == 4) typeDistribution.put("其他", cnt);
            }
        }
        typeDistribution.putIfAbsent("代取快递", 0L);
        typeDistribution.putIfAbsent("代买物品", 0L);
        typeDistribution.putIfAbsent("代送物品", 0L);
        typeDistribution.putIfAbsent("其他", 0L);

        // 在线用户（Redis）
        Long onlineUsers = 0L;
        try {
            onlineUsers = redisTemplate.opsForSet().size("online_users");
        } catch (Exception ignored) {}

        // 待审核数
        long pendingAuths = realNameAuthMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RealNameAuth>()
                .eq(RealNameAuth::getStatus, AuthStatus.PENDING.getValue())
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalOrders", totalOrders);
        data.put("todayOrders", todayOrders);
        data.put("todayGMV", todayGMV);
        data.put("onlineUsers", onlineUsers);
        data.put("statusCount", statusCount);
        data.put("typeDistribution", typeDistribution);
        data.put("pendingAuths", pendingAuths);

        return Result.success(data);
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Result<Page<User>> getUsers(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String keyword) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        var page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    /**
     * 用户封禁/解封
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 订单列表
     */
    @GetMapping("/orders")
    public Result<Page<Order>> getOrders(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Integer status) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        var page = orderService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }
}
