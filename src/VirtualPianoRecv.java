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
	
	public static void main(String[] args) {
		/*
		 * Local Variable declarations
		 */
		int recvPacketCount = 0,
		    recvPacketError = 0,
		    recvFrameCount = 0;
		byte[] recvFrame;
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
		recvFrame = new byte[PACKET_SIZE];
		GUI.addLog("Start Listening.");
		while(isServerSocketCreated){
			
		}
	}

}
