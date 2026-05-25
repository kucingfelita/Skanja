package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class Presence {
    @SerializedName("id")
    private int id;

    @SerializedName("subject")
    private String subjectName;

    @SerializedName("teacher")
    private String teacherName;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("student_name")
    private String studentName;

    @SerializedName("kelas")
    private String kelas;

    public String getSubjectName() { return subjectName != null ? subjectName : "Tanpa Mapel"; }
    public String getTeacherName() { return teacherName != null ? teacherName : "-"; }
    public String getStatus() { return status != null ? status : "hadir"; }
    public String getCreatedAt() { return createdAt; }
    public String getStudentName() { return studentName != null ? studentName : ""; }
    public String getKelas() { return kelas != null ? kelas : ""; }
}
