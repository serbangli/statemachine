package com.statemachine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"machineDefinition"})
public class StateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stateId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StateType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_definition_id", nullable = false)
    private MachineDefinitionEntity machineDefinition;

    public enum StateType {
        START, END, REGULAR
    }
}

