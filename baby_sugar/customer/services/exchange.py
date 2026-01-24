import requests
import xml.etree.ElementTree as ET
from decimal import Decimal
from datetime import date
from customer.models import ExchangeRate


def fetch_bnm_rates(target_date):
    url = (
        "https://www.bnm.md/ro/official_exchange_rates"
        f"?get_xml=1&date={target_date.strftime('%d.%m.%Y')}"
    )

    response = requests.get(url, timeout=10)
    response.raise_for_status()

    root = ET.fromstring(response.content)
    rates = {}

    for valute in root.findall(".//Valute"):
        code = valute.find("CharCode").text
        value = Decimal(valute.find("Value").text.replace(",", "."))
        nominal = Decimal(valute.find("Nominal").text)

        rates[code] = value / nominal

    return rates



def get_or_create_daily_rates():
    today = date.today()

    existing = ExchangeRate.objects.filter(date=today).first()
    if existing:
        return existing

    try:
        raw = fetch_bnm_rates(today)

        return ExchangeRate.objects.create(
            date=today,
            usd=raw.get("USD", 0),
            eur=raw.get("EUR", 0),
            ron=raw.get("RON", 0),
        )

    except Exception:
        # fallback – ultimul curs cunoscut
        return ExchangeRate.objects.order_by("-date").first()
