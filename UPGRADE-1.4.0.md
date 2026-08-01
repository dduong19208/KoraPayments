# Hướng dẫn nâng cấp KoraPayments 1.3.1 lên 1.4.0

## 1. Backup bắt buộc

Dừng máy chủ rồi sao lưu:

```text
plugins/KoraPayments/
plugins/KoraPayments/database.sqlite
```

Với MySQL/MariaDB/PostgreSQL, tạo dump database bằng công cụ quản trị tương ứng.

Không nâng cấp bằng PlugMan, `/reload` hoặc bất kỳ hot-reload manager nào. KoraPayments sở hữu JDA, HTTP client, thread pool và database pool; hot reload có thể để lại classloader/thread cũ.

## 2. Thay JAR

- Xóa JAR KoraPayments 1.3.1 khỏi thư mục `plugins/`.
- Chép JAR 1.4.0 vào.
- Không xóa `config.yml` cũ.

## 3. Khởi động lần đầu

Khi các tệp mới chưa tồn tại, plugin sẽ:

- tạo cấu hình mặc định;
- đọc `config.yml` cũ;
- sao chép từng root cũ sang đúng tệp mới;
- không xóa giá trị cũ;
- dựng merged configuration view để toàn bộ tính năng cũ tiếp tục đọc được.

Kiểm tra console để thấy log `Migrated legacy config.yml values into ...`.

## 4. Kiểm tra cấu hình

### Database

Tệp: `database.yml`

- `sqlite`: dùng database hiện có trong data folder.
- `mysql`: port tự động 3306 khi `port: 0`.
- `mariadb`: port tự động 3306 khi `port: 0`.
- `postgresql`: port tự động 5432 khi `port: 0`.

Đổi backend không tự chuyển dữ liệu. Hãy import dữ liệu trước, sau đó mới đổi `database.type` và restart.

### Provider

- Chọn provider đang dùng tại `config.yml` > `providers.bank`,
  `providers.card` và `providers.economy`.
- PayOS: `providers/payos.yml`
- SePay: `providers/sepay.yml`
- Card2K: `providers/card2k.yml`
- GachTheFast: `providers/gachthefast.yml`

### Reward/economy

Tệp: `payments.yml`

Giữ `config.yml` > `providers.economy: command` trong lần khởi động đầu để hành vi giống 1.3.1. Sau khi xác nhận plugin economy đang hoạt động, mới chuyển sang `auto`, `vault`, `playerpoints` hoặc `fancyeco`.

`economy.reward-commands` là danh sách nhiều lệnh. Cấu hình cũ dùng
`economy.command`, `reward-command` hoặc `napthe.rewards.commands` không cần sửa
ngay; plugin chỉ dùng các key đó khi danh sách mới chưa tồn tại.

## 5. Checklist test staging

1. `/napthe gui` mở Dialog hoặc inventory fallback.
2. Nạp thẻ test/sandbox và kiểm tra đúng một lần cộng điểm.
3. `/bank gui`, tạo QR và hủy giao dịch.
4. Giao dịch bank sandbox được ghi vào lịch sử.
5. `/topnap`, `/lichsunap`, `/mocnap` hoạt động.
6. Webhook Discord gửi đúng kênh được bật.
7. Discord Store login, publish và shutdown không còn stacktrace `zip file closed`.
8. Restart đầy đủ máy chủ hai lần để kiểm tra database migration idempotent.
9. Với Folia, test GUI, firework, sound và action bar trên nhiều region.
10. Kiểm tra console không có `REWARD_FAILED` hoặc `reward-delivery-failed`.

## 6. Rollback

- Dừng máy chủ.
- Khôi phục JAR 1.3.1 và toàn bộ backup data folder/database.
- Không dùng database đã được vận hành tiếp trên 1.4.0 mà không có backup nhất quán.
