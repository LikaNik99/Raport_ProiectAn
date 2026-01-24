from decimal import Decimal
from datetime import date
from customer.models import ExchangeRate
from .models import Cart


def convert_price(price_mdl, currency):
    try:
        price_mdl = Decimal(str(price_mdl))
    except Exception:
        return Decimal("0.00")

    if currency == "MDL":
        return price_mdl

    rate = ExchangeRate.objects.filter(date=date.today()).first()
    if not rate:
        return price_mdl

    value = getattr(rate, currency.lower(), None)
    if not value:
        return price_mdl

    return (price_mdl / value).quantize(Decimal("0.01"))



def get_or_create_cart(request):
    cart_id = request.session.get("cart_id")

    if cart_id:
        cart = Cart.objects.filter(id=cart_id).first()
        if cart:
            return cart

    cart = Cart.objects.create(
        user=request.user if request.user.is_authenticated else None
    )
    request.session["cart_id"] = cart.id
    return cart



def get_item_price(item):
    return item.price


def get_item_stock(item):
    if item.variant:
        return item.variant.stock
    return item.product.stock
