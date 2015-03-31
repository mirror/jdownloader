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

import java.io.IOException;
import java.text.ParseException;
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

//Decrypts embedded videos from dailymotion
@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "dailymotion.com" }, urls = { "https?://(www\\.)?dailymotion\\.com/((embed/)?video/[a-z0-9\\-_]+|swf(/video)?/[a-zA-Z0-9]+|user/[A-Za-z0-9_\\-]+/\\d+|playlist/[A-Za-z0-9]+_[A-Za-z0-9\\-_]+(/\\d+)?|(?!playlist)[A-Za-z0-9_\\-]+)" }, flags = { 0 })
public class DailyMotionComDecrypterV2 extends DailyMotionComDecrypter {

    public DailyMotionComDecrypterV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected void decryptSingleVideo(ArrayList<DownloadLink> decryptedLinks) throws IOException, ParseException {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        super.decryptSingleVideo(ret);
        ArrayList<DownloadLink> mp4s = new ArrayList<DownloadLink>();
        ArrayList<DailyMotionVariant> variantInfos = new ArrayList<DailyMotionVariant>();

        DownloadLink variantLink = null;
        DailyMotionVariant bestVi = null;

        for (DownloadLink dl : ret) {

            String type = dl.getStringProperty("plain_ext");
            if (".mp4".equals(type)) {

                mp4s.add(dl);
                if ("hds".equals(dl.getStringProperty("qualityname"))) {

                    // hds is not possible yet
                    continue;
                }
                DailyMotionVariant vi;
                variantInfos.add(vi = new DailyMotionVariant(dl));

                if (bestVi == null || vi.getQrate() < bestVi.getQrate()) {
                    bestVi = vi;
                    variantLink = dl;
                }
            }
        }
        ret.removeAll(mp4s);

        if (variantLink != null) {
            ret.add(variantLink);
            Collections.sort(variantInfos, new Comparator<DailyMotionVariant>() {

                @Override
                public int compare(DailyMotionVariant o1, DailyMotionVariant o2) {
                    return Integer.compare(o2.getQrate(), o1.getQrate());
                }
            });
            variantLink.setVariants(variantInfos);
            variantLink.setVariant(bestVi);

            // variantLink.setProperty("directlink", bestVi.getLink());
            // variantLink.setProperty("qualityvalue", bestVi.getqValue());
            // variantLink.setProperty("qualityname", bestVi.getqName());
            // variantLink.setProperty("originalqualityname", bestVi.getOrgQName());
            // variantLink.setProperty("qualitynumber", bestVi.getqNumber());
            //
            // final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(variantLink);
            // variantLink.setFinalFileName(formattedFilename);

            variantLink.setVariantSupport(true);

            variantLink.setLinkID("dailymotioncom" + variantLink.getStringProperty("plain_videoid") + "_" + bestVi.getqName());
            final SubConfiguration cfg = SubConfiguration.getConfig("dailymotion.com");
            if (cfg.getBooleanProperty(ALLOW_AUDIO, defaultAllowAudio)) {

                DownloadLink audio = createDownloadlink(variantLink.getDownloadURL());
                final Map<String, Object> props = variantLink.getProperties();
                if (props != null) {
                    for (Entry<String, Object> es : props.entrySet()) {
                        audio.setProperty(es.getKey(), es.getValue());
                    }
                }
                ArrayList<DailyMotionVariant> audioVariants = new ArrayList<DailyMotionVariant>();
                audioVariants.add(new DailyMotionVariant(bestVi, "aac", "128kbits", "AAC Audio (~128kbit/s)"));
                DailyMotionVariant mp4;
                audioVariants.add(mp4 = new DailyMotionVariant(bestVi, "m4a", "128kbits", "M4A Audio (~128kbit/s)"));
                audio.setVariants(audioVariants);

                audio.setVariant(mp4);
                audio.setProperty("plain_ext", ".m4a");
                audio.setVariantSupport(true);
                // audio.setUrlDownload(audio.getDownloadURL() + );
                audio.setProperty("qualityname", mp4.getqName());

                audio.setLinkID("dailymotioncom" + variantLink.getStringProperty("plain_videoid") + "_" + mp4.getDisplayName());
                variantLink.getFilePackage().add(audio);
                final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(audio);
                audio.setFinalFileName(formattedFilename);
                ret.add(audio);
            }
        }
        //

        decryptedLinks.addAll(ret);

    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = super.decryptIt(param, progress);

        return ret;
    }
}