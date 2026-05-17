# Ecommerce DB Optimization Lab

This context describes a learning lab that uses an ecommerce domain model to reproduce, measure, and explain database performance and operations problems.

## Language

**Ecommerce DB Optimization Lab**:
A learning project where ecommerce workflows provide realistic data relationships for database optimization experiments.
_Avoid_: Production ecommerce platform, benchmark suite

**Ecommerce Model**:
The sample commerce domain used as the experimental subject for users, products, orders, payments, benefits, delivery, and reviews.
_Avoid_: Real storefront, production domain

**Learning Phase**:
A roadmap step that introduces one database problem, measures it, applies one family of solutions, and records the result.
_Avoid_: Sprint, release

**Baseline**:
The measured behavior of the naive implementation before the relevant optimization is applied.
_Avoid_: Target result, final performance

**Naive Implementation**:
An intentionally unoptimized implementation used to make a database problem visible and measurable.
_Avoid_: Bad implementation, legacy implementation

**Evidence**:
Recorded proof that shows the observed behavior or before/after difference for a learning phase.
_Avoid_: Note, rough result

**Phase Evidence**:
Evidence recorded for a specific learning phase.
_Avoid_: Screenshot dump, temporary result

**Pre-change Evidence**:
Evidence captured before applying the optimization, operational pattern, or mitigation being studied.
_Avoid_: Old result

**Post-change Evidence**:
Evidence captured after applying the optimization, operational pattern, or mitigation being studied.
_Avoid_: New result

**Optimization Target**:
The specific query path, data relationship, or operational behavior being measured and improved in a learning phase.
_Avoid_: Feature, task

**User**:
A person represented in the ecommerce model who can have addresses, carts, orders, coupons, points, and reviews.
_Avoid_: Customer, member, account

**Product**:
An item displayed and searched in the ecommerce model.
_Avoid_: SKU, item

**Product Option**:
A configurable attribute type for a product, such as color or size.
_Avoid_: Variant

**Product Option Value**:
A concrete value for a product option, such as red, blue, M, or L.
_Avoid_: Variant value

**Product SKU**:
A purchasable product variant formed from option values, with its own stock quantity and price adjustment.
_Avoid_: Product, item, option

**Order**:
A purchase record created by a user that groups one or more purchased product SKUs.
_Avoid_: Purchase, transaction

**Order Item**:
A line in an order that represents the purchase of a specific product SKU and quantity.
_Avoid_: Product, item

**Order Item Snapshot**:
The product name, option description, and price values captured on an order item at the time of ordering.
_Avoid_: Live product data

**Payment**:
A monetary processing record for an order, including method, amount, provider transaction identity, and payment timing.
_Avoid_: Order, transaction

**Refund**:
A record of money returned for a payment or an order item.
_Avoid_: Cancellation

**Coupon**:
A benefit definition that describes discount rules, validity period, and issue limits.
_Avoid_: User coupon, discount instance

**User Coupon**:
A coupon issued to a specific user, including whether and when it was used.
_Avoid_: Coupon

**Point History**:
A chronological record of point changes for a user.
_Avoid_: Point balance

**Delivery**:
The shipping record for an order.
_Avoid_: Shipment

**Delivery Tracking**:
A chronological delivery-status history for a delivery.
_Avoid_: Delivery

**Review**:
A rating and written evaluation that a user leaves for a purchased product.
_Avoid_: Comment

**Review Like**:
A user's positive reaction to a review.
_Avoid_: Like count

## Relationships

- The **Ecommerce DB Optimization Lab** uses the **Ecommerce Model** as its experimental subject.
- A **Learning Phase** starts from a **Baseline** and focuses on one or more **Optimization Targets**.
- A **Naive Implementation** exists to produce a measurable **Baseline**.
- **Evidence** records whether a **Learning Phase** demonstrated the intended before/after behavior.
- **Phase Evidence** belongs to one **Learning Phase**.
- **Pre-change Evidence** and **Post-change Evidence** should be comparable when a **Learning Phase** applies an optimization or mitigation.
- A **User** can have addresses, carts, orders, coupons, points, and reviews.
- A **Product** has one or more **Product Options**.
- A **Product Option** has one or more **Product Option Values**.
- A **Product SKU** belongs to one **Product** and represents one combination of **Product Option Values**.
- Product search optimization usually targets **Product**, while stock and order consistency usually target **Product SKU**.
- A **User** can create one or more **Orders**.
- An **Order** contains one or more **Order Items**.
- An **Order Item** references one **Product SKU** and stores an **Order Item Snapshot**.
- An **Order Item Snapshot** remains stable even if the referenced **Product** or **Product SKU** changes later.
- An **Order** can have one or more **Payments**.
- A **Refund** belongs to one **Payment** and may refer to one **Order Item**.
- A **Coupon** can be issued as many **User Coupons** up to its issue limit.
- A **User Coupon** belongs to one **User** and one **Coupon**.
- **Point History** records changes for one **User** and is not the same thing as the user's current point balance.
- An **Order** has one **Delivery**.
- A **Delivery** has many **Delivery Tracking** records over time.
- A **Review** belongs to one **User**, one **Product**, and one **Order Item**.
- A **Review Like** belongs to one **Review** and one **User**.
- **Delivery Tracking** is a pagination optimization target, while **Review** is an aggregation optimization target.

## Example dialogue

> **Dev:** "Is Phase 2 a product-search feature?"
> **Domain expert:** "No. Product search is the **Optimization Target**. The **Learning Phase** is about proving how indexes change the **Baseline** behavior."

## Flagged ambiguities

- "이커머스 프로젝트" can mean a production ecommerce service or this learning lab. Resolved: this repo is an **Ecommerce DB Optimization Lab**, not a production ecommerce platform.
- "customer", "member", and "account" all refer to **User** unless a future bounded context explicitly separates them.
