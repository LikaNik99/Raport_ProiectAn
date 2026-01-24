from django.shortcuts import render,redirect,get_object_or_404
from django.http import HttpResponse,JsonResponse
from django.views.decorators.http import require_POST
from django.contrib import messages
from product.models import Category,Product,ProductImage
from django.contrib.auth.decorators import login_required
from customer.models import Profile
import random


def home(request):
    images = (
        ProductImage.objects
        .filter(product__is_active=True)
        .select_related("product")
    )

    images = list(images)
    random.shuffle(images)

    hero_images = images[:6]  # câte slide-uri vrei

    return render(request, "main/home.html", {
        "hero_images": hero_images,
    })



def catalog(request):
    categories = Category.objects.filter(is_active=True)
    return render(request, "main/catalog.html", {
        "categories": categories,
        "page_title": "Catalog",})


 
def category_products(request, slug):
    category = get_object_or_404(Category, slug=slug)
    products = Product.objects.filter(category=category)

    return render(request, "product/products.html", {
        "products": products, "category": category,
        "page_title": category.name})



@require_POST
def set_currency(request):
    currency = request.POST.get("currency", "MDL")

    if request.user.is_authenticated:
        request.user.preferred_currency = currency
        request.user.save(update_fields=["preferred_currency"])
        messages.info(request,f"Currency set as {currency}")
    else:
        response = redirect(request.META.get("HTTP_REFERER", "/"))
        response.set_cookie(
            "currency",
            currency,
            max_age=60 * 60 * 24 * 30,
            samesite="Lax",)
        return response

    return redirect(request.META.get("HTTP_REFERER", "/"))



@login_required
def profile_view(request):
    user = request.user
    profile, _ = Profile.objects.get_or_create(user=user)
    return render(request, "main/profile.html", {
        "user": user,"profile": profile,})