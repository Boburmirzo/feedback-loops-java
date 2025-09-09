package com.example.feedbackloops.services;

import com.pgvector.PGvector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SqlExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(SqlExecutorService.class);
    
    private final HikariDataSource dataSource;
    
    public SqlExecutorService(String connectionString) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionString);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        this.dataSource = new HikariDataSource(config);
        
        // Register PGvector types with a connection
        try (Connection connection = this.dataSource.getConnection()) {
            PGvector.addVectorType(connection);
        } catch (SQLException e) {
            logger.error("Failed to register PGvector types", e);
        }
    }
    
    public CompletableFuture<List<Map<String, Object>>> executeQueryAsync(String sqlQuery, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> result = new ArrayList<>();
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
                
                // Set parameters
                if (parameters != null) {
                    int paramIndex = 1;
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof List<?>) {
                            // Handle vector/embedding parameters
                            @SuppressWarnings("unchecked")
                            List<Float> embedding = (List<Float>) value;
                            float[] floatArray = new float[embedding.size()];
                            for (int i = 0; i < embedding.size(); i++) {
                                floatArray[i] = embedding.get(i);
                            }
                            PGvector vector = new PGvector(floatArray);
                            statement.setObject(paramIndex++, vector);
                        } else {
                            statement.setObject(paramIndex++, value);
                        }
                    }
                }
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            String columnType = metaData.getColumnTypeName(i);
                            
                            if ("vector".equals(columnType)) {
                                // Handle pgvector columns
                                PGvector vector = (PGvector) resultSet.getObject(i);
                                if (vector != null) {
                                    row.put(columnName, vector);
                                } else {
                                    row.put(columnName, null);
                                }
                            } else {
                                row.put(columnName, resultSet.getObject(i));
                            }
                        }
                        result.add(row);
                    }
                }
            } catch (SQLException e) {
                logger.error("Error executing query: " + sqlQuery, e);
                throw new RuntimeException("Database query failed", e);
            }
            
            return result;
        });
    }
    
    public CompletableFuture<Integer> executeUpdateAsync(String sqlQuery, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
                
                // Set parameters
                if (parameters != null) {
                    int paramIndex = 1;
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof List<?>) {
                            // Handle vector/embedding parameters
                            @SuppressWarnings("unchecked")
                            List<Float> embedding = (List<Float>) value;
                            float[] floatArray = new float[embedding.size()];
                            for (int i = 0; i < embedding.size(); i++) {
                                floatArray[i] = embedding.get(i);
                            }
                            PGvector vector = new PGvector(floatArray);
                            statement.setObject(paramIndex++, vector);
                        } else {
                            statement.setObject(paramIndex++, value);
                        }
                    }
                }
                
                return statement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error executing update: " + sqlQuery, e);
                throw new RuntimeException("Database update failed", e);
            }
        });
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
