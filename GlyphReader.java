package tess4j.example;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;


public class GlyphReader 
{
    int NUMBER_OF_NEIGHBORS = 3;

    ArrayList<ArrayList<String>> trainingData;  
    ITesseract ocrReader;
    
    public GlyphReader(ITesseract ocrReader, String dataFileName, int k) throws IOException {
        this.ocrReader = ocrReader;
        NUMBER_OF_NEIGHBORS = k;
        //Store training data in memory
        trainingData = new ArrayList<ArrayList<String>>();
        for (String line : Files.readAllLines(Paths.get(dataFileName))) {
            ArrayList<String> featureVector = new ArrayList<String>();
            for (String feature : line.split("\\s+")) {
                featureVector.add(feature);
            }
            
            trainingData.add(featureVector);
        }
    }
    
    // Given a set of characters segmented from the license plate, pass each through Tesseract and return the string.
    // The glyphs are ordered characters in the TreeMap from  left to right position on the plate.
    public String readPlateWithTess(TreeMap<Double, Mat> glyphs) throws TesseractException {
        String plate = "";
        
        for (Map.Entry<Double, Mat> glyph: glyphs.entrySet()) {
            Mat glyphMat = glyph.getValue();
            Imgproc.cvtColor(glyphMat, glyphMat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.equalizeHist(glyphMat, glyphMat);
            Imgproc.GaussianBlur(glyphMat, glyphMat, new Size(5, 5), 0, 0, 1);
            Imgproc.threshold(glyphMat, glyphMat, 85, 255, Imgproc.THRESH_BINARY);
            Imgproc.erode(glyphMat, glyphMat, new Mat());
            Imgproc.erode(glyphMat, glyphMat, new Mat());
            plate += ocrReader.doOCR(convertMatToBufferedImage(glyphMat));
        }
        
        return plate;
    }
    
    // Use KNN to determine a glyph based on its feature vector.
    public String getGlyph(ArrayList<String> featureVector) {
        TreeMap<Double, String> knn = new TreeMap<Double, String>();
            
        //Calculate the Euclidian Distance between the glyph feature vector and each training data vector. 
        // Store the distance and its corresponding glyph in an ordered Tree Map.
        for(int i = 0; i < trainingData.size(); i++) {
            double sum = 0;
            
            //There are 6 features in the feature vector: 
            //Area, CentroidX, CentroidY, Vertical Strokes, Horizontal Strokes, and Perimeter.
            for (int j = 0; j < 6; j++) {
                float x = Float.parseFloat(trainingData.get(i).get(j));
                float y = Float.parseFloat(featureVector.get(j));
                
                //Place a higher weight on the Number of strokes in each direction a glyph has. 
                //This generally is a good indicator of what a glyph is not.
                if (i == 3 || i == 4) 
                    sum += (Math.pow((x - y), 2)) * 5; 
                else
                    sum += Math.pow((x - y), 2);
            }
            
            double distance = Math.sqrt(sum);
            knn.put(distance, trainingData.get(i).get(6));
        }
        
        //From the k nearest neighbors, tally up the votes and select the best.
        Map<String, Integer> glyphVotes = new HashMap<String, Integer>();
        for (int i = 0; i < NUMBER_OF_NEIGHBORS; i++) {
            Double key = (Double) knn.keySet().toArray()[i];
            String glyph = knn.get(key);
            
            int count = glyphVotes.containsKey(glyph) ? glyphVotes.get(glyph) : 0;
            glyphVotes.put(glyph, count + 1);
        }
        
        int frequency = 0;
        String bestGlyph = "";
        for(Map.Entry<String, Integer> glyph: glyphVotes.entrySet()) {
            if (glyph.getValue() > frequency) {
                frequency = glyph.getValue();
                bestGlyph = glyph.getKey();
            }
        }

        return bestGlyph;
    }
    
    // Taken from StackOverflow helper function
    private static BufferedImage convertMatToBufferedImage(Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}
