package com.campushelp.service;

import com.campushelp.entity.User;
import com.campushelp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信订阅消息推送服务
 * 
 * 使用前需在 mp.weixin.qq.com → 功能 → 订阅消息 中添加模板
 * 然后将模板ID填入下方的常量中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TOKEN_CACHE_KEY = "wechat:access_token";
    private static final long TOKEN_TTL_SECONDS = 7100; // 提前100秒过期

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Value("${wechat.login-url}")
    private String loginUrl;

    // ===== 模板ID（微信订阅消息） =====
    /** 新订单通知（接单时通知发布者） */
    private static final String TEMPLATE_NEW_ORDER = "vlv_r9zUZpRTD--4wwF6r1MaZBXqkrkLA5TT0OeisjE";
    /** 业务受理通知（配送/送达时通知） */
    private static final String TEMPLATE_SERVICE = "kD5DCInkG-icoAsOpf2b26ljMTzSTKsP1UrML4Oz55U";
    /** 服务进度通知（状态变更） */
    private static final String TEMPLATE_PROGRESS = "Q1q5cSZjZewqag1Zkv5relFMXBb2QAuxamg_2-datL0";
    /** 收益到账通知（结算时通知跑腿员） */
    private static final String TEMPLATE_INCOME = "MCTypbaKF6ETjs7idj-s-5GLHMCSqEcdLgO-ybCtBV4";

    /**
     * 获取微信 access_token（Redis 缓存，有效期7200秒，缓存7100秒）
     */
    private String getAccessToken() {
        try {
            // 1. 先查 Redis 缓存
            Object cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
            if (cached != null) {
                return cached.toString();
            }

            // 2. 缓存未命中，调用微信接口
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + appid + "&secret=" + secret;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("access_token")) {
                String token = (String) resp.get("access_token");
                redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
                log.info("获取微信 access_token 成功（已缓存 {} 秒）", TOKEN_TTL_SECONDS);
                return token;
            } else {
                log.error("获取微信 access_token 失败: {}", resp);
                return null;
            }
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            return null;
        }
    }

    /**
     * 发送订阅消息
     */
    private boolean sendSubscribeMessage(String openid, String templateId, Map<String, Object> data) {
        try {
            String token = getAccessToken();
            if (token == null) return false;

            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token;

            Map<String, Object> body = new HashMap<>();
            body.put("touser", openid);           // 接收者的openid
            body.put("template_id", templateId);  // 模板ID
            body.put("data", data);               // 模板内容

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Map<String, Object> resp = restTemplate.postForObject(url, request, Map.class);

            if (resp != null && "ok".equals(resp.get("errmsg"))) {
                log.info("订阅消息发送成功: openid={}, templateId={}", openid, templateId);
                return true;
            } else {
                log.warn("订阅消息发送失败: openid={}, resp={}", openid, resp);
                return false;
            }
        } catch (Exception e) {
            log.error("发送订阅消息异常", e);
            return false;
        }
    }

    /**
     * 构建模板参数（所有值需包含 value 字段）
     */
    private Map<String, Object> buildData(String... pairs) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                Map<String, String> item = new HashMap<>();
                item.put("value", pairs[i + 1]);
                data.put(pairs[i], item);
            }
        }
        return data;
    }

    // ==================== 业务推送方法 ====================

    /**
     * 推送：订单被接单
     */
    public void pushOrderAccepted(Long publisherId, String orderType, String pickupAddress) {
        User user = userMapper.selectById(publisherId);
        if (user == null || user.getOpenid() == null) return;
        sendSubscribeMessage(user.getOpenid(), TEMPLATE_NEW_ORDER, buildData(
                "thing1", "您的" + orderType + "订单已被接单",
                "thing2", "取货地址: " + pickupAddress,
                "time3", "请耐心等待跑腿员送达"
        ));
    }

    /**
     * 推送：开始配送
     */
    public void pushOrderDelivering(Long publisherId) {
        User user = userMapper.selectById(publisherId);
        if (user == null || user.getOpenid() == null) return;
        sendSubscribeMessage(user.getOpenid(), TEMPLATE_PROGRESS, buildData(
                "thing1", "跑腿员已出发，正在配送中",
                "thing2", "请留意手机，准备收货"
        ));
    }

    /**
     * 推送：订单已送达，提醒确认收货
     */
    public void pushOrderDelivered(Long publisherId) {
        User user = userMapper.selectById(publisherId);
        if (user == null || user.getOpenid() == null) return;
        sendSubscribeMessage(user.getOpenid(), TEMPLATE_SERVICE, buildData(
                "thing1", "您的订单已送达",
                "thing2", "请及时确认收货",
                "time3", "确认后佣金将转入跑腿员"
        ));
    }

    /**
     * 推送：确认收货，佣金到账
     */
    public void pushOrderSettled(Long runnerId, String amount) {
        User user = userMapper.selectById(runnerId);
        if (user == null || user.getOpenid() == null) return;
        sendSubscribeMessage(user.getOpenid(), TEMPLATE_INCOME, buildData(
                "amount3", amount + "元",
                "thing1", "订单已完成，佣金已到账",
                "thing2", "查看钱包余额"
        ));
    }
}
