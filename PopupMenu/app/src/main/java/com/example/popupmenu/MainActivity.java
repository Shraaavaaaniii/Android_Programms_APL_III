package com.example.popupmenu;

import android.os.Bundle;import android.view.MenuItem;import android.view.View;import android.widget.Button;import android.widget.PopupMenu;import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, btn);

                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                    Toast.makeText(getApplicationContext(), "You clicked "+menuItem, Toast.LENGTH_SHORT).show();
                        return true;
                    }});
                popupMenu.show();
            }});
    }
}