package com.sentra.knowledge.util;

import com.jcraft.jsch.*;
import com.sentra.knowledge.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

/**
 * SFTP工具类
 */
@Slf4j
@Component
public class SftpUtil {

    private final StorageProperties storageProperties;

    public SftpUtil(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 获取SFTP会话
     */
    private Session getSession() throws JSchException {
        StorageProperties.SftpConfig sftpConfig = storageProperties.getSftp();

        JSch jsch = new JSch();
        Session session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
        session.setPassword(sftpConfig.getPassword());

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();
        return session;
    }

    /**
     * 上传文件到SFTP服务器
     *
     * @param inputStream 文件输入流
     * @param remotePath  远程文件路径
     * @return 是否上传成功
     */
    public boolean uploadFile(InputStream inputStream, String remotePath) {
        ChannelSftp sftp = null;
        Session session = null;

        try {
            session = getSession();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            // 确保目录存在
            String dir = remotePath.substring(0, remotePath.lastIndexOf("/"));
            try {
                sftp.mkdir(dir);
            } catch (SftpException e) {
                if (e.id != 4) { // 4表示目录已存在
                    throw e;
                }
            }

            // 上传文件
            sftp.put(inputStream, remotePath);
            log.info("文件上传成功: {}", remotePath);
            return true;

        } catch (Exception e) {
            log.error("文件上传失败: {}", remotePath, e);
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 从SFTP服务器下载文件
     *
     * @param remotePath 远程文件路径
     * @param localPath  本地文件路径
     * @return 是否下载成功
     */
    public boolean downloadFile(String remotePath, String localPath) {
        ChannelSftp sftp = null;
        Session session = null;

        try {
            session = getSession();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            sftp.get(remotePath, localPath);
            log.info("文件下载成功: {} -> {}", remotePath, localPath);
            return true;

        } catch (Exception e) {
            log.error("文件下载失败: {}", remotePath, e);
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 删除SFTP服务器上的文件
     *
     * @param remotePath 远程文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String remotePath) {
        ChannelSftp sftp = null;
        Session session = null;

        try {
            session = getSession();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            sftp.rm(remotePath);
            log.info("文件删除成功: {}", remotePath);
            return true;

        } catch (Exception e) {
            log.error("文件删除失败: {}", remotePath, e);
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
