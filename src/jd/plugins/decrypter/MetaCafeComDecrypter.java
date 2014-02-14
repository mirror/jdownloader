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
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metacafe.com" }, urls = { "http://(www\\.)?metacafe\\.com/((watch|fplayer)/(sy\\-)?\\d+/.{1}|watch/yt\\-[A-Za-z0-9\\-_]+/.{1})" }, flags = { 0 })
public class MetaCafeComDecrypter extends PluginForDecrypt {

    public MetaCafeComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_YOUTUBEEMBED = "http://(www\\.)?metacafe\\.com/watch/yt\\-[A-Za-z0-9\\-_]+/.{1}";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("/fplayer/", "/watch/");
        if (parameter.matches(TYPE_YOUTUBEEMBED)) {
            final String ytid = new Regex(parameter, "metacafe\\.com/watch/yt\\-([A-Za-z0-9\\-_]+)/").getMatch(0);
            decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + ytid));
            return decryptedLinks;
        }
        final DownloadLink main = createDownloadlink(parameter.replace("metacafe.com/", "metacafedecrypted.com/"));
        main.setName(new Regex(parameter, "(\\d+)/.{1}$").getMatch(0));
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            if (br.getRequest().getHttpConnection().getResponseCode() == 400) {
                main.setAvailable(false);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        if (br.getURL().contains("/?pageNotFound") || br.containsHTML("<title>Metacafe \\- Best Videos \\&amp; Funny Movies</title>") || br.getURL().contains("metacafe.com/?m=removed")) {
            main.setAvailable(false);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        final String externID = br.getRegex("src=\"(//(www\\.)?youtube\\.com/embed/[A-Za-z0-9\\-_]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http" + externID));
            return decryptedLinks;
        }
        String fileName = br.getRegex("name=\"title\" content=\"(.*?) \\- Video\"").getMatch(0);
        if (fileName == null) fileName = br.getRegex("<h1 id=\"ItemTitle\" >(.*?)</h1>").getMatch(0);
        if (fileName != null) {
            main.setFinalFileName(fileName.trim() + ".mp4");
            main.setAvailable(true);
        }
        decryptedLinks.add(main);

        return decryptedLinks;
    }

}
