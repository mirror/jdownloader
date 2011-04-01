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

package jd.gui.swing.jdgui.components.speedmeter;

import java.awt.Color;

import javax.swing.JLabel;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JSonWrapper;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.Graph;
import org.appwork.utils.swing.graph.Limiter;

public class SpeedMeterPanel extends Graph {

    private static final long serialVersionUID = 5571694800446993879L;

    private Limiter           speedLimiter;

    public static void main(String[] args) {

    }

    public SpeedMeterPanel(boolean contextMenu, boolean start) {
        super();
        SpeedMeterConfig config = JsonConfig.create(SpeedMeterConfig.class);
        this.setCapacity(config.getTimeFrame() * config.getFramesPerSecond());
        this.setInterval(1000 / config.getFramesPerSecond());
        setColorA(new Color((int) Long.parseLong(config.getCurrentColorA(), 16), true));
        setColorB(new Color((int) Long.parseLong(config.getCurrentColorB(), 16), true));
        setAverageColor(new Color((int) Long.parseLong(config.getAverageGraphColor(), 16), true));
        Color col = new JLabel().getForeground();
        setAverageTextColor(col);
        setTextColor(col);
        setOpaque(false);
        Color a = new Color((int) Long.parseLong(config.getLimitColorA(), 16), true);
        Color b = new Color((int) Long.parseLong(config.getLimitColorB(), 16), true);
        speedLimiter = new Limiter(a, b);
        speedLimiter.setValue(JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024);
        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_DOWNLOAD_MAX_SPEED) {

            @Override
            public void onPropertyChanged(Property source, String key) {

                resetAverage();
                speedLimiter.setValue(source.getIntegerProperty(key, 0) * 1024);
                // repaint immediately
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        repaint();
                    }
                };
            }
        });
        setLimiter(new Limiter[] { speedLimiter });
        if (start) start();
    }

    @Override
    public int getValue() {
        return DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
    }

}