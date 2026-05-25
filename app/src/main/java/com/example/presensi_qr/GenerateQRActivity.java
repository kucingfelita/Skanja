package com.example.presensi_qr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.TeachingSchedule;
import com.example.presensi_qr.models.Presence;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenerateQRActivity extends AppCompatActivity {

    private TextInputEditText etTeacher, etDate;
    private AutoCompleteTextView etSubject, etStartTime, etEndTime, etScheduleAutofill;
    private AutoCompleteTextView etSemesterInput, etClassAngkatanInput;
    private TextView tvSessionInfo;
    private ImageView ivQrCode;
    private MaterialButton btnGenerate;
    private TextView tvHintZoom;
    private Bitmap currentQrBitmap;
    private String selectedClassName = ""; // kelas dari jadwal yang dipilih
    private String selectedSemester = "";
    private List<TeachingSchedule> scheduleList = new ArrayList<>();
    private List<String> semesterList = new ArrayList<>();
    private List<String> classAndBatchList = new ArrayList<>();
    private List<com.example.presensi_qr.models.SchoolClass> rawClasses = new ArrayList<>();
    private List<com.example.presensi_qr.models.SchoolYear> rawYears = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTeacher = findViewById(R.id.et_teacher_name);
        etSubject = findViewById(R.id.et_subject);
        etSemesterInput = findViewById(R.id.et_semester);
        etClassAngkatanInput = findViewById(R.id.et_class_angkatan);
        tvSessionInfo = findViewById(R.id.tv_session_info);
        etDate = findViewById(R.id.et_date);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        etScheduleAutofill = findViewById(R.id.et_schedule_autofill);
        ivQrCode = findViewById(R.id.iv_qr_code);
        btnGenerate = findViewById(R.id.btn_generate);
        tvHintZoom = findViewById(R.id.tv_hint_zoom);

        // Isi otomatis nama guru
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        etTeacher.setText(pref.getString("NAME", "Guru"));

        // Isi otomatis tanggal hari ini
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        etDate.setText(day + "-" + (month + 1) + "-" + year);

        // Dropdown Jam Masuk & Jam Keluar (sampai Jam ke-11)
        String[] hours = new String[] {
            "Jam ke-1", "Jam ke-2", "Jam ke-3", "Jam ke-4", "Jam ke-5",
            "Jam ke-6", "Jam ke-7", "Jam ke-8", "Jam ke-9", "Jam ke-10", "Jam ke-11"
        };
        android.widget.ArrayAdapter<String> hoursAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, hours
        );
        etStartTime.setAdapter(hoursAdapter);
        etEndTime.setAdapter(hoursAdapter);

        etDate.setOnClickListener(v -> showDatePicker());

        btnGenerate.setOnClickListener(v -> generateQR());

        ivQrCode.setOnClickListener(v -> {
            if (currentQrBitmap != null) {
                showZoomDialog();
            }
        });
        
        loadSubjects();
        loadSchedules();
        loadSemesters();
        loadClassesAndYears();
    }

    private void loadSchedules() {
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");
        if (token.isEmpty()) return;

        ApiClient.getService().getSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
            @Override
            public void onResponse(Call<List<TeachingSchedule>> call, Response<List<TeachingSchedule>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    scheduleList.clear();
                    scheduleList.addAll(response.body());
                    ArrayAdapter<TeachingSchedule> adapter = new ArrayAdapter<>(
                            GenerateQRActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            scheduleList
                    );
                    etScheduleAutofill.setAdapter(adapter);
                    etScheduleAutofill.setOnItemClickListener((parent, view, position, id) -> {
                        TeachingSchedule selected = scheduleList.get(position);
                        // Autofill semua field
                        etSubject.setText(selected.getSubject(), false);
                        etStartTime.setText(selected.getStartTime(), false);
                        etEndTime.setText(selected.getEndTime(), false);
                        selectedClassName = selected.getClassName();
                        etClassAngkatanInput.setText(selectedClassName, false);
                        selectedSemester = selected.getSemester();
                        etSemesterInput.setText(selectedSemester, false);
                        calculateSessionNumber();
                    });
                }
            }
            @Override
            public void onFailure(Call<List<TeachingSchedule>> call, Throwable t) {}
        });
    }



    private void loadSubjects() {
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        if (token.isEmpty()) return;

        com.example.presensi_qr.api.ApiClient.getService().getSubjects(token).enqueue(new retrofit2.Callback<java.util.List<com.example.presensi_qr.models.Subject>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.example.presensi_qr.models.Subject>> call, retrofit2.Response<java.util.List<com.example.presensi_qr.models.Subject>> response) {
                if (isFinishing() || isDestroyed()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.example.presensi_qr.models.Subject> subjects = response.body();
                    android.widget.ArrayAdapter<com.example.presensi_qr.models.Subject> adapter = new android.widget.ArrayAdapter<>(
                            GenerateQRActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            subjects
                    );
                    etSubject.setAdapter(adapter);
                    etSubject.setOnItemClickListener((parent1, view1, position1, id1) -> {
                        calculateSessionNumber();
                    });
                } else {
                    android.util.Log.e("API_ERROR", "Gagal load subjects: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.example.presensi_qr.models.Subject>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                android.util.Log.e("API_ERROR", "Koneksi bermasalah saat load subjects: " + t.getMessage());
            }
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> etDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private void generateQR() {
        String teacher = etTeacher.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String semester = etSemesterInput.getText().toString().trim();
        String classAngkatan = etClassAngkatanInput.getText().toString().trim();

        if (teacher.isEmpty() || subject.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || semester.isEmpty() || classAngkatan.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("teacher", teacher);
            json.put("subject", subject);
            json.put("hour", startTime + " - " + endTime);
            json.put("date", date);
            json.put("start_time", startTime);
            json.put("end_time", endTime);
            json.put("class_name", classAngkatan); // kelas QR ini ditujukan
            json.put("semester", semester);
            json.put("session_number", calculatedSessionNum);
            json.put("token", "SECRET_" + UUID.randomUUID().toString());
            json.put("created_at", System.currentTimeMillis());

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(json.toString(), BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder encoder = new BarcodeEncoder();
            currentQrBitmap = encoder.createBitmap(matrix);
            
            ivQrCode.setImageBitmap(currentQrBitmap);
            ivQrCode.setVisibility(View.VISIBLE);
            tvHintZoom.setVisibility(View.VISIBLE);
            Toast.makeText(this, "QR Code Berhasil Dibuat", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void showZoomDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_zoom_qr);
        
        ImageView ivZoomed = dialog.findViewById(R.id.iv_qr_zoomed);
        ivZoomed.setImageBitmap(currentQrBitmap);

        // Tambahkan logic tutup untuk tombol di dialog
        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_dialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
        
        // Optional: set width to match parent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private int calculatedSessionNum = 1;

    private void loadSemesters() {
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");
        if (token.isEmpty()) return;

        ApiClient.getService().getSemesters(token).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    semesterList.clear();
                    semesterList.addAll(response.body());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            GenerateQRActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            semesterList
                    );
                    etSemesterInput.setAdapter(adapter);
                    etSemesterInput.setOnItemClickListener((parent, view, position, id) -> {
                        selectedSemester = semesterList.get(position);
                        calculateSessionNumber();
                    });
                }
            }
            @Override public void onFailure(Call<List<String>> call, Throwable t) {}
        });
    }

    private void loadClassesAndYears() {
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");
        if (token.isEmpty()) return;

        ApiClient.getService().getClasses(token).enqueue(new Callback<List<com.example.presensi_qr.models.SchoolClass>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.SchoolClass>> call, Response<List<com.example.presensi_qr.models.SchoolClass>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rawClasses.clear();
                    rawClasses.addAll(response.body());
                    combineClassesAndYearsForQR();
                }
            }
            @Override public void onFailure(Call<List<com.example.presensi_qr.models.SchoolClass>> call, Throwable t) {}
        });

        ApiClient.getService().getYears(token).enqueue(new Callback<List<com.example.presensi_qr.models.SchoolYear>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.SchoolYear>> call, Response<List<com.example.presensi_qr.models.SchoolYear>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rawYears.clear();
                    rawYears.addAll(response.body());
                    combineClassesAndYearsForQR();
                }
            }
            @Override public void onFailure(Call<List<com.example.presensi_qr.models.SchoolYear>> call, Throwable t) {}
        });
    }

    private void combineClassesAndYearsForQR() {
        if (rawClasses.isEmpty() || rawYears.isEmpty()) return;
        classAndBatchList.clear();
        for (com.example.presensi_qr.models.SchoolClass c : rawClasses) {
            for (com.example.presensi_qr.models.SchoolYear y : rawYears) {
                classAndBatchList.add(c.getName() + " (" + y.getYear() + ")");
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                GenerateQRActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                classAndBatchList
        );
        etClassAngkatanInput.setAdapter(adapter);
        etClassAngkatanInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedClassName = classAndBatchList.get(position);
            calculateSessionNumber();
        });
    }

    private void calculateSessionNumber() {
        String subject = etSubject.getText().toString().trim();
        String classAngkatan = etClassAngkatanInput.getText().toString().trim();
        String semester = etSemesterInput.getText().toString().trim();

        if (subject.isEmpty() || classAngkatan.isEmpty() || semester.isEmpty()) {
            tvSessionInfo.setVisibility(View.GONE);
            return;
        }

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");
        if (token.isEmpty()) return;

        ApiClient.getService().getHistory(token, "all").enqueue(new Callback<List<Presence>>() {
            @Override
            public void onResponse(Call<List<Presence>> call, Response<List<Presence>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Presence> history = response.body();
                    java.util.HashSet<String> uniqueDates = new java.util.HashSet<>();

                    String classPart = classAngkatan;
                    if (classAngkatan.contains(" (")) {
                        classPart = classAngkatan.split(" \\(")[0];
                    }

                    for (Presence p : history) {
                        String pClass = p.getKelas();
                        if (pClass != null && pClass.contains(" (")) {
                            pClass = pClass.split(" \\(")[0];
                        }
                        if (p.getSubjectName().equalsIgnoreCase(subject) && pClass != null && pClass.equalsIgnoreCase(classPart)) {
                            if (isDateInSemester(p.getCreatedAt(), semester)) {
                                String dateOnly = p.getCreatedAt().split(" ")[0];
                                uniqueDates.add(dateOnly);
                            }
                        }
                    }

                    calculatedSessionNum = uniqueDates.size() + 1;
                    tvSessionInfo.setText("Sesi ke-" + calculatedSessionNum + " (Berdasarkan " + uniqueDates.size() + " tanggal unik riwayat)");
                    tvSessionInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Presence>> call, Throwable t) {}
        });
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
