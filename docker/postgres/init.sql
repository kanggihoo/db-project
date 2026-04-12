-- ============================================
-- pg_stat_statements 확장 활성화 (가장 먼저)
-- ============================================
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- ============================================
-- Layer 0: 의존성 없는 독립 테이블
-- ============================================

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20),
    gender      VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    birth_date  DATE,
    grade       VARCHAR(10) NOT NULL DEFAULT 'BRONZE' CHECK (grade IN ('BRONZE', 'SILVER', 'GOLD', 'VIP')),
    point_balance INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES category(id),
    name        VARCHAR(100) NOT NULL,
    depth       INT NOT NULL DEFAULT 0
);

CREATE TABLE coupon (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    discount_type    VARCHAR(10) NOT NULL CHECK (discount_type IN ('RATE', 'FIXED')),
    discount_value   INT NOT NULL,
    min_order_amount INT NOT NULL DEFAULT 0,
    started_at       TIMESTAMP NOT NULL,
    expired_at       TIMESTAMP NOT NULL,
    max_issue_count  INT NOT NULL,
    issued_count     INT NOT NULL DEFAULT 0
);

-- ============================================
-- Layer 1: Layer 0에 의존
-- ============================================

CREATE TABLE user_address (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    address         VARCHAR(500) NOT NULL,
    detail_address  VARCHAR(500),
    is_default      BOOLEAN NOT NULL DEFAULT false,
    receiver_name   VARCHAR(100) NOT NULL,
    receiver_phone  VARCHAR(20) NOT NULL
);

CREATE TABLE user_coupon (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    coupon_id   BIGINT NOT NULL REFERENCES coupon(id),
    is_used     BOOLEAN NOT NULL DEFAULT false,
    used_at     TIMESTAMP
);

CREATE TABLE product (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES category(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    base_price  INT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'DISCONTINUED')),
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE point_history (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    type          VARCHAR(10) NOT NULL CHECK (type IN ('EARN', 'USE', 'EXPIRE')),
    amount        INT NOT NULL,
    balance_after INT NOT NULL,
    description   VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Layer 2: Layer 1에 의존
-- ============================================

CREATE TABLE product_option (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    option_name VARCHAR(100) NOT NULL
);

CREATE TABLE product_image (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    image_url   VARCHAR(500) NOT NULL,
    is_main     BOOLEAN NOT NULL DEFAULT false,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE product_sku (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    sku_code        VARCHAR(100) NOT NULL UNIQUE,
    stock_quantity  INT NOT NULL DEFAULT 0,
    extra_price     INT NOT NULL DEFAULT 0
);

CREATE TABLE cart (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Layer 3: Layer 2에 의존
-- ============================================

CREATE TABLE product_option_value (
    id          BIGSERIAL PRIMARY KEY,
    option_id   BIGINT NOT NULL REFERENCES product_option(id),
    value       VARCHAR(100) NOT NULL
);

CREATE TABLE product_sku_option (
    id                BIGSERIAL PRIMARY KEY,
    sku_id            BIGINT NOT NULL REFERENCES product_sku(id),
    option_value_id   BIGINT NOT NULL REFERENCES product_option_value(id)
);

CREATE TABLE cart_item (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT NOT NULL REFERENCES cart(id),
    sku_id      BIGINT NOT NULL REFERENCES product_sku(id),
    quantity    INT NOT NULL DEFAULT 1,
    added_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    address_id      BIGINT NOT NULL REFERENCES user_address(id),
    used_coupon_id  BIGINT REFERENCES user_coupon(id),
    total_price     INT NOT NULL,
    discount_price  INT NOT NULL DEFAULT 0,
    final_price     INT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID','PREPARING','SHIPPED','DELIVERED','CANCELLED')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Layer 4: Layer 3에 의존
-- ============================================

CREATE TABLE order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(id),
    sku_id        BIGINT NOT NULL REFERENCES product_sku(id),
    product_name  VARCHAR(255) NOT NULL,
    option_info   VARCHAR(500),
    quantity      INT NOT NULL,
    unit_price    INT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE payment (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES orders(id),
    method              VARCHAR(20) NOT NULL CHECK (method IN ('CARD', 'KAKAO_PAY', 'NAVER_PAY')),
    amount              INT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
    pg_transaction_id   VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at             TIMESTAMP
);

CREATE TABLE delivery (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT NOT NULL REFERENCES orders(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'PREPARING' CHECK (status IN ('PREPARING','SHIPPED','DELIVERING','DELIVERED')),
    tracking_number  VARCHAR(100),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Layer 5: Layer 4에 의존
-- ============================================

CREATE TABLE refund (
    id              BIGSERIAL PRIMARY KEY,
    payment_id      BIGINT NOT NULL REFERENCES payment(id),
    order_item_id   BIGINT NOT NULL REFERENCES order_item(id),
    amount          INT NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE delivery_tracking (
    id           BIGSERIAL PRIMARY KEY,
    delivery_id  BIGINT NOT NULL REFERENCES delivery(id),
    status       VARCHAR(20) NOT NULL,
    location     VARCHAR(255),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE review (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    product_id    BIGINT NOT NULL REFERENCES product(id),
    order_item_id BIGINT NOT NULL REFERENCES order_item(id),
    rating        INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content       TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Layer 6: Layer 5에 의존
-- ============================================

CREATE TABLE review_image (
    id          BIGSERIAL PRIMARY KEY,
    review_id   BIGINT NOT NULL REFERENCES review(id),
    image_url   VARCHAR(500) NOT NULL
);

CREATE TABLE review_like (
    id          BIGSERIAL PRIMARY KEY,
    review_id   BIGINT NOT NULL REFERENCES review(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (review_id, user_id)
);

-- ============================================
-- CHECK 제약 (Phase 4 격리 수준 실험용)
-- ============================================
ALTER TABLE product_sku ADD CONSTRAINT chk_stock_non_negative CHECK (stock_quantity >= 0);
