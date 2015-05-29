//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.Inflater;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tele5.de" }, urls = { "http://(www\\.)?tele5\\.de/[\\w/\\-]+\\.html" }, flags = { 0 })
public class TeleFiveDeDecrypter extends PluginForDecrypt {
    // we cannot do core updates right now, and should keep this class internal until we can do core updates
    private static final String DOMAIN = "tele5.de";

    public class SWFDecompressor {
        public SWFDecompressor() {
            super();
        }

        public byte[] decompress(String s) { // ~2000ms
            byte[] buffer = new byte[512];
            InputStream input = null;
            ByteArrayOutputStream result = null;
            byte[] enc = null;

            try {
                URL url = new URL(s);
                input = url.openStream(); // ~500ms
                result = new ByteArrayOutputStream();

                try {
                    int amount;
                    while ((amount = input.read(buffer)) != -1) { // ~1500ms
                        result.write(buffer, 0, amount);
                    }
                } finally {
                    try {
                        input.close();
                    } catch (Throwable e) {
                    }
                    try {
                        result.close();
                    } catch (Throwable e2) {
                    }
                    enc = result.toByteArray();
                }
            } catch (Throwable e3) {
                e3.getStackTrace();
                return null;
            }
            return uncompress(enc);
        }

        /**
         * Strips the uncompressed header bytes from a swf file byte array
         *
         * @param bytes
         *            of the swf
         * @return bytes array minus the uncompressed header bytes
         */
        private byte[] strip(byte[] bytes) {
            byte[] compressable = new byte[bytes.length - 8];
            System.arraycopy(bytes, 8, compressable, 0, bytes.length - 8);
            return compressable;
        }

        private byte[] uncompress(byte[] b) {
            Inflater decompressor = new Inflater();
            decompressor.setInput(strip(b));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length - 8);
            byte[] buffer = new byte[1024];

            try {
                while (true) {
                    int count = decompressor.inflate(buffer);
                    if (count == 0 && decompressor.finished()) {
                        break;
                    } else if (count == 0) {
                        return null;
                    } else {
                        bos.write(buffer, 0, count);
                    }
                }
            } catch (Throwable t) {
            } finally {
                decompressor.end();
            }

            byte[] swf = new byte[8 + bos.size()];
            System.arraycopy(b, 0, swf, 0, 8);
            System.arraycopy(bos.toByteArray(), 0, swf, 8, bos.size());
            swf[0] = 70; // F
            return swf;
        }

    }

    @SuppressWarnings("deprecation")
    public TeleFiveDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Example http url: http://dl.mnac-p-000000102.c.nmdn.net/mnac-p-000000102/12345678/0/0_moyme6yj_0_tfjmbp2l_2.mp4 */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost("tele5.de");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String nicehost = new Regex(parameter, "http://(?:www\\.)?([^/]+)").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final LinkedHashMap<String, String[]> formats = jd.plugins.hoster.TeleFiveDe.formats;
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final Throwable e) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String fpName = br.getRegex("class=\"grid_10\"><h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "tele5\\.de/([\\w/\\-]+)\\.html").getMatch(0);
        }

        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String[] youtubeurls = br.getRegex("\"(https?://(www\\.)?youtube\\.com/embed/[^<>\"]*?)\"").getColumn(0);
        final String[] videosinfo = br.getRegex("kWidget\\.(?:thumb)?Embed\\(\\{(.*?)</script>").getColumn(0);
        if ((videosinfo == null || videosinfo.length == 0) && (youtubeurls == null || youtubeurls.length == 0)) {
            if (!br.containsHTML("kaltura_player_")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            return null;
        }

        if (videosinfo != null && videosinfo.length > 0) {
            for (final String videosource : videosinfo) {
                HashMap<String, DownloadLink> foundLinks = getURLsFromMedianac(br, decryptedhost, videosource, formats);

                /* Now add the links the user wants. */
                final Iterator<Entry<String, DownloadLink>> it = foundLinks.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, DownloadLink> next = it.next();
                    final String qualityInfo = next.getKey();
                    final DownloadLink dl = next.getValue();
                    if (cfg.getBooleanProperty(qualityInfo, false)) {
                        decryptedLinks.add(dl);
                    }
                }
            }
        }

        if (youtubeurls != null && youtubeurls.length > 0) {
            for (final String yturl : youtubeurls) {
                decryptedLinks.add(createDownloadlink(yturl));
            }
        }

        if (decryptedLinks.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        decryptedLinks.addAll(decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Can extract directurls from api.medianac.com.
     *
     * @param br
     *            Browser to use
     * @param decryptedhost
     *            (String) Name of the host to use in created DownloadLink's
     * @param video_source
     *            (String)html/json source that contains medianac.com API parameters
     * @param formats
     *            (Stringarray) containing information about the supported videoformats
     *
     * @throws IOException
     */
    public HashMap<String, DownloadLink> getURLsFromMedianac(final Browser br, final String decryptedhost, final String video_source, final HashMap<String, String[]> formats) throws IOException {
        /* parse flash url */
        HashMap<String, DownloadLink> foundLinks = new HashMap<String, DownloadLink>();

        String entry_id = getJson(video_source, "entry_id");
        String wid = getJson(video_source, "wid");
        String uiconf_id = getJson(video_source, "uiconf_id");
        String cache_st = getJson(video_source, "cache_st");
        String streamerType = getJson(video_source, "streamerType");
        /* Check if values are missing. Most likely this is needed to support embedded medianac videos. */
        if (wid == null) {
            wid = new Regex(video_source, "api\\.medianac\\.com/p/(\\d+)/").getMatch(0);
            if (wid != null) {
                wid = "_" + wid;
            }
        }
        if (uiconf_id == null) {
            uiconf_id = new Regex(video_source, "/uiconf_id/(\\d+)/").getMatch(0);
        }
        if (entry_id == null) {
            entry_id = new Regex(video_source, "(?:entry_id/|\\&entry_id=)([^/\\&]+)").getMatch(0);
        }
        /* Force http */
        if (streamerType == null || true) {
            streamerType = "http";
        }
        if (entry_id == null || wid == null || uiconf_id == null) {
            return null;
        }
        String flashUrl = "http://api.medianac.com/index.php/kwidget/wid/" + wid + "/uiconf_id/" + uiconf_id + "/entry_id/" + entry_id;
        if (cache_st != null) {
            flashUrl += "/cache_st/" + cache_st;
        }
        /* decompress flash */
        String result = getUrlValues(flashUrl);
        if (result == null) {
            return null;
        }
        String kdpUrl = new Regex(result, "kdpUrl=([^#]+)#").getMatch(0);
        if (kdpUrl == null) {
            return null;
        }
        kdpUrl = Encoding.htmlDecode(kdpUrl);

        /* put url vars into map */
        Map<String, String> v = new HashMap<String, String>();
        for (String s : kdpUrl.split("&")) {
            if (!s.contains("=")) {
                s += "=placeholder";
            }
            if ("referer=".equals(s)) {
                s += br.getURL();
            }
            v.put(s.split("=")[0], s.split("=")[1]);
        }
        v.put("streamerType", streamerType);
        boolean isRtmp = "rtmp".equals(streamerType);
        Browser br2 = br.cloneBrowser();
        /* create final request */
        String getData = "?service=multirequest&action=null&";
        getData += "2:entryId=" + v.get("entryId");
        getData += "&2:service=flavorasset";
        getData += "&clientTag=kdp:3.4.10.3," + v.get("clientTag");
        getData += "&2:action=getWebPlayableByEntryId";
        getData += "&3:entryId=" + v.get("entryId");
        getData += "&1:version=-1";
        getData += "&3:service=baseentry";
        getData += "&ks=" + v.get("ks");
        getData += "&1:service=baseentry";
        getData += "&3:action=getContextData";
        getData += "&3:contextDataParams:objectType=KalturaEntryContextDataParams";
        getData += "&1:entryId=" + v.get("entryId");
        getData += "&3:contextDataParams:referrer=" + v.get("referer");
        getData += "&1:action=get";
        getData += "&ignoreNull=1";
        final String getpage = "http://api.medianac.com//api_v3/index.php" + getData;
        br2.getPage(getpage);
        Document doc = JDUtilities.parseXmlString(br2.toString(), false);

        /* xml data --> HashMap */
        // /xml/result/item --> name, ext etc.
        // /xml/result/item/item --> streaminfo entryId bitraten auflösung usw.
        final Node root = doc.getChildNodes().item(0);
        NodeList nl = root.getFirstChild().getChildNodes();

        HashMap<String, HashMap<String, String>> KalturaMediaEntry = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> KalturaFlavorAsset = null;
        HashMap<String, String> fileInfo = new HashMap<String, String>();

        for (int i = 0; i < nl.getLength(); i++) {
            Node childNode = nl.item(i);
            NodeList t = childNode.getChildNodes();

            for (int j = 0; j < t.getLength(); j++) {
                Node g = t.item(j);
                if ("item".equals(g.getNodeName())) {
                    KalturaFlavorAsset = new HashMap<String, String>();
                    NodeList item = g.getChildNodes();

                    for (int k = 0; k < item.getLength(); k++) {
                        Node kk = item.item(k);
                        KalturaFlavorAsset.put(kk.getNodeName(), kk.getTextContent());
                    }
                    KalturaMediaEntry.put(String.valueOf(j), KalturaFlavorAsset);
                    // if (isRtmp) break;
                    continue;
                }
                fileInfo.put(g.getNodeName(), g.getTextContent());
            }
        }
        if (fileInfo.size() == 0) {
            return null;
        }

        if (!isEmpty(fileInfo.get("categories"))) {
            fileInfo.put("categories", new String(fileInfo.get("categories").getBytes("ISO-8859-1"), "UTF-8"));
        }
        if (!isEmpty(fileInfo.get("name"))) {
            fileInfo.put("name", new String(fileInfo.get("name").getBytes("ISO-8859-1"), "UTF-8"));
        }

        boolean r = true;
        boolean gotPage = false;

        /* creating downloadLinks */
        for (Entry<String, HashMap<String, String>> next : KalturaMediaEntry.entrySet()) {
            KalturaFlavorAsset = new HashMap<String, String>(next.getValue());
            final String bitrate = KalturaFlavorAsset.get("bitrate");
            String fName = fileInfo.get("categories");
            if (isEmpty(fName)) {
                fName = br.getRegex("<div id=\"headline\" class=\"grid_\\d+\"><h1>(.*?)</h1><").getMatch(0);
                fileInfo.put("categories", fName);
            }
            String formatString = "";
            final String videoBitrate = KalturaFlavorAsset.get("bitrate");
            final String videoWidth = KalturaFlavorAsset.get("width");
            final String videoHeight = KalturaFlavorAsset.get("height");
            final String videoResolution = videoWidth + "x" + videoHeight;
            final String qualityInfo = videoBitrate.substring(0, 1) + "_" + videoWidth.substring(0, 1) + "_" + videoHeight.substring(0, 1);
            final String[] formatinfo = formats.get(qualityInfo);
            final String audioCodec = formatinfo[3];
            final String audioBitrate = formatinfo[4];
            final String videoCodec = formatinfo[0];
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
            final String filename = fileInfo.get("categories") + "_" + fileInfo.get("name").replaceAll("\\|", "-") + "_" + formatString + "." + KalturaFlavorAsset.get("fileExt");

            /* Always access rtmp urls as this way we get all qualities/formats --> Then build http urls out of them --> :) */
            String vidlink = "http://api.medianac.com/p/" + v.get("partnerId") + "/sp/" + v.get("subpId") + "/playManifest/entryId/" + v.get("entryId") + "/format/rtmp/protocol/rtmp/cdnHost/api.medianac.com";
            vidlink += (v.containsKey("storageId") ? "/storageId/" + v.get("storageId") : "");
            vidlink += (v.containsKey("ks") ? "/ks/" + v.get("ks") : "") + "/referrer/" + Encoding.Base64Encode(br.getURL()) + (v.containsKey("token") ? "/token/" + v.get("token") : "") + "/a/a.f4m";
            vidlink += "?referrer=" + Encoding.Base64Encode(br.getURL());
            if (vidlink.contains("null")) {
                /* WTF */
                return null;
            }
            if (r && !gotPage) {
                /* We only need to access this page once, at least if we use rtmp as streaming protocol. */
                gotPage = true;
                br2.getPage(vidlink);
            }
            final String rtmp_base_server = br2.getRegex("<baseURL>rtmp://rtmp\\.(mnac\\-p\\-\\d+)\\.c\\.nmdn\\.net/.+</baseURL>").getMatch(0);
            /* make dllink */
            vidlink = br2.getRegex("<media url=\"mp4:([^<>\"]*?)\" bitrate=\"" + bitrate + "\"").getMatch(0);
            if (vidlink != null) {
                /* rtmp --> http */
                if (rtmp_base_server == null) {
                    return null;
                }
                vidlink = "http://dl." + rtmp_base_server + ".c.nmdn.net/" + rtmp_base_server + "/" + vidlink + ".mp4";
            } else {
                vidlink = br2.getRegex("<media url=\"(http[^\"]+" + KalturaFlavorAsset.get("id") + ".*?)\"").getMatch(0);
            }
            if (vidlink == null) {
                return null;
            }
            final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            dl.setLinkID(filename);
            dl.setContentUrl(br.getURL());
            dl.setDownloadSize(SizeFormatter.getSize(KalturaFlavorAsset.get("size") + "kb"));
            dl.setProperty("streamerType", v.get("streamerType"));
            dl.setProperty("directURL", vidlink);
            if (isRtmp) {
                r = false;
                String baseUrl = br2.getRegex("<baseURL>(rtmp.*?)</baseURL").getMatch(0);
                if (baseUrl == null) {
                    /* Ignore rtmp */
                    continue;
                }
                dl.setProperty("directRTMPURL", baseUrl + "@" + vidlink);
            }
            foundLinks.put(qualityInfo, dl);
        }
        return foundLinks;
    }

    public String getUrlValues(String s) throws UnsupportedEncodingException {
        SWFDecompressor swfd = new SWFDecompressor();
        byte[] swfdec = swfd.decompress(s);
        if (swfdec == null || swfdec.length == 0) {
            return null;
        }

        for (int i = 0; i < swfdec.length; i++) {
            if (swfdec[i] < 33 || swfdec[i] > 127) {
                swfdec[i] = 35; // #
            }
        }
        return new String(swfdec, "UTF8");
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private static String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    private static boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}