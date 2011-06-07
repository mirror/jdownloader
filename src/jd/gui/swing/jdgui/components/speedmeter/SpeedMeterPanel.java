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

import jd.controlling.DownloadWatchDog;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.Graph;
import org.appwork.utils.swing.graph.Limiter;
import org.jdownloader.settings.GeneralSettings;

public class SpeedMeterPanel extends Graph {

    private static final long serialVersionUID = 5571694800446993879L;

    private Limiter           speedLimiter;

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
        speedLimiter.setValue(JsonConfig.create(GeneralSettings.class).getDownloadSpeedLimit() * 1024);
        JsonConfig.create(GeneralSettings.class).getStorageHandler().getEventSender().addListener(new ConfigEventListener() {

            public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
                if ("downloadSpeedLimit".equalsIgnoreCase(key)) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {

                            resetAverage();
                            speedLimiter.setValue(JsonConfig.create(GeneralSettings.class).getDownloadSpeedLimit() * 1024);
                            // repaint immediately
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    repaint();
                                }
                            };
                        }
                    };
                }

            }

            public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
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