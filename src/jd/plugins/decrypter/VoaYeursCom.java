//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "voayeurs.com" }, urls = { "http://(www\\.)?voayeurs\\.com/(video_\\d+/.*?|.*?\\d+)\\.html" })
public class VoaYeursCom extends PornEmbedParser {
    public VoaYeursCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>([^<>\"]*?) \\| VoAyeurs.com[^<>]*?</title>").getMatch(0);
        logger.info("filename: " + filename);
        if (filename == null) {
            filename = br.getRegex("<h1[^<>]*>([^<>\"]*?)</h1>").getMatch(0);
            logger.info("filename: " + filename);
        }
        String reproductor = br.getRegex("src=\"(/reproductor/[^<>\"]*?)\"").getMatch(0);
        if (reproductor != null) {
            br.getPage(reproductor);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.isEmpty()) {
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}