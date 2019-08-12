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

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "scribd.com" }, urls = { "https?://(?:(?:www|[a-z]{2})\\.)?scribd\\.com/(?:(?!doc/)collections/\\d+/[A-Za-z0-9\\-_%]+|user/\\d+/[^/]+|(?:audiobook|listen)/\\d+(?:/[^/]+)?)" })
public class ScribdCom extends PluginForDecrypt {
    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_collections = "https?://(www\\.)?((de|ru|es)\\.)?scribd\\.com/collections/\\d+/[A-Za-z0-9\\-_%]+";
    private static final String type_audiobook   = ".+/(?:audiobook|listen)/(\\d+).*?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://", "https://");
        br.setFollowRedirects(true);
        String fpname;
        final FilePackage fp = FilePackage.getInstance();
        if (parameter.matches(type_collections)) {
            fpname = "scribd.com collection - " + Encoding.htmlDecode(new Regex(parameter, "scribd\\.com/collections/\\d+/([A-Za-z0-9\\-_]+)").getMatch(0));
            final String collection_id = new Regex(parameter, "/collections/(\\d+)/").getMatch(0);
            br.getPage(parameter);
            if (br.getURL().equals("http://www.scribd.com/")) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(getOfflineLink(parameter));
                return decryptedLinks;
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String documentsnum_str = br.getRegex("class=\"subtitle\">(\\d+) books").getMatch(0);
            if (documentsnum_str == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final int documentsNum = Integer.parseInt(documentsnum_str);
            int page = 0;
            int decryptedLinksNum = 0;
            boolean has_more = false;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
                br.getPage("https://www.scribd.com/collections/" + collection_id + "/get_collection_documents?page=" + page);
                br.getRequest().setHtmlCode(Encoding.unicodeDecode(br.toString()));
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.containsHTML("\"objects\":null")) {
                    break;
                }
                final String[][] uplInfo = br.getRegex("href=\"(https?://([a-z]{2}|www)\\.scribd\\.com/(?:doc|document)/[^<>\"]*?)\">([^<>]*?)</a>").getMatches();
                if (uplInfo == null || uplInfo.length == 0) {
                    break;
                }
                for (final String[] docinfo : uplInfo) {
                    final String link = docinfo[0];
                    String title = docinfo[2];
                    title = encodeUnicode(title);
                    final DownloadLink dl = createDownloadlink(link);
                    dl.setAvailable(true);
                    dl.setName(title + ".pdf");
                    dl._setFilePackage(fp);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
                decryptedLinksNum += uplInfo.length;
                logger.info("Decrypted page: " + page);
                logger.info("Decrypted " + decryptedLinksNum + " / " + documentsNum);
                page++;
                has_more = this.br.containsHTML("\"has_more\":true");
            } while (decryptedLinksNum < documentsNum);
            if (decryptedLinksNum == 0) {
                if (this.br.containsHTML("/book/")) {
                    logger.info("Collection only contains unsupported urls");
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } else if (parameter.matches(type_audiobook)) {
            final String bookID = new Regex(parameter, type_audiobook).getMatch(0);
            final Account account = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
            if (account == null) {
                logger.info("Cannot crawl that URL without account");
                return decryptedLinks;
            }
            jd.plugins.hoster.ScribdCom.login(this.br, account, false);
            final String userID = account.getStringProperty("userid", null);
            if (StringUtils.isEmpty(userID)) {
                logger.warning("Failed to find userID");
                return null;
            }
            String title = null;
            br.getPage("https://de." + this.getHost() + "/listen/" + bookID);
            LinkedHashMap<String, Object> entries = null;
            String session_key = null;
            try {
                final String json = br.getRegex("ReactDOM\\.render\\(React\\.createElement\\(Scribd\\.Audiobooks\\.Show, (\\{.*?\\})\\), document\\.getElementById").getMatch(0);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
                title = (String) JavaScriptEngineFactory.walkJson(entries, "doc/title");
                session_key = (String) JavaScriptEngineFactory.walkJson(entries, "audiobook/session_key");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            if (StringUtils.isEmpty(title)) {
                title = bookID;
            }
            /*
             * They're using external provider "findawayworld.com" to get these audiobooks so now we need to find their ID of this
             * audiobook.
             */
            final String external_id = PluginJSonUtils.getJson(br, "external_id");
            if (StringUtils.isEmpty(session_key)) {
                logger.warning("Plugin failure or current account is not allowed to stream this audiobook");
                return decryptedLinks;
            } else if (StringUtils.isEmpty(external_id)) {
                return null;
            }
            final String license_id;
            final boolean grabLicenceIDFromWebsite = false;
            if (grabLicenceIDFromWebsite) {
                /* This call will also provide more details about the audiobook e.g. 'drm_free' */
                br.getPage("https://api.findawayworld.com/v4/accounts/scribd-" + userID + "/audiobooks/" + external_id);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                /* There are multiple licenses available. Use the first one. */
                license_id = (String) JavaScriptEngineFactory.walkJson(entries, "licenses/{0}/id");
            } else {
                /* 2019-08-12 */
                license_id = "5d51721799c3cd70e3dab9fd";
            }
            br.getHeaders().put("Sec-Fetch-Mode", "cors");
            br.getHeaders().put("Sec-Fetch-Site", "cross-site");
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Session-Key", session_key);
            br.getHeaders().put("Origin", "https://www." + this.getHost());
            /* 2019-08-12: Generated directurls will usually be valid for ~48 hours */
            br.postPageRaw("https://api.findawayworld.com/v4/audiobooks/" + external_id + "/playlists", "{\"license_id\":\"" + license_id + "\"}");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("playlist");
            int counter = 0;
            for (final Object audioO : ressourcelist) {
                counter++;
                entries = (LinkedHashMap<String, Object>) audioO;
                String filename = title;
                final String downloadurl = (String) entries.get("url");
                final long part_number = JavaScriptEngineFactory.toLong(entries.get("part_number"), 0);
                final long chapter_number = JavaScriptEngineFactory.toLong(entries.get("chapter_number"), 0);
                if (part_number > 0) {
                    filename += "_part_" + part_number;
                } else if (chapter_number > 0) {
                    filename += "_chapter_" + chapter_number;
                } else {
                    /* Use internal counter for numbering */
                    filename += "_internal_number_" + counter;
                }
                filename += ".mp3";
                final DownloadLink dl = this.createDownloadlink("directhttp://" + downloadurl);
                dl.setAvailable(true);
                dl.setFinalFileName(filename);
                decryptedLinks.add(dl);
            }
            fpname = title;
        } else {
            /* Crawl all uploads of a user */
            final Regex urlinfo = new Regex(parameter, "scribd\\.com/user/(\\d+)/([^/]+)");
            String id = urlinfo.getMatch(0);
            final String url_username = urlinfo.getMatch(1);
            fpname = "scribd.com uploads of user " + Encoding.htmlDecode(url_username);
            fp.setName(fpname);
            br.getPage(parameter);
            // if(id == null){+
            // id = br.getRegex("\"id\":(\\d+)").getMatch(0);
            // }
            if (id == null) {
                decryptedLinks.add(getOfflineLink(parameter));
                return decryptedLinks;
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            int documentsNum;
            String doccount = br.getRegex("class=\"document_count\">\\((\\d+)\\)</span>").getMatch(0);
            if (doccount == null) {
                doccount = br.getRegex(">(\\d+)</div><div class=\"stat_name\"><span class=\"underline_on_hover\">published</span>").getMatch(0);
            }
            if (doccount != null) {
                documentsNum = Integer.parseInt(doccount);
                if (documentsNum == 0) {
                    decryptedLinks.add(getOfflineLink(parameter));
                    return decryptedLinks;
                }
            } else {
                documentsNum = 36;
            }
            int page = 1;
            int decryptedLinksNum = 0;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
                /* E.g. for "/author/" urls */
                // https://de.scribd.com/profiles/content.json?content_key=authored_documents&id=229905341&page=2
                br.getPage("https://de.scribd.com/profiles/content.json?content_key=all_documents&id=" + id + "&page=" + page);
                if (!this.br.getHttpConnection().getContentType().contains("json")) {
                    decryptedLinks.add(getOfflineLink(parameter));
                    return decryptedLinks;
                }
                br.getRequest().setHtmlCode(Encoding.unicodeDecode(br.toString()));
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.containsHTML("\"objects\":null")) {
                    break;
                }
                final String[][] uplInfo = br.getRegex("href=\"(https?://([a-z]{2}|www)\\.scribd\\.com/(?:doc|document)/[^<>\"]*?)\">([^<>]*?)</a>").getMatches();
                if (uplInfo == null || uplInfo.length == 0) {
                    if (decryptedLinks.size() == 0 && br.containsHTML("\"has_more\":false")) {
                        decryptedLinks.add(getOfflineLink(parameter));
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String[] docinfo : uplInfo) {
                    final String link = docinfo[0];
                    String title = docinfo[2];
                    title = encodeUnicode(title);
                    final DownloadLink dl = createDownloadlink(link);
                    dl.setAvailable(true);
                    dl.setName(title + ".pdf");
                    dl._setFilePackage(fp);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
                decryptedLinksNum += uplInfo.length;
                logger.info("Decrypted page: " + page);
                logger.info("Decrypted " + decryptedLinksNum + " / " + documentsNum);
                page++;
            } while (decryptedLinksNum < documentsNum && this.br.containsHTML("\"has_more\":true"));
        }
        if (decryptedLinks.size() == 0) {
            decryptedLinks.add(getOfflineLink(parameter));
            return decryptedLinks;
        }
        fp.setName(fpname);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private DownloadLink getOfflineLink(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setFinalFileName(new Regex(parameter, "scribd\\.com/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }
}
