from django.db import models
from django.conf import settings
from decimal import Decimal

from django.db import models
from django.conf import settings

class Order(models.Model):

    STATUS_CHOICES = [
        ('new', 'new'),
        ('processing', 'În procesare'),
        ('processed', 'Procesată'),
        ('cancelled', 'Anulată'),
    ]
    CURRENCY_CHOICES = [
        ("MDL", "MDL"),
        ("EUR", "EUR"),
        ("USD", "USD"),
        ("RON", "RON"),
    ]

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name='orders'
    )

    guest_email = models.EmailField(null=True, blank=True)

    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='new'
    )
    
    items_summary = models.TextField(
        verbose_name="Detalii comandă",
        blank=True
    )

    total_price = models.DecimalField(
        max_digits=10,
        decimal_places=2
    )
    
    currency = models.CharField(
        max_length=3,
        choices=CURRENCY_CHOICES,
        default="MDL"
    )
    
    currency_rate = models.DecimalField(
    max_digits=10,
    decimal_places=4,
    help_text="Rata față de MDL la momentul comenzii"
    )


    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        if self.user:
            return f"Order #{self.id} | {self.user.username}"
        return f"Order #{self.id} | guest"



class OrderItem(models.Model):

    order = models.ForeignKey(
        Order,
        related_name='items',
        on_delete=models.CASCADE
    )

    product_name = models.CharField(max_length=255)
    product_code = models.CharField(max_length=50,blank=True)
    color = models.CharField(max_length=50, blank=True)
    size = models.CharField(max_length=50, blank=True)
    age = models.CharField(max_length=50, blank=True)
    price = models.DecimalField(max_digits=10, decimal_places=2)
    quantity = models.PositiveIntegerField()

    def has_stock(self, qty):
            return self.stock >= qty
    
    @property
    def line_total(self):
        return (self.price * self.quantity).quantize(Decimal("0.01"))
    
    def __str__(self):
        return f"{self.product_name} x{self.quantity}"


class Invoice(models.Model):
    order = models.OneToOneField(Order, on_delete=models.CASCADE)
    number = models.CharField(max_length=50)
    issued_at = models.DateTimeField(auto_now_add=True)
    pdf = models.FileField(upload_to="invoices/", blank=True)

