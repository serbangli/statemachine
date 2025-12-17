package com.statemachine.repository;

import com.statemachine.domain.entity.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<MachineEntity, Long> {
    @NonNull
    Optional<MachineEntity> findById(@NonNull Long id);
    @NonNull
    Optional<MachineEntity> findByManagedObjectIdAndManagedObjectType(String managedObjectId, String managedObjectType);
    List<MachineEntity> findByMachineDefinitionId(String machineDefinitionId);
        
}
