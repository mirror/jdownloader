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

import java.io.File;
import java.util.regex.Pattern;

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class MediafireCom extends PluginForHost {

    private static final String HOST = "mediafire.com";

    static private final String offlinelink = "tos_aup_violation";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+)", Pattern.CASE_INSENSITIVE);

    private String url;

    public MediafireCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
 
        try {
            br.setCookiesExclusive(true);br.clearCookies(HOST);
            String url = downloadLink.getDownloadURL();
            br.getPage(url);

            if (br.getRegex(offlinelink).matches()) { return false; }

            downloadLink.setName(br.getRegex("<title>(.*?)<\\/title>").getMatch(0).trim());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:
        url = downloadLink.getDownloadURL();
        br.setCookiesExclusive(true);br.clearCookies(HOST);
        br.getPage(url);
        if (br.getRegex(offlinelink).matches()) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_DOWNLOAD:
        String[][] para = br.getRegex("[a-z]{2}\\(\\'([a-z0-9]{7,14})\\'\\,\\'([0-f0-9]*?)\\'\\,\\'([a-z0-9]{2,14})\\'\\)\\;").getMatches();
        if (para.length == 0 || para[0].length == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        br.getPage("http://www.mediafire.com/dynamic/download.php?qk=" + para[0][0] + "&pk=" + para[0][1] + "&r=" + para[0][2]);
        String url = br.getRegex("http\\:\\/\\/\"(.*?)'\"").getMatch(0);
        url = "http://" + url.replaceAll("'(.*?)'", "$1");

        String[] vars = new Regex(url, "\\+ ?([a-z0-9]*?) ?\\+").getColumn(0);

        for (String var : vars) {
            String value = br.getRegex(var + " ?= ?\\'(.*?)\\'").getMatch(0);
            url = url.replaceAll("\\+ ?" + var + " ?\\+", value);

        }
        HTTPConnection urlConnection = br.openGetConnection(url);
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();
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