package com.campushelp.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushelp.common.Result;
import com.campushelp.entity.RealNameAuth;
import com.campushelp.entity.User;
import com.campushelp.enums.AuthStatus;
import com.campushelp.mapper.RealNameAuthMapper;
import com.campushelp.mapper.UserMapper;
import com.campushelp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RealNameAdminController {

    private final UserService userService;
    private final RealNameAuthMapper realNameAuthMapper;
    private final UserMapper userMapper;

    /**
     * 待审核列表（带用户信息）
     */
    @GetMapping("/auths/pending")
    public Result<Map<String, Object>> getPendingAuths(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        Page<RealNameAuth> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RealNameAuth> wrapper = new LambdaQueryWrapper<RealNameAuth>()
                .eq(RealNameAuth::getStatus, AuthStatus.PENDING.getValue())
                .orderByDesc(RealNameAuth::getCreatedAt);

        Page<RealNameAuth> result = realNameAuthMapper.selectPage(page, wrapper);

        // 关联查询用户昵称
        List<Map<String, Object>> records = new ArrayList<>();
        for (RealNameAuth auth : result.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", auth.getId());
            item.put("userId", auth.getUserId());
            item.put("studentNo", auth.getStudentNo());
            item.put("name", auth.getName());
            item.put("idCardPhoto", auth.getIdCardPhoto());
            item.put("status", auth.getStatus());
            item.put("createdAt", auth.getCreatedAt());

            User user = userMapper.selectById(auth.getUserId());
            item.put("userNickname", user != null ? user.getNickname() : "未知");
            item.put("userAvatar", user != null ? user.getAvatar() : "");

            records.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());

        return Result.success(data);
    }

    /**
     * 审核实名认证
     */
    @PostMapping("/auths/audit")
    public Result<Void> auditAuth(@RequestParam Long authId,
                                   @RequestParam Integer status,
                                   @RequestParam(required = false, defaultValue = "") String comment,
                                   @RequestAttribute(value = "userId", required = false) Long auditorId) {
        if (auditorId == null) {
            // 管理后台调用时从request获取，若未登录则使用1（管理员）
            auditorId = 1L;
        }
        userService.auditRealNameAuth(authId, status, comment, auditorId);
        return Result.success();
    }
}
