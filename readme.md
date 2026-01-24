<!-- run app  -->
daphne -b 0.0.0.0 -p 8000 baby_sugar.asgi:application

<!-- docker -->
docker compose up --build -d
docker compose down
docker compose exec web python manage.py createsuperuser
