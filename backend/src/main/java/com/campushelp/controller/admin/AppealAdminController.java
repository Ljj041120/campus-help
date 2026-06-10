package com.campushelp.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushelp.common.Result;
import com.campushelp.entity.Appeal;
import com.campushelp.entity.Order;
import com.campushelp.entity.User;
import com.campushelp.mapper.AppealMapper;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.service.AppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/appeals")
@RequiredArgsConstructor
public class AppealAdminController {

    private final AppealService appealService;
    private final AppealMapper appealMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @GetMapping
    public Result<Page<Appeal>> getAppeals(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Page<Appeal> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Appeal::getStatus, status);
        wrapper.orderByDesc(Appeal::getCreatedAt);
        return Result.success(appealMapper.selectPage(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getAppealDetail(@PathVariable Long id) {
        Appeal appeal = appealService.getById(id);
        if (appeal == null) return Result.error("申诉不存在");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appeal", appeal);
        Order order = orderMapper.selectById(appeal.getOrderId());
        data.put("order", order);
        User user = userMapper.selectById(appeal.getUserId());
        data.put("user", user);
        return Result.success(data);
    }

    @PostMapping("/audit")
    public Result<Void> auditAppeal(
            @RequestParam Long appealId,
            @RequestParam Integer status,
            @RequestParam(defaultValue = "") String result,
            @RequestAttribute(value = "userId", required = false) Long auditorId) {
        appealService.auditAppeal(appealId, status, result, auditorId != null ? auditorId : 1L);
        return Result.success();
    }
}
