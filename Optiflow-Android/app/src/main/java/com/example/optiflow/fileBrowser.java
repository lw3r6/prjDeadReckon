package com.example.optiflow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class fileBrowser extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        File directory = new File(String.valueOf(getFilesDir()));
        File[] files = directory.listFiles();
        String[] filesNames = new String[files.length];

        Log.d("Files", "Size: " + files.length);
        for (int i = 0; i < files.length; i++) {
            Log.d("Files", "FileName:" + files[i].getName());
            filesNames[i] = files[i].getName();
        }


        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.file_list_item, filesNames);

        ListView listView = findViewById(R.id.filesListView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("Clicked item pos. : " + i, filesNames[i]);
                Intent intent = new Intent(getApplicationContext(), savedGraphing.class);
                intent.putExtra("fileName", filesNames[i]);
                startActivity(intent);
            }
        });

    }

}
