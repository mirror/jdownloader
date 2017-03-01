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
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rtbf.be" }, urls = { "https?://(?:www\\.)?rtbf\\.be/(?:video|auvio)/detail_[a-z0-9}\\-_]+\\?id=\\d+" })
public class RtbfBeDecrypter extends PluginForDecrypt {

    public RtbfBeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    private static final String FAST_LINKCHECK = "FAST_LINKCHECK";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(this.getHost());
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "(\\d+)$").getMatch(0);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.RtbfBe.formats;
        final String decryptedhost = "http://" + this.getHost() + "decrypted/";
        String date_formatted = null;

        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\\s+(?::|-)\\s+RTBF\\s+(?:Vidéo|Auvio)\"").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        /* 2017-03-01: Removed subtitle for now as we got faulty names before. Title should actually contain everything we need! */
        final String subtitle = null;
        final String uploadDate = PluginJSonUtils.getJsonValue(this.br, "uploadDate");
        if (uploadDate != null) {
            date_formatted = new Regex(uploadDate, "^(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        }
        br.getPage("embed/media?id=" + fid + "&autoplay=1");
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // final String video_json_metadata = this.br.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
        String video_json = br.getRegex("<div class=\"js\\-player\\-embed.*?\" data\\-video=\"(.*?)\">").getMatch(0);
        if (video_json == null) {
            video_json = this.br.getRegex("data\\-video=\"(.*?)\"").getMatch(0);
        }
        if (video_json == null) {
            video_json = this.br.getRegex("data\\-media=\"(.*?)\"></div>").getMatch(0);
        }
        if (video_json == null) {
            return null;
        }
        // this is json encoded with htmlentities.
        video_json = HTMLEntities.unhtmlentities(video_json);
        video_json = Encoding.htmlDecode(video_json);
        video_json = PluginJSonUtils.unescape(video_json);
        // we can get filename here also.
        if (title == null) {
            title = PluginJSonUtils.getJsonValue(video_json, "title");
        }
        if (title == null) {
            return null;
        }
        title = "rtbf_" + title;
        if (date_formatted != null) {
            title = date_formatted + "_" + title;
        }
        if (subtitle != null && !subtitle.equalsIgnoreCase("{{subtitle}}")) {
            title = title + " - " + subtitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        final String[][] qualities = { { "download", "downloadUrl" }, { "high", "high" }, { "medium", "web" }, { "low", "mobile" } };
        for (final String[] qualityinfo : qualities) {
            final String qualityCfg = qualityinfo[0];
            final String qualityJson = qualityinfo[1];
            final String qualityDllink = PluginJSonUtils.getJsonValue(video_json, qualityJson);
            if (qualityDllink != null && formats.containsKey(qualityCfg) && cfg.getBooleanProperty("ALLOW_" + qualityCfg, true)) {
                final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String[] vidinfo = formats.get(qualityCfg);
                String filename = title + "_" + getFormatString(vidinfo);
                filename += ".mp4";

                dl.setContentUrl(parameter);
                dl.setLinkID(fid + filename);
                dl._setFilePackage(fp);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", qualityDllink);
                dl.setProperty("directfilename", filename);
                dl.setFinalFileName(filename);
                if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("None of the selected formats were found or none were selected, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String getFormatString(final String[] formatinfo) {
        String formatString = "";
        final String videoCodec = formatinfo[0];
        final String videoBitrate = formatinfo[1];
        final String videoResolution = formatinfo[2];
        final String audioCodec = formatinfo[3];
        final String audioBitrate = formatinfo[4];
        if (videoCodec != null) {
            formatString += videoCodec + "_";
        }
        if (videoResolution != null) {
            formatString += videoResolution + "_";
        }
        if (videoBitrate != null) {
            formatString += videoBitrate + "_";
        }
        if (audioCodec != null) {
            formatString += audioCodec + "_";
        }
        if (audioBitrate != null) {
            formatString += audioBitrate;
        }
        if (formatString.endsWith("_")) {
            formatString = formatString.substring(0, formatString.lastIndexOf("_"));
        }
        return formatString;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}