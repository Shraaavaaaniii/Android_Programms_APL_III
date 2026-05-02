package com.example.registrationform;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    EditText etName, etEmail;
    RadioGroup radioGroup;
    CheckBox cbReading, cbSports;
    ToggleButton toggle;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        radioGroup = findViewById(R.id.radioGroup);
        cbReading = findViewById(R.id.cbReading);
        cbSports = findViewById(R.id.cbSports);
        toggle = findViewById(R.id.toggle);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {

            String name = etName.getText().toString();
            String email = etEmail.getText().toString();

            // Gender
            int selectedId = radioGroup.getCheckedRadioButtonId();
            RadioButton rb = findViewById(selectedId);
            String gender = (rb != null) ? rb.getText().toString() : "Not Selected";

            // Hobbies
            String hobbies = "";
            if (cbReading.isChecked()) hobbies += "Reading ";
            if (cbSports.isChecked()) hobbies += "Sports ";

            // Toggle
            String subscription = toggle.isChecked() ? "Subscribed" : "Not Subscribed";

            String result = "Name: " + name +
                    "\nEmail: " + email +
                    "\nGender: " + gender +
                    "\nHobbies: " + hobbies +
                    "\nStatus: " + subscription;

            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        });
    }

}