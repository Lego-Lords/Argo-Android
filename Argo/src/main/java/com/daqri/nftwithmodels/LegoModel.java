package com.daqri.nftwithmodels;

/**
 * Created by Kingston on 3/7/2017.
 */
public class LegoModel {
    private int[] image_resource = {R.drawable.snowcat, R.drawable.pyramid, R.drawable.tree};
    private String[] lego_name = {"Snowcat", "Pyramid", "Tree"};
    private String[] lego_filename = {"snowcat", "pyramid", "tree"};

    public int getImageResource(int index){
        return this.image_resource[index];
    }

    public int[] getImageArray(){
        return this.image_resource;
    }

    public String getModelName(int index){
        return this.lego_name[index];
    }

    public String getModelFileName(int index){
        return this.lego_filename[index];
    }
}
