package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.Filter;
import org.jdownloader.gui.translate._GUI;

public class PluginStatusFilter extends Filter implements Storable {
    private PluginStatusMatchtype matchType    = PluginStatusMatchtype.IS;
    private PluginStatus          pluginStatus = PluginStatus.PREMIUM;

    public PluginStatusFilter() {
        // STorable
    }

    public PluginStatusMatchtype getMatchType() {
        return matchType;
    }

    public void setMatchType(PluginStatusMatchtype matchType) {
        this.matchType = matchType;
    }

    public PluginStatus getPluginStatus() {
        return pluginStatus;
    }

    public String toString() {
        switch (getMatchType()) {
        case IS:
            switch (getPluginStatus()) {
            case PREMIUM:
                return _GUI._.FilterRule_toString_premium();

            case AUTOCAPTCHA:
                return _GUI._.FilterRule_toString_autocaptcha();
            case NO_DIRECT_HTTP:
                return _GUI._.FilterRule_toString_directhttp_not();
            }
        case ISNOT:
            switch (getPluginStatus()) {
            case PREMIUM:
                return _GUI._.FilterRule_toString_premium_not();

            case AUTOCAPTCHA:
                return _GUI._.FilterRule_toString_autocaptcha_not();
            case NO_DIRECT_HTTP:
                return _GUI._.FilterRule_toString_directhttp();

            }
        }
        throw new WTFException();
    }

    public void setPluginStatus(PluginStatus pluginStatus) {
        this.pluginStatus = pluginStatus;
    }

    public PluginStatusFilter(PluginStatusMatchtype pluginStatusMatchtype, boolean selected, PluginStatus pluginStatus) {
        this.matchType = pluginStatusMatchtype;
        this.enabled = selected;
        this.pluginStatus = pluginStatus;
    }

    public static enum PluginStatusMatchtype {
        IS,
        ISNOT
    }

    public static enum PluginStatus {
        PREMIUM,
        AUTOCAPTCHA,
        NO_DIRECT_HTTP
    }

}
