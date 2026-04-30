package com.example.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


public class MainActivity extends AppCompatActivity {

    String channel_id = "01";
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.notify);

        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                // Create channel for Android 8+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    NotificationChannel nc = new NotificationChannel(channel_id, "Channel_01", NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(nc);
                }

                NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(MainActivity.this, channel_id)
                        .setSmallIcon(R.drawable.ic_notification) // create this icon
                        .setContentTitle("Notification")
                        .setContentText("This is my first notification")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

                nm.notify(1, nBuilder.build());
            }
        });
    }
}