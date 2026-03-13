# Stage 1: Build the Angular application
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the application code
COPY . .

# Build the project
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:alpine

# Copy the build output to replace the default nginx contents.
# With @angular-devkit/build-angular:application, root output is in `dist/salon-manager/browser`
COPY --from=build /app/dist/salon-manager/browser /usr/share/nginx/html

# Replace default Nginx configuration to enable Angular routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
