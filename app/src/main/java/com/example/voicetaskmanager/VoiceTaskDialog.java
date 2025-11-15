package com.example.voicetaskmanager;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Calendar;

public class VoiceTaskDialog extends DialogFragment {

    private TextView tvResult;
    private ImageView ivMic;
    private Button btnConfirm;
    private String recognizedText = "";

    private ActivityResultLauncher<Intent> launcher;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_voice);

        tvResult = dialog.findViewById(R.id.tvResult);
        ivMic = dialog.findViewById(R.id.ivMic);
        btnConfirm = dialog.findViewById(R.id.btnConfirm);

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() != null) {
                ArrayList<String> res = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (res != null && !res.isEmpty()) {
                    recognizedText = res.get(0);
                    tvResult.setText(recognizedText);
                }
            }
        });

        ivMic.setOnClickListener(v -> openVoice());
        btnConfirm.setOnClickListener(v -> {
            parseAndOpenSheet();
            dismiss();
        });

        return dialog;
    }

    private void openVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task e.g. 'Submit report tomorrow 6pm high'");
        try { launcher.launch(i); }
        catch (Exception ex) { tvResult.setText("Speech not available"); }
    }

    private void parseAndOpenSheet() {
        String text = recognizedText != null ? recognizedText : "";
        String priority = "Medium";
        if (text.toLowerCase().contains("high")) priority = "High";
        else if (text.toLowerCase().contains("low")) priority = "Low";

        long deadline = System.currentTimeMillis() + (60 * 60 * 1000);
        if (text.toLowerCase().contains("tomorrow")) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.set(Calendar.MINUTE, 0);
            deadline = c.getTimeInMillis();
        }

        AddTaskBottomSheet sheet = new AddTaskBottomSheet();
        sheet.setDataFromVoice(text, deadline, priority);
        sheet.show(getParentFragmentManager(), "addtask");
    }
}
