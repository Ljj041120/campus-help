package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.common.BusinessException;
import com.campushelp.entity.TransactionRecord;
import com.campushelp.entity.Wallet;
import com.campushelp.enums.TransactionType;
import com.campushelp.mapper.TransactionMapper;
import com.campushelp.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService extends ServiceImpl<WalletMapper, Wallet> {

    private final TransactionMapper transactionMapper;

    /**
     * 获取或创建用户钱包
     */
    public Wallet getOrCreateWallet(Long userId) {
        Wallet wallet = this.getOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenAmount(BigDecimal.ZERO);
            wallet.setTotalIncome(BigDecimal.ZERO);
            this.save(wallet);
        }
        return wallet;
    }

    /**
     * 结算订单佣金（订单完成后将冻结金额转入可用余额）
     */
    @Transactional
    public void settleOrderCommission(Long runnerId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(runnerId);

        // 解冻并转入可用余额
        wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalIncome(wallet.getTotalIncome().add(amount));
        this.updateById(wallet);

        // 记录交易流水
        saveTransaction(wallet.getId(), null, TransactionType.ORDER_SETTLE, amount,
                wallet.getBalance(), "订单结算，佣金到账");
    }

    /**
     * 冻结佣金（接单后冻结跑腿员收入）
     */
    @Transactional
    public void freezeCommission(Long runnerId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(runnerId);
        wallet.setFrozenAmount(wallet.getFrozenAmount().add(amount));
        this.updateById(wallet);
    }

    /**
     * 提现申请
     */
    @Transactional
    public void applyWithdraw(Long userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);

        if (amount.compareTo(new BigDecimal("10")) < 0) {
            throw new BusinessException("最低提现金额为10元");
        }
        if (amount.compareTo(new BigDecimal("500")) > 0) {
            throw new BusinessException("每日提现限额500元");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("余额不足");
        }

        // 扣除余额
        wallet.setBalance(wallet.getBalance().subtract(amount));
        this.updateById(wallet);

        // 记录提现流水
        saveTransaction(wallet.getId(), null, TransactionType.WITHDRAW, amount.negate(),
                wallet.getBalance(), "提现申请，金额" + amount);
    }

    /**
     * 记录交易流水
     */
    private void saveTransaction(Long walletId, Long orderId, TransactionType type,
                                  BigDecimal amount, BigDecimal balanceAfter, String description) {
        TransactionRecord record = new TransactionRecord();
        record.setWalletId(walletId);
        record.setOrderId(orderId);
        record.setType(type.getValue());
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setDescription(description);
        transactionMapper.insert(record);
    }

    /**
     * 获取交易流水
     */
    public java.util.List<TransactionRecord> getTransactionRecords(Long userId, int limit) {
        Wallet wallet = getOrCreateWallet(userId);
        return transactionMapper.selectList(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getWalletId, wallet.getId())
                        .orderByDesc(TransactionRecord::getCreatedAt)
                        .last("LIMIT " + limit));
    }
}
