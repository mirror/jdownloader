//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.awt.Color;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iload.to" }, urls = { "http://(beta\\.iload|iload)\\.to/((go/\\d+/merged|go/\\d+)(streaming/.+)?|(view|title|release)/.*?/)" }, flags = { 0 })
public class LdT extends PluginForDecrypt implements ProgressControllerListener {
    private boolean abort = false;
    private String patternSupported_Info = ".*?(beta\\.iload|iload)\\.to/(view|title|release)/.*?/";

    public LdT(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        progress.getBroadcaster().addListener(this);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        if (parameter.matches(patternSupported_Info)) {
            br.getPage(parameter);
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            String links_page[] = br.getRegex("href=\"(/go/[0-9]+)-").getColumn(0);
            if (links_page == null || links_page.length == 0) return null;
            logger.info("Found links to " + links_page.length + ". Decrypting now...");
            progress.setRange(links_page.length);
            for (String link : links_page) {
                if (abort) {
                    logger.info("Decrypt aborted by user.");
                    progress.setColor(Color.RED);
                    progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                    progress.doFinalize(5000l);
                    return new ArrayList<DownloadLink>();
                }
                String golink = "http://iload.to/" + link;
                br.getPage(golink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                DownloadLink dl_link = createDownloadlink(finallink);
                dl_link.addSourcePluginPassword("iload.to");
                decryptedLinks.add(dl_link);
                progress.increase(1);
            }
        } else {
            br.getPage(parameter);
            DownloadLink dl;
            if (br.getRedirectLocation().equalsIgnoreCase(parameter)) br.getPage(parameter);
            if (br.getRedirectLocation().equalsIgnoreCase(parameter)) return null;
            String url = br.getRedirectLocation();
            decryptedLinks.add(dl = createDownloadlink(url));
            dl.addSourcePluginPassword("iload.to");
            dl.setUrlDownload(url);
        }
        return decryptedLinks;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }
    }

}
