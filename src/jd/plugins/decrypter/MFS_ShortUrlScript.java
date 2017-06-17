//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 * This class supports, http://mfscripts.com/short_url_script.html template.
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mfs_shorturlscript", "gourl.us", "shori.xyz" }, urls = { "https?://(?:www\\.)?nullified\\.jdownloader\\.org/([a-zA-Z0-9]+)", "https?://(?:www\\.)?gourl\\.us/([a-zA-Z0-9_\\-]+)$", "https?://(?:www\\.)?shori\\.xyz/([a-zA-Z0-9_\\-]+)$" })
public class MFS_ShortUrlScript extends antiDDoSForDecrypt {

    public MFS_ShortUrlScript(PluginWrapper wrapper) {
        super(wrapper);
    }

    // default setters, don't change.
    private boolean supportsHTTPS = true;
    private boolean hasCaptcha    = false;

    /**
     * use to define hasCaptcha, and supportsHTTPS, only required when site doesn't match default setters
     *
     * @param link
     * @throws PluginException
     */
    private void setConstants(final CryptedLink link) throws PluginException {
        final String host = Browser.getHost(link.toString());
        if (host == null || "".equalsIgnoreCase(host)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if ("shori.xyz".equals(host)) {
            supportsHTTPS = false;
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        setConstants(param);
        final String parameter = param.toString().replaceFirst("^https?://", (supportsHTTPS ? "https://" : "http://"));
        final String fuid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // redirects happen
        getPage(parameter);
        // password files
        final String redirect = br.getRedirectLocation();
        if (!inValidate(redirect)) {
            if (StringUtils.containsIgnoreCase(redirect, Browser.getHost(redirect) + "/error.html?")) {
                return decryptedLinks;
            }
            getPage(redirect);
            final int repeat = 3;
            Form password = br.getFormByInputFieldKeyValue("accessPass", "");
            for (int i = 0; i <= repeat; i++) {
                if (password == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find password form");
                }
                final String pass = getUserInput("Password protected link", param);
                if (inValidate(pass)) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                password.put("accessPass", Encoding.urlEncode(pass));
                submitForm(password);
                password = br.getFormByInputFieldKeyValue("accessPass", "");
                if (password != null) {
                    if (i + 1 >= repeat) {
                        logger.warning("Incorrect solve of password");
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    continue;
                } else {
                    break;
                }
            }
        }
        String frame = br.getRegex("<frame [^>]*src=\"(interstitualAdTop\\.php\\?url=\\d+)\"").getMatch(0);
        if (frame == null) {
            logger.warning("Possible Plugin Defect, confirm in browser: " + parameter);
            return decryptedLinks;
        }
        getPage(frame);
        String link = br.getRegex("<a [^>]*[^\\S]*href=\"(.*?)\" class=[^>]+>skip advert\\s*</a>").getMatch(0);
        if (link == null) {
            link = br.getRegex("<a [^>]*href=\"(sk_redirect_ads\\.html\\?url=" + fuid + ")\"").getMatch(0);
            if (link != null) {
                final Browser br2 = br.cloneBrowser();
                getPage(br2, link);
                link = br2.getRedirectLocation();
                if (link == null) {
                    logger.warning("Possible Plugin Defect, confirm in browser: " + parameter);
                    return null;
                }
            }
        }
        decryptedLinks.add(createDownloadlink(HTMLEntities.unhtmlentities(link)));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) throws PluginException {
        setConstants(link);
        return hasCaptcha;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_Wurlie;
    }

    @Override
    public Boolean siteTesterDisabled() {
        if ("mfs_shorturlscript".equalsIgnoreCase(this.getHost())) {
            return Boolean.TRUE;
        }
        return super.siteTesterDisabled();
    }

}