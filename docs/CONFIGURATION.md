# Configuration - KoraPayments

KoraPayments 1.4.0 tách cấu hình theo chức năng trong `plugins/KoraPayments/`:

```text
config.yml
database.yml
payments.yml
discord.yml
milestones.yml
gui.yml
providers/payos.yml
providers/sepay.yml
providers/card2k.yml
providers/gachthefast.yml
store.yml
languages/*.yml
```

Không nên đổi cấu trúc key nếu bạn không chắc plugin có đọc key đó. Sau khi chỉnh cấu hình quan trọng, dùng:

```text
/kora-admin reload
```

---

## `config.yml`

### Chọn provider

```yaml
providers:
  bank: "sepay"          # sepay | payos
  card: "gachthefast"    # gachthefast | card2k
  economy: "command"     # command | auto | playerpoints | vault | fancyeco
```

Các key cũ `napbank.provider`, `napthe.provider` và `economy.provider` vẫn được
đọc nếu section mới chưa tồn tại.

### Ngôn ngữ

```yaml
language: "vi"
```

Ngôn ngữ có sẵn:

```text
vi, en, es, fr, de, pt, ru, zh, ja, ko, th, id, ms, tl, hi, ar, tr, pl
```

---

## Logging

```yaml
logging:
  debug: false
  console:
    enabled: true
    anti-spam: true
    duplicate-window-seconds: 30
    transaction-events: true
  file:
    enabled: true
    async: true
    transaction-log: "transactions.log"
    card-log: "nap_the.txt"
```

Khuyến nghị:

- Production: để `debug: false`.
- Khi test provider/API: bật `debug: true` tạm thời.
- Giữ `file.async: true` để giảm tải main thread.

---

## Nạp ngân hàng (`payments.yml`)

### Giới hạn và thời gian

```yaml
napbank:
  min-amount: 2000
  max-amount: 0
  timeout-seconds: 600
  poll-every-seconds: 10
```

| Key | Mô tả |
|---|---|
| `min-amount` | Số tiền tối thiểu cho `/bank` |
| `max-amount` | Số tiền tối đa, đặt `0` để không giới hạn |
| `timeout-seconds` | Thời gian giữ giao dịch chờ thanh toán |
| `poll-every-seconds` | Chu kỳ kiểm tra giao dịch qua provider |

Không nên đặt `poll-every-seconds` quá thấp vì có thể spam API provider.

### GUI mệnh giá bank

```yaml
napbank:
  gui:
    amounts:
      - 10000
      - 20000
      - 50000
      - 100000
```

### Khuyến mãi bank

```yaml
napbank:
  promotion:
    enabled: true
    percent: 20
    end-date: "31/05/2026 23:59:59"
```

---

## PayOS (`providers/payos.yml`)

```yaml
payos:
  client-id: ""
  api-key: ""
  checksum-key: ""
  payment-format: "{playername} TENCUM {ordercode}"
```

Biến hỗ trợ trong nội dung chuyển khoản:

```text
{playername}, {player}, {ordercode}, {code}
```

Không bỏ `{ordercode}` để tránh lỗi đối soát.

---

## SePay (`providers/sepay.yml`)

```yaml
sepay:
  api-token: ""
  bank-code: ""
  bank-name: ""
  account-number: ""
  account-name: ""
  payment-format: "{playername} TENCUM {ordercode}"
  transaction-lookup-limit: 20
```

| Key | Mô tả |
|---|---|
| `api-token` | Token API SePay |
| `bank-code` | Mã ngân hàng dùng cho VietQR |
| `bank-name` | Tên ngân hàng hiển thị trong chat |
| `account-number` | Số tài khoản nhận tiền |
| `account-name` | Tên chủ tài khoản |
| `payment-format` | Nội dung chuyển khoản |
| `transaction-lookup-limit` | Số giao dịch gần nhất cần kiểm tra mỗi lần gọi API |

---

## Nạp thẻ cào (`payments.yml`)

### Chiết khấu

```yaml
napthe:
  taxes:
    enabled: true
    rates:
      "10000": 16
      "20000": 17
      "50000": 15.5
```

Có thể cấu hình theo mệnh giá chung hoặc theo nhà mạng nếu provider cần.

### Quy đổi điểm/thưởng

```yaml
napthe:
  rewards:
    ratio: 1000
```

Công thức:

```text
points = số_tiền_sau_chiết_khấu / ratio
```

### Khuyến mãi nạp thẻ

```yaml
napthe:
  promotion:
    enabled: true
    percent: 20
    end-date: "31/05/2026 23:59:59"
```

---

## Card2K (`providers/card2k.yml`)

```yaml
card2k:
  api:
    partner_id: ""
    partner_key: ""
    domain: "card2k.com"
    endpoint: "/chargingws/v2"
    method: "POST"
```

---

## GachTheFast (`providers/gachthefast.yml`)

```yaml
gachthefast:
  api:
    partner_id: ""
    partner_key: ""
    wallet_id: ""
    domain: "gachthefast.com"
    endpoint: "/chargingws/v2"
    method: "GET"
```

---

## Reward/economy (`payments.yml`)

```yaml
economy:
  fallback-to-command: true
  reward-commands:
    - "p give {player} {points}"
    - "crate key give {player} napthe 1"
  post-commands:
    bank: []
    manual: []
    card:
      - "bc {player} vừa nạp {amount} VNĐ qua thẻ cào."
conversion-rate: 1
```

Công thức mặc định:

```text
points = số_tiền / 1000 * conversion-rate
```

Biến hỗ trợ:

```text
{player}, {points}, {amount}, {net_amount}, {channel}
```

Danh sách chạy bằng console theo thứ tự. Các dạng cũ `%player%`, `%points%`,
`reward-command`, `economy.command` và `napthe.rewards.commands` vẫn tương thích.

---

## Discord webhook (`discord.yml`)

```yaml
discord-webhook:
  enabled: true
  url: "DAN_LINK_WEBHOOK_DISCORD_CUA_BAN_VAO_DAY"
  username: "KoraPayments"
  avatar-url: "https://minotar.net/avatar/%player%/100.png"
  color: 65280
  transactions:
    bank:
      enabled: true
      color: 65280
    card:
      enabled: true
      color: 16776960
    manual:
      enabled: true
      color: 10181046
```

Bạn có thể bật/tắt thông báo theo từng loại giao dịch.

---

## Mốc nạp cá nhân (`milestones.yml`)

```yaml
milestones:
  '50000':
    rewards:
      - "give %player% diamond 5"
  '100000':
    rewards:
      - "give %player% diamond 10"
```

Biến thường dùng:

```text
%player%
```

---

## Mốc nạp toàn server

```yaml
server-milestones:
  enabled: true
  active-target: 1000000
  anti-clone:
    enabled: true
    minimum-personal-donated: 20000
    new-player-only: true
  bossbar:
    enabled: true
    color: BLUE
    style: SEGMENTED_10
    update-interval-ticks: 100
```

### Anti-clone

| Key | Mô tả |
|---|---|
| `enabled` | Bật chống clone nhận mốc server |
| `minimum-personal-donated` | Người chơi phải có tổng nạp cá nhân tối thiểu |
| `new-player-only` | Chỉ áp dụng với tài khoản vào server sau khi mốc đã đạt |

### Bossbar

```yaml
server-milestones:
  bossbar:
    progress-title: "&bMốc nạp server &8» &f{current}&7/&a{target} VNĐ &8(&e{percent}%&8)"
    reached-title: "&aMáy chủ đã đạt mốc nạp &e{target} VNĐ&a. &eDùng /mocnap server để nhận."
```

Biến hỗ trợ trong mốc server:

```text
%player%, %amount%, %target%, %server_total%,
%amount_formatted%, %target_formatted%, %server_total_formatted%
```

---

## bStats

```yaml
metrics:
  enabled: true
```

Nếu không muốn gửi thống kê bStats, đặt thành `false`.
