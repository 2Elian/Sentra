package com.sentra.knowledge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 知识库ID生成器
 * 使用SHA-256哈希算法生成唯一的kb_id
 */
public class KbIdGenerator {

    /**
     * 生成知识库唯一标识
     * 规则：SHA-256(tenant_id + owner_user_id + name)
     *
     * @param tenantId    租户ID
     * @param ownerUserId 所有者用户ID
     * @param name        知识库名称
     * @return 哈希后的kb_id（16进制字符串，前16位）
     */
    public static String generate(String tenantId, String ownerUserId, String name) {
        String input = tenantId + ownerUserId + name;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate kb_id", e);
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
