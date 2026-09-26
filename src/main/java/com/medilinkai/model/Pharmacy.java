package com.medilinkai.model;

import jakarta.persistence.*;

@Entity
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Neighbourhood, e.g. "Dhanmondi" */
    private String area;

    private String address;

    private double lat;

    private double lng;

    private String phone;

    private boolean open24h;

    @ManyToOne
    private User owner;

    public Pharmacy() {
    }

    public Pharmacy(String name, String area, String address, double lat, double lng,
                    String phone, boolean open24h, User owner) {
        this.name = name;
        this.area = area;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.phone = phone;
        this.open24h = open24h;
        this.owner = owner;
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

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isOpen24h() {
        return open24h;
    }

    public void setOpen24h(boolean open24h) {
        this.open24h = open24h;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
