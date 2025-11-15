package com.example.voicetaskmanager;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements TaskAdapter.Listener, AddTaskBottomSheet.AddTaskListener {

    private RecyclerView rv;
    private TaskAdapter adapter;
    private List<Task> list = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        rv = v.findViewById(R.id.rvTasks);
        FloatingActionButton fabAdd = v.findViewById(R.id.fabAddTask);
        FloatingActionButton fabVoice = v.findViewById(R.id.fabVoice);

        adapter = new TaskAdapter(requireContext(), list, this);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        fabAdd.setOnClickListener(view -> {
            AddTaskBottomSheet b = new AddTaskBottomSheet();
            b.setAddTaskListener(this);
            b.show(getParentFragmentManager(), "addtask");
        });

        fabVoice.setOnClickListener(view -> {
            new VoiceTaskDialog().show(getParentFragmentManager(), "voice");
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        listenTasks();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (reg != null) reg.remove();
    }

    private int priorityOrder(String p) {
        if (p == null) return 3;
        switch (p.toLowerCase()) {
            case "high": return 1;
            case "medium": return 2;
            default: return 3;
        }
    }

    private void listenTasks() {
        reg = db.collection("tasks")
                .whereEqualTo("status", "ongoing")
                .addSnapshotListener((value, error) -> {
                    if (value == null) {
                        Context c = getContext();
                        if (c != null) Toast.makeText(c, "Failed to load tasks", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    list.clear();
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Task t = dc.getDocument().toObject(Task.class);
                        if (t == null) continue;
                        t.setId(dc.getDocument().getId());
                        list.add(t);
                    }
                    Collections.sort(list, (a, b) -> {
                        int pa = priorityOrder(a.getPriority());
                        int pb = priorityOrder(b.getPriority());
                        if (pa != pb) return Integer.compare(pa, pb);
                        return Long.compare(a.getDeadline(), b.getDeadline());
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onTaskAdded() {
        Context c = getContext();
        if (c != null) Toast.makeText(c, "Task added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskClicked(Task task) {
        TaskDetailsActivity.open(requireContext(), task.getId());
    }

    @Override
    public void onTaskCompleted(Task task) {
        db.collection("tasks").document(task.getId()).update("status", "completed");
    }
}
