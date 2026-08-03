# KoraPayments v1.4.2

## Sửa lỗi

- Khắc phục `IllegalAccessException` khiến Native Dialog UI bị vô hiệu hóa trên Paper/Leaf 1.21.11.
- Reflection bridge gọi builder qua interface API công khai của Adventure, không truy cập implementation nội bộ không public.
- Giữ nguyên inventory/chat fallback khi runtime không hỗ trợ Dialog API.

## Cải tiến đi kèm từ 1.4.1

- Mỗi mốc nạp chung có thể đặt `minimum-personal-donated` riêng.
- Hỗ trợ `display-rewards` để mô tả phần thưởng rõ ràng trong GUI.
- Thêm lệnh `/kora-admin mocnap dieukien <mốc_server> <số_tiền|0|macdinh>`.

## Tương thích và dữ liệu

- Java 21+, Bukkit/Spigot/Paper/Folia/Leaf 1.21.x.
- Không thay đổi schema database, không xóa claim hoặc cấu hình đang dùng.
- Luôn dừng server và backup thư mục plugin/database trước khi thay JAR.

## Xác minh

- `mvn clean package`: PASS.
- Regression tests: 2/2 PASS với Adventure 4.26.1.
- Local staging SHA-256: `EDB3CE8EAF99A9CC5CA0D0475007606B92D85BB49143C3AB5B11A9BF69037716`.

## Hỗ trợ và góp ý

**Discord Support:** [https://dsc.gg/korapayments](https://dsc.gg/korapayments)
