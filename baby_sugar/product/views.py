from django.shortcuts import render,get_object_or_404
from .models import Product,ProductVariant
from django.db.models import Q



def product_search(request):
    q = request.GET.get("q", "").strip()

    products = Product.objects.filter(is_active=True)

    if q:
        products = products.filter(
            Q(name__icontains=q) |
            Q(code__icontains=q) |
            Q(tags__icontains=q)
        ).distinct()
    
    template ="product/products_search.html"
    return render(request, template, {
        "products": products,"query": q,})



def all_products(request):
    products = Product.objects.filter(is_active=True).exclude(slug="")
    # print(products)
    template ="product/products.html"
    return render(request, template, {
        "products": products,
        "page_title": "Toate produsele"})



def product_detail(request, slug):
    product = get_object_or_404(Product, slug=slug)
    variants = product.variants.all()

    in_wish = False

    if request.user.is_authenticated:
        try:
            wishlist = request.user.profile.wishlist
            in_wish = wishlist.products.filter(id=product.id).exists()
        except Exception:
            in_wish = False
                
    template = "product/product_detail.html"
    return render(request, template, {"product": product,
        "variants": variants,"in_wish": in_wish,})



def variant_quantity(request, variant_id):
    variant = get_object_or_404(ProductVariant, id=variant_id)

    return render(request, "partials/quantity_input.html", {
        "variant": variant, "max": variant.stock})



def variant_price(request, variant_id):
    variant = get_object_or_404(ProductVariant, id=variant_id)

    return render(request,"partials/variant_price.html",{"variant": variant})



