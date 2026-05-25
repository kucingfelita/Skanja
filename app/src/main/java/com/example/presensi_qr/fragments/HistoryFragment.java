package com.example.presensi_qr.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.Presence;
import com.example.presensi_qr.models.TeachingSchedule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {
    private RecyclerView rvHistory;
    private AutoCompleteTextView etSemesterInput;
    private TextView tvEmptyHistory;
    private List<Presence> presenceList;
    private String selectedSemester = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        etSemesterInput = view.findViewById(R.id.et_history_semester);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);

        setupSemesterDropdown();
        return view;
    }

    private void setupSemesterDropdown() {
        if (getActivity() == null) return;
        SharedPreferences pref = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");
        String role = pref.getString("ROLE", "");

        if (token.isEmpty()) return;

        ApiClient.getService().getSemesters(token).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (getContext() == null || !isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<String> semesters = response.body();
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            semesters
                    );
                    etSemesterInput.setAdapter(adapter);

                    // Set default semester (first one)
                    selectedSemester = semesters.get(0);
                    etSemesterInput.setText(selectedSemester, false);

                    loadHistoryForSemester(token, role, selectedSemester);
                } else {
                    // Fallback if empty
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                if (getContext() == null || !isAdded()) return;
                tvEmptyHistory.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Gagal memuat daftar semester", Toast.LENGTH_SHORT).show();
            }
        });

        etSemesterInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedSemester = (String) parent.getItemAtPosition(position);
            loadHistoryForSemester(token, role, selectedSemester);
        });
    }

    private void loadHistoryForSemester(String token, String role, String semester) {
        boolean isTeacher = "guru".equalsIgnoreCase(role);

        if (isTeacher) {
            ApiClient.getService().getSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
                @Override
                public void onResponse(Call<List<TeachingSchedule>> call, Response<List<TeachingSchedule>> response) {
                    if (getContext() == null || !isAdded()) return;
                    HashSet<String> teacherClasses = new HashSet<>();
                    if (response.isSuccessful() && response.body() != null) {
                        for (TeachingSchedule s : response.body()) {
                            if (semester.equalsIgnoreCase(s.getSemester())) {
                                String cls = s.getClassName();
                                if (cls.contains(" (")) {
                                    cls = cls.split(" \\(")[0];
                                }
                                teacherClasses.add(cls.toLowerCase());
                            }
                        }
                    }
                    fetchHistoryListForTeacher(token, teacherClasses, semester);
                }

                @Override
                public void onFailure(Call<List<TeachingSchedule>> call, Throwable t) {
                    if (getContext() == null || !isAdded()) return;
                    fetchHistoryListForTeacher(token, new HashSet<>(), semester);
                }
            });
        } else {
            android.content.SharedPreferences pref = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            final String userClass = pref.getString("KELAS", "");
            final String userYear = pref.getString("TAHUN_MASUK", "");

            ApiClient.getService().getMyClassSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
                @Override
                public void onResponse(Call<List<TeachingSchedule>> call, Response<List<TeachingSchedule>> response) {
                    if (getContext() == null || !isAdded()) return;
                    List<TeachingSchedule> myClassSchedules = response.body();
                    
                    // fetch all schedules to be robust and merge them
                    ApiClient.getService().getSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
                        @Override
                        public void onResponse(Call<List<TeachingSchedule>> call2, Response<List<TeachingSchedule>> response2) {
                            if (getContext() == null || !isAdded()) return;
                            List<TeachingSchedule> allSchedules = response2.body();
                            
                            List<TeachingSchedule> mergedSchedules = new ArrayList<>();
                            java.util.HashSet<Integer> addedIds = new java.util.HashSet<>();
                            
                            if (myClassSchedules != null) {
                                for (TeachingSchedule s : myClassSchedules) {
                                    if (addedIds.add(s.getId())) {
                                        mergedSchedules.add(s);
                                    }
                                }
                            }
                            
                            if (allSchedules != null) {
                                for (TeachingSchedule s : allSchedules) {
                                    if (addedIds.contains(s.getId())) continue;
                                    
                                    if (matchesStudentClass(s.getClassName(), userClass, userYear)) {
                                        if (addedIds.add(s.getId())) {
                                            mergedSchedules.add(s);
                                        }
                                    }
                                }
                            }
                            
                            List<TeachingSchedule> filteredSchedules = new ArrayList<>();
                            for (TeachingSchedule s : mergedSchedules) {
                                if (semester.equalsIgnoreCase(s.getSemester())) {
                                    filteredSchedules.add(s);
                                }
                            }
                            fetchHistoryListForStudent(token, filteredSchedules, semester);
                        }

                        @Override
                        public void onFailure(Call<List<TeachingSchedule>> call2, Throwable t2) {
                            if (getContext() == null || !isAdded()) return;
                            List<TeachingSchedule> filteredSchedules = new ArrayList<>();
                            if (myClassSchedules != null) {
                                for (TeachingSchedule s : myClassSchedules) {
                                    if (semester.equalsIgnoreCase(s.getSemester())) {
                                        filteredSchedules.add(s);
                                    }
                                }
                            }
                            fetchHistoryListForStudent(token, filteredSchedules, semester);
                        }
                    });
                }

                @Override
                public void onFailure(Call<List<TeachingSchedule>> call, Throwable t) {
                    if (getContext() == null || !isAdded()) return;
                    // Fallback to fetch all schedules directly
                    ApiClient.getService().getSchedules(token).enqueue(new Callback<List<TeachingSchedule>>() {
                        @Override
                        public void onResponse(Call<List<TeachingSchedule>> call2, Response<List<TeachingSchedule>> response2) {
                            if (getContext() == null || !isAdded()) return;
                            List<TeachingSchedule> allSchedules = response2.body();
                            List<TeachingSchedule> filteredSchedules = new ArrayList<>();
                            if (allSchedules != null) {
                                for (TeachingSchedule s : allSchedules) {
                                    if (matchesStudentClass(s.getClassName(), userClass, userYear)) {
                                        if (semester.equalsIgnoreCase(s.getSemester())) {
                                            filteredSchedules.add(s);
                                        }
                                    }
                                }
                            }
                            fetchHistoryListForStudent(token, filteredSchedules, semester);
                        }

                        @Override
                        public void onFailure(Call<List<TeachingSchedule>> call2, Throwable t2) {
                            if (getContext() == null || !isAdded()) return;
                            fetchHistoryListForStudent(token, new ArrayList<>(), semester);
                        }
                    });
                }
            });
        }
    }

    private void fetchHistoryListForTeacher(String token, HashSet<String> teacherClasses, String semester) {
        ApiClient.getService().getHistory(token, "all").enqueue(new Callback<List<Presence>>() {
            @Override
            public void onResponse(Call<List<Presence>> call, Response<List<Presence>> response) {
                if (getContext() == null || !isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    presenceList = response.body();
                    List<Presence> filteredList = new ArrayList<>();
                    for (Presence p : presenceList) {
                        if (p.getKelas() != null) {
                            String pClass = p.getKelas().toLowerCase();
                            if (pClass.contains(" (")) {
                                pClass = pClass.split(" \\(")[0];
                            }
                            if (teacherClasses.contains(pClass)) {
                                if (isDateInSemester(p.getCreatedAt(), semester)) {
                                    filteredList.add(p);
                                }
                            }
                        }
                    }
                    presenceList = filteredList;

                    com.example.presensi_qr.adapters.HistoryCardAdapter cardAdapter = 
                            new com.example.presensi_qr.adapters.HistoryCardAdapter(presenceList, true);
                    rvHistory.setAdapter(cardAdapter);
                    
                    if (presenceList.isEmpty()) {
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyHistory.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Presence>> call, Throwable t) {
                if (getContext() == null || !isAdded()) return;
                Toast.makeText(getContext(), "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchHistoryListForStudent(String token, List<TeachingSchedule> schedules, String semester) {
        ApiClient.getService().getHistory(token, "all").enqueue(new Callback<List<Presence>>() {
            @Override
            public void onResponse(Call<List<Presence>> call, Response<List<Presence>> response) {
                if (getContext() == null || !isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    presenceList = response.body();
                    List<Presence> filteredList = new ArrayList<>();
                    for (Presence p : presenceList) {
                        if (isDateInSemester(p.getCreatedAt(), semester)) {
                            filteredList.add(p);
                        }
                    }
                    presenceList = filteredList;

                    com.example.presensi_qr.adapters.HistoryCardAdapter cardAdapter = 
                            new com.example.presensi_qr.adapters.HistoryCardAdapter(presenceList, schedules);
                    rvHistory.setAdapter(cardAdapter);
                    
                    if (presenceList.isEmpty() && schedules.isEmpty()) {
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyHistory.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Presence>> call, Throwable t) {
                if (getContext() == null || !isAdded()) return;
                Toast.makeText(getContext(), "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean isDateInSemester(String createdAt, String semesterName) {
        if (createdAt == null || semesterName == null || semesterName.isEmpty()) return true;
        try {
            int year = 0;
            int month = 0;
            String datePart = createdAt.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                if (parts[0].length() == 4) { // yyyy-MM-dd
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                } else { // dd-MM-yyyy
                    year = Integer.parseInt(parts[2]);
                    month = Integer.parseInt(parts[1]);
                }
            } else {
                return true;
            }

            String cleanName = semesterName.toLowerCase();
            int year1 = 0, year2 = 0;
            Pattern p = Pattern.compile("(\\d{4})");
            Matcher m = p.matcher(cleanName);
            if (m.find()) {
                year1 = Integer.parseInt(m.group(1));
            }
            if (m.find()) {
                year2 = Integer.parseInt(m.group(1));
            }

            if (year1 == 0) return true;
            if (year2 == 0) year2 = year1 + 1;

            boolean isGanjil = cleanName.contains("ganjil") || cleanName.contains("odd");
            boolean isGenap = cleanName.contains("genap") || cleanName.contains("even");

            if (isGanjil) {
                return (year == year1 && month >= 7 && month <= 12);
            } else if (isGenap) {
                return (year == year2 && month >= 1 && month <= 6);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean matchesStudentClass(String scheduleClass, String userClass, String userYear) {
        if (scheduleClass == null || userClass == null) return false;
        
        String cleanSched = scheduleClass.trim().toLowerCase();
        String cleanUser = userClass.trim().toLowerCase();
        String cleanYear = userYear != null ? userYear.trim().toLowerCase() : "";

        // Strip year from userClass if userClass is already "X-1 (2026)"
        String pureUser = cleanUser;
        String userYearVal = cleanYear;
        if (cleanUser.contains(" (")) {
            String[] parts = cleanUser.split(" \\(");
            pureUser = parts[0].trim();
            if (userYearVal.isEmpty()) {
                userYearVal = parts[1].replace(")", "").trim();
            }
        }

        // Strip year from scheduleClass if it exists
        String pureSched = cleanSched;
        String schedYear = "";
        if (cleanSched.contains(" (")) {
            String[] parts = cleanSched.split(" \\(");
            pureSched = parts[0].trim();
            schedYear = parts[1].replace(")", "").trim();
        }

        // They match if the pure class names are equal and either year matches or is empty
        if (pureSched.equals(pureUser)) {
            if (schedYear.isEmpty() || userYearVal.isEmpty() || schedYear.equals(userYearVal)) {
                return true;
            }
        }
        return false;
    }
}
