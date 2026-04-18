package com.pipeline.engine.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.pipeline.engine.model.PipelineModel;
import com.pipeline.engine.service.EngineService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

import static com.pipeline.engine.AppUtils.DIR_DES;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngineServiceImpl implements EngineService {

    private final DockerClient dockerClient;

    @PostConstruct
    public void initJGitSsh() {
        SshdSessionFactory factory = new SshdSessionFactory();
        // Optional: Configure the factory to use a specific SSH directory
        // factory.setHomeDirectory(new File("/app/config/.ssh"));

        SshSessionFactory.setInstance(factory);
    }


    @Override
    public Boolean checkDockerStatus() {
        log.info("Docker Status For Underlying Application{}", dockerClient.infoCmd().exec());
        return true;
    }

    @Override
    public void dockerSpinner(String repoUrl, PipelineModel pipelineModel, String branch, String backgroundOs) {
        UUID uuid = UUID.randomUUID();
        File clonedPath = cloneRepository(repoUrl, branch, uuid.toString());
        checkDockerStatus();
        if (!checkDockerStatus()) {
            return;
        }
        var listOfCurrentImages = dockerClient.listImagesCmd().exec();
        log.info("Docker Images{}", listOfCurrentImages);
        try {
            var pulledImage = dockerClient.pullImageCmd(backgroundOs)
                    .start()
                    .awaitCompletion();

            log.info("Docker Pull Image {}", pulledImage);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        var aliveContainer = dockerClient.listContainersCmd().exec();
        for (var it : aliveContainer) {
//            dockerClient.stopContainerCmd(it.getId()).exec();
        }
        log.info("Docker Alive Containers {}", aliveContainer);
        var newContainer = dockerClient.createContainerCmd(backgroundOs)
                .withHostConfig(new HostConfig()
                        .withMemory(256 * 1024 * 1024L)
                        .withPortBindings(PortBinding.parse("9479:3000"))
                        .withBinds(new Bind(clonedPath.getAbsolutePath(), new Volume("/app/workspace"))))
                .withStopTimeout(10)
                .withCmd("tail", "-f", "/dev/null")
                .exec();
        log.info("Docker Create Container with Id {}", newContainer.getId());
        dockerClient.startContainerCmd(newContainer.getId()).exec();
        log.info("Docker Create Container with Id {} is started", newContainer.getId());
        for (var it : pipelineModel.getSteps()) {

            var containerId = newContainer.getId();
            var executionStatement = dockerClient.execCreateCmd(containerId).withAttachStderr(true)
                    .withAttachStdout(true)
                    .withCmd("sh", "-c", "cd app/workspace && " + it.getCommand() + "| tee /proc/1/fd/1")
                    .exec();

            //TODO: Understand exec method
            try {
                if (it.getTimeout() != null)
                    dockerClient.execStartCmd(executionStatement.getId()).exec(new ExecStartResultCallback(System.out, System.err))
                            .awaitCompletion(it.getTimeout(), java.util.concurrent.TimeUnit.SECONDS);
                else
                    dockerClient.execStartCmd(executionStatement.getId()).exec(new ExecStartResultCallback(System.out, System.err));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }


    }


    public File cloneRepository(String repoUrl, String branch, String uuid) {
        File localPath = new File(DIR_DES + uuid);
        if (localPath.exists()) {
            deleteDirectory(localPath);
        }
        try {
            Git.cloneRepository().
                    setURI(repoUrl).
                    setBranch(branch).
                    setDirectory(localPath).
                    call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        return localPath;
    }

    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            log.warn("Failed to delete directory: {}", directory.getAbsolutePath());
        }
    }

}
