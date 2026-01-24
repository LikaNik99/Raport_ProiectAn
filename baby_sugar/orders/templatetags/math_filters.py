from django import template
from decimal import Decimal

register = template.Library()

@register.filter
def mul(a, b):
    try:
        return (Decimal(a) * Decimal(b)).quantize(Decimal("0.01"))
    except Exception:
        return ""
