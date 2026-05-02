package com.example.databasedisplay;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;
    EditText etName, etMarks;
    TextView txtData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etMarks = findViewById(R.id.etMarks);
        txtData = findViewById(R.id.txtData);

        Button btnInsert = findViewById(R.id.btnInsert);
        Button btnView = findViewById(R.id.btnView);

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

        // DISPLAY
        btnView.setOnClickListener(v -> {
            Cursor c = db.rawQuery("SELECT * FROM Student", null);

            String data = "";

            if (c.moveToFirst()) {
                do {
                    data += "Name: " + c.getString(0) + "\n";
                    data += "Marks: " + c.getInt(1) + "\n\n";
                } while (c.moveToNext());
            } else {
                data = "No Data Found";
            }

            txtData.setText(data);
            c.close();
        });
    }
}