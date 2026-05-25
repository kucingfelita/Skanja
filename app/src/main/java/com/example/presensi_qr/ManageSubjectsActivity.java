package com.example.presensi_qr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.Subject;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class ManageSubjectsActivity extends AppCompatActivity {

    private RecyclerView rvSubjects;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private SubjectAdapter adapter;
    private List<Subject> subjectList = new ArrayList<>();
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subjects);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSubjects = findViewById(R.id.rv_subjects);
        pbLoading = findViewById(R.id.pb_loading);
        tvEmpty = findViewById(R.id.tv_empty);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectAdapter();
        rvSubjects.setAdapter(adapter);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        token = pref.getString("TOKEN", "");

        loadSubjects();
    }

    private void loadSubjects() {
        if (token.isEmpty()) return;

        pbLoading.setVisibility(View.VISIBLE);
        rvSubjects.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getService().getSubjects(token).enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    subjectList = response.body();
                    adapter.notifyDataSetChanged();
                    
                    if (subjectList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvSubjects.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(ManageSubjectsActivity.this, "Gagal memuat mata pelajaran", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ManageSubjectsActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(Subject subject) {
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject, null);
        final EditText input = viewInflated.findViewById(R.id.et_edit_subject_name);
        input.setText(subject.getName());
        input.setSelection(subject.getName().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Mata Pelajaran")
                .setView(viewInflated)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(ManageSubjectsActivity.this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Check duplicate locally
                    for (Subject s : subjectList) {
                        if (s.getId() != subject.getId() && s.getName().equalsIgnoreCase(newName)) {
                            Toast.makeText(ManageSubjectsActivity.this, "Mata Pelajaran ini sudah ada!", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    updateSubject(subject.getId(), newName);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void updateSubject(int id, String newName) {
        pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().updateSubject(token, id, newName).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(ManageSubjectsActivity.this, "Berhasil diupdate", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                } else {
                    Toast.makeText(ManageSubjectsActivity.this, "Gagal mengupdate (Error " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ManageSubjectsActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Subject subject) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Mata Pelajaran")
                .setMessage("Apakah Anda yakin ingin menghapus \"" + subject.getName() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteSubject(subject.getId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteSubject(int id) {
        pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getService().deleteSubject(token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(ManageSubjectsActivity.this, "Berhasil dihapus", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                } else {
                    Toast.makeText(ManageSubjectsActivity.this, "Gagal menghapus (Error " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ManageSubjectsActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // RecyclerView Adapter
    private class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

        @NonNull
        @Override
        public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
            return new SubjectViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
            Subject s = subjectList.get(position);
            holder.tvName.setText(s.getName());
            holder.btnEdit.setOnClickListener(v -> showEditDialog(s));
            holder.btnDelete.setOnClickListener(v -> confirmDelete(s));
        }

        @Override
        public int getItemCount() {
            return subjectList.size();
        }

        class SubjectViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageButton btnEdit, btnDelete;

            public SubjectViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_subject_name);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
