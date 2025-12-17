package com.statemachine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineDefinition {
    @NotNull
    private String id;
    private String name;
    private State startState;
    private State endState;
    private List<State> states = new ArrayList<>();
    private List<Transition> transitions = new ArrayList<>();
}

