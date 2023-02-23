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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "miklpro.com" }, urls = { "https?://(?:www\\.)?miklpro\\.com/([A-Za-z0-9]+)" })
public class MiklproCom extends MightyScriptAdLinkFly {
    public MiklproCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
        br.setFollowRedirects(false);
        getPage(param.getCryptedUrl());
        final String location = br.getRequest().getLocation();
        final UrlQuery query = UrlQuery.parse(location);
        final String base64Str = query.get("k");
        final String urlContainingTimestamp = Encoding.Base64Decode(base64Str);
        final UrlQuery query2 = UrlQuery.parse(urlContainingTimestamp);
        String timestamp = query2.get("d");
        if (!timestamp.matches("\\d+")) {
            logger.info("Decoding timestamp value: " + timestamp);
            timestamp = Encoding.Base64Decode(timestamp);
        }
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl() + "/?d=" + timestamp);
        if (br.containsHTML("(?i)>\\s*Please close VPN or proxy")) {
            /* 2022-10-05 */
            throw new DecrypterRetryException(RetryReason.GEO);
        }
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires captcha + waittime) */
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
