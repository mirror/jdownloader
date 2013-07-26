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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "veoh.com" }, urls = { "http://(www\\.)?veoh\\.com/((browse/videos/category/.*?/)?watch/[A-Za-z0-9]+|videos/[A-Za-z0-9]+)" }, flags = { 0 })
public class VeohComDecrypter extends PluginForDecrypt {

    public VeohComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * This decrypter exists to find embedded videos. If none are there the video is hosted on veoh.com itself so the link will then be
     * passed over to the hoster plugin.
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // /videos/ are embed link format. so correct it here, and away you go!
        String parameter = param.toString().replace(".com/videos/", ".com/watch/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String externID = br.getRegex("<param name=\"movie\" value=\"(http://(www\\.)?youtube\\.com/v/[^<>\"/]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("veoh.com/", "veohdecrypted.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}