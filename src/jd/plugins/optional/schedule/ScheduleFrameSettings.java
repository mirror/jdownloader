package jd.plugins.optional.schedule;

import java.io.Serializable;
import java.util.Date;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

public class ScheduleFrameSettings implements Serializable {

    private static final long serialVersionUID = 2529016898978067651L;

    private String name;

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
        maxSpeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED);
        premium = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM);
        reconnect = !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT);
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
