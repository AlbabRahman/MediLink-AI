package com.medilinkai.model;

import jakarta.persistence.*;

/**
 * A generic (chemical) name, e.g. Paracetamol. Many brands map to one generic.
 */
@Entity
public class Generic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    /** What it treats, in plain words, e.g. "Fever and pain" */
    private String treats;

    public Generic() {
    }

    public Generic(String name, String treats) {
        this.name = name;
        this.treats = treats;
    }

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

    public String getTreats() {
        return treats;
    }

    public void setTreats(String treats) {
        this.treats = treats;
    }
}
