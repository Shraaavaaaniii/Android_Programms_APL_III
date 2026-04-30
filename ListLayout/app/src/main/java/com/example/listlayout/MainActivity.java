package com.example.listlayout;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    String[] players = {"Rohit Sharma", "Virat Kohli", "Thala", "Bumrah", "R. Ashwin"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.listview , players);
        ListView listView = (ListView)findViewById(R.id.player_list);
        listView.setAdapter(adapter);
    }
}