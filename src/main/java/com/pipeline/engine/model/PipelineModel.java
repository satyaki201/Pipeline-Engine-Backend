package com.pipeline.engine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PipelineModel {
        public Runtime runtime;

        @Override
        public String toString() {
                return "PipelineModel{" +
                        "runtime=" + runtime +
                        ", steps=" + steps +
                        ", services=" + services +
                        '}';
        }

        public List<Step> steps;
        public List<Service> services;
}
