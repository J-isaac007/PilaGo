package com.queupid.pilago.model.person;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Abstraction: every person in the system has a name, an age and an address,
 * and must say what role they play. Residents, secretaries and captains
 * extend this class (inheritance) and override {@link #getRole()} (polymorphism).
 *
 * <p>All people share one database table; the {@code kind} column says which subclass a row is.
 */
@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind")
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int age;
    private String address;

    /** For JPA only. */
    protected Person() {
    }

    protected Person(String name, int age, String address) {
        setName(name);
        setAge(age);
        setAddress(address);
    }

    /** What this person does in PilaGo, e.g. {@code RESIDENT}, {@code SECRETARY}, {@code CAPTAIN}. */
    public abstract String getRole();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        this.name = name.strip();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Age must be between 0 and 130");
        }
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address == null ? null : address.strip();
    }

    @Override
    public String toString() {
        return getRole() + " " + name;
    }
}
