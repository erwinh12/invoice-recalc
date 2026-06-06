# Stage 1: Build Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve con Nginx
FROM nginx:alpine
COPY --from=build /app/dist/invoice-app/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
