package tess4j.example;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.ChartPanel;
import org.opencv.core.Mat;
import net.sourceforge.tess4j.TesseractException;

@SuppressWarnings("serial")
public class GUI extends JFrame 
{   
    private ImageObject imageData;
    private JButton openButton;
    private JButton nextButton;
    private JFileChooser fileChooser;
    private BufferedImage image;
    private ImagePanel imagePanel;
    private File imageFile;  
    private JLabel knnPlate;
    private JLabel tesseractPlate;
    private ImageProcessor imageProcessor;
    private GlyphReader glyphReader;
    
    String knnString = "";
    String tessString = "";
    private static int page = 0;

    public GUI(ImageProcessor imageProcessor, GlyphReader glyphReader) throws IOException {
        this.imageProcessor = imageProcessor;
        this.glyphReader = glyphReader;
        
        setPreferredSize(new Dimension(800, 800));
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 20));

        //Create the Image Panel
        imagePanel = new ImagePanel();
        imagePanel.setPreferredSize(new Dimension(800, 500));
        add(imagePanel);

        //Plate Label
        knnPlate = new JLabel("KNN OCR", SwingConstants.CENTER);
        knnPlate.setPreferredSize(new Dimension(800, 50));
        knnPlate.setForeground(Color.RED);
        knnPlate.setFont(knnPlate.getFont().deriveFont((float) 64.0));
        add(knnPlate, new BorderLayout(5, 5));

        //Tesseract Label
        tesseractPlate = new JLabel("Tesseract OCR", SwingConstants.CENTER);
        tesseractPlate.setPreferredSize(new Dimension(800, 50));
        tesseractPlate.setForeground(Color.GREEN);
        tesseractPlate.setFont(tesseractPlate.getFont().deriveFont((float) 64.0));
        add(tesseractPlate);

        //Create a file chooser
        fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg");
        fileChooser.setFileFilter(filter);

        // Create an Open file button
        openButton = new JButton("Open File");
        openButton.setEnabled(true);
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    onGetImageClick();
                } catch (IOException | TesseractException e1) {
                    e1.printStackTrace();
                }
            }
        });
        openButton.setPreferredSize(new Dimension(750, 25));
        add(openButton);
        
        // Create an Open file button
        nextButton = new JButton("Next");
        nextButton.setEnabled(false);
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    onNextButtonClicked();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        nextButton.setPreferredSize(new Dimension(750, 25));
        add(nextButton);

        setTitle("License Plate Reader");
        setVisible(true);


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();
        setVisible(true);
    }

    private void onGetImageClick() throws IOException, TesseractException {
        page = 0;
        knnString = "";
        int returnVal = fileChooser.showOpenDialog(GUI.this);
        imageFile = fileChooser.getSelectedFile();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (imageFile != null) {
                
                int LOW_THRESHOLD = 70;
                int HIGH_THRESHOLD = 170;               
                try {
                    OUTER:for (int i = LOW_THRESHOLD; i < HIGH_THRESHOLD; i = i + 10) {
                        for (int j = 0; j <= 1; j++) {
                            for (int k = 3; k < 12; k = k + 2) {
                                imageData = imageProcessor.processLargeImage(getImageByFile(imageFile), i, j, k);
                                if (imageData.featureVectors != null && imageData.featureVectors.size() == 6)
                                    break OUTER;
                            }
                        }
                    }

                    //Use KNN to read the plate.
                    for (Map.Entry < Double, ArrayList < String >> vector: imageData.featureVectors.entrySet()) {
                        knnString += glyphReader.getGlyph(vector.getValue());
                    }
        
                    //Use Tesseract to read the plate.
                    String tessString = glyphReader.readPlateWithTess(imageData.glyphs);
                    knnPlate.setText(knnString);  
                    tesseractPlate.setText(tessString);   
                    nextButton.setEnabled(true);
    
                } catch (Exception e) {
                    tesseractPlate.setText("Plate not found");
                    knnPlate.setText("Plate not found");
                }
                
                imagePanel.addImage(imageData.originalImage);
                imagePanel.repaint();
            }
        }
    }
    
    private void onNextButtonClicked() throws IOException {
        page++;
        
        switch (page % 10) {
        case 0:
            imagePanel.addImage(imageData.originalImage);
            repaint();
            break;
        case 1:
            imagePanel.addImage(imageData.blur);
            imagePanel.repaint();
            break;
        case 2:
            imagePanel.addImage(imageData.largeEdges);
            imagePanel.repaint();
            break;
        case 3:
            imagePanel.addImage(imageData.allLargeSegments);
            imagePanel.repaint();
            break;
        case 4:
            imagePanel.addImage(imageData.largeSegment);
            imagePanel.repaint();
            break;
        case 5:
            imagePanel.addImage(imageData.firstCrop);
            imagePanel.repaint();
            break;
        case 6:
            imagePanel.addImage(imageData.sobel);
            imagePanel.repaint();
            break;
        case 7:     
            ChartPanel chartPanel = new ChartPanel(imageData.verticalProjection);
            chartPanel.setPreferredSize(new Dimension(200, 350));
            chartPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            imagePanel.add(chartPanel);
            imagePanel.validate();
            break;
        case 8:
            imagePanel.removeAll();
            imagePanel.revalidate();
            imagePanel.addImage(imageData.glyphEdges);
            imagePanel.repaint();
            break;
        case 9:
            imagePanel.addImage(imageData.segmentedGlyphs);
            imagePanel.repaint();
            break;
        }
    }
       
    private Mat getImageByFile(File file) throws IOException {
        image = ImageIO.read(file);
        return imagePanel.imageToMat(image);
    }
}