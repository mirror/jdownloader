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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "try2link.com" }, urls = { "https?://(?:www\\.)?try2link\\.com/([A-Za-z0-9]+)" })
public class Try2LinkCom extends MightyScriptAdLinkFly {
    public Try2LinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected void handlePreCrawlProcess(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        /* Prefer https */
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("http://", "https://"));
        br.setFollowRedirects(true);
        /* 2021-11-30: Special parameter required to skip two pre-ad pages (forex-gold.net and newforex.online) */
        getPage(param.getCryptedUrl() + "/?d=" + (System.currentTimeMillis() / 1000 + 100));
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires 2nd captcha + waittime) */
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
