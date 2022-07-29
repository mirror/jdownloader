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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "badjojo.com" }, urls = { "https?://(?:www\\.)?badjojo\\.com/(\\d+)/[a-z0-9\\-]+/?" })
public class BadJoJoComDecrypter extends PornEmbedParser {
    public BadJoJoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        decryptedLinks.addAll(findEmbedUrls());
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        if (br.containsHTML("(?i)<h4>Source</h4>")) {
            /* 2017-03-21: Handle special case */
            // <a href="/out.php?siteid=89&amp;id=14064863&amp;url=http%3A%2F%2Fnudez.com%2Fvideo%2F...-221490.html"
            String externID = br.getRegex("(?i)<h4>Source</h4>\\s*<a href=\"[^\"]+?url=([^\"]+)\"").getMatch(0);
            if (externID != null) {
                externID = Encoding.urlDecode(externID, true);
                logger.info("externID: " + externID);
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
        }
        /* Looks like selfhosted content */
        decryptedLinks.add(createDownloadlink(param.getCryptedUrl()));
        return decryptedLinks;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!this.canHandle(br.getURL())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return br.getRegex("<title>(.*?)</title>").getMatch(0);
    }
}