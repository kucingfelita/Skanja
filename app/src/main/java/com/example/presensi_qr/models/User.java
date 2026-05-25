package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("nis_nama")
    private String nisNama;
    
    @SerializedName("role")
    private String role;
    
    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("kelas")
    private String kelas;

    @SerializedName("tahun_masuk")
    private String tahunMasuk;

    @SerializedName("status")
    private String status; // "Hadir", "Sakit", "Izin", "Alpa", "pending"

    @SerializedName("dispensation_id")
    private int dispensationId;

    @SerializedName("dispensation_reason")
    private String dispensationReason;

    @SerializedName("dispensation_pdf")
    private String dispensationPdf;

    @SerializedName("is_dispensation_owner")
    private boolean isDispensationOwner;

    @SerializedName("dispensation_teacher_name")
    private String dispensationTeacherName;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getNisNama() { return nisNama; }
    public String getRole() { return role; }
    public String getDeviceId() { return deviceId; }
    public String getKelas() { return kelas; }
    public String getTahunMasuk() { return tahunMasuk; }
    public String getStatus() { return status != null ? status : "Alpa"; }
    public int getDispensationId() { return dispensationId; }
    public String getDispensationReason() { return dispensationReason; }
    public String getDispensationPdf() { return dispensationPdf; }
    public boolean isDispensationOwner() { return isDispensationOwner; }
    public String getDispensationTeacherName() { return dispensationTeacherName; }
}
