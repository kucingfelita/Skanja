package com.example.presensi_qr.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.models.Presence;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Presence> presenceList;

    public HistoryAdapter(List<Presence> presenceList) {
        this.presenceList = presenceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Memuat layout item_history yang sudah kita buat
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Presence presence = presenceList.get(position);
        
        // Mengisi data dinamis dari model Presence
        holder.tvSubject.setText(presence.getSubjectName());
        holder.tvTeacher.setText("Guru: " + presence.getTeacherName());
        holder.tvStatus.setText(presence.getStatus().toUpperCase());
        holder.tvDate.setText(presence.getCreatedAt());
        
        // Mengatur warna teks berdasarkan status (hadir = hijau, lainnya = merah)
        if (presence.getStatus().equalsIgnoreCase("hadir")) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return presenceList != null ? presenceList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvStatus, tvDate, tvTeacher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Menghubungkan ID di XML item_history ke variabel Java
            tvSubject = itemView.findViewById(R.id.tv_history_subject);
            tvTeacher = itemView.findViewById(R.id.tv_history_teacher);
            tvStatus = itemView.findViewById(R.id.tv_history_status);
            tvDate = itemView.findViewById(R.id.tv_history_date);
        }
    }
}
