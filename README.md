<p align="center">
  <img src="assets/banner.png" alt="KoraPayments" width="100%">
</p>

<h1 align="center">KoraPayments</h1>

<p align="center">
  <strong>Nền tảng thanh toán và tự động hóa doanh thu cho Minecraft 1.21.x</strong>
</p>

<p align="center">
  Nạp ngân hàng · Nạp thẻ cào · Mốc nạp · Discord Webhook · Discord Auto Buy
</p>

<p align="center">
  <a href="https://github.com/dduong19208/KoraPayments/releases/latest"><img src="https://img.shields.io/github/v/release/dduong19208/KoraPayments?style=flat-square&color=0ea5e9" alt="Latest release"></a>
  <a href="https://github.com/dduong19208/KoraPayments/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/dduong19208/KoraPayments/build.yml?branch=main&style=flat-square&label=build" alt="Build status"></a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-65a30d?style=flat-square" alt="Minecraft 1.21.x">
  <img src="https://img.shields.io/badge/Java-21%2B-f59e0b?style=flat-square" alt="Java 21+">
  <img src="https://img.shields.io/badge/Folia-supported-8b5cf6?style=flat-square" alt="Folia supported">
  <a href="https://dsc.gg/korapayments"><img src="https://img.shields.io/badge/Discord-Support-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord Support"></a>
</p>

<p align="center">
  <a href="https://github.com/dduong19208/KoraPayments/releases/latest">Download</a>
  · <a href="docs/CONFIGURATION.md">Configuration</a>
  · <a href="docs/COMMANDS.md">Commands</a>
  · <a href="docs/PLACEHOLDERS.md">Placeholders</a>
  · <a href="docs/DISCORD_STORE.md">Discord Store</a>
  · <a href="https://dsc.gg/korapayments">Discord Support</a>
  · <a href="SUPPORT.md">Support</a>
</p>

---

## Tổng quan

KoraPayments hợp nhất các luồng nạp tiền, phát thưởng và đối soát vào một plugin duy nhất. Người chơi có giao diện rõ ràng; quản trị viên có cấu hình tách biệt, log chi tiết và nhiều lựa chọn database/economy để vận hành trên server production.

| Nhóm | Khả năng |
|---|---|
| Thanh toán | Nạp ngân hàng qua QR, nạp thẻ cào, giới hạn số tiền, timeout và tự động kiểm tra giao dịch |
| Giao diện | Native Dialog trên Paper/Leaf, inventory/chat fallback trên các runtime còn lại, GUI tùy chỉnh qua `gui.yml` |
| Phát thưởng | Command theo thứ tự, Vault, PlayerPoints, FancyEco/FancyEconomy, khuyến mãi riêng cho bank và thẻ |
| Cộng đồng | Mốc nạp cá nhân/toàn server, bossbar tiến độ, điều kiện cá nhân, chống nhận thưởng lặp |
| Quản trị | Lịch sử giao dịch, top tổng/tuần/tháng, nạp thủ công, reload và kiểm tra trạng thái trong game |
| Discord | Webhook giao dịch và KoraStore Auto Buy với storefront, đơn hàng, log, feedback và giao hàng tự động |
| Dữ liệu | SQLite, MySQL, MariaDB và PostgreSQL; pool, timeout và index phù hợp từng backend |
| Tích hợp | PlaceholderAPI, nhiều ngôn ngữ, scheduler tương thích Folia và cơ chế economy fallback |

### Provider được hỗ trợ

| Loại | Lựa chọn |
|---|---|
| Ngân hàng | SePay, PayOS |
| Thẻ cào | GachTheFast, Card2K |
| Economy | Command, Auto, Vault, PlayerPoints, FancyEco/FancyEconomy |
| Database | SQLite, MySQL, MariaDB, PostgreSQL |

## Có gì mới trong 1.4.2

- Khắc phục `IllegalAccessException` của Native Dialog UI trên Paper/Leaf 1.21.11.
- Giữ nguyên inventory/chat fallback khi Dialog API không khả dụng.
- Thêm regression test với Adventure 4.26.1 cho reflection bridge.
- Bao gồm cải tiến 1.4.1: `minimum-personal-donated`, `display-rewards` và lệnh chỉnh điều kiện riêng cho từng mốc nạp server.
- Không thay đổi schema database, claim hoặc cấu hình người dùng hiện có.

Xem toàn bộ thay đổi tại [CHANGELOG.md](CHANGELOG.md) và hướng dẫn nâng cấp tại [UPGRADE-1.4.2.md](UPGRADE-1.4.2.md).

## Tương thích

| Thành phần | Yêu cầu |
|---|---|
| Java | 21 trở lên |
| Minecraft | 1.21.x |
| Server | Bukkit, Spigot, Paper, Folia, Leaf |
| PlaceholderAPI | Tùy chọn |
| Vault / PlayerPoints / FancyEco | Tùy chọn theo economy provider |
| Discord Bot | Chỉ cần khi bật KoraStore Auto Buy |

Plugin khai báo `folia-supported: true` và sử dụng scheduler thích ứng để giữ các thao tác player/global đúng ngữ cảnh nền tảng.

## Cài đặt nhanh

1. Tải `KoraPayments-1.4.2.jar` từ [GitHub Releases](https://github.com/dduong19208/KoraPayments/releases/latest).
2. Dừng hoàn toàn Minecraft server.
3. Đặt JAR vào thư mục `plugins/`.
4. Khởi động server một lần để tạo cấu hình mặc định.
5. Chọn provider và điền thông tin xác thực trong các file tương ứng.
6. Kiểm tra bằng `/kora-admin status` trước khi mở tính năng cho người chơi.

> Khi nâng cấp server đang hoạt động, luôn backup `plugins/KoraPayments/` và database trước khi thay JAR. Không xóa các file YAML hiện có nếu bạn muốn giữ cấu hình tùy chỉnh.

### Bản đồ cấu hình

| File | Nội dung |
|---|---|
| `config.yml` | Ngôn ngữ, provider đang dùng, logging, metrics và ưu tiên Dialog UI |
| `database.yml` | SQLite/MySQL/MariaDB/PostgreSQL, pool và timeout |
| `payments.yml` | Luồng bank/thẻ, thuế, khuyến mãi, economy và lệnh phát thưởng |
| `providers/*.yml` | API key và thiết lập riêng của SePay, PayOS, GachTheFast, Card2K |
| `gui.yml` | Layout, item, màu sắc và nội dung các inventory GUI |
| `milestones.yml` | Mốc nạp cá nhân/toàn server, điều kiện và phần thưởng |
| `discord.yml` | Discord webhook cho giao dịch |
| `store.yml` | Discord Auto Buy, kênh, role quản trị và danh mục sản phẩm |
| `languages/*.yml` | Toàn bộ nội dung hiển thị theo ngôn ngữ |

Tài liệu cấu hình đầy đủ: [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

### Cấu hình provider

```yaml
providers:
  bank: "sepay"          # sepay | payos
  card: "gachthefast"    # gachthefast | card2k
  economy: "command"     # command | auto | vault | playerpoints | fancyeco
```

Thông tin nhạy cảm được tách vào `providers/*.yml`. Với Discord Store, có thể đọc bot token từ biến môi trường thay vì ghi trực tiếp vào repository.

### Cấu hình database

```yaml
database:
  type: "sqlite" # sqlite | mysql | mariadb | postgresql
```

SQLite phù hợp cho cài đặt đơn giản. MySQL, MariaDB hoặc PostgreSQL phù hợp hơn khi cần database dùng chung và quản trị tập trung.

### Nhiều lệnh phát thưởng

```yaml
economy:
  reward-commands:
    - "p give {player} {points}"
    - "crate key give {player} napthe 1"
```

Các command được chạy theo đúng thứ tự cấu hình sau khi giao dịch được provider xác nhận.

## Lệnh chính

| Lệnh | Mô tả |
|---|---|
| `/bank <số_tiền\|gui\|cancel>` | Tạo, mở giao diện hoặc hủy giao dịch ngân hàng |
| `/napthe gui` | Mở luồng nạp thẻ thích ứng theo runtime |
| `/napthe <loại> <mệnh_giá> [serial] [mã_thẻ]` | Nạp thẻ trực tiếp bằng command |
| `/confirmcard`, `/cancelcard` | Xác nhận hoặc hủy phiên nhập thẻ |
| `/topnap [all\|week\|month] [trang]` | Xem bảng xếp hạng nạp |
| `/lichsunap` | Xem lịch sử nạp cá nhân |
| `/mocnap <canhan\|server\|bossbar>` | Xem mốc nạp, nhận thưởng và quản lý bossbar cá nhân |
| `/kora-admin` | Quản trị, reload, status, nạp thủ công và quản lý dữ liệu |
| `/taokenhbanhang [publish\|reload\|status]` | Quản lý Discord Auto Buy storefront |

| Permission | Mặc định | Phạm vi |
|---|---|---|
| `korapayments.default` | `true` | Lệnh dành cho người chơi |
| `korapayments.admin` | `op` | Toàn bộ thao tác quản trị |

Danh sách tham số và lệnh quản trị đầy đủ: [docs/COMMANDS.md](docs/COMMANDS.md).

## PlaceholderAPI

KoraPayments đăng ký `%kp_*%` và alias `%korapayments_*%` khi PlaceholderAPI có mặt.

```text
%kp_donate_total%
%kp_donate_total_today%
%kp_donate_total_week%
%kp_donate_total_month%
%kp_donate_server_total%
%kp_donate_total_top_player_1%
%kp_donate_total_top_amount_1%
%kp_mocnapserver%
%kp_transactions%
```

Các modifier `_raw`, `_number`, `_plain`, `_unformatted`, `_noformat` trả về số không định dạng. Xem danh sách đầy đủ tại [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md).

## Discord Auto Buy

KoraStore cho phép đăng storefront trực tiếp lên Discord, nhận lựa chọn sản phẩm và tên nhân vật, theo dõi thanh toán rồi giao hàng bằng command khi người chơi online.

- Danh mục sản phẩm và embed có thể chỉnh trong `store.yml`.
- Có kênh riêng cho storefront, đơn thành công, feedback và lỗi.
- Có role quản trị, slash command kiểm tra đơn và lịch sử đơn có giới hạn.
- Đơn chờ được thử giao lại khi người chơi vào đúng server.

Hướng dẫn thiết lập: [docs/DISCORD_STORE.md](docs/DISCORD_STORE.md).

## Ngôn ngữ

Các bản dịch đi kèm: `vi`, `en`, `es`, `fr`, `de`, `pt`, `ru`, `zh`, `ja`, `ko`, `th`, `id`, `ms`, `tl`, `hi`, `ar`, `tr`, `pl`.

```yaml
language: "vi"
```

Mọi nội dung chính đều nằm trong `languages/*.yml`, giúp server tùy chỉnh cách diễn đạt mà không sửa source code.

## Build và kiểm thử

Yêu cầu: JDK 21+ và Maven 3.9+.

```bash
git clone https://github.com/dduong19208/KoraPayments.git
cd KoraPayments
mvn clean package
```

JAR đầu ra:

```text
target/KoraPayments-1.4.2.jar
```

Project có regression test cho Dialog reflection bridge. GitHub Actions chạy `mvn clean verify` cho mọi push và pull request trước khi tạo artifact.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [Configuration](docs/CONFIGURATION.md) | Cấu hình đầy đủ và ví dụ production |
| [Commands](docs/COMMANDS.md) | Toàn bộ lệnh người chơi và quản trị |
| [Placeholders](docs/PLACEHOLDERS.md) | Danh sách PlaceholderAPI và modifier |
| [Discord Store](docs/DISCORD_STORE.md) | Thiết lập storefront và quy trình đơn hàng |
| [Changelog](CHANGELOG.md) | Lịch sử phiên bản |
| [Security](SECURITY.md) | Bảo vệ token và báo cáo lỗ hổng |
| [Support](SUPPORT.md) | Checklist thu thập thông tin khi gặp lỗi |

## Bảo mật

Không commit API key, bot token, webhook URL hoặc database credential thật. Nếu secret từng bị public, hãy thu hồi và tạo lại ngay cả khi commit đã được xóa.

Xem chính sách đầy đủ tại [SECURITY.md](SECURITY.md).

## Hỗ trợ

> **Discord Support:** [https://dsc.gg/korapayments](https://dsc.gg/korapayments) — tham gia để nhận hỗ trợ và gửi góp ý trực tiếp.

**Author Discord:** `lz.dy.dg`

Trước khi báo lỗi, chạy `/kora-admin status`, bật `logging.debug` khi cần và chuẩn bị:

- phiên bản KoraPayments, server và Java;
- provider/database/economy đang sử dụng;
- log đầy đủ quanh thời điểm xảy ra lỗi;
- các bước tái hiện đã loại bỏ thông tin nhạy cảm.

Mở issue tại [GitHub Issues](https://github.com/dduong19208/KoraPayments/issues) hoặc làm theo [SUPPORT.md](SUPPORT.md).

## License

Copyright © DuyDuong. Dự án được phát hành theo điều khoản **All Rights Reserved**; xem [LICENSE](LICENSE) trước khi sao chép, chỉnh sửa hoặc phân phối.
