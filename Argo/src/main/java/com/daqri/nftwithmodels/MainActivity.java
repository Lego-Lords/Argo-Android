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

        Texture texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3001)), 64, 64));
        TextureManager.getInstance().addTexture("3001", texture);
        texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.modeltexture_3003)), 64, 64));
        TextureManager.getInstance().addTexture("3003", texture);




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
                    //data for rotation NEXT TIME
                    float[][] rotMatri = new float[3][3];

                    while (m.find()) {
                        counter++;
                        switch(counter){
                            case 2: //color
                            case 3: xPos = Float.valueOf(m.group(1));break;
                            case 4: zPos = Float.valueOf(m.group(1))*-1;break; // times -1 kasi opposite yung pag show sa phone
                            case 5: yPos = Float.valueOf(m.group(1));break;
                            case 6: rotMatri[0][0] = Float.valueOf(m.group(1));break;
                            case 7: rotMatri[0][1] = Float.valueOf(m.group(1));break;
                            case 8: rotMatri[0][2] = Float.valueOf(m.group(1));break;
                            case 9: rotMatri[1][0] = Float.valueOf(m.group(1));break;
                            case 10: rotMatri[1][1] = Float.valueOf(m.group(1));break;
                            case 11: rotMatri[1][2] = Float.valueOf(m.group(1));break;
                            case 12: rotMatri[2][0] = Float.valueOf(m.group(1));break;
                            case 13: rotMatri[2][1] = Float.valueOf(m.group(1));break;
                            case 14: rotMatri[2][2] = Float.valueOf(m.group(1));break;
                            case 15: modelID = m.group(1);modelID=modelID.replace(".","");break; // replace "." kasi may bug regex haha
                        }
                    }
                    // build brick model
                    Object3D brickModel = loadModel(modelID + ".3ds", 10);
                    brickModel.setTexture(modelID);
                    // set rotation
                    xPos = xPos*rotMatri[0][0] + yPos*rotMatri[0][1] + zPos*rotMatri[0][2];
                    yPos = xPos*rotMatri[1][0] + yPos*rotMatri[1][1] + zPos*rotMatri[1][2];
                    zPos = xPos*rotMatri[2][0] + yPos*rotMatri[2][1] + zPos*rotMatri[2][2];
                    brickModel.setOrigin(new SimpleVector(yPos, xPos, zPos));

/*                    brickModel.strip();
                    brickModel.build();*/
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