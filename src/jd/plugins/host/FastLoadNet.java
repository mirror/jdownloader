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

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.XPath;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JavaScript;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    private static final String HARDWARE_DEFECT = "Hardware-Defekt!";

    private static final String NOT_FOUND = "Datei existiert nicht";

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

        String filename = new XPath(page, "/html/body/div/div/div[4]/div/div/div[5]/table/tbody/tr/th[2]/span").getFirstMatch();
        String size = new XPath(page, "/html/body/div/div/div[4]/div/div/div[5]/table/tbody/tr[2]/td[2]/span").getFirstMatch();

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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String pid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4, downloadLink.getDownloadURL().indexOf("pid=") + 4 + 32);

        String serverStatusID = br.getPage("http://www.fast-load.net/api/jdownloader/" + pid);
        getFileInformation(downloadLink);

        downloadLink.setLocalSpeedLimit(-1);

        if (serverStatusID.equalsIgnoreCase("0")) {
            logger.warning("fastload Auslastung EXTREM hoch.. verringere Speed auf 20 kb/s");
            downloadLink.setLocalSpeedLimit(20 * 1024);
        }
        br.getRegex("<div id=\"traffic\">.*?Systemauslastung: (.*?) MBit").getMatch(0);
        Form captchaForm = getDownloadForm();

        captchaForm.put("downloadbutton", "Download+starten");

        dl = new RAFDownload(this, downloadLink, br.createFormRequest(captchaForm));
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.connect(br);
        if (!br.getHttpConnection().isContentDisposition()) {

            if (br.getHttpConnection().getContentLength() == 184) {
                logger.info("System overload: Retry in 20 seconds");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

            } else if (br.getHttpConnection().getContentLength() == 169) {
                logger.severe("File not found: File is deleted from Server");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getContentLength() == 529) {
                logger.severe("File not found: Unkown 404 Error");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}