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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "freefileload.ml" }, urls = { "https?://(?:www\\.)?freefileload\\.ml/.+" })
public class FreefileloadMl extends PluginForDecrypt {
    public FreefileloadMl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // final String redirect = br.getRedirectLocation();
        // if (redirect != null && redirect.contains(this.getHost() + "/")) {
        // decryptedLinks.add(this.createDownloadlink("directhttp://" + redirect));
        // return decryptedLinks;
        // }
        // br.setFollowRedirects(true);
        // if (redirect != null) {
        // br.getPage(redirect);
        // }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setAllowedResponseCodes(new int[] { 400 });
        Form continueForm = br.getForm(0);
        if (continueForm == null) {
            return null;
        }
        br.submitForm(continueForm);
        if (br.getHttpConnection().getResponseCode() == 400) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (continueForm.containsHTML("toDownload")) {
            /* Direct download - 2nd form required and then we'll get our final (direct)downloadurl. Not all URLs have this 2nd Form!! */
            continueForm = br.getForm(0);
            if (continueForm == null) {
                return null;
            }
            br.submitForm(continueForm);
        }
        final String finallink = br.getRegex("location\\.href=\"(http[^\"]+)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
