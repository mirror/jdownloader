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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fotoalbum.ee" }, urls = { "http://[\\w\\.]*?(pseudaholic\\.|nastazzy\\.)?fotoalbum\\.ee/photos/.+(/sets|/[0-9]+)?(/[0-9]+)?" }, flags = { 0 })
public class FotoAlbumEE extends PluginForDecrypt implements ProgressControllerListener {

    private Pattern setNamePattern = Pattern.compile("sets/[0-9]+/\">(<b>)?(.*?)(</b>)?</a>", Pattern.DOTALL);
    private Pattern setLinkPattern = Pattern.compile("<b><a href=\"/(photos/.*?/sets/[0-9]+)\">.*?</a></b> \\(([0-9]+)\\)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private Pattern nextPagePattern = Pattern.compile("<a href=\"(\\?page=[0-9]+)\" title=\"Proovi nooleklahve\">j.*?rgmised");
    private Pattern singleLinksPattern = Pattern.compile("<a href=\"(/photos/.*?/[0-9]+)\" alt=\"\" class=\"photolink");
    private Pattern pictureURLPattern = Pattern.compile("<img src=\"(http://[\\w\\.]*?fotoalbum\\.ee/fotoalbum/.*?)\" border=\"0\" alt=\"(.*?)\" vspace=\"3\"><", Pattern.CASE_INSENSITIVE);
    private boolean abort = false;

    public FotoAlbumEE(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        try {
            progress.getBroadcaster().addListener(this);
        } catch (Throwable e) {
            /* stable does not have appwork utils yet */
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> picLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        String link = parameter.toString();
        br.getPage(link);
        String nextPage = null;
        String[] sets = null;
        String[] links = null;
        String setName = null;
        FilePackage fp = null;
        if (link.matches(".*?fotoalbum\\.ee/photos/Tepsikas/?(/sets(/)?)?")) {
            sets = br.getRegex(setLinkPattern).getColumn(0);
            for (String set : sets) {
                decryptedLinks.add(createDownloadlink("http://fotoalbum.ee/" + set));
            }
            return decryptedLinks;
        }
        setName = br.getRegex(setNamePattern).getMatch(1);
        if (setName != null) {
            fp = FilePackage.getInstance();
            fp.setName(setName);
        }
        if (!link.contains("/sets/")) {
            picLinks.add(link); // add single picture link
        } else {
            do {
                if (abort) {
                }
                links = br.getRegex(singleLinksPattern).getColumn(0);
                for (String link2 : links) {
                    String picLink = "http://fotoalbum.ee/" + link2;
                    picLinks.add(picLink);
                }
                nextPage = br.getRegex(nextPagePattern).getMatch(0);
                if (nextPage == null) break;
                br.getPage(link + nextPage);
            } while (true);
        }
        String[][] picture = null;
        String pictureURL = null;
        // String filename = null; //some filenames are not correct in albums
        // TODO: maybe find a workaround later
        DownloadLink dlLink;
        progress.setRange(picLinks.size());
        for (String picLink : picLinks) {
            if (abort) {
                progress.setColor(Color.RED);
                progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                progress.doFinalize(5000l);
                return new ArrayList<DownloadLink>();
            }
            br.getPage(picLink);
            picture = br.getRegex(pictureURLPattern).getMatches();
            pictureURL = picture[0][0];
            // filename = picture[0][1];
            if (pictureURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dlLink = createDownloadlink(pictureURL);
            // if (filename != null) dlLink.setFinalFileName(filename);
            if (fp != null) dlLink.setFilePackage(fp);
            decryptedLinks.add(dlLink);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }

    }
}
