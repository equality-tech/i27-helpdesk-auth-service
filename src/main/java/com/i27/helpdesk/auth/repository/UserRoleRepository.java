package com.i27.helpdesk.auth.repository;

import com.i27.helpdesk.auth.model.UserRole;
import com.i27.helpdesk.auth.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(Long userId);
}
