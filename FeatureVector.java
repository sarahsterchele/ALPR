package tess4j.example;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public final class FeatureVector {
    static int BLACK = 0;
    static int WHITE = 255;
    
    private FeatureVector() {}
    
    public static ArrayList < String > calculateFeatureVector(Mat glyph) throws IOException {

        //Resize the glyph so height is 175 and aspect ratio maintained.
        double ratio = 175 / (double) glyph.height();
        double width = glyph.width() * ratio;
        Imgproc.resize(glyph, glyph, new Size(width, 175));

        //Process Glyph 
        Imgproc.cvtColor(glyph, glyph, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(glyph, glyph);
        Imgproc.GaussianBlur(glyph, glyph, new Size(9, 9), 0, 0, 1); //Blur heavily

        Imgproc.threshold(glyph, glyph, 100, 255, Imgproc.THRESH_BINARY);
        Imgproc.dilate(glyph,  glyph,  new Mat());
        Imgproc.erode(glyph, glyph, new Mat());

        // Normalize area
        float numOfPixels = glyph.width() * glyph.height();
        float area = area(glyph) / numOfPixels;

        return new ArrayList < String > (Arrays.asList(Float.toString(area), 
                Float.toString(centroid(glyph).x), 
                Float.toString(centroid(glyph).y), 
                Float.toString(horizontalStrokes(glyph)), 
                Float.toString(verticalStrokes(glyph)), 
                Float.toString(perimeter(glyph)))
                );
    }

    //Count the number of black pixels (0 is foreground pixel)
    private static float area(Mat m) {
        int area = 0;
    
        for (int y = 0; y < m.rows(); y++) {
            for (int x = 0; x < m.cols(); x++) {
                if (m.get(y, x)[0] == BLACK) {
                    area++;
                }
            }
        }
    
        return area;
    }

    private static Point centroid(Mat B) {
        int A = (int) area(B);
        int x = 0;
        int y = 0;
    
        for (int i = 0; i < B.rows(); i++) {
            for (int j = 0; j < B.cols(); j++) {
                //Convert from 0-255 to to 0-1 where 0 is foreground pixel
                double pixelValue = 1 - (B.get(i, j)[0] / WHITE);
                y += j * pixelValue;
                x += i * pixelValue;
            }
        }
    
        y = y / A;
        x = x / A;
    
        return new Point(x, y);
    }

    static int THICKNESS = 3;
    private static int horizontalStrokes(Mat m) {
        int currentWidth = 0;
        int MAX_WIDTH = m.width() - 15;
        int last_y_coord;
        int numberOfHorizontalLines = 0;
        boolean trueElementHit = false;
        int bestWidth = 0;
        int thickness = 0;
    
        for (int i = 5; i < m.cols(); i++) {
            last_y_coord = -1;
    
            for (int j = 0; j < m.rows(); j++) {
                if (m.get(j, i)[0] == 0) {
                    trueElementHit = true;
    
                    int k = i;
                    while (k < m.cols() && m.get(j, k)[0] == BLACK) {
                        k++;
                        currentWidth++;
                    }
    
                    if (currentWidth >= MAX_WIDTH) {
                        if (last_y_coord == j - 1) {
                            thickness++;
                            if (currentWidth > bestWidth)
                                bestWidth = currentWidth;
                        }
                        last_y_coord = j;
                    } else {
                        if (thickness > THICKNESS) {
                            numberOfHorizontalLines++;
                            thickness = 0;
                        }
                    }
                    currentWidth = 0;
                } else {
                    if (thickness > THICKNESS) {
                        numberOfHorizontalLines++;
                        thickness = 0;
                    }
                }
            }
    
            if (trueElementHit && bestWidth != 0)
                break;
        }
    
        if (thickness > THICKNESS) {
            numberOfHorizontalLines++;
        }
    
        return numberOfHorizontalLines;
    }
    
    private static int verticalStrokes(Mat m) {
        int currentHeight = 0;
        int MAX_HEIGHT = m.height() - 15;
        int last_x_coord = -2;
        int numberOfVerticalLines = 0;
        boolean trueElementHit = false;
        int bestHeight = 0;
        int thickness = 0;
    
        for (int i = 5; i < m.rows(); i++) {
            last_x_coord = -1;
            for (int j = 0; j < m.cols(); j++) {
                if (m.get(i, j)[0] == 0) {
                    trueElementHit = true;
    
                    int k = i;
                    while (k < m.rows() && m.get(k, j)[0] == BLACK) {
                        k++;
                        currentHeight++;
                    }
    
                    if (currentHeight >= MAX_HEIGHT) {
                        if (last_x_coord == j - 1) {
                            thickness++;
                            if (currentHeight > bestHeight)
                                bestHeight = currentHeight;
                        }
                        last_x_coord = j;
                    } else {
                        if (thickness > THICKNESS) {
                            numberOfVerticalLines++;
                            thickness = 0;
                        }
                    }
                    currentHeight = 0;
                } else {
                    if (thickness > THICKNESS) {
                        numberOfVerticalLines++;
                        thickness = 0;
                    }
                }
            }
    
            if (trueElementHit && bestHeight != 0)
                break;
        }
    
        if (thickness > THICKNESS) {
            numberOfVerticalLines++;
        }
    
        return numberOfVerticalLines;
    }

    private static float perimeter(Mat m) {
        float perimeter = 0;
        float numOfPixels = m.width() * m.height();
    
        Imgproc.Canny(m, m, 80, 160);
        for (int y = 0; y < m.rows(); y++) {
            for (int x = 0; x < m.cols(); x++) {
                if (m.get(y, x)[0] == WHITE) {
                    perimeter++;
                }
            }
        }
    
        return perimeter / numOfPixels;
    }
    
    /* DEBUGGING - Code taken from Stack */
    
    //Display image for debugging.
    public static void displayImage(Image img2) {
        ImageIcon icon = new ImageIcon(img2);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img2.getWidth(null) + 50, img2.getHeight(null) + 50);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    //Convert Mat image to a BufferedImage for debugging output.
    public static BufferedImage Mat2BufferedImage(Mat m) {
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