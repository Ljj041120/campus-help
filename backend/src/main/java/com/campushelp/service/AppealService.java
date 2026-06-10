package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.common.BusinessException;
import com.campushelp.entity.Appeal;
import com.campushelp.mapper.AppealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealService extends ServiceImpl<AppealMapper, Appeal> {

    /**
     * 提交申诉
     */
    public Appeal submitAppeal(Long orderId, Long userId, String reason, String evidence) {
        // 检查是否已有申诉
        Long count = this.count(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getOrderId, orderId)
                .eq(Appeal::getUserId, userId));
        if (count > 0) {
            throw new BusinessException("该订单您已申诉，请等待处理");
        }

        Appeal appeal = new Appeal();
        appeal.setOrderId(orderId);
        appeal.setUserId(userId);
        appeal.setReason(reason);
        appeal.setEvidence(evidence);
        appeal.setStatus(0); // 待处理
        this.save(appeal);
        return appeal;
    }

    /**
     * 仲裁处理
     */
    public void auditAppeal(Long appealId, Integer status, String result, Long auditorId) {
        Appeal appeal = this.getById(appealId);
        if (appeal == null) throw new BusinessException("申诉不存在");
        appeal.setStatus(status);
        appeal.setResult(result);
        appeal.setAuditorId(auditorId);
        this.updateById(appeal);
    }

    /**
     * 获取待处理申诉
     */
    public List<Appeal> getPendingAppeals() {
        return this.list(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getStatus, 0)
                .orderByDesc(Appeal::getCreatedAt));
    }
}
