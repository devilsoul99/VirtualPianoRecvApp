import javax.swing.*;
import java.util.*;

public class UserInterface {
	
	/*
	 *  Constant declarations
	 */
	private final int	WINDOW_WIDTH = 1285,
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
	private Date orgTimer;
	
    public UserInterface(){
		mainFrame = new JFrame();
		mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setTitle(APP_TITLE);
		mainFrame.setLayout(null);
		mainFrame.setResizable(false);
		orgTimer = new Date();
	}
	
    private String getTimeElapsed(){
    	Date curTimer = new Date();
    	long elapsedMS = curTimer.getTime() - orgTimer.getTime();
    	long min = elapsedMS/1000/60,
    		 sec = elapsedMS/1000%60,
    		 mSec = elapsedMS%1000;
    	String minStr = min<10 ? "0"+Long.toString(min):Long.toString(min);
    	String secStr = sec<10 ? "0"+Long.toString(sec):Long.toString(sec);
    	String mSecStr;
    	if(mSec < 10){
    		mSecStr = "00"+Long.toString(mSec);
    	}else if(mSec < 100){
    		mSecStr = "0"+Long.toString(mSec);
    	}else{
    		mSecStr = Long.toString(mSec);
    	}
    	return "[" + minStr + "." + secStr + "." + mSecStr + "]";
    }
    
 	public void loadElements(){
		/*
		 * Declaring all the elements
		 */
		txtLabel = new JLabel[5];
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
			txtLabel[i].setSize(300, 20);
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
		this.addLog("Runtime log:");
		msgPane.setSize(300,500);
		msgPane.setLocation(975, 110);
		mainFrame.add(msgPane);
		
		btn.setText("Base Lock");
		btn.setSize(150,50);
		btn.setLocation(975, 615);
		mainFrame.add(btn);
		
		return;
	}
	
	public void addLog(String newLog){
		msgLog.append(getTimeElapsed() + newLog+"\n");
		return;
	}
	
	public void trafficUpdate(int pCount, int pError, int fCount){
		return;
	}
	
	public void setMainVisible(boolean v){
		mainFrame.setVisible(v);
		return;
	}
}
