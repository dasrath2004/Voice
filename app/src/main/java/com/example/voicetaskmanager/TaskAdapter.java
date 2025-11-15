package com.example.voicetaskmanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.Holder> {

    Context ctx;
    List<Task> list;
    Listener listener;

    public interface Listener {
        void onTaskClicked(Task task);
        void onTaskCompleted(Task task);
    }

    public TaskAdapter(Context ctx, List<Task> list, Listener l) {
        this.ctx = ctx;
        this.list = list;
        this.listener = l;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_task, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int i) {
        Task t = list.get(i);
        h.title.setText(t.getTitle());
        h.deadline.setText("Deadline: " + new java.util.Date(t.getDeadline()).toString());

        h.priority.setText(t.getPriority());
        switch (t.getPriority()) {
            case "High": h.priority.setTextColor(Color.RED); break;
            case "Medium": h.priority.setTextColor(Color.parseColor("#FFA500")); break;
            default: h.priority.setTextColor(Color.GREEN); break;
        }

        h.itemView.setOnClickListener(v -> listener.onTaskClicked(t));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, deadline, priority;
        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            deadline = v.findViewById(R.id.tvDeadline);
            priority = v.findViewById(R.id.tvPriority);
        }
    }
}
