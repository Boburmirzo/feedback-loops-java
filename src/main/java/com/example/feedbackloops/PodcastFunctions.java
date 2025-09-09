package com.example.feedbackloops;

import com.azure.core.util.Context;
import com.example.feedbackloops.models.PodcastRecommendation;
import com.example.feedbackloops.models.PodcastRequest;
import com.example.feedbackloops.models.UserHistoryRequest;
import com.example.feedbackloops.services.ChatCompletionService;
import com.example.feedbackloops.services.EmbeddingService;
import com.example.feedbackloops.services.SqlExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PodcastFunctions {
    private static final Logger logger = Logger.getLogger(PodcastFunctions.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static EmbeddingService embeddingService;
    private static ChatCompletionService chatCompletionService;
    private static SqlExecutorService sqlExecutorService;
    
    static {
        // Initialize services
        String openAIEndpoint = System.getenv("AzureOpenAIEndpoint");
        String openAIApiKey = System.getenv("AzureOpenAIApiKey");
        String embeddingDeploymentName = System.getenv("AzureOpenAIEmbeddingDeploymentName");
        String chatDeploymentName = System.getenv("AzureOpenAIChatCompletionDeploymentName");
        String connectionString = System.getenv("NeonDatabaseConnectionString");
        
        embeddingService = new EmbeddingService(openAIEndpoint, openAIApiKey, embeddingDeploymentName);
        chatCompletionService = new ChatCompletionService(openAIEndpoint, openAIApiKey, chatDeploymentName);
        sqlExecutorService = new SqlExecutorService(connectionString);
    }
    
    @FunctionName("AddPodcast")
    public HttpResponseMessage addPodcast(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "add-podcast",
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Received a request to add a new podcast.");
        
        try {
            if (!request.getBody().isPresent()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing request body")
                    .build();
            }
            
            PodcastRequest data = objectMapper.readValue(request.getBody().get(), PodcastRequest.class);
            
            if (data.getTitle() == null || data.getTitle().isEmpty() || 
                data.getTranscript() == null || data.getTranscript().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing 'title' or 'transcript' in the request body.")
                    .build();
            }
            
            // Generate summary and embedding
            String summaryPrompt = String.format("Summarize the following podcast transcript:\n%s\nSummary:", data.getTranscript());
            String summary = chatCompletionService.getChatCompletionAsync(summaryPrompt).join();
            List<Float> embedding = embeddingService.getEmbeddingAsync(summary).join();
            
            // Insert into database
            String insertQuery = "INSERT INTO podcast_episodes (title, summary, transcript, embedding) VALUES (?, ?, ?, ?)";
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("1", data.getTitle());
            parameters.put("2", summary);
            parameters.put("3", data.getTranscript());
            parameters.put("4", embedding);
            
            sqlExecutorService.executeUpdateAsync(insertQuery, parameters).join();
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                .body(String.format("Podcast '%s' added successfully.", data.getTitle()))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding podcast", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage())
                .build();
        }
    }
    
    @FunctionName("UpdateUserHistory")
    public HttpResponseMessage updateUserHistory(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "update-user-history",
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Received a request to update user listening history.");
        
        try {
            if (!request.getBody().isPresent()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing request body")
                    .build();
            }
            
            UserHistoryRequest data = objectMapper.readValue(request.getBody().get(), UserHistoryRequest.class);
            
            if (data.getUserId() == null || data.getUserId().isEmpty() || 
                data.getListeningHistory() == null || data.getListeningHistory().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing 'userId' or 'listeningHistory' in the request body.")
                    .build();
            }
            
            // Generate embedding for listening history
            List<Float> embedding = embeddingService.getEmbeddingAsync(data.getListeningHistory()).join();
            
            // Update user in database
            String updateQuery = "UPDATE users SET listening_history = ?, embedding = ? WHERE id = ?";
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("1", data.getListeningHistory());
            parameters.put("2", embedding);
            parameters.put("3", Integer.parseInt(data.getUserId()));
            
            sqlExecutorService.executeUpdateAsync(updateQuery, parameters).join();
            
            return request.createResponseBuilder(HttpStatus.OK)
                .body(String.format("Listening history for user %s updated successfully.", data.getUserId()))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating user history", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage())
                .build();
        }
    }
    
    @FunctionName("RecommendPodcasts")
    public HttpResponseMessage recommendPodcasts(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                route = "recommend-podcasts",
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Received a request to recommend podcasts.");
        
        try {
            String userIdString = request.getQueryParameters().get("userId");
            
            if (userIdString == null || userIdString.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing 'userId' in query parameters.")
                    .build();
            }
            
            int userId = Integer.parseInt(userIdString);
            
            // Get user embedding
            String userEmbeddingQuery = "SELECT embedding FROM users WHERE id = ?";
            Map<String, Object> userParams = new LinkedHashMap<>();
            userParams.put("1", userId);
            
            List<Map<String, Object>> userResult = sqlExecutorService.executeQueryAsync(userEmbeddingQuery, userParams).join();
            
            if (userResult.isEmpty() || userResult.get(0).get("embedding") == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(String.format("No embedding found for user ID %d.", userId))
                    .build();
            }
            
            Object userEmbedding = userResult.get(0).get("embedding");
            
            // Find similar podcasts
            String recommendationQuery = """
                SELECT id, title, summary, embedding <-> ? AS similarity
                FROM podcast_episodes
                WHERE embedding IS NOT NULL
                ORDER BY similarity ASC
                LIMIT 3
                """;
            
            Map<String, Object> recParams = new LinkedHashMap<>();
            recParams.put("1", userEmbedding);
            
            List<Map<String, Object>> recommendations = sqlExecutorService.executeQueryAsync(recommendationQuery, recParams).join();
            
            List<PodcastRecommendation> responseList = new ArrayList<>();
            for (Map<String, Object> rec : recommendations) {
                String prompt = String.format("Summarize the following podcast in 5 words or less:\n\nPodcast: %s\nDescription: %s\n\nSummary:",
                    rec.get("title"), rec.get("summary"));
                String shortDescription = chatCompletionService.getChatCompletionAsync(prompt).join();
                
                // Insert suggestion into database
                String insertSuggestionQuery = "INSERT INTO suggested_podcasts (user_id, podcast_id, similarity_score) VALUES (?, ?, ?)";
                Map<String, Object> suggestionParams = new LinkedHashMap<>();
                suggestionParams.put("1", userId);
                suggestionParams.put("2", rec.get("id"));
                suggestionParams.put("3", rec.get("similarity"));
                
                sqlExecutorService.executeUpdateAsync(insertSuggestionQuery, suggestionParams).join();
                
                responseList.add(new PodcastRecommendation(
                    rec.get("id").toString(),
                    rec.get("title").toString(),
                    shortDescription,
                    Float.parseFloat(rec.get("similarity").toString())
                ));
            }
            
            String jsonResponse = objectMapper.writeValueAsString(responseList);
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(jsonResponse)
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error recommending podcasts", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage())
                .build();
        }
    }
    
    @FunctionName("GetSuggestedPodcasts")
    public HttpResponseMessage getSuggestedPodcasts(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                route = "get-suggested-podcasts",
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Received a request to fetch suggested podcasts for a user.");
        
        try {
            String userIdString = request.getQueryParameters().get("userId");
            
            if (userIdString == null || userIdString.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing 'userId' in query parameters.")
                    .build();
            }
            
            int userId = Integer.parseInt(userIdString);
            
            String query = """
                SELECT sp.user_id, pe.id AS podcast_id, pe.title
                FROM suggested_podcasts sp
                JOIN podcast_episodes pe ON sp.podcast_id = pe.id
                WHERE sp.user_id = ?
                ORDER BY sp.similarity_score ASC
                """;
            
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("1", userId);
            
            List<Map<String, Object>> suggested = sqlExecutorService.executeQueryAsync(query, parameters).join();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : suggested) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", row.get("user_id"));
                item.put("podcastId", row.get("podcast_id"));
                item.put("title", row.get("title"));
                result.add(item);
            }
            
            String jsonResponse = objectMapper.writeValueAsString(result);
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(jsonResponse)
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting suggested podcasts", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage())
                .build();
        }
    }
}
