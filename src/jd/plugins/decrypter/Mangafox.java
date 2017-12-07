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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangafox.me" }, urls = { "https?://[\\w\\.]*?mangafox\\.(com|me|mobi|la)/manga/.*?/(v\\d+/c[\\d\\.]+|c[\\d\\.]+)" })
public class Mangafox extends PluginForDecrypt {
    public Mangafox(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal("mangafox.me", 500);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String url = parameter.toString().replaceAll("://[\\w\\.]*?mangafox\\.(com|me|mobi)/", "://mangafox.la/");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        br.getPage(url + "/1.html");
        if (br.containsHTML("cannot be found|not available yet")) {
            logger.warning("Invalid link or release not yet available, check in your browser: " + parameter);
            return decryptedLinks;
        }
        if (!br.containsHTML("onclick=\"return enlarge\\(\\)\"")) {
            logger.warning("Invalid link: " + parameter);
            return decryptedLinks;
        }
        // We get the title
        String title = br.getRegex("<title>(.*?) \\- Read (.*?) Online \\- Page 1</title>").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for: " + parameter);
            return null;
        }
        title = Encoding.htmlDecode(title.trim());
        int numberOfPages = Integer.parseInt(br.getRegex("of (\\d+)").getMatch(0));
        final DecimalFormat df_page = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
        // We load each page and retrieve the URL of the picture
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        int skippedPics = 0;
        for (int i = 1; i <= numberOfPages; i++) {
            if (i != 1) {
                br.getPage(i + ".html");
            }
            if (isAbort()) {
                break;
            }
            final String[] unformattedSource = br.getRegex("onclick=\"return enlarge\\(\\);?\">\\s*<img src=\"(https?://[^\"]+(\\.[a-z]+)+(?:\\?token=(?:[a-f0-9]{32}|[a-f0-9]{40})&ttl=\\d+)?)\"").getRow(0);
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
            link.setFinalFileName(title + " – page " + df_page.format(i) + extension);
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