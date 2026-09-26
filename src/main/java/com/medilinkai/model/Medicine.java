package com.medilinkai.model;

import jakarta.persistence.*;

/**
 * A branded medicine, e.g. "Napa 500mg" by Beximco. Belongs to one Generic.
 */
@Entity
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brandName;

    private String strength;

    private String company;

    /** Price in Bangladeshi Taka */
    private double priceBdt;

    @ManyToOne
    private Generic generic;

    public Medicine() {
    }

    public Medicine(String brandName, String strength, String company, double priceBdt, Generic generic) {
        this.brandName = brandName;
        this.strength = strength;
        this.company = company;
        this.priceBdt = priceBdt;
        this.generic = generic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public double getPriceBdt() {
        return priceBdt;
    }

    public void setPriceBdt(double priceBdt) {
        this.priceBdt = priceBdt;
    }

    public Generic getGeneric() {
        return generic;
    }

    public void setGeneric(Generic generic) {
        this.generic = generic;
    }
}
