# Simple UI

A single self-contained `index.html` (no build step, no dependencies) that
exercises the whole API: register/login, browse hotels by city, view room
types/rooms/amenities/reviews, book a room, pay, cancel, mark completed,
and leave a review. Also includes basic HOTEL_MANAGER tools (create
hotel/room type/room/amenity) and an ADMIN role-change form.

## Usage

1. Start the backend (`docker-compose up --build`, or run it from your IDE).
2. Just open `frontend/index.html` directly in a browser — no server needed,
   since the backend's CORS is wide open for local dev (see `SecurityConfig`).
3. If your backend isn't on `http://localhost:8080`, change the "API адрес
   сервера" field at the top of the page.

## Notes

- The JWT is kept in `localStorage`, so refreshing the page keeps you
  logged in.
- There's no "list users" endpoint yet, so the admin role-change form
  needs a user id typed in manually (check the DB, or the network tab
  after registering).
- This is intentionally plain HTML/CSS/JS — no framework, no bundler —
  so it's easy to read end-to-end and easy to point at during a defense.
