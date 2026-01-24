from customer.services.notify import notify_user

def process_order(order):
    order.status = "processed"
    order.save(update_fields=["status"])

    if order.user:
        notify_user(
            recipient=order.user,
            sender=None,  # system
            title="Comandă procesată",
            message=f"Comanda #{order.id} a fost procesată.",
            event_key=f"order_{order.id}_processed",
            order=order,
        )

def build_items_summary(cart_items):
    lines = []

    for i, item in enumerate(cart_items, start=1):
        variant = item.variant

        color = variant.color.name if variant and variant.color else "-"
        size = variant.size.value if variant and variant.size else "-"
        age = variant.age.label if variant and variant.age else "-"
        code = item.product.code if hasattr(item.product, "code") else "-"

        lines.append(
            f"""{i}. {item.product.name}
            - Variantă: {color}
            - Mărime: {size} cm
            - Vârstă: {age} luni
            - Cod: #{code}
            - Cantitate: {item.quantity}
                """
                )

    return "\n".join(lines)
