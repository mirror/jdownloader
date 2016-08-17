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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video.cnbc.com" }, urls = { "http://video\\.cnbc\\.com/gallery/\\?video=\\d+" }) 
public class CnbcComDecrypter extends PluginForDecrypt {

    public CnbcComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Thanks: https://github.com/isync/www-video-download/blob/master/cnbc-downloader.pl */

    /* Example main: http://video.cnbc.com/gallery/?video=3000373412 */
    /*
     * Example
     * mpeg4_500000_Streaming|http://syndication.cnbc.com/vcps/media/content?id=MzAwMDM3MzQxMgptcGVnNF81MDAwMDAKU3RyZWFtaW5n&UserName
     * =cmsguest@cnbc.com&key=NA
     */
    /*
     * Example mpeg4_500000_HLSSBRStreaming|http://cnbcmbr-vh.akamaihd.net/i/mp4/VCPS/Y2015/M04D23/3000373412/
     * 4ED3-REQ-0422-RetireWell_MBR_0500.mp4/master.m3u8
     */
    /*
     * Example
     * mpeg4_1100000_Download|http://pdl.iphone.cnbc.com/VCPS/Y2015/M04D23/3000373412/4ED3-REQ-0422-RetireWell_H264_720x405_30p_1M.mp4
     */
    /*
     * Example mpeg4_1_HLSMBRStreaming|http://cnbcmbr-vh.akamaihd.net/i/mp4/VCPS/Y2015/M04D23/3000373412/
     * 4ED3-REQ-0422-RetireWell_MBR_,0240,0300,0500,0700,0900,1300,1700,.mp4.csmil/master.m3u8
     */

    private static final String DOMAIN         = "video.cnbc.com";
    /* Last checked: 27.04.15 */
    /* seems to be CNBC's thePlatform partner id */
    private static final String partner_id     = "6008";
    /* Settings stuff */
    private static final String FAST_LINKCHECK = "FAST_LINKCHECK";

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost("video.cnbc.com");
        final String parameter = param.toString();
        final String vid = new Regex(parameter, "(\\d+)$").getMatch(0);
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> ressourcelist = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.CnbcCom.formats;
        final String nicehost = new Regex(parameter, "http://(?:www\\.)?([^/]+)").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        this.br.setFollowRedirects(false);
        this.br.setAllowedResponseCodes(503);
        /* We use their mobile API. Mobile-UA is not needed but let's not make it too obvious :) */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile");
        br.getPage("http://www.cnbc.com/vapi/videoservice/rssvideosearch.do?callback=mobileVideoServiceJSON&action=videos&ids=" + vid + "&output=json&partnerId=" + partner_id);
        /* 503 = normal offline in this case */
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.getRequest().getHttpConnection().getResponseCode() == 503) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(vid);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("rss");
        entries = (LinkedHashMap<String, Object>) entries.get("channel");
        entries = (LinkedHashMap<String, Object>) entries.get("item");
        /*
         * Most times we will have 10 entries available - sometimes less and sometimes also less http-urls but usually at least 2 of 4 http
         * urls are available.
         */
        ressourcelist = (ArrayList) entries.get("metadata:formatLink");
        String title = (String) entries.get("title");
        title = encodeUnicode(title);
        final String description = (String) entries.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (final Object plo : ressourcelist) {
            final String urlinfo[] = ((String) plo).split("\\|");
            final String qualinfo = urlinfo[0];
            final String url = urlinfo[1];
            if (formats.containsKey(qualinfo) && cfg.getBooleanProperty(qualinfo, true)) {
                final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String[] vidinfo = formats.get(qualinfo);
                String filename = title + "_" + getFormatString(vidinfo);
                filename += ".mp4";

                dl.setContentUrl(parameter);
                if (description != null) {
                    dl.setComment(description);
                }
                dl.setLinkID(vid + filename);
                dl._setFilePackage(fp);
                dl.setProperty("format", qualinfo);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", url);
                dl.setProperty("directfilename", filename);
                dl.setFinalFileName(filename);
                if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected formats were found or none were selected, decrypting done...");
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