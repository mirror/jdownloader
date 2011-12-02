package org.jdownloader.controlling.filter;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;

public class CompiledPluginStatusFiler extends PluginStatusFilter {

    public CompiledPluginStatusFiler(PluginStatusFilter pluginStatusFilter) {
        super(pluginStatusFilter.getMatchType(), pluginStatusFilter.isEnabled(), pluginStatusFilter.getPluginStatus());

    }

    public boolean matches(LinkState linkState) {
        switch (getMatchType()) {
        case IS:
            switch (getPluginStatus()) {
            case PREMIUM:
                // TODO
                return true;
            case AUTOCAPTCHA:
                // TODO
                return true;

            }
        case ISNOT:

            switch (getPluginStatus()) {
            case PREMIUM:
                // TODO
                return false;
            case AUTOCAPTCHA:
                // TODO
                return false;

            }

        }

        return false;
    }

}
