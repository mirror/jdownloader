package jd.captcha;

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


public class Captcha extends PixelGrid {



	private int lastletterX = 0;
	public int prepared=0;
	private boolean[] gaps;
	public Letter[] decodedLetters;
	public int valityValue;


	/*
	 * Diese Klasse beinhaltet ein 2D-Pixel-Grid. Sie stellt mehrere Methoden
	 * zur verfügung dieses Grid zu bearbeiten Um Grunde sind hier alle Methoden
	 * zu finden um ein captcha als ganzes zu bearbeiten
	 */
	public Captcha(int width, int height) {
		super(width, height);
		gaps = new boolean[width + 1];

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

	public Captcha getSimplified(int faktor) {
		int newWidth = (int) Math.ceil(getWidth() / faktor);
		int newHeight = (int) Math.ceil(getHeight() / faktor);
		Captcha ret = new Captcha(newWidth, newHeight);
		int[][] newGrid = new int[newWidth][newHeight];
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
				
				
				PixelGrid.setPixelValue(x,y,newGrid,isElement((int) v, avg) ? 0
						: (int) getMaxPixelValue(),owner);
		
			}
		}

		ret.setGrid(newGrid);

		ret.printGrid();

		return ret;
	}

	/*
	 * Versucht die Buchstaben aus dem captcha zu extrahieren und gibt ein
	 * letter-array zuück
	 */
	public Letter[] getLetters(int letterNum) {
		Letter[] ret = new Letter[letterNum];
		lastletterX=owner.getLeftPadding();
	
		for (int letterId = 0; letterId < letterNum; letterId++) {
			ret[letterId] = getNextLetter(letterId);

			if (ret[letterId] == null) {

				return null;
				// ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
			} else {
				ret[letterId] = ret[letterId].getSimplified(this.owner.getSimplifyFaktor());
			}

		}

		return ret;
	}

	/*
	 * Gibt den Pixelwert an der stelle x,y zurück. Im grid sind die werte im
	 * RGB Format abgelegt. Hier könnte man farbraum umrechnungen einstellen
	 */

	/*
	 * die Prepare Methode lässt das bild zuerst verschwimmen um anschließend
	 * alle hellen teile auszufiltern. Übrig bleiben die dunklen teile des
	 * Bildes, wobei störungen wie linien und krazter ausgefiltert werden können
	 */
	

	/*
	 * Sucht angefangen bei der aktullen Positiond en ncähsten letter und gibt
	 * ihn zurück
	 */
	private Letter getNextLetter(int letterId) {
		Letter ret = createLetter();
	
		int nextGap=-1;
		if(owner.getGaps()!=null&&owner.getGaps().length>letterId){
			nextGap=owner.getGaps()[letterId];
		}
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

				for (int line = 0; line < owner.getGapWidthPeak(); line++) {
					if (getWidth() > x + line) {
						pixelValue = getPixelValue(x + line, y);
						if (pixelValue < rowPeak[x]) {
							rowPeak[x] = pixelValue;
						}
					}
				}

				for (int line = 0; line < owner.getGapWidthAverage(); line++) {
					if (getWidth() > x + line) {
						rowAverage[x] += getPixelValue(x + line, y);
						count++;
					}
				}

				letterGrid[x][y] = getPixelValue(x, y);
			}
			rowAverage[x] /= (count);
			if(nextGap==-1){
			if ((x) >= owner.getLeftPadding()) {
				boolean isGap = false;
				boolean isAverageGap;
				boolean isOverPeak;
				boolean isPeakGap;
				if (owner.getGapAndAverageLogic()) {
					isAverageGap = rowAverage[x] > (average * owner.getGapDetectionAverageContrast())
							|| !owner.isUseAverageGapDetection();

					isOverPeak = rowPeak[x] < (average * owner.getGapDetectionPeakContrast());
				
					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !owner.isUsePeakGapdetection();
					isGap = isAverageGap && isPeakGap;

				} else {
					isAverageGap = rowAverage[x] > (average * owner.getGapDetectionAverageContrast())
							&& owner.isUseAverageGapDetection();
					isOverPeak = rowPeak[x] < (average * owner.getGapDetectionPeakContrast());
					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !owner.isUsePeakGapdetection();
					isGap = isAverageGap || isPeakGap;
				}
				lastOverPeak = isOverPeak;
			
				if (isGap && noGapCount > owner.getMinimumLetterWidth()) {
					break;
				} else if (rowAverage[x] < (average * owner.getGapDetectionAverageContrast())) {
					
					noGapCount++;
				}
			}}else{
				if(nextGap==x){
					break;
				}
			}

		}
			
		ret.setGrid(letterGrid);

		if (!ret.trim(lastletterX, x))
			return null;

		if (!ret.clean())
			return null;

		lastletterX = x;
		

		gaps[Math.min(lastletterX, getWidth())] = true;
		return ret;
	}



	public Image getImageWithGaps(int faktor){
	
		Image image;
	
		int[][] tmp=grid;
		grid=getGridWithGaps();
		image=getImage(faktor);
		grid=tmp;
		return image;
				
	}

	public void saveImageasJpgWithGaps(File file) {
		BufferedImage bimg = null;

		bimg = new BufferedImage(getWidth(), getHeight(),
				BufferedImage.TYPE_INT_RGB);
		bimg.setRGB(0, 0, getWidth(), getHeight(), getPixelWithGaps(), 0,
				getWidth());

		// Encode as a JPEG
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

	public int[] getPixelWithGaps() {
		int[] pix = new int[getWidth() * getHeight()];
		int pixel = 0;

		int avg = getAverage();
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel] = getPixelValue(x, y);
				if (gaps[x] == true)
					pix[pixel] = 0;
				pixel++;
			}
		}
		return pix;
	}
	public int[][] getGridWithGaps() {
		int[][] pix = new int[getWidth()][getHeight()];
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[x][y]=grid[x][y];
				if (gaps[x] == true)
					pix[x][y] = 0;
			
			}
		}
		return pix;
	}
	public int[] getPixel() {
		int[] pix = new int[getWidth() * getHeight()];
		int pixel = 0;

		int avg = getAverage();
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel] = getPixelValue(x, y);
				pixel++;
			}
		}
		return pix;
	}

	/*
	 * Gibt ein ACSI bild des Captchas aus
	 */
	public void printCaptcha() {
		UTILITIES.trace("\r\n" + getString());
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

				ret += isElement(getPixelValue(x, y), avg) ? "*" : (int) Math
						.floor(9 * (getPixelValue(x, y) / getMaxPixelValue()));

			}
			ret += "\r\n";
		}

		return ret;

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
			UTILITIES.trace("ERROR: PixelGrabber exception");
		}

		Captcha ret = new Captcha(width, height);
		ret.setPixel((int[]) pg.getPixels());
		return ret;

	}
/*Captcha.getCaptcha(Letter a, Letter b)
 * Gibt einen captcha zurück der aus a +6pxTrennlinie +b besteht
 * 
 */
	public static Captcha getCaptcha(Letter a, Letter b) {
		
		int newWidth = (a.getWidth() + b.getWidth() + 6);
		int newHeight = Math.max(a.getHeight(), b.getHeight());
		Captcha ret = new Captcha(newWidth, newHeight);
		ret.grid = new int[newWidth][newHeight];
		for (int x = 0; x < a.getWidth(); x++) {
			for (int y = 0; y < newHeight; y++) {
				ret.grid[x][y] = y < a.getHeight() ? a.grid[x][y] : (int) a
						.getMaxPixelValue();

			}
		}
		for (int x = a.getWidth(); x < a.getWidth() + 6; x++) {
			for (int y = 0; y < newHeight; y++) {
				ret.grid[x][y] = (x==a.getWidth()+2 ||x==a.getWidth()+3)?0:(int)a.getMaxPixelValue();

			}
		}
	
		for (int x = a.getWidth() + 6; x < newWidth; x++) {
			for (int y = 0; y < newHeight; y++) {
				ret.grid[x][y] = y < b.getHeight() ? b.grid[x
						- (a.getWidth() + 6)][y] : (int) b.getMaxPixelValue();

			}
		}
		return ret;

	}

	public static Captcha getCaptcha(File file) {
		Image img = UTILITIES.loadImage(file);
		return Captcha.getCaptcha(img);

	}

	public void setDecodedLetters(Letter[] newLetters) {
		this.decodedLetters=newLetters;
		
	}
	public void setValityValue(int value){
		this.valityValue=value;
	}
}