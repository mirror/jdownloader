//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pastebin.com" }, urls = { "https?://(www\\.)?pastebin\\.com/(?:download\\.php\\?i=|raw.*?=|raw/|dl/)?[0-9A-Za-z]{2,}" })
public class PasteBinCom extends PluginForDecrypt {
    public PasteBinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: pastebin
    private final String type_invalid = "https?://[^/]+/(messages|report|dl|scraping|languages|trends|signup|login|pro|profile|tools|archive|login\\.php|faq|search|settings|alerts|domains|contact|stats|etc|favicon|users|api|download|privacy|passmailer)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(type_invalid)) {
            return ret;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling for invalid links */
        if (br.containsHTML("(Unknown paste ID|Unknown paste ID, it may have expired or been deleted)") || !this.canHandle(this.br.getURL()) || br.getHttpConnection().getResponseCode() == 404) {
            ret.add(this.createOfflinelink(parameter));
            return ret;
        }
        Form pwprotected = getPwProtectedForm();
        if (pwprotected != null) {
            final String passCode = this.getUserInput("Enter password", param);
            pwprotected.put("PostPasswordVerificationForm[password]", Encoding.urlEncode(passCode));
            this.br.submitForm(pwprotected);
            if (getPwProtectedForm() != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String plaintxt = br.getRegex("<textarea(.*?)</textarea>").getMatch(0);
        if (plaintxt == null) {
            plaintxt = br.getRegex("<div class\\s*=\\s*\"source.*?\"[^>]*>\\s*<ol[^>]*>\\s*(.*?)\\s*</ol>\\s*</div>").getMatch(0);
            if (plaintxt != null) {
                plaintxt = plaintxt.replaceAll("<li[^>]*>", "");
                plaintxt = plaintxt.replaceAll("<div[^>]*>", "");
                plaintxt = plaintxt.replaceAll("</li>", "");
                plaintxt = plaintxt.replaceAll("</div>", "");
            }
        }
        if (plaintxt == null && (parameter.contains("raw.php") || parameter.contains("/raw/"))) {
            plaintxt = br.toString();
        }
        if (plaintxt == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Find all those links
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no URLs in link: " + parameter);
            ret.add(this.createOfflinelink(parameter));
            return ret;
        }
        logger.info("Found " + links.length + " links in total.");
        for (String dl : links) {
            if (!dl.contains(parameter) && !new Regex(dl, "https?://(www\\.)?pastebin\\.com/(raw.*?=)?[0-9A-Za-z]+").matches()) {
                final DownloadLink link = createDownloadlink(dl);
                ret.add(link);
            }
        }
        return ret;
    }

    private Form getPwProtectedForm() {
        for (final Form form : this.br.getForms()) {
            if (form.containsHTML("postpasswordverificationform-password")) {
                return form;
            }
        }
        return null;
    }
}