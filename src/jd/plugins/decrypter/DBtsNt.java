//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiobeats.net" }, urls = { "http://[\\w\\.]*?audiobeats\\.net/(liveset|link|event|artist)\\?id=\\d+" }, flags = { 0 })
public class DBtsNt extends PluginForDecrypt implements ProgressControllerListener {

    private String                  fpName;
    private ArrayList<DownloadLink> decryptedLinks;

    public DBtsNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean abort = false;

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>();
        try {
            progress.getBroadcaster().addListener(this);
        } catch (Throwable e) {
            /* stable does not have appwork utils yet */
        }
        // TODO: beim hinzuf�gen von Events oder Artisten etc. sollte der
        // Linkgrabber gleiche links auch als gleiche Links erkennen

        Regex urlInfo = new Regex(parameter, "http://[\\w\\.]*?audiobeats\\.net/(liveset|link|event|artist)\\?id=(\\d+)");
        String type = urlInfo.getMatch(0);
        String id = urlInfo.getMatch(1);

        if (type.equals("link")) {

            progress.increase(1);
            String link = "/link?id=" + id;
            if (!decryptSingleLink(parameter.toString(), progress, decryptedLinks, link)) return null;
        } else if (type.equals("liveset")) {
            br.getPage(parameter.toString());
            fpName = br.getRegex("rel=\"alternate\" type=\"application/rss\\+xml\" title=\"(.*?)\"").getMatch(0);
            String scloudID = br.getRegex("return ShowSoundcloud\\((\\d+)\\);\"").getMatch(0);
            if (scloudID != null) {
                Browser br2 = br.cloneBrowser();
                br2.getPage("http://www.audiobeats.net/soundcloud?format=raw&id=" + scloudID);
                String scloudLink = br2.getRegex("\"http://player\\.soundcloud\\.com/player\\.swf\\?url=(http://soundcloud\\.com/.*?)\"").getMatch(0);
                if (scloudLink != null) {
                    logger.info("Grabbing scloudlink succeeded");
                    decryptedLinks.add(createDownloadlink(scloudLink));
                }
            }
            if (!decryptLiveset(parameter.toString(), progress, decryptedLinks)) return null;
        } else if (type.equals("event") || type.equals("artist")) {

            br.getPage(parameter.toString());
            String[] allLivesets = br.getRegex("\"(/liveset\\?id=\\d+)\"").getColumn(0);

            if (allLivesets == null || allLivesets.length == 0) return null;
            if (fpName == null) fpName = br.getRegex("<title>AudioBeats\\.net - (.*?)</title>").getMatch(0);

            progress.setRange(allLivesets.length);
            for (String aLiveset : allLivesets) {
                if (!decryptLiveset(aLiveset, progress, decryptedLinks)) return null;
                Thread.sleep(1000);
            }
        }

        return decryptedLinks;
    }

    private boolean decryptLiveset(String parameter, ProgressController progress, ArrayList<DownloadLink> decryptedLinks) throws IOException, DecrypterException {
        int i = 0;
        do {
            br.getPage(parameter);

            if (br.containsHTML("Error 403")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            } else {

                String[] allLinks = br.getRegex("\"(/link\\?id=\\d+-.*?)\"").getColumn(0);

                if (allLinks == null || allLinks.length == 0) {
                    if (decryptedLinks.size() > 0) {
                        return true;
                    } else {
                        if (br.containsHTML("Sorry this links is reported as dead\\.")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                        return false;
                    }
                }
                progress.setRange(progress.getMax() + allLinks.length);
                for (String aLink : allLinks) {
                    if (abort) {
                        progress.setColor(Color.RED);
                        progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                        logger.info("Decrypter is NOT defect, aborted by user!");
                        progress.doFinalize(5000l);
                        return false;
                    }
                    if (!decryptSingleLink(parameter, progress, decryptedLinks, aLink)) return false;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }

                String fpName = br.getRegex("<title>AudioBeats\\.net - (.*?)</title>").getMatch(0);
                if (fpName == null) fpName = br.getRegex("rel=\"alternate\" type=\".*?\" title=\"(.*?)\"").getMatch(0);

                setPackageName();
                break;
            }

        } while (i < 5);

        return true;
    }

    private void setPackageName() {
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
    }

    private boolean decryptSingleLink(String parameter, ProgressController progress, ArrayList<DownloadLink> decryptedLinks, String aLink) throws IOException {
        br.setFollowRedirects(false);
        aLink = "http://www.audiobeats.net" + aLink.replace("link?id", "link.php?id");
        br.getPage(aLink);
        String finallink = null;
        String cryptedLink = br.getRegex("\"(http://lsdb\\.eu/download/\\d+\\.html)\"").getMatch(0);
        if (cryptedLink == null) cryptedLink = br.getRegex("\"(http://djurl\\.com/[A-Za-z0-9]+)\"").getMatch(0);
        if (cryptedLink != null) {
            br.getPage(cryptedLink);
            if (cryptedLink.contains("djurl.com")) {
                finallink = br.getRegex("var finalStr = \"(.*?)\";").getMatch(0);
                if (finallink != null)
                    finallink = Encoding.Base64Decode(finallink);
                else
                    finallink = br.getRegex("var finalLink = \"(.*?)\";").getMatch(0);
            }
        }
        if (abort) {
            progress.setColor(Color.RED);
            progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
            logger.info("Decrypter is NOT defect, aborted by user!");
            progress.doFinalize(5000l);
            return false;
        }
        if (finallink == null) {
            finallink = br.getRegex("link_header\\.php\" noresize=\"noresize\">[\t\n\r ]+<frame src=\"(http.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRedirectLocation();
        }
        if (!br.containsHTML("3voor12\\.vpro\\.nl")) {
            if (finallink == null) {
                logger.warning("Decrypter must be defect, detailedLink = " + aLink + " Mainlink = " + parameter);
                return false;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        progress.increase(1);
        return true;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }

    }

}
