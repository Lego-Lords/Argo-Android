package com.daqri.nftwithmodels;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SelectionScreenActivity extends AppCompatActivity {

    //String your_IP_address = "192.168.0.110:8000";  //Enter your IP address : port
    private String your_IP_address = "";

    ViewPager viewPager;
    CustomSwipeAdapter adapter;
    Button build_button;
    int currentModelSelectedID;

    private LegoModel lm;
    //private String url = "http://www.roundsapp.com/post"; //SERVER POST URL
    private PostTask task;
    MediaType JSON;
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_screen);
        JSON = MediaType.parse("application/json; charset=utf-8");

        lm = new LegoModel();
        //String json = example.bowlingJson("Jesse", "Jake");

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        adapter =  new CustomSwipeAdapter(this);
        build_button = (Button) findViewById(R.id.build_button);

        //HINGI IP_ADDRESS
        buildIPDialog();

        viewPager.setAdapter(adapter);

        build_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //SEND MODEL ID TO SERVER HERE
                currentModelSelectedID = viewPager.getCurrentItem();
                PostTask task = new PostTask();
                task.execute();
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.putExtra("LEGO_MODEL_ID", String.valueOf(viewPager.getCurrentItem()));
                intent.putExtra("IP_ADDRESS", your_IP_address);
                startActivity(intent);
            }
        });

    }

    private void buildIPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input IP of Server");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                your_IP_address = input.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public class PostTask extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                String getResponse = post(lm.getModelName(currentModelSelectedID)); //"http://httpbin.org/post"
                /*(TESTING) Selected Model Posted*/
                return getResponse;
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String getResponse) {
            System.out.println(getResponse);
        }

        private String post(String modelName) throws IOException {
            String your_web_app = "model-id?id="+modelName+"";  //Replace this with your own web app name
            String baseUrl = "http://" + your_IP_address + "/" + your_web_app;

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        }
    }
}
