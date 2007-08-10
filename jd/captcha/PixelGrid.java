package jd.captcha;

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

/**
 * Diese Klasse behinhaltet alle wichtigen Methoden um das Image-Pixelgrid zu bearbeiten
 * @author coalado
 *
 */

public class PixelGrid {

	/**
	 * ParameterDump
	 */
	public JAntiCaptcha owner;
	/**
	 * Internes grid
	 */
	public int[][] grid;
	/**
	 * Pixel Array
	 */
	public int[] pixel;
/**
 * Konstruktor
 * @param width Breite des Bildes in pixel
 * @param height Höhe des Bildes in Pixel
 */
	public PixelGrid(int width, int height) {
		grid = new int[width][height];

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
 * @param pixel  Pixel Array
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
 * Sollte je nach farbmodell den Höchsten pixelwert zurückgeben. RGB: 0xffffff
 * @return Pixelwert je nach Farbbereich
 */
	public long getMaxPixelValue() {
		return 1*owner.getColorValueFaktor();
	}
/**
 * Setzt den pixel value bei x,y. Umrechnungen werden dabei gemacht. deshalb kann nicht auf grid direkt zugegriffen werden. Grid beinhaltet roh daten
 * @param x
 * @param y
 * @param value
 */
	public void setPixelValue(int x,int y,int value){
		try{
		grid[x][y]=Color.HSBtoRGB((float)0.0, (float)0.0, (float)((double)value/owner.getColorValueFaktor()));
		}catch(ArrayIndexOutOfBoundsException e){
			UTILITIES.trace("ERROR: Nicht im grid; ["+x+"]["+y+"] grid: "+getWidth()+"/"+getHeight());
			e.printStackTrace();
		
		}
	}
	/**
	 * Static setPixelValue Funktion
	 * @param x
	 * @param y
	 * @param localGrid Grid inn der richtigen größe für x,y
	 * @param value Pixelwert
	 * @param owner JAntiCaptcha Instanz als Parameterdump
	 */
	public static void setPixelValue(int x,int y,int[][] localGrid, int value,JAntiCaptcha owner){
		try{
			localGrid[x][y]=Color.HSBtoRGB((float)0.0, (float)0.0, (float)((double)value/owner.getColorValueFaktor()));
		}catch(ArrayIndexOutOfBoundsException e){
			UTILITIES.trace("ERROR: Nicht im grid; ["+x+"]["+y+"] grid "+localGrid.length);
			e.printStackTrace();
		
		}
	}
/**
 * Gibt den Pixelwert an der stelle x,y zurück. Im grid sind die werte im RGB Format abgelegt. Hier könnte man farbraum umrechnungen einstellen
 * Dass die Parameter gleich bleiben sollte amn immerschauen das sman sich in einem wertebereich von 0-0xffffff bewegt. Je nach captchatyp kanne s erforderlich sein den farbraum anzupassen
 * @param x
 * @param y
 */
	public int getPixelValue(int x, int y) {
	
	try{
		Color c=new Color(grid[x][y]);
		float[] hsb=UTILITIES.rgb2hsb(c.getRed(),c.getGreen(),c.getBlue());	
		
		return (int)(hsb[owner.getHSBType()]*owner.getColorValueFaktor());
	}catch(ArrayIndexOutOfBoundsException e){
		UTILITIES.trace("ERROR: Nicht im grid; ["+x+"]["+y+"] grid: "+getWidth()+"/"+getHeight());
		e.printStackTrace();
		return (int)getMaxPixelValue();
	}

	}
/**
 * Gibt den Durchschnittlichen pixelwert des Bildes zurück
 * @return int
 */
	public int getAverage() {
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
	/**
	 * Gibt den Durschnittlichen Pixelwert  im angegebenen raum zurück
	 *
	 * @param px Position x
	 * @param py Position y
	 * @param width Breite des Ausschnitts
	 * @param height Höhe des Ausschnitts
	 * @return int Durchschnittswert
	 */
	public int getAverage(int px,int py,int width,int height) {
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
	/**
	 * Gibt den Durschnittlichen Pixelwert  im angegebenen raum zurück. Allerdings wird hier im Vergleich zu getAverage(int px,int py,int width,int height)  der Punkt slebet nicht mitberechnet
	 *
	 * @param px Position x
	 * @param py Position y
	 * @param width Breite des Ausschnitts
	 * @param height Höhe des Ausschnitts
	 * @return int Durchschnittswert
	 */
	public int getAverageWithoutPoint(int px,int py,int width,int height) {
		long avg = 0;
		int i = 0;
		int halfW=width/2;
		int halfH=height/2;
		if(width==1&&px==0)width=2;
		if(height==1&&py==0)height=2;

		for (int x =  Math.max(0,px-halfW); x < Math.min(px+width-halfW,getWidth()); x++) {
			for (int y = Math.max(0,py-halfH); y < Math.min(py+height-halfH,getHeight()); y++) {
				if(x!=px || y!=py){
					
				
				avg = avg * i + getPixelValue(x, y);
				i++;
				avg /= i;
				}

			}
		}
		return (int) avg;
	}
/**
 * Verwendet die SampleDown Methode um ein reines Schwarzweißbild zu erzeugen
 *
 */
	public void toBlackAndWhite() {
		sampleDown(1);
	}
	/**
	 * Erzeugt ein schwarzweiß bild
	 * @param faktor Schwellwert für die Kontrasterkennung
	 */
	public void toBlackAndWhite(double faktor) {
		sampleDown(1,faktor);
	}
	
	
	
/**
 * Macht das Bild gröber und sw. 
 * @param faktor Grobheit.
 */
	public void sampleDown(int faktor) {
		sampleDown(faktor,1.0);
		
	}
/**
 * Macht das Bild gröber und trifft über contrast eine sw-auswahl
 * @param faktor Grobheit
 * @param contrast Kontrastschwelle
 */

	public void sampleDown(int faktor,double contrast) {
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
						setPixelValue(newX,newY,newGrid,isElement((int) v, (int)(avg*contrast)) ? 0
								: (int) getMaxPixelValue(),owner);
					
					}
				}

			}
		}

		this.grid = newGrid;

	}
	/**
	 * Lässt das Bild verschwimmen
	 * @param faktor Stärke des Effekts
	 */
	public void blurIt(int faktor) {
		

		int[][] newGrid = new int[getWidth()][getHeight()];
		
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {			
				setPixelValue(x,y,newGrid,getAverage(x,y,faktor,faktor),owner);
			}
		}

		this.grid = newGrid;

	}
	/**
	 * Nimmt an der angegebenen Positiond en farbwert auf und entfernt desen aus dem ganzen Bild
	 * @param px
	 * @param py
	 * @param width
	 * @param height
	 */
	public void cleanBackgroundBySample(int px,int py,int width,int height){
		int avg=getAverage(px+width/2,py+height/2,width,height);
		
		int[][] newgrid=new int[getWidth()][getHeight()];
		
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				int dif=Math.abs(avg-getPixelValue(x,y));
				if(dif<(getMaxPixelValue()*owner.getBackgroundSampleCleanContrast())){
					newgrid[x][y]=(int)getMaxPixelValue();
				}else{
					newgrid[x][y]=getPixelValue(x,y);
				}

			}
		}
		grid=newgrid;
		
	}

	/**
	 * Gibt das Pixelgrid als Image zurück
	 * @return Image
	 */
	public Image getImage(){
	
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
/**
 * Gibt das Pixelgrid als vergrößertes Image zurück
 * @param faktor Vergrößerung
 * @return
 */
	public Image getImage(int faktor){
	
		BufferedImage image = new BufferedImage (getWidth()*faktor,getHeight()*faktor,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		
		for (int y = 0; y < getHeight()*faktor; y+=faktor) {
			for (int x = 0; x < getWidth()*faktor; x+=faktor) {
				graphics.setColor(new Color (getPixelValue(x/faktor,y/faktor)));
				graphics.fillRect(x, y, faktor, faktor);
			}
		}
		return image;
				
	}
	/**
	 * Entfernt weißes Rauschen
	 * @param faktor Stärke des Effekts
	 */
	public void reduceWhiteNoise(int faktor){
		reduceWhiteNoise(faktor, 1.0);
	}

	/**
	 * Entfernt weißes Rauschen
	 * @param faktor Prüfradius
	 * @param contrast Kontrasteinstellungen.je kleiner, desto mehr Pixel werden als störung erkannt, hat bei sw bildern kaum auswirkungen
	 */
	public void reduceWhiteNoise(int faktor, double contrast){
		int avg=getAverage();
		int[][] newGrid=new int[getWidth()][getHeight()];
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				//Korrektur weil sonst das linke obere PUxel schwarz wird.
				if(x==0 && y==0 && faktor<3){
					newGrid[0][0]=grid[0][0];
				}else{
					
				
				if(!isElement(getPixelValue(x,y),(int)(avg*contrast))){
					setPixelValue(x,y,newGrid,getAverageWithoutPoint(x,y,faktor,faktor),this.owner);			
				}
				}
			}
		}
		grid=newGrid;
	}
/**
 * Erstellt das negativ
 *
 */
	public void invert(){
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {			
					setPixelValue(x,y,(int)getMaxPixelValue()-getPixelValue(x,y));					
			}
		}
	}
	/**
	 * Entfernt Schwarze Störungen
	 * @param faktor Stärke
	 */
	public void reduceBlackNoise(int faktor){
		reduceBlackNoise(faktor, 1.0);
	}
	/**
	 * Entfernt schwarze Störungen
	 * @param faktor prüfradius
	 * @param contrast Kontrasteinstellungen
	 */
	public void reduceBlackNoise(int faktor, double contrast){
		int avg=getAverage();
		int[][] newGrid=new int[getWidth()][getHeight()];
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {

				if(x==0 && y==0 && faktor<3){
					newGrid[0][0]=grid[0][0];
				}else{					
				int localAVG=getAverageWithoutPoint(x,y,faktor,faktor);
				if(isElement(getPixelValue(x,y),(int)(avg*contrast))&&localAVG>=(contrast*0xffffff)){

					setPixelValue(x,y,newGrid,(int)(localAVG),this.owner);			
				}else{
					setPixelValue(x,y,newGrid,getPixelValue(x,y),this.owner);			
				}
				}
			}
		}
		grid=newGrid;
	}
	/**
	 * Speichert das Bild asl JPG ab
	 * @param file Zielpfad
	 */
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
	/**
	 * Gibt ein Pixelarray des internen Grids zurück
	 * @return Pixelarray
	 */
	public int[] getPixel(){
		int[] pix = new int[getWidth() * getHeight()];
		int pixel=0;		
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pix[pixel]=getPixelValue(x,y);
				pixel++;
			}
		}
		return pix;
	}
/**
 * Gibt ein ACSI bild des Captchas aus
 */
	public void printGrid() {
		UTILITIES.trace("\r\n" + getString());
	}
	public String getDim(){
		return "("+getWidth()+"/"+getHeight()+")";
	}
/**
 * Kontrasterkennung. Prüft ob der wert über einer Schwelle ist
 * @param value 
 * @param avg vergleichswet (meistens durchschnitsswert)
 * @return
 */
	public boolean isElement(int value, int avg) {
		return value < (avg * this.owner.getRelativeContrast());
	}
/**
 * Setzt das interne Pixelgrid
 * @param letterGrid int[][]
 */
	public void setGrid(int[][] letterGrid) {
		grid = letterGrid;
	}

	
/**
 * Entfernt von allen 4 Seiten die Zeilen und Reihen bis nur noch der content übrig ist
 * @return true/False
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
				
					if (isElement(getPixelValue(x, y), avg)) {
						rowIsClear = false;
						break;
					}
				}
				if (!rowIsClear)
					break;
				leftLines++;
			}
		
			
			
			
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
				UTILITIES.trace("ERROR: cleaning failed. nothing left1");
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
		
			if ((getWidth() - leftLines - rightLines) < 0
					|| (getHeight() - topLines - bottomLines) < 0) {
				UTILITIES.trace("ERROR: cleaning failed. nothing left");
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
	
/**
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

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(JAntiCaptcha owner) {
		this.owner = owner;
	}
	/**
	 * factory Funktion um einen Letter zu erstellen
	 * @return
	 */
	public Letter createLetter(){
		Letter ret= new Letter();
	
		ret.setOwner(owner);
		return ret;
	}


}