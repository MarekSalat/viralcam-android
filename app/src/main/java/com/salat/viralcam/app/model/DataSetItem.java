package com.salat.viralcam.app.model;

/**
 * Created by Marek on 21.03.2016.
 */
public class DataSetItem implements Comparable<DataSetItem> {
    public final int id;
    public final String name;
    public final String imagePath;
    public final String trimapPath;
    public final String trueAlphaPath;

    public final String resultAlphaPath;
    public final String resultImagePath;

    public DataSetItem(int id, String name, String imagePath, String trimapPath, String trueAlphaPath, String resultAlphaPath, String resultImagePath) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.trimapPath = trimapPath;
        this.trueAlphaPath = trueAlphaPath;
        this.resultAlphaPath = resultAlphaPath;
        this.resultImagePath = resultImagePath;
    }

    @Override
    public String toString() {
        return "DataSetItem{" +
                "id=" + id +
                ", imagePath='" + imagePath + '\'' +
                ", trimapPath='" + trimapPath + '\'' +
                ", trueAlphaPath='" + trueAlphaPath + '\'' +
                '}';
    }

    @Override
    public int compareTo(DataSetItem another) {
        return id - another.id;
    }
}
