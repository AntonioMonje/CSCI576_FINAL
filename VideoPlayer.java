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

    public static void print(double [] array, int numFrames, int interval) {
    	for (int i = 0; i < (int) ((int) (numFrames/30) + 1)/interval + 1; i++) {
    		int j = i * interval;
        	System.out.println("Time: "+ (int) j/60 + ":"+j%60+ " Value: "+array[i]);
        }
    }
    
    public static double [] max(double [] array, int count, int interval) {
    	double newArray [] = new double [((int) count/interval) + 2];
    	int index = 0;
    	double max = 0;
    	for (int i = 0; i < count; i++) {
    		if (array[i] > max) max = array[i];
    		//newArray[index] = newArray[index] + array[i];
    		if (i % interval == interval - 1) {
        		newArray[index] = max;
        		index++;
        		max = 0;
        	}
    	}
    	return newArray;
    }
    
    public static double [] average(double [] array, int count, int interval) {
    	double newArray [] = new double [((int) count/interval) + 2];
    	int index = 0;
    	for (int i = 0; i < count; i++) {
    		//System.out.println(array[i]);
    		newArray[index] = newArray[index] + array[i];
    		if (i % interval == interval - 1) {
        		newArray[index] = newArray[index]/interval;
        		index++;
        	}
    	}
    	return newArray;
    }
    
    public static double sd(double [] array, int count) {
    	double average = average(array, count, count)[0];
    	double sd = 0;
    	for (int i = 0; i < count; i++) {
    		sd = sd + Math.pow((array[i] - average), 2);
    	}
    	sd = Math.sqrt(sd/count);
    	//System.out.println("average: "+ average);
    	//System.out.println("sd: "+ sd);
    	return sd;
    }
    
    public static int [] process(File file, int width, int height, int fps, int numFrames, JFrame frame) {
    	int nBytesRead = 0;
        byte[] abData = new byte[BUFFER_SIZE];
        int index = 0;
        
        try {
        	RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(width * height * 3);
            int previousR [][] = new int [width][height];
            int previousG [][] = new int [width][height];
            int previousB [][] = new int [width][height];
            double differences [] = new double [numFrames];
            double seconds [] = new double [(int) (numFrames/30) + 1];
            double shots [] = new double [(int) ((int) (numFrames/30) + 1)/3 + 1];
            double scenes [] = new double [(int) ((int) (numFrames/30) + 1)/10 + 1];
            double jumps [] = new double [(int) (numFrames/30) + 2];
            double scenejumps [] = new double [(int) ((int) (numFrames/30) + 1)/10 + 1];
            int processed [] = new int [(int) (numFrames/30)];

            //initialize seconds array with zeros
            for(int i = 0; i < seconds.length; i++)
            {
                seconds[i] = 0.0;
            }
            
            for (int i = 0; i < numFrames; i++) {
            	buffer.clear();
                channel.read(buffer);
                buffer.rewind();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int b = buffer.get() & 0xff;
                        differences[i] = differences[i] + (Math.abs(r - previousR [x][y]) 
                        		+ Math.abs(g - previousG [x][y]) + Math.abs(b - previousB [x][y]))/3;
                        previousR [x][y] = r;
                        previousG [x][y] = g;
                        previousB [x][y] = b;
                    }
                }
                differences[i] = differences[i]/(width * height);
                frame.validate();
                frame.repaint();
            }
            //seconds = average(differences, numFrames, fps);
            seconds = max(differences, numFrames, fps);
            for (int i = 0; i < (int) (numFrames/30) ; i++) {
            	if (i != 0) {
            		jumps[i] = Math.abs(seconds[i] - seconds[i - 1]);
            	}
            	else jumps[i] = seconds[i];
            }
            shots = average(seconds, numFrames/30, 3);
            scenes = average(seconds, numFrames/30, 10);
            scenejumps = average(jumps, numFrames/30, 10);
           
            double sd = sd(seconds, (int) ((int) (numFrames/30) + 1)/1 - 1);
            double average = average(seconds, (int) ((int) (numFrames/30) + 1)/1 - 1, 
            		(int) ((int) (numFrames/30) + 1)/1 - 1)[0];
            System.out.println("Average: " + average);
            System.out.println("Standard Deviation: " + sd);
           
            processed[0] = 3; //First second should always be new scene
            for (int i = 1; i < (int) (numFrames/30) ; i++) {
            	if (seconds[i] > average + 2*sd) processed[i] = 3;
            	else if (seconds[i] > average + sd - 1) processed[i] = 2;
            	else if (seconds[i] > average) processed[i] = 1;
            	else processed[i] = 0;
            }
            channel.close();
            raf.close();
            return processed;
        } catch (IOException e) {
            e.printStackTrace();
        }
    	return null;
    }
    

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
        int width = 480;
        int height = 270;
        int fps = 30;
        int numFrames = 8682;
        //create table model to store data
        DefaultTableModel tableModel = new DefaultTableModel();
        //add columns
        tableModel.addColumn("Scene");
        tableModel.addColumn("Shot");
        tableModel.addColumn("Subshot");
        

        JFrame frame = new JFrame();
        int[] processed = process(new File(filePath), width, height, fps, numFrames, frame);
        int currentScene = -1;
        int currentShot = -1;
        int currentSubshot = -1;
        for(int i = 0; i < processed.length; i++){
            if(processed[i] == 3){
                //new scene
                currentScene++;
                currentShot = -1;
                currentSubshot = -1;
                tableModel.addRow(new Object[]{"Scene " + (currentScene + 1), "", "" });
            }else if (processed[i] == 2){
                currentShot++;
                currentSubshot = -1;
                tableModel.addRow(new Object[]{"", "shot " + (currentShot + 1), "" });

            } else if (processed[i] == 1){
                currentSubshot++;
                tableModel.addRow(new Object[]{"", "", "Subshot " + (currentSubshot + 1)});
            }
        }

        return tableModel;
    }
   



    private static byte[] readRGBFrame(String filePath, int width, int height, int frameNumber){
        byte[] data = new byte[width * height * 3];
        try{
            RandomAccessFile raf = new RandomAccessFile(filePath, "r");
            raf.seek(frameNumber * width * height * 3);
            raf.read(data);
            raf.close();
        }catch(IOException e){
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

        //Contains location of all scenes, shots, and subshots
        int processed [] = new int [(int) (numFrames/30)];

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

         // Create a JTable with the sample data and column names
        DefaultTableModel tableModel = extractVideoMetaData("./InputVideo.rgb", currentFrame);
        JTable tableOfContents = new JTable(tableModel);

        processed = process(file, width, height, fps, numFrames, frame);
        for (int i = 0; i < (int) (numFrames/fps) ; i++) {
        	System.out.print("Time: "+ (int) i/60 + ":"+i%60+ ": ");
        	if (processed[i] == 1) System.out.println("Subshot");
        	else if (processed[i] == 2) System.out.println("Shot");
        	else if (processed[i] == 3) System.out.println("Scene");
        	else System.out.println();
        }

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