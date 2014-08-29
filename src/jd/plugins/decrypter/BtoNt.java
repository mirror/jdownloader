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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "batoto.net" }, urls = { "http://[\\w\\.]*?batoto\\.net/read/_/\\d+/[\\w\\-_\\.]+" }, flags = { 0 })
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
        String url = parameter.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // enforcing one img per page because you can't always get all images displayed on one page.
        br.setCookie(this.getHost(), "supress_webtoon", "t");
        // Access chapter one
        try {
            br.getPage(url + "/1");
        } catch (final BrowserException e) {
            logger.info("Link offline ? (server error): " + parameter);
            return decryptedLinks;
        }

        if (br.containsHTML("<div style=\"text-align:center;\"><img src=\"http://www.batoto.net/images/404-Error\\.jpg\" alt=\"File not found\" /></div>")) {
            logger.warning("Invalid link or release not yet available, check in your browser: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML(">This chapter has been removed due to infringement\\.<")) {
            logger.info("Offline content: " + parameter);
            return decryptedLinks;
        }

        // We get the title
        String[] t = new String[6];
        String tag_title = br.getRegex("<title>.*?</title>").getMatch(-1);
        if (tag_title != null) {
            // cleanup bad html entity
            tag_title = tag_title.replaceAll("&amp;?", "&");
        }
        // works for individual pages, with and without volume, and all in one page
        String reg = "<title>(.*?) - (vol ([\\d\\.]+) )?(ch ([\\d\\.v\\-&]+[a-z]*) )(Page [\\d\\.]+ )?\\|[^<]+</title";
        t = new Regex(tag_title, reg).getRow(0);
        if (t == null) {
            // some times no chapter or page is shown, this is a bug on there side.. we can then construct ourselves.
            String titties = new Regex(tag_title, "<title>(.*?) - (vol ([\\d\\.]+) )?(ch ([\\d\\.v\\-&]+[a-z]*) )?(Page [\\d\\.]+ )?\\|[^<]+</title").getMatch(0);
            String chapter = br.getRegex("selected=\"selected\">Ch\\.([\\d\\.v\\-]+[\\: a-z]*)</option>").getMatch(0);
            if (titties != null && chapter != null) {
                t = new String[6];
                t[0] = titties;
                t[4] = chapter;
            }
            if (t == null) {
                logger.warning("Decrypter broken for: " + parameter + " @ t");
                return null;
            }
        }
        final FilePackage fp = FilePackage.getInstance();

        DecimalFormat df_title = new DecimalFormat("000");
        // some rudimentary cleanup
        if (t[4] != null) {
            t[4] = t[4].replaceAll("[\\-\\: ]", "");
        }

        // decimal place fks with formatting!
        if (t[2] != null && (t[2].contains(".") || t[2].matches(".+[a-z]$"))) {
            String[] s = new Regex(t[2], "(\\d+)(\\.\\d+)?([a-z]*)").getRow(0);
            fp.setProperty("CLEANUP_NAME", false);
            t[2] = df_title.format(Integer.parseInt(s[0])) + (s[1] != null ? s[1] : "") + (s[2] != null ? s[2] : "");
        } else if (t[2] != null) {
            t[2] = df_title.format(Integer.parseInt(t[2]));
        } else if (t[4] != null && (t[4].matches("(\\d+)([\\.v]+\\d+)?([A-Za-z]*)"))) {
            String[] s = new Regex(t[4], "(\\d+)([\\.v]+\\d+)?([A-Za-z]*)").getRow(0);
            fp.setProperty("CLEANUP_NAME", false);
            t[4] = df_title.format(Integer.parseInt(s[0])) + (s[1] != null ? s[1] : "") + (s[2] != null ? s[2] : "");
        } else if (t[4] != null) {
            t[4] = df_title.format(Integer.parseInt(t[4]));
        } else {
            logger.warning("Decrypter broken for: " + parameter + " @ df_title");
            return null;
        }
        final String title = Encoding.htmlDecode(t[0].trim() + " -" + (t[2] != null ? " Volume " + t[2] : "") + " Chapter " + t[4]);

        String pages = br.getRegex(">page (\\d+)</option>\\s*</select></li>").getMatch(0);
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
                    br.getPage(url + "/" + i);
                }
                String pageNumber = df_page.format(i);
                final String[] unformattedSource = br.getRegex("src=\"(http://img\\.(batoto\\.net|bato\\.to)/comics/\\d{4}/\\d{1,2}/\\d{1,2}/[a-z]/read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
                if (unformattedSource == null || unformattedSource.length == 0) {
                    skippedPics++;
                    if (skippedPics > 5) {
                        logger.info("Too many links were skipped, stopping...");
                        break;
                    }
                    continue;
                }
                final String source = unformattedSource[0];
                final String extension = unformattedSource[2];
                final DownloadLink link = createDownloadlink("directhttp://" + source);
                link.setFinalFileName(title + " - Page " + pageNumber + extension);
                fp.add(link);
                try {
                    distribute(link);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
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