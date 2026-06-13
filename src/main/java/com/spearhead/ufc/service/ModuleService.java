package com.spearhead.ufc.service;

import com.spearhead.ufc.model.Module;
import com.spearhead.ufc.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ModuleService {
    @Autowired
    private ModuleRepository moduleRepository;

    public List<Module> getAllModules() {
        return moduleRepository.findByIsActiveTrue();
    }

    public List<Module> getModulesForRole(Integer roleId) {
        return moduleRepository.findByIsActiveTrue();
    }
}
