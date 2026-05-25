package com.example.presensi_qr.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.presensi_qr.LoginActivity;
import com.example.presensi_qr.R;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvId = view.findViewById(R.id.tv_profile_id);
        TextView tvInitial = view.findViewById(R.id.tv_profile_initial);
        MaterialButton btnEdit = view.findViewById(R.id.btn_edit);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);

        // Menampilkan Nama, Role, dan Inisial dinamis dari SharedPreferences
        if (getContext() != null) {
            SharedPreferences pref = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String name = pref.getString("NAME", "User");
            String role = pref.getString("ROLE", "murid");

            tvName.setText(name);
            if (!name.isEmpty()) {
                tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }

            String label;
            if (role.equalsIgnoreCase("operator") || role.equalsIgnoreCase("admin")) {
                label = "Admin / Operator";
            } else if (role.equalsIgnoreCase("guru")) {
                label = "Guru Pengajar";
            } else {
                label = "Siswa / Murid";
            }
            tvId.setText(label);
        }

        // Fungsikan Tombol Edit Profil
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.presensi_qr.EditProfileActivity.class);
            startActivity(intent);
        });

        // Memperbaiki Tombol Logout
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                // FIX: Hapus semua data sesi sebelum logout
                SharedPreferences pref = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                pref.edit().clear().apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // Flag ini akan menghapus semua tumpukan Activity sebelumnya
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }
}
