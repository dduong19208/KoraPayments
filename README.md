<p align="center">
  <img src="assets/banner.png" alt="KoraPayments - Modern Minecraft Payment Plugin" width="100%">
</p>

<h1 align="center">KoraPayments</h1>

<p align="center">
  <strong>Giải pháp thanh toán hiện đại cho máy chủ Minecraft</strong>
</p>

<p align="center">
  Nạp bank • Nạp thẻ • Mốc nạp • Khuyến mãi • Discord Webhook • Discord Auto Buy
</p>

<p align="center">
  <a href="https://github.com/dduong19208/KoraPayments/releases/latest">
    <img src="https://img.shields.io/badge/Release-v1.4.2-2ea44f?style=for-the-badge" alt="Release v1.4.2">
  </a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-brightgreen?style=for-the-badge" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge" alt="Java">
  <img src="https://img.shields.io/badge/Bukkit%20%2F%20Spigot%20%2F%20Paper%20%2F%20Folia%20%2F%20Leaf-Supported-blue?style=for-the-badge" alt="Platform">
</p>
<div align="center">

[⬇️ Tải plugin](../../releases/latest) · [📘 Hướng dẫn](#-hướng-dẫn-cài-đặt-nhanh) · [⚙️ Cấu hình](docs/CONFIGURATION.md) · [💬 Commands](docs/COMMANDS.md) · [🧩 Placeholders](docs/PLACEHOLDERS.md)

</div>

---

## ✨ Giới thiệu

**KoraPayments** là plugin thanh toán dành cho Minecraft server, tập trung vào trải nghiệm nạp tiền nhanh, rõ ràng và dễ quản trị. Plugin hỗ trợ nạp ngân hàng qua QR, nạp thẻ cào, khuyến mãi theo sự kiện, mốc nạp cá nhân/toàn server, lịch sử giao dịch, bảng xếp hạng nạp, Discord webhook và hệ thống Discord Auto Buy.

Plugin được thiết kế theo hướng thân thiện với server production:

- Tách module rõ ràng giữa nạp bank, nạp thẻ, mốc nạp, giao diện, Discord Store và PlaceholderAPI.
- Hỗ trợ **Bukkit/Spigot/Paper/Folia/Leaf 1.21.x**.
- Có scheduler tương thích nền tảng để giảm rủi ro chạy task sai thread trên Folia.
- Có log giao dịch, chống spam log và tùy chọn ghi log bất đồng bộ.
- Có nhiều ngôn ngữ sẵn trong thư mục `languages`.
- Không bắt buộc PlaceholderAPI, nhưng sẽ tự đăng ký placeholder khi plugin này có mặt.

---

## 🆕 Điểm mới trong 1.4.2

- Sửa Native Dialog UI trên Paper/Leaf 1.21.11 khi Adventure trả về builder implementation không public.
- Giữ nguyên luồng inventory/chat fallback trên Spigot, Paper/Leaf cũ hoặc runtime không có Dialog API.
- Bổ sung kiểm thử hồi quy trực tiếp với Adventure 4.26.1 để ngăn lỗi `IllegalAccessException` tái xuất hiện.
- Bao gồm cải tiến mốc nạp 1.4.1: điều kiện nạp cá nhân riêng cho từng mốc và mô tả phần thưởng thân thiện hơn.

---

## ⬇️ Tải plugin

### Bản ổn định mới nhất

➡️ **Download:** [KoraPayments Releases](../../releases/latest)

Sau khi mở trang Releases, tải file:

```text
KoraPayments-1.4.2.jar
```

> Không tải source code `.zip`/`.tar.gz` nếu bạn chỉ muốn cài plugin vào server. Hãy tải file `.jar` trong phần **Assets** của release.

---

## 🚀 Tính năng nổi bật

### 💳 Nạp ngân hàng

- Hỗ trợ provider: **SePay** và **PayOS**.
- Tạo giao dịch theo số tiền người chơi nhập.
- Hỗ trợ giao diện chọn mệnh giá bằng `/bank gui`.
- Hỗ trợ SQLite, MySQL, MariaDB và PostgreSQL.
- Hiển thị thông tin chuyển khoản và QR thanh toán trong game.
- Tự kiểm tra giao dịch theo chu kỳ cấu hình.
- Hỗ trợ giới hạn số tiền tối thiểu/tối đa và timeout giao dịch.
- Hỗ trợ khuyến mãi riêng cho nạp bank.

### 🧾 Nạp thẻ cào

- Hỗ trợ provider: **Card2K** và **GachTheFast**.
- Có Dialog UI trên Paper/Leaf và inventory/chat fallback bằng `/napthe gui`.
- Có thể nhập serial/mã thẻ trực tiếp bằng command.
- Hỗ trợ chiết khấu theo mệnh giá hoặc theo nhà mạng.
- Hỗ trợ nhiều command thưởng theo thứ tự sau khi nạp thành công.
- Hỗ trợ khuyến mãi riêng cho nạp thẻ.

### 🎁 Mốc nạp cá nhân và toàn server

- Mốc nạp cá nhân: người chơi nhận thưởng khi đạt từng mốc.
- Mốc nạp toàn server: toàn cộng đồng mở khóa phần thưởng khi tổng nạp server đạt mốc.
- Có bossbar theo dõi tiến độ mốc nạp server.
- Có cơ chế chống clone nhận thưởng mốc server.
- Admin có thể đổi mốc bossbar đang theo dõi bằng command.

### 🏆 Top nạp và lịch sử giao dịch

- `/topnap` hỗ trợ xem top tổng, tuần, tháng.
- `/lichsunap` cho người chơi xem lịch sử nạp cá nhân.
- Admin có thể xem lịch sử nạp của người chơi khác.
- Có lệnh reset dữ liệu top nạp khi cần.

### 🔔 Discord webhook

- Gửi thông báo giao dịch nạp bank, nạp thẻ và nạp thủ công về Discord.
- Có cấu hình riêng màu embed, avatar, username và bật/tắt từng loại giao dịch.

### 🛒 Discord Auto Buy / KoraStore

- Bot Discord đăng embed cửa hàng và menu chọn sản phẩm.
- Người mua nhập tên nhân vật, thanh toán, sau đó plugin tự giao hàng bằng command.
- Hỗ trợ trạng thái đơn đang chờ người chơi online.
- Có log đơn thành công, đơn lỗi và feedback sau mua.
- Có slash command staff để kiểm tra mã đơn.
- Có slash command Discord `/taokenhbanhang` để đăng storefront mà không cần vào Minecraft.

### 🧩 PlaceholderAPI

KoraPayments tự đăng ký các identifier:

- `%kp_*%`
- `%korapayments_*%` làm alias tương thích khi cần tránh trùng identifier.

Xem đầy đủ tại: [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md)

---

## ✅ Yêu cầu

| Thành phần | Yêu cầu |
|---|---|
| Java | Java 21 trở lên |
| Server | Bukkit/Spigot/Paper/Folia/Leaf 1.21.x |
| Folia | Có khai báo `folia-supported: true` và scheduler tương thích |
| PlaceholderAPI | Tùy chọn, dùng nếu cần placeholder |
| Plugin điểm/coin/economy | Tùy chọn, phụ thuộc command thưởng bạn cấu hình |
| Discord Bot | Chỉ cần nếu bật Discord Auto Buy |

---

## 📦 Hướng dẫn cài đặt nhanh

1. Tải file `KoraPayments-1.4.2.jar` tại [Releases](../../releases/latest).
2. Dừng server Minecraft.
3. Chép file `.jar` vào thư mục:

```text
plugins/
```

4. Khởi động server để plugin tạo file cấu hình.
5. Mở thư mục:

```text
plugins/KoraPayments/
```

6. Cấu hình các file:

```text
config.yml
database.yml
payments.yml
providers/*.yml
discord.yml
milestones.yml
gui.yml
store.yml
languages/vi.yml
```

7. Reload plugin:

```text
/kora-admin reload
```

8. Kiểm tra trạng thái:

```text
/kora-admin status
```

---

## ⚙️ Cấu hình nhanh

### Chọn ngôn ngữ

```yaml
language: "vi"
```

Ngôn ngữ có sẵn: `vi`, `en`, `es`, `fr`, `de`, `pt`, `ru`, `zh`, `ja`, `ko`, `th`, `id`, `ms`, `tl`, `hi`, `ar`, `tr`, `pl`.

### Chọn provider

```yaml
providers:
  bank: "sepay"          # sepay | payos
  card: "gachthefast"    # gachthefast | card2k
  economy: "command"     # command | auto | vault | playerpoints | fancyeco
```

API key/tài khoản được tách riêng trong `providers/*.yml`.

### Database

```yaml
database:
  type: sqlite # sqlite | mysql | mariadb | postgresql
```

### Nhiều lệnh thưởng

```yaml
economy:
  reward-commands:
    - "p give {player} {points}"
    - "crate key give {player} napthe 1"
```

Công thức mặc định:

```text
points = số_tiền / 1000 * conversion-rate
```

Xem chi tiết tại: [docs/CONFIGURATION.md](docs/CONFIGURATION.md)

---

## 💬 Lệnh chính

| Lệnh | Quyền | Mô tả |
|---|---:|---|
| `/bank <số_tiền>` | `korapayments.default` | Tạo giao dịch nạp ngân hàng |
| `/bank gui` | `korapayments.default` | Mở GUI chọn mệnh giá nạp bank |
| `/bank cancel` | `korapayments.default` | Hủy giao dịch bank đang chờ |
| `/napthe gui` | `korapayments.default` | Mở GUI nạp thẻ |
| `/napthe <loại> <mệnh_giá> [serial] [mã_thẻ]` | `korapayments.default` | Nạp thẻ bằng command |
| `/confirmcard` | `korapayments.default` | Xác nhận gửi thẻ đang nhập |
| `/cancelcard` | `korapayments.default` | Hủy phiên nhập thẻ |
| `/topnap [all\|week\|month] [trang]` | `korapayments.default` | Xem bảng xếp hạng nạp |
| `/lichsunap` | `korapayments.default` | Xem lịch sử nạp cá nhân |
| `/mocnap canhan` | `korapayments.default` | Xem/nhận mốc nạp cá nhân |
| `/mocnap server` | `korapayments.default` | Xem/nhận mốc nạp toàn server |
| `/mocnap bossbar [on\|off\|toggle\|status]` | `korapayments.default` | Tùy chỉnh bossbar cá nhân |
| `/kora-admin` hoặc `/kp-admin` | `korapayments.admin` | Mở lệnh quản trị |
| `/taokenhbanhang [publish\|reload\|status]` | `korapayments.admin` | Quản lý Discord Auto Buy storefront |

Danh sách đầy đủ: [docs/COMMANDS.md](docs/COMMANDS.md)

---

## 🔐 Quyền hạn

| Permission | Mặc định | Mục đích |
|---|---|---|
| `korapayments.default` | `true` | Cho phép người chơi dùng các lệnh nạp, top, lịch sử, mốc nạp |
| `korapayments.admin` | `op` | Cho phép quản trị plugin, reload, nạp thủ công, reset dữ liệu, quản lý store |

---

## 🧩 PlaceholderAPI nhanh

| Placeholder | Mô tả |
|---|---|
| `%kp_donate_total%` | Tổng nạp cá nhân |
| `%kp_donate_total_today%` | Tổng nạp cá nhân hôm nay |
| `%kp_donate_total_week%` | Tổng nạp cá nhân trong tuần |
| `%kp_donate_total_month%` | Tổng nạp cá nhân trong tháng |
| `%kp_donate_server_total%` | Tổng nạp toàn server |
| `%kp_donate_total_top_player_1%` | Tên người chơi top 1 |
| `%kp_donate_total_top_amount_1%` | Số tiền top 1 |
| `%kp_mocnapserver%` | Tiến độ mốc nạp server hiện tại |
| `%kp_transactions%` | Tổng số giao dịch |

Có thể thêm modifier raw number:

```text
_raw, _number, _plain, _unformatted, _noformat
```

Ví dụ:

```text
%kp_donate_total_raw%
%kp_donate_server_total_number%
```

---

## 🛠 Build từ source

Yêu cầu máy build có **Java 21+** và **Maven 3.9+**.

```bash
git clone https://github.com/YOUR_ORG/KoraPayments.git
cd KoraPayments
mvn clean package
```

File `.jar` sau build nằm tại:

```text
target/KoraPayments-1.4.2.jar
```

---

## 🚢 Tạo GitHub Release

Repo này đã có workflow tự build và đính kèm `.jar` khi tạo tag dạng `v*`.

```bash
git tag v1.4.2
git push origin v1.4.2
```

Sau đó mở tab **Actions** hoặc **Releases** trên GitHub để kiểm tra file jar đã được attach vào release.

Nội dung release có tại: [GITHUB_RELEASE_v1.4.2.md](GITHUB_RELEASE_v1.4.2.md)

---

## 🧯 Bảo mật vận hành

Không commit các thông tin thật sau lên GitHub:

- `sepay.api-token`
- `payos.client-id`, `payos.api-key`, `payos.checksum-key`
- `card2k.partner_id`, `card2k.partner_key`
- `gachthefast.partner_id`, `gachthefast.partner_key`, `wallet_id`
- `discord-webhook.url`
- `store.yml` chứa `bot-token`

Nếu bạn vô tình public token, hãy thu hồi token cũ và tạo token mới ngay.

Xem thêm: [SECURITY.md](SECURITY.md)

---

## 🧪 Checklist test sau khi cài

- `/kora-admin status` hiển thị đúng provider và PlaceholderAPI.
- `/bank 10000` tạo được giao dịch, QR và nội dung chuyển khoản.
- `/bank cancel` hủy được giao dịch đang chờ.
- `/napthe gui` mở được GUI chọn thẻ.
- `/topnap` mở được bảng xếp hạng.
- `/lichsunap` hiển thị lịch sử cá nhân.
- `/mocnap server` hiển thị mốc server.
- Nếu bật Discord webhook, giao dịch test gửi được embed về Discord.
- Nếu bật Store, `/taokenhbanhang status` không báo thiếu bot token/channel.

---

## 📁 Cấu trúc repo

```text
src/main/java/vn/korapayments/       Source plugin
src/main/resources/config.yml        Provider selector/cấu hình chung
src/main/resources/database.yml      SQLite/MySQL/MariaDB/PostgreSQL
src/main/resources/payments.yml      Bank, thẻ, economy và reward
src/main/resources/providers/        API từng provider
src/main/resources/gui.yml            Giao diện tùy chỉnh
src/main/resources/store.yml         Cấu hình Discord Auto Buy
src/main/resources/languages/        Ngôn ngữ hiển thị
docs/                                Tài liệu sử dụng chi tiết
.github/workflows/                   Build và release automation
```

---

## 🆘 Hỗ trợ

- Dùng `/kora-admin status` trước khi báo lỗi để kiểm tra trạng thái plugin.
- Bật `logging.debug: true` khi cần log chi tiết.
- Khi gửi issue, vui lòng đính kèm version server, Java version, provider đang dùng và log lỗi liên quan.

Tạo issue tại tab **Issues** của repo hoặc xem [SUPPORT.md](SUPPORT.md).
