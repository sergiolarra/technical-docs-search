# ---- Build the Angular application ----
FROM node:24-alpine AS build
WORKDIR /app

# Dependencies first: reused while package-lock.json does not change
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Serve the static files and proxy the API with nginx ----
FROM nginx:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
  CMD wget -qO- http://127.0.0.1/ > /dev/null || exit 1
