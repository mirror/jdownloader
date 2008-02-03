package jd.controlling;


/**
 * Diese Klasse kann einen Laufenden durschschnitt erstellen
 * 
 * @author JD-Team
 * 
 */
public class SpeedMeter {
	private int lastSpeed = 0;
	private long lastAccess = 0;
	private int c=0;
	private static final int capacity = 10;
	private long[] speed = new long[capacity];
	/**
	 * KOnstruktor dem die Zeit übergeben werden kann über die der durchschnitt
	 * eführt wird
	 * 
	 * @param average
	 */
	public SpeedMeter() {
		for (int i = 0; i < capacity; i++) {
			speed[i]=-1;
		}
	}
	/**
	 * Fügt einen weiteren wert hinzu
	 * 
	 * @param value
	 */
	public void addValue(long value, long difftime) {
		if(c==capacity)
			c=0;
		speed[c++]=value*1000/difftime;
	}

	/**
	 * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
	 * 
	 * @return speed
	 */

	public int getSpeed() {
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
		
	}

}
