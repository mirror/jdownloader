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
import java.util.Arrays;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 41244 $", interfaceVersion = 3, names = { "warez-world.org" }, urls = { "https?://(?:www\\.)?warez-world\\.org/(?:download|link)/.+" })
public class WarezWorldOrg extends PluginForDecrypt {
    public WarezWorldOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>([^<]+) Download &raquo; Warez-World.org").getMatch(0);
        String[] links = br.getRegex("<div id=\"download-links\"><a href=\"([^\"]+)\"").getColumn(0);
        String password = br.getRegex("<div class=\"ui1\">Passwort:</div>\\s*<div class=\"ui2\">\\s*([^<]+)\\s*</div>").getMatch(0);
        if (links == null || links.length == 0) {
            final Browser br2 = br.cloneBrowser();
            if (parameter.contains("/link/") && br2.containsHTML("grecaptcha")) {
                String[] fileIDs = new Regex(parameter, "/link/([^/]+)/([^/?]+)").getRow(0);
                if (fileIDs != null && fileIDs.length > 1) {
                    Form captcha = br2.getForm(0);
                    String sitekey = br.getRegex("sitekey:\\s*\"([^\"]+)\"").getMatch(0);
                    String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, sitekey) {
                        @Override
                        public TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    }.getToken();
                    captcha.put("original", "");
                    captcha.put("q", fileIDs[0]);
                    captcha.put("sq", fileIDs[1]);
                    captcha.put("tk", Encoding.urlEncode(recaptchaV2Response));
                    br2.submitForm(captcha);
                    links = new String[] { br2.getRedirectLocation() == null ? br2.getURL() : br2.getRedirectLocation() };
                }
            }
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            if (password != null && password.length() > 0 && !StringUtils.equalsIgnoreCase(password, "Kein Passwort")) {
                fp.setProperty("PWLIST", new ArrayList<String>(Arrays.asList(password)));
            }
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}