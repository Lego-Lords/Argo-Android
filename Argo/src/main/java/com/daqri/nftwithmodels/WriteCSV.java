package com.daqri.nftwithmodels;

import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kingston on 7/27/2017.
 */
public class WriteCSV {
    public WriteCSV(String[] data) throws IOException {
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "AnalysisData.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath );
        CSVWriter writer;
// File exist
        if(f.exists() && !f.isDirectory()){
            FileWriter mFileWriter = new FileWriter(filePath, true);
            writer = new CSVWriter(mFileWriter);
        }
        else {
            writer = new CSVWriter(new FileWriter(filePath));
        }
        //String[] data = {"United States", "Washington D.C"};

        writer.writeNext(data);

        writer.close();
    }
}
