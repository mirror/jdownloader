import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.Icon;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import de.lagcity.TLH.DEBUG;
import de.lagcity.TLH.SWING;
import de.lagcity.TLH.SYSTEM;

public class Captcha extends PixelGrid{


	public static int GAPWIDTHPEAK = 1;
	public static int GAPWIDTHAVERAGE = 1;
	public static boolean GAPANDAVERAGELOGIC = true;
	public static double GAPDETECTIONAVERAGECONTRAST = 1.3;
	public static double GAPDETECTIONPEAKECONTRAST = 0.25;
	public static boolean USEAVERAGEGAPDETECTION = false;
	public static boolean USEPEAKGAPDETECTION = true;
	public static int MINIMUMLETTERWIDTH = 10;
	public static int LEFTPADDING = 14;
	/*
	 * Parameter Ende
	 * 
	 */
	private int lastletterX = 0;

	private boolean[] gaps;
/*
 * Diese Klasse beinhaltet ein 2D-Pixel-Grid. Sie stellt mehrere Methoden zur verfügung dieses Grid zu bearbeiten
 * Um Grunde sind hier alle Methoden zu finden um ein captcha als ganzes zu bearbeiten
 */
	public Captcha(int width, int height) {
		super(width,height);
		gaps=new boolean[width+1];

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

	public Captcha getSimplified(int faktor){
		int newWidth=(int)Math.ceil(getWidth()/faktor);
		int newHeight=(int)Math.ceil(getHeight()/faktor);
		Captcha ret= new Captcha(newWidth,newHeight);
		int [][] newGrid=new int[newWidth][newHeight];
		int avg=getAverage();
	//DEBUG.trace(getWidth()+"/"+getHeight()+" --> "+newWidth+"/"+newHeight);
		for (int x = 0; x < newWidth; x++) {
			for (int y = 0; y < newHeight; y++) {								
				long v = 0;
				int values=0;
				for (int gx = 0; gx < faktor; gx++) {
					for (int gy = 0; gy < faktor; gy++) {									
						int newX=x * faktor+gx;
						int newY=y * faktor+gy;									
						if (newX > getWidth() || newY>getHeight()){				
							continue;
						}
						values++;
						v += getPixelValue(newX,newY);								
					}
				}
				v /= values;
				//DEBUG.trace(v);
				newGrid[x][y]= isElement((int) v, avg) ? 0
						: (int) getMaxPixelValue();
			}
		}			
	
		ret.setGrid(newGrid);
	
	ret.printGrid();
		
		
		return ret;
	}
/*
 * Versucht die Buchstaben aus dem captcha zu extrahieren und gibt ein letter-array zuück
 */
	public Letter[] getLetters(int letterNum) {
		Letter[] ret = new Letter[letterNum];
	
		for (int letterId = 0; letterId < letterNum; letterId++) {
			ret[letterId] = getNextLetter();
		
			if(ret[letterId]==null){
			
				return null;
				//ret[letterId]=	ret[letterId].getSimplified(SIMPLIFYFAKTOR);
			}else{
				ret[letterId]=	ret[letterId].getSimplified(SIMPLIFYFAKTOR);
			}
			
		}
	
		return ret;
	}
/*
 * Gibt den Pixelwert an der stelle x,y zurück. Im grid sind die werte im RGB Format abgelegt. Hier könnte man farbraum umrechnungen einstellen
 */

	/*
	 * die Prepare Methode lässt das bild zuerst verschwimmen um anschließend alle  hellen teile auszufiltern. Übrig bleiben die dunklen teile des Bildes, wobei störungen wie linien und krazter ausgefiltert werden können
	 */
/*
 * Sucht angefangen bei der aktullen Positiond en ncähsten letter und gibt ihn zurück
 */
	private Letter getNextLetter() {
		Letter ret = new Letter();

		int[][] letterGrid = new int[getWidth()][getHeight()];
		long[] rowAverage = new long[getWidth()];
		int[] rowPeak = new int[getWidth()];
		for (int i = 0; i < rowAverage.length; i++) {
			rowAverage[i] = 0;
			rowPeak[i] = Integer.MAX_VALUE;
		}

		int average = getAverage();
		int x;
		int noGapCount = 0;
	
		boolean lastOverPeak = false;
		for (x = lastletterX; x < getWidth(); x++) {
			int count = 0;
			for (int y = 0; y < getHeight(); y++) {
				int pixelValue;
				
				for (int line = 0; line < GAPWIDTHPEAK; line++) {
					if (getWidth() > x + line) {
						pixelValue = getPixelValue(x + line, y);
						if (pixelValue < rowPeak[x]) {
							rowPeak[x] = pixelValue;
						}
					}
				}

				for (int line = 0; line < GAPWIDTHAVERAGE; line++) {
					if (getWidth() > x + line) {
						rowAverage[x] += getPixelValue(x + line, y);
						count++;
					}
				}

				letterGrid[x][y] = getPixelValue(x, y);
			}
			rowAverage[x] /= (count);
			if ((x) >= LEFTPADDING) {
				boolean isGap = false;
				boolean isAverageGap;
				boolean isOverPeak;
				boolean isPeakGap;
				if (GAPANDAVERAGELOGIC) {
					isAverageGap = rowAverage[x] > (average * GAPDETECTIONAVERAGECONTRAST)
							|| !USEAVERAGEGAPDETECTION;
					
					isOverPeak = rowPeak[x] < (average * GAPDETECTIONPEAKECONTRAST);
					//DEBUG.trace(isOverPeak+" - "+rowPeak[x]+" -"+(average * GAPDETECTIONPEAKECONTRAST));
					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !USEPEAKGAPDETECTION;
					isGap = isAverageGap && isPeakGap;
				
				} else {
					isAverageGap = rowAverage[x] > (average * GAPDETECTIONAVERAGECONTRAST)
							&& USEAVERAGEGAPDETECTION;
					isOverPeak = rowPeak[x] < (average * GAPDETECTIONPEAKECONTRAST);
					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !USEPEAKGAPDETECTION;
					isGap = isAverageGap || isPeakGap;
				}
				lastOverPeak = isOverPeak;
				 //DEBUG.trace(": "+x+" - "+isAverageGap+":"+isPeakGap+" - "+noGapCount+">"+MINIMUMLETTERWIDTH);
				 //DEBUG.trace(rowAverage[x]+" - "+average * GAPDETECTIONAVERAGECONTRAST);
				if (isGap && noGapCount > MINIMUMLETTERWIDTH) {
					break;
				} else if (rowAverage[x] < (average * GAPDETECTIONAVERAGECONTRAST)) {
					// DEBUG.trace(x+"NO GAP: "+noGapCount);
					noGapCount++;
				}
			}

		}
		ret.setGrid(letterGrid);
	
		if(!ret.trim(lastletterX, x))return null;
		
		if(!ret.clean())return null;
		
		
		lastletterX = x+2;
	//DEBUG.trace("FOUND "+ x);
		
		gaps[Math.min(lastletterX,getWidth())]=true;
		return ret;
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
	public Icon getAsIconWithGaps(){
		File tempFile;
		saveImageasJpgWithGaps(tempFile=new File("tmp/img_"+(tmpFiles++)+".jpg"));
		tempFile.deleteOnExit();
		return (Icon) SYSTEM.getImageIcon(tempFile.getAbsolutePath(), "img",
				SYSTEM.ABSOLUTEPATH);
	}
	public void saveImageasJpgWithGaps(File file){
		BufferedImage bimg = null;

		bimg = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
		bimg.setRGB(0,0,getWidth(),getHeight(),getPixelWithGaps(),0,getWidth());

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
	public int[] getPixelWithGaps(){
		int[] pix = new int[getWidth() * getHeight()];
		int pixel=0;

		int avg=getAverage();
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel]=getPixelValue(x,y);
				if(gaps[x]==true)pix[pixel]=0;
				pixel++;
			}
		}
		return pix;
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
	public void printCaptcha() {
		DEBUG.trace("\r\n" + getString());
	}
/*
 * prüft ob ein pixel über der eingestellten schwelle ist
 */

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
		// Verschiebt die Peak erkennung
		file.setProperty("GAPWIDTHPEAK", GAPWIDTHPEAK+"");
		// gibt an über wieviel pixel die gapbreite per durchschnitt bestimmt
		// werden soll
		file.setProperty("GAPWIDTHAVERAGE", GAPWIDTHAVERAGE+"");
		// TRUE für UNd false für oder
		file.setProperty("GAPANDAVERAGELOGIC", GAPANDAVERAGELOGIC+"");
		// je kleiner desto leichter wird eine lücke als wirkliche lücke erkannt
		file.setProperty("GAPDETECTIONAVERAGECONTRAST", GAPDETECTIONAVERAGECONTRAST+"");
		// reihendurchschnittserkennung. Helle reihen werden erkannt
		file.setProperty("USEAVERAGEGAPDETECTION", USEAVERAGEGAPDETECTION+"");
		// Dunkelheitspeak wird erkennt. es wird die stelle bestimmt, bei der
		// keine dunklen punkte mehr kommen.
		file.setProperty("USEPEAKGAPDETECTION", USEPEAKGAPDETECTION+"");

		// Kontrasteinstelung für die peaks
		file.setProperty("GAPDETECTIONPEAKECONTRAST", GAPDETECTIONPEAKECONTRAST+"");
		// Anzahl der pixel die mindestens pro buchstabe gebraucht werden
		file.setProperty("MINIMUMLETTERWIDTH", MINIMUMLETTERWIDTH+"");
		// SW Bild wird erstellt gemittelt über XXX Pixel quatrate

		// einstellungen nicht beachtet
		file.setProperty("LEFTPADDING", LEFTPADDING+"");

	}
	public static void setProperties(Properties file) {
		GAPWIDTHPEAK=Integer.parseInt(file.getProperty("GAPWIDTHPEAK", GAPWIDTHPEAK+""));

		GAPWIDTHAVERAGE=Integer.parseInt(file.getProperty("GAPWIDTHAVERAGE", GAPWIDTHAVERAGE+""));
	
		GAPANDAVERAGELOGIC=file.getProperty("GAPANDAVERAGELOGIC", GAPANDAVERAGELOGIC+"").equals("true");

		GAPDETECTIONAVERAGECONTRAST=Double.parseDouble(file.getProperty("GAPDETECTIONAVERAGECONTRAST", GAPDETECTIONAVERAGECONTRAST+""));

		USEAVERAGEGAPDETECTION=file.getProperty("USEAVERAGEGAPDETECTION", USEAVERAGEGAPDETECTION+"").equals("true");

		USEPEAKGAPDETECTION=file.getProperty("USEPEAKGAPDETECTION", USEPEAKGAPDETECTION+"").equals("true");


		GAPDETECTIONPEAKECONTRAST=Double.parseDouble(file.getProperty("GAPDETECTIONPEAKECONTRAST", GAPDETECTIONPEAKECONTRAST+""));

		MINIMUMLETTERWIDTH=Integer.parseInt(file.getProperty("MINIMUMLETTERWIDTH", MINIMUMLETTERWIDTH+""));

		LEFTPADDING=Integer.parseInt(file.getProperty("LEFTPADDING", LEFTPADDING+""));
		
	}
	/*
	 * factory Methode für eine captchainstanz
	 */
	public static Captcha getCaptcha(Image image) {

		int width = image.getWidth(null);
		int height = image.getHeight(null);
		PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);
		try {
			pg.grabPixels();
		} catch (Exception e) {
			DEBUG.trace("ERROR: PixelGrabber exception");
		}
		
		Captcha ret = new Captcha(width, height);
		ret.setPixel((int[]) pg.getPixels());
		return ret;

	}
	
	public static Captcha getCaptcha(File file) {
		Image img= SWING.loadImage(file);
		return Captcha.getCaptcha(img);
		
		
	}
}