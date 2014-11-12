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
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scribd.com" }, urls = { "https?://(www\\.)?((de|ru|es)\\.)?scribd\\.com/(?!doc/)[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class ScribdCom extends PluginForDecrypt {

    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_collections = "https?://(www\\.)?((de|ru|es)\\.)?scribd\\.com/collections/\\d+/[A-Za-z0-9\\-_]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://", "https://");
        br.setFollowRedirects(true);
        String fpname;
        if (parameter.matches(type_collections)) {
            br.getPage(parameter);
            if (br.getURL().equals("http://www.scribd.com/")) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(getOfflineLink(parameter));
                return decryptedLinks;
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final int documentsNum = Integer.parseInt(br.getRegex("class=\"document_count\">\\((\\d+)\\)</span>").getMatch(0));
            int page = 1;
            int decryptedLinksNum = 0;
            do {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                br.getPage("http://www.scribd.com/profiles/collections/get_collection_documents?page=" + page + "&id=" + new Regex(parameter, "/collections/(\\d+)/").getMatch(0));
                br.getRequest().setHtmlCode(unescape(br.toString()));
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.containsHTML("\"objects\":null")) {
                    break;
                }
                final String[] links = br.getRegex("data\\-object_id=\"(\\d+)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String singleLink : links) {
                    final DownloadLink dl = createDownloadlink("http://www.scribd.com/doc/" + singleLink);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    decryptedLinks.add(dl);
                }
                decryptedLinksNum += links.length;
                logger.info("Decrypted page: " + page);
                logger.info("Decrypted " + decryptedLinksNum + " / " + documentsNum);
                page++;
            } while (decryptedLinksNum < documentsNum);
            if (decryptedLinksNum == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpname = "scribd.com collection - " + new Regex(parameter, "scribd\\.com/collections/\\d+/([A-Za-z0-9\\-_]+)").getMatch(0);
        } else {
            if (!parameter.endsWith("/documents")) {
                parameter += "/documents";
            }
            try {
                br.getPage(parameter);
            } catch (final BrowserException e) {
                if (br.getHttpConnection().getResponseCode() == 410) {
                    decryptedLinks.add(getOfflineLink(parameter));
                    return decryptedLinks;
                }
                throw e;
            }
            final String id = br.getRegex("\"id\":(\\d+)").getMatch(0);
            if (id == null) {
                decryptedLinks.add(getOfflineLink(parameter));
                return decryptedLinks;
            }

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            int documentsNum;
            final String doccount = br.getRegex("class=\"document_count\">\\((\\d+)\\)</span>").getMatch(0);
            if (doccount != null) {
                documentsNum = Integer.parseInt(doccount);
            } else {
                documentsNum = 36;
            }
            int page = 1;
            int decryptedLinksNum = 0;
            do {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                br.getPage("https://de.scribd.com/profiles/documents/get_documents?page=" + page + "&sort_by=hotness_pmp_first&id=" + id);
                br.getRequest().setHtmlCode(unescape(br.toString()));
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.containsHTML("\"objects\":null")) {
                    break;
                }
                final String[] uploads = br.getRegex("class=\"document_title\"><a href=\"(https?://[a-z]{2}\\.scribd\\.com/doc/[^<>\"]*?)\"").getColumn(0);
                if (uploads == null || uploads.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String singleLink : uploads) {
                    decryptedLinks.add(createDownloadlink(singleLink));
                }
                for (final String singleLink : uploads) {
                    final DownloadLink dl = createDownloadlink("http://www.scribd.com/doc/" + singleLink);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    decryptedLinks.add(dl);
                }
                decryptedLinksNum += uploads.length;
                logger.info("Decrypted page: " + page);
                logger.info("Decrypted " + decryptedLinksNum + " / " + documentsNum);
                page++;
            } while (decryptedLinksNum < documentsNum);
            fpname = "scribd.com uploads of user " + new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
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

    private static AtomicBoolean yt_loaded = new AtomicBoolean(false);

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (!yt_loaded.getAndSet(true)) {
            JDUtilities.getPluginForHost("youtube.com");
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

}
