package com.statemachine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "machine_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineDefinitionEntity {
    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String xmlContent;

    @OneToMany(mappedBy = "machineDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StateEntity> states = new ArrayList<>();

    @OneToMany(mappedBy = "machineDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TransitionEntity> transitions = new ArrayList<>();
}

