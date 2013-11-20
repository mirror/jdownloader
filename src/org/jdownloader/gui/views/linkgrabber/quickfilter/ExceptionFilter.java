package org.jdownloader.gui.views.linkgrabber.quickfilter;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Hash;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.images.NewTheme;

public class ExceptionFilter extends Filter {

    private final String                       description;
    private final LinkgrabberFilterRuleWrapper wrapperRule;
    private final String                       id;

    public ExceptionFilter(LinkgrabberFilterRuleWrapper rule) {
        super(rule.getName());
        this.wrapperRule = rule;
        if (rule.getRule().getIconKey() != null) setIcon(NewTheme.I().getIcon(rule.getRule().getIconKey(), 16));
        id = "Custom_" + Hash.getMD5(rule.getName() + ":" + getDescription());
        enabled = filterSubConfig.getBooleanProperty(getID(), false);
        description = rule.getRule().toString();
    }

    @Override
    public String getDescription() {
        return description;
    }

    public LinkgrabberFilterRuleWrapper getWrapperRule() {
        return wrapperRule;
    }

    @Override
    protected String getID() {
        return id;
    }

    @Override
    public boolean isFiltered(CrawledLink link) {
        try {
            if (!wrapperRule.checkHoster(link)) return false;
        } catch (NoDownloadLinkException e) {
            throw new WTFException();
        }
        try {
            if (!wrapperRule.checkPluginStatus(link)) return false;
        } catch (NoDownloadLinkException e) {
            throw new WTFException();
        }
        if (!wrapperRule.checkOrigin(link)) return false;
        if (!wrapperRule.checkOnlineStatus(link)) return false;
        if (!wrapperRule.checkSource(link)) return false;
        if (!wrapperRule.checkFileName(link)) return false;
        if (!wrapperRule.checkPackageName(link)) return false;
        if (!wrapperRule.checkFileSize(link)) return false;
        if (!wrapperRule.checkFileType(link)) return false;
        return true;
    }

}
