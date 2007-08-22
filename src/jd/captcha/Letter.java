package jd.captcha;



/**
 * Diese Klasse beinhaltet alle Methoden für einzellne Letter.
 * 
 * @author coalado
 * 
 */
public class Letter extends PixelGrid {

	/*
	 * der decoded Value wird heir abgelegt
	 */
	private String decodedValue;

	/**
	 * Hash des sourcecaptchas (Fürs training wichtig)
	 */
	private String sourcehash;

	/**
	 * Wert gibt an mit welcher Sicherheit der letter erkannt wurde
	 */
	private int valityValue=-1;
/**
 * Gibt an wie oft dieser letter positov aufgefallen ist
 */
	private int goodDetections=0;
	/**
	 * Gibt an wie oft dieser letter negativ aufgefallen ist
	 */
	private int badDetections=0;
    /**
     * ID des letters in der Lettermap
     */
	public int id;
	private Letter parent;
    
	/**
	 * 
	 */
	public Letter() {
		super(0, 0);
	}

	/**
	 * gibt den Letter um den faktor faktor verkleinert zurück. Es wird ein
	 * Kontrastvergleich vorgenommen
	 * @param faktor 
	 * @return Vereinfachter Buchstabe
	 */
	public Letter getSimplified(int faktor) {
     
		int newWidth = (int) Math.ceil(getWidth() / faktor);
		int newHeight = (int) Math.ceil(getHeight() / faktor);
		Letter ret = new Letter();
		;
		ret.setOwner(this.owner);
		int avg = getAverage();
		int[][] newGrid = new int[newWidth][newHeight];
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
				setPixelValue(x, y, newGrid, isElement((int) v, avg) ? 0
						: (int) getMaxPixelValue(), this.owner);

			}
		}

		ret.setGrid(newGrid);

		ret.clean();

		return ret;
	}

	/**
	 * Entfernt die Reihen 0-left und right-ende aus dem interne Grid
	 * @param left 
	 * @param right 
	 * @return true bei Erfolg, sonst false
	 */
	public boolean trim(int left, int right) {
		int width = right - left;
		int[][] tmp = new int[width][getHeight()];
		if (getWidth() < right) {
			logger.severe("Letter dim: " + getWidth() + " - " + getHeight()
					+ ". Cannot trim to " + left + "-" + right);
			return false;
		}
		for (int x = 0; x < width; x++) {

			tmp[x] = grid[x + left];
		}
		grid = tmp;
		return true;
	}

	/**
	 * Setzt das grid aus einem TextString. PixelSeperator: * Zeilensperator: |
	 * 
	 * @param content
	 *            PixelString
	 * @return true/false
	 */
	public boolean setTextGrid(String content) {
		String[] code = content.split("\\|");
		grid = null;
		int width=0;
		for (int y = 0; y < code.length; y++) {
			String line = code[y];
            width=line.length();
			if (grid == null) {
				grid = new int[width][code.length];
				if (width < 2 || code.length < 2)
					return false;

			}
			for (int x = 0; x < width; x++) {
				grid[x][y] = Integer.parseInt(String.valueOf(line.charAt(x)))* getMaxPixelValue();
               
                    
			}
      }
		return true;

	}

	/**
	 * Gibt den Pixelstring zurück. Pixelsep: * ZeilenSep: |
	 * 
	 * @return Pixelstring 1*0*1|0*0*1|...
	 */
	public String getPixelString() {

		String ret = "";

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				ret += (int) (getPixelValue(x, y) / getMaxPixelValue())
						+ "";

			}
			
			ret += "|";
		}
		ret = ret.substring(0, ret.length() - 1);
		return ret;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(JAntiCaptcha owner) {
		this.owner = owner;
	}

	/**
	 * 
	 * @param value
	 */
	public void setValityValue(int value) {
		this.valityValue = value;

	}

	/**
	 * @return the decodedValue
	 */
	public String getDecodedValue() {
		return decodedValue;
	}

	/**
	 * @param decodedValue
	 *            the decodedValue to set
	 */
	public void setDecodedValue(String decodedValue) {
		this.decodedValue = decodedValue;
	}

	/**
	 * @return the sourcehash
	 */
	public String getSourcehash() {
		return sourcehash;
	}

	/**
	 * @param sourcehash
	 *            the sourcehash to set
	 */
	public void setSourcehash(String sourcehash) {
		this.sourcehash = sourcehash;
	}

	/**
	 * @return the valityValue
	 */
	public int getValityValue() {
		return valityValue;
	}

	/**
	 * @return Anzahl der Erfolgreichen Erkennungen durch diesen letter
	 */
	public int getGoodDetections() {
		return goodDetections;
		
	}

	/**
	 * @return the badDetections
	 */
	public int getBadDetections() {
		return badDetections;
	}

	/**
	 * @param badDetections the badDetections to set
	 */
	public void setBadDetections(int badDetections) {
		this.badDetections = badDetections;
	}

	/**
	 * @param goodDetections the goodDetections to set
	 */
	public void setGoodDetections(int goodDetections) {
		this.goodDetections = goodDetections;
	}

	/**
	 * @return the parent
	 */
	public Letter getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Letter parent) {
		this.parent = parent;
	}

	/**
	 * Addiert eine gute Erkennung zu dem letter
	 */
	public void markGood() {
		this.goodDetections++;
		logger.warning("GOOD detection : ("+this.toString()+") ");
	}
/**
 * Addiert eine Schlechte Erkennung
 *
 */
	public void markBad() {
		this.badDetections++;
		logger.warning(getValityPercent()+"__");
		logger.warning("Bad detection : ("+this.toString()+") ");
		
	}
	public String toString(){
		return this.getDecodedValue()+" ["+this.getSourcehash()+"]["+this.getGoodDetections()+"/"+this.getBadDetections()+"]";
	}
	/**
	 * @return Ein Wert von 0(Perfekte Erkennung) bis 100 (Schlechte Erkennung)
	 */
	public int getValityPercent(){
		if(this.valityValue<0){
            return 100;
        }
		return (int)((100.0*(double)this.valityValue)/(double)this.getMaxPixelValue());
	}
/**
 * Versucht den Buchstaben automatisch auszurichten. Als kriterium dient das minimale Breite/Höhe Verhältniss
 * Es wird zuerst die optimale DRehrichtung ermittelt und dann gedreht. Die Methode funktioniert nicht immer zuverlässig. 
 * align(double contrast,double objectContrast,int angleA, int angleB) braucht länger, liefert aber bessere Ergebnisse
 * @param objectContrast
 * @return Ausgerichteter Buchstabe
 */
    public Letter align(double objectContrast) {
        PixelObject obj= this.toPixelObject(objectContrast);
     
        PixelObject aligned= obj.align();
        Letter newLetter=aligned.toLetter();
        this.setGrid(newLetter.grid);
        return this;
        
    }
    /**
     * Gibt einen Ausgerichteten Buchstaben zurück. Es wird vom Winkel angleA bis angleB nach der Besten Ausrichtung (Breite/Höhe) gesucht.
     * Ist zuverlässiger als align(double contrast,double objectContrast)
     * @param objectContrast
     * @param angleA
     * @param angleB
     * @return Ausgerichteter Buchstabe
     */
    public Letter align(double objectContrast,int angleA, int angleB) {
       PixelObject obj= this.toPixelObject(objectContrast);
    
         PixelObject aligned= obj.align(angleA,angleB);
         Letter newLetter=aligned.toLetter();
    
      this.setGrid(newLetter.grid);
      return this;
         
     }

    private PixelObject toPixelObject(double objectContrast) {
        PixelObject  object = new PixelObject(this);        
        object.setWhiteContrast(objectContrast);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
             if(getPixelValue(x,y)<(getMaxPixelValue()*objectContrast)){
                 object.add(x, y, getPixelValue(x,y));
             }
            }
            }
        
        return object;
    }

}