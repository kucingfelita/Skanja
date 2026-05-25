package com.example.presensi_qr;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.presensi_qr.api.ApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanActivity extends AppCompatActivity {
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    
    private double currentLat = 0, currentLng = 0;
    private static final double TARGET_LAT = -6.200000; 
    private static final double TARGET_LNG = 106.816666;
    private static final boolean BYPASS_LOCATION_FOR_TEST = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    barcodeScannerView.pause();
                    Log.d("SCAN_DEBUG", "QR Scanned: " + result.getText());
                    processScanResult(result.getText());
                }
            }
            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {}
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermissions();
    }

    private void processScanResult(String rawData) {
        try {
            JSONObject data = new JSONObject(rawData);
            String subject = data.getString("subject");
            String teacher = data.getString("teacher");
            
            // Check QR Code expiration (5 minutes = 300000 ms)
            if (data.has("created_at")) {
                long createdAt = data.getLong("created_at");
                long currentTime = System.currentTimeMillis();
                long diff = currentTime - createdAt;
                if (diff > 300000) { // 5 menit
                    Toast.makeText(this, "QR Code sudah kadaluarsa!", Toast.LENGTH_LONG).show();
                    barcodeScannerView.resume();
                    return;
                }
            } else {
                // Jika QR tidak memiliki timestamp, anggap kadaluarsa (atau izinkan jika ingin backward-compatibility)
                Toast.makeText(this, "QR Code tidak valid atau kadaluarsa!", Toast.LENGTH_LONG).show();
                barcodeScannerView.resume();
                return;
            }

            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String token = pref.getString("TOKEN", "");
            String userClass = pref.getString("KELAS", "");
            String userTahun = pref.getString("TAHUN_MASUK", "");
            String userClassWithTahun = userClass + " (" + userTahun + ")";

            final String targetClass = data.has("class_name") ? data.getString("class_name") : "";
            final String semester = data.has("semester") ? data.getString("semester") : "";
            final String sessionNum = data.has("session_number") ? String.valueOf(data.get("session_number")) : "";

            if (!targetClass.isEmpty()) {
                boolean isAuthorized = targetClass.equalsIgnoreCase(userClass) || targetClass.equalsIgnoreCase(userClassWithTahun);
                if (!isAuthorized) {
                    new AlertDialog.Builder(this)
                        .setTitle("Akses Kelas Ditolak ⚠️")
                        .setMessage("Maaf, Anda bukan anggota kelas " + targetClass + "!")
                        .setPositiveButton("OK", (dialog, which) -> {
                            barcodeScannerView.resume();
                        })
                        .setCancelable(false)
                        .show();
                    return;
                }
            }

            Log.d("API_DEBUG", "Token: " + token);
            Log.d("API_DEBUG", "Subject: " + subject + ", Teacher: " + teacher);

            if (token.isEmpty()) {
                Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kirim ke server Laravel
            ApiClient.getService().submitPresence(token, subject, teacher, currentLat, currentLng)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        // Guard: jangan akses UI kalau Activity sudah destroyed
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            saveLocalAttendance(subject, teacher);
                            showSuccessDialog(subject, teacher, targetClass, semester, sessionNum);
                        } else {
                            String errorBody = "";
                            try { if (response.errorBody() != null) errorBody = response.errorBody().string(); } 
                            catch (IOException e) { e.printStackTrace(); }
                            
                            Log.e("API_ERROR", "Code: " + response.code() + " | Body: " + errorBody);
                            Toast.makeText(ScanActivity.this, "Gagal simpan ke server (Error " + response.code() + ")", Toast.LENGTH_LONG).show();
                            barcodeScannerView.resume();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        Log.e("API_ERROR", "Failure: " + t.getMessage());
                        Toast.makeText(ScanActivity.this, "Koneksi Bermasalah", Toast.LENGTH_SHORT).show();
                        barcodeScannerView.resume();
                    }
                });

        } catch (Exception e) {
            Log.e("SCAN_ERROR", "JSON Parsing Error: " + e.getMessage());
            Toast.makeText(this, "QR Code tidak valid!", Toast.LENGTH_SHORT).show();
            barcodeScannerView.resume();
        }
    }

    private void saveLocalAttendance(String subject, String teacher) {
        SharedPreferences userPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String nisNama = userPref.getString("NIS_NAMA", "anonymous");

        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        SharedPreferences.Editor editor = getSharedPreferences("AttendancePrefs", MODE_PRIVATE).edit();
        editor.putBoolean("is_absent_" + nisNama + "_" + today, true);
        editor.putString("absent_subject_" + nisNama + "_" + today, subject);
        editor.putString("absent_teacher_" + nisNama + "_" + today, teacher);
        editor.apply();
    }

    private void showSuccessDialog(String subject, String teacher, String className, String semester, String sessionNum) {
        StringBuilder msg = new StringBuilder();
        msg.append("Mata Pelajaran: ").append(subject).append("\n");
        msg.append("Guru: ").append(teacher).append("\n");
        if (className != null && !className.isEmpty()) {
            msg.append("Kelas: ").append(className).append("\n");
        }
        if (semester != null && !semester.isEmpty()) {
            msg.append("Semester: ").append(semester).append("\n");
        }
        if (sessionNum != null && !sessionNum.isEmpty()) {
            msg.append("Sesi ke: ").append(sessionNum).append("\n");
        }
        msg.append("\nData telah tersimpan di server.");

        new AlertDialog.Builder(this)
            .setTitle("Absensi Berhasil!")
            .setMessage(msg.toString())
            .setPositiveButton("OK", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
        } else {
            validateLocation();
        }
    }

    private void validateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        float[] results = new float[1];
                        Location.distanceBetween(currentLat, currentLng, TARGET_LAT, TARGET_LNG, results);
                        if (!BYPASS_LOCATION_FOR_TEST && results[0] > 100) {
                            Toast.makeText(this, "Anda terlalu jauh dari sekolah!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
        }
    }

    @Override protected void onResume() { super.onResume(); capture.onResume(); barcodeScannerView.resume(); }
    @Override protected void onPause() { super.onPause(); capture.onPause(); barcodeScannerView.pause(); }
    @Override protected void onDestroy() { super.onDestroy(); capture.onDestroy(); }

    // FIX KRITIS: Callback wajib ada agar GPS diambil SETELAH user tap "Allow"
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean cameraGranted = false;
            boolean locationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                }
            }
            if (cameraGranted && locationGranted) {
                validateLocation();
            } else {
                Toast.makeText(this, "Izin kamera dan lokasi diperlukan untuk absensi!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
