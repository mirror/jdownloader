package org.jdownloader.plugins.components.youtube.converter;

import org.jdownloader.plugins.components.youtube.variants.VariantBase;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public interface YoutubeConverter {

    void run(DownloadLink downloadLink, PluginForHost plugin) throws Exception;

    double getQualityRating(VariantBase variantBase, double qualityRating);

}