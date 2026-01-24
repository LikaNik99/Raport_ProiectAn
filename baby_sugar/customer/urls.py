from django.urls import path
from . import views

app_name = "customer"

urlpatterns = [
    path("cart/", views.cart, name="cart"),
    path("cart/add/<int:product_id>/", views.add_to_cart, name="add_to_cart"),
    path("cart/plus/<int:item_id>/",   views.cart_plus, name="cart_plus"),
    path("cart/minus/<int:item_id>/",  views.cart_minus, name="cart_minus"),
    path("cart/remove/<int:item_id>/", views.cart_remove, name="cart_remove"),
    path("login/", views.login_view, name="login"),
    path("register/", views.register_view, name="register"),
    path("logout/", views.logout_view, name="logout"),
    path("wishlist/", views.wishlist_view, name="wishlist"),
    path("wishlist/toggle/<slug:product_slug>/", views.toggle_wishlist, name="toggle_wishlist"),
    path("profile/edit/", views.profile_edit, name="profile_edit"),
    path("profile/subscription/", views.toggle_subscription, name="toggle_subscription"),
    path("inbox/", views.inbox, name="inbox"),
    path("inbox/notifications/read/<int:pk>/", views.mark_notification_read, name="mark_notification_read"),
    path("inbox/notifications/read/", views.mark_notifications_bulk, name="mark_notifications_bulk"),
    path("inbox/notifications/delete/", views.delete_notifications_bulk, name="delete_notifications_bulk"),
    path("inbox/notifications/<int:pk>/", views.notification_detail, name="notification_detail"),
    path("toggle-theme/", views.toggle_theme, name="toggle_theme"),
   
]
