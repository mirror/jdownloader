//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.DailyMotionVariant;
import jd.plugins.hoster.DailyMotionCom;
import jd.plugins.hoster.DailyMotionComV2;

import org.jdownloader.plugins.controller.LazyPlugin;

//Decrypts embedded videos from dailymotion
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dailymotion.com" }, urls = { "https?://(?:www\\.)?(dailymotion\\.com|dai\\.ly)/.+" })
public class DailyMotionComDecrypterV2 extends DailyMotionComDecrypter {
    public DailyMotionComDecrypterV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    private DailyMotionVariant getDailyMotionVariant(DownloadLink link) {
        final String name = link.getStringProperty(DailyMotionCom.PROPERTY_QUALITY_NAME);
        return new DailyMotionVariant(DailyMotionCom.getDirectlink(link), DailyMotionCom.getQualityHeight(link), null, name, name);
    }

    @Override
    public ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param, final String contenturl, final SubConfiguration cfg, final boolean accessContenturl) throws Exception {
        final ArrayList<DownloadLink> ret = super.crawlSingleVideo(param, contenturl, cfg, accessContenturl);
        final ArrayList<DownloadLink> videos = new ArrayList<DownloadLink>();
        final ArrayList<DailyMotionVariant> variantInfos = new ArrayList<DailyMotionVariant>();
        DownloadLink variantLink = null;
        DailyMotionVariant bestVi = null;
        for (final DownloadLink dl : ret) {
            final String type = dl.getStringProperty("plain_ext");
            if (".mp4".equals(type)) {
                videos.add(dl);
                final DailyMotionVariant vi = getDailyMotionVariant(dl);
                variantInfos.add(vi);
                if (bestVi == null || vi.getVideoHeight() > bestVi.getVideoHeight()) {
                    bestVi = vi;
                    variantLink = dl;
                }
            }
        }
        ret.removeAll(videos);
        ret.clear();
        if (variantLink != null) {
            ret.add(variantLink);
            Collections.sort(variantInfos, new Comparator<DailyMotionVariant>() {
                @Override
                public int compare(DailyMotionVariant o1, DailyMotionVariant o2) {
                    return Integer.compare(o2.getVideoHeight(), o1.getVideoHeight());
                }
            });
            variantLink.setVariants(variantInfos);
            // variantLink.setProperty("directlink", bestVi.getLink());
            // variantLink.setProperty("qualityname", bestVi.getqName());
            // variantLink.setProperty("qualitynumber", bestVi.getqNumber());
            //
            // final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(variantLink);
            // variantLink.setFinalFileName(formattedFilename);
            variantLink.setVariantSupport(true);
            variantLink.setLinkID("dailymotioncom" + variantLink.getStringProperty(DailyMotionCom.PROPERTY_VIDEO_ID) + "_" + bestVi.getqName());
            DailyMotionComV2.setActiveVariant(variantLink, bestVi);
            if (cfg == null || cfg.getBooleanProperty(DailyMotionCom.ALLOW_AUDIO, defaultAllowAudio)) {
                final DownloadLink audio = createDownloadlink(variantLink.getPluginPatternMatcher());
                /* Inherit all properties of source-item. */
                final Map<String, Object> props = variantLink.getProperties();
                if (props != null) {
                    for (Entry<String, Object> es : props.entrySet()) {
                        audio.setProperty(es.getKey(), es.getValue());
                    }
                }
                ArrayList<DailyMotionVariant> audioVariants = new ArrayList<DailyMotionVariant>();
                audioVariants.add(new DailyMotionVariant(bestVi, "aac", "128kbits", "AAC Audio (~128kbit/s)"));
                final DailyMotionVariant chosenVariant;
                audioVariants.add(chosenVariant = new DailyMotionVariant(bestVi, "m4a", "128kbits", "M4A Audio (~128kbit/s)"));
                audio.setVariants(audioVariants);
                audio.setProperty("plain_ext", ".m4a");
                audio.setVariantSupport(true);
                // audio.setUrlDownload(audio.getDownloadURL() + );
                audio.setProperty("qualityname", chosenVariant.getqName());
                audio.setLinkID("dailymotioncom" + variantLink.getStringProperty(DailyMotionCom.PROPERTY_VIDEO_ID) + "_" + chosenVariant.getDisplayName());
                variantLink.getFilePackage().add(audio);
                final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(audio);
                audio.setFinalFileName(formattedFilename);
                DailyMotionComV2.setActiveVariant(audio, chosenVariant);
                ret.add(audio);
            }
        }
        return ret;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = super.decryptIt(param, progress);
        return ret;
    }
}