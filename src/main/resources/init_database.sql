-- =====================================================
-- 算力系统数据库初始化脚本
-- 数据库: wakapix
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS wakapix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wakapix;

-- =====================================================
-- 1. 修改用户表添加算力字段
-- =====================================================
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS available_power INT DEFAULT 300 COMMENT '可用算力' AFTER avatar;

-- 如果 ALTER TABLE IF NOT EXISTS 不支持（MySQL 5.7），使用以下语句：
-- ALTER TABLE t_user ADD COLUMN available_power INT DEFAULT 300 COMMENT '可用算力' AFTER avatar;

-- =====================================================
-- 2. 创建套餐表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description TEXT COMMENT '套餐描述',
    package_type VARCHAR(20) NOT NULL COMMENT '套餐类型：PERSONAL(个人版)/TEAM(团队版)/REFILL(加油包)',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    monthly_quota INT COMMENT '每月算力配额',
    valid_days INT COMMENT '有效期天数',
    priority INT DEFAULT 0 COMMENT '排序优先级',
    status INT DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_package_type (package_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐表';

-- =====================================================
-- 3. 创建订单表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    package_id BIGINT COMMENT '套餐ID',
    package_name VARCHAR(100) COMMENT '套餐名称',
    order_type VARCHAR(20) NOT NULL COMMENT '订单类型：PACKAGE(套餐)/POWER(算力)',
    payment_method VARCHAR(20) NOT NULL COMMENT '支付方式：WECHAT(微信)/ALIPAY(支付宝)',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    calculation_power INT COMMENT '算力数量',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING(待支付)/PAID(已支付)/CANCELLED(已取消)/REFUNDED(已退款)',
    pay_time DATETIME COMMENT '支付时间',
    transaction_id VARCHAR(100) COMMENT '交易号(微信/支付宝)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================================
-- 4. 创建用户套餐表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_user_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    remaining_quota INT NOT NULL COMMENT '剩余配额',
    total_quota INT NOT NULL COMMENT '总配额',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    is_auto_renew INT DEFAULT 0 COMMENT '是否自动续费：0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_end_time (end_time),
    INDEX idx_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户套餐表';

-- =====================================================
-- 5. 初始化套餐数据
-- =====================================================
-- 清空现有数据（可选，慎用）
-- TRUNCATE TABLE t_package;

-- 插入套餐数据
INSERT INTO t_package (name, description, package_type, price, original_price, monthly_quota, valid_days, priority, status) VALUES
-- 个人版套餐
('免费版', '免费获得300算力，有效期7天，适合新用户体验', 'PERSONAL', 0.00, 0.00, 300, 7, 0, 1),
('基础版', '每月3500算力，性价比之选，适合日常使用', 'PERSONAL', 69.00, 129.00, 3500, 30, 1, 1),
('高级版', '每月21000算力，专业级体验，适合深度用户', 'PERSONAL', 299.00, 599.00, 21000, 30, 2, 1),

-- 团队版套餐
('团队基础版', '每年432000算力，支持5人团队，适合小型团队', 'TEAM', 4790.40, 12468.00, 432000, 365, 10, 1),
('团队高级版', '每年864000算力，支持10人团队，适合中型团队', 'TEAM', 7660.80, 12456.00, 864000, 365, 11, 1),
('团队旗舰版', '每年1728000算力，支持20人团队，适合大型团队', 'TEAM', 19104.00, 27720.00, 1728000, 365, 12, 1),

-- 算力加油包
('算力加油包1000', '1000算力，即充即用，无有效期限制', 'REFILL', 60.00, 60.00, 1000, 0, 20, 1),
('算力加油包5000', '5000算力，性价比更高，即充即用', 'REFILL', 300.00, 300.00, 5000, 0, 21, 1),
('算力加油包20000', '20000算力，大额优惠，即充即用', 'REFILL', 1200.00, 1200.00, 20000, 0, 22, 1);

-- =====================================================
-- 6. 验证数据
-- =====================================================
SELECT '套餐数据初始化完成' AS result;
SELECT COUNT(*) AS package_count FROM t_package;

-- 显示所有套餐
SELECT id, name, package_type, price, monthly_quota, valid_days, status
FROM t_package
ORDER BY package_type, priority;

-- =====================================================
-- 执行说明：
-- 1. 确保MySQL服务已启动
-- 2. 使用root用户连接MySQL: mysql -u root -p
-- 3. 选择数据库: USE wakapix;
-- 4. 或者直接执行整个脚本: mysql -u root -p < init_database.sql
-- =====================================================
