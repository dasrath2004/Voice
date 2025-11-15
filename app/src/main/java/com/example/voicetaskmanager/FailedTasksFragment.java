package com.example.voicetaskmanager;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FailedTasksFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;

    public FailedTasksFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_failed_tasks, container, false);

        rv = v.findViewById(R.id.rvFailedTasks);
        tvEmpty = v.findViewById(R.id.tvEmptyFailed);

        adapter = new TaskAdapter(requireContext(), tasks, null);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        listenFailed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (registration != null) registration.remove();
    }

    private void listenFailed() {
        registration = db.collection("tasks")
                .whereEqualTo("status", "failed")
                .orderBy("deadline")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    tasks.clear();

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Task t = dc.getDocument().toObject(Task.class);
                        t.setId(dc.getDocument().getId());
                        tasks.add(t);
                    }

                    // Empty view handling
                    if (tasks.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
