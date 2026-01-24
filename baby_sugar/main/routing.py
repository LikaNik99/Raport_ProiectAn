from django.urls import path
from .consumers import UserInboxConsumer, AdminOrderConsumer

websocket_urlpatterns = [
    path("ws/inbox/", UserInboxConsumer.as_asgi()),
    path("ws/admin/", AdminOrderConsumer.as_asgi()),
]