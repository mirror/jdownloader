package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "sankakucomplex.com", type = Type.HOSTER)
public interface SankakucomplexComConfig extends PluginConfigInterface {
    final String                    text_SetCommaSeparatedTagsOfPostsAsComment = "Set comma separated tags of posts as comment?";
    final String                    text_BookTagCrawlerMaxPageLimit            = "Book tag crawler: Max page limit (-1 = no limit, 0 = disabled)";
    final String                    text_PostTagCrawlerMaxPageLimit            = "Post tag crawler: Max page limit (-1 = no limit, 0 = disabled)";
    public static final TRANSLATION TRANSLATION                                = new TRANSLATION();

    public static class TRANSLATION {
        public String getSetCommaSeparatedTagsOfPostsAsComment_label() {
            return text_SetCommaSeparatedTagsOfPostsAsComment;
        }

        public String getBookTagCrawlerMaxPageLimit_label() {
            return text_BookTagCrawlerMaxPageLimit;
        }

        public String getPostTagCrawlerMaxPageLimit_label() {
            return text_PostTagCrawlerMaxPageLimit;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_SetCommaSeparatedTagsOfPostsAsComment)
    @Order(10)
    boolean isSetCommaSeparatedTagsOfPostsAsComment();

    void setSetCommaSeparatedTagsOfPostsAsComment(boolean b);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 10000, step = 1)
    @DefaultIntValue(1)
    @DescriptionForConfigEntry(text_BookTagCrawlerMaxPageLimit)
    @Order(100)
    int getBookTagCrawlerMaxPageLimit();

    void setBookTagCrawlerMaxPageLimit(int pages);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 10000, step = 1)
    @DefaultIntValue(1)
    @DescriptionForConfigEntry(text_BookTagCrawlerMaxPageLimit)
    @Order(110)
    int getPostTagCrawlerMaxPageLimit();

    void setPostTagCrawlerMaxPageLimit(int pages);
    // public static enum LinkcheckMode implements LabelInterface {
    // API {
    // @Override
    // public String getLabel() {
    // return "Prefer API";
    // }
    // },
    // WEBSITE {
    // @Override
    // public String getLabel() {
    // return "Prefer website";
    // }
    // },
    // DEFAULT {
    // @Override
    // public String getLabel() {
    // return "Default: TODO";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("DEFAULT_1")
    // @Order(120)
    // @DescriptionForConfigEntry("TODO")
    // LinkcheckMode getStoryPackagenameSchemeType();
    //
    // void setStoryPackagenameSchemeType(final LinkcheckMode mode);
}