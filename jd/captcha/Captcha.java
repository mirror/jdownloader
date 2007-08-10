package jd.captcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class Captcha extends PixelGrid {

	/**
	 * Speichert die Positiond es letzten erkannten Letters
	 */
	private int lastletterX = 0;

	/**
	 * Speichert die Information ob der captcha schon vorverarbeitet wurde
	 */
	private boolean prepared = false;

	/**
	 * Falls der captcha schond decoded wurde, werden hier die gefundenen
	 * letters abgelegt
	 */
	private Letter[] decodedLetters;
/**
 * Temp Array für die getrennten letters; * 
 */
	private Letter[] seperatedLetters;
	/**
	 * Wert der angibt mit welcher Sicherheit der capture encoded wurde
	 */
	private int valityValue;

	/**
	 * Array der länge getWidth()+1. hier werden gefundene Gaps abgelegt.
	 * Einträge mit true bedeuten eine Lücke
	 */
	private boolean[] gaps;

	/**
	 * Diese Klasse beinhaltet ein 2D-Pixel-Grid. Sie stellt mehrere Methoden
	 * zur verfügung dieses Grid zu bearbeiten Um Grunde sind hier alle Methoden
	 * zu finden um ein captcha als ganzes zu bearbeiten
	 * 
	 * @author coalado
	 */
	public Captcha(int width, int height) {
		super(width, height);
		

	}

	/**
	 * Gibt die Breite des internen captchagrids zurück
	 */
	public int getWidth() {
		return grid.length;
	}

	/**
	 * Gibt die Höhe des internen captchagrids zurück
	 */
	public int getHeight() {
		if (grid.length == 0)
			return 0;
		return grid[0].length;
	}

	/**
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

	/**
	 * Errechnet einen Durchschnissfarbwert im angegebenen Bereich. Elemente der
	 * übergebenene Maske werden dabei vernachlässigt
	 * 
	 * @param px
	 * @param py
	 * @param width
	 * @param height
	 * @param mask
	 * @return durchschnittswert
	 */
	public int getAverage(int px, int py, int width, int height, Captcha mask) {
		long avg = 0;
		int i = 0;
		int halfW = width / 2;
		int halfH = height / 2;
		if (width == 1 && px == 0)
			width = 2;
		if (height == 1 && py == 0)
			height = 2;
		for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW,
				getWidth()); x++) {
			for (int y = Math.max(0, py - halfH); y < Math.min(py + height
					- halfH, getHeight()); y++) {
				if (mask.getPixelValue(x, y) > (getMaxPixelValue() * owner
						.getBlackPercent())) {
					avg = avg * i + getPixelValue(x, y);
					i++;
					avg /= i;
				}

			}
		}
		return (int) avg;
	}

	/**
	 * Entfernt Störungen über eine Maske und ersetzt diese mit den umliegenden
	 * pixeln
	 * 
	 * @param mask
	 *            Maske
	 * @param width
	 *            breite des Ersatzfeldes
	 * @param height
	 *            Höhe des Ersatzfeldes
	 */
	public void cleanWithMask(Captcha mask, int width, int height) {
		int[][] newgrid = new int[getWidth()][getHeight()];

		if (mask.getWidth() != getWidth() || mask.getHeight() != getHeight()) {
			logger.info("ERROR Maske und Bild passen nicht zusammmen");
			return;
		}

		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				if (mask.getPixelValue(x, y) < (getMaxPixelValue() * owner
						.getBlackPercent())) {
					newgrid[x][y] = getAverage(x, y, width, height, mask);

				} else {
					newgrid[x][y] = getPixelValue(x, y);
				}
			}
		}
		grid = newgrid;

	}

	/**
	 * Gibt einen vereifgachten captcha zurück. /gröber
	 * 
	 * @param faktor
	 *            der vereinfachung
	 * @return neuer captcha
	 */
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

				PixelGrid.setPixelValue(x, y, newGrid,
						isElement((int) v, avg) ? 0 : (int) getMaxPixelValue(),
						owner);

			}
		}

		ret.setGrid(newGrid);

		ret.printGrid();

		return ret;
	}

	/**
	 * Versucht die Buchstaben aus dem captcha zu extrahieren und gibt ein
	 * letter-array zuück
	 * 
	 * @param letterNum
	 *            Anzahl der vermuteten Buchstaben
	 * @return Array mit den gefundenen Lettern
	 */
	public Letter[] getLetters(int letterNum) {
		if(seperatedLetters!=null)return seperatedLetters;
		if (!owner.isUseAverageGapDetection() && !owner.isUsePeakGapdetection()
				&& owner.getGaps() != null)
			return getLetters(letterNum, owner.getGaps());
		this.gaps = new boolean[getWidth() + 1];
		Letter[] ret = new Letter[letterNum];
		lastletterX = owner.getLeftPadding();

		for (int letterId = 0; letterId < letterNum; letterId++) {
			ret[letterId] = getNextLetter(letterId);

			if (ret[letterId] == null) {
				if(owner.getGaps()!=null){
					return getLetters(letterNum, owner.getGaps());
				}else{
				return null;}
				// ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
			} else {
				ret[letterId] = ret[letterId].getSimplified(this.owner
						.getSimplifyFaktor());
			}

		}
		seperatedLetters=ret;
		return ret;
	}

	public Letter[] getLetters(int letterNum, int[] gaps) {
		if(seperatedLetters!=null)return seperatedLetters;
		Letter[] ret = new Letter[letterNum];
		lastletterX = owner.getLeftPadding();
		this.gaps = new boolean[getWidth() + 1];
		for (int letterId = 0; letterId < letterNum; letterId++) {
			ret[letterId] = getNextLetter(letterId, gaps);

			if (ret[letterId] == null) {

				return null;
				// ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
			} else {
				ret[letterId] = ret[letterId].getSimplified(this.owner
						.getSimplifyFaktor());
			}

		}
		seperatedLetters=ret;
		return ret;
	}
	/**
	 * Gibt in prozent zurück wie sicher die erkennung war (0. top sicher 100 schlecht)
	 * @return int validprozent
	 */
	public int getValityPercent(){
		if(this.valityValue<0)return 100;
		return (int)((100.0*(double)this.valityValue)/(double)this.getMaxPixelValue());
	}
	/**
	 * Sucht angefangen bei der aktullen Positiond en ncähsten letter und gibt
	 * ihn zurück
	 * 
	 * @param letterID:
	 *            Id des Letters (0-letterNum-1)
	 * @return Letter gefundener Letter
	 */
	private Letter getNextLetter(int letterId) {
		Letter ret = createLetter();

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

			if ((x) >= owner.getLeftPadding()) {
				boolean isGap = false;
				boolean isAverageGap;
				boolean isOverPeak;
				boolean isPeakGap;
				if (owner.getGapAndAverageLogic()) {
					isAverageGap = rowAverage[x] > (average * owner
							.getGapDetectionAverageContrast())
							|| !owner.isUseAverageGapDetection();

					isOverPeak = rowPeak[x] < (average * owner
							.getGapDetectionPeakContrast());

					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !owner.isUsePeakGapdetection();
					isGap = isAverageGap && isPeakGap;

				} else {
					isAverageGap = rowAverage[x] > (average * owner
							.getGapDetectionAverageContrast())
							&& owner.isUseAverageGapDetection();
					isOverPeak = rowPeak[x] < (average * owner
							.getGapDetectionPeakContrast());
					isPeakGap = (lastOverPeak && !isOverPeak)
							|| !owner.isUsePeakGapdetection();
					isGap = isAverageGap || isPeakGap;
				}
				lastOverPeak = isOverPeak;

				if (isGap && noGapCount > owner.getMinimumLetterWidth()) {
					break;
				} else if (rowAverage[x] < (average * owner
						.getGapDetectionAverageContrast())) {

					noGapCount++;
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
/**
 * Alternativ Methode über das gaps array.
 * @param letterId
 * @param gaps
 * @return
 */
	private Letter getNextLetter(int letterId, int[] gaps) {
		Letter ret = createLetter();

		int nextGap = -1;
		if (gaps != null && gaps.length > letterId) {
			nextGap = gaps[letterId];
		}
		int[][] letterGrid = new int[getWidth()][getHeight()];

		int x;

		boolean lastOverPeak = false;
		for (x = lastletterX; x < getWidth(); x++) {
			int count = 0;
			for (int y = 0; y < getHeight(); y++) {
				int pixelValue;

				letterGrid[x][y] = getPixelValue(x, y);
			}

			if (nextGap == x) {
				break;
			}

		}

		ret.setGrid(letterGrid);

		if (!ret.trim(lastletterX, x))
			return null;

		if (!ret.clean())
			return null;

		lastletterX = x;

		this.gaps[Math.min(lastletterX, getWidth())] = true;
		return ret;
	}

	/**
	 * Gibt den Captcha als Bild mit Trennzeilen für die gaps zurück
	 * 
	 * @param faktor
	 *            Vergrößerung
	 * @return neues Image
	 */

	public Image getImageWithGaps(int faktor) {

		Image image;

		int[][] tmp = grid;
		grid = getGridWithGaps();
		image = getImage(faktor);
		grid = tmp;
		return image;

	}

	/**
	 * Speichert den captcha als Bild mit Trennstrichen ab
	 * 
	 * @param file .
	 *            ZielPfad
	 */
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

			e.printStackTrace();
		} catch (ImageFormatException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/**
	 * Gibt ein pixelarray zurück. In das Pixelarray sind Trennstriche für die
	 * Gaps eingerechnet
	 * 
	 * @return Pixelarray
	 */
	public int[] getPixelWithGaps() {
		int[] pix = new int[getWidth() * getHeight()];
		int pixel = 0;

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

	/**
	 * Gibt ein pixelgrid zurück. Inkl.trennstrichen für die Gaps
	 * 
	 * @return int[][] PixelGrid
	 */
	public int[][] getGridWithGaps() {
		int[][] pix = new int[getWidth()][getHeight()];
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[x][y] = grid[x][y];
				if (gaps[x] == true)
					pix[x][y] = 0;

			}
		}
		return pix;
	}

	/**
	 * Gibt das Pixel Array zurück.
	 */
	public int[] getPixel() {
		int[] pix = new int[getWidth() * getHeight()];
		int pixel = 0;

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel] = getPixelValue(x, y);
				pixel++;
			}
		}
		return pix;
	}

	/**
	 * Gibt ein ACSI bild des Captchas aus
	 */
	public void printCaptcha() {
		logger.info("\r\n" + getString());
	}

	/**
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

	/**
	 * factory Methode für eine captchainstanz
	 */
	public static Captcha getCaptcha(Image image, JAntiCaptcha owner) {

		int width = image.getWidth(null);
		int height = image.getHeight(null);
		if (width <= 0 || height <= 0) {
			UTILITIES
					.trace("ERROR: Image nicht korrekt. Kein Inhalt. Pfad URl angaben Korrigieren");
		}
		PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);
		try {
			pg.grabPixels();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Captcha ret = new Captcha(width, height);
		ret.setOwner(owner);

		// if(owner.getImageType().equalsIgnoreCase("gif")){
		//			
		// ret.setPixel((byte[]) pg.getPixels());
		//			
		//			
		// }else{
		ret.setPixel((int[]) pg.getPixels());
		// }
		return ret;

	}

	private void setPixel(byte[] bs) {
		this.pixel = new int[bs.length];
		int i = 0;
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {

				this.pixel[i] = grid[x][y] = (int) ((double) (((double) bs[i]) / 127.0) * this
						.getMaxPixelValue());
				UTILITIES.trace(bs[i] + " - "
						+ (double) (((double) bs[i]) / 127.0) + " - "
						+ this.pixel[i] + " - " + this.getMaxPixelValue());
				i++;
			}
		}

	}

	/**
	 * Captcha.getCaptcha(Letter a, Letter b) Gibt einen captcha zurück der aus
	 * a +6pxTrennlinie +b besteht
	 * 
	 */
	public static Captcha getCaptcha(Letter a, Letter b) {

		int newWidth = (a.getWidth() + b.getWidth() + 6);
		int newHeight = Math.max(a.getHeight(), b.getHeight());
		Captcha ret = new Captcha(newWidth, newHeight);
		if (a.owner != null)
			ret.setOwner(a.owner);
		if (ret.owner == null)
			ret.setOwner(b.owner);
		if (ret.owner == null)
			ret.logger
					.warning("Owner konnte nicht bestimmt werden!Dieser captcha ist nur eingeschränkt verwendbar.");
		ret.grid = new int[newWidth][newHeight];
		for (int x = 0; x < a.getWidth(); x++) {
			for (int y = 0; y < newHeight; y++) {
				ret.grid[x][y] = y < a.getHeight() ? a.grid[x][y] : (int) a
						.getMaxPixelValue();

			}
		}
		for (int x = a.getWidth(); x < a.getWidth() + 6; x++) {
			for (int y = 0; y < newHeight; y++) {
				ret.grid[x][y] = (x == a.getWidth() + 2 || x == a.getWidth() + 3) ? 0
						: (int) a.getMaxPixelValue();

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

	/**
	 * Factory Funktion gibt einen captcha von File zurück
	 * 
	 * @param file
	 *            Pfad zum bild
	 * @param owner
	 *            Parameter Dump JAntiCaptcha
	 * @return Captcha neuer captcha
	 */
	public static Captcha getCaptcha(File file, JAntiCaptcha owner) {
		Image img = UTILITIES.loadImage(file);
		Captcha ret = Captcha.getCaptcha(img, owner);
		return ret;

	}

	/**
	 * Setter Methode für die decoded Letters
	 * 
	 * @param newLetters
	 */
	public void setDecodedLetters(Letter[] newLetters) {
		this.decodedLetters = newLetters;

	}

	/**
	 * Setter methode
	 * 
	 * @param value
	 */
	public void setValityValue(int value) {
		this.valityValue = value;
	}

	/**
	 * 
	 * @return the prepared
	 */
	public boolean isPrepared() {
		return prepared;
	}

	/**
	 * @param prepared
	 *            the prepared to set
	 */
	public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}

	/**
	 * @return the decodedLetters
	 */
	public Letter[] getDecodedLetters() {
		return decodedLetters;
	}

	/**
	 * @return the valityValue
	 */
	public int getValityValue() {
		return valityValue;
	}
}