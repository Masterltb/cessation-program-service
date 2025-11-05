package com.smokefree.program.domain.model;


public class PlanTemplate {

    private Long id;
    private String name;

    // Constructor
    public PlanTemplate(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}