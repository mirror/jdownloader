import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JOptionPane;



public class Letter extends PixelGrid {

	



	public String decodedValue;
	public String sourcehash;
	public int valityValue;

	public Letter() {
		super(0,0);
	}

	/*
	 * gibt den Letter umd en faktor faktor verkleinert zurück
	 */
	public Letter getSimplified(int faktor){
		int newWidth=(int)Math.ceil(getWidth()/faktor);
		int newHeight=(int)Math.ceil(getHeight()/faktor);
		Letter ret= new Letter();
;		ret.setOwner(this.owner);
		int avg=getAverage();
		int [][] newGrid=new int[newWidth][newHeight];
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
				setPixelValue(x,y,newGrid,isElement((int) v, avg) ? 0
						: (int) getMaxPixelValue(),this.owner);
			
			}
		}			
	
		ret.setGrid(newGrid);
	
		ret.clean();
		
		
		return ret;
	}
	public String getPixelString(){
	
		String ret = "";

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				ret += (int)(getPixelValue(x, y)/owner.getColorValueFaktor())+"*";

			}
			ret=ret.substring(0,ret.length()-1);
			ret += "|";
		}
		ret=ret.substring(0,ret.length()-1);
		return ret;
	}


	/*
	 * Entfernt die Reihen 0-left und right-ende aus dem interne Grid
	 */
	public boolean trim(int left, int right) {
		int width = right - left;
	//	DEBUG.trace("trim to "+width+" ("+left+" -"+right);
		//DEBUG.trace(getWidth()+" - "+getHeight());
		int[][] tmp = new int[width][getHeight()];
		if(getWidth()<right){
			UTILITIES.trace("ERROR: Letter dim: "+getWidth()+" - "+getHeight()+". Cannot trim to "+left+"-"+right);
			return false;
		}
		for (int x = 0; x < width; x++) {
			
		
			tmp[x] = grid[x + left];
		}
		grid = tmp;
		return true;
	}

/*
 * Setzt das interne PIxelgrid
 */



	
	
	public static void setCurrentProperties(Properties file) {
		
	}
	
	public static void setProperties(Properties file) {
		
		
	}

	public boolean setTextGrid(String content) {
		String[] code =content.split("\\|");
		grid= null;
	
		for (int y = 0; y < code.length; y++) {
			String[] line = code[y].split("\\*");
			if(grid==null){
				grid=new int[line.length][code.length];
				if(line.length<2 || code.length<2)return false;
				//DEBUG.trace("GRID MAX BY ["+(code.length-1)+"]["+(line.length-1)+"]");
			}
			for (int x = 0; x < line.length; x++) {
				//DEBUG.trace(x+" / "+y);
				
				//try{
				grid[x][y]=Integer.parseInt(line[x])*owner.getColorValueFaktor();
				//}catch(Exception e){
				//	grid[x][y]=(int)getMaxPixelValue();
				//}
			}
		}
		return true;
		
	}

	public void setSourceHash(String nodeValue) {
		this.sourcehash=nodeValue;
		
	}
	
	public void setDecoded(String nodeValue) {
		this.decodedValue=nodeValue;
		
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(CAntiCaptcha owner) {
		this.owner = owner;
	}

	public void setValityValue(int value) {
		this.valityValue=value;
		
	}


}