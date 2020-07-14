package com.example.optiflow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NavPage extends AppCompatActivity {

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        setContentView(R.layout.activity_nav_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        try {
            File f = new File(getFilesDir(),"tester" + ".csv");
            if (f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
                f.createNewFile();
            }
            FileWriter csvWriter = new FileWriter(f);
            csvWriter.append("test");
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button goLiveBut = findViewById(R.id.goToLiveButton);
        goLiveBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), LiveGraphing.class);
                startActivity(i);
            }
        });

        Button goSavedBut = findViewById(R.id.goToSavedDataButton);
        goSavedBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), fileBrowser.class);
                startActivity(i);
            }
        });
    }



}
