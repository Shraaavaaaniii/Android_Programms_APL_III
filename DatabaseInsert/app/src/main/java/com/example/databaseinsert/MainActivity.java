package com.example.databaseinsert;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b1 = findViewById(R.id.btnInsert);

        db = openOrCreateDatabase("StudentDB", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Student(name TEXT, marks INTEGER)");

        b1.setOnClickListener(v -> {

            EditText ename = findViewById(R.id.etName);
            EditText emarks = findViewById(R.id.etMarks);

            String name = ename.getText().toString();
            String marksStr = emarks.getText().toString();

            if (name.isEmpty() || marksStr.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int marks = Integer.parseInt(marksStr);

            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("marks", marks);

            long result = db.insert("Student", null, values);

            if (result != -1)
                Toast.makeText(this, "Inserted Successfully", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        });
    }
}