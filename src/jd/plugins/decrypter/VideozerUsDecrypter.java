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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videozer.us" }, urls = { "https?://(www\\.)?videozer\\.us/[A-Za-z0-9\\-_]+\\.html" }, flags = { 0 })
public class VideozerUsDecrypter extends PluginForDecrypt {

    public VideozerUsDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final DownloadLink main = createDownloadlink(parameter.replace("videozer.us/", "videozerdecrypted.us/"));
        if (!br.containsHTML("id=\"video\\-player\"")) {
            main.setFinalFileName(new Regex(parameter, "videozer\\.us/([A-Za-z0-9\\-_]+)\\.html" + ".mp4").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        String externID = br.getRegex("file: \\'(http[^<>\"]*?)\\'").getMatch(0);
        if (externID != null && !externID.endsWith(".mp4")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        decryptedLinks.add(main);

        return decryptedLinks;
    }

}
