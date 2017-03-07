package com.daqri.nftwithmodels;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SelectionScreenActivity extends AppCompatActivity {
    ViewPager viewPager;
    CustomSwipeAdapter adapter;
    Button build_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_screen);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        adapter =  new CustomSwipeAdapter(this);
        build_button = (Button) findViewById(R.id.build_button);
        viewPager.setAdapter(adapter);

        build_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //SEND MODEL ID TO SERVER HERE
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.putExtra("LEGO_MODEL_ID", String.valueOf(viewPager.getCurrentItem()));
                startActivity(intent);
            }
        });

    }
}
