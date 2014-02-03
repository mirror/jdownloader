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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "porn.to" }, urls = { "http://(www\\.)?porn\\.to/video/[a-z0-9\\-]+" }, flags = { 0 })
public class PornToDecrypter extends PluginForDecrypt {

    public PornToDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        final DownloadLink main = createDownloadlink(parameter.replace("porn.to/", "porndecrypted.to/"));
        br.getPage(parameter);
        final String redirect = br.getRedirectLocation();
        if ("http://www.porn.to/".equals(redirect) || br.containsHTML("<title>Watch Free Porn Videos and Amateur Sex Movies</title>")) {
            main.setAvailable(false);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        if (redirect != null && !redirect.contains("porn.to/")) {
            decryptedLinks.add(createDownloadlink(redirect));
        } else {
            decryptedLinks.add(main);
        }

        return decryptedLinks;
    }

}
