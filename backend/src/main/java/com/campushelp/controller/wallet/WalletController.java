package com.campushelp.controller.wallet;

import com.campushelp.common.Result;
import com.campushelp.entity.TransactionRecord;
import com.campushelp.entity.Wallet;
import com.campushelp.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 获取钱包信息
     */
    @GetMapping("/info")
    public Result<Wallet> getWalletInfo(@RequestAttribute Long userId) {
        return Result.success(walletService.getOrCreateWallet(userId));
    }

    /**
     * 提现申请
     */
    @PostMapping("/withdraw")
    public Result<Void> applyWithdraw(@RequestAttribute Long userId,
                                       @RequestParam java.math.BigDecimal amount) {
        walletService.applyWithdraw(userId, amount);
        return Result.success();
    }

    /**
     * 交易流水
     */
    @GetMapping("/records")
    public Result<List<TransactionRecord>> getRecords(@RequestAttribute Long userId,
                                                       @RequestParam(defaultValue = "50") int limit) {
        return Result.success(walletService.getTransactionRecords(userId, limit));
    }
}
