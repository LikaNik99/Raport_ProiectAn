from django import forms
from django.contrib.auth.forms import UserCreationForm
from django.contrib.auth import get_user_model
from .models import Profile

User = get_user_model()


class RegisterForm(UserCreationForm):
    email = forms.EmailField(
        required=True,
        widget=forms.EmailInput(attrs={
            "class": "form-control",
            "placeholder": "Email"
        })
    )

    class Meta:
        model = User
        fields = ("username", "email", "password1", "password2")

        widgets = {
            "username": forms.TextInput(attrs={
                "class": "form-control",
                "placeholder": "Username"
            }),
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.fields["password1"].widget.attrs.update({
            "class": "form-control",
            "placeholder": "Parolă"
        })

        self.fields["password2"].widget.attrs.update({
            "class": "form-control",
            "placeholder": "Confirmă parola"
        })

        self.fields["username"].label = "Username"
        self.fields["email"].label = "Email"
        self.fields["password1"].label = "Parolă"
        self.fields["password2"].label = "Confirmă parola"



class UserEditForm(forms.ModelForm):
    class Meta:
        model = User
        fields = ["first_name", "last_name", "email", "is_subscribed"]



class ProfileEditForm(forms.ModelForm):
    class Meta:
        model = Profile
        fields = ["phone_number", "address", "date_of_birth", "profile_picture", ]
        widgets = {
            "date_of_birth": forms.DateInput(
                attrs={"type": "date", "class": "form-control"}
            )
        }