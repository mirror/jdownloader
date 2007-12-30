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

    private Vector<Object[]> entries = new Vector<Object[]>();

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
        entries.add(new Object[] {System.currentTimeMillis(), value});
    }

    /**
     * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
     * 
     * @return speed
     */

    public int getSpeed() {
        if (entries.size() < 2) return 0;
        long bytes = 0;
        long start = -1;

        long current = System.currentTimeMillis();
        
        for (int i = entries.size()-1; i >= 0; i--) {
            Object[] elem = entries.elementAt(i);
            if ((current - (Long) elem[0]) > averageOver) {
                entries.remove(i);
            }
            else {
                start = (Long) elem[0];
                bytes += (Integer) elem[1];
            }
        }
        long dif = System.currentTimeMillis() - start;
        if (dif == 0) return 0;
        return (int) ((bytes * 1000) / dif);
    }

}
