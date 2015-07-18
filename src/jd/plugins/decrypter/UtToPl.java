//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 30210 $", interfaceVersion = 2, names = { "utnijto.pl" }, urls = { "https?://(?:www\\.)?utnijto\\.pl/([a-z0-9]+)" }, flags = { 0 })
public class UtToPl extends PluginForDecrypt {

    public UtToPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fuid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        final Form f = br.getFormByInputFieldKeyValue("hidden", fuid);
        if (f == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.submitForm(f);
        final String redirect = br.getRedirectLocation();
        if (redirect == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(redirect));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}