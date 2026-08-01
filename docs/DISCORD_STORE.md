# Discord Auto Buy / KoraStore

KoraPayments có module Discord Auto Buy, cấu hình trong:

```text
plugins/KoraPayments/store.yml
```

Module này dùng bot Discord để đăng cửa hàng, tạo đơn, theo dõi thanh toán và giao hàng bằng command trong Minecraft.

---

## Bật store

```yaml
settings:
  enabled: true
  bot-token: "TOKEN_BOT_DISCORD"
  storefront-channel-id: "ID_KENH_BAN_HANG"
  purchase-log-channel-id: "ID_KENH_LOG_DON_HANG"
  feedback-channel-id: "ID_KENH_FEEDBACK"
  error-log-channel-id: "ID_KENH_DON_LOI"
```

Không chia sẻ `bot-token` cho bất kỳ ai.

---

## Quyền staff Discord

```yaml
settings:
  admin-role-ids:
    - "ROLE_ID_STAFF"
  admin-command-guild-id: "GUILD_ID"
```

Người có quyền Discord Administrator hoặc Manage Server vẫn có thể dùng command staff theo logic plugin.

---

## Đăng storefront

Trong game, dùng:

```text
/taokenhbanhang publish
```

Hoặc alias:

```text
/storepost publish
/kpstore publish
```

Kiểm tra trạng thái:

```text
/taokenhbanhang status
```

Reload store:

```text
/taokenhbanhang reload
```

---

## Cấu hình embed storefront

```yaml
settings:
  storefront-embed:
    title: "🛒 AUTO BUY | KoraStore"
    description:
      - "Bot mua hàng tự động 24/7"
      - "Thanh toán xong bot sẽ giao hàng khi người chơi đang online."
    color: 5814783
    footer: "KoraPayments Auto Buy"
    select-placeholder: "Chọn mặt hàng bạn muốn mua..."
```

---

## Cấu hình sản phẩm

```yaml
products:
  key_vip:
    enabled: true
    name: "Key VIP"
    price: 10000
    detail: "Nhận 1 key VIP để mở crate."
    require-online: true
    commands:
      - "crate key give %player% vip 1"
      - "bc &bKoraStore &8» &f%player% vừa mua &aKey VIP&f."
```

### Placeholder trong command sản phẩm

```text
%player%, {player}, %buyer%, %discord_user%, %discord_id%,
%item%, %item_name%, %price%, %order_id%, %payment_order%
```

### `require-online`

| Giá trị | Ý nghĩa |
|---|---|
| `true` | Người chơi phải online để giao hàng. Nếu offline, đơn sẽ chờ và có thể chạy lại. |
| `false` | Plugin có thể chạy command giao hàng không cần người chơi online, tùy command bạn dùng. |

---

## Slash command staff

Discord command đăng storefront:

```text
/taokenhbanhang
```

Discord command kiểm tra đơn:

```text
/kora-admin kiemtramadon <ma-don>
```

Dùng để kiểm tra mã đơn khi người chơi báo lỗi.

---

## Checklist khi store không hoạt động

1. `settings.enabled` đã là `true` chưa.
2. Bot token còn hợp lệ không.
3. Bot đã được mời vào đúng server Discord chưa.
4. Bot có quyền đọc/gửi tin nhắn, gửi embed, dùng slash command không.
5. Channel ID có đúng không.
6. Provider nạp bank đã cấu hình đúng chưa.
7. Server Minecraft có online trong lúc bot xử lý đơn không.
8. Dùng `/taokenhbanhang status` để kiểm tra nhanh.
