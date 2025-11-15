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

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CompletedTasksFragment extends Fragment {

    private RecyclerView rv;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;

    public CompletedTasksFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_completed_tasks, container, false);

        rv = v.findViewById(R.id.rvCompleted);

        adapter = new TaskAdapter(requireContext(), tasks, null);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        listenCompleted();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (registration != null) registration.remove();
    }

    private void listenCompleted() {
        registration = db.collection("tasks")
                .whereEqualTo("status", "completed")
                .orderBy("deadline")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    tasks.clear();

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Task t = dc.getDocument().toObject(Task.class);
                        t.setId(dc.getDocument().getId());
                        tasks.add(t);
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
