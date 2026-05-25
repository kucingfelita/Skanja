package com.example.presensi_qr.models;

import com.google.gson.annotations.SerializedName;

public class TeachingSchedule {
    @SerializedName("id")
    private int id;

    @SerializedName("semester")
    private String semester; // e.g. "2025/2026 Ganjil"

    @SerializedName("day")
    private String day; // e.g. "Senin"

    @SerializedName("subject")
    private String subject; // e.g. "Matematika"

    @SerializedName("class_name")
    private String className; // e.g. "X-1"

    @SerializedName("start_time")
    private String startTime; // e.g. "Jam ke-1"

    @SerializedName("end_time")
    private String endTime; // e.g. "Jam ke-3"

    @SerializedName("user_id")
    private int userId;

    @SerializedName("teacher")
    private Teacher teacher;

    public static class Teacher {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;

        public int getId() { return id; }
        public String getName() { return name; }
    }

    public int getUserId() { return userId; }
    public Teacher getTeacher() { return teacher; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public int getId() { return id; }
    public String getSemester() { return semester; }
    public String getDay() { return day; }
    public String getSubject() { return subject; }
    public String getClassName() { return className; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public void setId(int id) { this.id = id; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setDay(String day) { this.day = day; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setClassName(String className) { this.className = className; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        String tName = (teacher != null && teacher.getName() != null) ? teacher.getName() : "Unknown";
        return day + " - " + subject + " (" + tName + ") " + startTime + "-" + endTime;
    }
}
