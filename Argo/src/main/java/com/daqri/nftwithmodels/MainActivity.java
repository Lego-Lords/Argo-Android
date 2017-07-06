package com.daqri.nftwithmodels;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;

import org.artoolkit.ar.jpct.ArJpctActivity;
import org.artoolkit.ar.jpct.TrackableObject3d;
import org.json.JSONArray;
import org.json.JSONException;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ArJpctActivity {
    String your_IP_address = "192.168.0.110:8000"; /* Enter your IP address : port */
    String your_web_app = "value"; /* Replace this with your own web app name */
    private String baseUrl = "http://" + your_IP_address + "/" + your_web_app + "/";

    private static String url= "http://api.androidhive.info/contacts/"; // SERVER FETCH URL
    ArrayList<HashMap<String, String>> contactList = new ArrayList<>();
    final Context context = this;
    private List<TrackableObject3d> tckobjList;
    private TrackableObject3d tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");
    private ArrayList<Object3D> modelList = new ArrayList<>();
    private boolean firstTap = true;
    private World world;
    private Context mContext;
    private int legoModelStructureID; // ETO ID NG LEGO MODEL TLGA LIKE 0 SNOWCAT 1 PYRAMID
    private LegoModel lm;
    private TextView brickTypeTextView;
    private ImageView brickTypeImageView;
    private TextView brickStepTextView;
    //private int currentStep = -1; //to be CHANGED
    private int maxStep = 0;
   // private int maxStep = 13; TEST
    private int nextStep = -1; //to be CHANGED
    //private int nextStep = 4; //TEST
    private String modelName;
    private int currentBuiltModel = -1;
    private boolean loadModelDone = false;
    private boolean isNewStep = false; //SHOYLD BE FALSE
    private int previouslyRecievedStep = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIntent().setAction("Already created");
        setContentView(R.layout.activity_main);

        System.out.println("ANG ORAS AY SEX");

        brickTypeTextView = (TextView) findViewById(R.id.brick_type);
        brickTypeImageView = (ImageView) findViewById(R.id.brick_pic);
        brickStepTextView = (TextView) findViewById(R.id.step_tv);

        brickTypeTextView.setText("");
        lm = new LegoModel();
        mContext = this.getApplicationContext();
        this.legoModelStructureID = Integer.parseInt(getIntent().getStringExtra("LEGO_MODEL_ID"));
        this.your_IP_address = getIntent().getStringExtra("IP_ADDRESS");
        System.out.println("IP ADD " + your_IP_address);
        baseUrl = "http://" + your_IP_address + "/" + your_web_app + "/";


        //while (loadModelDone == false) {}
        //DIS IS NEEDED IN THE START, REMOVE ALL THEN ADD 1 by 1

        // POSSIBLE BUG IN THE FUTURE
        // populateTrackableObjects IS USED AS INITIALIZATION
    }

    @Override
    public void onPause() {
        super.onPause();
        modelUpdaterHandler.removeCallbacks(modelUpdaterRunnable);
        nextStepAnimationHandler.removeCallbacks(nextStepAnimationRunnable);
    }

    //THREADING ON BRICK UPDATE
    Handler initializeHandler = new Handler();

    Runnable initializeRunnable = new Runnable() {
        public void run() {
            removeAllModelsOnScreen();
            modelUpdaterHandler.postDelayed(modelUpdaterRunnable, 0);
            nextStepAnimationHandler.postDelayed(nextStepAnimationRunnable, 0);
        }
    };

    //THREADING ON BRICK UPDATE
    Handler modelUpdaterHandler = new Handler();

    Runnable modelUpdaterRunnable = new Runnable() {
        public void run() {
        //DO SOMESHIT HERE
            new Recheck().execute();
            //nextStep++;

            System.out.println("THREAD IS RUNNING!!");
            System.out.println("BRICK UPDATER: currPota " + currentBuiltModel);
            System.out.println("BRICK UPDATER: nextPota" + nextStep);
            System.out.println("BRICK UPDATER: maxPota" + maxStep);

            if(nextStep != maxStep) {
                if(isNewStep) {
                    System.out.println("PUMASOK SA ETITS");
                    updateModelOnScreen();
                    isNewStep = false;
                }
                modelUpdaterHandler.postDelayed(this, 2000);
            }
            else {
                finishBuilding();
                modelUpdaterHandler.removeCallbacks(modelUpdaterRunnable);
            }
    }
    };

    //THREADING ON BRICK UPDATE
    Handler nextStepAnimationHandler = new Handler();

    Runnable nextStepAnimationRunnable = new Runnable() {
        public void run() {
            Log.d("animNextStep","PASOK: " + nextStep);
            if(nextStep!=-1) {
                if(modelList.get(nextStep).getTranslation().z > 0) // moves down
                    modelList.get(nextStep).translate(new SimpleVector(0, 0, -20));
                else
                    modelList.get(nextStep).translate(new SimpleVector(0, 0, 60)); // return to elevated offset
                Log.d("animNextStep","Z VALUE: " + String.valueOf(modelList.get(nextStep).getOrigin().z));
                Log.d("animNextStep","TRANSLATION VALUE: " + modelList.get(nextStep).getTranslation().z);
            }
            nextStepAnimationHandler.postDelayed(nextStepAnimationRunnable, 500);
        }
    };

    private void finishBuilding() {
        System.out.println("WTFPASOK");
        brickTypeImageView.setImageResource(0);
        brickStepTextView.setText("");
        brickTypeTextView.setText("");

        // custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setTitle("Title...");

        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.text);
        text.setText("Congratulations! you have successfully built " + lm.getModelName(legoModelStructureID));
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

    private void updateModelOnScreen() {
        System.out.println("MODEL SIZE " + modelList.size() );
        if (nextStep  < modelList.size()) {
            System.out.println("SEX: " + nextStep);
            if(checkIfChildExist(tckobj)) {
                Log.d("checkchild", "pasok");
                removeModelUntilStep(nextStep-1);
            }
            completeModelUntilStep(nextStep);
            //tckobj.addChild(modelList.get(nextStep));
            updateBrickTypeTV(modelList.get(nextStep).getName(), nextStep);
        }
    }

    private boolean checkIfChildExist(TrackableObject3d tckobj) {
        for(int i = 0; i <= nextStep; i++)
        {
            Log.d("checkchild", String.valueOf(nextStep) );
            if(tckobj.hasChild(modelList.get(i)))
                return true;
        }
        return false;
    }

    private void removeModelUntilStep(int nextStep) {
        for(int i = 0; i <= nextStep; i++)
        {
            tckobj.removeChild(modelList.get(i));
        }
    }

    //COMPLETE MODEL GIVEN STEP (UNTESTED IN REAL SERVER)
    private void completeModelUntilStep(int nextStep) {
        for(int i = 0; i <= nextStep; i++)
        {
            tckobj.addChild(modelList.get(i));
        }
    }

    private void updateBrickTypeTV(String name, int step) {
        String[] result = name.split("_");
        String brick = null;
        //add color
        step+=1;


        switch(result[0])
        {
            case "2456": brick = "2x6";
                switch (result[1])
                {
                    case "2": brickTypeImageView.setImageResource(R.drawable.step_2456_2); break;
                }
                break;
            case "3001": brick = "2x4";
                switch (result[1])
                {
                    case "1": brickTypeImageView.setImageResource(R.drawable.step_3001_1); break;
                    case "4": brickTypeImageView.setImageResource(R.drawable.step_3001_4); break;
                    case "14": brickTypeImageView.setImageResource(R.drawable.step_3001_14); break;
                    case "15": brickTypeImageView.setImageResource(R.drawable.step_3001_15); break;
                }
                break;
            case "3002": brick = "2x3";
                switch (result[1])
                {
                    case "1": brickTypeImageView.setImageResource(R.drawable.step_3002_1); break;
                    case "25": brickTypeImageView.setImageResource(R.drawable.step_3002_25); break;
                }
                break;
            case "3003": brick = "2x2";
                switch (result[1])
                {
                    case "0": brickTypeImageView.setImageResource(R.drawable.step_3003_0); break;
                    case "1": brickTypeImageView.setImageResource(R.drawable.step_3003_1); break;
                    case "2": brickTypeImageView.setImageResource(R.drawable.step_3003_2); break;
                    case "15": brickTypeImageView.setImageResource(R.drawable.step_3003_15); break;
                }
                break;
            case "3004": brick = "1x2";
            switch (result[1])
            {
                case "27": brickTypeImageView.setImageResource(R.drawable.step_3004_27); break;
            }
            break;
            case "3005": brick = "1x1"; break;
        }

        brickStepTextView.setText("STEP:"+ step + "/" + modelList.size());
        brickTypeTextView.setText(brick);

    }

    private void removeAllModelsOnScreen() {
        System.out.println("RIP " + modelList.size());
        for (int i = 0; i < modelList.size(); i++) {
            tckobj.removeChild(modelList.get(i));
        }
        System.out.println("REMOVED");
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
        this.tckobjList = list;
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
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_2456_green)), 64, 64));
        TextureManager.getInstance().addTexture("2456_2", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3002_blue)), 64, 64));
        TextureManager.getInstance().addTexture("3002_1", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3002_orange)), 64, 64));
        TextureManager.getInstance().addTexture("3002_25", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_blue)), 64, 64));
        TextureManager.getInstance().addTexture("3003_1", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_green)), 64, 64));
        TextureManager.getInstance().addTexture("3003_2", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3004_lime)), 64, 64));
        TextureManager.getInstance().addTexture("3004_27", texture);


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
                    float scaleFactor = 20;
                    Object3D brickModel = loadModel(modelID + ".3ds", scaleFactor); //DIS SCALE
                    brickModel.setTexture(modelID + "_" + color);
                    brickModel.setName(modelID + "_" + color);
                    brickModel.setRotationMatrix(rotMatrix);
                    brickModel.setOrigin(new SimpleVector(yPos*(scaleFactor/10)+100, xPos*(scaleFactor/10)-100, zPos*(scaleFactor/10)-0));
//                    brickModel.setOrigin(new SimpleVector(yPos*(scaleFactor/10), xPos*(scaleFactor/10), zPos*(scaleFactor/10)-0));

                    tckobj.addChild(brickModel);
                    modelList.add(brickModel);
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        tckobj.setOrigin(new SimpleVector(0,0,80));
//        tckobj.setOrigin(new SimpleVector(0,0,0));
//        tckobj.setOrigin(new SimpleVector(0,0,200));
//        tckobj.setOrigin(new SimpleVector(0,100,0));
        this.tckobjList.add(tckobj);

        initializeHandler.postDelayed(initializeRunnable, 0);
        System.out.println("KAKATAPOS LANG NG LOAD");
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
    private class Recheck extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
/*            //HARD CODE
            maxStep = modelList.size();
            nextStep = 2;
            //HARD CODE*/

            // IF NEXT STEP HAS CHANGED
            if(nextStep != previouslyRecievedStep)
            {
                isNewStep = true;
                previouslyRecievedStep = nextStep;
            }

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
            String jsonStr = sh.makeServiceCall(baseUrl);

            Log.e(TAG, "Response from url: " + jsonStr);
            jsonStr = "{ 'data': [{'currentStep': '6', 'maxStep': '7', 'modelName': 'Duck'}] }"; //dummy data in case no server

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray data = jsonObj.getJSONArray("data");

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject d = data.getJSONObject(i);

                        nextStep = d.getInt("currentStep");
                        maxStep = d.getInt("maxStep");
                        modelName = d.getString("modelName");
                        Log.d("fromServer", String.valueOf(nextStep));
                        Log.d("fromServer", String.valueOf(maxStep));


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
            }
//            else {
//                Log.e(TAG, "Couldn't get json from server.");
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplicationContext(),
//                                "Couldn't get json from server. Check LogCat for possible errors!",
//                                Toast.LENGTH_LONG)
//                                .show();
//                    }
//                });
//
//            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Log.d(TAG, "BRICK SERVER: nextStep " + nextStep);
            Log.d(TAG, "BRICK SERVER: maxStep " + maxStep);
            Log.d(TAG, "BRICK SERVER: modelName " + modelName);

        }

    }
}