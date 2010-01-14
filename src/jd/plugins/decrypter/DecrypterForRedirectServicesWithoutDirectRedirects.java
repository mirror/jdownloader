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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adf.ly", "download.su" }, urls = { "http://[\\w\\.]*?adf\\.ly/[a-z0-9]+", "http://[\\w\\.]*?download\\.su/go/\\?id=.*?=/files/\\d+/.+" }, flags = { 0, 0 })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends PluginForDecrypt {

    public DecrypterForRedirectServicesWithoutDirectRedirects(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        String finallink = null;
        br.getPage(parameter);
        if (parameter.contains("adf.ly"))
            finallink = br.getRegex("default_location = '(.*?)';").getMatch(0);
        else if (parameter.contains("download.su")) {
            String rspart = new Regex(parameter + "\"", "(/files.*?)\"").getMatch(0);
            finallink = "http://rapidshare.com" + rspart;
        }
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
