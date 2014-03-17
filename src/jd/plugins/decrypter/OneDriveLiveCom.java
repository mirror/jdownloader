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
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "onedrive.live.com" }, urls = { "https?://(www\\.)?(onedrive\\.live\\.com/\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|skydrive\\.live\\.com/(\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+)|sdrv\\.ms/[A-Za-z0-9]+)" }, flags = { 0 })
public class OneDriveLiveCom extends PluginForDecrypt {

    public OneDriveLiveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ALL                = "https?://(www\\.)?(onedrive\\.live\\.com/\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|skydrive\\.live\\.com/(\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+)|sdrv\\.ms/[A-Za-z0-9]+)";
    private static final String TYPE_DRIVE_ALL          = "https?://(www\\.)?(onedrive\\.live\\.com/\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|skydrive\\.live\\.com/(\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+|redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+))";
    private static final String TYPE_SKYDRIVE_REDIRECT  = "https?://(www\\.)?skydrive\\.live\\.com/redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#]+";
    private static final String TYPE_SKYDRIVE_SHORT     = "https?://(www\\.)?sdrv\\.ms/[A-Za-z0-9]+";
    private static final String TYPE_SKYDRIVE           = "https?://(www\\.)?skydrive\\.live\\.com/\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+";
    private static final String TYPE_ONEDRIVE           = "https?://(www\\.)?onedrive\\.live\\.com/\\?cid=[a-z0-9]+[A-Za-z0-9\\&\\!=#\\.,]+";
    private static final int    MAX_ENTRIES_PER_REQUEST = 1000;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String cid = null;
        String id = null;
        String authkey = null;
        final DownloadLink main = createDownloadlink("http://onedrivedecrypted.live.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        try {
            if (parameter.matches(TYPE_SKYDRIVE_REDIRECT)) {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = new Regex(parameter, "\\&resid=([A-Za-z0-9]+\\!\\d+)").getMatch(0);
            } else if (parameter.matches(TYPE_DRIVE_ALL)) {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = getLastID(parameter);
            } else if (parameter.matches(TYPE_SKYDRIVE_SHORT)) {
                br.getPage(parameter);
                String redirect = br.getRedirectLocation();
                if (!redirect.contains("live")) {
                    br.getPage(redirect);
                    redirect = br.getRedirectLocation();
                }
                cid = new Regex(redirect, "cid=([A-Za-z0-9]*)").getMatch(0);
                if (cid == null) cid = new Regex(redirect, "resid=([A-Z0-9]+)").getMatch(0);
                id = new Regex(redirect, "resid=([A-Za-z0-9]+\\!\\d+)").getMatch(0);
                if (id == null) id = getLastID(parameter);
                authkey = new Regex(redirect, "\\&authkey=(\\![A-Za-z0-9]+)").getMatch(0);
            } else {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = new Regex(parameter, "\\&id=([A-Za-z0-9]+\\!\\d+)(\\&authkey=\\![A-Za-z0-9]+)?$").getMatch(0);
            }
            if (authkey == null) authkey = new Regex(parameter, "\\&authkey=(\\![A-Za-z0-9]+)").getMatch(0);
            if (cid == null || id == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            parameter = "https://onedrive.live.com/?cid=" + cid + "&id=" + id;
            param.setCryptedUrl(parameter);

            prepBrAPI(this.br);
            String additional_data = "&ps=" + MAX_ENTRIES_PER_REQUEST;
            if (authkey != null) additional_data += "&authkey=" + Encoding.urlEncode(authkey);
            accessItems_API(this.br, cid, id, additional_data);
        } catch (final BrowserException e) {
            main.setFinalFileName(new Regex(parameter, "onedrive\\.live\\.com/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String folderName = br.getRegex("\"group\":0,\"iconType\":\"NonEmptyDocumentFolder\".*?\"name\":\"([^<>\"]*?)\"").getMatch(0);
        if (folderName == null) folderName = br.getRegex("\"name\":\"([^<>\"]*?)\",\"orderedFriendlyName\"").getMatch(0);
        if (folderName == null) folderName = "onedrive.live.com content of user " + cid + " - folder - " + id;

        main.setProperty("mainlink", parameter);
        main.setProperty("plain_cid", cid);
        main.setProperty("plain_id", id);
        main.setProperty("plain_authkey", authkey);

        if (br.containsHTML("\"code\":154")) {
            main.setFinalFileName(folderName);
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        } else if ("0".equals(getJson("totalCount", br.toString()))) {
            main.setFinalFileName(folderName);
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String linktext = getLinktext(this.br);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        folderName = Encoding.htmlDecode(folderName.trim());

        final String[] links = linktext.split("\"userRole\":2\\},\\{\"");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        long totalSize = 0;
        for (final String singleinfo : links) {
            /* Check if it's a folder */
            if ("32".equals(getJson("itemType", singleinfo))) {
                final String folder_id = new Regex(singleinfo, "\"(NonEmptyDocumentFolder|NonEmptyAlbum)\",\"id\":\"([^<>\"]*?)\"").getMatch(1);
                final String folder_cid = getJson("creatorCid", singleinfo);
                if (folder_id == null || folder_cid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("https://onedrive.live.com/?cid=" + folder_cid + "&id=" + folder_id);
                decryptedLinks.add(dl);
            } else {
                final String filesize = getJson("size", singleinfo);
                String filename = getJson("name", singleinfo);
                String view_url = getJson("viewInBrowser", singleinfo);
                String download_url = getJson("download", singleinfo);
                final String ext = getJson("extension", singleinfo);
                if (filesize == null || filename == null || view_url == null || download_url == null || ext == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("http://onedrivedecrypted.live.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                view_url = view_url.replace("\\", "");
                download_url = download_url.replace("\\", "");
                filename = Encoding.htmlDecode(filename.trim()) + ext;
                final long cursize = Long.parseLong(filesize);
                dl.setDownloadSize(cursize);
                totalSize += cursize;
                dl.setFinalFileName(filename);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("plain_name", filename);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("plain_view_url", view_url);
                dl.setProperty("plain_download_url", download_url);
                dl.setProperty("plain_cid", cid);
                dl.setProperty("plain_id", id);
                dl.setProperty("plain_authkey", authkey);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (decryptedLinks.size() > 1) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = folderName + ".zip";
            main.setFinalFileName(folderName);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            // decryptedLinks.add(main);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

    private String getLastID(final String parameter) {
        /* Get last ID */
        int pos = parameter.lastIndexOf("&id=") + 4;
        final String parameter_part = parameter.substring(pos, parameter.length());
        return new Regex(parameter_part, "([A-Z0-9]+\\!\\d+)").getMatch(0);
    }

    public static String getLinktext(final Browser br) {
        String linktext = br.getRegex("\"children\":\\[(.*?)\\],\"covers\":").getMatch(0);
        if (linktext == null) linktext = br.getRegex("\"children\":\\[(.*?)\\],\"defaultSort\":").getMatch(0);
        return linktext;
    }

    public static void prepBrAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("X-ForceCache", "1");
        br.getHeaders().put("X-SkyApiOriginId", "0.9554840477898046");
        br.getHeaders().put("Referer", "https://skyapi.onedrive.live.com/api/proxy?v=3");
        br.getHeaders().put("AppId", "1141147648");
        br.setCustomCharset("utf-8");
    }

    public static void accessItems_API(final Browser br, final String cid, final String id, final String additional) throws IOException {
        String data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(id) + additional;
        boolean failed = false;
        try {
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=&path=1&si=0&pi=5&m=de-DE&rset=skyweb&lct=1&v=0.9853249325176565" + data);
        } catch (final BrowserException e) {
            if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
                failed = true;
            } else {
                throw e;
            }
        }
        /* Maybe the folder is empty but we can move one up and get its contents... */
        if (failed || getLinktext(br) == null) {
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0%2C1%2C2&d=1&iabch=1&caller=&path=1&si=0&pi=5&m=de-DE&rset=skyweb&lct=1&v=0.4213298695524278" + data);
            final String parentID = getJson("parentId", br.toString());
            if (parentID != null) {
                /* Error 500 will happen on invalid API requests */
                data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(parentID) + "&sid=" + Encoding.urlEncode(id) + additional;
                br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=&path=1&si=0&pi=5&m=de-DE&rset=skyweb&lct=1&v=0.9853249325176565" + data);
            }
        }
    }

}
