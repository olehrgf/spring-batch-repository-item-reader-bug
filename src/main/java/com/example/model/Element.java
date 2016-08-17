package com.example.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Element {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String value;

    private String status = "new";

    public String getStatus() {
        return status;
    }

    public Element setStatus(String status) {
        this.status = status;
        return this;
    }

    public long getId() {
        return id;
    }

    public Element setId(long id) {
        this.id = id;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Element setValue(String value) {
        this.value = value;
        return this;
    }
}
