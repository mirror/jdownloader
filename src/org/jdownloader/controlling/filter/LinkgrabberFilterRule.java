package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.jdownloader.translate._JDT;

public class LinkgrabberFilterRule extends FilterRule implements Storable {

    public LinkgrabberFilterRule() {
        // required by Storable
    }

    private boolean accept;

    public void setAccept(boolean b) {
        accept = b;
    }

    public boolean isAccept() {
        return accept;
    }

    public LinkgrabberFilterRuleWrapper compile() {
        LinkgrabberFilterRuleWrapper ret = new LinkgrabberFilterRuleWrapper(this);
        return ret;
    }

    public LinkgrabberFilterRule duplicate() {
        LinkgrabberFilterRule ret = new LinkgrabberFilterRule();
        ret.accept = accept;
        ret.setEnabled(isEnabled());
        ret.setIconKey(getIconKey());
        ret.setFilenameFilter(getFilenameFilter());
        ret.setPackagenameFilter(getPackagenameFilter());
        ret.setFilesizeFilter(getFilesizeFilter());
        ret.setMatchAlwaysFilter(getMatchAlwaysFilter());
        ret.setFiletypeFilter(getFiletypeFilter());
        ret.setOnlineStatusFilter(getOnlineStatusFilter());
        ret.setOriginFilter(getOriginFilter());
        ret.setPluginStatusFilter(getPluginStatusFilter());
        ret.setHosterURLFilter(getHosterURLFilter());
        ret.setName(_JDT._.LinkgrabberFilterRule_duplicate(getName()));
        ret.setSourceURLFilter(getSourceURLFilter());

        return ret;
    }

}
