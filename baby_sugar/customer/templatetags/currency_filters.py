from django import template
from customer.utils import convert_price

register = template.Library()

@register.filter
def price_in(price, currency):
    return convert_price(price, currency)
