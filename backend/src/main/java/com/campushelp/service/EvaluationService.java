package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushelp.common.BusinessException;
import com.campushelp.entity.Evaluation;
import com.campushelp.entity.Order;
import com.campushelp.entity.User;
import com.campushelp.enums.OrderStatus;
import com.campushelp.mapper.EvaluationMapper;
import com.campushelp.mapper.OrderMapper;
import com.campushelp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j

@Service
@RequiredArgsConstructor
public class EvaluationService extends ServiceImpl<EvaluationMapper, Evaluation> {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    /**
     * 提交评价
     */
    @Transactional
    public void submitEvaluation(Long orderId, Long fromUserId, Long toUserId,
                                  Integer score, String content) {
        // 校验订单是否已完成
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != OrderStatus.COMPLETED.getValue()) {
            throw new BusinessException("订单未完成，无法评价");
        }

        // 检查是否已评价
        var existing = this.getOne(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getOrderId, orderId));
        if (existing != null) {
            throw new BusinessException("该订单已评价");
        }

        // 校验评分
        if (score < 1 || score > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setFromUserId(fromUserId);
        evaluation.setToUserId(toUserId);
        evaluation.setScore(score);
        evaluation.setContent(content);
        this.save(evaluation);

        // 信用分原子更新（SQL 原地加减，避免并发竞态）
        if (toUserId != null) {
            int delta;
            if (score <= 2) {
                delta = -2;  // 低分：扣2分
            } else if (score >= 4) {
                delta = +1;  // 高分：加1分
            } else {
                delta = 0;   // 3分：不变
            }
            if (delta != 0) {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, toUserId)
                        .setSql("credit_score = GREATEST(0, LEAST(100, credit_score + " + delta + "))"));
                log.info("信用分更新: userId={}, delta={}", toUserId, delta);
            }
        }
    }

    /**
     * 获取用户收到的评价
     */
    public List<Evaluation> getEvaluationsByUser(Long userId) {
        return this.list(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getToUserId, userId)
                .orderByDesc(Evaluation::getCreatedAt));
    }

    /**
     * 获取订单的评价
     */
    public Evaluation getEvaluationByOrder(Long orderId) {
        return this.getOne(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getOrderId, orderId));
    }
}
