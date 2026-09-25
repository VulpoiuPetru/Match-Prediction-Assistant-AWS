# Match Prediction Assistant — AWS

AI-assisted football match outcome predictions, using retrieval-augmented generation (RAG) over historical match data and Amazon Bedrock for generation. Backend deployed on AWS (ECS Fargate) with a full CI/CD pipeline; frontend in Angular.

<img width="1300" height="560" alt="architecture" src="https://github.com/user-attachments/assets/8d77eef4-3490-4ed8-8d7a-0b8ed70a7af1" />

## Features

- **Match predictions** — generate an outcome prediction for a given fixture, view predictions by match, the latest prediction, all predictions, and basic stats.
- **RAG pipeline** — before generating a prediction, the backend retrieves similar past predictions and match history from a **ChromaDB** vector store, augments the prompt with that context, then calls **Amazon Bedrock (Nova Micro)** to generate the response.
- **Contextual / conversational predictions** — refine a prediction through follow-up questions, with session-based history.
- **Simple chat endpoint** — free-form football-related questions, outside the structured prediction flow.
- **Dashboard (Angular + Chart.js)** — upcoming matches, prediction history, and basic analytics.
- **Sample data initialization** — seed teams/matches for a quick local demo, no manual data entry needed.

## Architecture

- **Frontend**: Angular, calling the backend over REST.
- **Backend**: Spring Boot (Java 21) packaged as a container, running on **Amazon ECS Fargate**.
- **AI / RAG**: **Amazon Bedrock** (Nova Micro) generates predictions; RAG context is retrieved from a self-hosted **ChromaDB** vector store.
- **Data**: **Amazon RDS (PostgreSQL)** for matches, teams, users, and stored predictions.
- **CI/CD**: push to **GitHub** → **AWS CodePipeline** → **CodeBuild** builds the Docker image and pushes it to **Amazon ECR** → ECS Fargate pulls and runs the new image. Registry and any deploy-time credentials come from **AWS Secrets Manager**.
- **Observability**: **CloudWatch** for application logs and monitoring.

The frontend isn't part of this AWS deployment story — it runs separately (e.g. any static host, or `ng serve` locally) and simply calls the backend's REST API.

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.2.5, Spring Data JPA, Spring Security, Maven |
| AI | Amazon Bedrock (Bedrock Runtime SDK), ChromaDB Java client, Spring AI + Ollama (local dev only) |
| Database | PostgreSQL (Amazon RDS in AWS, local Postgres container in dev) |
| Frontend | Angular, Angular Material, Chart.js, RxJS |
| Infra | Docker, Docker Compose (local), Amazon ECS Fargate, ECR, RDS, CodePipeline, CodeBuild, Secrets Manager, CloudWatch |

## API overview

| Base path | Purpose |
|---|---|
| `/api/predictions` | Generate and read match predictions, stats, upcoming matches, teams |
| `/api/contextual` | Conversational predictions with session history |
| `/api/chromadb` | Inspect the vector store directly (status, search, analytics) — useful for debugging the RAG pipeline |
| `/api/chat` | Simple free-form chat |

`TestController` also exposes a handful of debug endpoints (config check, manual data initialization, AI connectivity test). These exist for local development and aren't meant to be part of the public API surface.

## Running locally

**Prerequisites**: Docker and Docker Compose. Node 20+ and npm if you want to run the frontend outside Docker.

Start Postgres, ChromaDB, a local Ollama instance, and the backend together:

```bash
cd match-predictor
docker compose up --build
```

This brings up:
- `postgres` — the app's database
- `chromadb` — the vector store
- `ollama` — a local LLM (`llama3.2`), used in place of Bedrock for local development so you don't need AWS credentials to run the app
- `app` — the Spring Boot backend, on `http://localhost:8080`

In AWS, the app talks to **Amazon Bedrock** instead of Ollama (see `BedrockConfig`); locally it defaults to Ollama (see `OllamaConfig`) so development doesn't require AWS credentials.

Then run the frontend:

```bash
cd match-predictor-frontend/football-prediction-frontend
npm install
npm start
```

The frontend runs on `http://localhost:4200` and calls the backend at `http://localhost:8080`.

## Environment variables

| Variable | Used for | Local default |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/match_prediction_db` |
| `DB_USERNAME` | PostgreSQL user | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | local dev only — set a real value outside local Docker |
| `OLLAMA_URL` | Local LLM endpoint | `http://localhost:11434` |
| `OLLAMA_MODEL` | Local LLM model | `llama3.2` |
| `CHROMADB_URL` | Vector store endpoint | `http://localhost:8000` |

Bedrock access uses the standard AWS credential provider chain (IAM role in ECS, or your local AWS CLI credentials) — no AWS keys are set through `application.properties`.

## CI/CD

`buildspec.yml` defines the CodeBuild steps: log in to ECR, build the Docker image, tag it, and push it. CodePipeline triggers this on every push to `main`, and the resulting image is what ECS Fargate deploys.

## Project structure

```
match-predictor/                          Spring Boot backend
├─ src/main/java/.../config/              BedrockConfig, OllamaConfig, CorsConfig
├─ src/main/java/.../controller/          REST controllers (see API overview above)
├─ src/main/java/.../service/             AiPredictionService, ChromaDbService, ContextualAiService, ...
├─ src/main/java/.../entity/              Match, Team, User, UserPrediction, AiPrediction, ConversationContext
├─ src/main/java/.../repository/          Spring Data JPA repositories
├─ src/main/java/.../security/            SecurityConfig
├─ docker-compose.yml                     Local stack: postgres, chromadb, ollama, app
├─ Dockerfile                             Multi-stage build (Maven → JRE)
└─ buildspec.yml                          CodeBuild steps for CI/CD

match-predictor-frontend/football-prediction-frontend/   Angular frontend
├─ src/app/components/                    dashboard, history, login
└─ src/app/services/                      API client
```
