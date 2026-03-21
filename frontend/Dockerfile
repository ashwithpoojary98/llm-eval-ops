# ── Stage 1: Install dependencies ─────────────────────────────────────────────
FROM node:20-alpine AS deps

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --frozen-lockfile

# ── Stage 2: Build Next.js ────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build-time env — override with real URL at deploy time
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

RUN npm run build

# ── Stage 3: Minimal runtime ──────────────────────────────────────────────────
FROM node:20-alpine AS runtime

WORKDIR /app
ENV NODE_ENV=production

RUN addgroup -S llmops && adduser -S llmops -G llmops

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

RUN chown -R llmops:llmops /app
USER llmops

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:3000 || exit 1

CMD ["node", "server.js"]
