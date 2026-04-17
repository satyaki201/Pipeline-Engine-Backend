package com.pipeline.engine.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class DockerConfig {

    @Bean
    public DefaultDockerClientConfig dockerClientConfig() {
        String homeDir = System.getProperty("user.home");
        String dockerHost = getDockerHost(homeDir);

        log.info("Configuring Docker client with host: {}", dockerHost);

        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
    }

    private String getDockerHost(String homeDir) {
        // Try Colima socket first
        String colimaSocket = homeDir + "/.colima/default/docker.sock";
        if (Files.exists(Paths.get(colimaSocket))) {
            log.info("Found Colima socket at: {}", colimaSocket);
            return "unix://" + colimaSocket;
        }

        // Try standard Unix socket
        String standardSocket = "/var/run/docker.sock";
        if (Files.exists(Paths.get(standardSocket))) {
            log.info("Using standard Docker socket at: {}", standardSocket);
            return "unix://" + standardSocket;
        }

        // Fallback to TCP (should only be used if sockets are not available)
        log.warn("No Unix socket found, falling back to TCP. Make sure Docker is accessible via localhost:2375");
        return "tcp://localhost:2375";
    }

    @Bean
    public DockerHttpClient dockerHttpClient(DefaultDockerClientConfig config) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();
    }

    @Bean
    public DockerClient dockerClient(DefaultDockerClientConfig config,
                                     DockerHttpClient httpClient) {
        return DockerClientImpl.getInstance(config, httpClient);
    }
}