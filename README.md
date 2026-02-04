
# IndiChess - Microservices Chess Platform
A full-stack, real-time multiplayer chess application built with microservices architecture. Play chess with friends through matchmaking or private matches, featuring real-time gameplay, live chat, and time controls.

![Chess Game](https://img.shields.io/badge/Status-Active-success)
![Microservices](https://img.shields.io/badge/Architecture-Microservices-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![React](https://img.shields.io/badge/React-18-61dafb)

🐳 [Docker Setup Guide](Docker-Readme.md)

---
## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Folder Structure](#-folder-structure)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

---

## ✨ Features

### Core Gameplay
- ⚡ **Real-time Multiplayer Chess** - Play with opponents in real-time using WebSocket
- 🎯 **Matchmaking System** - Quick match with players of similar skill
- 🔒 **Private Matches** - Create and share match IDs to play with friends
- ⏱️ **Time Controls** - Blitz (3+0) and Rapid (10+0) game modes
- 💬 **In-game Chat** - Communicate with your opponent during matches
- 🏆 **Game Outcomes** - Win, loss, draw, and resignation tracking

### Authentication
- 🔐 **JWT Authentication** - Secure token-based authentication
- 🌐 **OAuth 2.0 Integration** - Login with Google and GitHub
- 👤 **User Profiles** - Manage your account and view game history

### Technical Features
- 🎨 **Modern UI** - Beautiful, responsive interface with Tailwind CSS
- 🔄 **Service Discovery** - Eureka for automatic service registration
- 🌉 **API Gateway** - Centralized routing and load balancing
- 📡 **WebSocket Support** - Real-time bidirectional communication
- 🗄️ **PostgreSQL** - Reliable data persistence

---

## 🏗️ Architecture

This project follows a **microservices architecture** with the following services:

```
┌─────────────────────────────────────────────────────────────┐
│                        FRONTEND                             │
│                    React + TypeScript                       │
│                    (Port 5173/8081)                         │
└──────────────┬─────────────────────────┬────────────────────┘
               │                         │
       REST API│                         │WebSocket (Direct)
               │                         │
               ▼                         ▼
    ┌──────────────────┐      ┌──────────────────────┐
    │   API GATEWAY    │      │   MATCH SERVICE      │
    │   (Port 8080)    │      │   (Port 8082)        │
    └────────┬─────────┘      │  - Game Logic        │
             │                │  - WebSocket Server  │
             │                │  - Matchmaking       │
    ┌────────┴─────────┐      │  - Chat              │
    │                  │      └──────────────────────┘
    ▼                  ▼
┌─────────────┐  ┌──────────────────┐
│USER SERVICE │  │  MATCH SERVICE   │
│(Port 8085)  │  │  (Port 8082)     │
│- Auth       │  │- Match CRUD      │
│- JWT        │  │- REST APIs       │
│- OAuth2     │  └──────────────────┘
└─────────────┘
         │
         ▼
┌──────────────────┐
│  EUREKA SERVER   │
│  (Port 8761)     │
│- Service Registry│
└──────────────────┘
```

### Service Communication
- **REST APIs** → Routed through API Gateway (Port 8080)
- **WebSocket** → Direct connection to Match Service (Port 8082)
- **Service Discovery** → All services register with Eureka

---

## 🛠️ Tech Stack

### Backend
- **Java 17** - Primary programming language
- **Spring Boot 3.2** - Application framework
- **Spring Cloud** - Microservices infrastructure
  - Eureka Server - Service discovery
  - Spring Cloud Gateway - API Gateway
- **Spring Security** - Authentication & authorization
- **Spring WebSocket** - Real-time communication
- **PostgreSQL** - Primary database
- **JWT** - Token-based authentication
- **OAuth 2.0** - Social login (Google, GitHub)
- **Maven** - Dependency management

### Frontend
- **React 18** - UI framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - Component library
- **React Router** - Client-side routing
- **STOMP.js** - WebSocket client
- **SockJS** - WebSocket fallback

---

## 📁 Project Structure

```
IndiChess-MicroServices/
│
├── Api-Gateway/                 # API Gateway Service
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com.Api_Gateway/
│   │       │       └── ApiGatewayApplication.java
│   │       └── resources/
│   │           ├── application.properties
│   │           └── application.yaml    # Gateway routes & CORS config
│   └── pom.xml
│
├── Eureka-Server/              # Service Registry
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com.Eureka_Server/
│   │       │       └── EurekaServerApplication.java
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
│
├── Match-Service/              # Game Logic & WebSocket
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com.Match_Service/
│   │       │       ├── Config/
│   │       │       │   ├── SecurityConfig.java
│   │       │       │   ├── WebSocketConfig.java
│   │       │       │   └── WebSocketAuthInterceptor.java
│   │       │       ├── Controller/
│   │       │       │   ├── MatchController.java
│   │       │       │   ├── MatchmakingController.java
│   │       │       │   ├── GameWebSocketController.java
│   │       │       │   └── ChatController.java
│   │       │       ├── Service/
│   │       │       │   └── MatchService.java
│   │       │       ├── Model/
│   │       │       ├── Repository/
│   │       │       ├── Security/
│   │       │       └── dto/
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
│
├── User-Service/               # Authentication & User Management
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com.User_Service/
│   │       │       ├── Config/
│   │       │       ├── Controller/
│   │       │       ├── Service/
│   │       │       ├── Model/
│   │       │       ├── Repository/
│   │       │       └── Security/
│   │       └── resources/
│   │           ├── application.properties
│   │           ├── static/            # OAuth2 success pages
│   │           └── templates/
│   └── pom.xml
│
└── frontend/                   # React Frontend
    ├── src/
    │   ├── components/         # Reusable UI components
    │   ├── contexts/          # React contexts (Auth, etc.)
    │   ├── hooks/             # Custom React hooks
    │   ├── lib/               # Utilities and API client
    │   │   └── api.ts         # API service layer
    │   ├── pages/             # Page components
    │   │   ├── ChessGame.tsx  # Main game interface
    │   │   ├── Play.tsx       # Matchmaking page
    │   │   ├── CreateMatch.tsx
    │   │   ├── Dashboard.tsx
    │   │   ├── Login.tsx
    │   │   └── Register.tsx
    │   └── types/             # TypeScript type definitions
    ├── public/
    ├── package.json
    ├── tailwind.config.js
    ├── tsconfig.json
    └── vite.config.ts
```

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **Java 17 or higher** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **PostgreSQL 14+** - [Download](https://www.postgresql.org/download/)
- **Git** - [Download](https://git-scm.com/downloads)

### Optional (Recommended)
- **IntelliJ IDEA** or **Eclipse** - For Java development
- **VS Code** - For frontend development
- **Postman** - For API testing

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/IndiChess-MicroServices.git
cd IndiChess-MicroServices
```

### 2. Database Setup

#### Create Databases

```bash
# Connect to PostgreSQL //You guys can also use MySQL
psql -U postgres

# Create databases
CREATE DATABASE user_service_db;
CREATE DATABASE match_service_db;

# Exit psql
\q
```

#### Configure Database Credentials

Update database credentials in each service's `application.properties`:

**User-Service** (`User-Service/src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/user_service_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
```

**Match-Service** (`Match-Service/src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/match_service_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
```

### 3. Configure JWT Secret

Both User-Service and Match-Service need the **same JWT secret** for token validation.

**User-Service** (`application.properties`):
```properties
jwt.secret=your-very-long-secret-key-min-256-bits-recommended-for-hs384
```

**Match-Service** (`application.properties`):
```properties
jwt.secret=your-very-long-secret-key-min-256-bits-recommended-for-hs384
```

⚠️ **Important:** Use a secure, random secret in production!

### 4. OAuth 2.0 Configuration (Optional)

To enable Google/GitHub login, add credentials to **User-Service**:

**application.properties:**
```properties
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=your-github-client-id
spring.security.oauth2.client.registration.github.client-secret=your-github-client-secret
```

**How to get credentials:**
- **Google:** [Google Cloud Console](https://console.cloud.google.com/)
- **GitHub:** [GitHub Developer Settings](https://github.com/settings/developers)

### 5. Build All Services

```bash
# Build Eureka Server
cd Eureka-Server
mvn clean install
cd ..

# Build API Gateway
cd Api-Gateway
mvn clean install
cd ..

# Build User Service
cd User-Service
mvn clean install
cd ..

# Build Match Service
cd Match-Service
mvn clean install
cd ..
```

### 6. Setup Frontend

```bash
cd frontend
npm install
```

---

## ▶️ Running the Application

### Start Services in Order

Services must be started in this specific order:

#### 1. Start Eureka Server (Service Registry)

```bash
cd Eureka-Server
mvn spring-boot:run
```

Wait for: `Eureka Server started on port 8761`

Verify at: [http://localhost:8761](http://localhost:8761)

#### 2. Start User Service

```bash
cd User-Service
mvn spring-boot:run
```

Wait for: `User Service started on port 8085`

#### 3. Start Match Service

```bash
cd Match-Service
mvn spring-boot:run
```

Wait for: `Match Service started on port 8082`

#### 4. Start API Gateway

```bash
cd Api-Gateway
mvn spring-boot:run
```

Wait for: `API Gateway started on port 8080`

#### 5. Start Frontend

```bash
cd frontend
npm run dev
```

Frontend will start on: [http://localhost:8081](http://localhost:8081)

---

## 🎮 Using the Application

### 1. Register an Account

1. Go to [http://localhost:8081/register](http://localhost:8081/register)
2. Create an account with email/password
3. Or use Google/GitHub OAuth

### 2. Play Chess

#### Quick Match (Matchmaking)
1. Click "Play" from dashboard
2. Select game mode (Blitz or Rapid)
3. Wait for opponent
4. Game starts automatically when matched

#### Private Match
1. Click "Create Private Match"
2. Share the Match ID with a friend
3. Friend enters Match ID to join
4. Game starts when both players ready

### 3. Game Features

- **Make Moves:** Click piece → Click destination
- **Chat:** Use chat panel to message opponent
- **Resign:** Click flag icon to resign
- **Time:** Watch your clock and opponent's clock

---

## 📚 API Documentation

### Authentication Endpoints

```http
POST /auth/register
POST /auth/login
GET  /auth/me
```

### Match Endpoints

```http
POST /match/create-private
POST /match/{id}/join
GET  /match/{id}
POST /match/{id}/resign
```

### WebSocket Endpoints

```
CONNECT /ws (Port 8082)
SUBSCRIBE /topic/matchmaking/{email}
SUBSCRIBE /topic/game/{matchId}
SUBSCRIBE /topic/game/{matchId}/chat
SEND /app/matchmaking/join
SEND /app/game/{matchId}/move
SEND /app/game/{matchId}/chat
```

---

## ⚙️ Configuration

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 8081 | React development server |
| Frontend (Build) | 8081 | Production build (optional) |
| API Gateway | 8080 | Main entry point for REST APIs |
| User Service | 8085 | Authentication & user management |
| Match Service | 8082 | Game logic & WebSocket |
| Eureka Server | 8761 | Service registry |

### Environment Variables

Create `.env` file in `frontend/`:

```env
VITE_API_URL=http://localhost:8080
```

### CORS Configuration

CORS is configured in API Gateway (`application.yaml`):

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:5173"
              - "http://localhost:8081"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
            allow-credentials: true
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Services Not Registering with Eureka

**Symptom:** Service doesn't appear in Eureka dashboard

**Solution:**
- Ensure Eureka Server is running first
- Check `eureka.client.service-url.defaultZone` in `application.properties`
- Verify network connectivity to `localhost:8761`

#### 2. Database Connection Errors

**Symptom:** `org.postgresql.util.PSQLException`

**Solution:**
```bash
# Check PostgreSQL is running
sudo service postgresql status

# Verify databases exist
psql -U postgres -l

# Check credentials in application.properties
```

#### 3. CORS Errors in Browser

**Symptom:** "blocked by CORS policy"

**Solution:**
- Ensure you're using correct ports (REST→8080, WebSocket→8082)
- Check `ChessGame.tsx` uses `http://localhost:8082/ws` for WebSocket
- Verify API Gateway CORS configuration

#### 4. JWT Token Errors

**Symptom:** "Invalid token" or 401 Unauthorized

**Solution:**
- Ensure **same JWT secret** in User-Service and Match-Service
- Clear browser localStorage and re-login
- Check token expiration (default 24 hours)

#### 5. WebSocket Connection Fails

**Symptom:** "Failed to connect to WebSocket"

**Solution:**
- Verify Match Service is running on port 8082
- Check browser console for exact error
- Ensure JWT token is valid
- Try hard refresh (Ctrl+Shift+R)

#### 6. Port Already in Use

**Symptom:** `Port 8080 already in use`

**Solution:**
```bash
# Find process using port
lsof -i :8080

# Kill process (replace PID)
kill -9 PID

# Or change port in application.properties
server.port=8081
```

---

## 🔧 Development

### Running in Development Mode

**Backend (Hot Reload with Spring DevTools):**
```bash
cd User-Service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend (Hot Reload):**
```bash
cd frontend
npm run dev
```

### Building for Production

**Backend:**
```bash
# Each service
mvn clean package -DskipTests

# Run JAR
java -jar target/service-name.jar
```

**Frontend:**
```bash
npm run build
npm run preview  # Test production build
```

### Database Migrations

Using Flyway (if configured):
```bash
mvn flyway:migrate
```

---

## 📊 Project Statistics

- **Total Services:** 4 (Eureka, Gateway, User, Match)
- **Lines of Code:** ~15,000+
- **Frontend Components:** 20+
- **API Endpoints:** 15+
- **WebSocket Events:** 6+

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Pratapani Vishnu** - *Initial work* - (https://github.com/Vishnu12222344)

---

## 🙏 Acknowledgments

- Chess piece Unicode symbols
- Spring Boot team for excellent microservices framework
- React community for frontend tools
- shadcn/ui for beautiful components

---

## 📧 Support

For support, email pratapanivishnu@gmail.com or open an issue in the repository.

---

## 🗺️ Roadmap

- [ ] Add ELO rating system
- [ ] Implement game analysis with AI
- [ ] Add replay functionality
- [ ] Tournament mode
- [ ] Mobile app (React Native)
- [ ] Docker containerization
- [ ] Kubernetes deployment
- [ ] CI/CD pipeline

---

**Made with ❤️ by Chess Enthusiasts**
