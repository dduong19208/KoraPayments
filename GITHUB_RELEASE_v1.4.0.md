# KoraPayments v1.4.0

Bản cập nhật lớn tập trung vào tương thích server, database, an toàn lifecycle,
giao diện và khả năng tùy chỉnh cho developer.

## Nổi bật

- Hỗ trợ Bukkit/Spigot/Paper/Folia/Leaf 1.21.x và Java 21+.
- Thêm PostgreSQL; tiếp tục hỗ trợ SQLite, MySQL và MariaDB qua HikariCP.
- Chọn bank/card/economy provider tại một khu vực `providers` trong `config.yml`.
- Hỗ trợ nhiều `economy.reward-commands`, chạy tuần tự với placeholder đầy đủ.
- `gui.yml` phiên bản 2: tùy chỉnh size, title, slot, material, name, lore,
  action, glow, custom model data, flags và bật/tắt từng item.
- Dialog UI tùy chọn trên Paper/Leaf; tự fallback về inventory/chat.
- Discord có slash command `/taokenhbanhang` để đăng storefront.
- Sửa shutdown JDA/OkHttp gây `IllegalStateException: zip file closed`.
- Tách cấu hình theo chức năng và tự migrate cấu hình 1.3.1 mà không xóa key cũ.

## Nâng cấp

1. Dừng hoàn toàn máy chủ; không hot reload.
2. Backup thư mục `plugins/KoraPayments/` và database.
3. Thay JAR cũ bằng `KoraPayments-1.4.0.jar`.
4. Khởi động một lần để plugin tạo/migrate cấu hình.
5. Kiểm tra `config.yml`, `database.yml`, `payments.yml` và `providers/`.

Xem hướng dẫn chi tiết tại [`UPGRADE-1.4.0.md`](UPGRADE-1.4.0.md).

## Kiểm tra build

- Maven clean package: PASS.
- Java 21, 44 source files: PASS.
- 30/30 YAML resources: PASS.
- Shaded JAR chứa driver MySQL, MariaDB và PostgreSQL: PASS.

> Không chia sẻ token/API key thật. Hãy kiểm thử provider trên staging trước khi
> triển khai production.
