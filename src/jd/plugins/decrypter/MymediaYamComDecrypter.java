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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mymedia.yam.com" }, urls = { "http://(www\\.)?mymedia\\.yam\\.com/(m/\\d+|media_playlist_listcontent\\.php\\?pID=\\d+(\\&numrw=\\d+\\&)?|embed_playlist\\.swf\\?pID=\\d+)" }, flags = { 0 })
public class MymediaYamComDecrypter extends PluginForDecrypt {

    public MymediaYamComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PLAYLISTLINK      = "http://(www\\.)?mymedia\\.yam\\.com/media_playlist_listcontent\\.php\\?pID=\\d+(\\&numrw=\\d+\\&)?";
    private static final String EMBEDPLAYLISTLINK = "http://(www\\.)?mymedia\\.yam\\.com/embed_playlist\\.swf\\?pID=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("utf-8");
        br.setReadTimeout(3 * 60 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 60 * 1000);

        if (parameter.matches(PLAYLISTLINK) || parameter.matches(EMBEDPLAYLISTLINK)) {
            if (parameter.matches(EMBEDPLAYLISTLINK)) {
                parameter = "http://mymedia.yam.com/media_playlist_listcontent.php?pID=" + new Regex(parameter, "(\\d+)$").getMatch(0) + "&page=";
            } else {
                if (parameter.endsWith("&")) {
                    parameter += "page=";
                } else {
                    parameter += "&page=";
                }
            }

            br.getPage(parameter);

            int highestPage = 0;
            final String[] pages = br.getRegex("ChangePage\\((\\d+),").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String page : pages) {
                    final int curPage = Integer.parseInt(page);
                    if (curPage > highestPage) highestPage = curPage;
                }
            }

            for (int i = 0; i <= highestPage; i++) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decrypter stopped!");
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not supported in old 0.9.581 Stable
                }
                br.getPage(parameter + i);

                if (i > 2 && decryptedLinks.size() == 0) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }

                final String links[] = br.getRegex("class=\"blue_13\">[\t\n\r ]+<a href=\"(/m/\\d+)\"").getColumn(0);

                if (links != null && links.length != 0) {
                    for (final String link : links) {
                        decryptedLinks.add(createDownloadlink("http://mymedia.yam.com" + link));
                    }
                }
            }
        } else {
            br.getPage(parameter);

            if (br.containsHTML("使用者影音平台存取發生錯誤<")) {
                final DownloadLink dl = createDownloadlink(parameter.replace("mymedia.yam.com/", "mymediadecrypted.yam.com/"));
                dl.setAvailable(false);
                dl.setProperty("offline", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

            String externID = br.getRegex("name=\"movie\" value=\"(http://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\\&").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }

            final DownloadLink dl = createDownloadlink(parameter.replace("mymedia.yam.com/", "mymediadecrypted.yam.com/"));
            String filename = br.getRegex("class=\"heading\"><span style=\\'float:left;\\'>([^<>\"]*?)</span>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>yam 天空部落-影音分享\\-(.*?)</title>").getMatch(0);
            if (filename != null) {
                // Set to .mp3, can be changed later in hostplugin
                dl.setName(Encoding.htmlDecode(filename.trim()) + ".mp3");
                dl.setAvailable(true);
            }
            if (br.containsHTML("type=\"password\" id=\"passwd\" name=\"passwd\"")) dl.getLinkStatus().setStatusText("Password protected links aren't supported yet. Please contact our support!");
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }
}
