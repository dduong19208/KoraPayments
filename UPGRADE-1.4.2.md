# Nâng cấp KoraPayments 1.4.1 lên 1.4.2

1. Dừng hoàn toàn máy chủ.
2. Backup thư mục `plugins/KoraPayments/` và database như quy trình cập nhật thông thường.
3. Thay JAR cũ bằng `KoraPayments-1.4.2.jar`, sau đó khởi động lại máy chủ.
4. Không xóa hoặc tạo lại các file YAML hiện có. Bản cập nhật này không thay đổi cấu hình hay schema database.
5. Trên Paper/Leaf 1.21.11, chạy `/napthe gui` và `/bank gui` để xác nhận Native Dialog hiển thị bình thường.

Nếu runtime không cung cấp Dialog API, plugin vẫn tự động sử dụng inventory/chat fallback như trước.
