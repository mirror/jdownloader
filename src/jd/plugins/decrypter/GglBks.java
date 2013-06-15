//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "books.google.com" }, urls = { "https?://books\\.google(\\.[a-z]+){1,2}/books\\?id=[0-9a-zA-Z-_]+.*" }, flags = { 0 })
public class GglBks extends PluginForDecrypt {

    private final boolean useRUA = true;

    public GglBks(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String               agent  = null;
    private static AtomicBoolean loaded = new AtomicBoolean(false);

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        if (useRUA) {
            if (agent == null) {
                /* we first have to load the plugin, before we can reference it */
                if (!loaded.getAndSet(true)) JDUtilities.getPluginForHost("mediafire.com");
                agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
            }
            prepBr.getHeaders().put("User-Agent", agent);
        }
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        return prepBr;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String uid = new Regex(parameter, "(&|\\?)id=([A-Za-z0-9_\\-]+)").getMatch(1);
        String host = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        String book = host + "/books?id=" + uid + "&printsec=frontcover&source=gbs_v2_summary_r";
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(book);

        // page moved + capcha - secure for automatic downloads
        if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*")) {
            book = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getRegex("<A HREF=\"(http://sorry.google.com/sorry/\\?continue=http://books.google.com/books.*?)\">").getMatch(0);
            if (book == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            logger.info("Reconnect needed to continue downloading, quitting decrypter...");
            return decryptedLinks;
            // TODO: can make redirect and capcha but this only for secure to continue connect not for download page
        }
        String bookname = br.getRegex("(fullview\"[^\\}]+)").getMatch(0);
        bookname = getJson(bookname, "title");

        String[] links = br.getRegex("\\{\\\"pid\\\":\\\"(.*?)\\\"").getColumn(0);
        if (links == null || links.length == 0) {
            if (bookname == null) {
                // not a available for download?
                logger.info("No download avialable for this book : " + book);
                return decryptedLinks;
            } // else book title found links not..
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        // if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 *
        // 60 * 1000); }

        for (String dl : links) {
            DownloadLink link = createDownloadlink(book.replace("books.google", "googlebooksdecrypter") + "&pg=" + dl);
            int counter = 120000;
            String filenumber = new Regex(dl, ".*?(\\d+)").getMatch(0);
            if (filenumber != null && !dl.contains("-")) {
                counter = counter + Integer.parseInt(filenumber);
                String regexedCounter = new Regex(Integer.toString(counter), "12(\\d+)").getMatch(0);
                link.setName(dl.replace(filenumber, "") + regexedCounter + "-" + bookname.replaceAll("\\s+", "_"));
            } else {
                link.setName(dl + "-" + bookname.replaceAll("\\s+", "_") + ".jpg");
            }
            link.setAvailable(true);
            decryptedLinks.add(link);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(bookname));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getJson(String source, String value) {
        String result = new Regex(source, "\"" + value + "\":\"([^\"]+)\"").getMatch(0);
        return result;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}