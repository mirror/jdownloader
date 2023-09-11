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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "try2link.com" }, urls = { "https?://(?:www\\.)?try2link\\.com/([A-Za-z0-9]+)" })
public class Try2LinkCom extends MightyScriptAdLinkFly {
    public Try2LinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected String getContentURL(final CryptedLink param) {
        final String contenturl = super.getContentURL(param);
        return contenturl.replaceFirst("(?i)http://", "https://");
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        // /* Pre-set Referer to skip multiple ad pages e.g. try2link.com -> forex-gold.net -> try2link.com */
        final String contentURL = this.getContentURL(param);
        getPage(contentURL);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String location = br.getRequest().getLocation();
        final UrlQuery query = UrlQuery.parse(location);
        final String urlBase64Decoded = Encoding.Base64Decode(query.get("k"));
        final String timestampBase64 = new Regex(urlBase64Decoded, "d=([^&]+)").getMatch(0);
        final String timestamp;
        if (timestampBase64.matches("[0-9]+")) {
            timestamp = timestampBase64;
        } else {
            timestamp = Encoding.Base64Decode(timestampBase64);
        }
        br.setFollowRedirects(true);
        getPage(contentURL + "/?d=" + timestamp);
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires captcha + waittime) */
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
