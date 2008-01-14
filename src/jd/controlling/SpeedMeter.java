package jd.controlling;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Diese Klasse kann einen Laufenden durschschnitt erstellen
 * 
 * @author coalado
 * 
 */
public class SpeedMeter {
	private long averageOver = 5000;

	boolean isRunning = false;

	private LinkedList<Object[]> entries = new LinkedList<Object[]>();

	/**
	 * KOnstruktor dem die Zeit übergeben werden kann über die der durchschnitt
	 * eführt wird
	 * 
	 * @param average
	 */
	public SpeedMeter(int average) {
		averageOver = average;
	}

	/**
	 * Fügt einen weiteren wert hinzu
	 * 
	 * @param value
	 */
	public void addValue(final int value) {
			final long cur = System.currentTimeMillis();
			new Thread(new Runnable() {
				public void run() {
					int c = 0;
					while (isRunning || c == 1000)
					{
						c++;
						try {
							Thread.sleep(0);
						} catch (InterruptedException e) {
							// TODO Automatisch erstellter Catch-Block
							e.printStackTrace();
						}
					}
					if(!isRunning)
					{
					isRunning = true;
					entries.add(new Object[] { cur, value });
					isRunning = false;
					}
				}
			}).start();
	}

	/**
	 * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
	 * 
	 * @return speed
	 */

	public int getSpeed() {
		int size = entries.size();
		if (size < 2)
			return 0;
		int c = 0;
		while (isRunning || c == 1000)
		{
			c++;
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				// TODO Automatisch erstellter Catch-Block
				e.printStackTrace();
			}
		}
		isRunning = true;
		long bytes = 0;
		long start = -1;

		long current = (Long) entries.get(size - 1)[0];
		Iterator<Object[]> iter = entries.iterator();
		while (iter.hasNext()) {
			Object[] element = (Object[]) iter.next();
			if ((current - (Long) element[0]) > averageOver) {
				iter.remove();
			} else {
				bytes += (Integer) element[1];
			}

		}
		start = (Long) entries.get(0)[0];
		long dif = current - start;
		isRunning = false;
		if (dif == 0)
			return 0;
		return (int) ((bytes * 1000) / dif);
	}

}
