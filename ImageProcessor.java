package tess4j.example;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.opencv.core.*;
import org.opencv.imgproc.*;


public class ImageProcessor 
{  
    File trainingDataStore;
    ImageObject imageData;

    public ImageProcessor(String trainingDataFileName) {
        trainingDataStore = new File(trainingDataFileName);
    }
    
    public ImageObject processLargeImage(Mat image, int threshold, int equalizeHistogram, int kernelSize) throws IOException {
        imageData = new ImageObject();
        
        Mat originalImage = image.clone();
        
        //Resize image, maintaining aspect ratio
        double imageRatio = 800 / (double) originalImage.width();
        double height = originalImage.height() * imageRatio;
        Imgproc.resize(originalImage, originalImage, new Size(800, height));
        
        imageData.setOriginalImage(originalImage.clone());
        
        Mat processedImage = originalImage.clone();
        
        Imgproc.cvtColor(processedImage, processedImage, Imgproc.COLOR_RGB2GRAY); 
               
        if (equalizeHistogram != 0)
            Imgproc.equalizeHist(processedImage,  processedImage);
        
        Imgproc.threshold(processedImage, processedImage, threshold, 255, Imgproc.THRESH_BINARY); //160

        Imgproc.GaussianBlur(processedImage, processedImage, new Size(kernelSize, kernelSize), 0, 0, 1); //Blur heavily
        imageData.setBlur(processedImage.clone());

        Imgproc.Canny(processedImage, processedImage, 80, 160);
        Imgproc.dilate(processedImage, processedImage, new Mat());

        imageData.setLargeEdges(processedImage.clone());
        
        List < MatOfPoint > contours = new ArrayList < MatOfPoint > ();
        Imgproc.findContours(processedImage, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat allContours = originalImage.clone();
        boolean found = false;
        for (int i = 0; i < contours.size(); i++) {
            
            MatOfPoint contour = contours.get(i);
            Rect drawnRect = Imgproc.boundingRect(contour);
            float ratio = (float) drawnRect.width / (float) drawnRect.height;
            Imgproc.rectangle(allContours, drawnRect.tl(), drawnRect.br(), new Scalar(0, 0, 255), 3);

            if (ratio > 1.9 
                    && ratio < 4 
                    && drawnRect.width > 150 
                    && drawnRect.width < 250 
                    && drawnRect.height > 70 
                    && drawnRect.height < 120 
                    && !found) {
                found = true;
                Mat m = originalImage.clone();
                Imgproc.rectangle(m, drawnRect.tl(), drawnRect.br(), new Scalar(0, 0, 255), 3);
                imageData.setLargeSegment(m);
                imageData =  processPlate(originalImage.submat(drawnRect));
            }
        }
        imageData.setAllLargeSegment(allContours.clone());
        
        return imageData;
    }
    
    private ImageObject processPlate(Mat originalImage) throws IOException {
        double imageRatio = 800 / (double) originalImage.width();
        double height = originalImage.height() * imageRatio;
        Imgproc.resize(originalImage, originalImage, new Size(800, height));
                
        imageData.setFirstCrop(originalImage.clone());
        
        Mat firstEdit = originalImage.clone();
        Mat secondEdit = originalImage.clone();
        Mat thirdEdit = originalImage.clone();
        
        //Find the horizontal lines using Sobel dy direction to get the horizontal projection
        Imgproc.cvtColor(firstEdit, firstEdit, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(firstEdit, firstEdit);

        Core.bitwise_not(firstEdit, firstEdit);
        Imgproc.GaussianBlur(firstEdit, firstEdit, new Size(3, 3), 0, 0, 1);
        Imgproc.Sobel(firstEdit, firstEdit, CvType.CV_8UC1, 0, 1);
        Imgproc.dilate(firstEdit, firstEdit, new Mat());
        Imgproc.threshold(firstEdit, firstEdit, 120, 255, Imgproc.THRESH_BINARY);
        imageData.setSobel(firstEdit.clone());

        //Get the horizontal projection of the plate.
        int horizontalProjection[] = new int[firstEdit.rows()];
        for (int y = 0; y < firstEdit.rows(); y++) {
            for (int x = 0; x < firstEdit.cols(); x++) {
                if (firstEdit.get(y, x)[0] == 255) {
                    horizontalProjection[y]++;
                }
            }
        }
        
        imageData.setVerticalProjectionGraph(horizontalProjection);        

        //Find the peaks in the first and third of the projection. 
        //There are where the plate numbers generally lie.
        int split = firstEdit.rows() / 2;
        
        //First Peak
        int startingBuffer = 10;
        int firstPeak = horizontalProjection[startingBuffer];
        int firstPeakLoc = startingBuffer;
        
        for (int i = startingBuffer; i < split; i++) {
            int value = horizontalProjection[i];
            if (value > firstPeak) {
                firstPeak = value;
                firstPeakLoc = i;
            }
        }
        
        //second Peak
        int endingBuffer = 25;
        int secondPeak = horizontalProjection[split];
        int secondPeakLoc = split;

        for (int i = split; i < firstEdit.rows() - endingBuffer; i++) {
            int value = horizontalProjection[i];
            if (value > secondPeak) {
                secondPeak = value;
                secondPeakLoc = i + 5;
            }
        }
        
        //Sometimes the top of the plate is already cropped by a frame. Check if that is likely.
        firstPeakLoc = firstPeak > 150 ? firstPeakLoc - 15 : 0; 

        //Crop the image to the plate numbers. Add a buffer in case it is cropped too tightly
        secondEdit = secondEdit.rowRange(firstPeakLoc, secondPeakLoc + 20);
        thirdEdit = thirdEdit.rowRange(firstPeakLoc, secondPeakLoc + 20);
        originalImage = originalImage.rowRange(firstPeakLoc, secondPeakLoc + 20);

        // From the reduced image, find the glyph segments.
        Imgproc.cvtColor(secondEdit, secondEdit, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(secondEdit, secondEdit);
        Core.bitwise_not(secondEdit, secondEdit);
        Imgproc.threshold(secondEdit, secondEdit, 190, 255, Imgproc.THRESH_BINARY);
        Imgproc.GaussianBlur(secondEdit, secondEdit, new Size(9, 9), 0, 0, 1);
        Imgproc.Canny(secondEdit, secondEdit, 80, 160, 3, true);
        Imgproc.dilate(secondEdit, secondEdit, new Mat());

        imageData.setGlyphEdges(secondEdit.clone());

        //Find the contours
        List < MatOfPoint > contours = new ArrayList < MatOfPoint > ();
        Imgproc.findContours(secondEdit, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //Store the contours/glyphs in order of left to right position in the image. TreeMap keeps in sorted order.
        TreeMap < Double, ArrayList < String >> featureVectors = new TreeMap < Double, ArrayList < String >> ();     
        TreeMap < Double, Mat > glyphs = new TreeMap < Double, Mat > ();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            Rect drawnRect = Imgproc.boundingRect(contour);
            double topLeftIndex = drawnRect.tl().x;

            if (drawnRect.height > 155 
                    && drawnRect.width > 30 
                    && drawnRect.width < 200 
                    && !featureVectors.containsKey(topLeftIndex)) {
                Mat glyph = thirdEdit.submat(drawnRect);
                glyphs.put(topLeftIndex, glyph.clone());

                ArrayList < String > featureVector = FeatureVector.calculateFeatureVector(glyph.clone());
                featureVectors.put(topLeftIndex, featureVector);

                Imgproc.rectangle(originalImage, drawnRect.tl(), drawnRect.br(), new Scalar(0, 0, 255), 3);
            }
        }
        
        imageData.setSegmentedGlyphs(originalImage.clone());
        imageData.setFeatureVectors(featureVectors);
        imageData.setGlyphs(glyphs);

        return imageData;
    }
    
    @SuppressWarnings("unused")
    private void storeTrainingData(TreeMap < Double, ArrayList < String >> featureVectors) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(trainingDataStore, true));
        for (Map.Entry < Double, ArrayList < String >> vector: featureVectors.entrySet()) {
            String output = vector.getValue() + "\r\n";
            out.write(output);
            out.flush();
        }
        out.close();
    }
}