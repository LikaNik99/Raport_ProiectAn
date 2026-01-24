from django.contrib import admin
from .models import Order
from customer.services.notify import notify_user


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "status", "created_at")

    def save_model(self, request, obj, form, change):
        old_status = None
        if obj.pk:
            old_status = Order.objects.get(pk=obj.pk).status

        super().save_model(request, obj, form, change)

        print("🟡 ADMIN save_model CALLED")
        print("🟡 Order ID:", obj.id)
        print("🟡 Old/New status:", old_status, "→", obj.status)

        if old_status != obj.status and obj.user:
            notify_user(
                recipient=obj.user,
                sender=request.user,
                title="Status comandă actualizat",
                message=f"Nou status: {obj.status}",
                event_key=f"order_{obj.id}_status_{obj.status}",
                order=obj,
            )
