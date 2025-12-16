package com.statemachine.repository;

import com.statemachine.domain.entity.MachineDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineDefinitionRepository extends JpaRepository<MachineDefinitionEntity, String> {
    @NonNull
    Optional<MachineDefinitionEntity> findById(@NonNull String id);
}

