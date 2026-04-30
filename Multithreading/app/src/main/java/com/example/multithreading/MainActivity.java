package com.example.multithreading;

import android.os.Bundle;import android.view.View;import android.widget.Button;import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    Button btn;
    ImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.button);
        img = findViewById(R.id.imageView);

        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        img.post(new Runnable(){
                            public void run(){
                                try{
                                    Thread.sleep(5000);
                                }catch(InterruptedException e){
                                    e.printStackTrace();
                                }

                                img.setImageResource(R.drawable.ic_launcher_background);
                            }
                        });
                    }
                }).start();
            }
        });
    }
}