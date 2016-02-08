package com.salat.viralcam.app.util;

import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Marek on 08.02.2016.
 */
public class AssetLoader {

    public static String loadFromRaw(Resources resources, int id){
        StringBuilder file = new StringBuilder();
        BufferedReader stream = null;
        try {
            stream = new BufferedReader(new InputStreamReader(resources.openRawResource(id), "UTF-8"));
            String line;
            while ((line=stream.readLine()) != null) {
                file.append(line);
                file.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(stream != null){
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.toString();
    }
}
