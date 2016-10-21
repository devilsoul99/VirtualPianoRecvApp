import javax.swing.*;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.*;

public class UserInterface {
	
	/*
	 *  Constant declarations
	 */
	private final int	WINDOW_WIDTH = 1385,
						WINDOW_HEIGHT = 800;
	private final String APP_TITLE = "Virtual Piano PC Client";
	
	/*
	 *  Variable declarations
	 */
	private JFrame mainFrame;
	private JLabel[] txtLabel;
	private JLabel[] imgLabel;
	private JTextArea msgLog;
	private JScrollPane msgPane;
	private JButton btn;
	private Date orgTimer,
				 frameRateTimer;

	
    public UserInterface(){
		mainFrame = new JFrame();
		mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setTitle(APP_TITLE);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		orgTimer = new Date();
	}
    
    public void importListener(ActionListener l){
    	btn.addActionListener(l);
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
    
 	public void loadElements(){
		/*
		 * Declaring all the elements
		 */
		txtLabel = new JLabel[6];
		imgLabel = new JLabel[4];
		msgLog = new JTextArea();
		msgPane = new JScrollPane(msgLog);
		btn = new JButton();
		
		
		/*
		 * Adjust size and location,
		 * then add those into main frame.
		 */
		for(int i = 0; i < txtLabel.length; i++){
			txtLabel[i] = new JLabel();
			txtLabel[i].setSize(400, 20);
			txtLabel[i].setLocation(975, 5+(20*i));
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
		msgPane.setSize(400,500);
		msgPane.setLocation(975, 130);
		mainFrame.add(msgPane);
		
		btn.setText("Base Lock");
		btn.setSize(150,50);
		btn.setLocation(975, 635);
		mainFrame.add(btn);
		
		return;
	}
	
	public void addLog(String newLog){
		msgLog.append(getTimeElapsed() + " " + newLog + "\n");
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
	
	public void showImage(BufferedImage subject, int position){
		if(position >= 0 && position < imgLabel.length && subject != null){
			imgLabel[position].setIcon(new ImageIcon(subject));
		}
		return;
	}
	
	public void setMainVisible(boolean v){
		mainFrame.setVisible(v);
		return;
	}
}
