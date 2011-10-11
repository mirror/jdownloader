package org.jdownloader.controlling.filter;

import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;

public class LinkgrabberFilterRuleWrapper {

    private CompiledRegexFilter fileNameRule;
    private boolean             requiresLinkcheck = false;

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

    public FilesizeFilter getFilesizeRule() {
        return filesizeRule;
    }

    public CompiledFiletypeFilter getFiletypeFilter() {
        return filetypeFilter;
    }

    private boolean                requiresHoster = false;
    private CompiledRegexFilter    hosterRule;
    private CompiledRegexFilter    sourceRule;
    private FilesizeFilter         filesizeRule;
    private CompiledFiletypeFilter filetypeFilter;
    private LinkgrabberFilterRule  rule;

    public LinkgrabberFilterRule getRule() {
        return rule;
    }

    public LinkgrabberFilterRuleWrapper(LinkgrabberFilterRule rule) {
        this.rule = rule;
        if (rule.getFilenameFilter().isEnabled()) {
            fileNameRule = new CompiledRegexFilter(rule.getFilenameFilter());
            requiresLinkcheck = true;
        }
        if (rule.getFilesizeFilter().isEnabled()) {
            filesizeRule = rule.getFilesizeFilter();
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

    public static Pattern createPattern(String regex) {
        if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } else {
            String[] parts = regex.split("\\*+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (sb.length() > 0) sb.append("(.*)");
                sb.append(Pattern.quote(parts[i]));
            }
            if (sb.length() == 0) sb.append("(.*)");
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }

    }

    public boolean checkFileType(CrawledLink link) {
        if (getFiletypeFilter() != null) {
            String ext = Files.getExtension(link.getName());
            if (ext == null) return true;
            return getFiletypeFilter().matches(ext);
        }
        return true;
    }

    public boolean checkFileSize(CrawledLink link) {
        if (getFilesizeRule() != null) {
            // if (link.getDownloadLink().getDownloadSize() <= 0) return true;
            return getFilesizeRule().matches(link.getSize());
        }
        return true;
    }

    public boolean checkFileName(CrawledLink link) {
        if (getFileNameRule() != null) { return getFileNameRule().matches(link.getName()); }
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
            } while ((p = p.getParentLink()) != null);
            return false;
        }
        return true;
    }

}
