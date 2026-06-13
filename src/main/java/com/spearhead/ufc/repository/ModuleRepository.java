package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    // Example: Find modules by roleId (customize as per your schema)
    List<Module> findByIsActiveTrue();
    // Add custom query for roleId if you have a mapping table
}
