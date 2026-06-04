# KoraPayments v1.3.1

Bản phát hành này tập trung vào KoraPayments cho server Minecraft Paper/Spigot/Folia với các tính năng nạp ngân hàng, nạp thẻ, mốc nạp, khuyến mãi, webhook và Discord Auto Buy.

## Download

Tải file trong phần **Assets**:

```text
KoraPayments-1.3.1.jar
```

Không tải source code nếu bạn chỉ muốn cài plugin vào server.

## Yêu cầu

- Java 17 trở lên.
- Paper/Spigot 1.21.x.
- PlaceholderAPI là tùy chọn.
- Discord bot chỉ cần khi bật Discord Auto Buy.

## Tính năng chính

- Nạp ngân hàng qua SePay hoặc PayOS.
- Tạo QR/nội dung chuyển khoản cho người chơi.
- Nạp thẻ qua Card2K hoặc GachTheFast.
- Chiết khấu và khuyến mãi riêng cho bank/thẻ.
- Mốc nạp cá nhân và mốc nạp toàn server.
- Bossbar tiến độ mốc nạp server.
- Top nạp, lịch sử nạp, nạp thủ công cho admin.
- Discord webhook thông báo giao dịch.
- Discord Auto Buy/KoraStore với embed sản phẩm, order, log và feedback.
- PlaceholderAPI với identifier `%kp_*%` và alias `%korapayments_*%`.
- Hỗ trợ nhiều ngôn ngữ.

## Nâng cấp từ bản cũ

1. Dừng server.
2. Backup thư mục `plugins/KoraPayments/`.
3. Thay file jar cũ bằng `KoraPayments-1.3.1.jar`.
4. Khởi động server.
5. So sánh `config.yml`/`store.yml` mới với file cũ nếu plugin tạo thêm key.
6. Chạy:

```text
/kora-admin reload
/kora-admin status
```

## Lưu ý bảo mật

Không chia sẻ hoặc commit token/API key thật lên repo public, bao gồm token SePay, PayOS, Card2K, GachTheFast, Discord webhook và Discord bot token.
