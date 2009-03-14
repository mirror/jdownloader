package jd;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class SplashScreenImages {
	private Image imgOriginal;
	private BufferedImage imgTemp;
	private int intInternalCounter;
	
	private ArrayList<Image> imgNormal;
	private ArrayList<Image> imgFinish;
	
	private ArrayList<Integer> intPosX;
	private ArrayList<Integer> intPosY;
	
	public SplashScreenImages(Image imgOriginal) {
		this.imgOriginal = imgOriginal;
		this.intInternalCounter = 0;
		
		this.imgNormal = new ArrayList<Image>();
		this.imgFinish = new ArrayList<Image>();
		
		this.intPosX = new ArrayList<Integer>();
		this.intPosY = new ArrayList<Integer>();
	}
	
	public void addEntry(Image imgNormal, Image imgFinish, int intPosX, int intPosY) {
		this.imgNormal.add(imgNormal);
		this.imgFinish.add(imgFinish);
		
		this.intPosX.add(intPosX);
		this.intPosY.add(intPosY);
	}
	
	public void addEntry(Image imgNormal, Image imgFinish) {
		this.imgNormal.add(imgNormal);
		this.imgFinish.add(imgFinish);
		
		this.intPosX.add(new Integer(5 + intPosX.get(intPosX.size()-1) + this.imgNormal.get(this.imgNormal.size() - 1).getWidth(null)));
		this.intPosY.add(new Integer(intPosY.get(intPosY.size()-1)));
	}
	
	public void addEntry(Image imgNormal, Image imgFinish, int intRelX) {
		this.imgNormal.add(imgNormal);
		this.imgFinish.add(imgFinish);
		
		this.intPosX.add(new Integer(5 + intPosX.get(intPosX.size()-1) + this.imgNormal.get(this.imgNormal.size() - 1).getWidth(null) + intRelX));
		this.intPosY.add(new Integer(intPosY.get(intPosY.size()-1)));
	}
	
	public BufferedImage paintNext() {	
		if(intInternalCounter < imgNormal.size()) {
			imgTemp = new BufferedImage(imgOriginal.getWidth(null), imgOriginal.getHeight(null), BufferedImage.TYPE_INT_RGB);
			imgTemp.getGraphics().drawImage(imgOriginal, 0, 0, null);
			
			if(intInternalCounter > 0) {
				for(int i = 0; i < intInternalCounter; i++) {
					imgTemp.getGraphics().drawImage(imgFinish.get(i), intPosX.get(i), intPosY.get(i), null);
				}
			}
			
			imgTemp.getGraphics().drawImage(imgNormal.get(intInternalCounter), intPosX.get(intInternalCounter), intPosY.get(intInternalCounter), null);
			intInternalCounter++;
		}
		return imgTemp;
	}
	
	public Image loadFile(String Path) {
		try {
			return ImageIO.read(new File(Path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
