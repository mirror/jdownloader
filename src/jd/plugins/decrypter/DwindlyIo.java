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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dwindly.io" }, urls = { "https?://(?:www\\.)?dwindly\\.io/[A-Za-z0-9]+" })
public class DwindlyIo extends PluginForDecrypt {
    public DwindlyIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.toString().length() <= 100) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String continueurl = br.getRegex("document\\.location\\.href = \"(https?://dwindly\\.io/[A-Za-z0-9]+)\"").getMatch(0);
        if (continueurl == null) {
            return null;
        }
        br.getPage(continueurl);
        continueurl = br.getRegex("window\\.open\\(encD\\(\"([^<>\"]+)\"\\)").getMatch(0);
        if (continueurl == null) {
            return null;
        }
        continueurl = Encoding.Base64Decode(continueurl);
        continueurl = "https://" + br.getHost() + "/" + continueurl;
        /* 2018-08-08: Waittime is skippable */
        // this.sleep(5500, param);
        br.setFollowRedirects(false);
        /* Important cookie! */
        br.setCookie(br.getHost(), "s32", "1");
        br.getPage(continueurl);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
