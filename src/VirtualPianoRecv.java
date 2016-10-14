/*
 * This application is used to receive real-time image and detect the motion of the finger,
 * then produce corresponding piano sound for output.
 */

// Import libraries
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import sun.audio.*;

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
	
	private static void onFrameReceived(byte[] frame){
		/*
		 * This function is called every time when program received a
		 * new frame of image, we import it to the ImageProcessor class.
		 */
		GUI.frameRateUpdate();
		if(!GPU.importFrame(frame)){
			GUI.addLog("Failed to convert YV12 image.");
			return;
		}
		return;
	}
	
	public static void main(String[] args) {
		/*
		 * Local Variable declarations
		 */
		int recvPacketCount = 0,
		    recvPacketError = 0,
		    recvFrameCount = 0;
		byte[] recvFrame = new byte[FRAME_SIZE];
		/*
		 * First, we create a GUI for the user using another implemented class,
		 * It will handle all the output functions.
		 */
		GUI = new UserInterface();
		GUI.loadElements();
		GUI.setMainVisible(true);
		/*
		 * Initializing socket, then obtain the local IP and port,
		 * this step is required for android application settings.
		 */
		DatagramSocket serverSocket = createSocket();
		if(isServerSocketCreated){
			selfAddressDetect(serverSocket);
		}
		/*
		 * Ready to receive data from mobile device.
		 */
		GUI.addLog("Start Listening.");
		
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
				onFrameReceived(recvFrame);
			}
		}
	}

}
