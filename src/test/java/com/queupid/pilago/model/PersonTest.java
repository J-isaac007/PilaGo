package com.queupid.pilago.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.model.person.BarangayCaptain;
import com.queupid.pilago.model.person.BarangaySecretary;
import com.queupid.pilago.model.person.Person;
import com.queupid.pilago.model.person.PriorityGroup;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.person.StaffAction;
import com.queupid.pilago.model.request.HoldReason;

class PersonTest {

    private static Resident resident(int age, boolean pwd, int months, boolean record) {
        return new Resident("Juan Dela Cruz", age, "Alangilan", months, pwd, record);
    }

    @Test
    void eachPersonReportsItsOwnRole() {
        List<Person> people = List.of(
                resident(30, false, 24, false),
                new BarangaySecretary("Sec", 35, "Hall", "SEC-001", "hash"),
                new BarangayCaptain("Cap", 50, "Hall", "CAP-001", "hash"));
        assertThat(people).extracting(Person::getRole).containsExactly("RESIDENT", "SECRETARY", "CAPTAIN");
    }

    @Test
    void priorityGroupFollowsAgeAndPwd() {
        assertThat(resident(60, false, 24, false).getPriorityGroup()).isEqualTo(PriorityGroup.SENIOR);
        assertThat(resident(59, true, 24, false).getPriorityGroup()).isEqualTo(PriorityGroup.PWD);
        assertThat(resident(70, true, 24, false).getPriorityGroup()).isEqualTo(PriorityGroup.SENIOR);
        assertThat(resident(59, false, 24, false).getPriorityGroup()).isEqualTo(PriorityGroup.REGULAR);
        assertThat(PriorityGroup.SENIOR.ticketNumber(4)).isEqualTo("S-004");
    }

    @Test
    void residencyCheck() {
        assertThatCode(() -> resident(30, false, 6, false).checkResidency()).doesNotThrowAnyException();
        assertThatThrownBy(() -> resident(30, false, 5, false).checkResidency())
                .isInstanceOf(NotAResidentException.class)
                .extracting(e -> ((NotAResidentException) e).getReason()).isEqualTo(HoldReason.SHORT_RESIDENCY);
        assertThatThrownBy(() -> resident(30, false, 24, true).checkResidency())
                .extracting(e -> ((NotAResidentException) e).getReason()).isEqualTo(HoldReason.PENDING_RECORD);
    }

    @Test
    void onlyTheCaptainSigns() {
        assertThat(new BarangayCaptain("Cap", 50, "Hall", "CAP-001", "h").canPerform(StaffAction.SIGN)).isTrue();
        assertThat(new BarangaySecretary("Sec", 35, "Hall", "SEC-001", "h").canPerform(StaffAction.SIGN)).isFalse();
        assertThat(new BarangaySecretary("Sec", 35, "Hall", "SEC-001", "h").canPerform(StaffAction.RELEASE)).isTrue();
    }

    @Test
    void invalidAgeIsRejected() {
        assertThatThrownBy(() -> resident(-1, false, 24, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
