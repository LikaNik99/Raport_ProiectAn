from django.contrib.auth.decorators import login_required
from django.contrib.admin.views.decorators import staff_member_required
from django.shortcuts import get_object_or_404, render, redirect
from django.http import HttpResponseForbidden
from django.db import transaction
from django.core.exceptions import ValidationError
from django.contrib import messages
from decimal import Decimal
from .models import Order, OrderItem
from product.models import ProductVariant, Product
from .services import process_order
from customer.models import Cart,ExchangeRate
from .services import build_items_summary
from django.urls import reverse


@login_required
def my_orders(request):
    orders = (
            Order.objects
            .filter(user=request.user)
            .prefetch_related("items", "invoice")
            .order_by("-created_at")
        )

    return render(request, "orders/my_orders.html", { "orders":orders})


@login_required
def order_detail(request, order_id):
    order = get_object_or_404(Order, id=order_id)

    if request.user != order.user and not request.user.is_staff:
        return HttpResponseForbidden("Nu ai acces la această comandă.")

    return render(request, "orders/order_detail.html", {"order": order})

@login_required
@transaction.atomic
def place_order(request):
    try:
        if request.method != "POST":
            return redirect("customer:cart")

        cart_id = request.session.get("cart_id")
        if not cart_id:
            return redirect("customer:cart")

        cart = Cart.objects.filter(id=cart_id).first()
        if not cart or not cart.items.exists():
            return redirect("customer:cart")

        rates = ExchangeRate.objects.order_by("-date").first()
        currency = request.user.preferred_currency

        rate_map = {
            "MDL": Decimal("1"),
            "EUR": rates.eur,
            "USD": rates.usd,
            "RON": rates.ron,
        }
        rate = rate_map.get(currency, Decimal("1"))

        cart_items = cart.items.select_related("product", "variant")

        total_converted = Decimal("0.00")


        order = Order.objects.create(
            user=request.user,
            currency=currency,
            currency_rate=rate,
            status="new",
            total_price=Decimal("0.00"),
        )


        for item in cart_items:

            if item.variant_id:
                variant = (
                    ProductVariant.objects
                    .select_for_update()
                    .filter(id=item.variant_id)
                    .first()
                )

                if not variant:
                    raise ValidationError(
                        f"Varianta produsului {item.product.name} nu mai există."
                    )

                if variant.stock < item.quantity:
                    raise ValidationError(
                        f"Stoc insuficient pentru {item.product.name} "
                        f"({variant.stock} disponibile)"
                    )


                variant.stock -= item.quantity
                variant.save(update_fields=["stock"])


                item.product.stock -= item.quantity
                item.product.save(update_fields=["stock"])

                stock_product = variant.product
                color = variant.color.name if variant.color else ""
                size = variant.size.value if variant.size else ""
                age = variant.age.label if variant.age else ""

            else:
                product = (
                    Product.objects
                    .select_for_update()
                    .get(id=item.product_id)
                )

                if product.stock < item.quantity:
                    raise ValidationError(
                        f"Stoc insuficient pentru {product.name} "
                        f"({product.stock} disponibile)"
                    )

                product.stock -= item.quantity
                product.save(update_fields=["stock"])

                stock_product = product
                color = ""
                size = ""
                age = ""

            price_converted = (item.price / rate).quantize(Decimal("0.01"))
            line_total = price_converted * item.quantity
            total_converted += line_total

            OrderItem.objects.create(
                order=order,
                product_name=stock_product.name,
                product_code=stock_product.code,
                color=color,
                size=size,
                age=age,
                price=price_converted,
                quantity=item.quantity,
            )


        order.total_price = total_converted.quantize(Decimal("0.01"))
        order.items_summary = build_items_summary(cart_items)
        order.save(update_fields=["total_price", "items_summary"])

        cart.items.all().delete()
        request.session.pop("cart_id", None)

        messages.success(request, "Comanda a fost plasată cu succes!")
        return redirect("orders:orders")

    except ValidationError as e:
        messages.error(request, str(e))
        return redirect("customer:cart")


@staff_member_required
def process_order_view(request, order_id):
    order = get_object_or_404(Order, id=order_id)

    process_order(order)

    return redirect("admin:orders_order_changelist")


