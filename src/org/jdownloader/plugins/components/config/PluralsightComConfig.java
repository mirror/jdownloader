package org.jdownloader.plugins.components.config;

import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pluralsight.com", type = Type.CRAWLER)
public interface PluralsightComConfig extends PluginConfigInterface {
    /** 2021-07-21: Removed "fastLinkcheck" setting for now and so far we never had subtitles support --> Removed subtitle setting too */
    // public static class TRANSLATION {
    // // public String isDownloadSubtitles_label() {
    // // return "Download Subtitles : ";
    // // }
    // public String isDownloadSubtitles_label() {
    // return _JDT.T.lit_add_subtitles();
    // }
    //
    // public String getDownloadSubtitles_label() {
    // return _JDT.T.lit_add_subtitles();
    // }
    // }
    //
    // public static TRANSLATION TRANSLATION = new TRANSLATION();
    //
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // boolean isDownloadSubtitles();
    //
    // void setDownloadSubtitles(boolean b);
    //
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // public boolean isFastLinkCheckEnabled();
    //
    // public void setFastLinkCheckEnabled(boolean b);
}
