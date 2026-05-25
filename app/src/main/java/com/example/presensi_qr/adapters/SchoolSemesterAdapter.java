package com.example.presensi_qr.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.models.SchoolSemester;
import java.util.List;

public class SchoolSemesterAdapter extends RecyclerView.Adapter<SchoolSemesterAdapter.ViewHolder> {

    private final List<SchoolSemester> semesterList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(SchoolSemester schoolSemester);
    }

    public SchoolSemesterAdapter(List<SchoolSemester> semesterList, OnDeleteClickListener listener) {
        this.semesterList = semesterList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meta_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SchoolSemester item = semesterList.get(position);
        holder.tvName.setText(item.getName());
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return semesterList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_meta_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_meta);
        }
    }
}
