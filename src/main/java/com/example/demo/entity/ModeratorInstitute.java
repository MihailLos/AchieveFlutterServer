package com.example.demo.entity;

//Промежуточная таблица для связи модераторов с институтами. Создана, чтобы избежать связи МногиеКоМногим

import javax.persistence.*;

@Entity
@Table(name = "moderator_institute", schema = "public")
public class ModeratorInstitute {

    //Колонки без foreign key
    @Id
    @Column(name = "id")
    private Integer id;

    // Колонки с foreign key
    @ManyToOne
    @JoinColumn(name = "moderator_id")
    private Moderator moderator;
    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Moderator getModerator() {
        return moderator;
    }

    public void setModerator(Moderator moderator) {
        this.moderator = moderator;
    }

    public Institute getInstitute() {
        return institute;
    }

    public void setInstitute(Institute institute) {
        this.institute = institute;
    }
}
