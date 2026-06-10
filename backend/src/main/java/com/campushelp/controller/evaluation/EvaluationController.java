package com.campushelp.controller.evaluation;

import com.campushelp.common.Result;
import com.campushelp.entity.Evaluation;
import com.campushelp.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 提交评价
     */
    @PostMapping("/submit")
    public Result<Void> submitEvaluation(@RequestAttribute Long userId,
                                          @RequestParam Long orderId,
                                          @RequestParam Long toUserId,
                                          @RequestParam Integer score,
                                          @RequestParam(required = false, defaultValue = "") String content) {
        evaluationService.submitEvaluation(orderId, userId, toUserId, score, content);
        return Result.success();
    }

    /**
     * 获取订单评价
     */
    @GetMapping("/order/{orderId}")
    public Result<Evaluation> getEvaluation(@PathVariable Long orderId) {
        var evaluation = evaluationService.getEvaluationByOrder(orderId);
        if (evaluation == null) {
            return Result.success(null);
        }
        return Result.success(evaluation);
    }
}
