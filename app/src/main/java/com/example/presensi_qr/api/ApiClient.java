package com.example.presensi_qr.api;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.presensi_qr.BuildConfig;

public class ApiClient {
    // UBAH BASE_URL DISINI:
    // 1. Production API: "https://api.wahyuindrasetiawan.my.id/api/"
    // 2. Local Emulator (Artisan Serve): "http://10.0.2.2:8000/api/"
    // 3. Local Emulator (Laragon/Apache): "http://10.0.2.2/apias/public/api/"
    // 4. Physical Device: gunakan IP komputer Anda, contoh: "http://192.168.1.100:8000/api/"
    private static final String BASE_URL = "https://api.wahyuindrasetiawan.my.id/api/";
    private static Retrofit retrofit = null;

    public static ApiService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            if (BuildConfig.DEBUG) {
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            } else {
                logging.setLevel(HttpLoggingInterceptor.Level.NONE);
            }
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        // WAJIB: Tambahkan Header Accept: application/json
                        // Agar Laravel tidak melakukan redirect ke halaman login web (penyebab error sessions)
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Accept", "application/json")
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    })
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
