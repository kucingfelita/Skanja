package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class SchoolClass {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public SchoolClass() {}

    public SchoolClass(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
