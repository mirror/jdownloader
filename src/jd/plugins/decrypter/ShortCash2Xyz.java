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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "short-cash2.xyz" }, urls = { "https?://(?:www\\.)?short-cash2\\.xyz/([A-Za-z0-9]+)" })
public class ShortCash2Xyz extends MightyScriptAdLinkFly {
    public ShortCash2Xyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected String getContentURL(final CryptedLink param) {
        String contenturl = super.getContentURL(param);
        return contenturl.replaceFirst("(?i)http://", "https://").replace("short2.cash", "short-cash2.xyz");
    }

    @Override
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(this.getContentURL(param));
        if (this.regexAppVars(this.br) == null) {
            logger.warning("Possible crawler failure...");
        }
        /* Now continue with parent class code (requires 2nd captcha + waittime) */
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return super.decryptIt(param, progress);
    }
}
