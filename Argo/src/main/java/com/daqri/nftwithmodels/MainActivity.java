package com.daqri.nftwithmodels;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ArJpctActivity {


    private List<TrackableObject3d> list;
    private TrackableObject3d tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");
    private ArrayList<Object3D> modelList = new ArrayList<>();
    private boolean firstTap = true;
    private int currentModel = 0;
    private World world;
    private Context mContext;
    private int legoModelStructureID;
    private LegoModel lm;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIntent().setAction("Already created");
        setContentView(R.layout.activity_main);

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
                    currentModel += 1;
                } else {
                    currentModel = 0;
                    firstTap = !firstTap;
                }


                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vib.vibrate(100);

            }

        });
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
}