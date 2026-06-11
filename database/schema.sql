-- ============================================
-- 校园互助跑腿平台 - 数据库初始化脚本
-- 版本: V1.0
-- 日期: 2026-05-13
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_help;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid          VARCHAR(64)   NOT NULL COMMENT '微信openid',
    nickname        VARCHAR(64)   DEFAULT '' COMMENT '微信昵称',
    avatar          VARCHAR(256)  DEFAULT '' COMMENT '头像URL',
    phone           VARCHAR(20)   DEFAULT '' COMMENT '手机号',
    credit_score    INT           DEFAULT 80 COMMENT '信用分(0-100,初始80)',
    is_realname     INT           DEFAULT 0 COMMENT '是否实名认证:0-否 1-是',
    roles           VARCHAR(32)   DEFAULT 'publisher,runner' COMMENT '角色',
    status          INT           DEFAULT 1 COMMENT '账号状态:0-禁用 1-正常',
    deleted         INT           DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 实名认证表
-- ============================================
CREATE TABLE IF NOT EXISTS real_name_auth (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    student_no      VARCHAR(32)   NOT NULL COMMENT '学号/工号',
    name            VARCHAR(32)   NOT NULL COMMENT '姓名',
    id_card_photo   VARCHAR(256)  NOT NULL COMMENT '学生证照片URL',
    status          INT           DEFAULT 0 COMMENT '审核状态:0-待审核 1-已通过 2-已拒绝',
    audit_comment   VARCHAR(256)  DEFAULT '' COMMENT '审核意见',
    auditor_id      BIGINT        DEFAULT NULL COMMENT '审核人ID',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实名认证表';

-- ============================================
-- 3. 订单表
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id    BIGINT        NOT NULL COMMENT '发布者ID',
    runner_id       BIGINT        DEFAULT NULL COMMENT '跑腿员ID',
    order_type      INT           NOT NULL COMMENT '跑腿类型:1-代取快递 2-代买物品 3-代送物品 4-其他',
    pickup_address  VARCHAR(256)  NOT NULL COMMENT '取货地址',
    pickup_lng      DECIMAL(10,7) NOT NULL COMMENT '取货经度',
    pickup_lat      DECIMAL(10,7) NOT NULL COMMENT '取货纬度',
    delivery_address VARCHAR(256) NOT NULL COMMENT '送货地址',
    delivery_lng    DECIMAL(10,7) NOT NULL COMMENT '送货经度',
    delivery_lat    DECIMAL(10,7) NOT NULL COMMENT '送货纬度',
    amount          DECIMAL(8,2)  NOT NULL COMMENT '报酬金额',
    platform_fee    DECIMAL(8,2)  DEFAULT 0.00 COMMENT '平台服务费',
    runner_income   DECIMAL(8,2)  DEFAULT 0.00 COMMENT '跑腿员实际收入',
    status          INT           DEFAULT 0 COMMENT '订单状态:0-待支付 1-待接单 2-已接单 3-进行中 4-待确认 5-已完成 6-已取消 7-申诉中',
    expected_time   DATETIME      NOT NULL COMMENT '期望完成时间',
    description     VARCHAR(512)  DEFAULT '' COMMENT '物品描述',
    remark          VARCHAR(256)  DEFAULT '' COMMENT '备注',
    images          VARCHAR(1024) DEFAULT '' COMMENT '图片URL,逗号分隔',
    alipay_trade_no VARCHAR(64)   DEFAULT '' COMMENT '支付宝交易号',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_publisher (publisher_id),
    KEY idx_runner (runner_id),
    KEY idx_status (status),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 4. 钱包表
-- ============================================
CREATE TABLE IF NOT EXISTS wallets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    balance         DECIMAL(10,2) DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount   DECIMAL(10,2) DEFAULT 0.00 COMMENT '冻结金额',
    total_income    DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计收入',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包表';

-- ============================================
-- 5. 交易流水表
-- ============================================
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       BIGINT        NOT NULL COMMENT '钱包ID',
    order_id        BIGINT        DEFAULT NULL COMMENT '订单ID',
    type            INT           NOT NULL COMMENT '交易类型:1-订单支付 2-订单结算 3-提现 4-退款 5-平台佣金',
    amount          DECIMAL(10,2) NOT NULL COMMENT '交易金额',
    balance_after   DECIMAL(10,2) NOT NULL COMMENT '交易后余额',
    description     VARCHAR(256)  DEFAULT '' COMMENT '描述',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wallet (wallet_id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水表';

-- ============================================
-- 6. 聊天消息表
-- ============================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    sender_id       BIGINT        NOT NULL COMMENT '发送者ID',
    receiver_id     BIGINT        NOT NULL COMMENT '接收者ID',
    content         TEXT          NOT NULL COMMENT '消息内容',
    message_type    INT           DEFAULT 1 COMMENT '消息类型:1-文字 2-图片 3-位置',
    status          INT           DEFAULT 1 COMMENT '消息状态:0-发送中 1-已发送 2-已读',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order (order_id),
    KEY idx_sender (sender_id),
    KEY idx_receiver (receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ============================================
-- 7. 评价表
-- ============================================
CREATE TABLE IF NOT EXISTS evaluations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    from_user_id    BIGINT        NOT NULL COMMENT '评价者ID',
    to_user_id      BIGINT        NOT NULL COMMENT '被评价者ID',
    score           INT           NOT NULL COMMENT '评分(1-5)',
    content         VARCHAR(512)  DEFAULT '' COMMENT '评价内容',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order (order_id),
    KEY idx_from_user (from_user_id),
    KEY idx_to_user (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 8. 申诉表
-- ============================================
CREATE TABLE IF NOT EXISTS appeals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    user_id         BIGINT        NOT NULL COMMENT '申诉人ID',
    reason          VARCHAR(512)  NOT NULL COMMENT '申诉原因',
    evidence        VARCHAR(1024) DEFAULT '' COMMENT '证据(图片URL,逗号分隔)',
    status          INT           DEFAULT 0 COMMENT '处理状态:0-待处理 1-已处理',
    result          VARCHAR(512)  DEFAULT '' COMMENT '仲裁结果',
    auditor_id      BIGINT        DEFAULT NULL COMMENT '仲裁人ID',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_order (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';

-- ============================================
-- 9. 系统配置表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(64)   NOT NULL COMMENT '配置键',
    config_value    VARCHAR(512)  NOT NULL COMMENT '配置值',
    description     VARCHAR(256)  DEFAULT '' COMMENT '描述',
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 初始化系统配置
-- ============================================
INSERT INTO sys_config (config_key, config_value, description) VALUES
('platform_fee_rate', '0.05', '平台服务费比例(默认5%)'),
('min_order_amount', '2.00', '最小订单金额'),
('max_order_amount', '50.00', '最大订单金额'),
('min_withdraw_amount', '10.00', '最小提现金额'),
('max_withdraw_per_day', '500.00', '每日提现限额'),
('credit_score_initial', '80', '新用户初始信用分'),
('credit_score_publish_min', '60', '最低发布信用分'),
('payment_timeout_minutes', '30', '支付超时(分钟)');

-- ============================================
-- 初始化测试数据
-- ============================================
-- 测试管理员用户
INSERT INTO users (openid, nickname, avatar, credit_score, is_realname, roles, status) VALUES
('admin_test', 'Admin', 'https://example.com/avatar_admin.png', 100, 1, 'admin', 1);

-- 测试跑腿员用户
INSERT INTO users (openid, nickname, avatar, credit_score, is_realname, roles, status) VALUES
('runner_test', 'Runner', 'https://example.com/avatar_runner.png', 85, 1, 'publisher,runner', 1);

-- 测试发布者用户
INSERT INTO users (openid, nickname, avatar, credit_score, is_realname, roles, status) VALUES
('publisher_test', 'Publisher', 'https://example.com/avatar_publisher.png', 80, 1, 'publisher,runner', 1);

-- ============================================
-- 性能索引（在初始化后执行）
-- ============================================
-- orders: 订单大厅查询
ALTER TABLE orders ADD INDEX idx_status_created (status, created_at);
-- orders: 防支付宝重复回调
ALTER TABLE orders ADD UNIQUE INDEX uk_alipay_trade_no (alipay_trade_no);
-- chat_messages: 聊天记录查询
ALTER TABLE chat_messages ADD INDEX idx_order_created (order_id, created_at);
-- transactions: 流水查询
ALTER TABLE transactions ADD INDEX idx_wallet_created (wallet_id, created_at);
