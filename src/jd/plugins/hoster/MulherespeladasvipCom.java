//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MulherespeladasvipCom extends KernelVideoSharingComV2 {
    public MulherespeladasvipCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://mulherespeladasvip.com/");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mulherespeladasvip.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/(videos/(\\d+/)?[^/\\?#]+/|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_SPECIAL       = "https?://[^/]+/videos/videos/((\\d+)/)?([^/]+)/$";
    private final String TYPE_SPECIAL_EMBED = "https?://[^/]+/videos/videos/embed/(\\d+)";

    @Override
    protected boolean isEmbedURL(final String url) {
        if (url.matches(TYPE_SPECIAL_EMBED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }

    @Override
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        } else {
            return new Regex(url, TYPE_SPECIAL).getMatch(2);
        }
    }

    @Override
    protected String getFUIDFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            if (url.matches(TYPE_SPECIAL)) {
                final String fuid = new Regex(url, TYPE_SPECIAL).getMatch(1);
                if (fuid != null) {
                    return fuid;
                } else {
                    return new Regex(url, TYPE_SPECIAL).getMatch(2);
                }
            } else {
                return new Regex(url, TYPE_SPECIAL_EMBED).getMatch(0);
            }
        }
    }

    @Override
    protected boolean isPrivateVideoWebsite(final Browser br) {
        if (br.containsHTML("(?i)class=\"message\">\\s*Você excedeu o numero admissivel de vizualizacoes")) {
            /* 2022-09-05: Video reached max number of views and can only be downloaded via account? */
            return true;
        } else if (br.containsHTML("(?i)class=\"message\">\\s*So os usuarios registados no sitio podem ter acesso aos videos")) {
            /* 2022-09-08: Video is only available for registered users */
            return true;
        } else {
            return super.isPrivateVideoWebsite(br);
        }
    }

    @Override
    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    getPage(getProtocol() + this.getHost() + "/");
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                /* 2020-11-04: Login-URL that fits most of all websites (example): https://www.porngem.com/login-required/ */
                logger.info("Performing full login");
                getPage(getProtocol() + this.getHost() + "/videos/login/");
                final Form loginform = br.getFormbyActionRegex(".*/login.*");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("remember_me", "1");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                this.submitForm(loginform);
                if (!isLoggedIN(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }
}