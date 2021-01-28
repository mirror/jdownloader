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
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uprotector.xyz" }, urls = { "https?://(?:www\\.)?(?:4snip\\.pw|uprotector\\.xyz)/out2?/([A-Za-z0-9\\-_]+)" })
public class UprotectorXyz extends PluginForDecrypt {
    public UprotectorXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final boolean continue1Required = false;
        final boolean continue2Required = true;
        Form continueform = null;
        if (continue1Required) {
            br.getPage("https://" + this.getHost() + "/out/" + linkid);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            // if (br.getRedirectLocation() != null && !this.canHandle(br.getRedirectLocation())) {
            // decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            // return decryptedLinks;
            // }
            continueform = br.getFormbyProperty("id", "link-view");
            if (continueform == null) {
                return null;
            }
            br.submitForm(continueform);
        }
        if (continue2Required) {
            if (br.getURL() == null) {
                br.getPage("https://" + this.getHost() + "/out2/" + linkid);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            }
            continueform = br.getFormbyProperty("id", "link-view");
            if (continueform == null) {
                return null;
            }
        } else {
            continueform = new Form();
            continueform.setMethod(MethodType.POST);
            continueform.setAction("https://" + this.getHost() + "/out2/" + linkid);
            continueform.put("url", linkid);
            br.getHeaders().put("Referer", "https://" + this.getHost() + "/out2/" + linkid);
        }
        br.submitForm(continueform);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        } else if (br.getHost().equals(Browser.getHost(finallink))) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MightyScript_AdLinkFly;
    }
}
