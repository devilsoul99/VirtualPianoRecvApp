import java.awt.Color;
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
	
	public ImageProcessor(){
		/*
		 * Allocates BufferedImage: 0 -> original image
		 */
		images[0] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
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
				images[0].setRGB((i/3)%480, (i/3)/480,orgImageColor);
			}
			
			return true;
		}
		return false;
	}
}
