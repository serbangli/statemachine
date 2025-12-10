package com.statemachine.repository;

import com.statemachine.domain.entity.MachineHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineHistoryRepository extends JpaRepository<MachineHistoryEntity, Long> {
    List<MachineHistoryEntity> findByMachineIdOrderByTimestampAsc(Long machineId);
    List<MachineHistoryEntity> findByMachineIdOrderByTimestampDesc(Long machineId);
}
