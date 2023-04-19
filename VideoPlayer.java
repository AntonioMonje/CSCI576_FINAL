import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import java.util.concurrent.Semaphore;
import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.Cursor;


public class VideoPlayer {
    private final static int BUFFER_SIZE = 1024 * 1024;
    private final static int CHUNK_SIZE = 5880;
    private static File soundFile;
    private static AudioInputStream audioStream;
    private static AudioFormat audioFormat;
    private static SourceDataLine sourceLine;
    private static Semaphore playSemaphore = new Semaphore(1);

    private static boolean isPaused = false;
    private static int currentFrame = 0;

    // Add a method to jump to a specific frame
    public static void jumpToFrame(int scene, int shot, int subshot, int numFrames) {
        double scenePercentage = 0;
        double shotPercentage = 0;
        double subshotPercentage = 0;
    
        switch (scene) {
            case 1:
                scenePercentage = 0.2;
                break;
            case 2:
                scenePercentage = 0.4;
                break;
            case 3:
                scenePercentage = 0.6;
                break;
            // Add more cases if needed
        }
    
        switch (shot) {
            case 1:
                shotPercentage = 0.2;
                break;
            // Add more cases if needed
        }
    
        switch (subshot) {
            case 1:
                subshotPercentage = 0.2;
                break;
            // Add more cases if needed
        }
    
        int sceneFrame = (int) (numFrames * scenePercentage);
        int shotFrame = (int) (sceneFrame * shotPercentage);
        int subshotFrame = (int) (shotFrame * subshotPercentage);
    
        currentFrame = sceneFrame + shotFrame + subshotFrame;
    }



    public static void main(String[] args) {
        File file = new File("./InputVideo.rgb");
        int width = 480;
        int height = 270;
        int fps = 30;
        int numFrames = 8682;

        JFrame frame = new JFrame("Video Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(width + 500, height + 200));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(width, height));
        frame.add(label, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(width, 50));
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Add a separate panel for the table of contents
        JPanel tablePanel = new JPanel();
        frame.add(tablePanel, BorderLayout.EAST);

        // Sample data for the table of contents
        Object[][] data = {
            {"Scene 1", "Shot 1", "Subshot 1"},
            {"Scene 2", "Shot 1", "Subshot 1"},
            {"Scene 3", "Shot 1", "Subshot 1"},
        };

        // Column names for the table
        String[] columnNames = {"Scene", "Shot", "Subshot"};

        // Create a JTable with the sample data and column names
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        JTable tableOfContents = new JTable(tableModel);

        

        tableOfContents.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tableOfContents.rowAtPoint(e.getPoint());
                int col = tableOfContents.columnAtPoint(e.getPoint());
        
                // Check if the clicked cell is in the "Scene" column
                if (col == 0) {
                    // Calculate the frame number for the middle of the movie
            
                    // Get the values for the clicked row
                    int scene = Integer.parseInt(data[row][0].toString().split(" ")[1]);
                    int shot = Integer.parseInt(data[row][1].toString().split(" ")[1]);
                    int subshot = Integer.parseInt(data[row][2].toString().split(" ")[1]);
                    
                    // Pause the video
                    isPaused = true;
                    sourceLine.flush();
        
                    // Jump to the middle of the movie
                    jumpToFrame(scene, shot, subshot, numFrames);
        
                    // Resume playback from the new index
                    isPaused = false;
                    playSemaphore.release();
                }
            }
        });

        // Set the cursor for the scene column to the hand cursor
        tableOfContents.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 0) {
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    c.setCursor(Cursor.getDefaultCursor());
                }
                return c;
            }
        });

        // Add the JTable to a JScrollPane for easy navigation
        JScrollPane tableScrollPane = new JScrollPane(tableOfContents);
        tableScrollPane.setPreferredSize(new Dimension(300, 150));

        // Add the JScrollPane to the table panel
        tablePanel.add(tableScrollPane);


        JButton playButton = new JButton("Play");
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused;
                if (isPaused) {
                    sourceLine.flush();
                } else {
                    playSemaphore.release();
                }
            }
        });
        controlPanel.add(playButton);

        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused;
                if (isPaused) {
                    sourceLine.flush();
                } else {
                    playSemaphore.release();
                }
            }
        });
        controlPanel.add(pauseButton);


        String strFilename = "./InputAudio.wav";

        try {
            soundFile = new File(strFilename);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            audioStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

        audioFormat = audioStream.getFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        sourceLine.start();

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(width * height * 3);
            long frameStartTime = System.currentTimeMillis();
            long frameDuration = 1000 / fps;

            for (; currentFrame < numFrames; currentFrame++) {
                try {
                    playSemaphore.acquire();
                    if (isPaused) {
                        playSemaphore.release();
                        currentFrame--;
                        continue;
                    }
                    playSemaphore.release();
            
                    // Set the position of the file channel based on currentFrame
                    channel.position((long) currentFrame * width * height * 3);
            
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    byte[] imageData = buffer.array();
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    int idx = 0;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = imageData[idx++] & 0xFF;
                            int g = imageData[idx++] & 0xFF;
                            int b = imageData[idx++] & 0xFF;
                            img.setRGB(x, y, (r << 16) | (g << 8) | b);
                        }
                    }
            
                    label.setIcon(new ImageIcon(img));
            
                    if (!isPaused) {
                        int nBytesRead = 0;
                        byte[] abData = new byte[BUFFER_SIZE];
                        nBytesRead = audioStream.read(abData, 0, CHUNK_SIZE);
                        if (nBytesRead >= 0) {
                            int nBytesWritten = sourceLine.write(abData, 0, nBytesRead);
                        }
                    }
            
                    long elapsedTime = System.currentTimeMillis() - frameStartTime;
                    long remainingTime = frameDuration - elapsedTime;
            
                    if (remainingTime > 0) {
                        Thread.sleep(remainingTime);
                    }
            
                    frameStartTime = System.currentTimeMillis();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            sourceLine.drain();
            sourceLine.close();
            audioStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   
}