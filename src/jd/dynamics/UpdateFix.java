package jd.dynamics;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class UpdateFix extends DynamicPluginInterface {

    @Override
    public void execute() {
        long revision = Long.parseLong(JDUtilities.getRevision().replaceAll(",|\\.", ""));
        if (revision < 6035) { 
            JDLogger.getLogger().info("UpdateFix: workaround enabled!");
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        }
    }
}
