package com.daqri.nftwithmodels;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends ArJpctActivity {
    String your_IP_address = ""; /* Enter your IP address : port */
    String your_web_app = "value"; /* Replace this with your own web app name */
    private String baseUrl = "http://" + your_IP_address + "/" + your_web_app + "/";

    private static String url= "http://api.androidhive.info/contacts/"; // SERVER FETCH URL
    ArrayList<HashMap<String, String>> contactList = new ArrayList<>();
    final Context context = this;
    private List<TrackableObject3d> tckobjList;
    private TrackableObject3d tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");
    private ArrayList<Object3D> modelList = new ArrayList<>();
    private ArrayList<Object3D> listForRotation = new ArrayList<>();
    private boolean firstTap = true;
    private World world;
    private Context mContext;
    private int legoModelStructureID; // ETO ID NG LEGO MODEL TLGA LIKE 0 SNOWCAT 1 PYRAMID
    private LegoModel lm;
    private TextView brickTypeTextView;
    private ImageView brickTypeImageView;
    private TextView brickStepTextView;
    private View wrongBrickView;
    private int currentStep = -1; //to be CHANGED
    private int maxStep = 0;
    //private int maxStep = 6; //TEST
    private int nextStep = -1; //to be CHANGED
    //private int nextStep = 1; //TEST
    private boolean hasError = false;
    private String modelName;
    private int currentBuiltModel = -1;
    private boolean loadModelDone = false;
    private boolean isNewStep = false; //SHOYLD BE FALSE
    private int previouslyRecievedStep = -1;
    private String errorValue_string = "0";
    private PostTaskOverride task;
    OkHttpClient client = new OkHttpClient();
    private View view;
    private Boolean isFirstBrick = true;

    private int rotValue = 0;

    private Object3D parentObject = Object3D.createDummyObj();
    private boolean isRotate = true;
    private View topLevelLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIntent().setAction("Already created");
        setContentView(R.layout.activity_main);
        topLevelLayout = findViewById(R.id.top_layout);
        View v = findViewById(android.R.id.content);
        //setContentView(v);

        final GestureDetector gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                PostTaskOverride task = new PostTaskOverride();
                task.execute();
                Toast.makeText(MainActivity.this,
                        "Wait for the next step", Toast.LENGTH_SHORT).show();

                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);

                isRotate = !isRotate;
                if(isRotate) {
                    Toast.makeText(MainActivity.this,
                            "Auto rotation is turned on", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this,
                            "Auto rotation is turned off", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }


        });
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return gd.onTouchEvent(event);
            }
        });


        brickTypeTextView = (TextView) findViewById(R.id.brick_type);
        brickTypeImageView = (ImageView) findViewById(R.id.brick_pic);
        brickStepTextView = (TextView) findViewById(R.id.step_tv);
        wrongBrickView = findViewById(R.id.wrong_brick_view);

        brickTypeTextView.setText("");
        lm = new LegoModel();
        mContext = this.getApplicationContext();
        this.legoModelStructureID = Integer.parseInt(getIntent().getStringExtra("LEGO_MODEL_ID"));
        this.your_IP_address = getIntent().getStringExtra("IP_ADDRESS");
        System.out.println("IP ADD " + your_IP_address);
        baseUrl = "http://" + your_IP_address + "/" + your_web_app + "/";

        if (isFirstTime()) {
            topLevelLayout.setVisibility(View.INVISIBLE);
        }

        


        //while (loadModelDone == false) {}
        //DIS IS NEEDED IN THE START, REMOVE ALL THEN ADD 1 by 1

        // POSSIBLE BUG IN THE FUTURE
        // populateTrackableObjects IS USED AS INITIALIZATION

        /*(TESTING) onCreate of MainActivity here is DONE*/
        try {
            String[] data = {"MainActivity OnCreate Done", DateFormat.getDateTimeInstance().format(new Date())};
            new WriteCSV(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        modelUpdaterHandler.removeCallbacks(modelUpdaterRunnable);
        nextStepAnimationHandler.removeCallbacks(nextStepAnimationRunnable);
    }

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.commit();
            topLevelLayout.setVisibility(View.VISIBLE);
            topLevelLayout.setOnTouchListener(new View.OnTouchListener(){

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    topLevelLayout.setVisibility(View.INVISIBLE);
                    return false;
                }

            });


        }
        return ranBefore;

    }

    //THREADING ON BRICK UPDATE
    Handler initializeHandler = new Handler();

    Runnable initializeRunnable = new Runnable() {
        public void run() {
            // transfer to parent object
            for(int i=0; i<modelList.size(); i++){
                Log.d("rotsex",String.valueOf(i));
                tckobj.removeChild(modelList.get(i));
                parentObject.addChild(modelList.get(i));
            }
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
                    //FINISH ANIMATION
                    if(nextStep > 0) {
                        for(int i = 1; i <= nextStep; i++) {
                            modelList.get(i - 1).clearTranslation();
                            modelList.get(i - 1).translate(new SimpleVector(0, 0, 0));
                        }
                    }
                    updateErrorTV();
                    updateModelOnScreen();
                    isNewStep = false;
                }
                updateErrorTV();
                modelUpdaterHandler.postDelayed(this, 500); // remove delay?
            }
            else {
                //FINISH ANIMATION
                if(nextStep > 0) {
                    for(int i = 1; i <= nextStep; i++) {
                        modelList.get(i - 1).clearTranslation();
                        modelList.get(i - 1).translate(new SimpleVector(0, 0, 0));
                    }
                }
                finishBuilding();
                modelUpdaterHandler.removeCallbacks(modelUpdaterRunnable);
            }
        }
    };

    private void updateErrorTV() {
        if(hasError){
            wrongBrickView.setVisibility(View.VISIBLE);

            Animation anim = new AlphaAnimation(0.0f, 1.0f);

            anim.setDuration(350); //You can manage the blinking time with this parameter

            anim.setStartOffset(20);

            anim.setRepeatMode(Animation.REVERSE);

            anim.setRepeatCount(Animation.INFINITE);

            wrongBrickView.startAnimation(anim);
        } else {
            wrongBrickView.setVisibility(View.INVISIBLE);
            wrongBrickView.clearAnimation();
        }
    }

    //THREADING ON BRICK UPDATE
    Handler nextStepAnimationHandler = new Handler();

    Runnable nextStepAnimationRunnable = new Runnable() {
        public void run() {
//            Log.d("animNextStep", "PASOK: " + nextStep);
            if(nextStep!=-1 && nextStep!=maxStep) {
                if(modelList.get(nextStep).getTranslation().z > 0) // moves down
                    modelList.get(nextStep).translate(new SimpleVector(0, 0, -1));
                else
                    modelList.get(nextStep).translate(new SimpleVector(0, 0, 70)); // return to elevated offset
//                Log.d("animNextStep","Z VALUE: " + String.valueOf(modelList.get(nextStep).getOrigin().z));
//                Log.d("animNextStep","TRANSLATION VALUE: " + modelList.get(nextStep).getTranslation().z);
            }
            nextStepAnimationHandler.postDelayed(nextStepAnimationRunnable, 20);
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
        text.setText("Congratulations! You have successfully built " + lm.getModelName(legoModelStructureID));
        text.setTextColor(Color.parseColor("#000000"));
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
            if(checkIfChildExist(parentObject)) {
                Log.d("checkchild", "pasok");
                removeModelUntilStep(nextStep-1);
            }
            completeModelUntilStep(nextStep);
            //tckobj.addChild(modelList.get(nextStep));
            updateBrickTypeTV(modelList.get(nextStep).getName(), nextStep);
            /*(TESTING) AR MODEL Finish Update*/
            try {
                String[] data = {"AR MODEL Finish Update", DateFormat.getDateTimeInstance().format(new Date())};
                new WriteCSV(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkIfChildExist(Object3D tckobj) {
        for(int i = 0; i <= nextStep; i++)
        {
            Log.d("checkchild", String.valueOf(nextStep));
            if(tckobj.hasChild(modelList.get(i)))
                return true;
        }
        return false;
    }

    private void removeModelUntilStep(int nextStep) {
        for(int i = 0; i <= nextStep; i++)
        {
            if(checkIfChildExist(parentObject)) // ADDED OK NA ATA?
                parentObject.removeChild(modelList.get(i));
        }
    }

    //COMPLETE MODEL GIVEN STEP
    private void completeModelUntilStep(int nextStep) {
        for(int i = 0; i <= nextStep; i++)
        {
            parentObject.addChild(modelList.get(i));
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
                    case "2": brickTypeImageView.setImageResource(R.drawable.step_3001_2); break;
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
                    case "4": brickTypeImageView.setImageResource(R.drawable.step_3003_4); break;
                    case "14": brickTypeImageView.setImageResource(R.drawable.step_3003_14); break;
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

        brickStepTextView.setText("STEP:" + step + "/" + modelList.size());
        brickTypeTextView.setText(brick);

    }

    private void removeAllModelsOnScreen() {
        System.out.println("RIP " + modelList.size());
        for (int i = 0; i < modelList.size(); i++) {
            parentObject.removeChild(modelList.get(i));
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
        //world.setAmbientLight(200, 200, 200); //255 all original * too bright for our eyes
        world.setAmbientLight(255,255,255);
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
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001_green)), 64, 64));
        TextureManager.getInstance().addTexture("3001_2", texture);
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
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_red)), 64, 64));
        TextureManager.getInstance().addTexture("3003_4", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003_yellow)), 64, 64));
        TextureManager.getInstance().addTexture("3003_14", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3004_lime)), 64, 64));
        TextureManager.getInstance().addTexture("3004_27", texture);

        /*(TESTING) Load All Texture Finished*/
        try {
            String[] data = {"Load All Texture Finished", DateFormat.getDateTimeInstance().format(new Date())};
            new WriteCSV(data);
        } catch (IOException e) {
            e.printStackTrace();
        }



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
                    float scaleFactor = 16;
                    Object3D brickModel = loadModel(modelID + ".3ds", scaleFactor); //DIS SCALE
                    brickModel.setTexture(modelID + "_" + color);
                    brickModel.setName(modelID + "_" + color);
                    brickModel.setRotationMatrix(rotMatrix);
                    brickModel.setOrigin(new SimpleVector(yPos * (scaleFactor / 10) + 200, xPos * (scaleFactor / 10) - 100, zPos * (scaleFactor / 10)-0));
//                    brickModel.setOrigin(new SimpleVector(yPos*(scaleFactor/10), xPos*(scaleFactor/10), zPos*(scaleFactor/10)-0));

                    tckobj.addChild(brickModel);
                    modelList.add(brickModel);

                    /*(TESTING) Brick Loaded and Data are set*/
                    try {
                        String[] data = {"Brick Loaded and Data are set", DateFormat.getDateTimeInstance().format(new Date())};
                        new WriteCSV(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        parentObject.setOrigin(new SimpleVector(200,-100,0));
        tckobj.addChild(parentObject);
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
            /*(TESTING) Requested Step from SERVER*/
            try {
                String[] data = {"Request Step from Server", DateFormat.getDateTimeInstance().format(new Date())};
                new WriteCSV(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("dataServer", "Time: " + DateFormat.getDateTimeInstance().format(new Date()));
            Log.e(TAG, "Response from url: " + jsonStr);

//            if(your_IP_address.equals(""))
            jsonStr = "{ 'data': [{'currentStep': '0', 'maxStep': '12', 'modelName': 'Duck', 'hasError': '0', 'rotValue': '0'}] }"; //dummy data in case no server
            //jsonStr = "shit";
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray data = jsonObj.getJSONArray("data");

                    // looping through All Contacts
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject d = data.getJSONObject(i);
                        if(d.getInt("currentStep") >= nextStep)
                            nextStep = d.getInt("currentStep");
                        maxStep = d.getInt("maxStep");
                        modelName = d.getString("modelName");
                        errorValue_string = d.getString("hasError");
                        int prevRotValue = rotValue;
                        rotValue = Integer.valueOf(d.getString("rotValue"));
                        if(isRotate){
                            parentObject.rotateZ(rotValue-prevRotValue);
                        }
                        else{
                            parentObject.clearRotation();
                        }
                        //ROTATE HERE

                        Log.d("fromServer wtf", errorValue_string);
                        if(errorValue_string.equals("1"))
                            hasError = true;
                        else if(errorValue_string.equals("0"))
                            hasError = false;


                        Log.d("fromServer", String.valueOf(nextStep));
                        Log.d("fromServer", String.valueOf(maxStep));
                        Log.d("fromServer ERROR", String.valueOf(hasError));
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


/*                if(nextStep + 2 < maxStep)
                    nextStep += 2;*/

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

    public class PostTaskOverride extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                String getResponse = post(Integer.toString(nextStep++)); //"http://httpbin.org/post"
                return getResponse;
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String getResponse) {
            System.out.println("Override to: " + getResponse);
        }

        private String post(String currentStep) throws IOException {
            String your_web_app = "next-step?id="+currentStep+"";  //Replace this with your own web app name
            System.out.println("hey " + currentStep);
            String baseUrl = "http://" + your_IP_address + "/" + your_web_app;

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        }
    }
}