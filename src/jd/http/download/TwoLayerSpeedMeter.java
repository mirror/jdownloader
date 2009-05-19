//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http.download;

/**
 * Diese Klasse verwendet zwei sich überschneidende Zähler, um den mittleren
 * Speed über ein Interval möglichst gut berechnen zu können.
 * 
 * @author coalado
 */
public class TwoLayerSpeedMeter {

    private int interval;
    private long latestCheckTime;
    private long bytes;
    private long bytes2;

    private int firstInterval;
    private int lastInterval;
    private long speed;
    private static final int DIVISOR = 3;

    public TwoLayerSpeedMeter(int i) {
        this.interval = i;
        bytes = 0;
        bytes2 = 0;

        latestCheckTime = System.currentTimeMillis();
        this.firstInterval = interval / DIVISOR;
        this.lastInterval = 0;
    }

    public void update(int value) {
        try {
            long current = System.currentTimeMillis();
            if (current > latestCheckTime + interval) {
                latestCheckTime = current;
                bytes = bytes2;
                bytes2 = 0;
                lastInterval = interval * (DIVISOR - 1) / DIVISOR;
            }
            this.speed = (bytes * 1000) / (current - latestCheckTime + lastInterval);
            bytes += value;
            if (current > latestCheckTime + firstInterval) {
                bytes2 += value;
            }
        } catch (Exception e) {
        }
    }

    public long getSpeed() {
        return speed;
    }

}