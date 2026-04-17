package com.pipeline.engine.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Step {
    public String name;
    public String command;
    public String description;
    public Integer timeout;
}