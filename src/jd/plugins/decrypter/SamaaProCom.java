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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "samaa-pro.com" }, urls = { "https?://(?:www\\.)?samaa-pro\\.com/([A-Za-z0-9]+)" })
public class SamaaProCom extends MightyScriptAdLinkFly {
    public SamaaProCom(PluginWrapper wrapper) {
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
        /* Pre-set Referer to skip multiple ad pages e.g. try2link.com -> forex-gold.net -> try2link.com */
        getPage(this.getContentURL(param));
        String location = br.getRequest().getLocation();
        String timestamp = new Regex(location, "done=(\\d+)").getMatch(0);
        String shortId = new Regex(location, "link=([^&]+)").getMatch(0);
        br.setFollowRedirects(true);
        getPage("https://sama-pro.com/2021/06/18/the-best-light-game-for-pc/?link=" + shortId + "&done=" + timestamp);
        getPage("https://samaa-pro.com/" + shortId + "?done=" + br.getRegex("done=(\\d+)").getMatch(0));
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
