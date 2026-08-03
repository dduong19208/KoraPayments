# Nâng cấp KoraPayments 1.4.0 lên 1.4.1

1. Dừng hoàn toàn máy chủ và backup thư mục `plugins/KoraPayments/` cùng database.
2. Thay JAR cũ bằng `KoraPayments-1.4.1.jar`, sau đó khởi động lại máy chủ.
3. Claim mốc nạp cũ được giữ nguyên; bản này không thay đổi schema database.
4. File `milestones.yml` đang dùng không bị ghi đè. Dùng lệnh dưới đây hoặc tự thêm key mới cho từng mốc:

```text
/kora-admin mocnap dieukien <mốc_server> <số_tiền|0|macdinh>
```

Muốn GUI mô tả đẹp các quà từ plugin crate/rank/EXP, thêm `display-rewards` vào mốc tương ứng. Danh sách này chỉ để hiển thị; `rewards` vẫn là các lệnh console thực sự được chạy.

Sau khi cấu hình, kiểm tra `/mocnap server` bằng một tài khoản chưa đủ và một tài khoản đã đủ điều kiện cá nhân trước khi mở cho toàn server.
