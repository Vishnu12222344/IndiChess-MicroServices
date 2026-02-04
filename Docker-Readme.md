# 🐳 IndiChess — Docker Setup Guide

Everything you need to build, run, and manage the **IndiChess** microservices stack using Docker — no Java, Maven, or Node.js install required on your machine.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Project Folder Structure](#project-folder-structure)
- [How the Dockerfiles Work](#how-the-dockerfiles-work)
- [Step 1 — Build All Images](#step-1--build-all-images)
- [Step 2 — Verify Images](#step-2--verify-images)
- [Step 3 — Run Everything with Docker Compose](#step-3--run-everything-with-docker-compose)
- [Step 4 — Verify Everything Is Running](#step-4--verify-everything-is-running)
- [Port Reference](#port-reference)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Quick-Start Cheat Sheet](#quick-start-cheat-sheet)

---

## Prerequisites

| Software | Why |
|---|---|
| [Docker Desktop](https://www.docker.com/products/docker-desktop) | Builds and runs all containers |
| [Git](https://git-scm.com/) | To clone the repository |

> ✅ That's it. Java, Maven, and Node.js are all handled **inside** the Docker builds. You do **not** need to install them on your machine.

---

## Project Folder Structure

After cloning, your root folder should look like this:

```
IndiChess-MicroServices/
│
├── Eureka-Server/                  ← Service registry
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile                  ← Builds eureka-server image
│
├── Api-Gateway/                    ← API Gateway
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile                  ← Builds api-gateway image
│
├── User-Service/                   ← User auth & OAuth2
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile                  ← Builds user-service image
│
├── Match-Service/                  ← Game logic & WebSocket
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile                  ← Builds match-service image
│
├── frontend/                       ← React chess app
│   ├── src/
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile                  ← Builds frontend image
│
└── docker-compose.yml              ← Starts all containers together (root)
```

---

## How the Dockerfiles Work

All the Java service Dockerfiles use a **two-stage build** pattern to keep the final image small:

```
Stage 1 (Build)     →    Stage 2 (Run)
─────────────────        ─────────────────
maven image              slim JRE image
  • copies pom.xml         • copies only the .jar
  • downloads deps         • runs the app
  • compiles code
  • produces .jar
```

| Dockerfile Location | Build Image | Runtime Image | Expose Port |
|---|---|---|---|
| `Eureka-Server/Dockerfile` | `maven:3.9-eclipse-temurin-21` | `eclipse-temurin:21-jre-alpine` | 8761 |
| `Api-Gateway/Dockerfile` | `maven:3.9-eclipse-temurin-21` | `eclipse-temurin:21-jre-alpine` | 8080 |
| `User-Service/Dockerfile` | `maven:3.9-eclipse-temurin-21` | `eclipse-temurin:21-jre-alpine` | 8085 |
| `Match-Service/Dockerfile` | `maven:3.9-eclipse-temurin-21` | `eclipse-temurin:21-jre-alpine` | 8082 |
| `frontend/Dockerfile` | `node:20-alpine` | `node:20-alpine` + `serve` | 8081 |

MySQL (`mysql:8.0`) is an **official image** — it is pulled automatically by Docker Compose. You do not need to build it.

---

## Step 1 — Build All Images

Run each command **from inside** the service folder. Since the `Dockerfile` is already in that folder, you don't need the `-f` flag.

### 1.1 Eureka Server

```bash
cd Eureka-Server
docker build -t eureka-server .
```

### 1.2 API Gateway

```bash
cd Api-Gateway
docker build -t api-gateway .
```

### 1.3 User Service

```bash
cd User-Service
docker build -t user-service .
```

### 1.4 Match Service

```bash
cd Match-Service
docker build -t match-service .
```

### 1.5 Frontend

```bash
cd frontend
docker build -t frontend .
```

> ⏱️ The first build of each image takes **2–5 minutes** because Maven / npm download dependencies. After that, rebuilds are fast because Docker caches layers.

---

## Step 2 — Verify Images

Run this to confirm all images were built:

```bash
docker images
```

You should see all of these:

| Image | Tag | Size (approx) |
|---|---|---|
| `eureka-server` | `latest` | ~390 MB |
| `api-gateway` | `latest` | ~380 MB |
| `user-service` | `latest` | ~450 MB |
| `match-service` | `latest` | ~445 MB |
| `frontend` | `latest` | ~215 MB |
| `mysql` | `8.0` | ~1.08 GB |

---

## Step 3 — Run Everything with Docker Compose

One command starts **all six containers** in the correct order:

```bash
docker compose up -d
```

The `-d` flag runs them in the background (detached mode).

### How the Startup Order Works

`docker-compose.yml` uses `depends_on` with `healthchecks` so containers start in the right order automatically:

```
MySQL (healthcheck)
    ↓
Eureka Server (healthcheck)
    ↓
User Service + Match Service    ← wait for BOTH MySQL & Eureka to be healthy
    ↓
API Gateway + Frontend
```

| Container | Waits For | Why |
|---|---|---|
| `mysql` | nothing | Starts first. Has a healthcheck so others know when it is actually ready |
| `eureka-server` | nothing | Starts alongside MySQL. Has its own healthcheck |
| `user-service` | MySQL ✅ + Eureka ✅ | Needs the database and service registry |
| `match-service` | MySQL ✅ + Eureka ✅ | Needs the database and service registry |
| `api-gateway` | Eureka ✅ | Needs service registry to discover other services |
| `frontend` | nothing | Just serves static files, no dependencies |

### Why MySQL Has No Port Mapping

MySQL **does not** expose port `3306` to your host machine on purpose. If you have MySQL installed locally on Windows, it already uses `3306` and it would clash. The containers talk to each other internally using the container name `mysql` — no host port needed.

---

## Step 4 — Verify Everything Is Running

### Check container status

```bash
docker ps
```

All 6 should show `Up`:

| Container | Ports | Status |
|---|---|---|
| `mysql` | (internal only) | Up |
| `eureka-server` | `0.0.0.0:8761 → 8761` | Up |
| `api-gateway` | `0.0.0.0:8080 → 8080` | Up |
| `user-service` | `0.0.0.0:8085 → 8085` | Up |
| `match-service` | `0.0.0.0:8082 → 8082` | Up |
| `frontend` | `0.0.0.0:8081 → 8081` | Up |

### Open in your browser

| What | URL |
|---|---|
| **Chess App (Frontend)** | `http://localhost:8081` |
| **Eureka Dashboard** | `http://localhost:8761` |
| **API Gateway Health** | `http://localhost:8080/actuator/health` |

---

## Port Reference

| Service | Port | Protocol | Purpose |
|---|---|---|---|
| MySQL | 3306 | TCP | Database — internal only, not exposed to host |
| Eureka Server | 8761 | HTTP | Service discovery dashboard + registry |
| API Gateway | 8080 | HTTP | All REST API calls from the frontend |
| User Service | 8085 | HTTP | Auth, login, OAuth2 (routed via Gateway) |
| Match Service | 8082 | HTTP + WebSocket | Game logic + live chess via WebSocket |
| Frontend | 8081 | HTTP | React chess app |

### API Gateway Route Map

The Gateway automatically forwards requests to the correct service:

| URL Pattern | Forwards To |
|---|---|
| `/auth/**`, `/user/**`, `/oauth2/**`, `/login/oauth2/**` | User Service |
| `/match/**` | Match Service |
| `/ws/**` | Match Service (WebSocket) |

---

## Useful Commands

| What | Command |
|---|---|
| Start all | `docker compose up -d` |
| Stop all | `docker compose down` |
| Stop all + delete DB data | `docker compose down -v` |
| Restart one service | `docker compose restart match-service` |
| View live logs of one service | `docker compose logs -f user-service` |
| Rebuild one image + restart | `cd Match-Service && docker build -t match-service .` then `cd .. && docker compose up -d match-service` |
| See all running containers | `docker ps` |
| See all images | `docker images` |

---

## Troubleshooting

### ❌ Port 3306 already in use

You have MySQL installed locally on Windows and it is already using `3306`. Make sure your `docker-compose.yml` does **not** have a `ports` section under `mysql`. The containers talk internally — no host port mapping is needed.

### ❌ Port 8080 / 8081 / 8082 already in use

Something else on your machine is using that port. Find and close it:

```powershell
# PowerShell — find what is using port 8080
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Select-Name, Id
```

Then either close that process or change the host port in `docker-compose.yml`.

### ❌ User Service or Match Service keeps restarting

Almost always means **MySQL is not ready yet**. It can take 30–60 seconds for MySQL to fully initialize the first time. Wait a bit, then check logs:

```bash
docker compose logs -f user-service
```

If it says something like `Access denied` or `Communications link failure`, MySQL is still starting up. Just run `docker compose up -d` again after waiting.

### ❌ Services not showing in Eureka Dashboard

Open `http://localhost:8761` — services can take up to **30 seconds** to register after they start. If they still do not show up after a minute, check their logs:

```bash
docker compose logs -f user-service
docker compose logs -f match-service
```

### ❌ CORS error in the browser

Make sure you are opening the app at exactly `http://localhost:8081`. The API Gateway only allows `localhost:8081` and `localhost:5173`. Do not use `127.0.0.1` or any other address.

### ❌ WebSocket not connecting (live chess not working)

The frontend connects to Match Service WebSocket directly on port `8082`. Make sure:
- `match-service` container is running (`docker ps`)
- You are on `http://localhost:8081` (not a different address)
- Try a hard refresh: `Ctrl + Shift + R`

---

## Quick-Start Cheat Sheet

Already know the setup? Here is everything in one block, copy-paste ready:

```bash
# ── 1. Build all images ────────────────────────────────────────
cd Eureka-Server    && docker build -t eureka-server  . && cd ..
cd Api-Gateway      && docker build -t api-gateway    . && cd ..
cd User-Service     && docker build -t user-service   . && cd ..
cd Match-Service    && docker build -t match-service  . && cd ..
cd frontend         && docker build -t frontend       . && cd ..

# ── 2. Start everything ────────────────────────────────────────
docker compose up -d

# ── 3. Verify ──────────────────────────────────────────────────
docker ps

# ── 4. Open the app ───────────────────────────────────────────
# → http://localhost:8081

# ── 5. Stop everything ─────────────────────────────────────────
docker compose down
```

---

> 🎮 If everything starts and you can open **http://localhost:8081** — the full IndiChess stack is running. Good luck!
