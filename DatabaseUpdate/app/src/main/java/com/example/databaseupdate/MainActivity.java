package com.example.databaseupdate;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;
    EditText etName, etMarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etMarks = findViewById(R.id.etMarks);

        Button btnInsert = findViewById(R.id.btnInsert);
        Button btnUpdate = findViewById(R.id.btnUpdate);

        db = openOrCreateDatabase("StudentDB", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Student(name TEXT, marks INTEGER)");

        // INSERT
        btnInsert.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String marksStr = etMarks.getText().toString();

            if (name.isEmpty() || marksStr.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int marks = Integer.parseInt(marksStr);
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("marks", marks);

            long result = db.insert("Student", null, values);

            Toast.makeText(this,
                    result != -1 ? "Inserted" : "Error",
                    Toast.LENGTH_SHORT).show();
        });

        // UPDATE
        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String marksStr = etMarks.getText().toString();

            if (name.isEmpty() || marksStr.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int marks = Integer.parseInt(marksStr);

            ContentValues values = new ContentValues();
            values.put("marks", marks);

            int result = db.update("Student", values,
                    "name=?", new String[]{name});

            if (result > 0)
                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Record Not Found", Toast.LENGTH_SHORT).show();
        });    }
}