//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.controlling;


/**
 * Diese Klasse kann einen Laufenden durschschnitt erstellen
 * 
 * @author JD-Team
 * 
 */
public class SpeedMeter {
	private static final int capacity = 5;
	
	private int c=0;
	private int lastSpeed = 0;
	private int[] speeds = new int[capacity];


//    private Logger logger;
	/**
	 * KOnstruktor dem die Zeit übergeben werden kann über die der durchschnitt
	 * eführt wird
	 * 
	 * @param average
	 */
	public SpeedMeter() {
//	    logger=JDUtilities.getLogger();
		for (int i = 0; i < capacity; i++) {
		    speeds[i]=-1;
		
			
		}
	}
	/**
	 * Fügt einen weiteren wert hinzu
	 * 
	 * @param value
	 */
	public void addSpeedValue(int value) {
	 
	
		
	
	    speeds[c]=value;
		c++;
		  if(c==capacity){
	            c=0;
	        }
		
		
	}

	/**
	 * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
	 * 
	 * @return speed
	 */

	public int getSpeed() {
	    /*
	    if(lastAccess+System.currentTimeMillis()<100)
            return lastSpeed;
    
        lastAccess=-System.currentTimeMillis();
        */
        long totalValue=0;
        int i = 0;
       
        while(i<capacity){
            if(speeds[i]==-1)break;          
            totalValue+=speeds[i];
            i++;            
        }
      
     if(i!=0)  lastSpeed=(int)(totalValue/i);
       //logger.info(totalValue+"/"+dif+"="+lastSpeed+" - "+(lastSpeed/1024));
        return lastSpeed;
	
	    /*
		//doppelzugriffe sind unnötig
		if(lastAccess+System.currentTimeMillis()<100)
			return lastSpeed;
	
		lastAccess=-System.currentTimeMillis();
		lastSpeed=0;
		int i = 0;
		while(i<capacity)
		{
			if(speed[i]==-1)break;
			lastSpeed+=speed[i++];
		}
		if(i!=0)
		lastSpeed/=i;
		return lastSpeed;
		*/
		
	}

}
