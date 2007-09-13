package jd.controlling;

import java.util.Vector;

import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

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

    public SpeedMeter(int average) {
        averageOver = 5000;
    }

    public void addValue(int value) {
      
          
                times.add(System.currentTimeMillis());
                entries.add(value);
               

     

    }
/**
 * Gibt die durschnittsgeschwindigkeit des letzten intervals zurück
 * @return speed
 */

    public int getSpeed() {
        if (times.size() < 2) return 0;
        long bytes = 0;
        long start = -1;
   
        long current = System.currentTimeMillis();

        for (int i = 0; i < times.size(); i++) {
            if ((current - times.elementAt(i)) > averageOver) {
                times.remove(i);
                entries.remove(i);
                i++;
            }
            else if (start == -1) {
                start = times.elementAt(i);
            }
            else {
                bytes += entries.elementAt(i);
            }
        }
        long dif=System.currentTimeMillis()-start;
if(dif==0)return 0;
        return (int) ((bytes * 1000) / dif);
    }

}
