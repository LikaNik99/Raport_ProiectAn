from django.conf import settings
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.contrib.auth import get_user_model

from customer.models import Cart, Profile, Wishlist

User = get_user_model()


@receiver(post_save, sender=User)
def create_user_profile_cart_and_wishlist(sender, instance, created, **kwargs):
    if created:
        profile = Profile.objects.create(user=instance)

        Cart.objects.create(user=instance)

        Wishlist.objects.create(profile=profile)
