# Security Policy

KoraPayments xử lý dữ liệu thanh toán, token provider và webhook Discord. Hãy vận hành theo nguyên tắc không public secret.

## Không commit secret

Không đưa các giá trị thật sau lên GitHub:

- `sepay.api-token`
- `payos.client-id`
- `payos.api-key`
- `payos.checksum-key`
- `card2k.api.partner_id`
- `card2k.api.partner_key`
- `gachthefast.api.partner_id`
- `gachthefast.api.partner_key`
- `gachthefast.api.wallet_id`
- `discord-webhook.url`
- `settings.bot-token` trong `store.yml`

## Khi lộ token

1. Tắt hoặc xóa token cũ trên dashboard provider.
2. Tạo token mới.
3. Cập nhật `config.yml`/`store.yml` trên server.
4. Chạy `/kora-admin reload`.
5. Kiểm tra log để chắc chắn provider hoạt động lại.

## Báo cáo lỗ hổng

Vào [Discord Support](https://dsc.gg/korapayments) để yêu cầu kênh liên hệ riêng. Không gửi token, API key hoặc dữ liệu thanh toán trong kênh công khai.

Khi báo cáo lỗi bảo mật, không đăng token, ảnh QR thật hoặc log chứa thông tin tài khoản ngân hàng ở issue public. Hãy che các phần nhạy cảm trước khi gửi.

Thông tin nên cung cấp:

- Version KoraPayments.
- Version server và Java.
- Provider đang dùng.
- Mô tả lỗi.
- Log đã được che token/API key.
