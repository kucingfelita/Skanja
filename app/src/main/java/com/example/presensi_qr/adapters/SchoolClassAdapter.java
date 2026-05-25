package com.example.presensi_qr.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.models.SchoolClass;
import java.util.List;

public class SchoolClassAdapter extends RecyclerView.Adapter<SchoolClassAdapter.ViewHolder> {

    private final List<SchoolClass> classList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(SchoolClass schoolClass);
    }

    public SchoolClassAdapter(List<SchoolClass> classList, OnDeleteClickListener listener) {
        this.classList = classList;
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
        SchoolClass item = classList.get(position);
        holder.tvName.setText(item.getName());
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
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
