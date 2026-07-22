package com.ticketing.catalog.domain;
import jakarta.persistence.*;
import java.util.UUID;


@Entity
@Table(name="venues")
public class Venue {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private int capacity;

    protected Venue(){
        //jpa
    }
    public Venue(String name, String city, int capacity){
        this.name = name;
        this.city = city;
        this.capacity = capacity;
    }
    public UUID getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getCity(){
        return city;
    }
    public int getCapacity(){
        return capacity;
    }

}
