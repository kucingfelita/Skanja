package com.example.presensi_qr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextInputEditText etReadFullname, etReadUsername;
    private MaterialButton btnSavePassword;
    private TextView tvInitial, tvName, tvRole;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        token = pref.getString("TOKEN", "");
        String name = pref.getString("NAME", "User");
        String role = pref.getString("ROLE", "murid");
        String nisNama = pref.getString("NIS_NAMA", "-");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etReadFullname = findViewById(R.id.et_read_fullname);
        etReadUsername = findViewById(R.id.et_read_username);
        btnSavePassword = findViewById(R.id.btn_save_password);

        tvInitial = findViewById(R.id.tv_edit_initial);
        tvName = findViewById(R.id.tv_edit_name);
        tvRole = findViewById(R.id.tv_edit_role);

        // Display user data
        tvName.setText(name);
        if (!name.isEmpty()) {
            tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        etReadFullname.setText(name);
        etReadUsername.setText(nisNama);

        String label;
        if (role.equalsIgnoreCase("operator") || role.equalsIgnoreCase("admin")) {
            label = "Admin / Operator";
        } else if (role.equalsIgnoreCase("guru")) {
            label = "Guru Pengajar";
        } else {
            label = "Siswa / Murid";
        }
        tvRole.setText(label);

        btnSavePassword.setOnClickListener(v -> saveNewPassword());
    }

    private void saveNewPassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua kolom password!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password baru minimal harus 6 karakter!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Konfirmasi password baru tidak cocok!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePassword.setEnabled(false);
        ApiClient.getService().changePassword(token, currentPassword, newPassword).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                btnSavePassword.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Password berhasil diubah!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = " (" + response.errorBody().string() + ")";
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(EditProfileActivity.this, "Gagal mengubah password" + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnSavePassword.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
