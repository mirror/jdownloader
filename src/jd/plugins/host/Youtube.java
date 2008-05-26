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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Youtube extends PluginForHost {
    static private final Pattern PAT_SUPPORTED = Pattern.compile("\\< youtubedl url=\".*\" decrypted=\".*\" convert=\".*\" \\>", Pattern.CASE_INSENSITIVE);

    static private final String HOST = "youtube.com";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "JD-Team";

    static private final Pattern FILENAME = Pattern.compile("<div id=\"watch-vid-title\">[\\s\\S]*?<div >(.*?)</div>", Pattern.CASE_INSENSITIVE);

    static private final Pattern YouTubeURL = Pattern.compile("< youtubedl url=\"(.*?)\" decrypted=\".*?\" convert=\"[0-9]+?\" >", Pattern.CASE_INSENSITIVE);
    static private final Pattern DOWNLOADFILE = Pattern.compile("< youtubedl url=\".*?\" decrypted=\"(.*?)\" convert=\"[0-9]+?\" >", Pattern.CASE_INSENSITIVE);

    
    public Youtube() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        steps.add(new PluginStep(PluginStep.STEP_CONVERT, null));
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }


    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            if (step.getStep() == PluginStep.STEP_DOWNLOAD) {

                requestInfo = getRequest(new URL(getFirstMatch(downloadLink.getDownloadURL(),YouTubeURL, 1)));
                if (requestInfo.getHtmlCode() == null||requestInfo.getHtmlCode().trim().length()==0) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    step.setParameter(JDLocale.L("plugins.host.youtube.unavailable", "YouTube Serverfehler"));
                    return step;
                }
                String name = getFirstMatch(requestInfo.getHtmlCode(), FILENAME, 1).trim();
                String downloadfile = getFirstMatch(downloadLink.getDownloadURL(),DOWNLOADFILE, 1).trim();
                
                if ((name == null)||(downloadfile == null)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    step.setParameter(JDLocale.L("plugins.host.youtube.unavailable", "YouTube Serverfehler"));
                    return step;

                }

                downloadLink.setName(name + ".flv" );

                logger.info(downloadfile);

                HTTPConnection urlConnection;
                requestInfo = getRequestWithoutHtmlCode(new URL(downloadfile), null, downloadfile, true);
                urlConnection = requestInfo.getConnection();

                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setChunkNum(1);
                dl.setRequestTimeout(100000);
                dl.setReadTimeout(1000000);
                dl.setResume(false);
                dl.startDownload();
             

               // step.setStatus(PluginStep.STATUS_DONE);
               // downloadLink.setStatus(DownloadLink.STATUS_DONE);
                return step;

            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        // this.url = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            requestInfo = getRequest(new URL(getFirstMatch(downloadLink.getDownloadURL(),YouTubeURL, 1)));
            String name = getFirstMatch(requestInfo.getHtmlCode(), FILENAME, 1);
            downloadLink.setName(name);
            
            if(name==null)return false;

            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 50;
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }
}
