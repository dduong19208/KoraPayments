# Commands - KoraPayments

Tài liệu này liệt kê các lệnh chính của KoraPayments theo nhóm người chơi và quản trị viên.

## Quyền cơ bản

| Permission | Mặc định | Mô tả |
|---|---|---|
| `korapayments.default` | `true` | Lệnh người chơi |
| `korapayments.admin` | `op` | Lệnh quản trị |

---

## Lệnh người chơi

### `/bank <số_tiền>`

Tạo giao dịch nạp ngân hàng với số tiền cụ thể.

Ví dụ:

```text
/bank 10000
/bank 50000
```

Plugin sẽ hiển thị thông tin chuyển khoản, nội dung chuyển khoản và QR nếu provider/cấu hình hợp lệ.

### `/bank gui`

Mở giao diện chọn mệnh giá nạp ngân hàng. Danh sách mệnh giá được cấu hình tại:

```yaml
napbank:
  gui:
    amounts:
      - 10000
      - 20000
      - 50000
```

### `/bank cancel`

Hủy giao dịch nạp ngân hàng đang chờ của người chơi.

---

### `/napthe gui`

Mở giao diện nạp thẻ cào, chọn nhà mạng và mệnh giá.

### `/napthe <loại> <mệnh_giá> [serial] [mã_thẻ]`

Tạo phiên nạp thẻ bằng command.

Ví dụ:

```text
/napthe VIETTEL 10000
/napthe VINAPHONE 50000 123456789 987654321
```

Nếu chưa nhập serial/mã thẻ, plugin sẽ yêu cầu người chơi nhập tiếp trong chat.

### `/confirmcard`

Xác nhận và gửi thẻ đang nhập lên provider.

### `/cancelcard`

Hủy phiên nhập thẻ hiện tại.

---

### `/topnap [all|week|month] [trang]`

Xem bảng xếp hạng nạp.

Ví dụ:

```text
/topnap
/topnap all
/topnap week
/topnap month 2
/topnap 2
```

---

### `/lichsunap`

Xem lịch sử nạp cá nhân.

---

### `/mocnap canhan`

Mở giao diện mốc nạp cá nhân.

Alias được code hỗ trợ trong xử lý command gồm: `canhan`, `ca-nhan`, `ca_nhan`, `personal`, `me`, `mine`, `cn`.

### `/mocnap server`

Mở giao diện mốc nạp toàn server và nhận quà mốc đã đạt nếu đủ điều kiện.

Alias được code hỗ trợ gồm: `server`, `sv`, `global`, `toanserver`, `toan-server`, `toan_server`, `rewards`, `reward`.

### `/mocnap bossbar [on|off|toggle|status]`

Điều chỉnh hiển thị bossbar mốc nạp server cho cá nhân người chơi.

Ví dụ:

```text
/mocnap bossbar on
/mocnap bossbar off
/mocnap bossbar toggle
/mocnap bossbar status
```

---

## Lệnh thông tin plugin

### `/korapayments`

Alias:

```text
/kp
/kora
```

Hiển thị trang help/thông tin plugin. Nếu nhập keyword admin, plugin sẽ nhắc dùng `/kora-admin`.

---

## Lệnh quản trị

### `/kora-admin`

Alias:

```text
/kp-admin
```

Hiển thị menu help quản trị.

### `/kora-admin status`

Xem trạng thái vận hành plugin:

- Nền tảng scheduler.
- Ngôn ngữ đang dùng.
- Provider nạp bank.
- Provider nạp thẻ.
- Chu kỳ kiểm tra giao dịch.
- Timeout giao dịch.
- Trạng thái webhook.
- Trạng thái PlaceholderAPI.

### `/kora-admin reload`

Reload cấu hình plugin.

Nên dùng sau khi đổi provider, webhook, ngôn ngữ, khuyến mãi, mốc nạp hoặc cấu hình store.

### `/kora-admin gui`

Mở dashboard quản trị trong game.

Alias xử lý trong code: `gui`, `menu`, `dashboard`.

### `/kora-admin napthucong <người_chơi> <số_tiền>`

Nạp thủ công cho người chơi.

Alias:

```text
/kora-admin manual <người_chơi> <số_tiền>
```

Ví dụ:

```text
/kora-admin napthucong Steve 50000
```

### `/kora-admin lichsunap <người_chơi>`

Xem lịch sử nạp của người chơi khác.

Alias:

```text
/kora-admin history <người_chơi>
```

### `/kora-admin reset topnap confirm`

Reset dữ liệu top nạp. Lệnh yêu cầu `confirm` để tránh thao tác nhầm.

### `/kora-admin mocnap set <số_tiền|auto>`

Đổi mốc bossbar toàn server đang theo dõi.

Ví dụ:

```text
/kora-admin mocnap set auto
/kora-admin mocnap set 1000000
```

### `/kora-admin mocnap bossbar <on|off|toggle|status>`

Bật/tắt bossbar mốc nạp toàn server ở cấp global.

Ví dụ:

```text
/kora-admin mocnap bossbar on
/kora-admin mocnap bossbar status
```

---

## Discord Store command trong Minecraft

### `/taokenhbanhang [publish|reload|status]`

Alias:

```text
/storepost
/kpstore
```

Subcommands:

| Subcommand | Mô tả |
|---|---|
| `publish` | Gửi embed cửa hàng Discord Auto Buy |
| `reload` | Reload cấu hình store |
| `status` | Xem trạng thái Discord Store |

Nếu không nhập subcommand, plugin mặc định chạy `publish`.

---

## Discord slash command

Khi bật Discord Auto Buy, plugin có slash command cho staff:

```text
/kora-admin kiemtramadon <ma-don>
```

Quyền dùng lệnh phụ thuộc vào:

- Role trong `settings.admin-role-ids`.
- Hoặc quyền Discord Administrator / Manage Server.
