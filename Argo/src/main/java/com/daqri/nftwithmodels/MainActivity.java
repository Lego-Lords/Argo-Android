package com.daqri.nftwithmodels;

import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.FrameLayout;

import com.threed.jpct.Config;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import org.artoolkit.ar.jpct.ArJpctActivity;
import org.artoolkit.ar.jpct.TrackableLight;
import org.artoolkit.ar.jpct.TrackableObject3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ArJpctActivity {


    private List<TrackableObject3d> list;
    private TrackableObject3d tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");
    private Object3D[] legoModel1;
    private Object3D[] legoModel2;
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
        world.setAmbientLight(90, 90, 90); //255 all original
    }

    protected void populateTrackableObjects(List<TrackableObject3d> list) {
        this.list = list;
        // Note: The NASA logo is really bad for tracking
         //tckobj = new TrackableObject3d("multi;Data/multi/marker.dat");

/*        Texture texture = new Texture(getResources().getDrawable(R.drawable.duplo4baked));
        TextureManager.getInstance().addTexture("duplo4baked", texture);
        texture = new Texture(getResources().getDrawable(R.drawable.moon_ground));
        TextureManager.getInstance().addTexture("moon_ground", texture);*/

        //TextureManager.getInstance().addTexture("duplo4baked", new Texture(getResources().getDrawable(R.drawable.duplo4baked)));

        try {
            legoModel1 = Loader.load3DS(getAssets().open("legoBrick.3ds"), 30);
            //Object3D [] astronaut = Loader.loadOBJ(getAssets().open("legoBrick.obj"), getAssets().open("legoBrick.mtl"), 50);
            legoModel1[0].setOrigin(new SimpleVector(200, -100, 0));
            //astronaut[0].setTexture("duplo4baked");
            tckobj.addChild(legoModel1[0]);
            modelList.add(legoModel1[0]);

            legoModel2 = Loader.load3DS(getAssets().open("duplo4.3ds"), 30);
            //Object3D [] astronaut = Loader.loadOBJ(getAssets().open("legoBrick.obj"), getAssets().open("legoBrick.mtl"), 50);
            legoModel2[0].setOrigin(new SimpleVector(150, -150, 30));
            //astronaut[0].setTexture("duplo4baked");
            tckobj.addChild(legoModel2[0]);
            modelList.add(legoModel2[0]);


          /*  // Put a plane to see where it cuts
            Object3D object3D = Primitives.getPlane(2, 200);
            // Planes are rotated 180 degrees, so we need to flip them
            object3D.rotateX((float) Math.PI);
            object3D.setOrigin(new SimpleVector(125, 125, 0));
            //object3D.setTexture("moon_ground");
            tckobj.addChild(object3D);*/

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.list.add(tckobj);
    }
}