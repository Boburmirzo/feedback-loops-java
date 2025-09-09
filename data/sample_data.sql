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

INSERT INTO users (name, listening_history)
VALUES
('Alice', 'Interested in AI, deep learning, and neural networks.'),
('Bob', 'Enjoys podcasts about robotics, automation, and machine learning.'),
('Charlie', 'Fascinated by space exploration, astronomy, and astrophysics.'),
('Diana', 'Prefers topics on fitness, nutrition, and mental health.'),
('Eve', 'Likes discussions on blockchain, cryptocurrency, and decentralized finance.'),
('Frank', 'Follows podcasts about history, culture, and ancient civilizations.');
