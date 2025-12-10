package com.statemachine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "machine_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = true)
    private String fromStateId;

    @Column(nullable = false)
    private String toStateId;

    @Column(nullable = false)
    private String transitionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_snapshot")
    private Map<String, Object> contextSnapshot = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
