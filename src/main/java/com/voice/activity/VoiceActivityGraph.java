package com.voice.activity;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * The VoiceActivityGraph class visualizes the microphone's voice activity.
 *
 * This class provides a visual representation of voice activity detected by the
 * system's microphone.
 *
 * @author Ashutosh Pandey
 */
public class VoiceActivityGraph extends JPanel {

    private static final int GRAPH_HEIGHT = 200;
    private static final int VAD_HEIGHT = 50;
    private static final int BUFFER_SIZE = 1280;
    private static final int SILENCE_THRESHOLD = 10000;

    private byte[] audioBuffer;
    private int rmsValue;
    private boolean isSpeechDetected;
    private List<Integer> vadResults;

    public VoiceActivityGraph() {
        audioBuffer = new byte[BUFFER_SIZE];
        isSpeechDetected = false;
        vadResults = new ArrayList<>();

        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            Timer timer = new Timer(50, e -> {
                line.read(audioBuffer, 0, BUFFER_SIZE);
                calculateRMSValue();
                performVAD();
                repaint();
            });
            timer.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void calculateRMSValue() {
        short[] shortBuffer = new short[BUFFER_SIZE / 2];
        ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);

        long sumSquared = 0;
        for (short sample : shortBuffer) {
            sumSquared += sample * sample;
        }

        rmsValue = (int) Math.sqrt(sumSquared / shortBuffer.length);
    }

    private void performVAD() {
        isSpeechDetected = rmsValue > SILENCE_THRESHOLD;
        if(vadResults.size() >= 600)
            vadResults.clear();
        vadResults.add(isSpeechDetected ? 1 : 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        int xOffset = 0;
        int graphYOffset = getHeight() - GRAPH_HEIGHT - VAD_HEIGHT;
        int vadYOffset = getHeight() - VAD_HEIGHT;

        // Draw RMS value line
        g.setColor(Color.RED);
        int rmsY = graphYOffset + (VAD_HEIGHT - rmsValue / 256);
        g.drawLine(0, rmsY, getWidth(), rmsY);

        // Draw VAD status
        String vadStatus = isSpeechDetected ? "Speech Detected" : "Silence Detected";
        g.setColor(Color.BLUE);
        g.drawString(vadStatus, 10, getHeight() - VAD_HEIGHT + 30);

        // Draw waveform
        g.setColor(Color.BLACK);
        short[] shortBuffer = new short[BUFFER_SIZE / 2];

        ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);
        for (int i = 1; i < shortBuffer.length; i++) {
            int x1 = xOffset + (i - 1);
            int y1 = graphYOffset + (VAD_HEIGHT - shortBuffer[i - 1] / 256);
            int x2 = xOffset + i;
            int y2 = graphYOffset + (VAD_HEIGHT - shortBuffer[i] / 256);
            g.drawLine(x1, y1, x2, y2);
        }

        // Draw VAD graph
        drawVADGraph(g, vadResults);
    }

    private void drawVADGraph(Graphics g, List<Integer> results) {
        int xOffset = 0;
        int vadGraphYOffset = getHeight() - VAD_HEIGHT;

        g.setColor(Color.GREEN);
        for (int i = 0; i < results.size(); i++) {
            int x = xOffset + i;
            int y = vadGraphYOffset - results.get(i) * VAD_HEIGHT;
            g.drawLine(x, vadGraphYOffset, x, y);
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("VAD - Voice Activity Detector");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 460);
            frame.add(new VoiceActivityGraph());

            // Center the window on the screen
            frame.setLocationRelativeTo(null);
            // Disable window maximize button
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
