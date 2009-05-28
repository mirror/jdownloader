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

package jd.plugins.optional.schedule;

import java.io.Serializable;
import java.util.Date;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

public class ScheduleFrameSettings implements Serializable {

    private static final long serialVersionUID = 2529016898978067651L;

    private final String name;

    private int maxDls;

    private int maxSpeed;

    private boolean premium;

    private boolean reconnect;

    private boolean startStop;

    private Date time;

    private int repeat;

    public ScheduleFrameSettings(String name, boolean def) {
        this.name = name;
        if (def) initDefaultValues();
    }

    private void initDefaultValues() {
        maxDls = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
        maxSpeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
        premium = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        reconnect = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        startStop = false;
        time = null;
        repeat = 0;
    }

    public String getName() {
        return name;
    }

    public int getMaxDls() {
        return maxDls;
    }

    public void setMaxDls(int maxDls) {
        this.maxDls = maxDls;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public boolean isStartStop() {
        return startStop;
    }

    public void setStartStop(boolean startStop) {
        this.startStop = startStop;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

}
