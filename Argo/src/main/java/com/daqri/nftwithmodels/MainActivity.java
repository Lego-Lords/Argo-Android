package com.daqri.nftwithmodels;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Vibrator;
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
    private Object3D legoModel1;
    private Object3D legoModel2;
    private ArrayList<Object3D> modelList = new ArrayList<>();
    private boolean firstTap = true;
    private int currentModel = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = supplyFrameLayout();
        // When the screen is tapped, inform the renderer and vibrate the phone
        mainLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(firstTap)
                {
                    for(int i = 0; i < modelList.size(); i++)
                    {
                        tckobj.removeChild(modelList.get(i));
                    }
                    firstTap = !firstTap;
                }
                else if(currentModel < modelList.size())
                {
                    tckobj.addChild(modelList.get(currentModel));
                    currentModel += 1;
                }
                else
                {
                    currentModel = 0;
                    firstTap = !firstTap;
                }




                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vib.vibrate(100);

            }

        });
    }
    

    /**
     * Use the FrameLayout in this Activity's UI.
     */
    @Override
    protected FrameLayout supplyFrameLayout() {
        return (FrameLayout)this.findViewById(R.id.mainLayout);
    }

    public void configureWorld(World world) {
        Config.farPlane = 2000;
        world.setAmbientLight(200, 200, 200); //255 all original
    }

    protected void populateTrackableObjects(List<TrackableObject3d> list) {
        this.list = list;
        // Note: The NASA logo is really bad for tracking
         //tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");

/*        Texture texture = new Texture(getResources().getDrawable(R.drawable.duplo4baked));
        TextureManager.getInstance().addTexture("duplo4baked", texture);
        texture = new Texture(getResources().getDrawable(R.drawable.moon_ground));
        TextureManager.getInstance().addTexture("moon_ground", texture);*/

        //TextureManager.getInstance().addTexture("wolf", new Texture(getResources().getDrawable(R.drawable.wolf)));
        Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001)), 64, 64));
        TextureManager.getInstance().addTexture("3001", texture);
        Texture texture2 = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003)), 64, 64));
        TextureManager.getInstance().addTexture("3003", texture2);
//        try {
//            //legoModel1 = Loader.load3DS(getAssets().open("thisisit.3ds"), 30);
////            legoModel1 = Loader.loadOBJ(getAssets().open("legotestfromblender.obj"), getAssets().open("legotestfromblender.mtl"), 10);
//            legoModel1 = loadModel("one_two.3ds", 20);
//
//            legoModel1.setTexture("one_two_blue");
//            legoModel1.strip();
//            legoModel1.build();
//            //legoModel1.setTransparency(-1);
//            //Object3D [] astronaut = Loader.loadOBJ(getAssets().open("legoBrick.obj"), getAssets().open("legoBrick.mtl"), 50);
//            // legoModel1[0].setOrigin(new SimpleVector(0, 0, 0));
//            //astronaut[0].setTexture("duplo4baked");
//            //legoModel1[0].setTexture("legotext");
//            tckobj.addChild(legoModel1);
//            modelList.add(legoModel1);
//
//            legoModel2 = loadModel("duplo4.3ds", 30);
//            //legoModel2 = Loader.load3DS(getAssets().open("duplo4.3ds"), 30);
//            //Object3D [] astronaut = Loader.loadOBJ(getAssets().open("legoBrick.obj"), getAssets().open("legoBrick.mtl"), 50);
//            legoModel2.setOrigin(new SimpleVector(150, -150, 30));
//            //astronaut[0].setTexture("duplo4baked");
//            tckobj.addChild(legoModel2);
//            modelList.add(legoModel2);
//
//
//          /*  // Put a plane to see where it cuts
//            Object3D object3D = Primitives.getPlane(2, 200);
//            // Planes are rotated 180 degrees, so we need to flip them
//            object3D.rotateX((float) Math.PI);
//            object3D.setOrigin(new SimpleVector(125, 125, 0));
//            //object3D.setTexture("moon_ground");
//            tckobj.addChild(object3D);*/
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        AssetManager assetManager = getResources().getAssets();
        // To load text file
        InputStream input;
        String filename = "snowcat";
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
                    //data for rotation NEXT TIME
                    while (m.find()) {
                        counter++;
                        switch(counter){
                            case 3: xPos = Float.valueOf(m.group(1));break;
                            case 4: zPos = Float.valueOf(m.group(1))*-1;break; // times -1 kasi opposite yung pag show sa phone
                            case 5: yPos = Float.valueOf(m.group(1));break;
                            case 15: modelID = m.group(1);modelID=modelID.replace(".","");break; // replace "." kasi may bug regex haha
                        }
                    }
                    // build brick model
                    Object3D brickModel = loadModel(modelID + ".3ds", 10);
                    brickModel.setTexture(modelID);
                    brickModel.setOrigin(new SimpleVector(yPos, xPos, zPos));
                    brickModel.strip();
                    brickModel.build();

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
        Object3D[] model = Loader.load3DS(getAssets().open(filename), scale);
        Object3D o3d = new Object3D(0);
        Object3D temp = null;
        for (int i = 0; i < model.length; i++) {
            temp = model[i];
            temp.setCenter(SimpleVector.ORIGIN);
            temp.rotateX((float)( -.5*Math.PI));
            //temp.rotateMesh();
            temp.setRotationMatrix(new Matrix());
            o3d = Object3D.mergeObjects(o3d, temp);
            o3d.build();
        }
        return o3d;
    }
}