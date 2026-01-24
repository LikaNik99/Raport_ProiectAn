from django.db.models.signals import post_save
from django.dispatch import receiver
from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync
from .models import Order


@receiver(post_save, sender=Order)
def order_created(sender, instance, created, **kwargs):
    print("🟣 SIGNAL post_save Order CALLED")
    print("🟣 created =", created)
    if not created:
        return

    print("🟢 ORDER CREATED ID:", instance.id)
    channel_layer = get_channel_layer()
    async_to_sync(channel_layer.group_send)(
        "admins",
        {
            "type": "order_new",
            "data": {
                "id": instance.id,
                "user": instance.user.username if instance.user else "guest",
                "status": instance.status,
                "created_at": instance.created_at.isoformat(),
            }
        }
    )
    print("🟢 SENT WS to group admins")
