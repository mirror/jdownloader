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
import java.util.Collections;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "etsy.com" }, urls = { "https?://(?:www\\.)?etsy\\.com/(?:[^/]+/)?listing/([0-9]+)/([a-z0-9\\-]+)" })
public class Etsy extends PluginForDecrypt {
    public Etsy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<h1[^>]+data-buy-box-listing-title\\s*=\\s*\"true\"[^>]*>\\s*([^<]+)\\s*").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = br.getRegex("<title>\\s*([^<]+)\\s+-\\s+Etsy\\.\\w+").getMatch(0);
        }
        if (StringUtils.isEmpty(title)) {
            String[] nameMatches = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getColumn(1);
            if (nameMatches.length > 1) {
                title = nameMatches[1];
            } else {
                title = nameMatches[0];
            }
        }
        /* Detail page images */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        /* Images */
        Collections.addAll(links, br.getRegex("data-src-zoom-image\\s*=\\s*\"([^\"]+)\"").getColumn(0));
        /* Videos */
        Collections.addAll(links, br.getRegex("<source src=\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getColumn(0));
        for (final String link : links) {
            ret.add(createDownloadlink(Encoding.htmlDecode(link)));
        }
        if (ret.isEmpty()) {
            final String dataDomeHeader = br.getRequest().getResponseHeader("X-DataDome");
            if (br.containsHTML("interstitial\\.captcha-delivery\\.com") || dataDomeHeader != null) {
                throw new DecrypterRetryException(RetryReason.BLOCKED_BY, "Anti bot captcha/page");
            } else if (br.containsHTML("<div elementtiming=\"ux-nla-message\">")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (final DownloadLink result : ret) {
            /* We know that all items are online -> Skip availablecheck */
            result.setAvailable(true);
        }
        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2023-01-11: Try to prevent anti bot page from showing up */
        return 1;
    }
}
