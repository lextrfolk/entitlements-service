package com.app.entitlement.model;

import lombok.Data;
import java.util.List;

@Data
public class FormScheduleMapping {

    private String form;
    private List<String> schedules;
}
