# Generative Feedback Loops Java Example

This project implements a **personalized podcast recommendation solution** with [**Neon**](https://neon.tech/), [**Azure OpenAI**](https://azure.microsoft.com/en-us/products/ai-services/openai-service), and [**Azure Functions**](https://learn.microsoft.com/en-us/azure/azure-functions/functions-overview) using **Java**. The solution dynamically analyzes user preferences and podcast data to provide highly relevant suggestions in real-time. It uses the **Generative Feedback Loops** (GFL) mechanism to continuously learn from new user interactions and content updates.

## What is Generative Feedback Loops

As vector search and Retrieval Augmented Generation(RAG) become mainstream for Generative AI (GenAI) use cases, we're looking ahead to what's next. GenAI primarily operates in a one-way direction, generating content based on input data. Generative Feedback Loops (GFL) are focused on optimizing and improving the AI's outputs over time through a cycle of feedback and learnings based on the production data. In GFL, results generated from Large Language Models (LLMs) like GPT are vectorized, indexed, and saved back into vector storage for better-filtered semantic search operations. This creates a dynamic cycle that adapts LLMs to new and continuously changing data, and user needs. GFL offers personalized, up-to-date summaries and suggestions.

---

## Features

1. **Add New Podcasts**:
    - Automatically generates embeddings and summaries using Azure OpenAI and stores them in Neon.
2. **Update User Preferences**:
    - Updates user listening history and generates embeddings for personalization.
3. **Recommend Podcasts**:
    - Provides personalized podcast recommendations based on user preferences and podcast similarity using the Neon `pgvector` extension.
4. **Feedback Loop**:
    - Continuously adapts recommendations by incorporating new user preferences and podcast data.

---

## Tech Stack

- **Java 17**: Backend implementation using Azure Functions Java SDK.
- **Azure Functions**: Expose APIs for podcast management and recommendations.
- **Azure OpenAI**: Generate embeddings and summaries using `text-embedding-ada-002` and `gpt-4`.
- **Neon**: Serverless PostgreSQL database with `pgvector` extension for storing embeddings and performing similarity queries.
- **Maven**: Build and dependency management.
- **HikariCP**: Connection pooling for database connections.
- **Jackson**: JSON processing.

---

## How It Works

1. **Add Podcast**:
    - Generates an embedding and summary using Azure OpenAI.
    - Saves the podcast data, embedding, and summary in Neon.
2. **Update User History**:
    - Generates an embedding for the user's updated listening history.
    - Saves the updated preferences and embedding in Neon.
3. **Recommend Podcasts**:
    - Fetches the user's embedding from Neon.
    - Finds the most relevant podcasts using `pgvector` similarity.
    - Generates a short description for each recommendation using GPT.
    - Stores the recommendation and GPT output in the `suggested_podcasts` table.

---

## Feedback Loop

1. **Real-Time Updates**:
    - Each new podcast or updated user preference is reflected immediately in the system.
2. **Dynamic Recommendations**:
    - Recommendations evolve based on user interactions and new data.
3. **Adaptability**:
    - The system automatically scales with more users and podcasts.

---

## Prerequisites

- **Java 17** or later
- **Maven 3.6+**
- [**Azure Functions Core Tools**](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local) v4.x
- [**Azure CLI**](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) (for deployment)
- A [**Neon account**](https://console.neon.tech/signup)
- An [**Azure account**](https://azure.microsoft.com/free/) with an active subscription

---

## Set Up Database

**Create a Neon Project**

1. Navigate to the [Neon Console](https://console.neon.tech/)
2. Click "New Project"
3. Select **Azure** as your cloud provider
4. Choose East US 2 as your region
5. Give your project a name (e.g., "generative-feedback-loop-java")
6. Click "Create Project"
7. Copy the Neon **connection string** from the Connection Details widget

**Set Up Database Tables**

Execute the following SQL script in the Neon SQL editor:

```sql
-- Create a table to store vector embeddings
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE podcast_episodes (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    summary TEXT,
    transcript TEXT NOT NULL,
    embedding VECTOR(1536)
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    listening_history TEXT,
    embedding VECTOR(1536)
);

CREATE TABLE suggested_podcasts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    podcast_id INT NOT NULL,
    suggested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    similarity_score FLOAT,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (podcast_id) REFERENCES podcast_episodes (id) ON DELETE CASCADE
);
```

**Insert Sample Data**

```sql
INSERT INTO users (name, listening_history)
VALUES
('Alice', 'Interested in AI, deep learning, and neural networks.'),
('Bob', 'Enjoys podcasts about robotics, automation, and machine learning.'),
('Charlie', 'Fascinated by space exploration, astronomy, and astrophysics.'),
('Diana', 'Prefers topics on fitness, nutrition, and mental health.'),
('Eve', 'Likes discussions on blockchain, cryptocurrency, and decentralized finance.'),
('Frank', 'Follows podcasts about history, culture, and ancient civilizations.');
```

---

## Setup Instructions

### 1. Clone and Build

```bash
git clone <your-repo-url>
cd feedback-loops-java
mvn clean compile
```

### 2. Set Up Environment Variables

Update the `local.settings.json` file with your configuration:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "AzureOpenAIApiKey": "your-azure-openai-api-key",
    "AzureOpenAIEndpoint": "https://your-openai-resource.openai.azure.com/",
    "AzureOpenAIEmbeddingDeploymentName": "text-embedding-ada-002",
    "AzureOpenAIChatCompletionDeploymentName": "gpt-4",
    "NeonDatabaseConnectionString": "postgresql://username:password@host:port/database?sslmode=require"
  }
}
```

### 3. Run Locally

```bash
mvn azure-functions:run
```

The functions will be available at:
- `http://localhost:7071/api/add-podcast`
- `http://localhost:7071/api/update-user-history`
- `http://localhost:7071/api/recommend-podcasts`
- `http://localhost:7071/api/get-suggested-podcasts`

### 4. Deploy to Azure

```bash
# Login to Azure
az login

# Create a resource group
az group create --name feedback-loops-rg --location eastus

# Deploy the function app
mvn azure-functions:deploy
```

---

## API Endpoints

### 1. Add Podcast

- **Description**: Add a new podcast episode.
- **Endpoint**: `POST /api/add-podcast`
- **Request Body**:
    
    ```json
    {
      "title": "Future of Robotics",
      "transcript": "This episode discusses robotics, AI, and automation..."
    }
    ```
    
- **Response**:
    
    ```json
    "Podcast 'Future of Robotics' added successfully."
    ```

### 2. Update User History

- **Description**: Updates user listening history and generates embeddings.
- **Endpoint**: `POST /api/update-user-history`
- **Request Body**:
    
    ```json
    {
      "userId": "1",
      "listeningHistory": "Exploring robotics, AI, and automation advancements."
    }
    ```
    
- **Response**:
    
    ```json
    "Listening history for user 1 updated successfully."
    ```

### 3. Recommend Podcasts

- **Description**: Fetches personalized podcast recommendations.
- **Endpoint**: `GET /api/recommend-podcasts?userId=1`
- **Response**:
    
    ```json
    [
      {
        "id": "1",
        "title": "AI and the Future",
        "summary": "AI shapes tomorrow.",
        "similarity": 0.123
      }
    ]
    ```

### 4. Get Suggested Podcasts

- **Description**: Retrieves previously suggested podcasts for a user.
- **Endpoint**: `GET /api/get-suggested-podcasts?userId=1`
- **Response**:
    
    ```json
    [
      {
        "userId": 1,
        "podcastId": 1,
        "title": "AI and the Future"
      }
    ]
    ```

---

## Project Structure

```
feedback-loops-java/
├── pom.xml
├── host.json
├── local.settings.json
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── feedbackloops/
                        ├── PodcastFunctions.java      # Main Azure Functions class
                        ├── models/
                        │   ├── PodcastRequest.java
                        │   ├── UserHistoryRequest.java
                        │   └── PodcastRecommendation.java
                        └── services/
                            ├── EmbeddingService.java
                            ├── ChatCompletionService.java
                            └── SqlExecutorService.java
```

---

## Future Improvements

1. **Enhanced User Analytics**: Track user interactions for better recommendations
2. **Caching**: Implement Redis caching for frequently accessed data
3. **Monitoring**: Add Application Insights integration
4. **Testing**: Comprehensive unit and integration tests
5. **Security**: Implement proper authentication and authorization
6. **Performance**: Optimize database queries and connection pooling

---

## Troubleshooting

### Common Issues

1. **PGVector Extension**: Ensure the pgvector extension is installed in your Neon database
2. **Java Version**: Make sure you're using Java 17 or later
3. **Environment Variables**: Verify all required environment variables are set in `local.settings.json`
4. **Dependencies**: Run `mvn clean install` to ensure all dependencies are downloaded

### Logs

- Local development: Check the Azure Functions Core Tools console output
- Azure deployment: Use Azure Portal or Azure CLI to view function logs

```bash
# View logs using Azure CLI
az functionapp logs tail --name <your-function-app-name> --resource-group feedback-loops-rg
```
