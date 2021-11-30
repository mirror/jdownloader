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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "try2link.com" }, urls = { "https?://(?:www\\.)?try2link\\.com/([A-Za-z0-9]+)" })
public class Try2linkCom extends MightyScriptAdLinkFly {
    public Try2linkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected void handlePreCrawlProcess(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
        br.setFollowRedirects(false);
        /* Pre-set Referer to skip multiple ad pages e.g. linkjust.com -> forex-articles.com -> forexmab.com */
        getPage(param.getCryptedUrl());
        String location = br.getRequest().getLocation();
        String timestampBase64 = new Regex(location, "d=(.*)").getMatch(0);
        String timestamp = Encoding.Base64Decode(timestampBase64);
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl() + "/?d=" + timestamp);
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires 2nd captcha + waittime) */
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
