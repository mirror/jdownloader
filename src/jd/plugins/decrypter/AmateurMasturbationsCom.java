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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amateurmasturbations.com" }, urls = { "https?://(?:www\\.)?amateurmasturbations\\.com/(\\d+/[a-z0-9\\-]+/|video/[\\w\\-]+-\\d+\\.html)" })
public class AmateurMasturbationsCom extends PornEmbedParser {
    public AmateurMasturbationsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        while (true) {
            if (br.getRedirectLocation() != null && this.canHandle(br.getRedirectLocation())) {
                br.followRedirect();
            } else {
                break;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)Page Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null && !this.canHandle(redirect)) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        } else if (redirect != null && redirect.contains("/404.php")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (redirect != null) {
            br.setFollowRedirects(true);
            br.getPage(redirect);
        }
        if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }
}