package com.example.demo.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Таблица институтов
@Entity
@Table(name = "institute", schema = "public")
public class Institute {
    //Колонки без foreign key
    @Id
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String instituteName;
    @Column(name = "full_name")
    private String instituteFullName;

    @ManyToMany(mappedBy = "institutes", fetch = FetchType.LAZY)
    private Set<Moderator> moderators = new HashSet<>();

    //Геттеры, сеттеры
    public int getIdInstitute() {
        return id;
    }

    public void setIdInstitute(int id) {
        this.id = id;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public String getInstituteFullName() {
        return instituteFullName;
    }

    public void setInstituteFullName(String instituteFullName) {
        this.instituteFullName = instituteFullName;
    }

    public Set<Moderator> getModerators() {
        return moderators;
    }

    public void setModerators(Set<Moderator> moderators) {
        this.moderators = moderators;
    }
}
