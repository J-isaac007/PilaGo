package com.queupid.pilago.web.staff;

import java.security.Principal;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.queupid.pilago.model.person.StaffAction;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.storage.StaffRepository;

/**
 * The staff member who is logged in, and the check that they may do an action.
 *
 * <p>The URL rules in {@code SecurityConfig} already keep captains out of {@code /staff} and
 * secretaries out of {@code /captain}. This second check asks the object itself:
 * {@code member.canPerform(action)} answers differently for a {@code BarangaySecretary} and a
 * {@code BarangayCaptain} (polymorphism), so the rule lives in the model, not in the controller.
 */
@Component
public class CurrentStaff {

    private final StaffRepository staff;

    public CurrentStaff(StaffRepository staff) {
        this.staff = staff;
    }

    public StaffMember get(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Not logged in");
        }
        return staff.findByStaffIdIgnoreCase(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Unknown staff ID " + principal.getName()));
    }

    /** @throws AccessDeniedException (shown as the 403 page) if this staff member may not do the action */
    public StaffMember require(Principal principal, StaffAction action) {
        StaffMember member = get(principal);
        if (!member.canPerform(action)) {
            throw new AccessDeniedException(member.getRole() + " may not " + action);
        }
        return member;
    }
}
