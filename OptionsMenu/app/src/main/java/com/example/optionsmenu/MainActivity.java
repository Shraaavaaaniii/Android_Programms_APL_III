package com.example.optionsmenu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    LinearLayout currentLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentLayout = findViewById(R.id.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        int id = item.getItemId();
        if(id == R.id.red){
            currentLayout.setBackgroundColor(Color.RED);
            return true;
        } else if(id == R.id.green){
            currentLayout.setBackgroundColor(Color.GREEN);
            return true;
        } else if (id == R.id.gray) {
            currentLayout.setBackgroundColor(Color.GRAY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}