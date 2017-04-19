//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "watchnaruto.tv" }, urls = { "https?://(www\\.)?watchnaruto\\.tv/watch/[a-z0-9\\-_/]+" })
public class WatchVideo extends PluginForDecrypt {

    public WatchVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (parameter.contains("watchnaruto")) {
            final String iframe = br.getRegex("<iframe src=\"([^<>\"]*?)\"").getMatch(0);
            if (iframe == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            } // logger.info("iframe: " + iframe);
            br.getPage(iframe);
            final String[] vps = { "1080p", "720p", "480p", "360p" };
            for (final String vp : vps) {
                String file = br.getRegex("file: \"(https?://[^\"]+?)\",\\s*label: \"" + vp + "\"").getMatch(0);
                logger.info("file: " + file);
                DownloadLink dl = createDownloadlink(file);
                dl.setFinalFileName(filename + "." + vp + ".mp4");
                dl.setProperty("refresh_url_plugin", iframe);
                decryptedLinks.add(dl);
            }
            String vtt = br.getRegex("file: '([^<>\"]*?.vtt)'").getMatch(0);
            if (vtt != null && !vtt.contains("http")) {
                vtt = "http:" + vtt;
            }
            logger.info("vtt: " + vtt);
            DownloadLink dl = createDownloadlink(vtt);
            dl.setFinalFileName(filename + ".vtt");
            dl.setProperty("refresh_url_plugin", iframe);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}