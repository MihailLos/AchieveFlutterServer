package com.example.demo.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "message_error", schema = "public")
public class MessageError {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int id;
    @Column(name = "theme")
    private String theme;
    @Column(name = "description")
    private String description;
    @Column(name = "comment")
    private String comment;
    @Column(name = "message_error_date")
    private LocalDate messageErrorDate;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusMessageError statusMessageError;

    public int getIdError() {
        return id;
    }

    public void setIdError(int id) {
        this.id = id;
    }

    public String getThemeError() {
        return theme;
    }

    public void setThemeError(String theme) {
        this.theme = theme;
    }

    public String getDescriptionError() {
        return description;
    }

    public void setDescriptionError(String description) {
        this.description = description;
    }

    public String getCommentError() {
        return comment;
    }

    public void setCommentError(String comment) {
        this.comment = comment;
    }

    public Student getStudentError() {
        return student;
    }

    public void setStudentError(Student student) {
        this.student = student;
    }

    public StatusMessageError getStatusMessageError() {
        return statusMessageError;
    }

    public void setStatusMessageError(StatusMessageError statusMessageError) {
        this.statusMessageError = statusMessageError;
    }

    public LocalDate getMessageErrorDate() {
        return messageErrorDate;
    }

    public void setMessageErrorDate(LocalDate messageErrorDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String tempDateText = messageErrorDate.format(formatter);
        this.messageErrorDate = LocalDate.parse(tempDateText, formatter);
    }
}
