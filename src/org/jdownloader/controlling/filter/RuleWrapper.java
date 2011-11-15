package org.jdownloader.controlling.filter;

import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;

public class RuleWrapper<T extends FilterRule> {

    protected CompiledRegexFilter fileNameRule;
    protected boolean             requiresLinkcheck = false;

    public RuleWrapper(T rule2) {
        this.rule = rule2;
        if (rule.getOnlineStatusFilter().isEnabled()) {
            onlineStatusFilter = new CompiledOnlineStatusFiler(rule.getOnlineStatusFilter());
            requiresLinkcheck = true;
        }

        if (rule.getFilenameFilter().isEnabled()) {
            fileNameRule = new CompiledRegexFilter(rule.getFilenameFilter());
            requiresLinkcheck = true;
        }
        if (rule.getFilesizeFilter().isEnabled()) {
            filesizeRule = new CompiledFilesizeFilter(rule.getFilesizeFilter());
            requiresLinkcheck = true;
        }
        if (rule.getFiletypeFilter().isEnabled()) {
            filetypeFilter = new CompiledFiletypeFilter(rule.getFiletypeFilter());
            requiresLinkcheck = true;
        }

        if (rule.getHosterURLFilter().isEnabled()) {
            hosterRule = new CompiledRegexFilter(rule.getHosterURLFilter());
            requiresHoster = true;
        }
        if (rule.getSourceURLFilter().isEnabled()) {
            sourceRule = new CompiledRegexFilter(rule.getSourceURLFilter());

        }
    }

    public CompiledRegexFilter getFileNameRule() {
        return fileNameRule;
    }

    public boolean isRequiresLinkcheck() {
        return requiresLinkcheck;
    }

    public boolean isRequiresHoster() {
        return requiresHoster;
    }

    public CompiledRegexFilter getHosterRule() {
        return hosterRule;
    }

    public CompiledRegexFilter getSourceRule() {
        return sourceRule;
    }

    public CompiledFilesizeFilter getFilesizeRule() {
        return filesizeRule;
    }

    public CompiledFiletypeFilter getFiletypeFilter() {
        return filetypeFilter;
    }

    protected boolean                   requiresHoster = false;
    protected CompiledRegexFilter       hosterRule;
    protected CompiledRegexFilter       sourceRule;
    protected CompiledFilesizeFilter    filesizeRule;
    protected CompiledFiletypeFilter    filetypeFilter;
    protected T                         rule;
    protected CompiledOnlineStatusFiler onlineStatusFilter;

    public T getRule() {
        return rule;
    }

    public CompiledOnlineStatusFiler getOnlineStatusFilter() {
        return onlineStatusFilter;
    }

    public static Pattern createPattern(String regex, boolean simpleRegex) {
        if (simpleRegex) {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } else {
            String[] parts = regex.split("\\*+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (sb.length() > 0) sb.append("(.*)");
                if (parts[i].length() != 0) sb.append(Pattern.quote(parts[i]));
            }
            if (sb.length() == 0) sb.append("(.*)");
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
    }

    public static Pattern createPattern(String regex) {
        return createPattern(regex, JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled());

    }

    public boolean checkFileType(CrawledLink link) {
        if (getFiletypeFilter() != null) {
            if (link.getLinkState() != LinkState.ONLINE) return false;
            String ext = Files.getExtension(link.getName());
            if (ext == null) return true;
            return getFiletypeFilter().matches(ext);
        }
        return true;
    }

    public boolean checkFileSize(CrawledLink link) {
        if (getFilesizeRule() != null) {
            // if (link.getDownloadLink().getDownloadSize() <= 0) return true;
            if (link.getLinkState() != LinkState.ONLINE) return false;
            return getFilesizeRule().matches(link.getSize());
        }
        return true;
    }

    public boolean checkFileName(CrawledLink link) {

        if (getFileNameRule() != null) {
            if (link.getLinkState() != LinkState.ONLINE) return false;

            return getFileNameRule().matches(link.getName());
        }
        return true;
    }

    public boolean checkHoster(CrawledLink link) throws NoDownloadLinkException {
        if (getHosterRule() != null) {
            if (link.getDownloadLink() == null) { throw new NoDownloadLinkException(); }
            return getHosterRule().matches(link.getURL());
        }
        return true;
    }

    public boolean checkSource(CrawledLink link) {
        CrawledLink p = link;
        if (getSourceRule() != null) {
            do {
                if (getSourceRule().matches(p.getURL())) { return true; }
            } while ((p = p.getSourceLink()) != null);
            return false;
        }
        return true;
    }

    public boolean checkOnlineStatus(CrawledLink link) {
        if (getOnlineStatusFilter() != null) { return getOnlineStatusFilter().matches(link.getLinkState()); }
        return true;
    }

    public String getName() {
        return rule.getName();
    }

    public boolean isEnabled() {
        return rule.isEnabled();
    }

}
