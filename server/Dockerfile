# Use Node.js 20 Alpine for smaller image size
FROM node:20-alpine

# Install build dependencies for better-sqlite3 (native module)
RUN apk add --no-cache python3 make g++ sqlite

# Set working directory
WORKDIR /app

# Copy package files first for better caching
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application source code
COPY . .

# Create uploads and data directories
RUN mkdir -p uploads data

# Expose the application port
EXPOSE 5555

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:5555/ || exit 1

# Start the application
CMD ["node", "src/server.js"]
