package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.common.BusinessException;
import com.campushelp.config.JwtUtil;
import com.campushelp.dto.request.WechatLoginRequest;
import com.campushelp.dto.response.LoginResponse;
import com.campushelp.entity.User;
import com.campushelp.entity.RealNameAuth;
import com.campushelp.enums.AuthStatus;
import com.campushelp.mapper.RealNameAuthMapper;
import com.campushelp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final JwtUtil jwtUtil;
    private final RealNameAuthMapper realNameAuthMapper;

    /**
     * 微信登录
     */
    @Transactional
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        // 1. 调用微信接口获取openid（简化：使用code作为模拟openid）
        String openid = getOpenidFromWechat(request.getCode());
        if (openid == null) {
            throw new BusinessException("微信登录失败，请重试");
        }

        // 2. 查询或创建用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = createUser(openid);
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getRoles());

        // 5. 返回登录信息
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setCreditScore(user.getCreditScore());
        response.setRoles(user.getRoles());

        // 查询实名认证状态
        var auth = realNameAuthMapper.selectOne(
            new LambdaQueryWrapper<RealNameAuth>().eq(RealNameAuth::getUserId, user.getId()));
        if (auth != null && auth.getStatus() == AuthStatus.APPROVED.getValue()) {
            response.setIsRealname(1);
            user.setIsRealname(1);
        } else {
            response.setIsRealname(user.getIsRealname());
        }

        // 更新用户登录时间
        user.setUpdatedAt(LocalDateTime.now());
        this.updateById(user);

        return response;
    }

    /**
     * 模拟获取openid（实际项目中调用微信api）
     */
    private String getOpenidFromWechat(String code) {
        // 模拟：code 直接映射为 openid
        // 实际实现：调用 https://api.weixin.qq.com/sns/jscode2session
        if (code == null || code.isEmpty()) {
            return null;
        }
        // 这里简化处理，返回模拟openid
        return "wx_" + code.hashCode();
    }

    /**
     * 创建新用户
     */
    private User createUser(String openid) {
        User user = new User();
        user.setOpenid(openid);
        user.setNickname("校园帮用户");
        user.setAvatar("https://example.com/default_avatar.png");
        user.setCreditScore(80);
        user.setIsRealname(0);
        user.setRoles("publisher,runner");
        user.setStatus(1);

        this.save(user);
        return user;
    }

    /**
     * 获取用户信息
     */
    public User getUserById(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(Long userId, String nickname, String avatar) {
        User user = getUserById(userId);
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        this.updateById(user);
    }

    /**
     * 提交实名认证
     */
    @Transactional
    public void submitRealNameAuth(Long userId, String studentNo, String name, String idCardPhoto) {
        // 检查是否已有待审核记录
        var existing = realNameAuthMapper.selectOne(
            new LambdaQueryWrapper<RealNameAuth>()
                .eq(RealNameAuth::getUserId, userId)
                .eq(RealNameAuth::getStatus, AuthStatus.PENDING.getValue()));

        if (existing != null) {
            throw new BusinessException("已有待审核记录，请稍候");
        }

        RealNameAuth auth = new RealNameAuth();
        auth.setUserId(userId);
        auth.setStudentNo(studentNo);
        auth.setName(name);
        auth.setIdCardPhoto(idCardPhoto);
        auth.setStatus(AuthStatus.PENDING.getValue());
        realNameAuthMapper.insert(auth);

        // 模拟OCR识别：如果学号和姓名匹配，自动通过
        if (studentNo != null && studentNo.length() >= 8 && name != null && name.length() >= 2) {
            auth.setStatus(AuthStatus.APPROVED.getValue());
            User user = getUserById(userId);
            user.setIsRealname(1);
            this.updateById(user);
        }
    }

    /**
     * 审核实名认证（管理员用）
     */
    @Transactional
    public void auditRealNameAuth(Long authId, Integer status, String comment, Long auditorId) {
        var auth = realNameAuthMapper.selectById(authId);
        if (auth == null) {
            throw new BusinessException("实名认证记录不存在");
        }

        auth.setStatus(status);
        auth.setAuditComment(comment);
        auth.setAuditorId(auditorId);
        realNameAuthMapper.updateById(auth);

        // 更新用户实名认证状态
        if (status == AuthStatus.APPROVED.getValue()) {
            User user = getUserById(auth.getUserId());
            user.setIsRealname(1);
            this.updateById(user);
        }
    }
}
