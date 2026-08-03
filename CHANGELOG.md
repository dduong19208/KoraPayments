# Changelog

## 1.4.2

### Fixed

- Sửa `IllegalAccessException` khiến Native Dialog UI bị tắt trên Paper/Leaf 1.21.11 khi chạy với Adventure/Java mới.
- Reflection bridge giờ gọi các method builder qua interface API công khai thay vì implementation nội bộ không public.

### Compatibility

- Giữ nguyên toàn bộ luồng nạp thẻ, nạp ngân hàng và inventory/chat fallback hiện có.
- Không thay đổi cấu hình, schema database hoặc dữ liệu người dùng.
- Thêm regression test cho cơ chế truy cập builder để bảo vệ các bản cập nhật sau.

## 1.4.1

### Added

- Mỗi mốc nạp chung hỗ trợ `minimum-personal-donated` riêng và kiểm tra bắt buộc khi nhận quà.
- `display-rewards` cho phép mô tả chính xác phần thưởng trên GUI mà không phụ thuộc cú pháp plugin crate/rank/EXP bên ngoài.
- `/kora-admin mocnap dieukien <mốc_server> <số_tiền|0|macdinh>` để chỉnh điều kiện trực tiếp trong game/console.
- GUI đánh số mốc nạp chung, hiển thị tiến độ cá nhân, số phần thưởng và trạng thái nhận quà.

### Compatibility

- Claim cũ và schema database được giữ nguyên, không có migration dữ liệu.
- Mốc không khai báo điều kiện riêng tiếp tục dùng cơ chế `anti-clone` cũ.

## 1.4.0

### Added

- Hỗ trợ Leaf runtime.
- Native Dialog UI cho Paper/Leaf khi Dialog API khả dụng.
- `/napthe gui` và `/bank gui` dùng một giao diện thích ứng, có inventory/chat fallback.
- `CardFlowManager` dùng chung cho command, GUI inventory và Dialog.
- `ConfigurationManager` hợp nhất cấu hình tách file và tự migrate từ `config.yml` 1.3.1.
- PostgreSQL backend.
- Economy bridge: command, auto, Vault, PlayerPoints và FancyEco/FancyEconomy bridge.
- Reward delivery result và log đối soát khi giao dịch đã xác nhận nhưng phát thưởng thất bại.
- Danh sách `economy.reward-commands` hỗ trợ nhiều lệnh console theo thứ tự, dùng chung cho bank/thẻ/nạp thủ công và command fallback.
- Khu vực `providers` trong `config.yml` để chọn bank, card và economy provider tại một nơi.
- `gui.yml` phiên bản 2 có cấu hình mặc định dùng chung và có thể bật/tắt từng static/dynamic item.
- Index PostgreSQL phục vụ lịch sử, leaderboard và claim mốc nạp không phân biệt hoa/thường.

### Changed

- Tách database, payment, Discord, milestone và provider khỏi `config.yml`.
- Giữ nguyên các key cũ qua merged configuration view.
- Tập trung hóa chuẩn hóa tên nhà mạng trong `CardProviderNames`.
- Reward command legacy tiếp tục là mặc định để không làm thay đổi hành vi máy chủ đang chạy.
- Các key provider/reward cũ tiếp tục là fallback khi cấu hình mới chưa có.
- MySQL/MariaDB tiếp tục được hỗ trợ và cấu hình pool/timeout rõ ràng hơn.
- Cập nhật version plugin lên 1.4.0.

### Fixed

- Sửa lỗi JDA/WebSocket callback chạy sau khi plugin classloader đã đóng JAR (`zip file closed`).
- Che serial/mã thẻ ở bước xác nhận theo mặc định để hạn chế lộ dữ liệu nhạy cảm trong chat client.
- Shutdown JDA, executor, OkHttp, scheduler, database và log writer theo lifecycle có timeout.
- Tránh ghi nhận nạp thủ công khi reward thất bại.
- Ghi nhận giao dịch bank/card đã được provider xác nhận ngay cả khi reward cần đối soát.
- Sửa truy vấn leaderboard riêng cho PostgreSQL.
- Giữ Folia entity operations trên player scheduler và command/economy operations trên global scheduler.

### Compatibility

- Java 21+.
- Bukkit/Spigot/Paper/Folia/Leaf 1.21.x.
- Dialog UI là optional; không tạo hard dependency vào Paper API.
