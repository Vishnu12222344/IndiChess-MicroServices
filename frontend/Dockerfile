# =========================
# Build stage
# =========================
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


# =========================
# Runtime stage
# =========================
FROM node:20-alpine

WORKDIR /app

# Install lightweight static server
RUN npm install -g serve

# Copy build output
COPY --from=build /app/dist ./dist

# React frontend port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081 || exit 1

# Serve React app
CMD ["serve", "-s", "dist", "-l", "8081"]
