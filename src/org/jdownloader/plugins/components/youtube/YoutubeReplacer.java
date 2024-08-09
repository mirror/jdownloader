package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;

public abstract class YoutubeReplacer {
    public static enum TagTasks {
        TO_UPPERCASE,
        TO_LOWERCASE,
        WHITESPACE_TO_UNDERSCORE
    }

    private final String[] tags;
    private DataOrigin[]   dataOrigins = null;

    public String[] getTags() {
        return tags;
    }

    abstract public String getDescription();

    public YoutubeReplacer(String... tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return String.valueOf(Arrays.asList(tags));
    }

    public String replace(String name, YoutubeHelper helper, DownloadLink link) {
        tagLoop: for (final String tag : tags) {
            modifyLoop: while (true) {
                String usedTag = new Regex(name, "\\*" + tag + "\\*").getMatch(-1);
                if (usedTag == null) {
                    usedTag = new Regex(name, "\\*" + tag + "\\[[^\\]]*\\]" + "\\*").getMatch(-1);
                    if (usedTag == null) {
                        continue tagLoop;
                    }
                }
                final ArrayList<TagTasks> performTasks = new ArrayList<TagTasks>();
                final String[] mods = new Regex(usedTag, "(?:\\[(.*?)\\])").getColumn(0);
                if (mods != null && mods.length > 0) {
                    String value = null;
                    for (final String mod : mods) {
                        // UC|LC. One or the other, but not both!
                        if ("UC".equalsIgnoreCase(mod) && !performTasks.contains(TagTasks.TO_LOWERCASE)) {
                            performTasks.add(TagTasks.TO_UPPERCASE);
                        } else if ("LC".equalsIgnoreCase(mod) && !performTasks.contains(TagTasks.TO_UPPERCASE)) {
                            performTasks.add(TagTasks.TO_LOWERCASE);
                        } else if ("SU".equalsIgnoreCase(mod)) {
                            performTasks.add(TagTasks.WHITESPACE_TO_UNDERSCORE);
                        } else {
                            // this has to be last!
                            value = getValue(link, helper, mod);
                        }
                    }
                    // dates are the only tags using [] without having easily parsible pattern,
                    if (value != null) {
                        // date format might be in text! So we should offer to reformat them also.
                        if (performTasks.contains(TagTasks.TO_LOWERCASE)) {
                            value = value.toLowerCase(Locale.ENGLISH);
                        } else if (performTasks.contains(TagTasks.TO_UPPERCASE)) {
                            value = value.toUpperCase(Locale.ENGLISH);
                        }
                        if (performTasks.contains(TagTasks.WHITESPACE_TO_UNDERSCORE)) {
                            value = value.replaceAll("\\s+", "_");
                        }
                        name = optimizedReduceSpacesReplace(name, usedTag, value);
                        continue modifyLoop;
                    }
                }
                String value = getValue(link, helper, null);
                if (value != null) {
                    if (value != null && performTasks.contains(TagTasks.TO_LOWERCASE)) {
                        value = value.toLowerCase(Locale.ENGLISH);
                    } else if (performTasks.contains(TagTasks.TO_UPPERCASE)) {
                        value = value.toUpperCase(Locale.ENGLISH);
                    }
                    if (performTasks.contains(TagTasks.WHITESPACE_TO_UNDERSCORE)) {
                        value = value.replaceAll("\\s+", "_");
                    }
                }
                if (StringUtils.isEmpty(value)) {
                    name = optimizedReduceSpacesReplace(name, usedTag, null);
                } else {
                    name = optimizedReduceSpacesReplace(name, usedTag, value);
                }
            }
        }
    return name;
    }

    private static String optimizedReduceSpacesReplace(final String input, final String search, final String replace) {
        if (replace != null) {
            return input.replace(search, replace);
        }
        final String qSearch = Pattern.quote(search);
        String ret = input.replaceAll("\\s" + qSearch + "((p|fps|kbit)?(_|\\-)?)?\\s", " ");
        ret = ret.replaceAll("^" + qSearch + "((p|fps|kbit)?\\s*(_|\\-)?)?\\s*", "");
        ret = ret.replaceAll("\\s*\\." + qSearch + "$", "");
        ret = ret.replaceAll(qSearch + "((p|fps|kbit)?(_|\\-)?)?", "");
        return ret;
    }

    abstract protected String getValue(DownloadLink link, YoutubeHelper helper, String mod);

    public boolean isExtendedRequired() {
        return false;
    }

    public static enum DataSource {
        WEBSITE,
        API_VIDEOS,
        API_USERS
    }

    public DataSource getDataSource() {
        return DataSource.WEBSITE;
    }

    public static enum DataOrigin {
        YT_CHANNEL,
        YT_PLAYLIST,
        YT_SINGLE_VIDEO
    }

    public DataOrigin[] getDataOrigins() {
        return this.dataOrigins;
    }

    public void setDataOrigins(final DataOrigin... dataOrigins) {
        this.dataOrigins = dataOrigins;
    }

    public boolean matches(String checkName) {
        for (String tag : tags) {
            if (Pattern.compile("\\*" + tag + "[^\\*]*\\*", Pattern.CASE_INSENSITIVE).matcher(checkName).find()) {
                return true;
            }
        }
        return false;
    }
}
