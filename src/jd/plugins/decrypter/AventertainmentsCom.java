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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "aventertainments.com" }, urls = { "https?://(?:www\\.)?aventertainments\\.com/.+" }, flags = { 0 })
public class AventertainmentsCom extends PluginForDecrypt {

    public AventertainmentsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        this.br.setFollowRedirects(false);
        String fpName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);

        boolean foundScreenshot = false;
        final String screenshot_url_part = this.br.getRegex("imgs\\.aventertainments\\.com/new/bigcover/([A-Za-z0-9\\-_]+\\.jpg)\"").getMatch(0);
        final String[] screenshotRegexes = { "(https://imgs\\.aventertainments\\.com/new/screen_shot/[^<>\"\\']+\\.jpg)", "(https?://imgs\\.aventertainments\\.com//?vodimages/screenshot/large/[^<>\"\\']+\\.jpg)" };
        for (final String screenshotRegex : screenshotRegexes) {
            final String[] screenshots = br.getRegex(screenshotRegex).getColumn(0);
            if (screenshots != null && screenshots.length > 0) {
                foundScreenshot = true;
                for (final String singleLink : screenshots) {
                    decryptedLinks.add(createDownloadlink("directhttp://" + singleLink));
                }
            }
        }
        if (!foundScreenshot && screenshot_url_part != null) {
            /* E.g. for DVDs these screenshots are officially not available --> We can work around this limitation */
            decryptedLinks.add(createDownloadlink("directhttp://http://imgs.aventertainments.com/new/screen_shot/" + screenshot_url_part));
        }

        final String[] videos = br.getRegex("(https?://(?:www\\.)?aventertainments\\.com/newdlsample\\.aspx[^<>\"\\']+\\.mp4)").getColumn(0);
        try {
            if (videos != null && videos.length > 0) {
                for (final String singleLink : videos) {
                    this.br.getPage(singleLink);
                    final String directlink = this.br.getRedirectLocation();
                    if (directlink == null) {
                        continue;
                    }
                    decryptedLinks.add(createDownloadlink("directhttp://" + directlink));
                }
            }
        } catch (final Throwable e) {
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
