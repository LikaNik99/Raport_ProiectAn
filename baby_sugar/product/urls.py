from django.urls import path
from . import views


app_name="product"

urlpatterns = [
    path("search/", views.product_search, name="product_search"),
    path("all/", views.all_products, name="all_products"),
    path("product/<slug:slug>/", views.product_detail, name="product_detail"),
    path("variant/<int:variant_id>/quantity/", views.variant_quantity, name="variant_quantity"),
    path("variant/<int:variant_id>/price/", views.variant_price, name="variant_price"),

]