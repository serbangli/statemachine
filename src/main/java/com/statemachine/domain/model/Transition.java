package com.statemachine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transition {
    private String id;
    private String name;
    private String sourceStateId;
    private String destinationStateId;
    private String singleActionPerUser;
    private String sapuCtxList;
    private Condition condition;
    private String role; // Role required to use this transition
}

