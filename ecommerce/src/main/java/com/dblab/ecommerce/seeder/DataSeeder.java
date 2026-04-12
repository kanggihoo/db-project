package com.dblab.ecommerce.seeder;

import com.dblab.ecommerce.repository.BulkInsertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 이커머스 더미 데이터 시더
 *
 * 실행 방법: --spring.profiles.active=seeder
 * 이미 데이터가 있으면 자동으로 건너뜀 (멱등성 보장)
 *
 * 데이터 생성 순서: Layer 0 → Layer 1 → ... → Layer 6
 * FK 의존성을 위해 상위 Layer의 ID를 하위 Layer에서 참조한다.
 *
 * 현재 설정: 소량 테스트 모드 (각 테이블 소수 건)
 * 실제 대량 삽입 시 각 SEED_COUNT 상수를 변경하면 된다.
 */
@Slf4j
@Component
@Profile("seeder")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BulkInsertRepository bulkRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext context;

    // ============================================================
    // 시딩 건수 설정 (소량 테스트 모드)
    // 실제 대량 데이터가 필요할 때 아래 상수를 변경한다.
    // ============================================================
    private static final int USER_COUNT              = 100;
    private static final int CATEGORY_COUNT          = 20;
    private static final int COUPON_COUNT            = 10;
    private static final int PRODUCT_COUNT           = 200;
    private static final int ORDER_COUNT             = 500;
    private static final int POINT_HISTORY_COUNT     = 1000;
    private static final int DELIVERY_TRACKING_COUNT = 2000;

    // Layer 간 ID 공유를 위한 인스턴스 변수
    private List<Long> userIds;
    private List<Long> categoryIds;
    private List<Long> couponIds;
    private List<Long> addressIds;
    private List<Long> userCouponIds;
    private List<Long> productIds;
    private List<Long> productOptionIds;
    private List<Long> optionValueIds;
    private List<Long> skuIds;
    private List<Long> cartIds;
    private List<Long> orderIds;
    private List<Long> orderItemIds;
    private List<Long> paymentIds;
    private List<Long> deliveryIds;

    private final Faker faker = new Faker(Locale.KOREA);
    private final Random random = new Random(42); // 재현 가능한 랜덤

    @Override
    public void run(String... args) {
        if (isAlreadySeeded()) {
            log.info("데이터가 이미 존재합니다. 시딩을 건너뜁니다.");
            SpringApplication.exit(context, () -> 0);
            return;
        }

        log.info("===== 데이터 시딩 시작 =====");
        long startTime = System.currentTimeMillis();

        seedLayer0();
        seedLayer1();
        seedLayer2();
        seedLayer3();
        seedLayer4();
        seedLayer5();
        seedLayer6();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("===== 데이터 시딩 완료 ({} ms) =====", elapsed);

        SpringApplication.exit(context, () -> 0);
    }

    /** users 테이블 건수로 이미 시딩 여부를 판단한다 */
    private boolean isAlreadySeeded() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count != null && count > 0;
    }

    // ============================================================
    // Layer 0: users, category, coupon
    // ============================================================

    private void seedLayer0() {
        log.info("[Layer 0] 시작 — users, category, coupon");

        // users: BRONZE(60%), SILVER(25%), GOLD(10%), VIP(5%)
        userIds = bulkRepo.reserveSequence("users_id_seq", USER_COUNT);
        List<Object[]> userRows = new ArrayList<>(USER_COUNT);
        String[] grades = buildWeightedArray(
            new String[]{"BRONZE", "SILVER", "GOLD", "VIP"},
            new int[]{60, 25, 10, 5}
        );
        String[] genders = buildWeightedArray(
            new String[]{"MALE", "FEMALE", "OTHER"},
            new int[]{48, 48, 4}
        );

        LocalDateTime baseCreatedAt = LocalDateTime.now().minusYears(3);

        for (int i = 0; i < USER_COUNT; i++) {
            LocalDateTime createdAt = baseCreatedAt.plusSeconds(
                random.nextLong(0, ChronoUnit.SECONDS.between(baseCreatedAt, LocalDateTime.now()))
            );
            userRows.add(new Object[]{
                userIds.get(i),
                "user" + userIds.get(i) + "@example.com",
                "hashed_password_" + i,
                faker.name().fullName(),
                faker.phoneNumber().cellPhone(),
                genders[random.nextInt(genders.length)],
                java.sql.Date.valueOf(LocalDate.now().minusYears(20 + random.nextInt(40))),
                grades[random.nextInt(grades.length)],
                random.nextInt(100000),
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt)
            });
        }
        bulkRepo.bulkInsertUsers(userRows);

        // category: depth 0 (대분류 5개), depth 1 (중분류), depth 2 (소분류)
        categoryIds = bulkRepo.reserveSequence("category_id_seq", CATEGORY_COUNT);
        List<Object[]> categoryRows = new ArrayList<>(CATEGORY_COUNT);
        String[] topCategories = {"전자제품", "패션", "식품", "스포츠", "뷰티"};
        // 대분류 5개
        for (int i = 0; i < 5 && i < CATEGORY_COUNT; i++) {
            categoryRows.add(new Object[]{categoryIds.get(i), null, topCategories[i], 0});
        }
        // 중/소분류 나머지
        for (int i = 5; i < CATEGORY_COUNT; i++) {
            Long parentId = categoryIds.get(random.nextInt(Math.min(i, 5)));
            categoryRows.add(new Object[]{categoryIds.get(i), parentId, faker.commerce().department(), 1});
        }
        bulkRepo.bulkInsertCategories(categoryRows);

        // coupon
        couponIds = bulkRepo.reserveSequence("coupon_id_seq", COUPON_COUNT);
        List<Object[]> couponRows = new ArrayList<>(COUPON_COUNT);
        for (int i = 0; i < COUPON_COUNT; i++) {
            LocalDateTime startedAt = LocalDateTime.now().minusDays(30);
            LocalDateTime expiredAt = LocalDateTime.now().plusDays(60);
            String discountType = random.nextBoolean() ? "RATE" : "FIXED";
            int discountValue = discountType.equals("RATE") ? (random.nextInt(4) + 1) * 10 : (random.nextInt(5) + 1) * 1000;
            couponRows.add(new Object[]{
                couponIds.get(i),
                faker.commerce().promotionCode(),
                discountType,
                discountValue,
                random.nextInt(3) * 10000,
                Timestamp.valueOf(startedAt),
                Timestamp.valueOf(expiredAt),
                1000,
                random.nextInt(800)
            });
        }
        bulkRepo.bulkInsertCoupons(couponRows);

        log.info("[Layer 0] 완료");
    }

    // ============================================================
    // Layer 1: user_address, user_coupon, product, point_history
    // ============================================================

    private void seedLayer1() {
        log.info("[Layer 1] 시작 — user_address, user_coupon, product, point_history");

        // user_address: 회원당 1~2개
        int addressCount = (int)(USER_COUNT * 1.5);
        addressIds = bulkRepo.reserveSequence("user_address_id_seq", addressCount);
        List<Object[]> addressRows = new ArrayList<>(addressCount);
        for (int i = 0; i < addressCount; i++) {
            addressRows.add(new Object[]{
                addressIds.get(i),
                userIds.get(i % USER_COUNT),
                faker.address().streetAddress(),
                faker.address().secondaryAddress(),
                i % USER_COUNT == 0, // 첫 번째 주소는 기본 주소
                faker.name().fullName(),
                faker.phoneNumber().cellPhone()
            });
        }
        bulkRepo.bulkInsertUserAddresses(addressRows);

        // user_coupon: 회원의 60%에게 쿠폰 1장씩 발급
        int couponUserCount = (int)(USER_COUNT * 0.6);
        userCouponIds = bulkRepo.reserveSequence("user_coupon_id_seq", couponUserCount);
        List<Object[]> userCouponRows = new ArrayList<>(couponUserCount);
        List<Long> shuffledUserIds = new ArrayList<>(userIds);
        Collections.shuffle(shuffledUserIds, random);
        for (int i = 0; i < couponUserCount; i++) {
            boolean isUsed = random.nextInt(10) < 3; // 30% 사용됨
            LocalDateTime usedAt = isUsed ? LocalDateTime.now().minusDays(random.nextInt(30)) : null;
            userCouponRows.add(new Object[]{
                userCouponIds.get(i),
                shuffledUserIds.get(i),
                couponIds.get(random.nextInt(COUPON_COUNT)),
                isUsed,
                usedAt != null ? Timestamp.valueOf(usedAt) : null
            });
        }
        bulkRepo.bulkInsertUserCoupons(userCouponRows);

        // product: ON_SALE(80%), SOLD_OUT(15%), DISCONTINUED(5%)
        productIds = bulkRepo.reserveSequence("product_id_seq", PRODUCT_COUNT);
        List<Object[]> productRows = new ArrayList<>(PRODUCT_COUNT);
        String[] productStatuses = buildWeightedArray(
            new String[]{"ON_SALE", "SOLD_OUT", "DISCONTINUED"},
            new int[]{80, 15, 5}
        );
        LocalDateTime productBase = LocalDateTime.now().minusYears(2);
        for (int i = 0; i < PRODUCT_COUNT; i++) {
            LocalDateTime createdAt = productBase.plusSeconds(
                random.nextLong(0, ChronoUnit.SECONDS.between(productBase, LocalDateTime.now()))
            );
            boolean isDeleted = random.nextInt(100) < 5; // 5%가 삭제됨
            productRows.add(new Object[]{
                productIds.get(i),
                categoryIds.get(random.nextInt(CATEGORY_COUNT)),
                faker.commerce().productName(),
                faker.lorem().sentence(10),
                (random.nextInt(10) + 1) * 10000, // 10,000 ~ 100,000원
                productStatuses[random.nextInt(productStatuses.length)],
                isDeleted,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt)
            });
        }
        bulkRepo.bulkInsertProducts(productRows);

        // point_history: 최근 1년 데이터
        List<Long> phIds = bulkRepo.reserveSequence("point_history_id_seq", POINT_HISTORY_COUNT);
        List<Object[]> phRows = new ArrayList<>(POINT_HISTORY_COUNT);
        String[] pointTypes = buildWeightedArray(
            new String[]{"EARN", "USE", "EXPIRE"},
            new int[]{60, 35, 5}
        );
        LocalDateTime phBase = LocalDateTime.now().minusYears(1);
        for (int i = 0; i < POINT_HISTORY_COUNT; i++) {
            int amount = (random.nextInt(10) + 1) * 100;
            phRows.add(new Object[]{
                phIds.get(i),
                userIds.get(random.nextInt(USER_COUNT)),
                pointTypes[random.nextInt(pointTypes.length)],
                amount,
                random.nextInt(50000),
                "포인트 " + (i % 3 == 0 ? "적립" : "사용"),
                Timestamp.valueOf(phBase.plusSeconds(random.nextLong(0, ChronoUnit.SECONDS.between(phBase, LocalDateTime.now()))))
            });
        }
        bulkRepo.bulkInsertPointHistories(phRows);

        // categoryIds는 Layer 2에서 사용하지 않으므로 해제
        categoryIds = null;

        log.info("[Layer 1] 완료");
    }

    // ============================================================
    // Layer 2: product_option, product_image, product_sku, cart
    // ============================================================

    private void seedLayer2() {
        log.info("[Layer 2] 시작 — product_option, product_image, product_sku, cart");

        // product_option: 상품당 1~2개 옵션
        int optionCount = (int)(PRODUCT_COUNT * 1.5);
        productOptionIds = bulkRepo.reserveSequence("product_option_id_seq", optionCount);
        List<Object[]> optionRows = new ArrayList<>(optionCount);
        String[] optionNames = {"색상", "사이즈", "용량", "재질"};
        for (int i = 0; i < optionCount; i++) {
            optionRows.add(new Object[]{
                productOptionIds.get(i),
                productIds.get(i % PRODUCT_COUNT),
                optionNames[i % optionNames.length]
            });
        }
        bulkRepo.bulkInsertProductOptions(optionRows);

        // product_image: 상품당 2~3개
        int imageCount = PRODUCT_COUNT * 2;
        List<Long> imageIds = bulkRepo.reserveSequence("product_image_id_seq", imageCount);
        List<Object[]> imageRows = new ArrayList<>(imageCount);
        for (int i = 0; i < imageCount; i++) {
            imageRows.add(new Object[]{
                imageIds.get(i),
                productIds.get(i % PRODUCT_COUNT),
                "https://example.com/images/" + UUID.randomUUID() + ".jpg",
                i % 2 == 0, // 짝수 인덱스가 메인 이미지
                i % 2
            });
        }
        bulkRepo.bulkInsertProductImages(imageRows);

        // product_sku: 상품당 2~4개 SKU
        int skuCount = PRODUCT_COUNT * 3;
        skuIds = bulkRepo.reserveSequence("product_sku_id_seq", skuCount);
        List<Object[]> skuRows = new ArrayList<>(skuCount);
        for (int i = 0; i < skuCount; i++) {
            skuRows.add(new Object[]{
                skuIds.get(i),
                productIds.get(i % PRODUCT_COUNT),
                "SKU-" + skuIds.get(i),
                random.nextInt(500), // 0~499 재고
                random.nextInt(5) * 1000 // 0~4,000원 추가 금액
            });
        }
        bulkRepo.bulkInsertProductSkus(skuRows);

        // cart: 회원의 70%에게 장바구니
        int cartCount = (int)(USER_COUNT * 0.7);
        cartIds = bulkRepo.reserveSequence("cart_id_seq", cartCount);
        List<Object[]> cartRows = new ArrayList<>(cartCount);
        for (int i = 0; i < cartCount; i++) {
            cartRows.add(new Object[]{
                cartIds.get(i),
                userIds.get(i),
                Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(90)))
            });
        }
        bulkRepo.bulkInsertCarts(cartRows);

        log.info("[Layer 2] 완료");
    }

    // ============================================================
    // Layer 3: product_option_value, product_sku_option, cart_item, orders
    // ============================================================

    private void seedLayer3() {
        log.info("[Layer 3] 시작 — product_option_value, product_sku_option, cart_item, orders");

        // product_option_value: 옵션당 3~4개 값
        int ovCount = productOptionIds.size() * 3;
        optionValueIds = bulkRepo.reserveSequence("product_option_value_id_seq", ovCount);
        List<Object[]> ovRows = new ArrayList<>(ovCount);
        String[][] optionValues = {
            {"빨강", "파랑", "초록"},
            {"S", "M", "L"},
            {"250ml", "500ml", "1L"},
            {"면", "폴리에스터", "울"}
        };
        for (int i = 0; i < ovCount; i++) {
            String[] vals = optionValues[i % optionValues.length];
            ovRows.add(new Object[]{
                optionValueIds.get(i),
                productOptionIds.get(i % productOptionIds.size()),
                vals[i % vals.length]
            });
        }
        bulkRepo.bulkInsertProductOptionValues(ovRows);

        // product_sku_option: SKU당 1개 옵션값 연결
        List<Long> psoIds = bulkRepo.reserveSequence("product_sku_option_id_seq", skuIds.size());
        List<Object[]> psoRows = new ArrayList<>(skuIds.size());
        for (int i = 0; i < skuIds.size(); i++) {
            psoRows.add(new Object[]{
                psoIds.get(i),
                skuIds.get(i),
                optionValueIds.get(i % optionValueIds.size())
            });
        }
        bulkRepo.bulkInsertProductSkuOptions(psoRows);

        // cart_item: 장바구니당 1~3개
        int cartItemCount = cartIds.size() * 2;
        List<Long> ciIds = bulkRepo.reserveSequence("cart_item_id_seq", cartItemCount);
        List<Object[]> ciRows = new ArrayList<>(cartItemCount);
        for (int i = 0; i < cartItemCount; i++) {
            ciRows.add(new Object[]{
                ciIds.get(i),
                cartIds.get(i % cartIds.size()),
                skuIds.get(random.nextInt(skuIds.size())),
                random.nextInt(3) + 1,
                Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(30)))
            });
        }
        bulkRepo.bulkInsertCartItems(ciRows);

        // orders: DELIVERED(50%), SHIPPED(15%), PREPARING(10%), PAID(10%), PENDING(10%), CANCELLED(5%)
        orderIds = bulkRepo.reserveSequence("orders_id_seq", ORDER_COUNT);
        List<Object[]> orderRows = new ArrayList<>(ORDER_COUNT);
        String[] orderStatuses = buildWeightedArray(
            new String[]{"DELIVERED", "SHIPPED", "PREPARING", "PAID", "PENDING", "CANCELLED"},
            new int[]{50, 15, 10, 10, 10, 5}
        );
        LocalDateTime orderBase = LocalDateTime.now().minusYears(2);
        for (int i = 0; i < ORDER_COUNT; i++) {
            int totalPrice = (random.nextInt(10) + 1) * 10000;
            // 30%만 쿠폰 사용
            boolean useCoupon = random.nextInt(10) < 3 && !userCouponIds.isEmpty();
            Long usedCouponId = useCoupon ? userCouponIds.get(random.nextInt(userCouponIds.size())) : null;
            int discountPrice = useCoupon ? (int)(totalPrice * 0.1) : 0;
            orderRows.add(new Object[]{
                orderIds.get(i),
                userIds.get(random.nextInt(USER_COUNT)),
                addressIds.get(random.nextInt(addressIds.size())),
                usedCouponId,
                totalPrice,
                discountPrice,
                totalPrice - discountPrice,
                orderStatuses[random.nextInt(orderStatuses.length)],
                Timestamp.valueOf(orderBase.plusSeconds(random.nextLong(0, ChronoUnit.SECONDS.between(orderBase, LocalDateTime.now()))))
            });
        }
        bulkRepo.bulkInsertOrders(orderRows);

        // Layer 3 완료 후 더 이상 필요 없는 리스트 해제
        couponIds = null;
        userCouponIds = null;

        log.info("[Layer 3] 완료");
    }

    // ============================================================
    // Layer 4: order_item, payment, delivery
    // ============================================================

    private void seedLayer4() {
        log.info("[Layer 4] 시작 — order_item, payment, delivery");

        // order_item: 주문당 1~3개
        int oiCount = ORDER_COUNT * 2;
        orderItemIds = bulkRepo.reserveSequence("order_item_id_seq", oiCount);
        List<Object[]> oiRows = new ArrayList<>(oiCount);
        for (int i = 0; i < oiCount; i++) {
            int unitPrice = (random.nextInt(5) + 1) * 10000;
            oiRows.add(new Object[]{
                orderItemIds.get(i),
                orderIds.get(i % ORDER_COUNT),
                skuIds.get(random.nextInt(skuIds.size())),
                faker.commerce().productName(),
                "색상: " + new String[]{"빨강", "파랑", "초록"}[random.nextInt(3)],
                random.nextInt(3) + 1,
                unitPrice,
                "DELIVERED"
            });
        }
        bulkRepo.bulkInsertOrderItems(oiRows);

        // payment: 주문당 1개
        paymentIds = bulkRepo.reserveSequence("payment_id_seq", ORDER_COUNT);
        List<Object[]> paymentRows = new ArrayList<>(ORDER_COUNT);
        String[] paymentMethods = {"CARD", "KAKAO_PAY", "NAVER_PAY"};
        String[] paymentStatuses = buildWeightedArray(
            new String[]{"COMPLETED", "PENDING", "FAILED", "REFUNDED"},
            new int[]{80, 10, 5, 5}
        );
        for (int i = 0; i < ORDER_COUNT; i++) {
            String status = paymentStatuses[random.nextInt(paymentStatuses.length)];
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(365));
            LocalDateTime paidAt = status.equals("COMPLETED") ? createdAt.plusMinutes(random.nextInt(5)) : null;
            paymentRows.add(new Object[]{
                paymentIds.get(i),
                orderIds.get(i),
                paymentMethods[random.nextInt(paymentMethods.length)],
                (random.nextInt(10) + 1) * 10000,
                status,
                "PG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                Timestamp.valueOf(createdAt),
                paidAt != null ? Timestamp.valueOf(paidAt) : null
            });
        }
        bulkRepo.bulkInsertPayments(paymentRows);

        // delivery: 주문당 1개
        deliveryIds = bulkRepo.reserveSequence("delivery_id_seq", ORDER_COUNT);
        List<Object[]> deliveryRows = new ArrayList<>(ORDER_COUNT);
        String[] deliveryStatuses = buildWeightedArray(
            new String[]{"DELIVERED", "SHIPPED", "DELIVERING", "PREPARING"},
            new int[]{50, 20, 15, 15}
        );
        for (int i = 0; i < ORDER_COUNT; i++) {
            deliveryRows.add(new Object[]{
                deliveryIds.get(i),
                orderIds.get(i),
                deliveryStatuses[random.nextInt(deliveryStatuses.length)],
                "TRACK-" + (1000000 + random.nextInt(9000000)),
                Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(365)))
            });
        }
        bulkRepo.bulkInsertDeliveries(deliveryRows);

        // Layer 4 완료 후 해제
        orderIds = null;

        log.info("[Layer 4] 완료");
    }

    // ============================================================
    // Layer 5: refund, delivery_tracking, review
    // ============================================================

    private void seedLayer5() {
        log.info("[Layer 5] 시작 — refund, delivery_tracking, review");

        // refund: 결제의 5%만 환불
        int refundCount = Math.max(1, (int)(paymentIds.size() * 0.05));
        List<Long> refundIds = bulkRepo.reserveSequence("refund_id_seq", refundCount);
        List<Object[]> refundRows = new ArrayList<>(refundCount);
        for (int i = 0; i < refundCount; i++) {
            refundRows.add(new Object[]{
                refundIds.get(i),
                paymentIds.get(i),
                orderItemIds.get(i),
                (random.nextInt(5) + 1) * 5000,
                faker.lorem().sentence(5),
                "COMPLETED",
                Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(30)))
            });
        }
        bulkRepo.bulkInsertRefunds(refundRows);

        // delivery_tracking: 배송당 2~5개 이력
        List<Long> dtIds = bulkRepo.reserveSequence("delivery_tracking_id_seq", DELIVERY_TRACKING_COUNT);
        List<Object[]> dtRows = new ArrayList<>(DELIVERY_TRACKING_COUNT);
        String[] trackingStatuses = {"PREPARING", "SHIPPED", "DELIVERING", "DELIVERED"};
        String[] locations = {"서울 물류센터", "경기 허브", "부산 터미널", "대전 환승센터", "배송 중", "배송 완료"};
        LocalDateTime dtBase = LocalDateTime.now().minusMonths(6);
        for (int i = 0; i < DELIVERY_TRACKING_COUNT; i++) {
            dtRows.add(new Object[]{
                dtIds.get(i),
                deliveryIds.get(i % deliveryIds.size()),
                trackingStatuses[i % trackingStatuses.length],
                locations[random.nextInt(locations.length)],
                Timestamp.valueOf(dtBase.plusSeconds(random.nextLong(0, ChronoUnit.SECONDS.between(dtBase, LocalDateTime.now()))))
            });
        }
        bulkRepo.bulkInsertDeliveryTrackings(dtRows);

        // review: 주문 항목의 30%에 리뷰
        int reviewCount = Math.max(1, (int)(orderItemIds.size() * 0.3));
        List<Long> reviewIds = bulkRepo.reserveSequence("review_id_seq", reviewCount);
        List<Object[]> reviewRows = new ArrayList<>(reviewCount);
        for (int i = 0; i < reviewCount; i++) {
            reviewRows.add(new Object[]{
                reviewIds.get(i),
                userIds.get(random.nextInt(USER_COUNT)),
                productIds.get(random.nextInt(PRODUCT_COUNT)),
                orderItemIds.get(i),
                random.nextInt(5) + 1,
                faker.lorem().sentence(20),
                Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(365)))
            });
        }
        bulkRepo.bulkInsertReviews(reviewRows);

        // 메모리 해제
        paymentIds = null;
        deliveryIds = null;
        orderItemIds = null;

        // reviewIds는 Layer 6에서 필요하므로 저장
        this.reviewIdsForLayer6 = reviewIds;
        this.reviewCountForLayer6 = reviewCount;

        log.info("[Layer 5] 완료");
    }

    // Layer 6에 전달할 임시 변수
    private List<Long> reviewIdsForLayer6;
    private int reviewCountForLayer6;

    // ============================================================
    // Layer 6: review_image, review_like
    // ============================================================

    private void seedLayer6() {
        log.info("[Layer 6] 시작 — review_image, review_like");

        List<Long> reviewIds = reviewIdsForLayer6;
        int reviewCount = reviewCountForLayer6;

        // review_image: 리뷰의 40%에 이미지 1장
        int riCount = Math.max(1, (int)(reviewCount * 0.4));
        List<Long> riIds = bulkRepo.reserveSequence("review_image_id_seq", riCount);
        List<Object[]> riRows = new ArrayList<>(riCount);
        for (int i = 0; i < riCount; i++) {
            riRows.add(new Object[]{
                riIds.get(i),
                reviewIds.get(i),
                "https://example.com/review-images/" + UUID.randomUUID() + ".jpg"
            });
        }
        bulkRepo.bulkInsertReviewImages(riRows);

        // review_like: 리뷰당 0~5명 좋아요
        // (review_id, user_id) UNIQUE 제약을 준수하기 위해 Set으로 중복 방지
        int rlCount = Math.max(1, reviewCount * 2);
        List<Long> rlIds = bulkRepo.reserveSequence("review_like_id_seq", rlCount);
        List<Object[]> rlRows = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        int idIndex = 0;
        for (int i = 0; i < reviewCount && idIndex < rlCount; i++) {
            int likeCount = random.nextInt(3); // 0~2개 좋아요
            for (int j = 0; j < likeCount && idIndex < rlCount; j++) {
                Long rId = reviewIds.get(i);
                Long uId = userIds.get(random.nextInt(USER_COUNT));
                String pair = rId + ":" + uId;
                if (seenPairs.add(pair)) {
                    rlRows.add(new Object[]{
                        rlIds.get(idIndex++),
                        rId,
                        uId,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(180)))
                    });
                }
            }
        }
        bulkRepo.bulkInsertReviewLikes(rlRows);

        // 나머지 메모리 해제
        userIds = null;
        productIds = null;
        productOptionIds = null;
        optionValueIds = null;
        skuIds = null;
        cartIds = null;
        reviewIdsForLayer6 = null;
        addressIds = null;

        log.info("[Layer 6] 완료");
    }

    // ============================================================
    // 유틸: 가중치 기반 배열 생성
    // 예: buildWeightedArray(["A","B"], [60, 40]) → 60개 "A" + 40개 "B"
    // ============================================================
    private String[] buildWeightedArray(String[] values, int[] weights) {
        int total = Arrays.stream(weights).sum();
        String[] result = new String[total];
        int idx = 0;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < weights[i]; j++) {
                result[idx++] = values[i];
            }
        }
        return result;
    }
}
