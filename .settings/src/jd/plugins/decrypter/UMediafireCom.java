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

@DecrypterPlugin(revision = "$Revision: 16966 $", interfaceVersion = 2, names = { "umediafire.com" }, urls = { "http://(www\\.)?umediafire\\.com/\\?d=[A-Za-z0-9]+" }, flags = { 0 })
public class UMediafireCom extends PluginForDecrypt {

    public UMediafireCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String iFrame = br.getRegex("\"(index\\.php\\?get=\\d+)\"").getMatch(0);
        if (iFrame == null) {
            logger.info("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://umediafire.com/" + iFrame);
        if (br.containsHTML("google\\.com/recaptcha")) {
            br.getPage("http://umediafire.com/" + iFrame);
            if (br.containsHTML("google\\.com/recaptcha")) {
                logger.warning("Couldn't skip captcha and there is no captcha handling, stopping, link: " + parameter);
                return null;
            }
        }
        System.out.println(br.toString());
        final String check = br.getRegex("var check = \\'(http://[^<>\"]*?)\\';").getMatch(0);
        if (check == null) {
            final String finallink = br.getRegex("var link = \"(http://[^<>\"]*?)\";").getMatch(0);
            if (finallink == null) {
                logger.info("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            decryptedLinks.add(createDownloadlink("directhttp://" + check));
        }
        return decryptedLinks;
    }

}
