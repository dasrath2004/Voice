package com.example.voicetaskmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class TaskDetailsActivity extends AppCompatActivity {

    public static void open(Context c, String id) {
        Intent i = new Intent(c, TaskDetailsActivity.class);
        i.putExtra("id", id);
        c.startActivity(i);
    }

    TextView tvTitle, tvDesc, tvDeadline, tvPriority, tvStatus;
    Button btnComplete, btnDelete;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_task_details);

        tvTitle = findViewById(R.id.tvTitle);
        tvDesc = findViewById(R.id.tvDesc);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvPriority = findViewById(R.id.tvPriority);
        tvStatus = findViewById(R.id.tvStatus);
        btnComplete = findViewById(R.id.btnComplete);
        btnDelete = findViewById(R.id.btnDelete);

        String id = getIntent().getStringExtra("id");
        if (id == null) finish();

        FirebaseFirestore.getInstance().collection("tasks")
                .document(id)
                .get().addOnSuccessListener(ds -> {
                    Task t = ds.toObject(Task.class);
                    if (t == null) return;
                    tvTitle.setText(t.getTitle());
                    tvDesc.setText(t.getDescription());
                    tvDeadline.setText(new java.util.Date(t.getDeadline()).toString());
                    tvPriority.setText(t.getPriority());
                    tvStatus.setText(t.getStatus());

                    btnComplete.setOnClickListener(v ->
                            ds.getReference().update("status", "completed"));

                    btnDelete.setOnClickListener(v -> {
                        ScheduleReminder.cancel(getApplicationContext(), id);
                        ds.getReference().delete();
                        finish();
                    });
                });
    }
}
