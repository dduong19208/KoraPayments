# KoraPayments 1.4.0 - QA Report

Ngày kiểm tra gần nhất: 2026-08-01

## Phạm vi thay đổi

- Giữ toàn bộ command, permission và tài nguyên của bản 1.3.1.
- Sửa lifecycle shutdown JDA/OkHttp/executor để xử lý lỗi `IllegalStateException: zip file closed`.
- Tách cấu hình thành các tệp theo chức năng, có migration từ `config.yml` cũ.
- Thêm backend PostgreSQL; chuẩn hóa cấu hình SQLite/MySQL/MariaDB qua HikariCP.
- Thêm economy bridge tập trung: command, auto, Vault, PlayerPoints, FancyEco/FancyEconomy.
- Thêm nhiều reward command, provider selector tập trung và GUI config phiên bản 2.
- Thêm Dialog UI tùy chọn cho Paper/Leaf và fallback inventory/chat.
- Tập trung command/GUI/Dialog nạp thẻ vào `CardFlowManager`.
- Che serial/mã thẻ ở bước xác nhận theo mặc định.

## Kết quả kiểm tra tự động

- YAML parse: PASS - 30 tệp.
- POM XML parse: PASS.
- Bảo toàn config 1.3.1: PASS - 127/127 đường dẫn cũ còn tồn tại.
- Thay đổi default có chủ đích: `database.remote.port` từ `3306` thành `0`; runtime tự chọn 3306 hoặc 5432 theo backend.
- Command: PASS - 10/10 command cũ được giữ.
- Permission: PASS - 2/2 permission cũ được giữ.
- Vietnamese translation keys: PASS - 275 key literal, 0 key thiếu.
- Bundled resource references: PASS.
- Hard Paper API imports: PASS - 0; Dialog bridge dùng reflection nên không khóa plugin vào Paper API.
- Tracked source deletions: PASS - 0.
- Default credential fields: PASS - chỉ để trống hoặc placeholder.
- `git diff --check`: PASS.
- Card provider normalization tests: PASS.
- SQLite schema/index/leaderboard/claim/upsert tests: PASS.
- Java parser/structure pass với classpath rỗng: PASS - 0 lỗi cú pháp/cấu trúc.
- Source ZIP integrity: PASS.
- Patch apply trên baseline 1.3.1: PASS.
- Cây tệp sau khi áp dụng patch khớp Source ZIP: PASS - 80 tệp.

## Giới hạn xác minh trong môi trường hiện tại

Kiểm tra bổ sung ngày 2026-08-01:

- `mvn clean package`: PASS, biên dịch 44 source Java 21 và tạo shaded JAR.
- Maven Surefire: PASS, project hiện không có test source riêng.
- Bukkit `YamlConfiguration` parse: PASS, 30/30 tệp YAML.
- `gui.yml` version 2, `economy.reward-commands` và ba provider selector: PASS.
- `config.yml`, `payments.yml`, `gui.yml` trong JAR khớp byte với source: PASS.
- Local staging JAR SHA-256: `8A72FA1989462ECEE160CD8BD795D492FFDCCBBBF1ADA2807E10AF7B6B12671E`.

Chưa thực hiện integration test trực tiếp trên:

- Paper/Leaf/Folia server thật;
- MySQL server thật;
- MariaDB server thật;
- PostgreSQL server thật;
- provider thanh toán production/sandbox thật;
- FancyEco/FancyEconomy bản cụ thể của máy chủ.

Project đã có GitHub Actions tại `.github/workflows/build.yml`. Trên máy có JDK 21 và Maven 3.9+, có thể kiểm tra lại bằng:

```bash
mvn clean verify
```

JAR dự kiến:

```text
target/KoraPayments-1.4.0.jar
```

## Ghi chú EzPay

EzPay JAR chỉ được dùng để tham khảo hành vi giao diện và chữ ký API runtime. KoraPayments sử dụng kiến trúc và mã nguồn triển khai riêng; không sao chép trực tiếp class hoặc tài nguyên của EzPay.
