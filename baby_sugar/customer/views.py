from django.shortcuts import get_object_or_404, render,redirect
from django.contrib.auth.decorators import login_required
from django.http import JsonResponse
from django.views.decorators.http import require_POST
from .forms import RegisterForm
from django.contrib.auth import authenticate, login,logout
from django.http import HttpResponse
from product.models import Product, ProductVariant
from .models import CartItem,Wishlist,Profile,Notification
from .utils import get_or_create_cart, get_item_stock
from django.contrib import messages
from .forms import UserEditForm, ProfileEditForm
from django.utils.http import url_has_allowed_host_and_scheme


def register_view(request):
    next_url = request.GET.get("next") or request.POST.get("next")

    if request.user.is_authenticated:
        if next_url and url_has_allowed_host_and_scheme(
            next_url,
            allowed_hosts={request.get_host()},
            require_https=request.is_secure(),
        ):
            return redirect(next_url)
        return redirect("main:profile")

    if request.method == "POST":
        form = RegisterForm(request.POST)
        if form.is_valid():
            user = form.save()
            login(request, user)
            messages.success(request, "Cont creat cu succes")

            if next_url and url_has_allowed_host_and_scheme(
                next_url,
                allowed_hosts={request.get_host()},
                require_https=request.is_secure(),
            ):
                return redirect(next_url)

            return redirect("main:profile")

        else:
            messages.error(request, "Datele nu sunt valide")

    else:
        form = RegisterForm()

    return render(request, "main/register.html", {
        "form": form,
        "next": next_url
    })



def login_view(request):
    next_url = request.GET.get("next") or request.POST.get("next")

    if request.user.is_authenticated:
        if next_url and url_has_allowed_host_and_scheme(
            next_url,
            allowed_hosts={request.get_host()},
            require_https=request.is_secure(),
        ):
            return redirect(next_url)
        return redirect("main:profile")

    if request.method == "POST":
        username = request.POST.get("username")
        password = request.POST.get("password")

        user = authenticate(request, username=username, password=password)

        if user is None:
            messages.error(request, "Username sau parolă incorectă")
            return redirect("customer:login")

        if user.is_banned:
            messages.error(request, "Contul tău este blocat")
            return redirect("customer:login")

        login(request, user)
        messages.success(request, "Autentificare reușită")

        cookie_currency = request.COOKIES.get("currency")
        if cookie_currency and cookie_currency != user.preferred_currency:
            user.preferred_currency = cookie_currency
            user.save(update_fields=["preferred_currency"])

        if next_url and url_has_allowed_host_and_scheme(
            next_url,
            allowed_hosts={request.get_host()},
            require_https=request.is_secure(),
        ):
            return redirect(next_url)

        return redirect("main:profile")

    return render(request, "main/login.html", {
        "next": next_url
    })




def logout_view(request):
    logout(request)
    messages.success(request, "Ai fost delogat cu succes.")
    return redirect("main:home")



def cart(request):
    cart = get_or_create_cart(request)
    cart_items = cart.items.select_related("product", "variant")
    total = sum(item.price * item.quantity for item in cart_items)

    return render(request, "customer/cart.html", {
        "cart_items": cart_items, "total": total,})



def add_to_cart(request, product_id):
    if request.method != "POST":
        return redirect("customer:cart")

    cart = get_or_create_cart(request)
    product = get_object_or_404(Product, id=product_id)
    quantity = int(request.POST.get("quantity", 1))
    variant_id = request.POST.get("variant_id")
    variant = None

    if product.variants.exists():
        if not variant_id:
            messages.error(request, "Selectează o variantă.")
            return redirect("product:product_detail", slug=product.slug)

        variant = get_object_or_404(ProductVariant, id=variant_id, product=product)
        available_stock = variant.stock
        unit_price = variant.calculated_price
    else:
        available_stock = product.stock
        unit_price = product.active_price

    if quantity > available_stock:
        messages.error(request, f"Stoc disponibil: {available_stock}")
        return redirect("product:product_detail", slug=product.slug)

    cart_item, created = CartItem.objects.get_or_create(
        cart=cart,
        product=product,
        variant=variant,
        defaults={"quantity": quantity, "price": unit_price,})

    if not created:
        new_qty = cart_item.quantity + quantity
        if new_qty > available_stock:
            messages.error(request, f"Poți avea maxim {available_stock} bucăți.")
            return redirect("customer:cart")

        cart_item.quantity = new_qty
        cart_item.save()

    messages.success(request, "Produs adăugat în coș.")
    return redirect("customer:cart")



def cart_plus(request, item_id):
    cart = get_or_create_cart(request)
    item = get_object_or_404(CartItem, id=item_id, cart=cart)
    stock = get_item_stock(item)

    if item.quantity + 1 > stock:
        messages.error(request, "Stoc insuficient")
    else:
        item.quantity += 1
        item.save()

    return redirect("customer:cart")



def cart_minus(request, item_id):
    cart = get_or_create_cart(request)
    item = get_object_or_404(CartItem, id=item_id, cart=cart)

    if item.quantity > 1:
        item.quantity -= 1
        item.save()
    else:
        item.delete()

    return redirect("customer:cart")



def cart_remove(request, item_id):
    cart = get_or_create_cart(request)
    item = get_object_or_404(CartItem, id=item_id, cart=cart)
    item.delete()
    messages.success(request,"articol sters cu success!")
    return redirect("customer:cart")



@login_required
def wishlist_view(request):
    profile = request.user.profile
    wishlist, _ = Wishlist.objects.get_or_create(profile=profile)
    products = wishlist.products.all()
    
    return render(request,"customer/wishlist.html",context={"products":products})



@require_POST
@login_required
def toggle_wishlist(request, product_slug):
    product = get_object_or_404(Product, slug=product_slug)

    profile, _ = Profile.objects.get_or_create(user=request.user)
    wishlist, _ = Wishlist.objects.get_or_create(profile=profile)

    if wishlist.products.filter(id=product.id).exists():
        wishlist.products.remove(product)
        messages.success(request,"articol adaugat la lista de dorinte")
    else:
        wishlist.products.add(product)
        messages.success(request,"articol sters cu success din liata de dorinte!")

    return redirect(request.META.get("HTTP_REFERER", "main:home"))



@login_required
def profile_edit(request):
    user = request.user
    profile, _ = Profile.objects.get_or_create(user=user)

    if request.method == "POST":
        user_form = UserEditForm(request.POST, instance=user)
        profile_form = ProfileEditForm(
            request.POST, request.FILES, instance=profile
        )

        if user_form.is_valid() and profile_form.is_valid():
            user_form.save()
            profile_form.save()
            messages.info(request, "Profil actualizat.")
            return redirect("main:profile")
        else:
            messages.error(request, "Verifică datele introduse.")
    else:
        user_form = UserEditForm(instance=user)
        profile_form = ProfileEditForm(instance=profile)

    return render(request, "customer/profile_edit.html", {
        "user_form": user_form,"profile_form": profile_form,"user": user,})



@require_POST
@login_required
def toggle_subscription(request):
    user = request.user
    user.is_subscribed = not user.is_subscribed
    user.save(update_fields=["is_subscribed"])
    return redirect("main:profile")



@login_required
def inbox(request):
    notifications = (Notification.objects
        .filter(recipient=request.user)
        .order_by("-created_at"))
    
    unread_count = notifications.filter(is_read=False).count()

    return render(request, "main/inbox.html",
        {"notifications": notifications,"unread_count": unread_count,})



@login_required
def mark_notification_read(request, pk):
    if request.method != "POST":
        return redirect("customer:inbox")

    notification = get_object_or_404(
        Notification,
        pk=pk,
        recipient=request.user
    )

    notification.is_read = True
    notification.save(update_fields=["is_read"])

    return redirect("customer:inbox")



@login_required
def mark_notifications_bulk(request):
    if request.method != "POST":
        return redirect("customer:inbox")

    ids = request.POST.getlist("ids")

    if not ids:
        return redirect("customer:inbox")

    Notification.objects.filter(id__in=ids,recipient=request.user).update(is_read=True)
    return redirect("customer:inbox")



@login_required
def delete_notifications_bulk(request):
    if request.method != "POST":
        return redirect("customer:inbox")

    ids = request.POST.getlist("ids")

    if not ids:
        return redirect("customer:inbox")

    Notification.objects.filter(id__in=ids,recipient=request.user).delete()

    return redirect("customer:inbox")



@login_required
def notification_detail(request, pk):
    notification = get_object_or_404(
        Notification, id=pk,
        recipient=request.user)

    if not notification.is_read:
        notification.is_read = True
        notification.save(update_fields=["is_read"])

    return render(request, "customer/notification_detail.html", {"notification": notification})



@login_required
def inbox_unread_count(request):
    count = Notification.objects.filter(
        recipient=request.user,
        is_read=False
    ).count()
    return JsonResponse({"count": count})


@login_required
def toggle_theme(request):
    profile = request.user.profile
    profile.theme = "dark" if profile.theme == "light" else "light"
    profile.save()
    return redirect(request.META.get("HTTP_REFERER", "/"))