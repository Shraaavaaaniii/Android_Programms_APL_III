package com.example.contextmenu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    TextView txt;
    RelativeLayout r;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.clickTxt);
        r = findViewById(R.id.main);

        registerForContextMenu(txt);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Choose a Color:");

        menu.add(0, v.getId(), 0, "Yellow");
        menu.add(0, v.getId(), 0, "Green");
        menu.add(0, v.getId(), 0, "Gray");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        if(item.getTitle() == "Yellow"){
            r.setBackgroundColor(Color.YELLOW);
        }else if(item.getTitle() == "Green"){
            r.setBackgroundColor(Color.GREEN);
        }else{
            r.setBackgroundColor(Color.GRAY);
        }
        return true;
    }
}