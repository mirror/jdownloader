package jd.plugins.components;

import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;

public abstract class YoutubeReplacer {

    private final String[] tags;

    public String[] getTags() {
        return tags;
    }

    abstract public String getDescription();

    public YoutubeReplacer(String... tags) {
        this.tags = tags;
    }

    public String replace(String name, YoutubeHelperInterface helper, DownloadLink link) {
        for (String tag : tags) {
            String mod = new Regex(name, "\\*" + tag + "\\[(.+?)\\]\\*").getMatch(0);
            if (mod != null) {

                name = name.replaceAll("\\*" + tag + "(\\[[^\\]]+\\])\\*", getValue(link, helper, mod));
            }
            if (name.contains("*" + tag + "*")) {
                String v = getValue(link, helper, null);
                name = name.replace("*" + tag + "*", v == null ? "" : v);
            }

        }
        return name;
    }

    abstract protected String getValue(DownloadLink link, YoutubeHelperInterface helper, String mod);

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

    public boolean matches(String checkName) {
        for (String tag : tags) {
            if (checkName.contains("*" + tag + "*")) {
                return true;
            }
            if (Pattern.compile("\\*" + tag + "\\[(.+?)\\]\\*", Pattern.CASE_INSENSITIVE).matcher(checkName).find()) {
                return true;
            }

        }
        return false;
    }

}
