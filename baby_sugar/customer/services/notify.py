from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync
from customer.models import Notification


def notify_user(*, recipient, title, message, sender=None, event_key=None, order=None):
    print("🟣 notify_user CALLED")
    print("🟣 recipient:", recipient)
    print("🟣 event_key:", event_key)
    print("🟣 sender:", sender)

    if event_key:
        exists = Notification.objects.filter(
            recipient=recipient,
            event_key=event_key
        ).exists()
        if exists:
            print("🟡 DUPLICATE notification (already exists)")
            return

    notification = Notification.objects.create(
        recipient=recipient,
        sender=sender,
        title=title,
        message=message,
        event_key=event_key,
        order=order,
        is_read=False
        
    )

    print("🟢 Notification CREATED:", notification.id)


    print("🟢 SENDING WS to user group")
    
    # 🔑 ADEVĂRUL ABSOLUT
    unread_count = Notification.objects.filter(
        recipient=recipient,
        is_read=False
    ).count()

    channel_layer = get_channel_layer()
    async_to_sync(channel_layer.group_send)(
        f"user_{recipient.id}",
        {
            "type": "notify",
            "data": {
                "type":"notification",
                "id": notification.id,
                "title": title,
                "message": message,
                "order": order.id if order else None,
                "sender":(sender.get_full_name() or sender.email if sender else "system"),
                "created_at": notification.created_at.isoformat(),
                "is_new":True,
                "unread_count": unread_count,
            }
        }
    )
