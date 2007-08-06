import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.Icon;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import de.lagcity.TLH.COLOR;
import de.lagcity.TLH.DEBUG;
import de.lagcity.TLH.SYSTEM;

public class PixelGrid {

	
	public static int SIMPLIFYFAKTOR = 1;
	public static int COLORVALUEFAKTOR= 0xffffff;
	public static double RELATIVCONTRAST = 0.85;
	public static double BACKGROUNDSAMPLECLEANPERCENT=0.1;
	public static double BLACKPERCENT=0.1;




	protected int[][] grid;

	protected int[] pixel;
	public static int tmpFiles=0;
	public PixelGrid(int width, int height) {
		grid = new int[width][height];
	//create Folder für die Temfiles
		if(!new File("tmp/").exists() ||!new File("tmp/").isDirectory()){
			new File("tmp/").mkdirs();
			
		}
		//Löscht alte Tempfiles;
		File file;
		int num=0;
		while((file=(new File("tmp/tf_"+num))).exists()){
			num++;
			file.delete();
		}
	}
/*
 * Gibt die Breite des internen captchagrids zurück
 */
	public int getWidth() {
		return grid.length;
	}
/*
 * Gibt die Höhe des internen captchagrids zurück
 */
	public int getHeight() {
		if (grid.length == 0)
			return 0;
		return grid[0].length;
	}
/*
 * Nimmt ein int-array auf und wandelt es in das interne Grid um
 */
	public void setPixel(int[] pixel) {
		this.pixel = pixel;
		int i = 0;
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				grid[x][y] = pixel[i++];
			}
		}

	}
/*
 * Sollte je nach farbmodell den Höchsten pixelwert zurückgeben. RGB: 0xffffff
 */
	protected long getMaxPixelValue() {
		return 1*COLORVALUEFAKTOR;
	}

/*
 * Gibt den Pixelwert an der stelle x,y zurück. Im grid sind die werte im RGB Format abgelegt. Hier könnte man farbraum umrechnungen einstellen
 * Dass die Parameter gleich bleiben sollte amn immerschauen das sman sich in einem wertebereich von 0-0xffffff bewegt. Je nach captchatyp kanne s erforderlich sein den farbraum anzupassen
 */
	protected int getPixelValue(int x, int y) {
	
	try{
		Color c=new Color(grid[x][y]);
		float[] hsb=COLOR.rgb2hsb(c.getRed(),c.getGreen(),c.getBlue());	
		return (int)(hsb[2]*COLORVALUEFAKTOR);
	}catch(ArrayIndexOutOfBoundsException e){
		DEBUG.trace("ERROR: Nicht im grid; ["+x+"]["+y+"] grid: "+getWidth()+"/"+getHeight());
		DEBUG.error(e);
		return (int)getMaxPixelValue();
	}

	}
/*
 * Gibt dne Durchschnittlichen pixelwert des Bildes zurück
 */
	protected int getAverage() {
		long avg = 0;
		int i = 0;
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				avg = avg * i + getPixelValue(x, y);
				i++;
				avg /= i;

			}
		}
		return (int) avg;

	}
	
	protected int getAverage(int px,int py,int width,int height) {
		long avg = 0;
		int i = 0;
		int halfW=width/2;
		int halfH=height/2;
		if(width==1&&px==0)width=2;
		if(height==1&&py==0)height=2;
		
		for (int x =  Math.max(0,px-halfW); x < Math.min(px+width-halfW,getWidth()); x++) {
			for (int y = Math.max(0,py-halfH); y < Math.min(py+height-halfH,getHeight()); y++) {
				avg = avg * i + getPixelValue(x, y);
				i++;
				avg /= i;

			}
		}
		return (int) avg;
	}
	protected int getAverage(int px,int py,int width,int height,Captcha mask) {
		long avg = 0;
		int i = 0;
		int halfW=width/2;
		int halfH=height/2;
		if(width==1&&px==0)width=2;
		if(height==1&&py==0)height=2;
		for (int x = Math.max(0,px-halfW); x < Math.min(px+width-halfW,getWidth()); x++) {
			for (int y = Math.max(0,py-halfH); y < Math.min(py+height-halfH,getHeight()); y++) {
				if(mask.getPixelValue(x, y)>(getMaxPixelValue()*BLACKPERCENT)){				
				avg = avg * i + getPixelValue(x, y);
				i++;
				avg /= i;
				}

			}
		}
		return (int) avg;
	}
/*
 * machtd as Bild gröber und trifft eine Pixel vorauswahl. püarameter ist RELATIVCONTRAST
 */
	public void sampleDown(int faktor) {
		int newWidth = (int) Math.ceil(getWidth() / faktor);
		int newHeight = (int) Math.ceil(getHeight() / faktor);

		int[][] newGrid = new int[getWidth()][getHeight()];
		int avg = getAverage();
		for (int x = 0; x < newWidth; x++) {
			for (int y = 0; y < newHeight; y++) {
				long v = 0;
				int values = 0;
				for (int gx = 0; gx < faktor; gx++) {
					for (int gy = 0; gy < faktor; gy++) {
						int newX = x * faktor + gx;
						int newY = y * faktor + gy;
						if (newX > getWidth() || newY > getHeight()) {
							continue;
						}
						values++;
						v += getPixelValue(newX, newY);
					}
				}
				v /= values;
				for (int gx = 0; gx < faktor; gx++) {
					for (int gy = 0; gy < faktor; gy++) {
						int newX = x * faktor + gx;
						int newY = y * faktor + gy;
						newGrid[newX][newY] = isElement((int) v, avg) ? 0
								: (int) getMaxPixelValue();
					}
				}

			}
		}

		this.grid = newGrid;

	}
	public void blurIt(int faktor) {
		

		int[][] newGrid = new int[getWidth()][getHeight()];
		
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				newGrid[x][y]=getAverage(x,y,faktor,faktor);

			}
		}

		this.grid = newGrid;

	}
	protected void cleanBackgroundBySample(int px,int py,int width,int height){
		int avg=getAverage(px+width/2,py+height/2,width,height);
		
		int[][] newgrid=new int[getWidth()][getHeight()];
		
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				int dif=Math.abs(avg-getPixelValue(x,y));
				if(dif<(getMaxPixelValue()*BACKGROUNDSAMPLECLEANPERCENT)){
					newgrid[x][y]=(int)getMaxPixelValue();
				}else{
					newgrid[x][y]=getPixelValue(x,y);
				}

			}
		}
		grid=newgrid;
		
	}

	protected void cleanWithMask(Captcha mask,int width, int height){		
		int[][] newgrid=new int[getWidth()][getHeight()];
		

		if(mask.getWidth()!=getWidth()||mask.getHeight()!=getHeight()){
			DEBUG.trace("ERROR Maske und Bild passen nicht zusammmen");
			return;
		}
		
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				if(mask.getPixelValue(x, y)<(getMaxPixelValue()*BLACKPERCENT)){
					newgrid[x][y]=getAverage(x,y,width,height,mask);
					
					
					
				}else{
					newgrid[x][y]=getPixelValue(x,y);
				}
			}
		}
		grid=newgrid;
		
	}
	public Image getImage(){
		int[] pix = new int[getWidth() * getHeight()];
		int pixel=0;
		BufferedImage image = new BufferedImage (getWidth(),getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				graphics.setColor(new Color (getPixelValue(x,y)));
				graphics.fillRect(x, y, 1, 1);
			}
		}
		return image;
				
	}
	public void saveImageasJpg(File file){
		BufferedImage bimg = null;

		bimg = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
		bimg.setRGB(0,0,getWidth(),getHeight(),getPixel(),0,getWidth());

//		 Encode as a JPEG
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
	
		JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
		jpeg.encode(bimg);
		fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ImageFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int[] getPixel(){
		int[] pix = new int[getWidth() * getHeight()];
		int pixel=0;

		int avg=getAverage();
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel]=getPixelValue(x,y);
				pixel++;
			}
		}
		return pix;
	}
/*
 * Gibt ein ACSI bild des Captchas aus
 */
	public void printGrid() {
		DEBUG.trace("\r\n" + getString());
	}
	public String getDim(){
		return "("+getWidth()+"/"+getHeight()+")";
	}
/*
 * prüft ob ein pixel über der eingestellten schwelle ist
 */
	protected boolean isElement(int value, int avg) {
		return value < (avg * RELATIVCONTRAST);
	}
	public void setGrid(int[][] letterGrid) {
		grid = letterGrid;
	}
	public Icon getAsIcon(){
		File tempFile;
		saveImageasJpg(tempFile=new File("tmp/img_"+(tmpFiles++)+".jpg"));
		tempFile.deleteOnExit();
		return (Icon) SYSTEM.getImageIcon(tempFile.getAbsolutePath(), "img",
				SYSTEM.ABSOLUTEPATH);
	}
	
	/*
	 * Trennt auf allen 4 Seiten reihen ab die unter der Schwelle sind (isElement(...))
	 */
		public boolean clean() {
			byte topLines = 0;
			byte bottomLines = 0;
			byte leftLines = 0;
			byte rightLines = 0;
			int avg = getAverage();
			//BasicWindow.showImage(getImage(), "befor clean");
			for (int x = 0; x < getWidth(); x++) {
				boolean rowIsClear = true;
				;
				for (int y = 0; y < getHeight(); y++) {
				//	DEBUG.trace(x+"-"+y+" : "+getPixelValue(x, y)+"-"+avg);
					if (isElement(getPixelValue(x, y), avg)) {
						rowIsClear = false;
						break;
					}
				}
				if (!rowIsClear)
					break;
				leftLines++;
			}
			// DEBUG.trace(kontrastBorder);
			
			
			
			for (int x = getWidth() - 1; x >= 0; x--) {
				boolean rowIsClear = true;
				;
				for (int y = 0; y < getHeight(); y++) {

					if (isElement(getPixelValue(x, y), avg)) {
						rowIsClear = false;
						break;
					}
				}
				if (!rowIsClear)
					break;

				rightLines++;
			}

			if(leftLines>=getWidth()||(getWidth() - rightLines)>getWidth()){
				DEBUG.trace("ERROR: cleaning failed. nothing left1");
				grid = new int[0][0];
				return false;
			
			}
			for (int y = 0; y < getHeight(); y++) {
				boolean lineIsClear = true;
				;
				for (int x = leftLines; x < getWidth() - rightLines; x++) {
					if (isElement(getPixelValue(x, y), avg)) {
						lineIsClear = false;
						break;
					}
				}
				if (!lineIsClear)
					break;
				topLines++;
			}
		
			for (int y = getHeight() - 1; y >= 0; y--) {
				boolean lineIsClear = true;
				;
				for (int x = leftLines; x < getWidth() - rightLines; x++) {
					if (isElement(getPixelValue(x, y), avg)) {
						lineIsClear = false;
						break;
					}
				}
				if (!lineIsClear)
					break;
				bottomLines++;
			}
			//DEBUG.trace("clear: " + leftLines + " - " + topLines + " - "
			//		+ rightLines + " - " + bottomLines);
			if ((getWidth() - leftLines - rightLines) < 0
					|| (getHeight() - topLines - bottomLines) < 0) {
				DEBUG.trace("ERROR: cleaning failed. nothing left");
				grid = new int[0][0];
				return false;
			}
			int[][] ret = new int[getWidth() - leftLines - rightLines][getHeight()
					- topLines - bottomLines];

			for (int y = 0; y < getHeight() - topLines - bottomLines; y++) {
				for (int x = 0; x < getWidth() - leftLines - rightLines; x++) {
					ret[x][y] = getPixelValue(x + leftLines, y + topLines);
				}

			}
			grid = ret;
			//BasicWindow.showImage(getImage(), "after clean");
			
			return true;

		}
	
/*
 * Gibt einen ASCII String des Bildes zurück
 */
	public String getString() {
		int avg = getAverage();
		String ret = "";

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {

				ret += isElement(getPixelValue(x, y), avg) ? "*" : (int)Math.floor(9*(getPixelValue(x, y)/getMaxPixelValue()));
				
			}
			ret += "\r\n";
		}

		return ret;

	}
/*
 * schreibt die static properties in eie Property file
 */
	public static void setCurrentProperties(Properties file) {

		// Faktor um den die Buchstaben vereinfacht werden
		file.setProperty("SIMPLIFYFAKTOR", SIMPLIFYFAKTOR+"");
		// je kleiner, desto mehr wird ausgefiltert. wenn dieser wert kleiner
		// wird sollte GAPDETECTIONAVERAGECONTRAST größer werden
		file.setProperty("RELATIVCONTRAST", RELATIVCONTRAST+"");
		// Links davon wird keine gap erkannt. Wird bei geeigneter average
		// einstellungen nicht beachtet
	
	}
	public static void setProperties(Properties file) {



	













	

		SIMPLIFYFAKTOR=Integer.parseInt(file.getProperty("SIMPLIFYFAKTOR", SIMPLIFYFAKTOR+""));

		RELATIVCONTRAST=Double.parseDouble(file.getProperty("RELATIVCONTRAST", RELATIVCONTRAST+""));


		
	}

}