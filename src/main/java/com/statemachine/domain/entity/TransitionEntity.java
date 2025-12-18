package com.statemachine.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"machineDefinition"})
public class TransitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transitionId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sourceStateId;

    @Column(nullable = false)
    private String destinationStateId;

    @Column(columnDefinition = "TEXT")
    private String conditionExpression;
    
    @Column(nullable = false)
    private String singleActionPerUser;
    
    @Column(nullable = false)
    private String sapuCtxList;

    @Column
    private String role; // Role required to use this transition

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_definition_id", nullable = false)
    private MachineDefinitionEntity machineDefinition;
}

