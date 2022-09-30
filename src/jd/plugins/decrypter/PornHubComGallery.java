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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PornHubCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.com/album/\\d+" })
public class PornHubComGallery extends PluginForDecrypt {
    public PornHubComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String preferredSubdomain = PornHubCom.getPreferredSubdomain(param.getCryptedUrl());
        param.setCryptedUrl(param.getCryptedUrl().replaceAll("https://", "http://"));
        param.setCryptedUrl(param.getCryptedUrl().replaceAll("^http://(www\\.)?([a-z]{2}\\.)?", "https://" + preferredSubdomain));
        br.setFollowRedirects(true);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        final PornHubCom hosterPlugin = (PornHubCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hosterPlugin.login(account, false);
        }
        final String url;
        final String domainFromURL = Browser.getHost(param.getCryptedUrl());
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            /* Premium account available --> Use premium domain */
            if (PornHubCom.isPremiumDomain(domainFromURL)) {
                url = param.getCryptedUrl().replaceFirst(Pattern.quote(domainFromURL), PornHubCom.getConfiguredDomainURL(this.getHost(), domainFromURL));
            } else {
                url = param.getCryptedUrl().replaceFirst(Pattern.quote(domainFromURL), PornHubCom.getConfiguredDomainURL(this.getHost(), PornHubCom.getPrimaryPremiumDomain()));
            }
        } else {
            /* No account or free account --> User free domain */
            if (PornHubCom.isPremiumDomain(domainFromURL)) {
                url = param.getCryptedUrl().replaceFirst(Pattern.quote(domainFromURL), PornHubCom.getConfiguredDomainURL(this.getHost(), PornHubCom.getPrimaryFreeDomain()));
            } else {
                url = param.getCryptedUrl().replaceFirst(Pattern.quote(domainFromURL), PornHubCom.getConfiguredDomainURL(this.getHost(), domainFromURL));
            }
        }
        final String domainFromURLNew = Browser.getHost(url);
        jd.plugins.hoster.PornHubCom.getFirstPageWithAccount(hosterPlugin, account, url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean privateImage = br.containsHTML(jd.plugins.hoster.PornHubCom.html_privateimage);
        String fpName = br.getRegex("class=\"photoAlbumTitleV2\">\\s*([^<>\"]*?)\\s*<").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("title>\\s*([^<>\"]*?)\\s*</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = domainFromURLNew + " album " + new Regex(param.getCryptedUrl(), "(\\d+)$").getMatch(0);
        }
        final Set<String> pages = new HashSet<String>();
        while (true) {
            final String[] links = br.getRegex("\"/photo/(\\d+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                if (privateImage) {
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (final String singleID : links) {
                final DownloadLink dl = createDownloadlink("https://www.pornhub.com/photo/" + singleID);
                if (privateImage) {
                    dl.setProperty("private", Boolean.TRUE);
                }
                if (br.containsHTML(singleID + "\">\\s*<div[^>]*class=\"gifStamp\"")) {
                    /* gif images --> .mp4 videos without sound */
                    dl.setName(singleID + ".mp4");
                } else {
                    dl.setName(singleID + ".jpg");
                }
                dl.setAvailable(true);
                ret.add(dl);
            }
            final String nextPage = br.getRegex("\"page_next\"\\s*>\\s*<a\\s*href\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (nextPage == null || !pages.add(nextPage)) {
                break;
            } else {
                jd.plugins.hoster.PornHubCom.getPage(br, nextPage);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }
}
