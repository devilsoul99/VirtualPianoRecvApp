import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.org.apache.bcel.internal.generic.NEW;

/*
 * This application is used to receive real-time image and detect the motion of the finger,
 * then produce corresponding piano sound for output.
 */

public class Core {
	private static final boolean debugMode = false;
	/*
	 * Constant declarations of networking
	 */
	private static final int DEFAULT_PORT = 55835,
							 FRAME_SIZE = 230400,
							 PACKET_SIZE = 1000,
							 BUFFER_SIZE = 3;
	/*
	 * Global variable declarations
	 */
	private static UserInterface GUI;
	private static ImageProcessor GPU;
	private static Thread receiver, graber;
	private static boolean isServerSocketCreated;
	private static ServerSocket serverSocket;
	public static Object lock = new Object();
	public static byte[][] recvFrame = new byte[BUFFER_SIZE][FRAME_SIZE];
	public static byte[] receiverBuffer = new byte[FRAME_SIZE];
	public static byte[] graberBuffer = new byte[FRAME_SIZE];
	public static int bufferTail = 0;
	public static int recvPacketCount = 0,
		    		  recvPacketError = 0,
		    		  recvFrameCount = 0;
	
	public static class ButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent obj) {
			if(!isServerSocketCreated){
				GUI.addLog("Command ignored, server socket not created.");
				return;
			}
			String command = obj.getActionCommand();
			if(command.equals("Base Lock")){
				if(GPU.baseLock()){
					GUI.addLog("Base lock complete, start to detect motion.");
				}else{
					GUI.addLog("Base lock failed, adjust the picture or camera then retry.");
				}
			}else if(command.equals("White Balance Switch")){
				if(GPU.switchWhiteBalance()){
					GUI.addLog("White balance ON");
				}else{
					GUI.addLog("White balance OFF");
				}
			}
			
		}
	}
	
	private static void motionHandler(String m){
		if(m.equals("NOT_BASE_LOCKED")){
			if(debugMode){
				GUI.addLog("Base lock process not completed.");
				return;
			}
		}else if(m.equals("TIP_NOT_FOUND")){
			if(debugMode){
				GUI.addLog("Cannot find a tip point on image.");
				return;
			}
		}else if(m.equals("TIP_NOT_ON_KEY")){
			if(debugMode){
				GUI.addLog("Found a tip point but not on a key.");
				return;
			}
		}else if(m.equals("NOT PRESSED")){
			if(debugMode){
				GUI.addLog("Found a tip point but it is not pressed.");
				return;
			}
		}else if(m.equals("PRESSED_IS_PLAYING")){
			if(debugMode){
				GUI.addLog("The pressing key is now playing in progress.");
				return;
			}
		}else{
			if(debugMode){
				GUI.addLog("Pressed Key: " + m);
			}
			GUI.playSound(m);
			return;
		}
	}
	
	private static void onFrameReceived(byte[] frame, int frameSeq){
		/*
		 * This function is called every time when program received a
		 * new frame of image, we import it to the ImageProcessor class.
		 */
		GUI.frameRateUpdate();
		Date framePreocessTimeStart = new Date();				
		
		if(!GPU.importFrame(frame)){
			GUI.addLog("Failed to convert YV12 image on frame " + frameSeq + ".");
			return;
		}
		/*
		 * After the original image, we do a gray scale conversion for later use.
		 */
		GPU.deepCopy(0, 1);
		/*
		 * Call GPU to detect motion if base lock process is finished,
		 * the function return the following:
		 * 		NOT_BASE_LOCKED		-> The base lock process is not completed.
		 * 		TIP_NOT_FOUND		-> Cannot find a tip point on image 3.
		 * 		TIP_NOT_ON_KEY		-> Found a tip point but not on a key.
		 * 		NOT PRESSED			-> Found a tip point but it is not pressed.
		 * 		PRESSED_IS_PLAYING	-> The pressing key is now playing in progress.
		 * 		...					-> else, it return a string which is the key index.
		 */
		motionHandler(GPU.detectMotion());
		/*
		 * Update image processing time in millisecond.
		 */
		Date framePreocessTimeFin = new Date();
		GUI.processTimeUpdate((framePreocessTimeFin.getTime()-framePreocessTimeStart.getTime()));
		/*
		 * Show every result to the GUI.
		 */
		for(int i = 0; i < GPU.getImageBufferCount(); i++){
			GUI.showImage(GPU.getImage(i), i);
		}
		return;
	}

	private static class Graber implements Runnable {

		public void run() {
			while(isServerSocketCreated){
				System.out.println("GGG");
				/*
				 * Lookup buffer, if it's not empty,
				 * grab the oldest frame out.
				 */
				if(bufferTail > 0){
					graberBuffer = recvFrame[0].clone();
					synchronized (lock) {
						for(int i = 0; i < bufferTail - 1; i++){
							recvFrame[i] = recvFrame[i + 1].clone();
						}
						bufferTail--;
					}
					onFrameReceived(graberBuffer,recvFrameCount);
					
				}else{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						
					}
					continue;
					
				}
				
			}
		}
	}
	
	private static class PacketReceiver implements Runnable {
		public void run(){
			try {
				while(isServerSocketCreated){
					System.out.println("RRR");
					Socket sc = serverSocket.accept();
					InputStream inputStream = sc.getInputStream();
					if(recvPacketCount * PACKET_SIZE > FRAME_SIZE){
						recvPacketError++;
						recvPacketCount = 0;
					}
					
					int factByte = -1;
					try{
						factByte = inputStream.read(receiverBuffer, recvPacketCount * PACKET_SIZE, PACKET_SIZE);
					}catch (ArrayIndexOutOfBoundsException e) {
						factByte = inputStream.read(receiverBuffer, recvPacketCount * PACKET_SIZE, FRAME_SIZE - recvPacketCount * PACKET_SIZE);
						System.out.println(recvPacketCount + ", " +bufferTail);
					}
					if(factByte != PACKET_SIZE){
						if(recvPacketCount == FRAME_SIZE / PACKET_SIZE){
							/*
							 * Success to receive a whole frame.
							 */
							recvFrameCount++;
							recvPacketCount = 0;
							
							if(bufferTail == BUFFER_SIZE){
								/*
								 * Buffer is full, pop the oldest out.
								 */
								synchronized (lock) {
									for(int i = 0; i < BUFFER_SIZE - 1; i++){
										recvFrame[i] = recvFrame[i + 1].clone();
									}
									bufferTail--;
								}
							}
							/*
							 * Load up the newest one.
							 */
							synchronized (lock) {
								recvFrame[bufferTail] = receiverBuffer;
								bufferTail++;
							}
						}else{
							recvPacketError++;
							recvPacketCount = 0;
						}
					}else{
						recvPacketCount++;
					}
				}
			} catch (IOException e) {
				GUI.addLog("Accept Error!");
			}
		}
	}
	
	private static ServerSocket createSocket(){
		isServerSocketCreated = false;
		ServerSocket serverSock = null;
		try {
			serverSock = new ServerSocket(DEFAULT_PORT);
		} catch (IOException e) {
			GUI.addLog("Socket creation error, maybe another one is running?");
			return null;
		}
		GUI.addLog("Socket creation complete.");
		isServerSocketCreated = true;
		return serverSock;
	}
	
	
	private static void selfAddressDetect(ServerSocket serSock){
		/*
		 * Find the local IP on which the server runs,
		 * Then output to the log.
		 */
		try {
			GUI.addLog("Local IP:"+InetAddress.getLocalHost().getHostAddress()+" Local Port: "+serSock.getLocalPort());
		} catch (UnknownHostException e) {
			GUI.addLog("Unable to obtain local IP.");
		}
		return;
	}
	
	public static void main(String[] args) {
		ButtonListener listener = new ButtonListener();
		/*
		 * First, we create a GUI for the user using another implemented class,
		 * It will handle all the output functions.
		 */
		GUI = new UserInterface();
		GUI.loadElements();
		GUI.setMainVisible(true);
		GUI.importListener(listener);
		/*
		 * Initializing socket, then obtain the local IP and port,
		 * this step is required for android application settings.
		 */
		serverSocket = createSocket();
		if(isServerSocketCreated){
			selfAddressDetect(serverSocket);
			GUI.addLog("Start Listening.");
		}
		/*
		 * Ready to receive data from mobile device,
		 * Instantiate a ImageProcessor.
		 */
		GPU = new ImageProcessor();
		receiver = new Thread(new PacketReceiver());
		graber = new Thread(new Graber());
		if(isServerSocketCreated){
			/*ExecutorService executorService = Executors.newCachedThreadPool();   
			executorService.execute(graber);
			executorService.execute(receiver);*/
			graber.start();
			receiver.start();
		}
		

	}

}
