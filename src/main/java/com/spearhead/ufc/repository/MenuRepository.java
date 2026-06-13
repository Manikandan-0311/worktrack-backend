package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.MenuTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<MenuTO, Integer> {
    List<MenuTO> findByIsActiveTrue();
}
