package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class Subject {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("teacher_id")
    private int teacherId;

    public Subject() {}

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTeacherId() {
        return teacherId;
    }

    @Override
    public String toString() {
        // Return name so it shows up nicely in the Dropdown/ArrayAdapter
        return name;
    }
}
