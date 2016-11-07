/*
 * This application is used to receive real-time image and detect the motion of the finger,
 * then produce corresponding piano sound for output.
 */

// Import libraries
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class VirtualPianoRecv {
	/*
	 * This class is the main class of the program,
	 * it handles the network-transporting part.
	 */
	
	/*
	 * Constant declarations of networking
	 */
	private static final int DEFAULT_PORT = 55835,
							 FRAME_SIZE = 230400,
							 PACKET_SIZE = 1000;
	private static final boolean debugMode = false;
	/*
	 * Global variable declarations
	 */
	private static UserInterface GUI;
	private static ImageProcessor GPU;
	private static boolean isServerSocketCreated;
	
	private static DatagramSocket createSocket(){
		/*
		 * Use the predefined arguments to create an listening socket,
		 * This program will act as a server.
		 */
		isServerSocketCreated = false;
		DatagramSocket rSocket = null;
		try {
			rSocket = new DatagramSocket(DEFAULT_PORT);//Assign port manually
		} catch (SocketException e) {
			GUI.addLog("Socket creation error, maybe another one is running?");
			return null;
		}
		GUI.addLog("Socket creation complete.");
		isServerSocketCreated = true;
		return rSocket;
	}
	
	private static void selfAddressDetect(DatagramSocket serSock){
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
	
	public static void main(String[] args) {
		/*
		 * Local Variable declarations
		 */
		int recvPacketCount = 0,
		    recvPacketError = 0,
		    recvFrameCount = 0;
		byte[] recvFrame = new byte[FRAME_SIZE];
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
		DatagramSocket serverSocket = createSocket();
		if(isServerSocketCreated){
			selfAddressDetect(serverSocket);
			GUI.addLog("Start Listening.");
		}
		/*
		 * Ready to receive data from mobile device,
		 * Instantiate a ImageProcessor.
		 */
		GPU = new ImageProcessor();
		
		while(isServerSocketCreated){
			GUI.trafficUpdate(recvPacketCount,recvPacketError,recvFrameCount);
			
			/*
			 * Allocates memory for incoming data.
			 */
			byte[] receiveData = new byte[PACKET_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/*
			 * Try to receive data packet from client.
			 */
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {
				recvPacketError++;
				continue;
			}
			
			/*
			 * Proceed if a packet successfully received,
			 * load the data buffer into frame buffer.
			 */
			try{
            	System.arraycopy(receivePacket.getData(), 0, recvFrame, PACKET_SIZE*recvPacketCount, receivePacket.getLength());
            	recvPacketCount++;
            }catch(ArrayIndexOutOfBoundsException e){
            	/*
            	 * This occur when a packet somehow is lost,
            	 * Discard the whole frame.
            	 */
            	recvPacketCount = 0;
            	recvPacketError++;
            	continue;
            }
			
			if(receivePacket.getLength() != PACKET_SIZE){
				/*
				 * The size of the packet varies if it's the last one of the frame.
				 */
				if(recvPacketCount != FRAME_SIZE / PACKET_SIZE + 1){
					/*
					 * But maybe the packet sequence is messed up.
					 */
					recvPacketCount = 0;
					recvPacketError++;
					continue;
				}
				/*
				 * As long as the code reaches here, we obtain a whole image.
				 */
				recvPacketCount = 0;
				recvFrameCount++;
				onFrameReceived(recvFrame,recvFrameCount);
			}
		}
	}

}
