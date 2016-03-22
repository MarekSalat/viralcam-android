package com.salat.viralcam.app.model;

import android.support.annotation.NonNull;

public class EvaluationResult implements Comparable<EvaluationResult>{
    public final int id;
    private int width;
    private int height;

    private int foregroundPixels;
    private int backgroundPixels;

    private long computationTime;
    private double meanSquaredError;
    private long sumOfAbsoluteDifferences;

    public EvaluationResult(int id){
        this.id = id;
    }

    public int width() {
        return width;
    }

    public EvaluationResult width(int width) {
        this.width = width;
        return this;
    }

    public int height() {
        return height;
    }

    public EvaluationResult height(int height) {
        this.height = height;
        return this;
    }

    public int pixels() {
        return height() * width();
    }

    public int foregroundPixels() {
        return foregroundPixels;
    }

    public EvaluationResult foregroundPixels(int foregroundPixels) {
        this.foregroundPixels = foregroundPixels;
        return this;
    }

    public int backgroundPixels() {
        return backgroundPixels;
    }

    public EvaluationResult backgroundPixels(int backgroundPixels) {
        this.backgroundPixels = backgroundPixels;
        return this;
    }

    public int unknownPixels() {
        return pixels() - foregroundPixels() - backgroundPixels();
    }

    public long computationTime() {
        return computationTime;
    }

    public EvaluationResult computationTime(long computationTime) {
        this.computationTime = computationTime;
        return this;
    }

    public double meanSquaredError() {
        return meanSquaredError;
    }

    public EvaluationResult squaredError(long squaredError) {
        this.meanSquaredError = squaredError / pixels();
        return this;
    }

    public long sumOfAbsoluteDifferences() {
        return sumOfAbsoluteDifferences;
    }

    public EvaluationResult sumOfAbsoluteDifferences(long sumOfAbsoluteDifferences) {
        this.sumOfAbsoluteDifferences = sumOfAbsoluteDifferences;
        return this;
    }

    public double timePerUnknownPixel(){
        return computationTime() / (double) unknownPixels();
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "id=" + id +
                ", width=" + width() +
                ", height=" + height() +
                ", pixels=" + pixels() +
                ", foregroundPixels=" + foregroundPixels() +
                ", backgroundPixels=" + backgroundPixels() +
                ", unknownPixels=" + unknownPixels() +
                ", computationTime=" + computationTime() +
                ", timePerUnknownPixel=" + String.format("%.2f", timePerUnknownPixel()) +
                ", meanSquaredError=" + meanSquaredError() +
                ", sumOfAbsoluteDifferences=" + sumOfAbsoluteDifferences() +
                '}';
    }

    @Override
    public int compareTo(@NonNull EvaluationResult another) {
        return id - another.id;
    }
}
