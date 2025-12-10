package com.statemachine.repository;

import com.statemachine.domain.entity.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<MachineEntity, Long> {
    Optional<MachineEntity> findById(Long id);
    List<MachineEntity> findByMachineDefinitionId(String machineDefinitionId);
}

