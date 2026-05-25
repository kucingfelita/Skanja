package com.example.presensi_qr.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.presensi_qr.R;
import com.example.presensi_qr.models.Presence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryCardAdapter extends RecyclerView.Adapter<HistoryCardAdapter.ViewHolder> {

    public static class GroupedHistory {
        public String key; // Subject name for student, Class name for teacher
        public String title;
        public String subtitle;
        public int hadirCount = 0;
        public int izinCount = 0;
        public int alpaCount = 0;
        public List<Presence> list = new ArrayList<>();
    }

    private final List<GroupedHistory> groupedList;
    private final boolean isTeacher;

    public HistoryCardAdapter(List<Presence> rawList, boolean isTeacher) {
        this.isTeacher = isTeacher;
        this.groupedList = groupData(rawList, isTeacher);
    }

    public HistoryCardAdapter(List<Presence> rawList, List<com.example.presensi_qr.models.TeachingSchedule> schedules) {
        this.isTeacher = false;
        this.groupedList = groupStudentData(rawList, schedules);
    }

    private List<GroupedHistory> groupStudentData(List<Presence> rawList, List<com.example.presensi_qr.models.TeachingSchedule> schedules) {
        Map<String, GroupedHistory> groups = new HashMap<>();

        // First, pre-populate all subjects from the class schedules
        if (schedules != null) {
            for (com.example.presensi_qr.models.TeachingSchedule s : schedules) {
                String key = s.getSubject();
                if (key == null || key.isEmpty()) continue;
                
                if (!groups.containsKey(key)) {
                    GroupedHistory group = new GroupedHistory();
                    group.key = key;
                    group.title = key;
                    group.subtitle = "Guru: " + (s.getTeacher() != null ? s.getTeacher().getName() : "-");
                    groups.put(key, group);
                }
            }
        }

        // Now, count history matching those subjects or insert any historical subject that is not in the schedules
        if (rawList != null) {
            for (Presence p : rawList) {
                String key = p.getSubjectName();
                if (key == null || key.isEmpty()) {
                    key = "Lainnya";
                }

                GroupedHistory group = groups.get(key);
                if (group == null) {
                    // This history subject is not in current schedules, let's still add it so we don't lose data
                    group = new GroupedHistory();
                    group.key = key;
                    group.title = key;
                    group.subtitle = "Guru: " + p.getTeacherName();
                    groups.put(key, group);
                }

                group.list.add(p);

                String status = p.getStatus().toLowerCase();
                if (status.equalsIgnoreCase("hadir")) {
                    group.hadirCount++;
                } else if (status.equalsIgnoreCase("izin") || status.equalsIgnoreCase("sakit") || status.equalsIgnoreCase("pending")) {
                    group.izinCount++;
                } else {
                    group.alpaCount++;
                }
            }
        }

        return new ArrayList<>(groups.values());
    }

    private List<GroupedHistory> groupData(List<Presence> rawList, boolean isTeacher) {
        Map<String, GroupedHistory> groups = new HashMap<>();

        if (rawList != null) {
            for (Presence p : rawList) {
                // Group key: Subject for student, Class for teacher
                String key = isTeacher ? p.getKelas() : p.getSubjectName();
                if (key == null || key.isEmpty()) {
                    key = "Lainnya";
                }

                GroupedHistory group = groups.get(key);
                if (group == null) {
                    group = new GroupedHistory();
                    group.key = key;
                    group.title = key;
                    group.subtitle = isTeacher ? p.getSubjectName() : "Guru: " + p.getTeacherName();
                    groups.put(key, group);
                }

                group.list.add(p);

                String status = p.getStatus().toLowerCase();
                if (status.equalsIgnoreCase("hadir")) {
                    group.hadirCount++;
                } else if (status.equalsIgnoreCase("izin") || status.equalsIgnoreCase("sakit") || status.equalsIgnoreCase("pending")) {
                    group.izinCount++;
                } else {
                    group.alpaCount++;
                }
            }
        }
        return new ArrayList<>(groups.values());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupedHistory item = groupedList.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);
        holder.tvHadir.setText(String.valueOf(item.hadirCount));
        holder.tvIzin.setText(String.valueOf(item.izinCount));
        holder.tvAlpa.setText(String.valueOf(item.alpaCount));

        holder.itemView.setOnClickListener(v -> showDetailDialog(v.getContext(), item));
    }

    private static java.util.Date parseDateOnly(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return new java.util.Date(0);
        String datePart = createdAt.split(" ")[0];
        try {
            if (datePart.contains("-")) {
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    int year, month, day;
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        year = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]) - 1;
                        day = Integer.parseInt(parts[2]);
                    } else { // dd-MM-yyyy
                        day = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]) - 1;
                        year = Integer.parseInt(parts[2]);
                    }
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(year, month, day, 0, 0, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.Date(0);
    }

    private static String getStandardizedDate(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return "";
        String datePart = createdAt.split(" ")[0];
        try {
            if (datePart.contains("-")) {
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    int year, month, day;
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        year = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]);
                        day = Integer.parseInt(parts[2]);
                    } else { // dd-MM-yyyy
                        day = Integer.parseInt(parts[0]);
                        month = Integer.parseInt(parts[1]);
                        year = Integer.parseInt(parts[2]);
                    }
                    return String.format(java.util.Locale.US, "%02d-%02d-%04d", day, month, year);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datePart;
    }

    private void showDetailDialog(Context context, GroupedHistory item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Detail Presensi: " + item.title);

        ListView listView = new ListView(context);
        List<String> details = new ArrayList<>();

        java.util.List<Presence> sortedList = new ArrayList<>(item.list);
        java.util.Collections.sort(sortedList, (p1, p2) -> {
            java.util.Date d1 = parseDateOnly(p1.getCreatedAt());
            java.util.Date d2 = parseDateOnly(p2.getCreatedAt());
            return d1.compareTo(d2);
        });

        java.util.Map<String, String> dateToSessionMap = new java.util.HashMap<>();
        int sessionCounter = 1;
        for (Presence p : sortedList) {
            String stdDate = getStandardizedDate(p.getCreatedAt());
            if (!dateToSessionMap.containsKey(stdDate)) {
                dateToSessionMap.put(stdDate, "Sesi " + sessionCounter);
                sessionCounter++;
            }
        }

        String lastDate = "";
        for (Presence p : sortedList) {
            String stdDate = getStandardizedDate(p.getCreatedAt());
            if (!stdDate.equalsIgnoreCase(lastDate)) {
                String sessionLabel = dateToSessionMap.get(stdDate);
                details.add("── " + sessionLabel + " (" + stdDate + ") ──");
                lastDate = stdDate;
            }
            String timePart = "";
            if (p.getCreatedAt() != null && p.getCreatedAt().contains(" ")) {
                timePart = p.getCreatedAt().split(" ")[1];
                if (timePart.length() > 5) {
                    timePart = timePart.substring(0, 5); // HH:mm
                }
            }
            String timeStr = timePart.isEmpty() ? "" : " " + timePart;
            if (isTeacher) {
                details.add(p.getStudentName() + timeStr + " - " + p.getStatus().toUpperCase());
            } else {
                details.add(stdDate + timeStr + " - " + p.getStatus().toUpperCase());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, details) {
            @Override
            public boolean isEnabled(int position) {
                String itemText = getItem(position);
                if (itemText != null && itemText.startsWith("── ") && itemText.endsWith(" ──")) {
                    return false;
                }
                return super.isEnabled(position);
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                String itemText = getItem(position);
                if (itemText != null) {
                    if (itemText.startsWith("── ") && itemText.endsWith(" ──")) {
                        view.setTextColor(Color.parseColor("#4B5563"));
                        view.setTypeface(null, android.graphics.Typeface.BOLD);
                        view.setTextSize(14);
                        view.setPadding(32, 24, 32, 8);
                    } else {
                        view.setTypeface(null, android.graphics.Typeface.NORMAL);
                        view.setTextSize(16);
                        view.setPadding(48, 12, 48, 12);
                        if (itemText.contains("HADIR")) {
                            view.setTextColor(Color.parseColor("#10B981"));
                        } else if (itemText.contains("IZIN") || itemText.contains("SAKIT") || itemText.contains("PENDING")) {
                            view.setTextColor(Color.parseColor("#3B82F6"));
                        } else {
                            view.setTextColor(Color.parseColor("#EF4444"));
                        }
                    }
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
        builder.setView(listView);
        builder.setPositiveButton("Tutup", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return groupedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvHadir, tvIzin, tvAlpa;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvSubtitle = itemView.findViewById(R.id.tv_card_subtitle);
            tvHadir = itemView.findViewById(R.id.tv_stat_hadir);
            tvIzin = itemView.findViewById(R.id.tv_stat_izin);
            tvAlpa = itemView.findViewById(R.id.tv_stat_alpa);
        }
    }
}
