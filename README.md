# CalorizerBot

CalorizerBot is a Telegram bot designed to assist users in managing their calorie intake and nutrition. The project uses a microservices architecture, adhering to production-oriented principles, to provide robust functionality such as user profile management, dietary recommendations, and language support.

---
## Key Features
* **User Management:** Create and manage detailed user profiles, including physical metrics (weight, height, age, body fat) and nutritional goals.
* **AI-Powered Nutritionist:** Integrated with **Google Gemini API** to provide intelligent dietary advice, personalized recipe suggestions, and natural language analysis of food logs.
* **Advanced Calorie Calculation:** Supports multiple industry-standard formulas for precise daily needs:
    * Mifflin-St Jeor & Harris-Benedict
    * Katch-McArdle (based on Lean Body Mass)
    * Tom Venuto methods
* **Multilingual Support:** Full localization for **English, German, Russian, and Ukrainian**.
* **Context-Aware Responses:** The bot leverages AI to understand user intent and provide feedback tailored to their specific physical profile and progress.
* **Database Reliability:** Uses **MySQL** for persistent storage with **Liquibase** for version-controlled database migrations.
* **Containerized Environment:** Fully dockerized for seamless deployment and environment consistency.

---
## Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.3 |
| **Data Access** | Spring Data JPA (Hibernate) |
| **Database** | MySQL 8.0 |
| **Migrations** | Liquibase |
| **Cloud AI** | Google Gemini API |
| **Platform** | Telegram Bots API |
| **DevOps** | Docker, Docker Compose |

---
## Configuration & Environment

The project uses a `.env` file for configuration. Create a `.env` file in the root directory and fill in the following variables:

```env
# Database Settings
DB_NAME=calorizer_db
DB_USER=root
DB_PASSWORD=your_secure_password

# Application Settings
APP_PORT=8081

# Telegram Bot Settings
BOT_TOKEN=your_telegram_bot_token
BOT_NAME=your_bot_name

# AI Configuration (Google Gemini)
AI_MODEL_ID=gemini-3-flash-preview
GEMINI_API_KEY=your_google_gemini_api_key
```

---
## Getting Started

### Prerequisites
* **Docker & Docker Compose:** Ensure that Docker and Docker Compose are installed on your machine.

* **Java Development Kit (JDK):** Install JDK 21 or later.

### Installation Steps

1. **Clone the Repository**
```
git clone [https://github.com/your-repo/CalorizerBot.git](https://github.com/your-repo/CalorizerBot.git)
cd CalorizerBot
```
2. **Create and Configure .env File**

Copy env-example to .env and fill in necessary variables:
```
cp env-example .env
nano .env # or use your preferred editor
```

3. **Build the Project**
```
./mvnw clean install
```

4. **Run the Application**

    **Directly via JAR:**
```
java -jar target/Bot-0.0.1-SNAPSHOT.jar
```

Using Docker Compose (Recommended for production):

```
docker-compose up --build
```

## Troubleshooting
* **Database Connection Issues:** Ensure your MySQL server is running and the credentials in .env are correct.

* **API Integration Errors:** Verify that BOT_TOKEN and GEMINI_API_KEY are valid and have sufficient permissions.

* **Port Conflicts:** Adjust the APP_PORT in .env if default port 8081 is already in use.
