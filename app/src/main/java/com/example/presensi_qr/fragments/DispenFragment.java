package com.example.presensi_qr.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.StudentListActivity;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.SchoolClass;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DispenFragment extends Fragment {

    private RecyclerView rvClasses;
    private ProgressBar progressBar;
    private View layoutEmptyState;
    
    private ClassAdapter adapter;
    private List<SchoolClass> classList = new ArrayList<>();
    private String token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dispen, container, false);

        rvClasses = view.findViewById(R.id.rv_classes);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        rvClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter(classList);
        rvClasses.setAdapter(adapter);

        SharedPreferences pref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        token = pref.getString("TOKEN", "");

        loadClasses();

        return view;
    }

    private void loadClasses() {
        if (token.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        ApiClient.getService().getSchedules(token).enqueue(new Callback<List<com.example.presensi_qr.models.TeachingSchedule>>() {
            @Override
            public void onResponse(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call, Response<List<com.example.presensi_qr.models.TeachingSchedule>> response) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    classList.clear();
                    java.util.HashSet<String> uniqueClasses = new java.util.HashSet<>();
                    for (com.example.presensi_qr.models.TeachingSchedule schedule : response.body()) {
                        String className = schedule.getClassName();
                        if (className != null && !className.isEmpty() && uniqueClasses.add(className)) {
                            SchoolClass sc = new SchoolClass(0, className);
                            classList.add(sc);
                        }
                    }
                    
                    adapter.notifyDataSetChanged();

                    if (classList.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Gagal memuat kelas", Toast.LENGTH_SHORT).show();
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<com.example.presensi_qr.models.TeachingSchedule>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Kesalahan jaringan", Toast.LENGTH_SHORT).show();
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

        private final List<SchoolClass> items;

        public ClassAdapter(List<SchoolClass> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_dispen, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SchoolClass item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), StudentListActivity.class);
                intent.putExtra("CLASS_NAME", item.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_class_name);
            }
        }
    }
}
