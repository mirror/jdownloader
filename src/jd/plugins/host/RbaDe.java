//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class RbaDe extends PluginForHost {

    private static final String CODER = "ToKaM";

    private static final String AGB_LINK = "http://www.r-b-a.de/index.php?ID=3003";

    private static final String REGEX_NICKNAME = "<td>(.+?)</td><td>\\d+</td></tr>";

    private static final Pattern REGEX_RAPPERNAMEN = Pattern.compile(REGEX_NICKNAME, Pattern.CASE_INSENSITIVE);

    private static final String ERR_IDS_NOT_FOUND = "Konnte BattleId und Rundennummer nicht herausfinden. Plugin defeckt!";
    private static final String ERR_MC_NAME_NOT_FOUND = "Konnte Herausforderer oder Gegner nicht ermitteln. Plugin defeckt!";
    private static final String ERR_FILE_NOT_FOUND = "Datei Konnte nicht gefunden werden. Entweder der Link ist ungültig oder das Plugin defeckt!";
    private static final String REGEX_ALLOWED_FILENAME_CHARS = "[^\\w$_ ~]";
    /** Downloads will be downloaded to DOWNLOAD_DIR + PackageName */
    private static final String DOWNLOAD_DIR = "RBA-BATTELS";

    public RbaDe() {
        super();
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    private String getFileName(String herausforderer, String gegner, int rundenId, int battleId) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(rundenId);
        fileName.append(" - ");
        fileName.append(herausforderer);
        fileName.append("-vs-");
        fileName.append(gegner);
        switch (rundenId % 3) {
        case 0:
            fileName.append("-RR");
            fileName.append(rundenId / 3);
            break;
        case 1:
            fileName.append("-Beat");
            fileName.append(rundenId / 3 + 1);
            break;
        case 2:
            fileName.append("-HR");
            fileName.append(rundenId / 3 + 1);
            break;
        }
        fileName.append("-(");
        fileName.append(battleId);
        fileName.append(").mp3");
        return fileName.toString();
    }

    @Override
    public boolean getFileInformation(DownloadLink link) {
        Browser br = new Browser();
        br.clearCookies(getHost());
        LinkStatus linkstatus = link.getLinkStatus();
        try {
            int battleId = Integer.parseInt(new Regex(link.getDownloadURL(), getSupportedLinks()).getMatch(0));
            int rundenId = Integer.parseInt(new Regex(link.getDownloadURL(), getSupportedLinks()).getMatch(1));
            String page = br.getPage("http://www.r-b-a.de/index.php?ID=4101&BATTLE=" + battleId);
            // String herausforderer = new Regex(page,
            // REGEX_HERAUSFORDERER).getFirstMatch();
            // String gegner = new Regex(page,REGEX_GEGNER).getFirstMatch();
            String[][] rapperNamen = new Regex(page, REGEX_RAPPERNAMEN).getMatches();
            String herausforderer = null;
            String gegner = null;
            try {
                herausforderer = rapperNamen[0][0].replaceAll(REGEX_ALLOWED_FILENAME_CHARS, "");
                gegner = rapperNamen[1][0].replaceAll(REGEX_ALLOWED_FILENAME_CHARS, "");
            } catch (ArrayIndexOutOfBoundsException e) {
                if (herausforderer != null) {
                    gegner = JDUtilities.getGUI().showUserInputDialog("Der Name des Battlegegner von " + herausforderer + " konnt nicht ermittelt werden. Entweder dieser hat noch keine Runde hochgeladen, oder das Plugin ist defeckt.");
                } else {
                    herausforderer = JDUtilities.getGUI().showUserInputDialog("Plugin defeckt, bitte geben sie den Namen der Herausforderers ein.");
                    gegner = JDUtilities.getGUI().showUserInputDialog("Plugin defeckt, bitte geben sie den Namen der Battlegegners ein.");
                }
            }
            if (herausforderer == null || gegner == null) {
                linkstatus.setStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkstatus.setErrorMessage(ERR_MC_NAME_NOT_FOUND);
                return false;
            }
            String fileName = getFileName(herausforderer, gegner, rundenId, battleId);
            link.setName(fileName);
            link.setStaticFileName(fileName);
            FilePackage filePackage = new FilePackage();
            String packageName = new StringBuilder().append(herausforderer).append("-vs-").append(gegner).append(" (").append(battleId).append(")").toString();
            link.addSubdirectory(DOWNLOAD_DIR);
            link.addSubdirectory(packageName);
            filePackage.setName(packageName);
            link.setFilePackage(filePackage);
            String sizeTxt = br.openGetConnection(link.getDownloadURL()).getHeaderField("Content-Length");
            if (sizeTxt != null) {
                try {
                    link.setDownloadSize(Integer.parseInt(sizeTxt));
                } catch (NumberFormatException e) {
                    logger.severe(e.toString());
                }
            }
            return true;
        } catch (IOException e) {
            linkstatus.setStatus(LinkStatus.ERROR_NO_CONNECTION);
            logger.severe(e.toString());
            return false;
        } catch (NumberFormatException e) {
            linkstatus.setStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            linkstatus.setErrorMessage(ERR_IDS_NOT_FOUND);
            logger.severe(e.toString());
            return false;
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        Browser br = new Browser();
        br.clearCookies(getHost());
        /*
         * String header;
         * logger.finer("Überprüfe Headerfled - \"Conten-Type\"! URL: --> "+
         * link.getDownloadURL()); if
         * ((header=br.openGetConnection(link.getDownloadURL
         * ()).getHeaderField("Content-Type"
         * ))!=null&&header.equals("application/octetstream")) { new
         * RAFDownload(
         * this,link,br.openGetConnection(link.getDownloadURL())).startDownload
         * (); }else{
         * link.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
         * link.getLinkStatus().setErrorMessage(ERR_FILE_NOT_FOUND); }
         */
        String path = new Regex(link.getDownloadURL(), getSupportedLinks()).getColumn(2)[0];
        if (path.equals("5")) {
            new RAFDownload(this, link, br.openGetConnection(link.getDownloadURL())).startDownload();
        } else {
            logger.finer("Path = " + path + "nicht supported? Möglicherweise zu unrecht?!?");
            link.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            link.getLinkStatus().setErrorMessage(ERR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void reset() {
    }
}
