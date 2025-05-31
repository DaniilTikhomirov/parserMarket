package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Smartphone {

    @Id
    private String id;


    @Lob
    @Column(columnDefinition = "TEXT")
    private String url;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String model;

    private String diagonal;

    private String memory;

    @Column(name = "op_memory")
    private String opMemory;

    private String version;

    @Column(name = "count_camera")
    private int countCamera;

    private double rating;
}