package com.example.presensi_qr.api;

import com.example.presensi_qr.models.LoginResponse;
import com.example.presensi_qr.models.Presence;
import java.util.List;
import okhttp3.ResponseBody;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {
    @FormUrlEncoded
    @POST("login")
    Call<LoginResponse> login(
        @Field("nis_nama") String nisNama,
        @Field("password") String password,
        @Field("device_id") String deviceId
    );

    @FormUrlEncoded
    @POST("register")
    Call<ResponseBody> register(
        @Field("name") String name,
        @Field("nis_nama") String nisNama,
        @Field("password") String password,
        @Field("role") String role,
        @Field("kelas") String kelas,
        @Field("tahun_masuk") String tahunMasuk
    );

    @FormUrlEncoded
    @POST("presence")
    Call<ResponseBody> submitPresence(
        @Header("Authorization") String token,
        @Field("subject") String subject,
        @Field("teacher") String teacher,
        @Field("latitude") double lat,
        @Field("longitude") double lng
    );

    @GET("history")
    Call<List<Presence>> getHistory(
        @Header("Authorization") String token,
        @Query("range") String range
    );

    @FormUrlEncoded
    @POST("subjects")
    Call<ResponseBody> addSubject(
        @Header("Authorization") String token,
        @Field("name") String name
    );

    @GET("subjects")
    Call<List<com.example.presensi_qr.models.Subject>> getSubjects(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @PUT("subjects/{id}")
    Call<ResponseBody> updateSubject(
        @Header("Authorization") String token,
        @Path("id") int id,
        @Field("name") String name
    );

    @DELETE("subjects/{id}")
    Call<ResponseBody> deleteSubject(
        @Header("Authorization") String token,
        @Path("id") int id
    );

    @FormUrlEncoded
    @POST("students/reset-device")
    Call<ResponseBody> resetDevice(
        @Header("Authorization") String token,
        @Field("nis") String nis
    );

    @FormUrlEncoded
    @POST("students/reset-password")
    Call<ResponseBody> resetPassword(
        @Header("Authorization") String token,
        @Field("nis") String nis,
        @Field("password") String password
    );

    @GET("classes")
    Call<List<com.example.presensi_qr.models.SchoolClass>> getClasses(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("classes")
    Call<ResponseBody> addClass(
        @Header("Authorization") String token,
        @Field("name") String name
    );

    @DELETE("classes/{id}")
    Call<ResponseBody> deleteClass(
        @Header("Authorization") String token,
        @Path("id") int id
    );

    @GET("years")
    Call<List<com.example.presensi_qr.models.SchoolYear>> getYears(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("years")
    Call<ResponseBody> addYear(
        @Header("Authorization") String token,
        @Field("year") String year
    );

    @DELETE("years/{id}")
    Call<ResponseBody> deleteYear(
        @Header("Authorization") String token,
        @Path("id") int id
    );

    @FormUrlEncoded
    @POST("change-password")
    Call<ResponseBody> changePassword(
        @Header("Authorization") String token,
        @Field("current_password") String currentPassword,
        @Field("new_password") String newPassword
    );

    // SCHEDULES
    @GET("schedules")
    Call<List<com.example.presensi_qr.models.TeachingSchedule>> getSchedules(
        @Header("Authorization") String token
    );

    @GET("schedules/my-class")
    Call<List<com.example.presensi_qr.models.TeachingSchedule>> getMyClassSchedules(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("schedules")
    Call<ResponseBody> addSchedule(
        @Header("Authorization") String token,
        @Field("semester") String semester,
        @Field("day") String day,
        @Field("subject") String subject,
        @Field("class_name") String className,
        @Field("start_time") String startTime,
        @Field("end_time") String endTime
    );

    @DELETE("schedules/{id}")
    Call<ResponseBody> deleteSchedule(
        @Header("Authorization") String token,
        @Path("id") int id
    );

    // SEMESTERS
    @GET("semesters")
    Call<List<String>> getSemesters(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("semesters")
    Call<ResponseBody> addSemester(
        @Header("Authorization") String token,
        @Field("semester") String semester
    );

    @GET("school-semesters")
    Call<List<com.example.presensi_qr.models.SchoolSemester>> getSchoolSemesters(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("school-semesters")
    Call<ResponseBody> addSchoolSemester(
        @Header("Authorization") String token,
        @Field("name") String name
    );

    @DELETE("school-semesters/{id}")
    Call<ResponseBody> deleteSchoolSemester(
        @Header("Authorization") String token,
        @Path("id") int id
    );

    // DISPENSATIONS
    @Multipart
    @POST("dispensation")
    Call<ResponseBody> submitDispensation(
        @Header("Authorization") String token,
        @Part("teacher_id") RequestBody teacherId,
        @Part("reason") RequestBody reason,
        @Part("date") RequestBody date,
        @Part MultipartBody.Part file
    );

    @GET("dispensation/pending")
    Call<List<com.example.presensi_qr.models.Dispensation>> getPendingDispensations(
        @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("dispensation/{id}/confirm")
    Call<ResponseBody> confirmDispensation(
        @Header("Authorization") String token,
        @Path("id") int id,
        @Field("status") String status // "approved" or "rejected"
    );

    // CLASS STUDENTS ATTENDANCE STATUS
    @GET("classes/{class_name}/students")
    Call<List<com.example.presensi_qr.models.User>> getClassStudents(
        @Header("Authorization") String token,
        @Path("class_name") String className,
        @Query("date") String date
    );
}
