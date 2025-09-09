package com.example.feedbackloops.services;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatCompletionService {
    private static final Logger logger = LoggerFactory.getLogger(ChatCompletionService.class);
    
    private final OpenAIClient client;
    private final String deploymentName;
    
    public ChatCompletionService(String endpoint, String apiKey, String deploymentName) {
        this.client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(apiKey))
            .buildClient();
        this.deploymentName = deploymentName;
    }
    
    public CompletableFuture<String> getChatCompletionAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ChatRequestMessage> chatMessages = new ArrayList<>();
                chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant that generates podcast summaries."));
                chatMessages.add(new ChatRequestUserMessage(prompt));
                
                ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
                ChatCompletions chatCompletions = client.getChatCompletions(deploymentName, chatCompletionsOptions);
                
                if (chatCompletions.getChoices() != null && !chatCompletions.getChoices().isEmpty()) {
                    return chatCompletions.getChoices().get(0).getMessage().getContent().trim();
                }
                
                throw new RuntimeException("No chat completion was returned.");
            } catch (Exception e) {
                logger.error("Error generating chat completion: ", e);
                throw new RuntimeException("Failed to generate chat completion", e);
            }
        });
    }
}
