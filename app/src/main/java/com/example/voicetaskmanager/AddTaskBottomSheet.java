package com.example.voicetaskmanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText etTitle, etDesc;
    private Button btnDeadline, btnSave;
    private Spinner spinnerPriority;
    private long selectedDeadline = 0;
    private AddTaskListener listener;

    public interface AddTaskListener { void onTaskAdded(); }
    public void setAddTaskListener(AddTaskListener l) { this.listener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_add_task, container, false);

        etTitle = v.findViewById(R.id.etTitle);
        etDesc = v.findViewById(R.id.etDesc);
        btnDeadline = v.findViewById(R.id.btnDeadline);
        spinnerPriority = v.findViewById(R.id.spinnerPriority);
        btnSave = v.findViewById(R.id.btnSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"High","Medium","Low"}
        );
        spinnerPriority.setAdapter(adapter);

        btnDeadline.setOnClickListener(view -> pickDate());
        btnSave.setOnClickListener(view -> saveTask());
        return v;
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(requireContext(),
                (DatePicker view, int y, int m, int d) -> pickTime(y, m, d),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void pickTime(int y, int m, int d) {
        Calendar now = Calendar.getInstance();
        TimePickerDialog tp = new TimePickerDialog(requireContext(),
                (TimePicker view, int hour, int minute) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(y, m, d, hour, minute, 0);
                    selectedDeadline = cal.getTimeInMillis();
                    btnDeadline.setText("Deadline: " + DateFormat.format("dd MMM yyyy HH:mm", cal));
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
        tp.show();
    }

    private void saveTask() {
        final String title = etTitle.getText().toString().trim();
        final String desc = etDesc.getText().toString().trim();
        final String priority = spinnerPriority.getSelectedItem().toString();

        if (title.isEmpty()) { etTitle.setError("Required"); return; }
        if (selectedDeadline == 0) {
            Toast.makeText(getContext(),"Select deadline",Toast.LENGTH_SHORT).show();
            return;
        }

        long reminder = selectedDeadline - (30 * 60 * 1000);
        if (reminder < System.currentTimeMillis()) reminder = System.currentTimeMillis() + 5000;
        final long finalReminder = reminder;

        // add userId for production-safe rules
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", desc);
        map.put("priority", priority);
        map.put("deadline", selectedDeadline);
        map.put("createdAt", System.currentTimeMillis());
        map.put("status", "ongoing");
        map.put("reminderTime", finalReminder);
        map.put("userId", uid);




        Context appCtx = getContext() != null ? getContext().getApplicationContext() : null;

        FirebaseFirestore.getInstance().collection("tasks").add(map)
                .addOnSuccessListener(docRef -> {
                    // schedule reminder only if we have context
                    if (appCtx != null) {
                        ScheduleReminder.setAlarm(appCtx, finalReminder, docRef.getId(), title);
                    }
                    if (listener != null) listener.onTaskAdded();
                })
                .addOnFailureListener(e -> {
                    // use getContext() safely
                    Context c = getContext();
                    if (c != null) {
                        Toast.makeText(c, "Failed to save task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        dismiss();
    }

    public void setDataFromVoice(String title, long deadline, String priority) {
        if (title != null) etTitle.setText(title);
        if (deadline > 0) {
            selectedDeadline = deadline;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(deadline);
            btnDeadline.setText("Deadline: " + android.text.format.DateFormat.format("dd MMM yyyy HH:mm", cal));
        }
        if (priority != null) {
            int idx = priority.equalsIgnoreCase("High") ? 0 : priority.equalsIgnoreCase("Medium") ? 1 : 2;
            spinnerPriority.setSelection(idx);
        }
    }
}
