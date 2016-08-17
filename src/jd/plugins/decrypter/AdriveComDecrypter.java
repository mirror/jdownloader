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

import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adrive.com" }, urls = { "https?://(www\\.)?adrive\\.com/public/[0-9a-zA-Z]+" }) 
public class AdriveComDecrypter extends PluginForDecrypt {

    public AdriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final String parameter = Encoding.htmlDecode(param.toString());
        final String fid = new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0);
        br.getPage(parameter);
        final String continuelink = br.getRegex("\"(https?://\\w+\\.adrive.com/public/(?:view/)?[A-Za-z0-9]+\\.html)\"").getMatch(0);
        if (br.containsHTML("The file you are trying to access is no longer available publicly\\.|The public file you are trying to download is associated with a non\\-valid ADrive") || br.getURL().equals("https://www.adrive.com/login") || continuelink == null) {
            decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
            return decryptedLinks;
        }
        // continue links can be direct download links
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(continuelink);
            if (con.getContentType().contains("html")) {
                br.followConnection();
            } else {
                String filename = getFileNameFromHeader(con);
                long filesize = con.getContentLength();
                if (filename != null && filesize > 0) {
                    final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));

                    dl.setDownloadSize(filesize);
                    dl.setFinalFileName(filename);
                    dl.setProperty("LINKDUPEID", "adrivecom://" + fid);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("directlink", br.getURL());
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        // content can be offline!
        if (br.toString().matches("<b>File doesn't exist\\. Please turn on javascript\\.</b>\\s*<script> window.top.location=\"http://www\\.adrive\\.com/public/noexist\"; </script>")) {
            decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
            return decryptedLinks;
        }

        final String linktext = br.getRegex("<table>(.*?)</table>").getMatch(0);
        if (linktext == null) {
            // not always defect! -raztoki20160119
            if (br.toString().startsWith("<b>File overlimit.")) {
                // we can assume its a SINGLE file! we wont know its size
                final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filename = getFileNameFromURL(new URL(continuelink));
                dl.setFinalFileName(filename);
                dl.setProperty("LINKDUPEID", "adrivecom://" + fid + "/" + filename);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", continuelink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        final String[] links = new Regex(linktext, "<tr>(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleinfo : links) {
            final Regex info = new Regex(singleinfo, "<td class=\"name\">[\t\n\r ]+<a href=\"(https?://download[^<>\"]*?)\">([^<>\"]*?)</a>");
            final String directlink = info.getMatch(0);
            String filename = info.getMatch(1);
            final String filesize = new Regex(singleinfo, "<td class=\"size\">([^<>\"]*?)</td>").getMatch(0);

            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            if (filesize == null || directlink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setFinalFileName(filename);
            dl.setProperty("LINKDUPEID", "adrivecom://" + fid + "/" + filename);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("directlink", directlink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
