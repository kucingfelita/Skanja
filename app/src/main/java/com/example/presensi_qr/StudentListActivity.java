package com.example.presensi_qr;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView rvStudents;
    private ProgressBar progressBar;
    private View layoutEmptyState;

    private StudentAdapter adapter;
    private List<User> studentList = new ArrayList<>();
    private String className;
    private String token;
    private String selectedDate;
    private com.google.android.material.button.MaterialButton btnDateFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        className = getIntent().getStringExtra("CLASS_NAME");
        if (className == null) className = "Kelas";

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Siswa " + className);
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        token = pref.getString("TOKEN", "");

        rvStudents = findViewById(R.id.rv_students);
        progressBar = findViewById(R.id.progress_bar);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnDateFilter = findViewById(R.id.btn_date_filter);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(studentList);
        rvStudents.setAdapter(adapter);

        // Default: today
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        selectedDate = sdf.format(new java.util.Date());
        btnDateFilter.setOnClickListener(v -> showDatePicker());

        loadStudents();
        calculateAndShowSession();
    }

    private void showDatePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = String.format("%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    btnDateFilter.setText(String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year1));
                    loadStudents();
                    calculateAndShowSession();
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void loadStudents() {
        if (token.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        String pureClass = className;
        String classYear = "";
        if (className.contains(" (")) {
            String[] parts = className.split(" \\(");
            pureClass = parts[0];
            classYear = parts[1].replace(")", "").trim();
        }

        final String finalPureClass = pureClass;
        final String finalClassYear = classYear;

        // Fetch with full name (e.g. "X-1 (2026)")
        ApiClient.getService().getClassStudents(token, className, selectedDate).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response1) {
                List<User> list1 = (response1.isSuccessful() && response1.body() != null) ? response1.body() : new ArrayList<>();
                
                // Fetch with pure class name (e.g. "X-1") if it's different
                if (!finalPureClass.equals(className)) {
                    ApiClient.getService().getClassStudents(token, finalPureClass, selectedDate).enqueue(new Callback<List<User>>() {
                        @Override
                        public void onResponse(Call<List<User>> call, Response<List<User>> response2) {
                            List<User> list2 = (response2.isSuccessful() && response2.body() != null) ? response2.body() : new ArrayList<>();
                            mergeAndDisplayStudents(list1, list2, finalClassYear);
                        }

                        @Override
                        public void onFailure(Call<List<User>> call, Throwable t) {
                            mergeAndDisplayStudents(list1, new ArrayList<>(), finalClassYear);
                        }
                    });
                } else {
                    mergeAndDisplayStudents(list1, new ArrayList<>(), finalClassYear);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                // Try pure class anyway
                if (!finalPureClass.equals(className)) {
                    ApiClient.getService().getClassStudents(token, finalPureClass, selectedDate).enqueue(new Callback<List<User>>() {
                        @Override
                        public void onResponse(Call<List<User>> call, Response<List<User>> response2) {
                            List<User> list2 = (response2.isSuccessful() && response2.body() != null) ? response2.body() : new ArrayList<>();
                            mergeAndDisplayStudents(new ArrayList<>(), list2, finalClassYear);
                        }

                        @Override
                        public void onFailure(Call<List<User>> call, Throwable t2) {
                            if (isFinishing() || isDestroyed()) return;
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(StudentListActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                            layoutEmptyState.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    if (isFinishing() || isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(StudentListActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void mergeAndDisplayStudents(List<User> list1, List<User> list2, String targetYear) {
        if (isFinishing() || isDestroyed()) return;
        progressBar.setVisibility(View.GONE);

        studentList.clear();
        java.util.HashSet<Integer> addedIds = new java.util.HashSet<>();

        // Add from list1 (full name matches)
        for (User u : list1) {
            if (addedIds.add(u.getId())) {
                studentList.add(u);
            }
        }

        // Add from list2 (pure class matches, but filter by year)
        for (User u : list2) {
            if (addedIds.contains(u.getId())) continue;

            // If targetYear is specified, filter by user's tahun_masuk or from their kelas format
            boolean matchesYear = true;
            if (!targetYear.isEmpty()) {
                String uYear = u.getTahunMasuk();
                if (uYear != null && !uYear.isEmpty()) {
                    matchesYear = uYear.equalsIgnoreCase(targetYear);
                } else if (u.getKelas() != null && u.getKelas().contains(" (")) {
                    String[] parts = u.getKelas().split(" \\(");
                    String uYearFromClass = parts[1].replace(")", "").trim();
                    matchesYear = uYearFromClass.equalsIgnoreCase(targetYear);
                } else {
                    matchesYear = false; // no year info, don't match
                }
            }

            if (matchesYear) {
                if (addedIds.add(u.getId())) {
                    studentList.add(u);
                }
            }
        }

        // Urutkan siswa berdasarkan abjad nama
        Collections.sort(studentList, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));

        adapter.notifyDataSetChanged();

        if (studentList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void showDispensationReviewDialog(User student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_review_dispensation, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvDetail = view.findViewById(R.id.tv_dialog_detail);
        MaterialButton btnViewPdf = view.findViewById(R.id.btn_view_pdf);
        MaterialButton btnApprove = view.findViewById(R.id.btn_approve);
        MaterialButton btnReject = view.findViewById(R.id.btn_reject);

        tvTitle.setText("Konfirmasi Dispensasi 📄");
        
        String teacherName = student.getDispensationTeacherName() != null ? student.getDispensationTeacherName() : "Guru Lain";
        String detailText = "Siswa: " + student.getName() + "\n" +
                            "NIS: " + student.getNisNama() + "\n\n" +
                            "Alasan:\n\"" + student.getDispensationReason() + "\"\n\n" +
                            "Ditujukan kepada: " + teacherName;
        
        if (!student.isDispensationOwner()) {
            detailText += "\n(Hanya guru bersangkutan yang dapat mengonfirmasi)";
        }
        tvDetail.setText(detailText);

        btnViewPdf.setOnClickListener(v -> {
            String pdfUrl = student.getDispensationPdf();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(StudentListActivity.this, "Berkas PDF tidak ditemukan!", Toast.LENGTH_SHORT).show();
            }
        });

        if (student.isDispensationOwner()) {
            btnApprove.setOnClickListener(v -> {
                dialog.dismiss();
                confirmDispen(student.getDispensationId(), "approved");
            });

            btnReject.setOnClickListener(v -> {
                dialog.dismiss();
                confirmDispen(student.getDispensationId(), "rejected");
            });
        } else {
            btnApprove.setText("TUTUP");
            btnApprove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9CA3AF"))); // Neutral Gray
            btnReject.setVisibility(View.GONE);
            
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) btnApprove.getLayoutParams();
            params.weight = 2.0f;
            params.setMarginStart(0);
            btnApprove.setLayoutParams(params);
            
            btnApprove.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void confirmDispen(int dispenId, String status) {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getService().confirmDispensation(token, dispenId, status).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(StudentListActivity.this, "Dispensasi berhasil dikonfirmasi!", Toast.LENGTH_SHORT).show();
                    loadStudents(); // Reload list
                } else {
                    Toast.makeText(StudentListActivity.this, "Gagal memproses konfirmasi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentListActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

        private final List<User> items;

        public StudentAdapter(List<User> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_attendance, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvNis.setText("NIS: " + item.getNisNama());

            String initial = "S";
            if (item.getName() != null && !item.getName().isEmpty()) {
                initial = String.valueOf(item.getName().charAt(0)).toUpperCase();
            }
            holder.tvInitial.setText(initial);

            // Styling status badge
            String status = item.getStatus();
            holder.tvStatus.setText(status.toUpperCase());

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16);

            if ("hadir".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Emerald Green
                shape.setColor(Color.parseColor("#D1FAE5"));
            } else if ("sakit".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Amber
                shape.setColor(Color.parseColor("#FEF3C7"));
            } else if ("izin".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#3B82F6")); // Blue
                shape.setColor(Color.parseColor("#DBEAFE"));
            } else if ("pending".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#8B5CF6")); // Purple
                shape.setColor(Color.parseColor("#EDE9FE"));
                holder.tvStatus.setText("DISPEN (PENDING)");
            } else {
                // Alpa
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
                shape.setColor(Color.parseColor("#FEE2E2"));
            }

            holder.tvStatus.setBackground(shape);

            // Jika status pending, guru bisa mengklik untuk meninjau dispen
            if ("pending".equalsIgnoreCase(status)) {
                holder.itemView.setOnClickListener(v -> showDispensationReviewDialog(item));
            } else {
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvNis, tvInitial, tvStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_student_name);
                tvNis = itemView.findViewById(R.id.tv_student_nis);
                tvInitial = itemView.findViewById(R.id.tv_student_initial);
                tvStatus = itemView.findViewById(R.id.tv_status_badge);
            }
        }
    }

    private void calculateAndShowSession() {
        if (token.isEmpty() || className == null || className.isEmpty()) return;

        ApiClient.getService().getSchedules(token).enqueue(new Callback<List<com.example.presensi_qr.models.TeachingSchedule>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call, Response<List<com.example.presensi_qr.models.TeachingSchedule>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String subject = "";
                    String semester = "";
                    for (com.example.presensi_qr.models.TeachingSchedule s : response.body()) {
                        if (className.equalsIgnoreCase(s.getClassName())) {
                            subject = s.getSubject();
                            semester = s.getSemester();
                            break;
                        }
                    }

                    if (subject.isEmpty()) {
                        String classPart = className.contains(" (") ? className.split(" \\(")[0] : className;
                        for (com.example.presensi_qr.models.TeachingSchedule s : response.body()) {
                            String sClass = s.getClassName();
                            String sClassPart = sClass.contains(" (") ? sClass.split(" \\(")[0] : sClass;
                            if (classPart.equalsIgnoreCase(sClassPart)) {
                                subject = s.getSubject();
                                semester = s.getSemester();
                                break;
                            }
                        }
                    }

                    if (!subject.isEmpty()) {
                        final String finalSubject = subject;
                        final String finalSemester = semester;
                        ApiClient.getService().getHistory(token, "all").enqueue(new Callback<List<com.example.presensi_qr.models.Presence>>() {
                            @Override
                            public void onResponse(Call<List<com.example.presensi_qr.models.Presence>> call, Response<List<com.example.presensi_qr.models.Presence>> responseHistory) {
                                if (isFinishing() || isDestroyed()) return;
                                if (responseHistory.isSuccessful() && responseHistory.body() != null) {
                                    List<com.example.presensi_qr.models.Presence> history = responseHistory.body();
                                    java.util.List<String> sortedUniqueDates = new ArrayList<>();

                                    String classPart = className.contains(" (") ? className.split(" \\(")[0] : className;

                                    for (com.example.presensi_qr.models.Presence p : history) {
                                        String pClass = p.getKelas();
                                        if (pClass != null && pClass.contains(" (")) {
                                            pClass = pClass.split(" \\(")[0];
                                        }
                                        if (p.getSubjectName().equalsIgnoreCase(finalSubject) && pClass != null && pClass.equalsIgnoreCase(classPart)) {
                                            if (isDateInSemester(p.getCreatedAt(), finalSemester)) {
                                                String dateOnly = p.getCreatedAt().split(" ")[0];
                                                String stdDate = getStandardizedDate(dateOnly);
                                                if (!sortedUniqueDates.contains(stdDate)) {
                                                    sortedUniqueDates.add(stdDate);
                                                }
                                            }
                                        }
                                    }

                                    java.util.Collections.sort(sortedUniqueDates, (d1, d2) -> {
                                        java.util.Date date1 = parseDateOnly(d1);
                                        java.util.Date date2 = parseDateOnly(d2);
                                        return date1.compareTo(date2);
                                    });

                                    String stdSelectedDate = getStandardizedDate(selectedDate);
                                    int sessionNum = -1;
                                    for (int i = 0; i < sortedUniqueDates.size(); i++) {
                                        if (sortedUniqueDates.get(i).equalsIgnoreCase(stdSelectedDate)) {
                                            sessionNum = i + 1;
                                            break;
                                        }
                                    }

                                    MaterialToolbar toolbar = findViewById(R.id.toolbar);
                                    if (sessionNum != -1) {
                                        toolbar.setSubtitle("Sesi " + sessionNum + " (" + finalSubject + ")");
                                    } else {
                                        int nextSession = sortedUniqueDates.size() + 1;
                                        toolbar.setSubtitle("Sesi " + nextSession + " (" + finalSubject + ")");
                                    }
                                }
                            }

                            @Override public void onFailure(Call<List<com.example.presensi_qr.models.Presence>> call, Throwable t) {}
                        });
                    }
                }
            }

            @Override public void onFailure(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call, Throwable t) {}
        });
    }

    private static java.util.Date parseDateOnly(String datePart) {
        if (datePart == null || datePart.isEmpty()) return new java.util.Date(0);
        try {
            if (datePart.contains("-")) {
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    int year, month, day;
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        year = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]) - 1;
                        day = Integer.parseInt(parts[2]);
                    } else { // dd-MM-yyyy
                        day = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]) - 1;
                        year = Integer.parseInt(parts[2]);
                    }
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(year, month, day, 0, 0, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.Date(0);
    }

    private static String getStandardizedDate(String datePart) {
        if (datePart == null || datePart.isEmpty()) return "";
        try {
            if (datePart.contains("-")) {
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    int year, month, day;
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        year = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]);
                        day = Integer.parseInt(parts[2]);
                    } else { // dd-MM-yyyy
                        day = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]);
                        year = Integer.parseInt(parts[2]);
                    }
                    return String.format(java.util.Locale.US, "%02d-%02d-%04d", day, month, year);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datePart;
    }

    public static boolean isDateInSemester(String createdAt, String semesterName) {
        if (createdAt == null || semesterName == null || semesterName.isEmpty()) return true;
        try {
            int year = 0;
            int month = 0;
            String datePart = createdAt.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                if (parts[0].length() == 4) { // yyyy-MM-dd
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                } else { // dd-MM-yyyy
                    year = Integer.parseInt(parts[2]);
                    month = Integer.parseInt(parts[1]);
                }
            } else {
                return true;
            }

            String cleanName = semesterName.toLowerCase();
            int year1 = 0, year2 = 0;
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{4})");
            java.util.regex.Matcher m = p.matcher(cleanName);
            if (m.find()) {
                year1 = Integer.parseInt(m.group(1));
            }
            if (m.find()) {
                year2 = Integer.parseInt(m.group(1));
            }

            if (year1 == 0) return true;
            if (year2 == 0) year2 = year1 + 1;

            boolean isGanjil = cleanName.contains("ganjil") || cleanName.contains("odd");
            boolean isGenap = cleanName.contains("genap") || cleanName.contains("even");

            if (isGanjil) {
                return (year == year1 && month >= 7 && month <= 12);
            } else if (isGenap) {
                return (year == year2 && month >= 1 && month <= 6);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
