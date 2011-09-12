package org.jdownloader.api.jd;

import jd.Main;
import jd.utils.JDUtilities;

public class JDAPIImpl implements JDAPI {

    public long getUptime() {
        return System.currentTimeMillis() - Main.startup;
    }

    public long getVersion() {
        return JDUtilities.getRevisionNumber();
    }

}
