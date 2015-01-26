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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pinterest.com" }, urls = { "https?://(www\\.)?pinterest\\.com/(?!pin/)[^/]+/[^/]+/" }, flags = { 0 })
public class PinterestComDecrypter extends PluginForDecrypt {

    public PinterestComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     unsupported_urls = "https://(www\\.)?pinterest\\.com/(business/create/|android\\-app:/.+|ios\\-app:/.+)";

    private ArrayList<DownloadLink> decryptedLinks   = new ArrayList<DownloadLink>();
    private String                  parameter        = null;
    private FilePackage             fp               = null;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        parameter = param.toString().replace("http://", "https://");
        br.setFollowRedirects(true);
        if (parameter.matches(unsupported_urls)) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        final boolean loggedIN = getUserLogin(false);
        String fpName = null;
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        /* Don'r proceed with invalid/unsupported links. */
        if (!br.containsHTML("class=\"boardName\"")) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        String numberof_pins = br.getRegex("class=\"value\">(\\d+(?:\\.\\d+)?)</span> <span class=\"label\">Pins</span>").getMatch(0);
        if (numberof_pins == null) {
            numberof_pins = br.getRegex("class=\'value\'>(\\d+(?:\\.\\d+)?)</span> <span class=\'label\'>Pins</span>").getMatch(0);
        }
        if (numberof_pins == null) {
            numberof_pins = br.getRegex("name=\"pinterestapp:pins\" content=\"(\\d+)\"").getMatch(0);
        }
        fpName = br.getRegex("class=\"boardName\">([^<>]*?)<").getMatch(0);
        if (numberof_pins == null || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final long lnumberof_pins = Long.parseLong(numberof_pins.replace(".", ""));
        if (lnumberof_pins == 0) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        if (loggedIN) {
            /* First, get the first 25 pictures from their site. */
            decryptSite();
            final String board_id = br.getRegex("\"board_id\":[\t\n\r ]+\"(\\d+)\"").getMatch(0);
            String nextbookmark = br.getRegex("\"bookmarks\": \\[\"([^<>\"]{6,})\"").getMatch(0);
            final String source_url = new Regex(parameter, "pinterest\\.com(/.+)").getMatch(0);
            if (board_id == null || nextbookmark == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            do {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                prepAPIBR(br);
                String getpage = "http://www.pinterest.com/resource/BoardFeedResource/get/?source_url%s&data={\"options\":{\"board_id\":\"%s\",\"board_url\":\"%s\",\"page_size\":null,\"prepend\":true,\"access\":[],\"board_layout\":\"default\",\"bookmarks\":[\"%s\"]},\"context\":{}}";
                getpage = String.format(getpage, source_url, board_id, source_url, nextbookmark);
                getpage += "&_=" + System.currentTimeMillis();
                br.getPage(getpage);
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                final ArrayList<Object> resource_data_list = (ArrayList) entries.get("resource_data_cache");
                final LinkedHashMap<String, Object> t2 = (LinkedHashMap<String, Object>) resource_data_list.get(0);
                final ArrayList<Object> t3 = (ArrayList) t2.get("data");
                for (final Object pint : t3) {
                    final LinkedHashMap<String, Object> single_pinterest_data = (LinkedHashMap<String, Object>) pint;
                    final LinkedHashMap<String, Object> single_pinterest_pinner = (LinkedHashMap<String, Object>) single_pinterest_data.get("pinner");
                    final LinkedHashMap<String, Object> single_pinterest_images = (LinkedHashMap<String, Object>) single_pinterest_data.get("images");
                    final LinkedHashMap<String, Object> single_pinterest_images_original = (LinkedHashMap<String, Object>) single_pinterest_images.get("orig");
                    final String pin_directlink = (String) single_pinterest_images_original.get("url");
                    final String pin_id = (String) single_pinterest_data.get("id");
                    String pin_name = (String) single_pinterest_data.get("description");
                    final String username = (String) single_pinterest_pinner.get("username");
                    final String pinner_name = (String) single_pinterest_pinner.get("full_name");
                    if (pin_name == null || pin_id == null || pin_directlink == null || pinner_name == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    /* Description == Nice name as long as it is not extremely long! */
                    if (pin_name.length() <= 50) {
                        pin_name = pinner_name + "_" + pin_id + "_" + Encoding.htmlDecode(pin_name).trim() + ".jpg";
                    } else {
                        pin_name = pinner_name + "_" + pin_id + "_" + ".jpg";
                    }
                    pin_name = encodeUnicode(pin_name);
                    final String content_url = "http://www.pinterest.com/pin/" + pin_id + "/";
                    final DownloadLink dl = createDownloadlink(content_url);
                    try {
                        dl.setContentUrl(content_url);
                        dl.setLinkID(pin_id);
                    } catch (final Throwable e) {
                        /* Not supported in old 0.9.581 Stable */
                        dl.setBrowserUrl(content_url);
                        dl.setProperty("LINKDUPEID", pin_id);
                    }
                    dl._setFilePackage(fp);
                    dl.setProperty("free_directlink", pin_directlink);
                    dl.setProperty("boardid", board_id);
                    dl.setProperty("source_url", source_url);
                    dl.setProperty("username", username);
                    dl.setProperty("decryptedfilename", pin_name);
                    dl.setFinalFileName(pin_name);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* Not supported in old 0.9.581 Stable */
                    }
                }
                final LinkedHashMap<String, Object> resource_list = (LinkedHashMap<String, Object>) entries.get("resource");
                final LinkedHashMap<String, Object> options = (LinkedHashMap<String, Object>) resource_list.get("options");
                final ArrayList<Object> bookmarks_list = (ArrayList) options.get("bookmarks");
                nextbookmark = (String) bookmarks_list.get(0);
                logger.info("Decrypter " + decryptedLinks.size() + " of " + lnumberof_pins + " pins");
            } while (nextbookmark != null && !nextbookmark.equals("-end-"));
        } else {
            decryptSite();
        }

        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private void decryptSite() {
        /*
         * Also possible using json of P.start.start( to get the first 25 entries: resourceDataCache --> Last[] --> data --> Here we go --->
         * But I consider this as an unsafe method.
         */
        final String[] linkinfo = br.getRegex("<div class=\"bulkEditPinWrapper\">(.*?)id=\"Pin\\-\\d+\"").getColumn(0);
        if (linkinfo == null || linkinfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            decryptedLinks = null;
            return;
        }
        for (final String sinfo : linkinfo) {
            String title = new Regex(sinfo, "title=\"([^<>\"]*?)\"").getMatch(0);
            if (title == null) {
                title = new Regex(sinfo, "<p class=\"pinDescription\">([^<>]*?)<").getMatch(0);
            }
            final String directlink = new Regex(sinfo, "\"(https?://[a-z0-9\\.\\-]+/originals/[^<>\"]*?)\"").getMatch(0);
            final String pin_id = new Regex(sinfo, "/pin/(\\d+)/").getMatch(0);
            if (pin_id == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            String pin_filename;
            if (title != null) {
                pin_filename = pin_id + "_" + Encoding.htmlDecode(title).trim() + ".jpg";
            } else {
                pin_filename = pin_id + ".jpg";
            }
            pin_filename = encodeUnicode(pin_filename);
            final String content_url = "http://www.pinterest.com/pin/" + pin_id + "/";
            final DownloadLink dl = createDownloadlink(content_url);
            try {
                dl.setContentUrl(content_url);
                dl.setLinkID(pin_id);
            } catch (final Throwable e) {
                /* Not supported in old 0.9.581 Stable */
                dl.setBrowserUrl(content_url);
                dl.setProperty("LINKDUPEID", pin_id);
            }
            dl._setFilePackage(fp);
            if (directlink != null) {
                dl.setProperty("free_directlink", directlink);
            }
            dl.setProperty("decryptedfilename", pin_filename);
            dl.setFinalFileName(pin_filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* Not supported in old 0.9.581 Stable */
            }
        }
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "deprecation", "static-access" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("pinterest.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.PinterestCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    private void prepAPIBR(final Browser br) throws PluginException {
        jd.plugins.hoster.PinterestCom.prepAPIBR(br);
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

}
