from django.db import models
from django.utils import timezone
from django.utils.text import slugify
from django.core.exceptions import ValidationError
from decimal import Decimal
import re


class Product(models.Model):
    name = models.CharField(max_length=255)
    code = models.CharField(max_length=50, unique=True)
    description = models.TextField()
    price = models.DecimalField(max_digits=8, decimal_places=2)
    reduced_price = models.DecimalField(max_digits=8, decimal_places=2, null=True, blank=True)
    badge = models.CharField(max_length=50, null=True, blank=True)    
    stock = models.PositiveIntegerField(default=0)
    tags = models.CharField(max_length=255, null=True, blank=True)
    sales = models.PositiveIntegerField(default=0)
    slug = models.SlugField(unique=True,blank=True)
    category = models.ForeignKey("Category", on_delete=models.SET_NULL, null=True, blank=True, related_name="products") 
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)
    is_active = models.BooleanField(default=True)
    sizes = models.ManyToManyField("ProductSize", related_name="products", blank=True)
    ages  = models.ManyToManyField("ProductAge", related_name="products", blank=True)


    def __str__(self):
        return self.name


    @property
    def active_price(self):
        if self.reduced_price and self.reduced_price < self.price:
            return self.reduced_price
        return self.price


    @property
    def is_in_stock(self):
        return self.stock > 0


    @property
    def main_image(self):
        """
        Returnează prima imagine ca imagine principală
        """
        first_image = self.images.first()  # legătura related_name="images"
        return first_image.image.url if first_image else None
    
    def variants_json(self):
        return [
            {
                "id": v.id,
                "color": v.color.name if v.color else None,
                "size": str(v.size) if v.size else None,
                "stock": v.stock,
                "price": v.product.active_price,
                "image_url": v.product.main_image
            }
            for v in self.variants.select_related('color', 'size').all()
        ]

    def save(self, *args, **kwargs):
        if not self.slug:
            base_slug = slugify(self.name)
            slug = base_slug
            counter = 1

            while Product.objects.filter(slug=slug).exclude(pk=self.pk).exists():
                slug = f"{base_slug}-{counter}"
                counter += 1

            self.slug = slug

        super().save(*args, **kwargs)



class ProductSize(models.Model):
    value = models.CharField(max_length=20)  # ex: "56", "62", "68"
    
    def __str__(self):
        return self.value



class ProductAge(models.Model):
    label = models.CharField(max_length=50)
    def __str__(self):
        return self.label



class ProductColor(models.Model):
    name = models.CharField(max_length=50)
    hex_code = models.CharField(max_length=7, null=True, blank=True)

    def __str__(self):
        return self.name



class ProductVariant(models.Model):
    product = models.ForeignKey(Product, on_delete=models.CASCADE, related_name="variants")
    color = models.ForeignKey(ProductColor, on_delete=models.SET_NULL, null=True, blank=True)
    size = models.ForeignKey(ProductSize, on_delete=models.SET_NULL, null=True, blank=True)
    age = models.ForeignKey(ProductAge, on_delete=models.CASCADE, null=True, blank=True)
    stock = models.PositiveIntegerField(default=0)

    def clean(self):
        if not self.product_id:
            return

        total_variants_stock = (
            self.product.variants
            .exclude(pk=self.pk)
            .aggregate(total=models.Sum("stock"))["total"] or 0
        )

        if total_variants_stock + self.stock > self.product.stock:
            raise ValidationError(
                f"Stocul total al variantelor ({total_variants_stock + self.stock}) "
                f"depășește stocul produsului ({self.product.stock})."
            )

    def save(self, *args, **kwargs):
        self.full_clean()
        super().save(*args, **kwargs)

    def __str__(self):
        parts = []
        if self.color:
            parts.append(self.color.name)
        if self.size:
            parts.append(str(self.size))
        return f"{self.product.name} - {' / '.join(parts)}"

    # ---------------- PREȚ CALCULAT ----------------
    @property
    def calculated_price(self):
        """
        - dacă produsul are reducere → pornește de la reduced_price
        - mărime minimă → coeficient = 1
        - mărime maximă → coeficient = 1.3
        - creștere liniară pe pași de 6 cm
        """

        # 🔑 PREȚ DE BAZĂ (activ)
        base_price = (
            Decimal(self.product.reduced_price)
            if self.product.reduced_price is not None
            else Decimal(self.product.price)
        )

        if not self.size:
            return base_price.quantize(Decimal("0.01"))

        # CONFIG
        MIN_SIZE = 50
        MAX_SIZE = 116
        STEP = 6
        MAX_COEF = Decimal("1.1")

        # extrage cifra (ex: "56-62" → 56)
        raw_value = str(self.size.value)
        size_int = int(re.findall(r"\d+", raw_value)[0])

        # ⭐ mărime minimă → fără coeficient
        if size_int <= MIN_SIZE:
            return base_price.quantize(Decimal("0.01"))

        total_steps = (MAX_SIZE - MIN_SIZE) // STEP
        size_index = (size_int - MIN_SIZE) // STEP
        size_index = min(size_index, total_steps)  # 🔒 limitare

        coef = Decimal("1.0") + (
            Decimal(size_index) / Decimal(total_steps)
        ) * (MAX_COEF - Decimal("1.0"))

        return (base_price * coef).quantize(Decimal("0.01"))



class ProductImage(models.Model):
    product = models.ForeignKey(Product, related_name='images', on_delete=models.CASCADE)
    image = models.ImageField(upload_to='products/')
    alt_text = models.CharField(max_length=255, blank=True, null=True)
    is_main = models.BooleanField(default=False)  # imaginea principală opțional

    def __str__(self):
        return f"Image for {self.product.name}"



class Category(models.Model):
    name = models.CharField(max_length=100)
    slug = models.SlugField(unique=True)
    image = models.ImageField(
        upload_to="categories/",
        blank=True,
        null=True
    )
    is_active = models.BooleanField(default=True)
    order = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["order", "name"]
    
    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = slugify(self.name)
        super().save(*args, **kwargs)

    def __str__(self):
        return self.name

