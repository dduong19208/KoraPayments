# Nâng cấp KoraPayments 1.4.2 lên 1.4.3

## Trước khi nâng cấp

1. Dừng hoàn toàn server.
2. Backup thư mục `plugins/KoraPayments/` và database đang sử dụng.
3. Thay JAR cũ bằng `KoraPayments-1.4.3.jar`.
4. Khởi động server và kiểm tra `/kora-admin status`.

## Cấu hình Discord Support

Server cũ không bắt buộc phải chỉnh `config.yml`: nếu thiếu key mới, plugin tự dùng
`https://dsc.gg/korapayments` mà không ghi đè file hiện tại.

Để tùy chỉnh link hiển thị, thêm:

```yaml
support:
  discord-url: "https://dsc.gg/korapayments"
```

Thông tin tác giả vẫn là `Discord: lz.dy.dg` và được hiển thị riêng với link hỗ trợ.

## Dữ liệu và tính năng cũ

Bản cập nhật không thay đổi schema database, claim, provider, milestone hoặc luồng
thanh toán. Không cần migrate dữ liệu.
