package com.example.presensi_qr;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.ArrayAdapter;
import com.example.presensi_qr.models.TeachingSchedule;
import java.util.ArrayList;
import java.util.List;

public class ApplyDispensationActivity extends AppCompatActivity {

    private MaterialCardView cardSelectPdf;
    private TextView tvPdfTitle, tvPdfSubtitle;
    private ImageView ivPdfIcon;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView etSubject;
    private TextInputEditText etReason;
    private MaterialButton btnUpload;
    
    private Uri selectedPdfUri;
    private String token;
    
    private List<TeachingSchedule> schedules = new ArrayList<>();
    private int selectedTeacherId = -1;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    if (selectedPdfUri != null) {
                        displaySelectedPdf();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_dispensation);

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

        cardSelectPdf = findViewById(R.id.card_select_pdf);
        tvPdfTitle = findViewById(R.id.tv_pdf_title);
        tvPdfSubtitle = findViewById(R.id.tv_pdf_subtitle);
        ivPdfIcon = findViewById(R.id.iv_pdf_icon);
        etSubject = findViewById(R.id.et_dispen_subject);
        etReason = findViewById(R.id.et_dispen_reason);
        btnUpload = findViewById(R.id.btn_upload_dispen);

        cardSelectPdf.setOnClickListener(v -> selectPdfFile());
        btnUpload.setOnClickListener(v -> uploadDispensation());

        fetchSchedules();
    }

    private void fetchSchedules() {
        ApiClient.getService().getMyClassSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
            @Override
            public void onResponse(Call<List<TeachingSchedule>> call, Response<List<TeachingSchedule>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    schedules = response.body();
                    List<String> scheduleStrings = new ArrayList<>();
                    for (TeachingSchedule ts : schedules) {
                        scheduleStrings.add(ts.toString());
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ApplyDispensationActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            scheduleStrings
                    );
                    etSubject.setAdapter(adapter);
                    
                    etSubject.setOnItemClickListener((parent, view, position, id) -> {
                        selectedTeacherId = schedules.get(position).getUserId();
                    });
                } else {
                    Toast.makeText(ApplyDispensationActivity.this, "Gagal memuat jadwal kelas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TeachingSchedule>> call, Throwable t) {
                Toast.makeText(ApplyDispensationActivity.this, "Koneksi gagal saat memuat jadwal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Pilih File PDF"));
    }

    private void displaySelectedPdf() {
        String fileName = "Surat_Dispensasi.pdf";
        try {
            android.database.Cursor cursor = getContentResolver().query(selectedPdfUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvPdfTitle.setText(fileName);
        tvPdfSubtitle.setText("File siap untuk diunggah");
        ivPdfIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        ivPdfIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
    }

    private File getFileFromUri(Uri uri) {
        try {
            File tempFile = new File(getCacheDir(), "temp_dispen.pdf");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadDispensation() {
        String subject = etSubject.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (selectedPdfUri == null) {
            Toast.makeText(this, "Silakan pilih berkas PDF terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subject.isEmpty() || reason.isEmpty() || selectedTeacherId == -1) {
            Toast.makeText(this, "Harap pilih pelajaran dan lengkapi alasan dispensasi!", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = getFileFromUri(selectedPdfUri);
        if (file == null) {
            Toast.makeText(this, "Gagal memproses berkas PDF!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        btnUpload.setText("SEDANG MENGIRIM...");

        // Combine subject and reason
        String combinedReason = "Pelajaran: " + subject + "\nAlasan: " + reason;
        
        // Use today's date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String date = sdf.format(new java.util.Date());

        RequestBody teacherIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(selectedTeacherId));
        RequestBody reasonBody = RequestBody.create(MediaType.parse("text/plain"), combinedReason);
        RequestBody dateBody = RequestBody.create(MediaType.parse("text/plain"), date);
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/pdf"), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        ApiClient.getService().submitDispensation(token, teacherIdBody, reasonBody, dateBody, filePart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                btnUpload.setEnabled(true);
                btnUpload.setText("KIRIM PENGAJUAN DISPENSASI");

                if (response.isSuccessful()) {
                    Toast.makeText(ApplyDispensationActivity.this, "Pengajuan dispensasi berhasil dikirim!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String error = "Gagal mengirim pengajuan";
                    try {
                        if (response.errorBody() != null) {
                            error += " (" + response.errorBody().string() + ")";
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ApplyDispensationActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnUpload.setEnabled(true);
                btnUpload.setText("KIRIM PENGAJUAN DISPENSASI");
                Toast.makeText(ApplyDispensationActivity.this, "Koneksi bermasalah: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
