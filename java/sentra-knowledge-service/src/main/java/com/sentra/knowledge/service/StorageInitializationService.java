package com.sentra.knowledge.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.sentra.knowledge.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 存储初始化服务
 * 负责创建MongoDB集合和本地图存储目录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageInitializationService {

    private final MongoClient mongoClient;
    private final Neo4jClient neo4jClient;
    private final StorageProperties storageProperties;

    /**
     * 为知识库初始化存储
     *
     * @param kbId 知识库ID
     */
    public void initializeKnowledgeBaseStorage(String kbId) {
        try {
            // 创建MongoDB集合
            createMongoDBCollection(kbId);
            log.info("MongoDB collection created for kb: {}", kbId);

            // 创建本地存储目录
            createNeo4jLocalDirectory(kbId);
            log.info("Neo4j local directory created for kb: {}", kbId);

            // 在Neo4j中创建知识库命名空间
            // TODO 这个不一定需要 因为我们的Graph数据 存储到了本地 而不是neo4j
            createNeo4jNamespace(kbId);
            log.info("Neo4j namespace created for kb: {}", kbId);

        } catch (Exception e) {
            log.error("Failed to initialize storage for kb: {}", kbId, e);
            throw new RuntimeException("Failed to initialize knowledge base storage", e);
        }
    }

    /**
     * 创建MongoDB集合
     */
    private void createMongoDBCollection(String kbId) {
        MongoDatabase database = mongoClient.getDatabase("sentra_kb");
        database.createCollection(kbId);
        log.info("Created MongoDB collection: {}", kbId);
    }

    /**
     * 创建Neo4j本地存储目录
     */
    private void createNeo4jLocalDirectory(String kbId) {
        String graphPath = storageProperties.getGraphPath();
        File kbDir = new File(graphPath, kbId);

        if (!kbDir.exists()) {
            boolean created = kbDir.mkdirs();
            if (created) {
                log.info("Created Neo4j local directory: {}", kbDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Failed to create directory: " + kbDir.getAbsolutePath());
            }
        } else {
            log.info("Neo4j local directory already exists: {}", kbDir.getAbsolutePath());
        }
    }

    /**
     * 在Neo4j中创建知识库命名空间
     * 使用节点属性来标识知识库
     */
    private void createNeo4jNamespace(String kbId) {
        // 创建一个虚拟节点来标识知识库命名空间
        String cypher = """
            MERGE (kb:KnowledgeBase {kbId: $kbId})
            SET kb.createdAt = datetime()
            RETURN kb
            """;

        neo4jClient.query(cypher)
                .bind(kbId).to("kbId")
                .run();
    }

    /**
     * 删除知识库存储
     *
     * @param kbId 知识库ID
     */
    public void deleteKnowledgeBaseStorage(String kbId) {
        try {
            // 删除MongoDB集合
            dropMongoDBCollection(kbId);

            // 删除Neo4j本地存储目录
            deleteNeo4jLocalDirectory(kbId);

            // 删除Neo4j中的知识库数据
            deleteNeo4jNamespace(kbId);

            log.info("Storage deleted for kb: {}", kbId);
        } catch (Exception e) {
            log.error("Failed to delete storage for kb: {}", kbId, e);
            throw new RuntimeException("Failed to delete knowledge base storage", e);
        }
    }

    private void dropMongoDBCollection(String kbId) {
        MongoDatabase database = mongoClient.getDatabase("sentra_kb");
        database.getCollection(kbId).drop();
        log.info("Dropped MongoDB collection: {}", kbId);
    }

    private void deleteNeo4jLocalDirectory(String kbId) {
        String graphPath = storageProperties.getGraphPath();
        File kbDir = new File(graphPath, kbId);

        if (kbDir.exists()) {
            deleteDirectory(kbDir);
            log.info("Deleted Neo4j local directory: {}", kbDir.getAbsolutePath());
        }
    }

    private void deleteNeo4jNamespace(String kbId) {
        // 删除该知识库相关的所有节点和关系
        String cypher = """
            MATCH (n)
            WHERE n.kbId = $kbId
            DETACH DELETE n
            """;

        neo4jClient.query(cypher)
                .bind(kbId).to("kbId")
                .run();

        // 删除知识库命名空间节点
        String deleteKbCypher = """
            MATCH (kb:KnowledgeBase {kbId: $kbId})
            DELETE kb
            """;

        neo4jClient.query(deleteKbCypher)
                .bind(kbId).to("kbId")
                .run();
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
