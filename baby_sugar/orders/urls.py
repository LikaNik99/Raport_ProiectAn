from django.urls import path
from . import views

app_name = "orders"

urlpatterns = [
    path("", views.my_orders,name="orders"),
    path("place/", views.place_order, name="place"),
    path("process/<int:order_id>/", views.process_order_view, name="process"),
    path("<int:order_id>/", views.order_detail, name="order_detail"),
    
]
