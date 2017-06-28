package tess4j.example;
import java.io.File;
import java.io.IOException;
import org.opencv.core.Core;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class ALPR {
    static String TRAINING_DATA_FILE_NAME = "data.txt";

    public static void main(String[] args) throws IOException {
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        ImageProcessor imageProcessor = new ImageProcessor(TRAINING_DATA_FILE_NAME);
        
        // Configure Tesseract to find the trained data and what characters to look for.
        ITesseract tesseractOCR = new Tesseract();
        String datapath = new File(".").getCanonicalPath();
        tesseractOCR.setDatapath(datapath + "/Tess4J-3.3.1-src/Tess4J/");
        tesseractOCR.setTessVariable("tessedit_char_whitelist", "ABCDEFGHJKLMNPQRSTVWXYZ1234567890");
        
        int k = 3; //K nearest neighbor
        GlyphReader glyphReader = new GlyphReader(tesseractOCR, TRAINING_DATA_FILE_NAME, k);

        @SuppressWarnings("unused")
        GUI gui = new GUI(imageProcessor, glyphReader);
    }
}