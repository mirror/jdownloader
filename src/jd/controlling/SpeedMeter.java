package jd.controlling;

import java.util.Iterator;
import java.util.Vector;

/**
 * Diese Klasse kann einen Laufenden durschschnitt erstellen
 * 
 * @author JD-Team
 * 
 */
public class SpeedMeter {
	private long averageOver = 5000;

	boolean isRunning = false;
	boolean isListManagerRunning = false;
	private Vector<Object[]> entries = new Vector<Object[]>(200);

	/**
	 * KOnstruktor dem die Zeit übergeben werden kann über die der durchschnitt
	 * eführt wird
	 * 
	 * @param average
	 */
	public SpeedMeter(int average) {
		averageOver = average;
	}
	private void clearList()
	{
		int c = 0;
		while (isRunning && c != 1000)
		{
			c++;
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(isRunning)
		return;
		isRunning = true;
		long current = System.currentTimeMillis();
		Iterator<Object[]> iter = entries.iterator();
		while (iter.hasNext()) {
			Object[] element = (Object[]) iter.next();
			if ((current - (Long) element[0]) > averageOver) {
				iter.remove();
			} else {
				break;
			}

		}
		isRunning = false;
	}
	private void listManager() {
		if(!isListManagerRunning)
		{ 
		new Thread(new Runnable() {
			public void run() {
				isListManagerRunning=true;
				while (entries.size()>0)
				{
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Automatisch erstellter Catch-Block
						e.printStackTrace();
					}
					clearList();
				}
				isListManagerRunning=false;
			}
		}).start();
		}
	}
	/**
	 * Fügt einen weiteren wert hinzu
	 * 
	 * @param value
	 */
	public void addValue(final int value) {
			final long cur = System.currentTimeMillis();
			if(isRunning)
			{
			new Thread(new Runnable() {
				public void run() {
					int c = 0;
					while (isRunning && c != 1000)
					{
						c++;
						try {
							Thread.sleep(5);
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
			else
			{
				isRunning = true;
				entries.add(new Object[] { cur, value });
				isRunning = false;
			}
			listManager();
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
		while (isRunning && c != 1000)
		{
			c++;
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Automatisch erstellter Catch-Block
				e.printStackTrace();
			}
		}
		isRunning = true;
		long bytes = 0;
		long start = -1;

		long current = System.currentTimeMillis();
		Iterator<Object[]> iter = entries.iterator();
		while (iter.hasNext()) {
			Object[] element = (Object[]) iter.next();
			if ((current - (Long) element[0]) > averageOver) {
				iter.remove();
			} else {
				bytes += (Integer) element[1];
			}

		}
		if(entries.size()==0)
			return 0;
		start = (Long) entries.get(0)[0];
		long dif = current - start;
		isRunning = false;
		if (dif == 0)
			return 0;
		return (int) ((bytes * 1000) / dif);
	}

}
