FROM node:22-alpine
RUN apk add --no-cache docker-cli docker-cli-compose
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts --no-audit
COPY *.mjs ./
ENV PORT=4200
EXPOSE 4200
CMD ["node", "server.mjs"]
