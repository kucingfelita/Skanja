package com.example.presensi_qr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.adapters.SchoolClassAdapter;
import com.example.presensi_qr.adapters.SchoolYearAdapter;
import com.example.presensi_qr.adapters.SchoolSemesterAdapter;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.SchoolClass;
import com.example.presensi_qr.models.SchoolYear;
import com.example.presensi_qr.models.SchoolSemester;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ManageClassYearActivity extends AppCompatActivity {

    private TextInputEditText etClassName, etYearValue, etSemesterName;
    private MaterialButton btnAddClass, btnAddYear, btnAddSemester;
    private RecyclerView rvClasses, rvYears, rvSemesters;

    private SchoolClassAdapter classAdapter;
    private SchoolYearAdapter yearAdapter;
    private SchoolSemesterAdapter semesterAdapter;

    private final List<SchoolClass> classList = new ArrayList<>();
    private final List<SchoolYear> yearList = new ArrayList<>();
    private final List<SchoolSemester> semesterList = new ArrayList<>();

    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_class_year);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        token = pref.getString("TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etClassName = findViewById(R.id.et_class_name);
        etYearValue = findViewById(R.id.et_year_value);
        etSemesterName = findViewById(R.id.et_semester_name);
        btnAddClass = findViewById(R.id.btn_add_class);
        btnAddYear = findViewById(R.id.btn_add_year);
        btnAddSemester = findViewById(R.id.btn_add_semester);
        rvClasses = findViewById(R.id.rv_classes);
        rvYears = findViewById(R.id.rv_years);
        rvSemesters = findViewById(R.id.rv_semesters);

        // Config lists
        rvClasses.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new SchoolClassAdapter(classList, this::confirmDeleteClass);
        rvClasses.setAdapter(classAdapter);

        rvYears.setLayoutManager(new LinearLayoutManager(this));
        yearAdapter = new SchoolYearAdapter(yearList, this::confirmDeleteYear);
        rvYears.setAdapter(yearAdapter);

        rvSemesters.setLayoutManager(new LinearLayoutManager(this));
        semesterAdapter = new SchoolSemesterAdapter(semesterList, this::confirmDeleteSemester);
        rvSemesters.setAdapter(semesterAdapter);

        btnAddClass.setOnClickListener(v -> addClass());
        btnAddYear.setOnClickListener(v -> addYear());
        btnAddSemester.setOnClickListener(v -> addSemester());

        fetchClasses();
        fetchYears();
        fetchSemesters();
    }

    private void fetchClasses() {
        ApiClient.getService().getClasses(token).enqueue(new Callback<List<SchoolClass>>() {
            @Override
            public void onResponse(Call<List<SchoolClass>> call, Response<List<SchoolClass>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    classList.clear();
                    classList.addAll(response.body());
                    classAdapter.notifyDataSetChanged();
                } else {
                    Log.e("API_ERROR", "Failed fetching classes: code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SchoolClass>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void fetchYears() {
        ApiClient.getService().getYears(token).enqueue(new Callback<List<SchoolYear>>() {
            @Override
            public void onResponse(Call<List<SchoolYear>> call, Response<List<SchoolYear>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    yearList.clear();
                    yearList.addAll(response.body());
                    yearAdapter.notifyDataSetChanged();
                } else {
                    Log.e("API_ERROR", "Failed fetching years: code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SchoolYear>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void addClass() {
        String name = etClassName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Masukkan nama kelas terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddClass.setEnabled(false);
        ApiClient.getService().addClass(token, name).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                btnAddClass.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Kelas berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    etClassName.setText("");
                    fetchClasses();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menambahkan kelas" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnAddClass.setEnabled(true);
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addYear() {
        String year = etYearValue.getText().toString().trim();
        if (year.isEmpty()) {
            Toast.makeText(this, "Masukkan tahun terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddYear.setEnabled(false);
        ApiClient.getService().addYear(token, year).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                btnAddYear.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Tahun masuk berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    etYearValue.setText("");
                    fetchYears();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menambahkan tahun masuk" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnAddYear.setEnabled(true);
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteClass(SchoolClass item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Kelas")
                .setMessage("Apakah Anda yakin ingin menghapus kelas \"" + item.getName() + "\"? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> deleteClass(item.getId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteClass(int id) {
        ApiClient.getService().deleteClass(token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Kelas berhasil dihapus", Toast.LENGTH_SHORT).show();
                    fetchClasses();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menghapus kelas" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteYear(SchoolYear item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Tahun Masuk")
                .setMessage("Apakah Anda yakin ingin menghapus tahun masuk \"" + item.getYear() + "\"? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> deleteYear(item.getId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteYear(int id) {
        ApiClient.getService().deleteYear(token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Tahun masuk berhasil dihapus", Toast.LENGTH_SHORT).show();
                    fetchYears();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menghapus tahun masuk" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchSemesters() {
        ApiClient.getService().getSchoolSemesters(token).enqueue(new Callback<List<SchoolSemester>>() {
            @Override
            public void onResponse(Call<List<SchoolSemester>> call, Response<List<SchoolSemester>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    semesterList.clear();
                    semesterList.addAll(response.body());
                    semesterAdapter.notifyDataSetChanged();
                } else {
                    Log.e("API_ERROR", "Failed fetching semesters: code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SchoolSemester>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void addSemester() {
        String name = etSemesterName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Masukkan nama semester terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddSemester.setEnabled(false);
        ApiClient.getService().addSchoolSemester(token, name).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                btnAddSemester.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Semester berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    etSemesterName.setText("");
                    fetchSemesters();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menambahkan semester" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnAddSemester.setEnabled(true);
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteSemester(SchoolSemester item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Semester")
                .setMessage("Apakah Anda yakin ingin menghapus semester \"" + item.getName() + "\"? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> deleteSemester(item.getId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteSemester(int id) {
        ApiClient.getService().deleteSchoolSemester(token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(ManageClassYearActivity.this, "Semester berhasil dihapus", Toast.LENGTH_SHORT).show();
                    fetchSemesters();
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = " (" + response.code() + ": " + response.errorBody().string() + ")";
                        }
                    } catch (Exception e) {
                        errorDetail = " (Error " + response.code() + ")";
                    }
                    Toast.makeText(ManageClassYearActivity.this, "Gagal menghapus semester" + errorDetail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ManageClassYearActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
