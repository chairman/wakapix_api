ALTER TABLE t_user ADD COLUMN available_power INT DEFAULT 300 COMMENT '可用算力' AFTER avatar;

CREATE TABLE IF NOT EXISTS t_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description TEXT COMMENT '套餐描述',
    package_type VARCHAR(20) NOT NULL COMMENT '套餐类型：PERSONAL/TEAM/REFILL',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    package_id BIGINT COMMENT '套餐ID',
    package_name VARCHAR(100) COMMENT '套餐名称',
    order_type VARCHAR(20) NOT NULL COMMENT '订单类型：PACKAGE/POWER',
    payment_method VARCHAR(20) NOT NULL COMMENT '支付方式：WECHAT/ALIPAY',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    calculation_power INT COMMENT '算力数量',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PAID/CANCELLED',
    pay_time DATETIME COMMENT '支付时间',
    transaction_id VARCHAR(100) COMMENT '交易号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS t_user_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户套餐表';

INSERT INTO t_package (name, description, package_type, price, original_price, monthly_quota, valid_days, priority, status) VALUES
('免费版', '免费获得300算力，有效期7天', 'PERSONAL', 0.00, 0.00, 300, 7, 0, 1),
('基础版', '每月3500算力，性价比之选', 'PERSONAL', 69.00, 129.00, 3500, 30, 1, 1),
('高级版', '每月21000算力，专业级体验', 'PERSONAL', 299.00, 599.00, 21000, 30, 2, 1),
('团队版', '每年432000算力，支持5人团队', 'TEAM', 4790.40, 12468.00, 432000, 365, 10, 1),
('团队版高级年付包', '每年864000算力，支持10人团队', 'TEAM', 7660.80, 12456.00, 864000, 365, 11, 1),
('团队版旗舰年付包', '每年1728000算力，支持20人团队', 'TEAM', 19104.00, 27720.00, 1728000, 365, 12, 1),
('算力加油包1000', '1000算力，即充即用', 'REFILL', 60.00, 60.00, 1000, 0, 20, 1),
('算力加油包5000', '5000算力，性价比更高', 'REFILL', 300.00, 300.00, 5000, 0, 21, 1),
('算力加油包20000', '20000算力，大额优惠', 'REFILL', 1200.00, 1200.00, 20000, 0, 22, 1);