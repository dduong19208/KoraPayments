# PlaceholderAPI - KoraPayments

KoraPayments hỗ trợ PlaceholderAPI qua identifier chính:

```text
%kp_*%
```

Alias tương thích:

```text
%korapayments_*%
```

Alias hữu ích khi server có plugin khác đang dùng identifier `kp`.

---

## Tổng nạp cá nhân

| Placeholder | Mô tả |
|---|---|
| `%kp_donate_total%` | Tổng nạp cá nhân |
| `%kp_donate_total_today%` | Tổng nạp cá nhân hôm nay |
| `%kp_donate_total_week%` | Tổng nạp cá nhân trong tuần |
| `%kp_donate_total_month%` | Tổng nạp cá nhân trong tháng |
| `%kp_donate_total_year%` | Tổng nạp cá nhân trong năm |

---

## Tổng nạp server

| Placeholder | Mô tả |
|---|---|
| `%kp_donate_server_total%` | Tổng doanh thu/nạp toàn server |

---

## Mốc nạp server

| Placeholder | Mô tả |
|---|---|
| `%kp_mocnapserver%` | Thông tin mốc nạp server hiện tại |
| `%kp_mocnapserver_total%` | Tổng nạp server phục vụ mốc |
| `%kp_mocnapserver_progress_1000000%` | Tiến độ đến mốc `1,000,000` |
| `%kp_mocnapserver_percent_1000000%` | Phần trăm tiến độ đến mốc `1,000,000` |

Bạn có thể thay `1000000` bằng mốc khác đã cấu hình.

---

## Top nạp

### Tổng thời gian

| Placeholder | Mô tả |
|---|---|
| `%kp_donate_total_top_player_1%` | Tên người chơi top 1 |
| `%kp_donate_total_top_amount_1%` | Số tiền người chơi top 1 đã nạp |

Đổi số `1` thành vị trí khác, ví dụ:

```text
%kp_donate_total_top_player_2%
%kp_donate_total_top_amount_2%
```

### Theo ngày/tuần/tháng/năm

| Placeholder | Mô tả |
|---|---|
| `%kp_donate_total_top_player_1_today%` | Top 1 hôm nay |
| `%kp_donate_total_top_amount_1_today%` | Số tiền top 1 hôm nay |
| `%kp_donate_total_top_player_1_week%` | Top 1 tuần này |
| `%kp_donate_total_top_amount_1_week%` | Số tiền top 1 tuần này |
| `%kp_donate_total_top_player_1_month%` | Top 1 tháng này |
| `%kp_donate_total_top_amount_1_month%` | Số tiền top 1 tháng này |
| `%kp_donate_total_top_player_1_year%` | Top 1 năm này |
| `%kp_donate_total_top_amount_1_year%` | Số tiền top 1 năm này |

---

## Alias tương thích ngược

Plugin vẫn nhận một số placeholder cũ:

```text
%kp_player_total%
%kp_total%
%kp_server_total%
%kp_total_server%
%kp_top_player_1%
%kp_top_amount_1%
%kp_top_1_player%
%kp_top_1_amount%
%kp_transactions%
%kp_transaction_count%
```

---

## Modifier số thô

Mặc định số tiền có thể được format thân thiện. Nếu cần số thô để dùng trong điều kiện, scoreboard hoặc plugin khác, thêm một trong các suffix:

```text
_raw
_number
_plain
_unformatted
_noformat
```

Ví dụ:

```text
%kp_donate_total_raw%
%kp_donate_server_total_number%
%kp_donate_total_top_amount_1_plain%
```

---

## Gợi ý dùng với scoreboard/menu

Ví dụ hiển thị profile người chơi:

```text
Tổng nạp: %kp_donate_total%
Nạp tháng: %kp_donate_total_month%
Top 1: %kp_donate_total_top_player_1% - %kp_donate_total_top_amount_1%
Server: %kp_donate_server_total%
```

Nếu placeholder hiện nguyên text `%kp_...%`, hãy kiểm tra:

1. PlaceholderAPI đã cài chưa.
2. `/kora-admin status` có báo PlaceholderAPI enabled không.
3. Identifier `kp` có bị plugin khác chiếm không.
4. Thử alias `%korapayments_...%`.
