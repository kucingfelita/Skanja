package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class Dispensation {
    @SerializedName("id")
    private int id;

    @SerializedName("student_name")
    private String studentName;

    @SerializedName("nis")
    private String nis;

    @SerializedName("class_name")
    private String className;

    @SerializedName("reason")
    private String reason;

    @SerializedName("date")
    private String date; // dd-MM-yyyy

    @SerializedName("pdf_path")
    private String pdfPath; // file URL/path

    @SerializedName("status")
    private String status; // "pending", "approved", "rejected"

    public int getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getNis() { return nis; }
    public String getClassName() { return className; }
    public String getReason() { return reason; }
    public String getDate() { return date; }
    public String getPdfPath() { return pdfPath; }
    public String getStatus() { return status; }

    public void setId(int id) { this.id = id; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setNis(String nis) { this.nis = nis; }
    public void setClassName(String className) { this.className = className; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDate(String date) { this.date = date; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public void setStatus(String status) { this.status = status; }
}
