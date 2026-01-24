from django.urls import resolve, reverse
from product.models import Product, Category
from orders.models import Order

def breadcrumbs(request):
    crumbs = [
        {"label": "Acasă", "url": reverse("main:home")}
    ]

    if request.path == "/":
        return {"breadcrumbs": []}

    match = resolve(request.path)
    url_name = match.url_name

    if url_name == "catalog":
        crumbs.append({"label": "Catalog", "url": ""})

    elif url_name == "category_products":
        category = Category.objects.filter(slug=match.kwargs.get("slug")).first()
        if category:
            crumbs.extend([
                {"label": "Catalog", "url": reverse("main:catalog")},
                {"label": category.name, "url": ""}
            ])

    elif url_name == "product_detail":
        product = (
            Product.objects
            .select_related("category")
            .filter(slug=match.kwargs.get("slug"))
            .first()
        )

        if product:
            crumbs.extend([
                {"label": "Catalog", "url": reverse("main:catalog")},
                {
                    "label": product.category.name,
                    "url": reverse("main:category_products", args=[product.category.slug])
                },
                {"label": product.name, "url": ""}
            ])


    elif url_name == "cart":
        crumbs.append({"label": "Coș", "url": ""})


    elif url_name == "profile":
        crumbs.append({"label": "Profil", "url": ""})

    elif url_name == "profile_edit":
        crumbs.extend([
            {"label": "Profil", "url": reverse("main:profile")},
            {"label": "Editare", "url": ""}
        ])


    elif url_name == "orders":
        crumbs.append({"label": "Comenzi", "url": ""})

    elif url_name == "order_detail":
        order_id = match.kwargs.get("order_id")
        crumbs.extend([
            {"label": "Comenzi", "url": reverse("orders:orders")},
            {"label": f"Comanda #{order_id}", "url": ""}
        ])


    else:
        crumbs.append({
            "label": url_name.replace("_", " ").title(),
            "url": ""
        })

    return {"breadcrumbs": crumbs}
