package org.jdownloader.api.jd;

import jd.Main;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class JDAPIImpl implements JDAPI {

    public long uptime() {
        return System.currentTimeMillis() - Main.startup;
    }

    public long version() {
        return JDUtilities.getRevisionNumber();
    }

    public String log() {
        return JDLogger.getLog();
    }

}
