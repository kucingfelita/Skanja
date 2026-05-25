package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class SchoolYear {
    @SerializedName("id")
    private int id;

    @SerializedName("year")
    private String year;

    public SchoolYear() {}

    public SchoolYear(int id, String year) {
        this.id = id;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public String getYear() {
        return year;
    }

    @Override
    public String toString() {
        return year;
    }
}
