from django.contrib import admin
from django.utils.html import format_html

from .models import (
    Product,
    ProductVariant,
    ProductImage,
    ProductSize,
    ProductColor,
    ProductAge,
    Category
)

# =========================
# INLINE MODELS
# =========================

class ProductImageInline(admin.TabularInline):
    model = ProductImage
    extra = 1
    fields = ("image", "alt_text", "is_main", "image_preview")
    readonly_fields = ("image_preview",)

    def image_preview(self, obj):
        if obj.image:
            return format_html(
                '<img src="{}" style="height:80px; border-radius:6px;" />',
                obj.image.url
            )
        return "-"
    image_preview.short_description = "Preview"


class ProductVariantInline(admin.TabularInline):
    model = ProductVariant
    extra = 1
    fields = ("color", "size", "age", "stock")


# =========================
# PRODUCT ADMIN (CENTRAL)
# =========================

@admin.register(Product)
class ProductAdmin(admin.ModelAdmin):
    list_display = (
        "name",
        "category",
        "price",
        "reduced_price",
        "stock",
        "is_active",
    )
    list_filter = ("is_active", "category")
    search_fields = ("name", "code", "slug")
    readonly_fields = ("slug", "created_at", "updated_at")
    prepopulated_fields = {}  # slug este gestionat DOAR în model
    inlines = [ProductImageInline, ProductVariantInline]

    fieldsets = (
        ("Informații produs", {
            "fields": (
                "name",
                "code",
                "slug",
                "category",
                "description",
                "badge",
                "tags",
            )
        }),
        ("Prețuri", {
            "fields": (
                "price",
                "reduced_price",
            )
        }),
        ("Stoc & status", {
            "fields": (
                "stock",
                "sales",
                "is_active",
            )
        }),
        ("Meta", {
            "fields": (
                "created_at",
                "updated_at",
            )
        }),
    )

    def save_model(self, request, obj, form, change):
        """
        Asigură-te că există o singură imagine principală.
        """
        super().save_model(request, obj, form, change)

        main_images = obj.images.filter(is_main=True)
        if main_images.count() > 1:
            first = main_images.first()
            main_images.exclude(id=first.id).update(is_main=False)


# =========================
# CATEGORY ADMIN
# =========================

@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ("name", "is_active", "order")
    list_editable = ("is_active", "order")
    search_fields = ("name", "slug")
    readonly_fields = ("slug",)


# =========================
# SIMPLE LOOKUP ADMINS
# =========================

@admin.register(ProductSize)
class ProductSizeAdmin(admin.ModelAdmin):
    list_display = ("value",)
    search_fields = ("value",)


@admin.register(ProductAge)
class ProductAgeAdmin(admin.ModelAdmin):
    list_display = ("label",)
    search_fields = ("label",)


@admin.register(ProductColor)
class ProductColorAdmin(admin.ModelAdmin):
    list_display = ("name", "hex_code")
    search_fields = ("name",)

