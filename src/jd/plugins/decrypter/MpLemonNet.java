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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3lemon.net" }, urls = { "http://(www\\.)?mp3lemon\\.(net|org)/((song|album)/\\d+/|download\\.php\\?idfile=\\d+)" }, flags = { 0 })
public class MpLemonNet extends PluginForDecrypt {

    public MpLemonNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mp3lemon.org", "mp3lemon.net");
        if (parameter.contains("download.php?idfile=")) parameter = parameter.replace("download.php?idfile=", "song/") + "/";
        br.setFollowRedirects(false);
        br.setCustomCharset("windows-1251");
        br.getPage(parameter);
        if (parameter.contains("/song/")) {
            String filename = br.getRegex("<TD class=\"razdel\" width=\"100%\">Скачать песню: (.*?)</TD>").getMatch(0);
            if (filename == null) filename = br.getRegex("<table class=\"song\"><tr><td><h1 style=\"display: inline;\">(.*?)</h1></td>").getMatch(0);
            String finallink = decryptSingleLink(new Regex(parameter, "mp3lemon\\.net/song/(\\d+)/").getMatch(0));
            if (filename == null || finallink == null) {
                logger.warning("mp3link-decrypt failed: " + parameter);
                logger.warning(br.toString());
                return null;
            }
            DownloadLink dlllink = createDownloadlink("directhttp://" + finallink);
            dlllink.setFinalFileName(filename + ".mp3");
            decryptedLinks.add(dlllink);
        } else {
            String[][] fileInfo = br.getRegex(">\\+</a></td><td class=\"list_tracks\"><a href=\"/song/(\\d+)/.{1,50}\">(.*?)</a><br/>").getMatches();
            String artist = br.getRegex("<tr><td><a style=\"color: #ff462a; font-size: 20px;\" href=\"/artist/\\d+/\">(.*?)</a></td></tr>").getMatch(0);
            if (fileInfo == null || fileInfo.length == 0) return null;
            String albumName = br.getRegex("<tr><td style=\"font-size: 15px; font-weight: bold;\">(.*?)</td></tr>").getMatch(0);
            progress.setRange(fileInfo.length);
            for (String[] dl : fileInfo) {
                String finallink = dl[0];
                String filename = dl[1];
                if (finallink == null || filename == null) return null;
                finallink = decryptSingleLink(dl[0]);
                if (finallink == null) return null;
                DownloadLink dlllink = createDownloadlink("directhttp://" + finallink);
                if (artist != null)
                    dlllink.setFinalFileName(artist + " - " + filename + ".mp3");
                else
                    dlllink.setFinalFileName(filename + ".mp3");
                decryptedLinks.add(dlllink);
                progress.increase(1);
            }
            if (artist != null && albumName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(artist + " - " + albumName);
                fp.addLinks(decryptedLinks);
            } else if (albumName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(albumName);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private String decryptSingleLink(String fID) throws IOException {
        br.getPage("http://mp3lemon.net/download.php?idfile=" + fID);
        return br.getRedirectLocation();
    }
}
