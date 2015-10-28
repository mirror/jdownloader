//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "batoto.net" }, urls = { "http://bato\\.to/reader#[a-z0-9]+" }, flags = { 0 })
public class BtoNt extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public BtoNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String url = "http://bato.to/areader?id=" + new Regex(parameter.toString(), "([a-z0-9]+)$").getMatch(0) + "&p=";
        // // enforcing one img per page because you can't always get all images displayed on one page.
        // br.setCookie("bato.to", "supress_webtoon", "t");
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.getHeaders().put("Referer", "http://bato.to/reader");
        // Access page one
        br.getPage(url + 1);

        if (br.containsHTML("<div style=\"text-align:center;\"><img src=\"https?://[\\w\\.]*(?:batoto\\.net|bato\\.to)/images/404-Error\\.jpg\" alt=\"File not found\" /></div>|The page you were looking for is no longer available")) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        } else if (br.containsHTML(">This chapter has been removed due to infringement\\.<")) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        }

        // We get the title
        String tag_title = br.getRegex("<title>.*?</title>").getMatch(-1);
        if (tag_title == null) {
            tag_title = this.br.getRegex("document\\.title = \\'([^<>\"]*?) Page \\d+ \\| Batoto\\!';").getMatch(0);
        }
        if (tag_title != null) {
            // cleanup bad html entity
            tag_title = tag_title.replaceAll("&amp;?", "&");
        }
        final FilePackage fp = FilePackage.getInstance();
        // may as well set this globally. it used to belong inside 2 of the formatting if statements
        fp.setProperty("CLEANUP_NAME", false);

        final String title = tag_title;

        String pages = br.getRegex(">page (\\d+)</option>\\s*</select>\\s*</li>").getMatch(0);
        if (pages == null) {
            // even though the cookie is set... they don't always respect this for small page count
            // http://www.batoto.net/read/_/249050/useful-good-for-nothing_ch1_by_suras-place
            /* TODO: Check if this is still working ... */
            br.getPage("?supress_webtoon=t");
            pages = br.getRegex(">page (\\d+)</option>\\s*</select>\\s*</li>").getMatch(0);
        }
        if (pages != null) {
            int numberOfPages = Integer.parseInt(pages);
            DecimalFormat df_page = new DecimalFormat("00");
            if (numberOfPages > 999) {
                df_page = new DecimalFormat("0000");
            } else if (numberOfPages > 99) {
                df_page = new DecimalFormat("000");
            }

            // We load each page and retrieve the URL of the picture
            fp.setName(title);
            int skippedPics = 0;
            for (int i = 1; i <= numberOfPages; i++) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                if (i != 1) {
                    this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    this.br.getHeaders().put("Referer", "http://bato.to/reader");
                    br.getPage(url + i);
                }
                String pageNumber = df_page.format(i);
                // /comics/2014/02/02/1/read52ee48ff90491/img000001.jpg /comics/date/date/date/first[0-z]charof title/read+hash/img\\d+
                String[] unformattedSource = br.getRegex("src=\"(https?://img\\.(?:batoto\\.net|bato\\.to)/comics/\\d{4}/\\d{1,2}/\\d{1,2}/[a-z0-9]/read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
                if (unformattedSource == null) {
                    // <img
                    // src="http://arc.bato.to/comics/t/toloverudarkness/0.5/cxc-scans/English/read4d69e24fa5247/%5BToLoveRuDarkness%5D-ch00_004d69e250dd8a4.jpg"
                    unformattedSource = br.getRegex("<img[^>]+src=\"(https?://\\w+\\.(?:batoto\\.net|bato\\.to)/comics/(?:[^/]+/){1,}read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
                }
                if (unformattedSource == null || unformattedSource.length == 0) {
                    skippedPics++;
                    if (skippedPics > 5) {
                        logger.info("Too many links were skipped, stopping...");
                        break;
                    }
                    continue;
                }
                final String source = unformattedSource[0];
                final String extension = unformattedSource[1];
                final DownloadLink link = createDownloadlink("directhttp://" + source);
                link.setFinalFileName(title + " - Page " + pageNumber + extension);
                link.setAvailable(true);
                fp.add(link);
                distribute(link);
                decryptedLinks.add(link);
            }
        } else {
            logger.warning("Decrypter broken for: " + parameter + " @ pages");
            return null;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}