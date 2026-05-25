package com.example.presensi_qr.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.presensi_qr.GenerateQRActivity;
import com.example.presensi_qr.R;
import com.example.presensi_qr.ScanActivity;
import com.example.presensi_qr.RegisterActivity;
import com.example.presensi_qr.ResetStudentActivity;
import com.example.presensi_qr.ManageClassYearActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private String userRole = "murid";
    private MaterialCardView cardAttendanceInfo;
    private TextView tvAttendanceDetail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // FIX: Ambil dari SharedPreferences, lebih aman daripada Intent
        if (getContext() != null) {
            SharedPreferences pref = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            userRole = pref.getString("ROLE", "murid");
            String name = pref.getString("NAME", "User");
            
            TextView tvUserName = view.findViewById(R.id.tv_user_name);
            tvUserName.setText(name);
        }

        TextView tvActionTitle = view.findViewById(R.id.tv_action_title);
        TextView tvActionDesc = view.findViewById(R.id.tv_action_desc);
        ImageView ivActionIcon = view.findViewById(R.id.iv_action_icon);
        MaterialButton btnAction = view.findViewById(R.id.btn_action);
        MaterialButton btnApplyDispensation = view.findViewById(R.id.btn_apply_dispensation);
        MaterialButton btnAddSubject = view.findViewById(R.id.btn_add_subject);
        MaterialButton btnAddTeacher = view.findViewById(R.id.btn_add_teacher);
        MaterialButton btnAddOperator = view.findViewById(R.id.btn_add_operator);
        MaterialButton btnResetStudent = view.findViewById(R.id.btn_reset_student);
        MaterialButton btnManageClassYear = view.findViewById(R.id.btn_manage_class_year);
        MaterialButton btnManageSchedule = view.findViewById(R.id.btn_manage_schedule);
        cardAttendanceInfo = view.findViewById(R.id.card_attendance_info);
        tvAttendanceDetail = view.findViewById(R.id.tv_attendance_detail);

        if ("operator".equalsIgnoreCase(userRole) || "admin".equalsIgnoreCase(userRole)) {
            tvActionTitle.setText("Kelola Mata Pelajaran");
            tvActionDesc.setText("Tambah atau edit mata pelajaran");
            ivActionIcon.setImageResource(android.R.drawable.ic_menu_add);
            btnAction.setText("KELOLA MATA PELAJARAN");
            btnAction.setOnClickListener(v -> startActivity(new Intent(getContext(), com.example.presensi_qr.AddSubjectActivity.class)));
            
            btnApplyDispensation.setVisibility(View.GONE);
            btnAddSubject.setVisibility(View.GONE);
            btnManageSchedule.setVisibility(View.GONE);
            btnAddTeacher.setVisibility(View.VISIBLE);
            btnAddOperator.setVisibility(View.VISIBLE);
            btnResetStudent.setVisibility(View.VISIBLE);
            btnManageClassYear.setVisibility(View.VISIBLE);
            cardAttendanceInfo.setVisibility(View.GONE);

            btnAddTeacher.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RegisterActivity.class);
                intent.putExtra("TARGET_ROLE", "guru");
                startActivity(intent);
            });

            btnAddOperator.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RegisterActivity.class);
                intent.putExtra("TARGET_ROLE", "operator");
                startActivity(intent);
            });

            btnResetStudent.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ResetStudentActivity.class);
                startActivity(intent);
            });

            btnManageClassYear.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ManageClassYearActivity.class);
                startActivity(intent);
            });
        } else if ("guru".equalsIgnoreCase(userRole)) {
            tvActionTitle.setText("Buat QR Presensi");
            tvActionDesc.setText("Generate kode QR untuk murid");
            ivActionIcon.setImageResource(android.R.drawable.ic_menu_edit);
            btnAction.setText("BUAT KODE QR");
            btnAction.setOnClickListener(v -> startActivity(new Intent(getContext(), GenerateQRActivity.class)));
            
            btnApplyDispensation.setVisibility(View.GONE);
            btnAddSubject.setVisibility(View.GONE); // Guru tidak bisa lagi menambah mata pelajaran
            btnManageSchedule.setVisibility(View.VISIBLE);
            btnManageSchedule.setOnClickListener(v -> startActivity(new Intent(getContext(), com.example.presensi_qr.ManageScheduleActivity.class)));
            btnAddTeacher.setVisibility(View.GONE);
            btnAddOperator.setVisibility(View.GONE);
            btnResetStudent.setVisibility(View.GONE);
            btnManageClassYear.setVisibility(View.GONE);
            cardAttendanceInfo.setVisibility(View.GONE);
        } else {
            tvActionTitle.setText("Scan QR Code");
            tvActionDesc.setText("Ketuk untuk melakukan presensi");
            ivActionIcon.setImageResource(android.R.drawable.ic_menu_camera);
            btnAction.setText("MULAI SCAN");
            btnAction.setOnClickListener(v -> startActivity(new Intent(getContext(), ScanActivity.class)));
            
            btnApplyDispensation.setVisibility(View.VISIBLE);
            btnApplyDispensation.setOnClickListener(v -> startActivity(new Intent(getContext(), com.example.presensi_qr.ApplyDispensationActivity.class)));
            
            btnAddSubject.setVisibility(View.GONE);
            btnManageSchedule.setVisibility(View.GONE);
            btnAddTeacher.setVisibility(View.GONE);
            btnAddOperator.setVisibility(View.GONE);
            btnResetStudent.setVisibility(View.GONE);
            btnManageClassYear.setVisibility(View.GONE);
            checkAttendanceStatus();
        }


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ("murid".equals(userRole)) {
            checkAttendanceStatus();
        }
    }

    private void checkAttendanceStatus() {
        if (getContext() == null) return;

        SharedPreferences userPref = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String nisNama = userPref.getString("NIS_NAMA", "anonymous");
        
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        SharedPreferences pref = getContext().getSharedPreferences("AttendancePrefs", Context.MODE_PRIVATE);
        
        boolean isAbsent = pref.getBoolean("is_absent_" + nisNama + "_" + today, false);
        if (isAbsent) {
            String subject = pref.getString("absent_subject_" + nisNama + "_" + today, "");
            String teacher = pref.getString("absent_teacher_" + nisNama + "_" + today, "");
            
            tvAttendanceDetail.setText(subject + " - " + teacher);
            cardAttendanceInfo.setVisibility(View.VISIBLE);
        } else {
            cardAttendanceInfo.setVisibility(View.GONE);
        }
    }
}