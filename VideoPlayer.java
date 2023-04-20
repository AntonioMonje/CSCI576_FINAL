import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.awt.image.BufferedImage;

import javax.imageio.stream.FileImageInputStream;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.table.DefaultTableModel;


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
    private static byte[] data = readRGBFile("./InputVideo.rgb", currentFrame);

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
            default:
                scenePercentage = 0;
                break;
            // Add more cases if needed
        }
    
        switch (shot) {
            case 1:
                shotPercentage = 0.2;
                break;
            default:
                scenePercentage = 0;
                break;
            // Add more cases if needed
        }
    
        switch (subshot) {
            case 1:
                subshotPercentage = 0.2;
                break;
            default:
                scenePercentage = 0;
                break;
            // Add more cases if needed
        }
    
        int sceneFrame = (int) (numFrames * scenePercentage);
        int shotFrame = (int) (sceneFrame * shotPercentage);
        int subshotFrame = (int) (shotFrame * subshotPercentage);
    
        currentFrame = sceneFrame + shotFrame + subshotFrame;
    }

    public static DefaultTableModel extractVideoMetaData(String filePath, int currentFrame)
    {
        //read the video frames
       byte[] data = readRGBFile(filePath, currentFrame);

        //create table model to store data
        DefaultTableModel tableModel = new DefaultTableModel();
        //add columns
        //tableModel.addColumn("Movie");
        tableModel.addColumn("Scene");
        tableModel.addColumn("Shot");
        tableModel.addColumn("Subshot");

        int currentSceneValue = 0;
        int currentShotValue = 0;
        int currentSubshotValue = 0;
        
        String scene = "";
        String shot = "";
        String subshot = "";

        //if(tableModel.getRowCount() == 0){
        //    tableModel.addRow(new Object[]{movie,"","",""});
        //}

        String currentScene = "";
        String currentShot = "";
        String currentSubshot = "";

        for(int i = 0; i < data.length; i++){
            scene = extractScene(data[i], currentSceneValue);
            shot = extractShot(data[i], currentShotValue);
            subshot = extractSubshot(data[i],currentSubshotValue);
            
            //check for new scene
            if(!scene.equals(currentScene)){
                tableModel.addRow(new Object[]{"Scene " + scene, "", ""});
                currentScene = scene;
                currentShot = "";
                currentSubshot = "";
            }

            //check for new shot
            if(!shot.equals(currentShot)){
                tableModel.addRow(new Object[]{"", "shot " + shot, ""});
                currentShot = shot;
                currentSubshot = "";
            }

             //check for new subshot
             if(!subshot.equals(currentSubshot)){
                tableModel.addRow(new Object[]{"", "", "subshot " + subshot});
                currentSubshot = subshot;
            }
            
            //tableModel.addRow(new Object[]{scene, shot, subshot});
        }
        return tableModel;
    }
   
    

    private static String extractScene(byte videoData, int currentSceneValue){
        int sceneValue = (videoData & 0xC0) >> 6;
        currentSceneValue += sceneValue + 1;
        return String.valueOf(currentSceneValue);
    }

    private static String extractShot(byte videoData, int currentShotValue){
        int shotValue = (videoData & 0x38) >> 3;
        currentShotValue += shotValue + 1;
        return String.valueOf(currentShotValue);
    }

    private static String extractSubshot(byte videoData, int currentSubshotValue){
        int subshotValue = videoData & 0x07;
        currentSubshotValue += subshotValue + 1;
        return String.valueOf(currentSubshotValue);
    }

    private static byte[] readRGBFile(String filePath, int currentFrame){
        int width = 480;
        int height = 270;
        int fps = 30;
        int numFrames = 8682;
        File file = new File(filePath);
        byte[] data = null;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(width * height * 3);
            long frameStartTime = System.currentTimeMillis();
            long frameDuration = 1000 / fps;
           
            //Set position based on current frame
            if(currentFrame >=0 && currentFrame < numFrames){
                int numBytesPerFrame = width * height * 3;
                channel.position((long) currentFrame * numBytesPerFrame);
            }
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            byte[] imageData = new byte[buffer.remaining()];
            buffer.get(imageData);
            data = imageData;


        } catch(IOException e){
            e.printStackTrace();
        }
        return data;
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
        frame.add(tablePanel, BorderLayout.WEST);

/*
        // Sample data for the table of contents
        Object[][] data = {
            {"Scene 1", "Shot 1", "Subshot 1"},
            {"Scene 1", "Shot 2", ""},
            {"Scene 1", "Shot 3", ""},
            {"Scene 2", "Shot 1", ""},
            {"Scene 2", "Shot 2", ""},
            {"Scene 2", "Shot 3", "Subshot 1"},
            {"Scene 3", "Shot 1", ""},
            {"Scene 3", "Shot 2", "Subshot 1"},
            {"Scene 3", "Shot 3", ""},
        };

        // Column names for the table
        String[] columnNames = {"Scene", "Shot", "Subshot"};


        // Create a JTable with the sample data and column names
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        JTable tableOfContents = new JTable(tableModel);
*/

         // Create a JTable with the sample data and column names
         DefaultTableModel tableModel = extractVideoMetaData("./InputVideo.rgb", currentFrame);
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
                    int scene = Integer.parseInt(tableOfContents.getValueAt(row, 0).toString().split(" ")[1]);
                    int shot = Integer.parseInt(tableOfContents.getValueAt(row, 1).toString().split(" ")[1]);
                    int subshot = Integer.parseInt(tableOfContents.getValueAt(row, 2).toString().split(" ")[1]);
                    
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

                 //calculate the identation level based on the row
                 int indentationlevel = 0;
                 if(row > 0) {
                     String previousScene = tableOfContents.getValueAt(row-1, 0).toString();
                     String currentScene = tableOfContents.getValueAt(row, 0).toString();
                     if(!currentScene.equals(previousScene)){
                         indentationlevel = 0;
                     } else {
                         indentationlevel = tableOfContents.getValueAt(row-1, column).toString().split("\\s+").length;
                     }
                 }
 
                 //add indentation based on the indentation level
                 String indentation = "";
                 for(int i = 0; i < indentationlevel; i++){
                     indentation += "    "; // 1 tab for each level
                 }
                 setValue(indentation + value.toString());

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
        tablePanel.add(tableScrollPane, BorderLayout.WEST);
        //add the table to the frame
        frame.add(tableScrollPane, BorderLayout.WEST);
        frame.pack();
        frame.setVisible(true);
        


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


        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pauseButton.doClick();
                jumpToFrame(0,0,0, 0);
            }
        });
        controlPanel.add(stopButton);

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