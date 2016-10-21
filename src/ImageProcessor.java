import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ImageProcessor {
	/*
	 * Constant declaration
	 */
	private final int IMAGE_WIDTH = 480,
					  IMAGE_HEIGHT = 320,
					  IMAGE_BUFFER_COUNT = 5;
	/*
	 * Variable declarations
	 */
	private BufferedImage[] images = new BufferedImage[IMAGE_BUFFER_COUNT];
	private int th_mark_redMinValue = 120,
				th_mark_redMaxOut = 50,
				th_mark_greenMinValue = 100,
				th_mark_greenMaxOut = 40,
				th_mark_blueMinValue = 100,
				th_mark_blueMaxOut = 40;
	
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
	
	public void WriteLabel(int[][] labelTag, int x, int y){
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
		if(markedColor==Color.RED.getRGB()){
			tagArr[x][y] = tagNumRed;
			if(tagNumRed<9){
				redPoint[tagNumRed++] = new Point(x,y);
			}
		}else if(markedColor==Color.GREEN.getRGB()){
			tagArr[x][y] = tagNumGreen;
			if(tagNumGreen<9){
				greenPoint[tagNumGreen++] = new Point(x,y);
			}
		}else if(markedColor==Color.BLUE.getRGB()){
			tagArr[x][y] = tagNumBlue;
			if(tagNumBlue<9){
				bluePoint[tagNumBlue++] = new Point(x,y);
			}
		}
		for(int i=x-1;i<=x+1;i++){
			for(int j=y-1;j<=y+1;j++){
				copyTag(tagArr,i,j,markedColor,x,y);
			}
		}
	}
	
	
	public boolean baseLock(){
		/*
		 * Base Lock process started, first we copy two sample 
		 * to gray scale base image and tag result slot.
		 */
		deepCopy(0, 2);
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
		boolean[][] isNoise = new boolean[480][320];
    	for(int x = 0; x < IMAGE_WIDTH; x++){
    		for(int y = 0; y < IMAGE_HEIGHT; y++){
    			isNoise[x][y] = false;
    			int pixelColor = images[2].getRGB(x, y);
    			if(pixelColor == Color.RED.getRGB() || pixelColor == Color.GREEN.getRGB() || pixelColor == Color.BLUE.getRGB()){
    				/*
    				 * Use a 3x3 mask to determine.
    				 */
    				for(int i = x - 1; i <= x + 1; i++){
    					for(int j = y - 1; j <= y + 1; i++){
    						if(i >= 0 && i < IMAGE_WIDTH && j >= 0 && j < IMAGE_HEIGHT && images[2].getRGB(i, j) != pixelColor){
    							/*
    							 * This pixel (x,y) is a noise. 
    							 */
    							isNoise[x][y] = true;
    						}
    					}
    				}
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
		
		
		return true;
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
