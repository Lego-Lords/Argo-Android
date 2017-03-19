package com.daqri.nftwithmodels;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.threed.jpct.Config;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;

import org.artoolkit.ar.jpct.ArJpctActivity;
import org.artoolkit.ar.jpct.TrackableLight;
import org.artoolkit.ar.jpct.TrackableObject3d;
import org.json.JSONArray;
import org.json.JSONException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ArJpctActivity {
    private static String url= "http://api.androidhive.info/contacts/"; // SERVER FETCH URL
    ArrayList<HashMap<String, String>> contactList = new ArrayList<>();
    final Context context = this;
    private List<TrackableObject3d> list;
    private TrackableObject3d tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");
    private ArrayList<Object3D> modelList = new ArrayList<>();
    private boolean firstTap = true;
    private int currentModel = 0;
    private World world;
    private Context mContext;
    private int legoModelStructureID;
    private LegoModel lm;
    private TextView brickTypeTextView;
    private ImageView brickTypeImageView;
    private TextView brickStepTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIntent().setAction("Already created");
        setContentView(R.layout.activity_main);

        brickTypeTextView = (TextView) findViewById(R.id.brick_type);
        brickTypeImageView = (ImageView) findViewById(R.id.brick_pic);
        brickStepTextView = (TextView) findViewById(R.id.step_tv);

        brickTypeTextView.setText("");
        lm = new LegoModel();
        mContext = this.getApplicationContext();
        this.legoModelStructureID = Integer.parseInt(getIntent().getStringExtra("LEGO_MODEL_ID"));
        mainLayout = supplyFrameLayout();
        // When the screen is tapped, inform the renderer and vibrate the phone
        mainLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (firstTap) {
                    for (int i = 0; i < modelList.size(); i++) {
                        tckobj.removeChild(modelList.get(i));
                    }
                    firstTap = !firstTap;
                } else if (currentModel < modelList.size()) {
                    tckobj.addChild(modelList.get(currentModel));
                    updateBrickTypeTV(modelList.get(currentModel).getName(), currentModel);
                    currentModel += 1;
                } else {
                    currentModel = 0;
                    firstTap = !firstTap;
                    brickTypeImageView.setImageResource(0);
                    brickStepTextView.setText("");
                    brickTypeTextView.setText("");
                    // custom dialog
                    final Dialog dialog = new Dialog(context);
                    dialog.setContentView(R.layout.custom_dialog);
                    dialog.setTitle("Title...");

                    // set the custom dialog components - text, image and button
                    TextView text = (TextView) dialog.findViewById(R.id.text);
                    text.setText("Congratulations! you have successfully build " + lm.getModelName(legoModelStructureID));
                    ImageView image = (ImageView) dialog.findViewById(R.id.image);
                    image.setImageResource(lm.getImageResource(legoModelStructureID));

                    Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                    // if button is clicked, close the custom dialog
                    dialogButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            finish();
                        }
                    });

                    dialog.show();
                }

                //call server to get data on tap
                new GetContacts().execute();
                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vib.vibrate(100);

            }

        });
    }

    private void updateBrickTypeTV(String name, int step) {
        String[] result = name.split("_");
        String brick = null;
        //add color
        step+=1;


        switch(result[0])
        {
            case "3001": brick = "2x4";
                switch (result[1])
                {
                    case "1": brickTypeImageView.setImageResource(R.drawable.step_3001_1); break;
                    case "4": brickTypeImageView.setImageResource(R.drawable.step_3001_4); break;
                    case "14": brickTypeImageView.setImageResource(R.drawable.step_3001_14); break;
                    case "15": brickTypeImageView.setImageResource(R.drawable.step_3001_15); break;
                }
                break;
            case "3003": brick = "2x2";
                switch (result[1])
                {
                    case "0": brickTypeImageView.setImageResource(R.drawable.step_3003_0); break;
                    case "15": brickTypeImageView.setImageResource(R.drawable.step_3003_15); break;
                }
                break;
            case "3005": brick = "1x1"; break;
        }

        brickStepTextView.setText("STEP:"+ step + "/" + modelList.size());
        brickTypeTextView.setText(brick);

    }

    @Override
    public void onStop() {
        super.onStop();
        finish();
    }

    /**
     * Use the FrameLayout in this Activity's UI.
     */
    @Override
    protected FrameLayout supplyFrameLayout() {
        return (FrameLayout)this.findViewById(R.id.mainLayout);
    }

    public void configureWorld(World world) {
        this.world = world;
        Config.farPlane = 2000;
        world.setAmbientLight(200, 200, 200); //255 all original * too bright for our eyes
    }

    protected void populateTrackableObjects(List<TrackableObject3d> list) {
        this.list = list;
        //Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.one_three_green)), 64, 64));
        //TextureManager.getInstance().addTexture("one_three_green", texture);
        Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001_white)), 64, 64));
        TextureManager.getInstance().addTexture("3001_15", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_white)), 64, 64));
        TextureManager.getInstance().addTexture("3003_15", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001_blue)), 64, 64));
        TextureManager.getInstance().addTexture("3001_1", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001_red)), 64, 64));
        TextureManager.getInstance().addTexture("3001_4", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001_yellow)), 64, 64));
        TextureManager.getInstance().addTexture("3001_14", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_black)), 64, 64));
        TextureManager.getInstance().addTexture("3003_0", texture);
/*        try {
            legoModel1 = loadModel("one_three.3ds", 15);
            legoModel1.setTexture("one_three_green");
   *//*         legoModel1.strip();
            legoModel1.build();*//*
            tckobj.addChild(legoModel1);
            modelList.add(legoModel1);

            legoModel2 = loadModel("duplo4.3ds", 30);
            legoModel2.setOrigin(new SimpleVector(150, -150, 30));
            tckobj.addChild(legoModel2);
            modelList.add(legoModel2);


          *//*  // Put a plane to see where it cuts
            Object3D object3D = Primitives.getPlane(2, 200);
            // Planes are rotated 180 degrees, so we need to flip them
            object3D.rotateX((float) Math.PI);
            object3D.setOrigin(new SimpleVector(125, 125, 0));
            //object3D.setTexture("moon_ground");
            tckobj.addChild(object3D);*//*
        } catch (IOException e) {
           e.printStackTrace();
        }*/

        AssetManager assetManager = getResources().getAssets();
        // To load text file
        InputStream input;
        String filename = lm.getModelFileName(legoModelStructureID);
        try {
            input = assetManager.open(filename+".ldr");

            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();

            // byte buffer into a string
            String text = new String(buffer);
            String[] lines = text.split("\\n");

            for(String line : lines){
                // is not a comment aka new brick
                if(line.charAt(0) == '1') {
                    // pattern to get all numbers
                    Pattern p = Pattern.compile("(-*\\.*\\d+\\.*\\d*)");
                    Matcher m = p.matcher(line);
                    int counter = 0;

                    // get brick data
                    String modelID = "";
                    float xPos=0;
                    float yPos=0;
                    float zPos=0;
                    int color = -1;
                    //data for rotation
                    Matrix rotMatrix = new Matrix();
                    Matrix initMatrix = new Matrix();

                    while (m.find()) {
                        counter++;
                        switch(counter){
                            case 2: color = Integer.valueOf(m.group(1));break;
                            case 3: xPos = Float.valueOf(m.group(1));break;
                            case 4: zPos = Float.valueOf(m.group(1))*-1;break; // times -1 kasi opposite yung pag show sa phone
                            case 5: yPos = Float.valueOf(m.group(1));break;
                            case 6: initMatrix.set(0,0, Float.valueOf(m.group(1)));break;
                            case 7: initMatrix.set(0,1, Float.valueOf(m.group(1)));break;
                            case 8: initMatrix.set(0,2, Float.valueOf(m.group(1)));break;
                            case 9: initMatrix.set(1,0, Float.valueOf(m.group(1)));break;
                            case 10:initMatrix.set(1,1, Float.valueOf(m.group(1)));break;
                            case 11:initMatrix.set(1,2, Float.valueOf(m.group(1)));break;
                            case 12:initMatrix.set(2,0, Float.valueOf(m.group(1)));break;
                            case 13:initMatrix.set(2,1, Float.valueOf(m.group(1)));break;
                            case 14:initMatrix.set(2,2, Float.valueOf(m.group(1)));break;
                            case 15: modelID = m.group(1);modelID=modelID.replace(".","");break; // replace "." kasi may bug regex haha
                        }
                    }
                    // CHECK IF IDENTITY MATRIX
                    if(!initMatrix.isIdentity()){
                        //override kasi weirdo ang mundo
                        rotMatrix.set(0,0, initMatrix.get(1,0));
                        rotMatrix.set(0,1, initMatrix.get(1,1));
                        rotMatrix.set(0,2, initMatrix.get(1,2));
                        rotMatrix.set(1,0, initMatrix.get(2,0));
                        rotMatrix.set(1,1, initMatrix.get(2,1));
                        rotMatrix.set(1,2, initMatrix.get(2,2));
                        rotMatrix.set(2,0, initMatrix.get(0,0));
                        rotMatrix.set(2,1, initMatrix.get(0,1));
                        rotMatrix.set(2,2, initMatrix.get(0,2));
                    }
                    else {
                        rotMatrix = initMatrix;
                    }
                    // build brick model
                    Object3D brickModel = loadModel(modelID + ".3ds", 10);
                    brickModel.setTexture(modelID + "_" + color);
                    brickModel.setName(modelID + "_" + color);
                    brickModel.setRotationMatrix(rotMatrix);
                    brickModel.setOrigin(new SimpleVector(yPos+200, xPos-200, zPos));

                    tckobj.addChild(brickModel);
                    modelList.add(brickModel);
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.list.add(tckobj);
    }

    private Object3D loadModel(String filename, float scale) throws IOException {
        Logger.log("FUCK ME: " + filename);
        InputStream stream = mContext.getAssets().open(filename);
        Object3D[] model = Loader.load3DS(stream, scale);
        Object3D o3d = new Object3D(0);
        Object3D temp = null;
        for (int i = 0; i < model.length; i++) {
            temp = model[i];
            temp.setCenter(SimpleVector.ORIGIN);
            //temp.rotateX((float) (-.5 * Math.PI));
            //temp.rotateMesh();
            temp.setRotationMatrix(new Matrix());
            o3d = Object3D.mergeObjects(o3d, temp);
            o3d.build();
        }
        return o3d;
    }

    /**
     * Async task class to get json by making HTTP call
     */
    private class GetContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
/*            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();*/

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray contacts = jsonObj.getJSONArray("contacts");

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        String id = c.getString("id");
                        String name = c.getString("name");
                        String email = c.getString("email");
                        String address = c.getString("address");
                        String gender = c.getString("gender");

                        // Phone node is JSON Object
                        JSONObject phone = c.getJSONObject("phone");
                        String mobile = phone.getString("mobile");
                        String home = phone.getString("home");
                        String office = phone.getString("office");

                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        contact.put("id", id);
                        contact.put("name", name);
                        contact.put("email", email);
                        contact.put("mobile", mobile);

                        // adding contact to contact list
                        contactList.add(contact);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
/*            // Dismiss the progress dialog
*//*            if (pDialog.isShowing())
                pDialog.dismiss();*//*
            *//**
             * Updating parsed JSON data into ListView
             * *//*
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, contactList,
                    R.layout.list_item, new String[]{"name", "email",
                    "mobile"}, new int[]{R.id.name,
                    R.id.email, R.id.mobile});

            lv.setAdapter(adapter);*/

            Log.d(TAG, "eto" + contactList.get(0).get("name"));
        }

    }
}