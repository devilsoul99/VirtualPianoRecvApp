/*
 * This application is used to receive real-time image and detect the motion of the finger,
 * then produce corresponding piano sound for output.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
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
	private static boolean isServerSocketCreated,
						   isGameMode = false,
						   isGameCompleted = true;
	private static String[] trackSequence;
	private static int trackProgress;
	private static int game_totalHit, game_correctHit;
	
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
			if(isGameMode && !isGameCompleted){
				if(trackSequence[trackProgress].equals(m)){
					GUI.playSound(m);
					game_correctHit++;
					game_totalHit++;
					trackProgress++;
					if(trackProgress >= trackSequence.length){
						isGameCompleted = true;
						GUI.showGameView("e");
						
						double rate = (double)game_correctHit / (double) game_totalHit;
						if(rate >= 0.8){
							GUI.playSound("_success");
						}else{
							GUI.playSound("boo");
						}
						game_totalHit = 0;
						game_correctHit = 0;
					}else{
						GUI.showGameView(trackSequence[trackProgress]);
					}
				}else{
					int n = (int)(Math.random() * 3 + 1);
					GUI.playSound("_wrong" + n);
					game_totalHit++;
				}
				GUI.updateGameUI(game_totalHit, game_correctHit);
			}else{
				GUI.playSound(m);
			}
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
		
		GUI.showImage(GPU.getImages(),GPU.getBaseLockState());
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
			}else if(command.equals("White Balance")){
				if(GPU.switchWhiteBalance()){
					GUI.addLog("White balance ON");
				}else{
					GUI.addLog("White balance OFF");
				}
			}else if(command.equals("Apply Arguments")){
				GPU.setArgumentSetting(GUI.getArgumentField());
				GUI.setArgumentField(GPU.getArgumentSetting());
				GUI.addLog("Arguments applied.");
			}else if(command.equals("Game Mode")){
				if(!isGameMode && !GPU.getBaseLockState()){
					GUI.addLog("Base lock first, then retry");
					return;
				}
				isGameMode = !isGameMode;
				GUI.gameModeSwitch(isGameMode);
				if(isGameMode){
					trackProgress = 0;
					gameStart();
					GUI.showGameView(trackSequence[trackProgress]);
				}
			}
			
		}
	}
	
	public static void gameStart(){
		try {
			FileReader fr=new FileReader("game\\track.dat");
			BufferedReader br=new BufferedReader(fr);
			String line;
			int lineCount = 0;
			while ((line = br.readLine()) != null) {
				lineCount ++;
			}
			br.close();
			int trackCount = lineCount / 2;
			int trackNumber = (int)(Math.random() * trackCount + 1);
			
			fr=new FileReader("game\\track.dat");
			br=new BufferedReader(fr);
			lineCount = 0;
			String trackTitle = "";
			while ((line = br.readLine()) != null) {
				lineCount++;
				if(lineCount == trackNumber * 2 - 1){
					trackTitle = line;
				}else if(lineCount == trackNumber * 2){
					trackSequence = line.split(" ");
				}
			}
			GUI.gameSetTitle(trackTitle);
			isGameCompleted = false;
			game_totalHit = 0;
			game_correctHit = 0;
			br.close();
			
		}catch(Exception e){}
		
	}
	
	public static void main(String[] args) {
		/*
		 * Local Variable declarations
		 */
		int recvPacketCount = 0,
		    recvPacketError = 0,
		    recvFrameCount = 0,
		    currentWaitingFrameSeq = 0;
		
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
		GUI.setArgumentField(GPU.getArgumentSetting());
		while(isServerSocketCreated){
			GUI.trafficUpdate(recvPacketCount,recvPacketError,recvFrameCount);
			
			/*
			 * Allocates memory for incoming data.
			 */
			byte[] receiveData = new byte[PACKET_SIZE + 5];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/*
			 * Try to receive data packet from client.
			 */
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {
				GUI.addLog("Error receiving packet " + recvPacketCount + " of frame " + recvFrameCount);
				recvPacketError++;
				continue;
			}
			
			/*
			 * Proceed if a packet successfully received,
			 * load the data buffer into frame buffer.
			 */
			int incomingFrameSeq = receiveData[0]&0xFF;
			int incomingPacket = ((receiveData[1]&0xFF)<<24) + ((receiveData[2]&0xFF)<<16) + ((receiveData[3]&0xFF)<<8) + (receiveData[4]&0xFF);
			//System.out.println("GET " + incomingFrameSeq + ", " + incomingPacket + ",should be " + recvFrameCount);
			try{
				if(incomingFrameSeq == currentWaitingFrameSeq){
					System.arraycopy(receivePacket.getData(), 5, recvFrame, PACKET_SIZE*incomingPacket, receivePacket.getLength() - 5);
	            	recvPacketCount++;
				}else{
					//Not surely an error.
					continue;
				}
            }catch(ArrayIndexOutOfBoundsException e){
            	/*
            	 * This occur when a packet somehow is lost,
            	 * Discard the whole frame.
            	 */
            	GUI.addLog("Out of bound exception on packet " + recvPacketCount + " of frame " + recvFrameCount);
            	recvPacketCount = 0;
            	recvPacketError++;
            	continue;
        	
            }
			
			if(receivePacket.getLength() != PACKET_SIZE + 5){
				/*
				 * The size of the packet varies if it's the last one of the frame.
				 */
				//System.out.println(recvPacketCount);
				int lossPacketCount = (FRAME_SIZE / PACKET_SIZE + 1) - recvPacketCount;
				if(lossPacketCount >= 30){
					if(debugMode){
						GUI.addLog("Discard frame " + recvFrameCount + " due to too many packet loss");
					}
					recvPacketCount = 0;
					recvPacketError++;
					currentWaitingFrameSeq = 1 - currentWaitingFrameSeq;
					continue;
				}
				recvPacketCount = 0;
				recvFrameCount++;
				currentWaitingFrameSeq = 1 - currentWaitingFrameSeq;
				onFrameReceived(recvFrame,recvFrameCount);
			}
		}
	}

}
