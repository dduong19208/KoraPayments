# KoraPayments v1.4.3

## Discord Support

- Thêm link hỗ trợ có thể cấu hình tại `support.discord-url`.
- Hiển thị `Discord Support: https://dsc.gg/korapayments` trong banner console và các lệnh thông tin.
- Bổ sung badge, liên kết góp ý, tài liệu hỗ trợ và contact link trong GitHub Issues.
- Giữ nguyên thông tin tác giả: `Author Discord: lz.dy.dg`.

## README và tài liệu

- Làm mới README theo bố cục sạch, hiện đại và dễ tra cứu.
- Đồng bộ hướng dẫn cấu hình, command, PlaceholderAPI, Discord Store, bảo mật và hỗ trợ.

## Tương thích và dữ liệu

- Java 21+, Bukkit/Spigot/Paper/Folia/Leaf 1.21.x.
- Không thay đổi schema database, claim, provider hoặc luồng thanh toán hiện có.
- Server cũ chưa có `support.discord-url` vẫn tự dùng link mặc định mà không bị ghi đè cấu hình.

## Xác minh

- `mvn clean package`: PASS.
- Regression tests: 2/2 PASS.
- SHA-256: `46F6BA180BCEA125CC5695B625764835E0938E2BF0B49749B6798E3F0A919D3B`.

## Hỗ trợ và góp ý

**Discord Support:** [https://dsc.gg/korapayments](https://dsc.gg/korapayments)

**Author Discord:** `lz.dy.dg`
