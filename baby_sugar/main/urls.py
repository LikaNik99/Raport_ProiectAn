from django.urls import path
from . import views

app_name = "main"

urlpatterns = [
    path('',views.home,name='home'),
    path('set-currency/', views.set_currency, name='set_currency'),
    path("catalog/", views.catalog, name="catalog"),
    path("catalog/<slug:slug>/", views.category_products, name="category_products"),
    path('profile/',views.profile_view,name='profile'),
]