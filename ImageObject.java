package tess4j.example;
import java.util.ArrayList;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.opencv.core.Mat;

public class ImageObject {
    
    Mat originalImage;
    Mat firstCrop;
    Mat segmentedGlyphs;
    Mat glyphEdges;
    Mat largeSegment;
    Mat allLargeSegments;
    Mat largeEdges;
    Mat sobel;
    Mat blur;
    JFreeChart verticalProjection;
    TreeMap<Double, ArrayList<String>> featureVectors;     
    TreeMap<Double, Mat> glyphs;
    
    public ImageObject() {};
    
    public ImageObject(Mat segmentedGlyphs, Mat largeSegment, TreeMap<Double, ArrayList<String>> featureVectors,  TreeMap<Double, Mat> glyphs) {
        this.segmentedGlyphs = segmentedGlyphs;
        this.featureVectors = featureVectors;
        this.glyphs = glyphs;
        this.largeSegment = largeSegment;
    }
    
    public void setLargeSegment(Mat m) {
        largeSegment = m;
    }
    
    public void setBlur(Mat m) {
        blur = m;
    }
    
    public void setOriginalImage(Mat m) {
        originalImage = m;
    }
    
    public void setAllLargeSegment(Mat m) {
        allLargeSegments = m;
    }
    
    public void setSegmentedGlyphs(Mat m) {
        segmentedGlyphs = m;
    }
    
    public void setSobel(Mat m) {
        sobel = m;
    }
    
    public void setFirstCrop(Mat m) {
        firstCrop = m;
    }
    
    public void setVerticalProjectionGraph(int[] p) {
        createVerticalProjectionGraph(p);
    }
    
    public void setFeatureVectors(TreeMap<Double, ArrayList<String>> f) {
        featureVectors = f;
    }
    
    public void setGlyphs(TreeMap<Double, Mat> g) {
        glyphs = g;
    }
    
    public void setGlyphEdges(Mat m) {
        glyphEdges = m;
    }
    
    public void setLargeEdges(Mat m) {
        largeEdges = m;
    }
    
    private void createVerticalProjectionGraph(int[] array) {
        verticalProjection = ChartFactory.createLineChart(
            null, null, null,
            createDataset(array),
            PlotOrientation.HORIZONTAL,
            true, false, false
        );
    }

    private DefaultCategoryDataset createDataset(int[] array) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < array.length; i++)
            dataset.addValue(array[i], "", Integer.toString(i));

        return dataset;
    }
}
