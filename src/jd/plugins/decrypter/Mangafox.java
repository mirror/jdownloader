//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangafox.me" }, urls = { "https?://[\\w\\.]*?(?:mangafox\\.(com|me|mobi|la)|fanfox\\.net)/manga/[A-Za-z0-9\\-_]+/((v[A-Za-z0-9]+/c[\\d\\.]+|c[\\d\\.]+))?" })
public class Mangafox extends PluginForDecrypt {
    public Mangafox(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal("fanfox.net", 500);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        /* Change URL to current domain */
        String url = parameter.toString().replaceAll("://[\\w\\.]*?mangafox\\.(com|me|mobi|la)/", "://fanfox.net/");
        br.setCookie(new URL(url).getHost(), "isAdult", "1");
        if (url.matches("^https?://[^/]+/manga/[^/]+/?$")) {
            /* Manga overview page - find chapters/volumes which then go back into decrypter. */
            br.getPage(url);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(url));
                return decryptedLinks;
            }
            /* Try to only grab relevant chapters / Volumes! */
            String[] htmls = br.getRegex("id=\"list\\-\\d+\".*?class=\"detail-main-list-more\"").getColumn(-1);
            if (htmls == null || htmls.length == 0) {
                /* Fallback */
                htmls = new String[] { br.toString() };
            }
            for (final String html : htmls) {
                if (htmls.length > 1) {
                    /* E.g. class="">VOL<span>（1251）</span> */
                    final String listname = new Regex(html, "class=\"\">([^<>\"]+)<span>").getMatch(0);
                    if (listname != null) {
                        logger.info("Crawling list: " + listname);
                    }
                }
                final String[] chapters = new Regex(html, "\"(/manga/[^\"]+/c\\d+/\\d+\\.html)\"").getColumn(0);
                if (chapters == null || chapters.length == 0) {
                    return null;
                }
                for (String chapterurl : chapters) {
                    chapterurl = "http://" + br.getHost() + chapterurl;
                    decryptedLinks.add(this.createDownloadlink(chapterurl));
                }
            }
            return decryptedLinks;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        /* Access URL of first picture */
        br.getPage(url + "/1.html");
        if (jd.plugins.hoster.Mangafox.isOffline(br)) {
            if (br.containsHTML("Caution to under-aged viewers") || br.containsHTML("\"id\\s*=\\s*\"checkAdult\"")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                decryptedLinks.add(this.createOfflinelink(url));
                return decryptedLinks;
            }
        }
        String title = br.getRegex("<title>(.*?) \\- Read (.*?) Online \\- Page 1</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for: " + parameter);
            return null;
        }
        title = Encoding.htmlDecode(title.trim());
        int numberOfPages = 0;
        String maxPage = br.getRegex("var imagecount\\s*?=\\s*?(\\d+);").getMatch(0);
        if (maxPage == null) {
            maxPage = br.getRegex("of (\\d+)").getMatch(0);
        }
        if (maxPage != null) {
            numberOfPages = Integer.parseInt(maxPage);
        }
        if (numberOfPages == 0) {
            /* 2018-12-04: New */
            final String[] pages = this.br.getRegex("data\\-page=\"(\\d+)\"").getColumn(0);
            for (final String page_temp_str : pages) {
                final short page_temp = Short.parseShort(page_temp_str);
                if (page_temp > numberOfPages) {
                    numberOfPages = page_temp;
                }
            }
        }
        if (numberOfPages == 0) {
            return null;
        }
        final DecimalFormat df_page = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
        // We load each page and retrieve the URL of the picture
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        // int skippedPics = 0;
        br.addAllowedResponseCodes(503);
        for (int i = 1; i <= numberOfPages; i++) {
            // if (i != 1) {
            // br.getPage(i + ".html");
            // if (br.getRequest().getHttpConnection().getResponseCode() == 503) {
            // sleep(2000, parameter);
            // br.getPage(i + ".html");
            // }
            // }
            if (isAbort()) {
                break;
            }
            // final String[] unformattedSource = br.getRegex("onclick=\"return enlarge\\(\\);?\">\\s*<img
            // src=\"(https?://[^\"]+(\\.[a-z]+)+(?:\\?token=(?:[a-f0-9]{32}|[a-f0-9]{40})&ttl=\\d+)?)\"").getRow(0);
            // if (unformattedSource == null || unformattedSource.length == 0) {
            // skippedPics++;
            // if (skippedPics > 5) {
            // logger.info("Too many links were skipped, stopping...");
            // break;
            // }
            // continue;
            // }
            // String source = unformattedSource[0];
            // String extension = unformattedSource[1];
            final String extension = ".jpg";
            final String contentURL = url + "/" + i + ".html";
            final DownloadLink link = createDownloadlink(contentURL);
            link.setFinalFileName(title + " – page " + df_page.format(i) + extension);
            link.setAvailable(true);
            fp.add(link);
            distribute(link);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}