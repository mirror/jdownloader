package org.jdownloader.api.jd;

import jd.Launcher;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class JDAPIImpl implements JDAPI {

    public long uptime() {
        return System.currentTimeMillis() - Launcher.startup;
    }

    public long version() {
        return JDUtilities.getRevisionNumber();
    }

    public String log() {
        return JDLogger.getLog();
    }

}
