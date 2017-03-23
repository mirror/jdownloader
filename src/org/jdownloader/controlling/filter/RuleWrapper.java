package org.jdownloader.controlling.filter;

import java.util.regex.Pattern;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.DownloadLink;
import jd.plugins.LinkInfo;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.RegexFilter.MatchType;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;

public class RuleWrapper<T extends FilterRule> {

    private final CompiledRegexFilter        fileNameRule;
    private final CompiledPluginStatusFilter pluginStatusFilter;
    private final BooleanFilter              alwaysFilter;
    private final CompiledOriginFilter       originFilter;
    private final CompiledRegexFilter        packageNameRule;
    private final CompiledConditionFilter    conditionFilter;

    public CompiledPluginStatusFilter getPluginStatusFilter() {
        return pluginStatusFilter;
    }

    public RuleWrapper(T rule2) {
        this.rule = rule2;
        boolean requiresHoster = false;
        if (rule.getPluginStatusFilter().isEnabled()) {
            pluginStatusFilter = new CompiledPluginStatusFilter(rule.getPluginStatusFilter());
            requiresHoster = true;
        } else {
            pluginStatusFilter = null;
        }

        if (rule.getOnlineStatusFilter().isEnabled()) {
            onlineStatusFilter = new CompiledOnlineStatusFiler(rule.getOnlineStatusFilter());
        } else {
            onlineStatusFilter = null;
        }

        if (rule.getFilenameFilter().isEnabled()) {
            fileNameRule = new CompiledRegexFilter(rule.getFilenameFilter());
        } else {
            fileNameRule = null;
        }

        if (rule.getPackagenameFilter().isEnabled()) {
            packageNameRule = new CompiledRegexFilter(rule.getPackagenameFilter());
        } else {
            packageNameRule = null;
        }

        if (rule.getFilesizeFilter().isEnabled()) {
            filesizeRule = new CompiledFilesizeFilter(rule.getFilesizeFilter());
        } else {
            filesizeRule = null;
        }

        if (rule.getFiletypeFilter().isEnabled()) {
            filetypeFilter = new CompiledFiletypeFilter(rule.getFiletypeFilter());
        } else {
            filetypeFilter = null;
        }

        if (rule.getHosterURLFilter().isEnabled()) {
            hosterRule = new CompiledRegexFilter(rule.getHosterURLFilter());
            requiresHoster = true;
        } else {
            hosterRule = null;
        }

        if (rule.getSourceURLFilter().isEnabled()) {
            sourceRule = new CompiledRegexFilter(rule.getSourceURLFilter());
        } else {
            sourceRule = null;
        }

        if (rule.getOriginFilter().isEnabled()) {
            originFilter = new CompiledOriginFilter(rule.getOriginFilter());
        } else {
            originFilter = null;
        }

        if (rule.getConditionFilter().isEnabled()) {
            conditionFilter = new CompiledConditionFilter(rule.getConditionFilter());
        } else {
            conditionFilter = null;
        }

        if (rule.getMatchAlwaysFilter().isEnabled()) {
            alwaysFilter = rule.getMatchAlwaysFilter();
            // overwrites all others
            requiresHoster = false;
        } else {
            alwaysFilter = null;
        }
        this.requiresHoster = requiresHoster;
    }

    public CompiledConditionFilter getConditionFilter() {
        return conditionFilter;
    }

    public CompiledOriginFilter getOriginFilter() {
        return originFilter;
    }

    public BooleanFilter getAlwaysFilter() {
        return alwaysFilter;
    }

    public CompiledRegexFilter getFileNameRule() {
        return fileNameRule;
    }

    public CompiledRegexFilter getPackageNameRule() {
        return packageNameRule;
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

    private final boolean                   requiresHoster;
    private final CompiledRegexFilter       hosterRule;
    private final CompiledRegexFilter       sourceRule;
    private final CompiledFilesizeFilter    filesizeRule;
    private final CompiledFiletypeFilter    filetypeFilter;
    private final T                         rule;
    private final CompiledOnlineStatusFiler onlineStatusFilter;

    public T getRule() {
        return rule;
    }

    public CompiledOnlineStatusFiler getOnlineStatusFilter() {
        return onlineStatusFilter;
    }

    public static Pattern createPattern(String regex, boolean useRegex) {
        if (useRegex) {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } else {
            final String[] parts = regex.split("\\*+");
            final StringBuilder sb = new StringBuilder();
            if (regex.startsWith("*")) {
                sb.append("(.*)");
            }
            int actualParts = 0;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() != 0) {
                    if (actualParts > 0) {
                        sb.append("(.*?)");
                    }
                    sb.append(Pattern.quote(parts[i]));
                    actualParts++;
                }
            }
            if (sb.length() == 0) {
                sb.append("(.*?)");
            } else {
                if (regex.endsWith("*")) {
                    sb.append("(.*)");
                }
            }
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
    }

    public boolean checkFileType(final CrawledLink link) {
        final CompiledFiletypeFilter filetypeFilter = getFiletypeFilter();
        if (filetypeFilter != null) {
            final DownloadLink downloadLink = link.getDownloadLink();
            if (downloadLink != null) {
                final LinkInfo linkInfo = link.getLinkInfo();
                if (downloadLink.getFinalFileName() != null || downloadLink.getForcedFileName() != null) {
                    // filename available
                    return filetypeFilter.matches(linkInfo.getExtension().name(), linkInfo);
                } else if (link.getLinkState() == AvailableLinkState.ONLINE) {
                    // file is online
                    return filetypeFilter.matches(linkInfo.getExtension().name(), linkInfo);
                } else if (checkOnlineStatus(link)) {
                    // onlinestatus matches so we trust the available filename
                    return filetypeFilter.matches(linkInfo.getExtension().name(), linkInfo);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkFileSize(final CrawledLink link) {
        final CompiledFilesizeFilter fileSizeRule = getFilesizeRule();
        if (fileSizeRule != null) {
            final DownloadLink downloadLink = link.getDownloadLink();
            if (downloadLink != null) {
                if (downloadLink.getVerifiedFileSize() >= 0) {
                    return fileSizeRule.matches(downloadLink.getVerifiedFileSize());
                } else if (link.getLinkState() == AvailableLinkState.ONLINE) {
                    return fileSizeRule.matches(link.getSize());
                } else if (checkOnlineStatus(link)) {
                    return fileSizeRule.matches(link.getSize());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean checkPackageName(final CrawledLink link) {
        final CompiledRegexFilter packageNameRule = getPackageNameRule();
        if (packageNameRule != null) {
            String packagename = null;
            if (link != null) {
                if (link.getParentNode() != null) {
                    packagename = link.getParentNode().getName();
                }
                if (StringUtils.isEmpty(packagename) && link.getDesiredPackageInfo() != null) {
                    packagename = link.getDesiredPackageInfo().getName();
                }
            }
            if (StringUtils.isEmpty(packagename)) {
                return false;
            } else {
                return packageNameRule.matches(packagename);
            }
        } else {
            return true;
        }
    }

    public boolean checkFileName(final CrawledLink link) {
        final CompiledRegexFilter fileNameRule = getFileNameRule();
        if (fileNameRule != null) {
            final DownloadLink downloadLink = link.getDownloadLink();
            if (downloadLink != null) {
                if (downloadLink.getFinalFileName() != null || downloadLink.getForcedFileName() != null) {
                    // final or forced filename available
                    return fileNameRule.matches(link.getName());
                } else if (link.getLinkState() == AvailableLinkState.ONLINE) {
                    // file is online
                    return fileNameRule.matches(link.getName());
                } else if (checkOnlineStatus(link)) {
                    // onlinestatus matches so we trust the available filename
                    return fileNameRule.matches(link.getName());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean checkHoster(final CrawledLink link) {
        final CompiledRegexFilter hosterRule = getHosterRule();
        if (hosterRule != null) {
            final DownloadLink dlLink = link.getDownloadLink();
            if (dlLink == null || link.gethPlugin() == null) {
                return false;
            } else {
                final String host = dlLink.getServiceHost();
                switch (hosterRule.getMatchType()) {
                case CONTAINS:
                case EQUALS:
                    return (host != null && hosterRule.matches(host)) || hosterRule.matches(dlLink.getContentUrlOrPatternMatcher());
                case CONTAINS_NOT:
                case EQUALS_NOT:
                    return (host == null || hosterRule.matches(host)) && hosterRule.matches(dlLink.getContentUrlOrPatternMatcher());
                default:
                    return false;
                }
            }
        } else {
            return true;
        }
    }

    public boolean checkSource(CrawledLink link) {
        final CompiledRegexFilter sourceRule = getSourceRule();
        if (sourceRule != null) {
            String[] sources = link.getSourceUrls();
            int i = 1;
            final String pattern = sourceRule.getPattern().pattern();
            final boolean indexed = pattern.matches("^\\-?\\d+\\\\\\. .+");
            final boolean inverted = indexed && pattern.startsWith("-");
            if (sources == null || sources.length == 0) {
                /* the first link never has sourceURLs */
                sources = new String[2];
                sources[0] = LinkCrawler.cleanURL(link.getURL());
                LinkCollectingJob job = link.getSourceJob();
                if (job != null) {
                    sources[1] = LinkCrawler.cleanURL(job.getCustomSourceUrl());
                }
            }
            final MatchType matchType = sourceRule.getMatchType();
            Boolean matches = null;
            for (int j = inverted ? 0 : sources.length - 1; (inverted ? (j < sources.length) : (j >= 0)); j = (inverted ? (j + 1) : (j - 1))) {
                final String url = sources[j];
                if (url != null) {
                    final String toMatch = indexed ? (inverted ? "-" : "") + (i++) + ". " + url : url;
                    if (indexed) {
                        switch (sourceRule.getMatchType()) {
                        case EQUALS:
                        case EQUALS_NOT:
                            if (sourceRule.matches(url)) {
                                return true;
                            }
                            break;
                        default:
                            // nothing
                            break;
                        }
                    } else {
                        final boolean match = sourceRule.matches(toMatch);
                        switch (matchType) {
                        case EQUALS:
                        case CONTAINS:
                            return match;
                        case CONTAINS_NOT:
                        case EQUALS_NOT:
                            if (matches == null) {
                                matches = match;
                            } else {
                                matches = matches.booleanValue() && match;
                            }
                            break;
                        }
                    }
                }
            }
            if (matches != null) {
                return matches.booleanValue();
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkOnlineStatus(final CrawledLink link) {
        final CompiledOnlineStatusFiler onlineStatusFilter = getOnlineStatusFilter();
        if (onlineStatusFilter != null) {
            final AvailableLinkState linkState = link.getLinkState();
            if (AvailableLinkState.UNKNOWN == linkState) {
                return false;
            } else {
                return onlineStatusFilter.matches(linkState);
            }
        } else {
            return true;
        }
    }

    public boolean checkConditions(final CrawledLink link) {
        final CompiledConditionFilter conditionFiler = getConditionFilter();
        if (conditionFiler != null) {
            return conditionFiler.matches(link);
        } else {
            return true;
        }
    }

    public boolean checkOrigin(final CrawledLink link) {
        final CompiledOriginFilter originFiler = getOriginFilter();
        if (originFiler != null) {
            final LinkOriginDetails origin = link.getOrigin();
            if (origin == null) {
                return false;
            } else {
                return originFiler.matches(origin.getOrigin());
            }
        } else {
            return true;
        }
    }

    public boolean checkPluginStatus(final CrawledLink link) {
        final CompiledPluginStatusFilter pluginStatusFilter = getPluginStatusFilter();
        if (pluginStatusFilter != null) {
            if (link.getDownloadLink() == null || link.gethPlugin() == null) {
                return false;
            } else {
                return pluginStatusFilter.matches(link);
            }
        } else {
            return true;
        }
    }

    public String getName() {
        return rule.getName();
    }

    public boolean isEnabled() {
        return rule.isEnabled();
    }

}
