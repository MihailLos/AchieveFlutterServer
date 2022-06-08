package com.example.demo.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Таблица для модераторов
@Entity
@Table(name = "moderator", schema = "public")
public class Moderator {

    //Колонки без foreign key
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int id;

    //Связь с таблицами по foreign key
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "moderator_institute",
            joinColumns = @JoinColumn(name = "moderator_id", referencedColumnName = "id", nullable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "institute_id", referencedColumnName = "id", nullable = false, updatable = false)
    )
    private Set<Institute> institutes = new HashSet<>();

    //Геттеры, сеттеры
    public int getIdModerator() {
        return id;
    }

    public void setIdModerator(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Institute> getInstitutes() {
        return institutes;
    }

    public Set<Integer> getInstitutesId() {
        Set<Integer> institutesId = new HashSet<>();
        for (Institute institute : institutes) {
            institutesId.add(institute.getIdInstitute());
        }
        return institutesId;
    }

    public void setInstitutes(Set<Institute> institutes) {
        this.institutes = institutes;
    }

    public void removeInstitute(Institute institute) {
        this.institutes.remove(institute);
        institute.getModerators().remove(this);
    }
}
