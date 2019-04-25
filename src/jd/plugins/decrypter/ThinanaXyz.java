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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ThinanaXyz extends PluginForDecrypt {
    public ThinanaXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String[] domains = { "thinana.xyz", "technodia.xyz", "viralcollect.info", "thanda.xyz", "mutharammss.xyz", "ourtechnoew.xyz" };

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        final String host = getHostsPattern();
        return new String[] { host + "/\\?ass=.+" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /*
         * 2019-04-25: Offline URLs will usually not happen. They will redirect back to some clicksfly.com URL which will then redirect to
         * an invalid URL --> Crawl process done
         */
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form continueform = br.getFormbyProperty("id", "fh5co-subscribe");
        if (continueform == null) {
            /* There should only be one Form anyways! */
            continueform = br.getForm(0);
        }
        if (continueform == null) {
            logger.warning("Failed to find continueform");
            return null;
        }
        br.submitForm(continueform);
        br.setFollowRedirects(false);
        /* Usually this: 'http://<host>/?ass=entrar&ref=2' */
        final String redirecturl = br.getRegex("location\\.href=\"(http[^<>\"]+)\";").getMatch(0);
        if (redirecturl == null) {
            logger.warning("Failed to find redirecturl");
            return null;
        }
        br.getPage(redirecturl);
        /* Skippable waittime here (3-6 seconds) */
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Failed to find finallink");
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MightyScript_AdLinkFly2;
    }
}
