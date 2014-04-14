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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 25040 $", interfaceVersion = 2, names = { "batoto.net" }, urls = { "http://[\\w\\.]*?batoto\\.net/read/_/\\d+/[\\w\\-_\\.]+" }, flags = { 0 })
public class BtoNt extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public BtoNt(PluginWrapper wrapper) {
        super(wrapper);
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
        br.getPage(url + "/1");

        if (br.containsHTML("<div style=\"text-align:center;\"><img src=\"http://www.batoto.net/images/404-Error.jpg\" alt=\"File not found\" /></div>")) {
            logger.warning("Invalid link or release not yet available, check in your browser: " + parameter);
            return decryptedLinks;
        }
        // We get the title
        String[] t = new String[6];
        // works for individual pages, with and without volume, and all in one page
        String reg = "<title>(.*?) - (vol ([\\d\\.]+) )?(ch ([\\d\\.]+) )(Page [\\d\\.]+ )?\\|[^<]+</title";
        t = br.getRegex(reg).getRow(0);
        if (t == null) {
            logger.warning("Decrypter broken for: " + parameter);
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();

        DecimalFormat df_title = new DecimalFormat("000");
        // decimal place fks with formatting!
        if (t[2] != null && t[2].contains(".")) {
            String[] s = new Regex(t[2], "(\\d+)(\\.\\d+)").getRow(0);
            fp.setProperty("CLEANUP_NAME", false);
            t[2] = df_title.format(Integer.parseInt(s[0])) + s[1];
        } else {
            t[2] = df_title.format(Integer.parseInt(t[2]));
        }
        if (t[4] != null && t[4].contains(".")) {
            String[] s = new Regex(t[4], "(\\d+)(\\.\\d+)").getRow(0);
            fp.setProperty("CLEANUP_NAME", false);
            t[4] = df_title.format(Integer.parseInt(s[0])) + s[1];
        } else {
            t[4] = df_title.format(Integer.parseInt(t[4]));
        }
        final String title = Encoding.htmlDecode(t[0].trim() + " -" + (t[2] != null ? " Volume " + t[2] : "") + " Chapter " + t[4]);

        String pages = br.getRegex(">page (\\d+)</option>\\s*</select></li>").getMatch(0);
        if (pages != null) {
            int numberOfPages = Integer.parseInt(pages);
            DecimalFormat df_page = new DecimalFormat("00");
            if (numberOfPages > 999)
                df_page = new DecimalFormat("0000");
            else if (numberOfPages > 99) df_page = new DecimalFormat("000");

            // We load each page and retrieve the URL of the picture
            fp.setName(title);
            int skippedPics = 0;
            for (int i = 1; i <= numberOfPages; i++) {
                if (i != 1) br.getPage(url + "/" + i);
                String pageNumber = df_page.format(i);
                final String[] unformattedSource = br.getRegex("src=\"(http://img\\.batoto\\.net/comics/\\d{4}/\\d{1,2}/\\d{1,2}/[a-z]/read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
                if (unformattedSource == null || unformattedSource.length == 0) {
                    skippedPics++;
                    if (skippedPics > 5) {
                        logger.info("Too many links were skipped, stopping...");
                        break;
                    }
                    continue;
                }
                String source = unformattedSource[0];
                String extension = unformattedSource[1];
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
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}