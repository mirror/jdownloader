package jd.controlling;

import java.util.Vector;

/**
 * Diese Klasse kann einen Laufenden durschschnitt erstellen
 * 
 * @author coalado
 * 
 */
public class SpeedMeter {
    private long            averageOver = 5000;

    private Vector<Long>    times       = new Vector<Long>();

    private Vector<Integer> entries     = new Vector<Integer>();

    /**
     * KOnstruktor dem die Zeit übergeben werden kann über die der durchschnitt eführt wird
     * @param average
     */
    public SpeedMeter(int average) {
        averageOver = average;
    }

    /**
     * Fügt einen weiteren wert hinzu
     * @param value
     */
    public void addValue(int value) {

        times.add(System.currentTimeMillis());
        entries.add(value);

    }

    /**
     * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
     * 
     * @return speed
     */

    public int getSpeed() {
        if (times.size() < 2) return 0;
        long bytes = 0;
        long start = -1;

        long current = System.currentTimeMillis();

        for (int i = times.size()-1; i >= 0; i--) {
            if ((current - times.elementAt(i)) > averageOver) {
                times.remove(i);
                entries.remove(i);

            }
            else {
                start = times.elementAt(i);
                bytes += entries.elementAt(i);
            }
        }
        long dif = System.currentTimeMillis() - start;
        if (dif == 0) return 0;
        return (int) ((bytes * 1000) / dif);
    }

}
