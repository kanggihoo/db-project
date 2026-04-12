package com.dblab.ecommerce.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JDBC Batch Insert 전용 리포지토리
 *
 * JPA save()는 건당 INSERT + 영속성 컨텍스트 관리 오버헤드가 크다.
 * JdbcTemplate.batchUpdate()로 JPA를 우회하여 대량 삽입 성능을 극대화한다.
 *
 * application-seeder.yaml의 reWriteBatchedInserts=true와 함께 사용 시
 * 여러 INSERT가 하나의 네트워크 패킷으로 묶여 성능이 더욱 향상된다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BulkInsertRepository {

    private final JdbcTemplate jdbcTemplate;

    // ============================================================
    // ID Pre-allocation: PostgreSQL 시퀀스에서 N개 ID를 미리 확보
    // INSERT 시 id 컬럼에 직접 지정 → 다음 Layer에서 참조 가능
    // ============================================================

    /**
     * 지정한 시퀀스에서 count개의 ID를 미리 확보하여 반환한다.
     *
     * @param sequenceName PostgreSQL 시퀀스 이름 (예: users_id_seq)
     * @param count        확보할 ID 수
     * @return 확보된 ID 리스트 (오름차순)
     */
    public List<Long> reserveSequence(String sequenceName, int count) {
        String sql = "SELECT nextval('" + sequenceName + "') FROM generate_series(1, " + count + ")";
        return jdbcTemplate.queryForList(sql, Long.class);
    }

    // ============================================================
    // Layer 0: users, category, coupon
    // ============================================================

    /** users 벌크 삽입 */
    public void bulkInsertUsers(List<Object[]> rows) {
        String sql = "INSERT INTO users (id, email, password, name, phone, gender, birth_date, grade, point_balance, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "users");
    }

    /** category 벌크 삽입 */
    public void bulkInsertCategories(List<Object[]> rows) {
        String sql = "INSERT INTO category (id, parent_id, name, depth) VALUES (?, ?, ?, ?)";
        batchUpdate(sql, rows, "category");
    }

    /** coupon 벌크 삽입 */
    public void bulkInsertCoupons(List<Object[]> rows) {
        String sql = "INSERT INTO coupon (id, name, discount_type, discount_value, min_order_amount, started_at, expired_at, max_issue_count, issued_count) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "coupon");
    }

    // ============================================================
    // Layer 1: user_address, user_coupon, product, point_history
    // ============================================================

    /** user_address 벌크 삽입 */
    public void bulkInsertUserAddresses(List<Object[]> rows) {
        String sql = "INSERT INTO user_address (id, user_id, address, detail_address, is_default, receiver_name, receiver_phone) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "user_address");
    }

    /** user_coupon 벌크 삽입 */
    public void bulkInsertUserCoupons(List<Object[]> rows) {
        String sql = "INSERT INTO user_coupon (id, user_id, coupon_id, is_used, used_at) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "user_coupon");
    }

    /** product 벌크 삽입 */
    public void bulkInsertProducts(List<Object[]> rows) {
        String sql = "INSERT INTO product (id, category_id, name, description, base_price, status, is_deleted, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "product");
    }

    /** point_history 벌크 삽입 */
    public void bulkInsertPointHistories(List<Object[]> rows) {
        String sql = "INSERT INTO point_history (id, user_id, type, amount, balance_after, description, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "point_history");
    }

    // ============================================================
    // Layer 2: product_option, product_image, product_sku, cart
    // ============================================================

    /** product_option 벌크 삽입 */
    public void bulkInsertProductOptions(List<Object[]> rows) {
        String sql = "INSERT INTO product_option (id, product_id, option_name) VALUES (?, ?, ?)";
        batchUpdate(sql, rows, "product_option");
    }

    /** product_image 벌크 삽입 */
    public void bulkInsertProductImages(List<Object[]> rows) {
        String sql = "INSERT INTO product_image (id, product_id, image_url, is_main, sort_order) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "product_image");
    }

    /** product_sku 벌크 삽입 */
    public void bulkInsertProductSkus(List<Object[]> rows) {
        String sql = "INSERT INTO product_sku (id, product_id, sku_code, stock_quantity, extra_price) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "product_sku");
    }

    /** cart 벌크 삽입 */
    public void bulkInsertCarts(List<Object[]> rows) {
        String sql = "INSERT INTO cart (id, user_id, created_at) VALUES (?, ?, ?)";
        batchUpdate(sql, rows, "cart");
    }

    // ============================================================
    // Layer 3: product_option_value, product_sku_option, cart_item, orders
    // ============================================================

    /** product_option_value 벌크 삽입 */
    public void bulkInsertProductOptionValues(List<Object[]> rows) {
        String sql = "INSERT INTO product_option_value (id, option_id, value) VALUES (?, ?, ?)";
        batchUpdate(sql, rows, "product_option_value");
    }

    /** product_sku_option 벌크 삽입 */
    public void bulkInsertProductSkuOptions(List<Object[]> rows) {
        String sql = "INSERT INTO product_sku_option (id, sku_id, option_value_id) VALUES (?, ?, ?)";
        batchUpdate(sql, rows, "product_sku_option");
    }

    /** cart_item 벌크 삽입 */
    public void bulkInsertCartItems(List<Object[]> rows) {
        String sql = "INSERT INTO cart_item (id, cart_id, sku_id, quantity, added_at) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "cart_item");
    }

    /** orders 벌크 삽입 */
    public void bulkInsertOrders(List<Object[]> rows) {
        String sql = "INSERT INTO orders (id, user_id, address_id, used_coupon_id, total_price, discount_price, final_price, status, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "orders");
    }

    // ============================================================
    // Layer 4: order_item, payment, delivery
    // ============================================================

    /** order_item 벌크 삽입 */
    public void bulkInsertOrderItems(List<Object[]> rows) {
        String sql = "INSERT INTO order_item (id, order_id, sku_id, product_name, option_info, quantity, unit_price, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "order_item");
    }

    /** payment 벌크 삽입 */
    public void bulkInsertPayments(List<Object[]> rows) {
        String sql = "INSERT INTO payment (id, order_id, method, amount, status, pg_transaction_id, created_at, paid_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "payment");
    }

    /** delivery 벌크 삽입 */
    public void bulkInsertDeliveries(List<Object[]> rows) {
        String sql = "INSERT INTO delivery (id, order_id, status, tracking_number, created_at) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "delivery");
    }

    // ============================================================
    // Layer 5: refund, delivery_tracking, review
    // ============================================================

    /** refund 벌크 삽입 */
    public void bulkInsertRefunds(List<Object[]> rows) {
        String sql = "INSERT INTO refund (id, payment_id, order_item_id, amount, reason, status, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "refund");
    }

    /** delivery_tracking 벌크 삽입 */
    public void bulkInsertDeliveryTrackings(List<Object[]> rows) {
        String sql = "INSERT INTO delivery_tracking (id, delivery_id, status, location, created_at) VALUES (?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "delivery_tracking");
    }

    /** review 벌크 삽입 */
    public void bulkInsertReviews(List<Object[]> rows) {
        String sql = "INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        batchUpdate(sql, rows, "review");
    }

    // ============================================================
    // Layer 6: review_image, review_like
    // ============================================================

    /** review_image 벌크 삽입 */
    public void bulkInsertReviewImages(List<Object[]> rows) {
        String sql = "INSERT INTO review_image (id, review_id, image_url) VALUES (?, ?, ?)";
        batchUpdate(sql, rows, "review_image");
    }

    /** review_like 벌크 삽입 */
    public void bulkInsertReviewLikes(List<Object[]> rows) {
        String sql = "INSERT INTO review_like (id, review_id, user_id, created_at) VALUES (?, ?, ?, ?)";
        batchUpdate(sql, rows, "review_like");
    }

    // ============================================================
    // 공통 배치 업데이트 헬퍼
    // ============================================================

    /**
     * List<Object[]> 형태의 행 데이터를 청크 단위로 분할하여 배치 삽입한다.
     *
     * @param sql       INSERT SQL
     * @param rows      삽입할 행 데이터 리스트
     * @param tableName 로깅용 테이블 이름
     */
    private void batchUpdate(String sql, List<Object[]> rows, String tableName) {
        int chunkSize = 5000;
        int total = rows.size();
        int inserted = 0;

        for (int i = 0; i < total; i += chunkSize) {
            List<Object[]> chunk = rows.subList(i, Math.min(i + chunkSize, total));
            jdbcTemplate.batchUpdate(sql, chunk);
            inserted += chunk.size();
            log.debug("[{}] {}/{} 건 삽입 완료", tableName, inserted, total);
        }

        log.info("[{}] 총 {} 건 삽입 완료", tableName, total);
    }
}
