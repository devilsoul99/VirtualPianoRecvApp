import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

public class ImageProcessor {
	/*
	 * Constant declaration
	 */
	private final int IMAGE_WIDTH = 480,
					  IMAGE_HEIGHT = 320,
					  IMAGE_BUFFER_COUNT = 5,
					  EX_MARK_RED_COUNT = 9,
					  EX_MARK_GREEN_COUNT = 9,
					  EX_MARK_BLUE_COUNT = 2,
					  EX_KEY_COUNT = 8;
	
	/*
	 * Threshold declarations
	 */
	private int th_mark_redMinValue = 120,
				th_mark_redMaxOut = 50,
				th_mark_greenMinValue = 100,
				th_mark_greenMaxOut = 40,
				th_mark_blueMinValue = 100,
				th_mark_blueMaxOut = 40;
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
				labelNumberBlue;
	private boolean isBaseLocked = false;
	
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
	    				int keyColor = (key + 1) * 30;
	    				images[2].setRGB(x, y, (keyColor << 16) + (keyColor << 8) + keyColor);
	    			}          			            			
	    		}
	    	}
		}
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
			labelTag[x][y] = labelNumberRed++;
			if(labelNumberRed < 9){
				redMark[labelNumberRed] = new Point(x,y);
			}
		}else if(markedColor == Color.GREEN.getRGB()){
			labelTag[x][y] = labelNumberGreen++;
			if(labelNumberGreen < 9){
				greenMark[labelNumberGreen] = new Point(x,y);
			}
		}else if(markedColor==Color.BLUE.getRGB()){
			labelTag[x][y] = labelNumberBlue++;
			if(labelNumberBlue < 2){
				blueMark[labelNumberBlue] = new Point(x,y);
			}
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
		 * Find mark point in tag result image by RGB vector.
		 */
		for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			int[] pixelRGB = getPixelRGB(0, x, y);
    			/*
    			 * Threshold analyzing.
    			 */
    			if(pixelRGB[0] > th_mark_redMinValue && pixelRGB[0] - pixelRGB[1] > th_mark_redMaxOut && pixelRGB[0] - pixelRGB[2] > th_mark_redMaxOut){
    				images[2].setRGB(x, y, Color.RED.getRGB());
    			}else if(pixelRGB[1] > th_mark_greenMinValue && pixelRGB[1] - pixelRGB[2] > th_mark_greenMaxOut && pixelRGB[1] - pixelRGB[0] > th_mark_greenMaxOut){
    				images[2].setRGB(x, y, Color.GREEN.getRGB());
    			}else if(pixelRGB[2] > th_mark_blueMinValue && pixelRGB[2] - pixelRGB[0] > th_mark_blueMaxOut && pixelRGB[2] - pixelRGB[1] > th_mark_blueMaxOut){
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
		if(labelNumberRed != 9 || labelNumberGreen != 9 || labelNumberBlue != 2){
			isBaseLocked = false;
    	}else{
    		writeArea();
    		isBaseLocked = true;
    	}
		return isBaseLocked;
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

  	public BufferedImage getImage(int seq){
  		if(seq >= 0 && seq < images.length){
  			return images[seq];
  		}
  		return null;
  	}
  	
  	public int getImageBufferCount(){
  		return IMAGE_BUFFER_COUNT;
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
			
			return true;
		}
		return false;
	}
}
