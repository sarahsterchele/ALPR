package tess4j.example;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JPanel;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

@SuppressWarnings("serial")
public class ImagePanel extends JPanel {
    private BufferedImage image;

    public void addImage(Mat m) {
        int type = m.channels() > 1 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];

        m.get(0, 0, b);
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        System.arraycopy(b, 0, targetPixels, 0, b.length);

        this.image = image;
    }
    
    public void addChart(BufferedImage image) {
        this.image = image;
    }

    public Mat imageToMat(BufferedImage b) {
        Mat m = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) b.getRaster().getDataBuffer()).getData();

        m.put(0, 0, data);

        return m;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, this);
        }
    }
}