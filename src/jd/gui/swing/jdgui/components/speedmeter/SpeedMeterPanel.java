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

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.events.ConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.Graph;
import org.appwork.utils.swing.graph.Limiter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class SpeedMeterPanel extends Graph implements ConfigEventListener {

    private static final long serialVersionUID = 5571694800446993879L;

    private Limiter           speedLimiter;

    private GeneralSettings   config;

    public SpeedMeterPanel(boolean contextMenu, boolean start) {
        super();

        SpeedMeterConfig sConfig = JsonConfig.create(SpeedMeterConfig.class);
        this.setCapacity(sConfig.getTimeFrame() * sConfig.getFramesPerSecond());
        this.setInterval(1000 / sConfig.getFramesPerSecond());
        setColorA(new Color((int) Long.parseLong(sConfig.getCurrentColorA(), 16), true));
        setColorB(new Color((int) Long.parseLong(sConfig.getCurrentColorB(), 16), true));
        setAverageColor(new Color((int) Long.parseLong(sConfig.getAverageGraphColor(), 16), true));
        Color col = new JLabel().getForeground();
        setAverageTextColor(col);
        setTextColor(col);
        setOpaque(false);
        Color a = new Color((int) Long.parseLong(sConfig.getLimitColorA(), 16), true);
        Color b = new Color((int) Long.parseLong(sConfig.getLimitColorB(), 16), true);
        speedLimiter = new Limiter(a, b);
        config = JsonConfig.create(GeneralSettings.class);
        speedLimiter.setValue(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
        GeneralSettings.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(this, true);
        GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(this, true);
        setLimiter(new Limiter[] { speedLimiter });
        if (start) start();
    }

    public void onConfigValidatorError(KeyHandler<?> keyHandler, Throwable validateException) {
    }

    public void onConfigValueModified(KeyHandler<?> keyHandler, Object newValue) {

        if (keyHandler == GeneralSettings.DOWNLOAD_SPEED_LIMIT || keyHandler == GeneralSettings.DOWNLOAD_SPEED_LIMIT_ENABLED) {
            speedLimiter.setValue(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
        }
    }

    protected String createTooltipText() {
        if (config.isDownloadSpeedLimitEnabled() && config.getDownloadSpeedLimit() > 0) {
            return getAverageSpeedString() + "  " + getSpeedString() + "\r\n" + _GUI._.SpeedMeterPanel_createTooltipText_(SizeFormatter.formatBytes(config.getDownloadSpeedLimit()));
        } else {
            return getAverageSpeedString() + "  " + getSpeedString();
        }
    }

    @Override
    public int getValue() {
        return DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
    }

}