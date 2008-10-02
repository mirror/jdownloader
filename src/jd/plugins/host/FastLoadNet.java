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

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.XPath;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.JavaScript;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    private static final String HARDWARE_DEFECT = "Hardware-Defekt!";

    private static final String NOT_FOUND = "Datei existiert nicht";

    private static int SIM = 20;

    public FastLoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {

        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadurl);
        downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));

        if (br.containsHTML(NOT_FOUND)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        if (br.containsHTML(HARDWARE_DEFECT)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

        }

        String page = JavaScript.evalPage(br);

        String filename = new XPath(page + "", "/html/body/div/div/div[4]/div/div[2]/div/table/tbody/tr/th[2]/span").getFirstMatch();
        String size = new XPath(page + "", "/html/body/div/div/div[4]/div/div[2]/div/table/tbody/tr[2]/td[2]/span").getFirstMatch();

        // downloadinfos gefunden? -> download verfügbar
        if (filename != null && size != null) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(size));
            return true;
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final String pid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4, downloadLink.getDownloadURL().indexOf("pid=") + 4 + 32);
        // der observer prüft alle 10 min auf happy hour.
        Thread observer = new Thread("Fast-load speed observer") {
            public void run() {
                while (true) {
                    downloadLink.setLocalSpeedLimit(-1);
                    String serverStatusID;
                    SIM = 20;
                    try {
                        serverStatusID = br.getPage("http://www.fast-load.net/api/jdownloader/" + pid).trim();

                        if (serverStatusID.equalsIgnoreCase("0")) {
                            logger.warning("fastload Auslastung EXTREM hoch.. verringere Speed auf 20 kb/s");
                            downloadLink.setLocalSpeedLimit(20 * 1024);
                            JDUtilities.getGUI().displayMiniWarning(JDLocale.L("plugins.host.fastload.overload.short", "Fast-load.net Overload"), JDLocale.L("plugins.host.fastload.overload.long", "Fast-load.net Overload!. Fullspeed download only in browsers"), 10 * 60 * 1000);
                            SIM = 1;
                        } else {
                            downloadLink.setLocalSpeedLimit(-1);
                        }
                    } catch (IOException e1) {
                        downloadLink.setLocalSpeedLimit(-1);
                    }

                    try {
                        Thread.sleep(10 * 60 * 1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        observer.start();
        try {
            handleFree0(downloadLink);
        } catch (Exception e) {
            observer.interrupt();
            throw e;
        }
        observer.interrupt();
    }

    public void handleFree0(final DownloadLink downloadLink) throws Exception {
        // LinkStatus linkStatus = downloadLink.getLinkStatus();
        // br.setDebug(true);

        getFileInformation(downloadLink);

        Form captchaForm = getDownloadForm();

        captchaForm.setVariable(1, br.getRegex("clearInterval\\(oCountDown\\).*?document\\.forms\\[0\\]\\.elements\\['.*?'\\]\\.value = '(.*?)';").getMatch(0));
        captchaForm.setVariable(1, "start+download");

        dl = br.openDownload(downloadLink, captchaForm);
        // dl.connect(br);
        if (!br.getHttpConnection().isContentDisposition()) {

            if (br.getHttpConnection().getContentLength() == 184) {
                logger.info("System overload: Retry in 20 seconds");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

            } else if (br.getHttpConnection().getContentLength() == 169) {
                logger.severe("File not found: File is deleted from Server");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getContentLength() == 529) {
                logger.severe("File not found: Unkown 404 Error");
            } else if (!br.getHttpConnection().isOK()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
            } else {
                logger.severe("Unknown error page");
                throw new PluginException(LinkStatus.ERROR_FATAL);
            }
        }
        dl.startDownload();

    }

    private Form getDownloadForm() {
        /* richtige form suchen, da fakeforms verwendet werden */
        Form[] forms = br.getForms();
        if (forms != null) {
            for (int i = 0; i < forms.length; i++) {
                if (forms[i].getVars().size() >= 2) { return forms[i]; }
            }
        }
        return null;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return SIM == 1 ? 1 : JDUtilities.getController().getRunningDownloadNumByHost(this) + 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}