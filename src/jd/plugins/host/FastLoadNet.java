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
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

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

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadurl);

        if (br.containsHTML(NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }

        if (br.containsHTML(HARDWARE_DEFECT)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }
        String txt = (br + "").replaceAll("<.*?>", "|");
        txt = txt.replaceAll("\r", "|");
        txt = txt.replaceAll("\n", "|");
        txt = txt.replaceAll("\\|\\s+\\|", "|");
        txt = txt.replaceAll("[|]+", "|");
        String fileName = Encoding.htmlDecode(new Regex(txt, "Datei\\|(.+?)\\|").getMatch(0));
        String fileSize = Encoding.htmlDecode(new Regex(txt, "Gr&ouml;sse\\|(.+?)\\|").getMatch(0));
        // downloadinfos gefunden? -> download verfügbar
        if (fileName != null && fileSize != null) {
            downloadLink.setName(fileName.trim());
            downloadLink.setDownloadSize(Regex.getSize(fileSize));
            return true;
        }
        downloadLink.setName(downloadurl.substring(downloadurl.indexOf("pid=") + 4));
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies(getHost());

        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FATAL, getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));

        }

        Form captcha_form = getDownloadForm();

        captcha_form.put("downloadbutton", "Download+starten");
        br.openFormConnection(captcha_form);

        if (!br.getHttpConnection().isContentDisposition()) {

            if (br.getHttpConnection().getContentLength() == 184) {
                logger.info("System overload: Retry in 20 seconds");
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(20 * 60 * 1000l);
                return;
            } else if (br.getHttpConnection().getContentLength() == 169) {
                logger.severe("File not found: File is deleted from Server");
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            } else if (br.getHttpConnection().getContentLength() == 529) {
                logger.severe("File not found: Unkown 404 Error");
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            } else {
                logger.severe("Unknown error page");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                return;
            }
        }

        RAFDownload.download(downloadLink, br.getHttpConnection(), false, 1);

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