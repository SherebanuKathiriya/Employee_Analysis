package com.example;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Employee {
    private String name;
    private String position;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
