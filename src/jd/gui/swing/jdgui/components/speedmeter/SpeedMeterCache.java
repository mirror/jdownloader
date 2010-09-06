//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.components.speedmeter;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;

public class SpeedMeterCache extends Thread implements Runnable {

    private static SpeedMeterCache INSTANCE = null;

    public static SpeedMeterCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpeedMeterCache();
            INSTANCE.start();
        }
        return INSTANCE;
    }

    private static final int CAPACITY = 40;

    private int index = 0;
    private int[] cache;

    private int window;

    private Eventsender<SpeedMeterListener, SpeedMeterEvent> broadcaster = new Eventsender<SpeedMeterListener, SpeedMeterEvent>() {

        @Override
        protected void fireEvent(SpeedMeterListener listener, SpeedMeterEvent event) {
            listener.onSpeedMeterEvent(event);
        }

    };

    private SpeedMeterCache() {
        super("SpeedMeterCache");

        cache = new int[CAPACITY];
        for (int x = 0; x < CAPACITY; x++) {
            cache[x] = 0;
        }

        window = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, 60);
        JDUtilities.getController().addControlListener(new ConfigPropertyListener(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE) {

            @Override
            public void onPropertyChanged(Property source, String key) {
                window = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, 60);
            }

        });
    }

    public Eventsender<SpeedMeterListener, SpeedMeterEvent> getBroadcaster() {
        return broadcaster;
    }

    public int getIndex() {
        return index;
    }

    public int[] getCache() {
        return cache;
    }

    @Override
    public void run() {
        long nextCacheEntry = 0;
        long tempValue = 0;
        long tempTime = 0;
        while (!this.isInterrupted()) {
            broadcaster.fireEvent(new SpeedMeterEvent(this, SpeedMeterEvent.UPDATED));

            if (nextCacheEntry < System.currentTimeMillis()) {
                if (tempValue == 0) {
                    cache[index] = 0;
                } else {
                    cache[index] = (int) (tempValue / tempTime);
                }
                tempValue = 0;
                tempTime = 0;
                index++;
                index = index % cache.length;
                nextCacheEntry = System.currentTimeMillis() + ((window * 1000) / CAPACITY);
            } else {
                tempValue += DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
                tempTime += 1;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

}
