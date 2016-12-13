import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.xml.internal.ws.org.objectweb.asm.Label;

import sun.awt.util.IdentityArrayList;

public class ImageProcessor {
	/*
	 * Constant declaration
	 */
	private final int IMAGE_WIDTH = 480,
					  IMAGE_HEIGHT = 320,
					  IMAGE_BUFFER_COUNT = 5,
					  EX_MARK_RED_COUNT = 12,
					  EX_MARK_GREEN_COUNT = 12,
					  EX_MARK_BLUE_COUNT = 2,
					  EX_KEY_COUNT = 11;
	
	/*
	 * Threshold declarations
	 */
	private int th_mark_redMinDegree = 320,
				th_mark_redMaxDegree = 40,
				th_mark_greenMinDegree = 80,
				th_mark_greenMaxDegree = 160,
				th_mark_blueMinDegree = 220,
				th_mark_blueMaxDegree = 300,
				th_tip_aboveLineLenth = 30,
				th_press_rectWidth = 100,
				th_press_rectHeight = 100;
	private float th_mark_minS = (float) 0.25,
				  th_mark_minB = (float) 0.25,
				  th_ls_B1_ratio = (float) 0.3,
				  th_ls_B2_ratio = (float) 1.2,
				  th_ls_minB = (float) 0.25,
				  th_ls_minB_var = (float) 0.25;
	private double th_press_shadowPercentage = 1;
	
	/*
	 * Variable declarations
	 */
	private BufferedImage[] images = new BufferedImage[IMAGE_BUFFER_COUNT];
	private Point[] redMark = new Point[EX_MARK_RED_COUNT],
					greenMark = new Point[EX_MARK_GREEN_COUNT],
					blueMark = new Point[EX_MARK_BLUE_COUNT];
	private int[][] keyArea = new int[IMAGE_WIDTH][IMAGE_HEIGHT];
	private int labelNumberRed,
				labelNumberGreen,
				labelNumberBlue,
				topestRedPointY;
	private boolean isBaseLocked = false,
					isWhiteBalanceOn = false;
	private boolean[] iskeyPlaying = new boolean[EX_KEY_COUNT * 2];
	
	public ImageProcessor(){
		/*
		 * Allocates BufferedImage: 0 -> original image
		 * 							1 -> gray scale
		 * 							2 -> tag result
		 * 							3 -> tip process
		 * 							5 -> base image (in gray scale)
		 */
		images[0] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		images[1] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		images[2] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		images[3] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		images[4] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
	}
	
	private int[] getPixelRGB(int subIndex, int x, int y){
		int[] rv = new int[3];
		int color = images[subIndex].getRGB(x, y);
		rv[0] = (color >> 16) & 0xFF;
		rv[1] = (color >> 8 ) & 0xFF;
		rv[2] = color & 0xFF;
		return rv;
	}
	
	public void deepCopy(int src, int dest){
		/*
		 *  Convert the original colored image to gray scale.
		 */
		Graphics g = images[dest].getGraphics();
		g.drawImage(images[src], 0, 0, null);
		g.dispose();
		return;
	}
	
	private void writeArea(){
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			keyArea[x][y] = -1;			            			
    		}
    	}
		for(int key = 0; key < EX_KEY_COUNT; key++){
			double slopeLeft = (greenMark[key].getY() - redMark[key].getY()) / (greenMark[key].getX() - redMark[key].getX());
			double slopeRight = (greenMark[key + 1].getY() - redMark[key + 1].getY()) / (greenMark[key + 1].getX() - redMark[key + 1].getX());
			for(int x = 0; x < IMAGE_WIDTH; x++){
				for(int y = 0; y < IMAGE_HEIGHT; y++){
					double left, right;
					if(slopeLeft < 0){
						left = (y - greenMark[key].getY()) - slopeLeft * (x - greenMark[key].getX());
					}else{
						 left = slopeLeft * (x - greenMark[key].getX()) - (y - greenMark[key].getY());
					}
					if(slopeRight < 0){
						right = (y - greenMark[key + 1].getY()) - slopeRight * (x - greenMark[key + 1].getX());
					}else{
						right = slopeRight * (x - greenMark[key + 1].getX()) - (y - greenMark[key + 1].getY());
					}
	    			if(left > 0 && right < 0 && y > redMark[key].getY() && y < greenMark[key].getY()){
	    				keyArea[x][y] = key;
	    				int keyColor = (key + 1) * 22;
	    				images[2].setRGB(x, y, (keyColor << 16) + (keyColor << 8) + keyColor);
	    			}          			            			
	    		}
	    	}
		}
		keyReset();
	}
	
	private void copyBorderLabel(int[][] labelTag,int x,int y,int color,int ox,int oy){
		/*
		 * Label itself by copy the label from caller,
		 * recursive for its own bordered pixel.
		 */
		if(x < 0 || x >= IMAGE_WIDTH || y < 0 || y >= IMAGE_HEIGHT || labelTag[x][y] != -1){
			/*
			 * Out of bound or already labeled.
			 */
			return;
		}
		int markedColor = images[2].getRGB(x, y);
		if(markedColor == color){
			labelTag[x][y] = labelTag[ox][oy];
			for(int i = x-1; i <= x + 1; i++){
				for(int j = y - 1; j <= y + 1; j++){
					copyBorderLabel(labelTag,i,j,color,x,y);
				}
			}
		}
	}
	
	private void writeLabel(int[][] labelTag, int x, int y){
		/*
		 * Labeling the current pixel, if the bordering pixel
		 * is in the same color, make a recursive call to copy label.
		 */
		if(x < 0 || x >= IMAGE_WIDTH || y < 0 || y >= IMAGE_HEIGHT || labelTag[x][y] != -1){
			/*
			 * Out of bound or already labeled.
			 */
			return;
		}
		int markedColor = images[2].getRGB(x, y);
		if(markedColor == Color.BLACK.getRGB()){
			/*
			 * Not a marked pixel.
			 */
			return;
		}
		if(markedColor == Color.RED.getRGB()){
			labelTag[x][y] = labelNumberRed;
			if(labelNumberRed < EX_MARK_RED_COUNT){
				redMark[labelNumberRed] = new Point(x,y);
			}
			labelNumberRed++;
		}else if(markedColor == Color.GREEN.getRGB()){
			labelTag[x][y] = labelNumberGreen;
			if(labelNumberGreen < EX_MARK_GREEN_COUNT){
				greenMark[labelNumberGreen] = new Point(x,y);
			}
			labelNumberGreen++;			
		}else if(markedColor==Color.BLUE.getRGB()){
			labelTag[x][y] = labelNumberBlue;
			if(labelNumberBlue < EX_MARK_BLUE_COUNT){
				blueMark[labelNumberBlue] = new Point(x,y);
			}
			labelNumberBlue++;
		}
		for(int i = x - 1; i <= x + 1; i++){
			for(int j = y - 1; j <= y + 1; j++){
				copyBorderLabel(labelTag, i, j, markedColor, x, y);
			}
		}
	}
	
	public boolean baseLock(){
		/*
		 * Base Lock process started, first we copy two sample 
		 * to gray scale base image and tag result slot.
		 */
		deepCopy(1, 2);
		deepCopy(2, 4);
		/*
		 * Find mark point in tag result image by using HSV vector.
		 */
		float redMinH = (float)th_mark_redMinDegree / (float)360 ;
		float redMaxH = (float)th_mark_redMaxDegree / (float)360 ;
		float greenMinH = (float)th_mark_greenMinDegree / (float)360 ;
		float greenMaxH = (float)th_mark_greenMaxDegree / (float)360 ;
		float blueMinH = (float)th_mark_blueMinDegree / (float)360 ;
		float blueMaxH = (float)th_mark_blueMaxDegree / (float)360 ;
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			float[] pixelHSB = new float[3];
    			Color.RGBtoHSB(pixelRGB[0], pixelRGB[1], pixelRGB[2], pixelHSB);
    			if((pixelHSB[0] >= redMinH || pixelHSB[0] <= redMaxH) && pixelHSB[1] >= th_mark_minS && pixelHSB[2] >= th_mark_minB){
    				images[2].setRGB(x, y, Color.RED.getRGB());
    			}else if((pixelHSB[0] >= greenMinH && pixelHSB[0] <= greenMaxH) && pixelHSB[1] >= th_mark_minS && pixelHSB[2] >= th_mark_minB){
    				images[2].setRGB(x, y, Color.GREEN.getRGB());
    			}else if((pixelHSB[0] >= blueMinH && pixelHSB[0] <= blueMaxH) && pixelHSB[1] >= th_mark_minS && pixelHSB[2] >= th_mark_minB){
    				images[2].setRGB(x, y, Color.BLUE.getRGB());
    			}
    		}
    	}
		
		/*
		 * Noise elimination, needed to optimize the labeling process.
		 */
		boolean[][] isNoise = new boolean[IMAGE_WIDTH][IMAGE_HEIGHT];
    	for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			isNoise[x][y] = false;
    			int pixelColor = images[2].getRGB(x, y);
    			if(pixelColor == Color.RED.getRGB() || pixelColor == Color.GREEN.getRGB() || pixelColor == Color.BLUE.getRGB()){
    				/*
    				 * Use a 3x3 mask to determine.
    				 */
    				for(int i = x - 1; i <= x + 1; i++){
    					for(int j = y - 1; j <= y + 1; j++){
    						if(i >= 0 && i < IMAGE_WIDTH && j >= 0 && j < IMAGE_HEIGHT && images[2].getRGB(i, j) != pixelColor){
    							/*
    							 * This pixel (x,y) is a noise. 
    							 */
    							isNoise[x][y] = true;
    						}
    					}
    				}
    			}else{
    				isNoise[x][y] = true;
    			}
    		}
    	}
    	for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			if(isNoise[x][y]){
    				/*
        			 * Turn to black if it is a noise.
        			 */
    				images[2].setRGB(x, y, 0);
    			}
    		}
    	}
		/*
		 * Labeling marked pixel to check whether the base lock
		 * process can proceed.
		 */
		int[][] pixelLabel = new int[IMAGE_WIDTH][IMAGE_HEIGHT];
		labelNumberRed = 0;
		labelNumberGreen = 0;
		labelNumberBlue = 0;
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			pixelLabel[x][y] = -1;		            			
    		}
    	}
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			writeLabel(pixelLabel, x, y);	            			
    		}
    	}
		System.out.println("Label detected: " + labelNumberRed + ", " + labelNumberGreen + ", " + labelNumberBlue);
		if(labelNumberRed != EX_MARK_RED_COUNT || labelNumberGreen != EX_MARK_GREEN_COUNT || labelNumberBlue != EX_MARK_BLUE_COUNT){
			isBaseLocked = false;
    	}else{
    		writeArea();
    		findTopestRedPoint();
    		isBaseLocked = true;
    	}
		return isBaseLocked;
	}

	private void findTopestRedPoint(){
		topestRedPointY = redMark[0].y; 
		for(int i = 1; i < redMark.length; i++) {
			if(redMark[i].y < topestRedPointY) {
				topestRedPointY = redMark[i].y;
			}
		}
	}
	
	private float avgBrightness(){
		float avgB = (float) 0;
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			float[] pixelHSB = new float[3];
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			Color.RGBtoHSB(pixelRGB[0], pixelRGB[1], pixelRGB[2], pixelHSB);
    			avgB += pixelHSB[2];
    		}
		}
		return avgB / (IMAGE_HEIGHT * IMAGE_WIDTH);
	}
	
	private void layerSeparation(){
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = topestRedPointY; y < IMAGE_HEIGHT; y++){
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			int[] baseRGB = getPixelRGB(4, x, y);
    			float[] pixelHSB = new float[3];
    			float[] baseHSB = new float[3];
    			Color.RGBtoHSB(pixelRGB[0], pixelRGB[1], pixelRGB[2], pixelHSB);
    			Color.RGBtoHSB(baseRGB[0], baseRGB[1], baseRGB[2], baseHSB);
    			boolean is_var = Math.abs(baseHSB[2] - pixelHSB[2]) > th_ls_minB_var;    			
    			if(pixelHSB[2] < th_ls_minB && is_var){
    				images[3].setRGB(x, y, Color.BLACK.getRGB());
    			}else {
    				if(is_var){
    					images[3].setRGB(x, y, Color.BLUE.getRGB());
    				}else{
    					images[3].setRGB(x, y, Color.WHITE.getRGB());
    				}
    			}
    		}
    	}
	}
	
	private void drawCross(Point intersectiion, int color){
		int interX = (int)intersectiion.getX();
		int interY = (int)intersectiion.getY();
		for(int y = topestRedPointY; y < IMAGE_HEIGHT; y++){
			images[3].setRGB(interX, y, color);
		}
		for(int x = 0; x < IMAGE_WIDTH; x++){
			images[3].setRGB(x, interY, color);
		}
		return;
	}
	
	private Point findTip(){
		/*
		 * Find the tip locate on image 3
		 * 1. The pixel itself is black
		 * 2. The pixel below it is not black
		 * 3. 5 pixels above is all black
		 */
		for(int y = IMAGE_HEIGHT - 1; y >= topestRedPointY; y--){
			for(int x = 0; x < IMAGE_WIDTH; x++){
				if(images[3].getRGB(x, y) == Color.BLACK.getRGB()){
					if(y + 1 < IMAGE_HEIGHT && images[3].getRGB(x, y + 1) != 0){
						if(y >= th_tip_aboveLineLenth){
							boolean isLineBlack = true;
							for(int i = 0; i <= th_tip_aboveLineLenth; i++){
								if(images[3].getRGB(x, y - i) != Color.BLACK.getRGB()){
									isLineBlack = false;
									break;
								}
							}
							if(isLineBlack){
								return new Point(x, y);
							}
						}
					}
				}
			}
		}
		return null;
	}

	private void keyReset(){
		for(int i = 0; i < iskeyPlaying.length; i++){
			iskeyPlaying[i] = false;
		}
		return;
	}
	
	private String tipKey(Point tip){
		int onTipKey = keyArea[(int)tip.getX()][(int)tip.getY()];
		if(onTipKey != -1){
			boolean isBlackKeys = tip.getY() > blueMark[0].getY() && onTipKey != 2 && onTipKey != 6 && onTipKey != 9;
			int keyIndex = isBlackKeys? (EX_KEY_COUNT - onTipKey - 1) * 2: (EX_KEY_COUNT - onTipKey) * 2 - 1;
			if(!iskeyPlaying[keyIndex]){
				keyReset();
				iskeyPlaying[keyIndex] = true;
				return Integer.toString(keyIndex);
			}
			return "PRESSED_IS_PLAYING";
		}
		return "TIP_NOT_ON_KEY";
	}
	
	private boolean isPressed(Point tip){
		int calculatedPixelCount = 0;
		int calculatedshadowCount = 0;
		for(int x = ((int)tip.getX() - (th_press_rectWidth/2)); x < ((int)tip.getX() + (th_press_rectWidth/2)); x++){
    		for(int y = (int)tip.getY(); y < (int)tip.getY() + th_press_rectHeight; y++){
    			if(x < 0 || x >= IMAGE_WIDTH || y < 0 || y >= IMAGE_HEIGHT){
    				continue;
    			}else{
    				calculatedPixelCount++;
    				if(images[3].getRGB(x, y) == Color.BLUE.getRGB()){
    					calculatedshadowCount++;
    				}
    			}
    		}
    	}
		double shadowPercentage = (double)calculatedshadowCount / (double)calculatedPixelCount * 100;
		if(shadowPercentage < th_press_shadowPercentage){
			return true;
		}
		keyReset();
		return false;
	}
	
	public String detectMotion(){
		if(!isBaseLocked){
			return "NOT_BASE_LOCKED";
		}
		/*
		 * Separates the shadow and the hand from background.
		 */
		for(int y = 0; y <= topestRedPointY; y++){
			for(int x = 0; x < IMAGE_WIDTH; x++){
				images[3].setRGB(x, y, Color.WHITE.getRGB());
			}
		}
		layerSeparation();
		Point tipPoint = findTip();
		if(tipPoint != null){
			drawCross(tipPoint,Color.RED.getRGB());
			if(isPressed(tipPoint)){
				return tipKey(tipPoint);
			}else{
				return "NOT PRESSED";
			}
		}
		return "TIP_NOT_FOUND";
	}
	
  	private boolean YV12ToRGB24(byte[] pYUV,int[] pRGB24,int width,int height){
		
	    if (width < 1 || height < 1 || pYUV == null || pRGB24 == null){
	    	return false;
	    }
	    long len = width * height;
	    byte[] yData = pYUV;
	    
	    long vDataStart = len;

	    int[] bgr = new int[3];
	    int yIdx,uIdx,vIdx,idx;
	    for (int i = 0;i < height;i++){
	        for (int j = 0;j < width;j++){
	        	
	            yIdx = i * width + j;
	            vIdx = (i/2) * (width/2) + (j/2) + (int)vDataStart;
	            uIdx = vIdx + (int)(len>>2);

	            bgr[0] = (int)((yData[yIdx]&0xFF) + 1.732446 * ((yData[vIdx]&0xFF) - 128));
	            bgr[1] = (int)((yData[yIdx]&0xFF) - 0.698001 * ((yData[uIdx]&0xFF) - 128) - 0.703125 * ((yData[vIdx]&0xFF) - 128));
	            bgr[2] = (int)((yData[yIdx]&0xFF) + 1.370705 * ((yData[uIdx]&0xFF) - 128)); 
	            
	            for (int k = 0;k < 3;k++){
	                idx = (i * width + j) * 3 + k;
	                if(bgr[k] >= 0 && bgr[k] <= 255)
	                    pRGB24[idx] = bgr[k]&0xFF;
	                else
	                    pRGB24[idx] = ((bgr[k]) < 0)?0:255;
	            }
	        }
	    }
	    return true;
	}

  	public BufferedImage[] getImages(){
  		return images;
  	}
  	
  	public boolean getBaseLockState(){
  		return isBaseLocked;
  	}
  	
  	public int getImageBufferCount(){
  		return IMAGE_BUFFER_COUNT;
  	}
  	
  	public boolean switchWhiteBalance(){
  		isWhiteBalanceOn = !isWhiteBalanceOn;
  		return isWhiteBalanceOn;
  	}
  	
  	public void whiteBalance(){
  		/*
  		 * Using GWA method to white balance image.
  		 */
  		double avgRed = 0, avgGreen = 0, avgBlue = 0;
  		for(int x = 0; x < IMAGE_WIDTH; x++){
  			double colAvgRed = 0, colAvgGreen = 0, colAvgBlue = 0;
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			colAvgRed += pixelRGB[0];
    			colAvgGreen += pixelRGB[1];
    			colAvgBlue += pixelRGB[2];
    		}
    		colAvgRed /= IMAGE_HEIGHT;
    		avgRed += colAvgRed;
    		colAvgGreen /= IMAGE_HEIGHT;
    		avgGreen += colAvgGreen;
    		colAvgBlue /= IMAGE_HEIGHT;
    		avgBlue += colAvgBlue;
    	}
  		avgRed /= IMAGE_WIDTH;
  		avgGreen /= IMAGE_WIDTH;
  		avgBlue /= IMAGE_WIDTH;
  		
  		double Kr = avgGreen / avgRed;
  		double Kb = avgGreen / avgBlue;
  		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			int newR = (int)(pixelRGB[0]*Kr) > 255 ? 255 : (int)(pixelRGB[0]*Kr);
    			int newB = (int)(pixelRGB[2]*Kb) > 255 ? 255 : (int)(pixelRGB[2]*Kb);
    			//System.out.println(newB);
    			Color newColor = new Color(newR, pixelRGB[1], newB);
    			images[0].setRGB(x, y,newColor.getRGB());
    		}
    	}
  	}
  	
	public boolean importFrame(byte[] frameData){
		/*
		 * Convert the frame data from YV12 to RGB form,
		 * then store it into BufferedImage.
		 */
		int[] orgImageRGB = new int[IMAGE_WIDTH * IMAGE_HEIGHT * 3];
		if(YV12ToRGB24(frameData,orgImageRGB,IMAGE_WIDTH,IMAGE_HEIGHT)){
			for(int i = 0; i < orgImageRGB.length; i += 3){
				/*
				 * For each pixel's RGB, copy to original BufferedImage.
				 */
				int orgImageColor = new Color(orgImageRGB[i],orgImageRGB[i+1],orgImageRGB[i+2]).getRGB();
				images[0].setRGB((i/3)%IMAGE_WIDTH, (i/3)/IMAGE_WIDTH,orgImageColor);
			}
			if(isWhiteBalanceOn){
				whiteBalance();
			}
			return true;
		}
		return false;
	}
}
