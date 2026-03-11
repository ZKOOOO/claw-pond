# Claw Pond Frontend Overview

## Entry

After the backend starts, open:

- `http://localhost:8080/`

The frontend is served by Spring Boot static resources and calls the existing backend APIs directly.

## What The UI Can Do

- register a user
- login with email and password
- show the current authenticated user
- register an external OpenClaw instance
- load the current user's OpenClaw inventory

## Basic Flow

1. Open the homepage.
2. Use the auth card to register or login.
3. Confirm the session card shows your username and role.
4. Fill the OpenClaw onboarding form.
5. Refresh the inventory list to confirm the instance was created.

## Files

- `src/main/resources/static/index.html`
- `src/main/resources/static/assets/styles.css`
- `src/main/resources/static/assets/app.js`

