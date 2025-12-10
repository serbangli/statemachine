package com.statemachine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private String id;
    private String name;
    private StateType type; // START, END, REGULAR

    public enum StateType {
        START, END, REGULAR
    }
}

