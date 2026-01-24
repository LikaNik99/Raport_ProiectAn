from django.db import models
from django.contrib.auth.models import AbstractUser
from itsdangerous import URLSafeTimedSerializer
from django.conf import settings
from decimal import Decimal
from orders.models import Order
from product.models import Product, ProductVariant
from django.db import models
from django.conf import settings

class ExchangeRate(models.Model):
    date = models.DateField(unique=True)
    usd = models.DecimalField(max_digits=10, decimal_places=4, default=0)
    eur = models.DecimalField(max_digits=10, decimal_places=4, default=0)
    ron = models.DecimalField(max_digits=10, decimal_places=4, default=0)

    def __str__(self):
        return f"Curs {self.date}"



class User(AbstractUser):
    token = models.CharField(max_length=250, null=True, blank=True)
    is_subscribed = models.BooleanField(default=True)
    is_banned = models.BooleanField(default=False)
    preferred_currency = models.CharField(max_length=3,choices=[('MDL', 'MDL'),('USD', 'USD'),('EUR', 'EUR'),('RON', 'RON')], default='MDL')
    is_online = models.BooleanField(default=False)

    USERNAME_FIELD = 'username'
    REQUIRED_FIELDS = ['email']

    def __str__(self):
        return f"{self.username} | {self.email}"

    def generate_reset_token(self, expires_sec=3600):
        s = URLSafeTimedSerializer(settings.SECRET_KEY)
        return s.dumps(self.id, salt='reset-password')

    @staticmethod
    def verify_reset_token(token, expires_sec=3600):
        s = URLSafeTimedSerializer(settings.SECRET_KEY)
        try:
            user_id = s.loads(token, salt='reset-password', max_age=expires_sec)
        except:
            return None
        return User.objects.filter(id=user_id).first()

    def ban(self):
        self.is_banned = True
        self.save()

    def unban(self):
        self.is_banned = False
        self.save()



class Profile(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    phone_number = models.CharField(max_length=20, blank=True, null=True)
    address = models.CharField(max_length=255, blank=True, null=True)
    department = models.CharField(max_length=100, blank=True, null=True)
    role = models.CharField(max_length=50, blank=True, null=True)
    profile_picture = models.ImageField(upload_to='profiles/', blank=True, null=True)
    date_of_birth = models.DateField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    theme = models.CharField(max_length=10,choices=[("light","Light"),("dark","Dark")],default="light")

    def __str__(self):
        return f"{self.user.username} | {self.role or 'No role'}"



class Notification(models.Model):
    recipient = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='notifications'
    )
    title = models.CharField(max_length=255)
    message = models.TextField()
    
    sender = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="sent_notifications"
    )

    event_key = models.CharField(
        max_length=100,
        null=True,
        blank=True,
        db_index=True
    )
    
    order = models.ForeignKey(
        Order,
        null=True,
        blank=True,
        on_delete=models.CASCADE,
        related_name="notifications"
    )

    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("recipient", "event_key")

    def __str__(self):
        return f"Notification for {self.recipient.username}: {self.title}"



class Cart(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        null=True,
        blank=True
    )
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Cart #{self.id}"
    


class CartItem(models.Model):
    cart = models.ForeignKey(Cart, on_delete=models.CASCADE, related_name="items")
    product = models.ForeignKey(Product, on_delete=models.CASCADE)
    variant = models.ForeignKey(ProductVariant, on_delete=models.CASCADE, null=True, blank=True)
    quantity = models.PositiveIntegerField(default=1)
    
    price = models.DecimalField(max_digits=10, decimal_places=2)

    class Meta:
        unique_together = ("cart", "product", "variant")

    @property
    def line_total(self):
        return (self.price * self.quantity).quantize(Decimal("0.01"))



class Wishlist(models.Model):
    profile = models.OneToOneField(
        Profile,
        on_delete=models.CASCADE,
        related_name="wishlist"
    )
    products = models.ManyToManyField(
        Product,
        blank=True,
        related_name="wishlists"
    )
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Wishlist | {self.profile.user.username}"