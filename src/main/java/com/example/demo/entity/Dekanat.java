package com.example.demo.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

//Таблица для модераторов
@Entity
@Table(name = "dekanat", schema = "public")
public class Dekanat {

    //Колонки без foreign key
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int id;

    //Связь с таблицами по foreign key
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Institute getInstitute() {
        return institute;
    }

    public void setInstitute(Institute institute) {
        this.institute = institute;
    }
}
