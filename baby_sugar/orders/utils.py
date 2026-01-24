import json
from product.models import Product

def get_cart_from_cookies(request):
    cart = request.COOKIES.get("cart")
    if not cart:
        return {}

    try:
        return json.loads(cart)
    except json.JSONDecodeError:
        return {}

def cart_summary(cart):
    items = []
    total = 0

    for product_id, data in cart.items():
        product = Product.objects.get(id=product_id)

        subtotal = product.price * data["qty"]
        total += subtotal

        items.append({
            "name": product.name,
            "price": product.price,
            "qty": data["qty"]
        })

    return items, total
