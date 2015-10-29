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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bato.to" }, urls = { "http://bato\\.to/reader#[a-z0-9]+" }, flags = { 0 })
public class BatoTo extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public BatoTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        /* Login if possible */
        final PluginForHost host_plugin = JDUtilities.getPluginForHost("bato.to");
        final Account acc = AccountController.getInstance().getValidAccount(host_plugin);
        if (acc != null) {
            try {
                jd.plugins.hoster.BatoTo.login(this.br, acc, false);
            } catch (final Throwable e) {
            }
        }

        final String id = new Regex(parameter.toString(), "([a-z0-9]+)$").getMatch(0);
        final String url = "http://bato.to/areader?id=" + id + "&p=";
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
            for (int i = 1; i <= numberOfPages; i++) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
                final String pageNumber = df_page.format(i);
                final DownloadLink link = createDownloadlink("http://bato.to/areader?id=" + id + "&p=" + pageNumber);
                final String fname_without_ext = title + " - Page " + pageNumber;
                link.setProperty("fname_without_ext", fname_without_ext);
                link.setName(fname_without_ext + ".png");
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