package com.pipeline.engine.controller;

import com.pipeline.engine.model.PipelineModel;
import com.pipeline.engine.service.EngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;


@Slf4j
@RestController
@RequestMapping("/api/v1")
public class EngineController {

    @Autowired
    private EngineService engineService;
    @GetMapping("/status")
    public String getStatus() {
        engineService.checkDockerStatus();
        return "Pipeline Engine is running!";
    }

    @PostMapping("/dockerSpinner")
    public ResponseEntity<String> post(@RequestParam String repoUrl, @RequestParam("jobFile") MultipartFile file,
                                       @RequestParam String branch, @RequestParam String backgroundOs) {

        if (repoUrl == null || repoUrl.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Job file is required");
        }

        // Validate file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidYamlFile(fileName)) {
            return ResponseEntity.badRequest().body("Wrong jobFile format! Only .yaml or .yml files are allowed");
        }

        Yaml pipelineYaml = new Yaml();
        PipelineModel pipeline;
        try {
            InputStream inputStream = file.getInputStream();
            pipeline = pipelineYaml.loadAs(inputStream, PipelineModel.class);
            log.info("Pipeline Model: {}", pipeline.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body("Failed to convert file to String");
        }

        engineService.dockerSpinner(repoUrl, pipeline, branch, backgroundOs);
        return new ResponseEntity<>("Processed Fine", HttpStatus.OK);
    }

    private boolean isValidYamlFile(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = fileName.substring(lastDotIndex);
        return extension.equals(".yaml") || extension.equals(".yml");
    }
}
