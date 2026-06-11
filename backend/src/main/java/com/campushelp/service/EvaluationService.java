package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.common.BusinessException;
import com.campushelp.entity.Evaluation;
import com.campushelp.entity.User;
import com.campushelp.mapper.EvaluationMapper;
import com.campushelp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService extends ServiceImpl<EvaluationMapper, Evaluation> {

    private final UserMapper userMapper;

    /**
     * 提交评价
     */
    @Transactional
    public void submitEvaluation(Long orderId, Long fromUserId, Long toUserId,
                                  Integer score, String content) {
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

        // 信用分原子更新（避免并发竞态条件）
        if (toUserId != null) {
            User toUser = userMapper.selectById(toUserId);
            if (toUser != null) {
                int delta;
                if (score <= 2) {
                    delta = -2;  // 低分：扣2分
                } else if (score >= 4) {
                    delta = +1;  // 高分：加1分
                } else {
                    delta = 0;   // 3分：不变
                }
                if (delta != 0) {
                    toUser.setCreditScore(Math.max(0, Math.min(100, toUser.getCreditScore() + delta)));
                    userMapper.updateById(toUser);
                }
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
