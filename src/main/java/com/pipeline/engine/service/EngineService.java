package com.pipeline.engine.service;

import com.pipeline.engine.model.PipelineModel;

import java.io.File;

public interface EngineService {
    Boolean checkDockerStatus();
    void dockerSpinner(String repoUrl, PipelineModel pipelineModel, String branch, String backgroundOs);
}
