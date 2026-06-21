package com.tools.manager.repository;

import com.tools.manager.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandRepository extends JpaRepository<Command, Long> {

    List<Command> findByModuleIdOrderBySortOrderAsc(Long moduleId);

    List<Command> findByModuleId(Long moduleId);

}
