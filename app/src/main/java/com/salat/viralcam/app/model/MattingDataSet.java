package com.salat.viralcam.app.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Marek on 21.03.2016.
 */
public class MattingDataSet {
    public static class AlphamattingComDataSet {
        public static final String ROOT_FOLDER = "viralcam";
        public static final String DATASET_FOLDER_LOCATION = ROOT_FOLDER + "/dataset";
        public static final String RESULT_FOLDER_LOCATION = ROOT_FOLDER + "/results";
        public static final String IMAGES_FOLDER_NAME = "input_training_lowres";
        public static final String TRIMAPS_FOLDER_NAME = "trimap_training_lowres";
        public static final String TRIMAP_VERSION_FOLDER_NAME_FORMAT = "Trimap%d"; // Trimap2
        public static final String TRUE_ALPHA_FOLDER_NAME = "gt_training_lowres";
        public static final String FILE_NAME_FORMAT = "GT%02d.png";
        public static final String RESULT_ALPHA_FOLDER_NAME = "alpha";
        public static final String RESULT_IMAGE_FOLDER_NAME = "image";

        public static final String PATH_TO_IMAGES = DATASET_FOLDER_LOCATION + "/" + IMAGES_FOLDER_NAME;
        public static final String PATH_TO_TRIMAPS = DATASET_FOLDER_LOCATION + "/" + TRIMAPS_FOLDER_NAME ;
        public static final String PATH_TO_TRUE_ALPHA = DATASET_FOLDER_LOCATION + "/" + TRUE_ALPHA_FOLDER_NAME;

        public static String getImagePath(int id){
            return PATH_TO_IMAGES + "/" + String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id);
        }

        public static String getTrimapPath(int id, int version){
            return PATH_TO_TRIMAPS + "/" +
                    String.format(AlphamattingComDataSet.TRIMAP_VERSION_FOLDER_NAME_FORMAT, version) + "/" +
                    String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id);
        }

        public static String getTrueAlphaPath(int id){
            return PATH_TO_TRUE_ALPHA + "/" + String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id);
        }

        public static String getResultAlphaPath(int id, int version){
            return RESULT_FOLDER_LOCATION + "/" +
                    String.format(AlphamattingComDataSet.TRIMAP_VERSION_FOLDER_NAME_FORMAT, version) + "/" +
                    RESULT_ALPHA_FOLDER_NAME + "/" +
                    String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id);
        }

        public static String getResultImagePath(int id, int version){
            return RESULT_FOLDER_LOCATION + "/" +
                    String.format(AlphamattingComDataSet.TRIMAP_VERSION_FOLDER_NAME_FORMAT, version) + "/" +
                    RESULT_IMAGE_FOLDER_NAME + "/" +
                    String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id);
        }
    }

    private Map<Integer, DataSetItem> dataset;
    private Map<DataSetItem, EvaluationResult> results = new TreeMap<>();

    private MattingDataSet(Map<Integer, DataSetItem> dataset){
        this.dataset = dataset;
    }

    public Map<Integer, DataSetItem> getItems(){
        return dataset;
    }

    public Map<DataSetItem, EvaluationResult> getResults(){
        return results;
    }

    public void addResult(DataSetItem item, EvaluationResult result){
        results.put(item, result);
    }

    public double avgComputationTime() {
        double sum = 0;
        for(EvaluationResult result : results.values()){
            sum += result.computationTime();
        }
        return sum / (double) results.size();
    }

    public double avgComputationTimePerUnknownPixel() {
        double sum = 0;
        for(EvaluationResult result : results.values()){
            sum += result.timePerUnknownPixel();
        }
        return sum / (double) results.size();
    }

    public double avgMse() {
        double sum = 0;
        for(EvaluationResult result : results.values()){
            sum += result.meanSquaredError();
        }
        return sum / (double) results.size();
    }

    public double avgSad() {
        double sum = 0;
        for(EvaluationResult result : results.values()){
            sum += result.sumOfAbsoluteDifferences();
        }
        return sum / (double) results.size();
    }

    public static MattingDataSet generateAlphamattingComDataSet(int trimapSetVersion){
        Map<Integer, DataSetItem> dataset = new TreeMap<>();

        for (int id = 1; id <= 27; id++) {
            dataset.put(id, new DataSetItem(id,
                    String.format(AlphamattingComDataSet.FILE_NAME_FORMAT, id),
                    AlphamattingComDataSet.getImagePath(id),
                    AlphamattingComDataSet.getTrimapPath(id, trimapSetVersion),
                    AlphamattingComDataSet.getTrueAlphaPath(id),
                    AlphamattingComDataSet.getResultAlphaPath(id, trimapSetVersion),
                    AlphamattingComDataSet.getResultImagePath(id, trimapSetVersion)));
        }

        return new MattingDataSet(dataset);
    }
}
