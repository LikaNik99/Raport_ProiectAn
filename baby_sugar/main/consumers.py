from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
import json
from customer.models import Notification


class UserInboxConsumer(AsyncWebsocketConsumer):

    async def connect(self):
        user = self.scope["user"]
        print("🔥 WS CONNECT CALLED")

        if user.is_anonymous:
            await self.close()
            return

        self.user = user
        self.group_name = f"user_{user.id}"

        await self.set_online(True)

        await self.channel_layer.group_add(
            self.group_name,
            self.channel_name
        )
        await self.accept()


        unread_count = await self.get_unread_count()
        
        # for n in unread:
        #     await self.send(json.dumps({
        #         "id": n["id"],
        #         "title": n["title"],
        #         "message": n["message"],
        #         "created_at": str(n["created_at"]),
        #     }))
        await self.send(json.dumps({
        "type": "init",
        "unread_count": unread_count,}))
        
    async def disconnect(self, close_code):
        await self.set_online(False)

        await self.channel_layer.group_discard(
            self.group_name,
            self.channel_name
        )

    async def notify(self, event):
        data = event.get("data")
        if not data:
            print("⚠️ WS notify fără data, ignorat:", event)
            return
        print("🟢 USER WS notify SEND:", data)
        await self.send(json.dumps(event["data"]))


    @database_sync_to_async
    def set_online(self, status):
        self.user.is_online = status
        self.user.save(update_fields=["is_online"])

    # @database_sync_to_async
    # def get_unread_notifications(self):
    #     return list(
    #         Notification.objects.filter(
    #             recipient=self.user,
    #             is_read=False
    #         ).values("id", "title", "message", "created_at")
    #     )

    @database_sync_to_async
    def mark_notifications_read(self):
        Notification.objects.filter(
            recipient=self.user,
            is_read=False
        ).update(is_read=True)
    
    @database_sync_to_async
    def get_unread_count(self):
        return Notification.objects.filter(
            recipient=self.user,
            is_read=False
        ).count()




class AdminOrderConsumer(AsyncWebsocketConsumer):

    async def connect(self):
        user = self.scope["user"]
        if not user.is_staff:
            await self.close()
            return

        await self.channel_layer.group_add("admins", self.channel_name)
        await self.accept()
        print("🟢 Admin WS CONNECTED:", user)

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard("admins", self.channel_name)

    async def order_new(self, event):
        await self.send(json.dumps(event["data"]))
