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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Inflater;

import jd.PluginWrapper;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tele5.de" }, urls = { "http://(www\\.)?tele5\\.de/[\\w/\\-]+" }, flags = { 0 })
public class TeleFiveDeDecrypter extends PluginForDecrypt {
    // we cannot do core updates right now, and should keep this calss internal until we can do core updates
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

    public TeleFiveDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        /* parse flash url */
        br.setFollowRedirects(true);
        br.getPage(parameter);
        for (String flashUrl : br.getRegex("rel=\"media:video\"\\s*resource=\"(http[^\"]+)\"").getColumn(0)) {
            // String flashUrl = br.getRegex("rel=\"media:video\"\\s*resource=\"(http[^\"]+)\"").getMatch(0);
            String streamerType = br.getRegex("value=\"streamerType=(http|rtmp)").getMatch(0);
            if (streamerType == null) streamerType = "http";
            if (flashUrl == null) {
                logger.warning("tele5 Decrypter broken!");
                return null;
            }
            /* decompress flash */
            String result = getUrlValues(flashUrl);
            if (result == null) return null;
            String kdpUrl = new Regex(result, "kdpUrl=([^#]+)#").getMatch(0);
            if (kdpUrl == null) return null;
            kdpUrl = Encoding.htmlDecode(kdpUrl);

            /* put url vars into map */
            Map<String, String> v = new HashMap<String, String>();
            for (String s : kdpUrl.split("&")) {
                if (!s.contains("=")) s += "=placeholder";
                if ("referer=".equals(s)) s += br.getURL();
                v.put(s.split("=")[0], s.split("=")[1]);
            }
            v.put("streamerType", streamerType);
            boolean isRtmp = "rtmp".equals(streamerType);
            Browser br2 = br.cloneBrowser();
            /* create final request */
            String postData = "2:entryId=" + v.get("entryId");
            postData += "&2:service=flavorasset";
            postData += "&clientTag=kdp:3.4.10.3," + v.get("clientTag");
            postData += "&2:action=getWebPlayableByEntryId";
            postData += "&3:entryId=" + v.get("entryId");
            postData += "&1:version=-1";
            postData += "&3:service=baseentry";
            postData += "&ks=" + v.get("ks");
            postData += "&1:service=baseentry";
            postData += "&3:action=getContextData";
            postData += "&3:contextDataParams:objectType=KalturaEntryContextDataParams";
            postData += "&1:entryId=" + v.get("entryId");
            postData += "&3:contextDataParams:referrer=" + v.get("referer");
            postData += "&1:action=get";
            postData += "&ignoreNull=1";
            Document doc = JDUtilities.parseXmlString(br2.postPage("http://medianac.nacamar.de//api_v3/index.php?service=multirequest&action=null", postData), false);

            /* xml data --> HashMap */
            // /xml/result/item --> name, ext etc.
            // /xml/result/item/item --> streaminfo entryId bitraten auflösung usw.
            final Node root = doc.getChildNodes().item(0);
            System.out.println(root.getNodeName());
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
            if (fileInfo.size() == 0) return null;

            if (!isEmpty(fileInfo.get("categories"))) fileInfo.put("categories", new String(fileInfo.get("categories").getBytes("ISO-8859-1"), "UTF-8"));
            if (!isEmpty(fileInfo.get("name"))) fileInfo.put("name", new String(fileInfo.get("name").getBytes("ISO-8859-1"), "UTF-8"));

            ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
            boolean r = true;

            /* creating downloadLinks */
            for (Entry<String, HashMap<String, String>> next : KalturaMediaEntry.entrySet()) {
                KalturaFlavorAsset = new HashMap<String, String>(next.getValue());
                String fName = fileInfo.get("categories");
                if (isEmpty(fName)) {
                    fName = br.getRegex("<div id=\"headline\" class=\"grid_\\d+\"><h1>(.*?)</h1><").getMatch(0);
                    fileInfo.put("categories", fName);
                }
                String filename = fileInfo.get("categories") + "__" + fileInfo.get("name").replaceAll("\\|", "-") + "_" + KalturaFlavorAsset.get("width") + "x" + KalturaFlavorAsset.get("height") + "@" + KalturaFlavorAsset.get("bitrate") + "Kbps." + KalturaFlavorAsset.get("fileExt");
                String dllink = "http://" + v.get("host") + "/p/" + v.get("partnerId") + "/sp/" + v.get("subpId") + "/playManifest/entryId/" + v.get("entryId");
                dllink += ("http".equals(streamerType) && KalturaFlavorAsset.containsKey("id") ? "/flavorId/" + KalturaFlavorAsset.get("id") : "") + "/format/" + v.get("streamerType");
                dllink += "/protocol/" + v.get("streamerType") + (v.containsKey("cdnHost") ? "/cdnHost/" + v.get("cdnHost") : "") + (v.containsKey("storageId") ? "/storageId/" + v.get("storageId") : "");
                dllink += (v.containsKey("ks") ? "/ks/" + v.get("ks") : "") + "/referrer/" + Encoding.Base64Encode(parameter) + (v.containsKey("token") ? "/token/" + v.get("token") : "") + "/a/a.f4m";
                if (dllink.contains("null")) {
                    logger.warning("tele5 Decrypter: null in API Antwort entdeckt!");
                    logger.warning("tele5 Decrypter: " + dllink);
                }
                if (r) br2.getPage(dllink);
                /* make dllink */
                dllink = br2.getRegex("<media url=\"([^\"]+" + KalturaFlavorAsset.get("id") + ".*?)\"").getMatch(0);
                final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=" + KalturaFlavorAsset.get("bitrate") + "&vId=" + KalturaFlavorAsset.get("id"));
                link.setAvailable(true);
                link.setFinalFileName(filename);
                link.setBrowserUrl(parameter);
                link.setDownloadSize(SizeFormatter.getSize(KalturaFlavorAsset.get("size") + "kb"));
                link.setProperty("streamerType", v.get("streamerType"));
                link.setProperty("directURL", dllink);
                if (isRtmp) {
                    r = false;
                    String baseUrl = br2.getRegex("<baseURL>(.*?)</baseURL").getMatch(0);
                    if (baseUrl == null) {
                        logger.warning("tele5 Decrypter: baseUrl regex broken!");
                        continue;
                    }
                    link.setProperty("directURL", baseUrl + "@" + dllink);
                }
                newRet.add(link);
            }

            if (newRet.size() > 1) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fileInfo.get("categories"));
                fp.addLinks(newRet);
            }
            decryptedLinks.addAll(newRet);
        }
        return decryptedLinks;
    }

    private String getUrlValues(String s) throws UnsupportedEncodingException {
        SWFDecompressor swfd = new SWFDecompressor();
        byte[] swfdec = swfd.decompress(s);
        if (swfdec == null || swfdec.length == 0) return null;

        for (int i = 0; i < swfdec.length; i++) {
            if (swfdec[i] < 33 || swfdec[i] > 127) {
                swfdec[i] = 35; // #
            }
        }
        return new String(swfdec, "UTF8");
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}