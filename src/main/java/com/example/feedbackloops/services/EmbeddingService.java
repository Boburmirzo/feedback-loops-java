package com.example.feedbackloops.services;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.credential.AzureKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final OpenAIClient client;
    private final String deploymentName;
    
    public EmbeddingService(String endpoint, String apiKey, String deploymentName) {
        this.client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(apiKey))
            .buildClient();
        this.deploymentName = deploymentName;
    }
    
    public CompletableFuture<List<Float>> getEmbeddingAsync(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(List.of(input));
                Embeddings embeddings = client.getEmbeddings(deploymentName, embeddingsOptions);
                
                if (embeddings.getData() != null && !embeddings.getData().isEmpty()) {
                    EmbeddingItem embeddingItem = embeddings.getData().get(0);
                    return embeddingItem.getEmbedding();
                }
                
                throw new RuntimeException("No embeddings were returned.");
            } catch (Exception e) {
                logger.error("Error generating embedding: ", e);
                throw new RuntimeException("Failed to generate embedding", e);
            }
        });
    }
}
