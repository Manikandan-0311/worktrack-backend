package com.spearhead.ufc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	
	Optional<User> findByUsername(String username);

	// Optional<User> findByEmailId(String emailId);

	Optional<User> findByUserId(int userId);

    Optional<User> findFirstByEmployee_EmployeeId(Integer employeeId);
}
