package com.daqri.nftwithmodels;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    String your_IP_address = "192.168.0.103:8000";  //Enter your IP address : port

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
        viewPager.setAdapter(adapter);

        build_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //SEND MODEL ID TO SERVER HERE
                currentModelSelectedID = viewPager.getCurrentItem();
                PostTask task = new PostTask();
                task.execute();
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.putExtra("LEGO_MODEL_ID", String.valueOf(viewPager.getCurrentItem()));
                startActivity(intent);
            }
        });

    }

    private String buildJSON(int s) {
        return "{'modelName': " + "'" + lm.getModelFileName(s) + "'}";
    }

    public class PostTask extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                String getResponse = post(lm.getModelName(currentModelSelectedID)); //"http://httpbin.org/post"
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
            String baseUrl = "http://" + your_IP_address + "/" + your_web_app + "/";

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        }
    }
    public String bowlingJson(String player1, String player2) {
        return "{'winCondition':'HIGH_SCORE',"
                + "'name':'Bowling',"
                + "'round':4,"
                + "'lastSaved':1367702411696,"
                + "'dateStarted':1367702378785,"
                + "'players':["
                + "{'name':'" + player1 + "','history':[10,8,6,7,8],'color':-13388315,'total':39},"
                + "{'name':'" + player2 + "','history':[6,10,5,10,10],'color':-48060,'total':41}"
                + "]}";
    }
}
