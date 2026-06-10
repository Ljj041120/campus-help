package com.campushelp.controller.auth;

import com.campushelp.common.Result;
import com.campushelp.dto.request.RealNameAuthRequest;
import com.campushelp.dto.request.WechatLoginRequest;
import com.campushelp.dto.response.LoginResponse;
import com.campushelp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 微信登录
     */
    @PostMapping("/wechat")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        LoginResponse response = userService.wechatLogin(request);
        return Result.success(response);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<LoginResponse> getUserInfo(@RequestAttribute Long userId) {
        var user = userService.getUserById(userId);
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setCreditScore(user.getCreditScore());
        response.setIsRealname(user.getIsRealname());
        response.setRoles(user.getRoles());
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestAttribute Long userId,
                                        @RequestParam(required = false) String nickname,
                                        @RequestParam(required = false) String avatar) {
        userService.updateUserInfo(userId, nickname, avatar);
        return Result.success();
    }

    /**
     * 提交实名认证
     */
    @PostMapping("/realname")
    public Result<Void> submitRealName(@RequestAttribute Long userId,
                                        @Valid @RequestBody RealNameAuthRequest request) {
        userService.submitRealNameAuth(userId, request.getStudentNo(),
                request.getName(), request.getIdCardPhoto());
        return Result.success();
    }

    /**
     * 获取实名认证状态
     */
    @GetMapping("/realname")
    public Result<Integer> getRealNameStatus(@RequestAttribute Long userId) {
        return Result.success(userService.getUserById(userId).getIsRealname());
    }
}
