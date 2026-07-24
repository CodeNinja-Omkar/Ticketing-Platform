// user/repository/UserRepository.java
package com.ticketing.user.repository;

import com.ticketing.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}