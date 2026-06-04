# Support

Trước khi tạo issue, vui lòng kiểm tra nhanh các bước sau.

## Checklist chung

1. Server đang chạy Java 17+.
2. File jar đúng version `KoraPayments-1.3.1.jar`.
3. `/kora-admin status` không báo provider sai.
4. `config.yml` không bị lỗi YAML.
5. Token/API key provider chưa hết hạn.
6. Nếu dùng PlaceholderAPI, plugin này đã được cài và enable.

## Khi báo lỗi nạp bank

Cung cấp:

- Provider: `sepay` hoặc `payos`.
- Số tiền test.
- Nội dung chuyển khoản đã tạo.
- Log liên quan trong console hoặc `transactions.log`.
- Ảnh cấu hình đã che token và số tài khoản nếu cần.

## Khi báo lỗi nạp thẻ

Cung cấp:

- Provider: `card2k` hoặc `gachthefast`.
- Nhà mạng/mệnh giá.
- Log lỗi đã che partner key.
- Trạng thái trả về từ provider nếu có.

## Khi báo lỗi Discord Store

Cung cấp:

- `/taokenhbanhang status`.
- Bot có online không.
- Channel ID đã đúng chưa.
- Log console khi bot khởi động.
- Cấu hình sản phẩm liên quan đã che token.
