package com.queupid.pilago.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.queupid.pilago.model.person.StaffMember;

/** Staff accounts. The login page (Part 3) looks staff up by Staff ID. */
public interface StaffRepository extends JpaRepository<StaffMember, Long> {

    Optional<StaffMember> findByStaffIdIgnoreCase(String staffId);
}
