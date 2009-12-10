package jd.dynamics;

import jd.OptionalPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class XTMWorkaround extends DynamicPluginInterface {

    @Override
    public void execute() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) {
            /* with next major jd update this workaround is no longer needed */
            JDLogger.getLogger().info("Disabling *Delete Files after Merge* of JDHJMerge addon, because of XTM issue! Next Major JD Version will support XTM!");
            try {
                SubConfiguration.getConfig("hjsplit").setProperty("REMOVE_MERGED", false);
                SubConfiguration.getConfig("hjsplit").save();
                OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("hjsplit");
                if (addon != null) {
                    addon.getPluginConfig().setProperty("REMOVE_MERGED", false);
                    addon.getPluginConfig().save();
                }
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
        }
    }

}
