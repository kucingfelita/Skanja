package com.example.presensi_qr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.SchoolClass;
import com.example.presensi_qr.models.Subject;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageScheduleActivity extends AppCompatActivity {

    private TabLayout tabDays;
    private LinearLayout containerScheduleItems;
    private MaterialButton btnAddSchedule;
    private AutoCompleteTextView etSemester;

    private String token;
    private String selectedSemester = "";
    private String selectedDay = "Senin";

    private final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private final String[] HOURS = {
        "Jam ke-1", "Jam ke-2", "Jam ke-3", "Jam ke-4", "Jam ke-5",
        "Jam ke-6", "Jam ke-7", "Jam ke-8", "Jam ke-9", "Jam ke-10", "Jam ke-11"
    };

    private List<String> subjectNames = new ArrayList<>();
    private List<String> classNames = new ArrayList<>();
    private List<String> semesterList = new ArrayList<>();
    private List<com.example.presensi_qr.models.SchoolClass> rawClasses = new ArrayList<>();
    private List<com.example.presensi_qr.models.SchoolYear> rawYears = new ArrayList<>();

    private void combineClassesAndYears() {
        if (rawClasses.isEmpty() || rawYears.isEmpty()) return;
        classNames.clear();
        for (com.example.presensi_qr.models.SchoolClass c : rawClasses) {
            for (com.example.presensi_qr.models.SchoolYear y : rawYears) {
                classNames.add(c.getName() + " (" + y.getYear() + ")");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_schedule);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        token = pref.getString("TOKEN", "");
        if (token.isEmpty()) { finish(); return; }

        etSemester = findViewById(R.id.et_semester);
        tabDays = findViewById(R.id.tab_days);
        containerScheduleItems = findViewById(R.id.container_schedule_items);
        btnAddSchedule = findViewById(R.id.btn_add_schedule);

        // Setup tab hari
        for (String day : DAYS) {
            tabDays.addTab(tabDays.newTab().setText(day));
        }
        tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = DAYS[tab.getPosition()];
                loadScheduleForDay(selectedDay);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnAddSchedule.setOnClickListener(v -> addScheduleRow());

        loadMetaData();
    }

    private void loadMetaData() {
        // Load subjects
        ApiClient.getService().getSubjects(token).enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subjectNames.clear();
                    for (Subject s : response.body()) subjectNames.add(s.getName());
                }
            }
            @Override public void onFailure(Call<List<Subject>> call, Throwable t) {}
        });

        // Load classes
        ApiClient.getService().getClasses(token).enqueue(new Callback<List<SchoolClass>>() {
            @Override
            public void onResponse(Call<List<SchoolClass>> call, Response<List<SchoolClass>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rawClasses.clear();
                    rawClasses.addAll(response.body());
                    combineClassesAndYears();
                }
            }
            @Override public void onFailure(Call<List<SchoolClass>> call, Throwable t) {}
        });

        // Load years (batches)
        ApiClient.getService().getYears(token).enqueue(new Callback<List<com.example.presensi_qr.models.SchoolYear>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.SchoolYear>> call, Response<List<com.example.presensi_qr.models.SchoolYear>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rawYears.clear();
                    rawYears.addAll(response.body());
                    combineClassesAndYears();
                }
            }
            @Override public void onFailure(Call<List<com.example.presensi_qr.models.SchoolYear>> call, Throwable t) {}
        });

        // Load semesters
        ApiClient.getService().getSemesters(token).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    semesterList.clear();
                    semesterList.addAll(response.body());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageScheduleActivity.this,
                            android.R.layout.simple_dropdown_item_1line, semesterList);
                    etSemester.setAdapter(adapter);
                    etSemester.setOnItemClickListener((parent, view, position, id) ->
                            selectedSemester = semesterList.get(position));
                }
            }
            @Override public void onFailure(Call<List<String>> call, Throwable t) {}
        });

        // Load jadwal hari pertama
        loadScheduleForDay(selectedDay);
    }

    /** Tambah baris jadwal baru pada hari yang sedang aktif */
    private void addScheduleRow() {
        if (selectedSemester.isEmpty()) {
            Toast.makeText(this, "Pilih semester terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        View rowView = LayoutInflater.from(this).inflate(R.layout.item_schedule_row, containerScheduleItems, false);

        AutoCompleteTextView etSubject = rowView.findViewById(R.id.et_sched_subject);
        AutoCompleteTextView etClass   = rowView.findViewById(R.id.et_sched_class);
        AutoCompleteTextView etStart   = rowView.findViewById(R.id.et_sched_start);
        AutoCompleteTextView etEnd     = rowView.findViewById(R.id.et_sched_end);
        MaterialButton btnDelete       = rowView.findViewById(R.id.btn_delete_schedule_row);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjectNames);
        ArrayAdapter<String> classAdapter   = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, classNames);
        ArrayAdapter<String> hourAdapter    = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, HOURS);

        etSubject.setAdapter(subjectAdapter);
        etClass.setAdapter(classAdapter);
        etStart.setAdapter(hourAdapter);
        etEnd.setAdapter(hourAdapter);

        // Tombol simpan jadwal baru ini
        btnDelete.setText("Simpan & Hapus");
        btnDelete.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        btnDelete.setOnClickListener(v -> {
            String sub   = etSubject.getText().toString().trim();
            String cls   = etClass.getText().toString().trim();
            String start = etStart.getText().toString().trim();
            String end   = etEnd.getText().toString().trim();

            if (sub.isEmpty() || cls.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Lengkapi semua field jadwal!", Toast.LENGTH_SHORT).show();
                return;
            }

            saveSchedule(sub, cls, start, end, rowView);
        });

        containerScheduleItems.addView(rowView);
    }

    private void saveSchedule(String subject, String className, String startTime, String endTime, View rowView) {
        ApiClient.getService().addSchedule(token, selectedSemester, selectedDay, subject, className, startTime, endTime)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(ManageScheduleActivity.this, "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show();
                            containerScheduleItems.removeView(rowView);
                            loadScheduleForDay(selectedDay);
                        } else {
                            Toast.makeText(ManageScheduleActivity.this, "Gagal menyimpan jadwal (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(ManageScheduleActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadScheduleForDay(String day) {
        // Clear existing saved rows (keep unsaved input rows)
        containerScheduleItems.removeAllViews();

        if (selectedSemester.isEmpty()) return;

        ApiClient.getService().getSchedules(token).enqueue(new Callback<List<com.example.presensi_qr.models.TeachingSchedule>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call,
                                   Response<List<com.example.presensi_qr.models.TeachingSchedule>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (com.example.presensi_qr.models.TeachingSchedule sched : response.body()) {
                        if (day.equalsIgnoreCase(sched.getDay()) && selectedSemester.equalsIgnoreCase(sched.getSemester())) {
                            addSavedScheduleRow(sched);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call, Throwable t) {}
        });
    }

    private void addSavedScheduleRow(com.example.presensi_qr.models.TeachingSchedule sched) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_schedule_row, containerScheduleItems, false);

        AutoCompleteTextView etSubject = rowView.findViewById(R.id.et_sched_subject);
        AutoCompleteTextView etClass   = rowView.findViewById(R.id.et_sched_class);
        AutoCompleteTextView etStart   = rowView.findViewById(R.id.et_sched_start);
        AutoCompleteTextView etEnd     = rowView.findViewById(R.id.et_sched_end);
        MaterialButton btnDelete       = rowView.findViewById(R.id.btn_delete_schedule_row);

        etSubject.setText(sched.getSubject(), false);
        etClass.setText(sched.getClassName(), false);
        etStart.setText(sched.getStartTime(), false);
        etEnd.setText(sched.getEndTime(), false);

        // Disable editing saved rows
        etSubject.setEnabled(false);
        etClass.setEnabled(false);
        etStart.setEnabled(false);
        etEnd.setEnabled(false);

        btnDelete.setText("Hapus Jadwal");
        btnDelete.setOnClickListener(v -> {
            ApiClient.getService().deleteSchedule(token, sched.getId()).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        Toast.makeText(ManageScheduleActivity.this, "Jadwal dihapus", Toast.LENGTH_SHORT).show();
                        containerScheduleItems.removeView(rowView);
                    } else {
                        Toast.makeText(ManageScheduleActivity.this, "Gagal menghapus jadwal", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        });

        containerScheduleItems.addView(rowView);
    }
}
