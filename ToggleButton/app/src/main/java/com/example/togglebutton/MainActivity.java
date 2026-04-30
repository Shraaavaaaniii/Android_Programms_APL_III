package com.example.togglebutton;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    TextView txt;
    ToggleButton btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.textView);
        btn = findViewById(R.id.toggleButton);

    }

    public void onToggleClick(View view){
        if(btn.isChecked()){
            txt.setText("Toggle is on");
        }else{
            txt.setText("Toggle is OFF");
        }
    }
}