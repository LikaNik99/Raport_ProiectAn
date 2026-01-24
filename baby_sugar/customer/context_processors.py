from customer.services.exchange import get_or_create_daily_rates


def currency(request):
    get_or_create_daily_rates()

    if request.user.is_authenticated:
        currency = request.user.preferred_currency
    else:
        currency = request.COOKIES.get("currency", "MDL")

    return {"currency": currency}
