import javax.imageio.ImageIO;
import javax.swing.*;

import com.sun.org.apache.bcel.internal.generic.NEW;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class UserInterface {
	
	/*
	 *  Constant declarations
	 */
	private static final int	WINDOW_WIDTH = 1385,
								WINDOW_HEIGHT = 800;
	
	private final String APP_TITLE = "Virtual Piano PC Client";
	/*
	 *  Variable declarations
	 */
	private BufferedImage gameImage = null;
	private JFrame mainFrame;
	private JLabel[] txtLabel;
	private JLabel[] arguLabel;
	private JLabel[] to;
	private JLabel game_title, game_totalHit, game_correctHit;
	private JTextField[] arguText;
	private JLabel[] imgLabel;
	private JTextArea msgLog;
	private JScrollPane msgPane;
	private JButton[] btn;
	private Date orgTimer,
				 frameRateTimer;
	private static final Font FONT_S = new Font("Consolas", Font.PLAIN, 12),
							  FONT_M = new Font("Berlin Sans FB", Font.PLAIN, 24);

	
    public UserInterface(){
		mainFrame = new JFrame();
		mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setTitle(APP_TITLE);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		orgTimer = new Date();
	}
    
    public void showGameView(String k){
    	try {
    		gameImage = ImageIO.read(new File("game\\key_" + k + ".jpg"));
    		imgLabel[3].setIcon(new ImageIcon(gameImage.getScaledInstance(imgLabel[3].getWidth(), imgLabel[3].getHeight(),java.awt.Image.SCALE_FAST)));
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}
    } 
    
    public void importListener(ActionListener l){
    	for(int i = 0; i < btn.length; i++){
    		btn[i].addActionListener(l);
    	}
    	return;
    }
	
    private String getTimeElapsed(){
    	Date curTimer = new Date();
    	long elapsedMS = curTimer.getTime() - orgTimer.getTime();
    	
    	int min = (int) elapsedMS/1000/60,
    		sec = (int) elapsedMS/1000%60,
    		mSec = (int) elapsedMS%1000;
    	return "[" + prefixZeros(min,2) + "." + prefixZeros(sec,2) + "." + prefixZeros(mSec,3) + "]";
    }
    
    private String prefixZeros(int subject, int totalDigit){
    	String rv = Integer.toString(subject);
    	while(rv.length()<totalDigit){
    		rv = "0" + rv;
    	}
    	return rv;
    }
    
    private String floatDigit(float subject, int postDigit){
    	String rv;
    	if(!Float.isNaN(subject) && postDigit > 0){
			rv = Float.toString(subject);
			rv = rv.substring(0, rv.indexOf('.') + postDigit + 1);
		}else{
			rv = "0.0";
		}
    	return rv;
    }
    
    public void gameSetTitle(String t){
    	game_title.setText(t);
    }
    
    public void updateGameUI(int total_hit, int correct_hit){
		game_totalHit.setText("Total hit count: " + total_hit);
		game_correctHit.setText("Correct hit count: " + correct_hit);
    }
    
    public void gameModeSwitch(boolean b){
    	if(b){
    		for(int i = 0; i < 3; i++){
    			imgLabel[i].setSize(240, 160);
    			imgLabel[i].setLocation(5+i*245, 5);
    		}
    		imgLabel[3].setSize(960, 320);
			imgLabel[3].setLocation(5, 300);
			imgLabel[3].setVisible(true);
			game_title.setVisible(true);
			game_totalHit.setVisible(true);
			game_correctHit.setVisible(true);
    	}else{
    		for(int i = 0; i < 3; i++){
    			imgLabel[i].setSize(480, 320);
    			imgLabel[i].setLocation(5+(i%2)*485, 5+(i/2)*325);
    			imgLabel[3].setVisible(false);
    		}
    		game_title.setVisible(false);
			game_totalHit.setVisible(false);
			game_correctHit.setVisible(false);
    	}
    }
    
    public void showGameView(BufferedImage g){
    	if(g==null){
    		return;
    	}
    	imgLabel[3].setIcon(new ImageIcon(g.getScaledInstance(imgLabel[3].getWidth(), imgLabel[3].getHeight(),java.awt.Image.SCALE_FAST)));
    }
    
 	public void loadElements(){
		/*
		 * Declaring all the elements
		 */
		txtLabel = new JLabel[6];
		imgLabel = new JLabel[4];
		msgLog = new JTextArea();
		msgPane = new JScrollPane(msgLog);
		btn = new JButton[4];
		/*
		 * Adjust size and location,
		 * then add those into main frame.
		 */
		for(int i = 0; i < txtLabel.length; i++){
			txtLabel[i] = new JLabel();
			txtLabel[i].setSize(400, 20);
			txtLabel[i].setLocation(975, 5+(20*i));
			txtLabel[i].setFont(FONT_S);
			mainFrame.add(txtLabel[i]);
		}
		for(int i = 0; i < imgLabel.length; i++){
			imgLabel[i] = new JLabel();
			imgLabel[i].setSize(480, 320);
			imgLabel[i].setLocation(5+(i%2)*485, 5+(i/2)*325);
			mainFrame.add(imgLabel[i]);
		}
		msgLog.setEditable(false);
		this.addLog("Start runtime log.");
		msgPane.setSize(400,195);
		msgPane.setLocation(975, 130);
		msgLog.setFont(FONT_S);
		mainFrame.add(msgPane);
		
		for(int i = 0; i < btn.length; i++){
			btn[i] = new JButton();
			btn[i].setSize(150,50);
			btn[i].setFont(FONT_S);
			mainFrame.add(btn[i]);
    	}
		btn[0].setText("Base Lock");
		btn[0].setLocation(1070, 690);
		btn[1].setText("White Balance");
		btn[1].setLocation(1225, 690);
		btn[2].setText("Apply Arguments");
		btn[2].setLocation(1225, 635);
		btn[3].setText("Game Mode");
		btn[3].setLocation(1070, 635);
		
		arguLabel = new JLabel[10];
		arguLabel[0] = new JLabel("Red mark Hue (0~360):");
		arguLabel[1] = new JLabel("Green mark Hue (0~360):");
		arguLabel[2] = new JLabel("Blue mark Hue (0~360):");
		arguLabel[3] = new JLabel("Mark saturation minimum (0~1):");
		arguLabel[4] = new JLabel("Mark brightness minimum (0~1):");
		arguLabel[5] = new JLabel("Hand brightness maximum (0~1):");
		arguLabel[6] = new JLabel("Brightness variation threshold (0~1):");
		arguLabel[7] = new JLabel("Shadow mask width:");
		arguLabel[8] = new JLabel("Shadow mask height:");
		arguLabel[9] = new JLabel("Shadow percentage threshold (0~100):");
		for(int i = 0; i < arguLabel.length; i++){
			arguLabel[i].setSize(260, 25);
			arguLabel[i].setLocation(975, 330 + i*30);
			arguLabel[i].setFont(FONT_S);
			mainFrame.add(arguLabel[i]);
    	}
		
		to = new JLabel[3];
		for(int i = 0; i < to.length; i++){
			to[i]= new JLabel("~");
			to[i].setSize(20, 25);
			to[i].setLocation(1305, 330 + i*30);
			to[i].setFont(FONT_S);
			mainFrame.add(to[i]);
    	}
		
		
		arguText = new JTextField[13];
		for(int i = 0; i < arguText.length; i++){
			arguText[i] = new JTextField();
			arguText[i].setSize(50, 25);
			arguText[i].setFont(FONT_S);
			if(i < 6){
				arguText[i].setLocation(1250 + i%2*70, 330 + (i/2)*30);
			}else{
				arguText[i].setLocation(1250, 420 + (i-6)*30);
			}
			mainFrame.add(arguText[i]);
    	}
		
		game_title = new JLabel();
		game_totalHit = new JLabel();
		game_correctHit = new JLabel();
		mainFrame.add(game_title);
		mainFrame.add(game_totalHit);
		mainFrame.add(game_correctHit);
		game_title.setSize(200, 25);
		game_totalHit.setSize(200, 25);
		game_correctHit.setSize(200, 25);
		game_title.setLocation(5, 275);
		game_totalHit.setLocation(205, 275);
		game_correctHit.setLocation(405, 275);
		return;
	}
	
 	public ArgumentSetting getArgumentField(){
		ArgumentSetting rv = new ArgumentSetting();
		rv.th_mark_redMinDegree = Integer.parseInt(arguText[0].getText());
		rv.th_mark_redMaxDegree = Integer.parseInt(arguText[1].getText());
		rv.th_mark_greenMinDegree = Integer.parseInt(arguText[2].getText());
		rv.th_mark_greenMaxDegree = Integer.parseInt(arguText[3].getText());
		rv.th_mark_blueMinDegree = Integer.parseInt(arguText[4].getText());
		rv.th_mark_blueMaxDegree = Integer.parseInt(arguText[5].getText());
		rv.th_press_rectWidth = Integer.parseInt(arguText[10].getText());
		rv.th_press_rectHeight = Integer.parseInt(arguText[11].getText());
		rv.th_mark_minS = Float.parseFloat(arguText[6].getText());
		rv.th_mark_minB = Float.parseFloat(arguText[7].getText());
		rv.th_ls_hand_maxB = Float.parseFloat(arguText[8].getText());
		rv.th_ls_var = Float.parseFloat(arguText[9].getText());
		rv.th_press_shadowPercentage = Double.parseDouble(arguText[12].getText());
		return rv;
 	}
 	
 	public void setArgumentField(ArgumentSetting a){
 		arguText[0].setText(Integer.toString(a.th_mark_redMinDegree));
 		arguText[1].setText(Integer.toString(a.th_mark_redMaxDegree));
 		arguText[2].setText(Integer.toString(a.th_mark_greenMinDegree));
 		arguText[3].setText(Integer.toString(a.th_mark_greenMaxDegree));
 		arguText[4].setText(Integer.toString(a.th_mark_blueMinDegree));
 		arguText[5].setText(Integer.toString(a.th_mark_blueMaxDegree));
 		arguText[6].setText(Float.toString(a.th_mark_minS));
 		arguText[7].setText(Float.toString(a.th_mark_minB));
 		arguText[8].setText(Float.toString(a.th_ls_hand_maxB));
 		arguText[9].setText(Float.toString(a.th_ls_var));
 		arguText[10].setText(Integer.toString(a.th_press_rectWidth));
 		arguText[11].setText(Integer.toString(a.th_press_rectHeight));
 		arguText[12].setText(Double.toString(a.th_press_shadowPercentage));
 	}
 	
	public void addLog(String newLog){
		msgLog.append(getTimeElapsed() + " " + newLog + "\n");
		msgLog.setCaretPosition(msgLog.getDocument().getLength()); 
		return;
	}
	
	public void processTimeUpdate(long pMS){
		txtLabel[5].setText("Processing time: " + pMS + " ms");
		return;
	}
	
	public void trafficUpdate(int pCount, int pError, int fCount){
		/*
		 * Update the network packet info to the txtLabel.
		 * 	
		 */
		float frameLossRate = (float)pError/(float)(fCount+pError)*100;

		txtLabel[0].setText("Packet sequence: " + pCount);
		txtLabel[1].setText("Packet receive error: " + pError);
		txtLabel[2].setText("Frame count: " + fCount);
		txtLabel[3].setText("Frame loss rate: " + floatDigit(frameLossRate,1) + "%");
		return;
	}
	
	public void frameRateUpdate(){
		/*
		 * This function will be called when a complete frame is received.
		 */
		if(frameRateTimer==null){
			frameRateTimer = new Date();
			return;
		}
		Date currentTimer = new Date();
		int frameDelayMsec = (int) (currentTimer.getTime() - frameRateTimer.getTime());
		float instantFrameRate = (float)1 / ((float)frameDelayMsec / (float) 1000);
		txtLabel[4].setText("Frame rate (FPS): " + floatDigit(instantFrameRate,1));
		frameRateTimer.setTime(currentTimer.getTime());;
		return;
	}
	
	public void showImage(BufferedImage[] subject, boolean isBaseLock){
		imgLabel[0].setIcon(new ImageIcon(subject[0].getScaledInstance(imgLabel[0].getWidth(), imgLabel[0].getHeight(),java.awt.Image.SCALE_FAST)));
		if(isBaseLock){
			imgLabel[2].setIcon(new ImageIcon(subject[3].getScaledInstance(imgLabel[2].getWidth(), imgLabel[2].getHeight(),java.awt.Image.SCALE_FAST)));
			imgLabel[1].setIcon(new ImageIcon(subject[2].getScaledInstance(imgLabel[1].getWidth(), imgLabel[1].getHeight(),java.awt.Image.SCALE_FAST)));
		}else{
			imgLabel[1].setIcon(new ImageIcon(subject[2].getScaledInstance(imgLabel[1].getWidth(), imgLabel[1].getHeight(),java.awt.Image.SCALE_FAST)));
		}
		return;
	}
	
	public void setMainVisible(boolean v){
		mainFrame.setVisible(v);
		return;
	}

	public void playSound(String keyIndex){
		InputStream in;
		AudioStream audioStream = null;
		try {
			in = new FileInputStream("soundbank\\Piano" + keyIndex + ".wav");
			audioStream = new AudioStream(in);
			AudioPlayer.player.start(audioStream);
		} catch (FileNotFoundException e) {
			addLog("Sound bank not found, index: " + keyIndex);
		} catch (IOException e) {
			addLog("IO Exception while playing, index: " + keyIndex);
		}
		return;
	}
}
