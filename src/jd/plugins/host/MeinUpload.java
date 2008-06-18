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
import java.util.regex.Pattern;

import jd.http.PostRequest;
import jd.parser.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://www.xup.in/dl,43227676/YourFilesBiz.java/

public class MeinUpload extends PluginForHost {
    // 
    private static final String CODER = "jD-Team";
    private static final String HOST = "meinupload.com";
    private static final String PLUGIN_VERSION = "0.1.0";
    private static final String AGB_LINK = "http://meinupload.com/#help.html";

    static private final Pattern PATTERN_SUPPORTED = getSupportPattern("http://[*]meinupload.com/dl/[+]/[+]");
    private static final int MAX_SIMULTAN_DOWNLOADS = 30;

    public MeinUpload() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + PLUGIN_VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTED;
    }

    @Override
    public void reset() {

    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        try {

            PostRequest r = new PostRequest(downloadLink.getDownloadURL());

            r.setPostVariable("submit", "Kostenlos");
            r.setPostVariable("sent", "1");
            String page = r.load();
            // http://meinupload.com/dl/3407292519/Bios.part03.rar
            // if(!Regex.matches(page, PATTERN_FIND))

            Form[] forms = Form.getForms(r.getRequestInfo());

            if (forms.length == 1 && forms[0].vars.containsKey("download")) return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // unbekannter fehler
        return false;

    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {
            PostRequest r = new PostRequest(downloadLink.getDownloadURL());
            r.setPostVariable("submit", "Kostenlos");
            r.setPostVariable("sent", "1");
            r.load();
            Form[] forms = Form.getForms(r.getRequestInfo());
            if (forms.length != 1 || !forms[0].vars.containsKey("download")) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return step;
            }        
            r = (PostRequest)new PostRequest(forms[0]).connect();         
            
            if(r.getResponseHeader("Content-Disposition")==null){
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                return step;
            }
            int length = r.getHttpConnection().getContentLength();
            downloadLink.setDownloadMax(length);        
            dl = new RAFDownload(this, downloadLink, r.getHttpConnection());      
            dl.startDownload();
            return step;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void sleep(int i, DownloadLink downloadLink) throws InterruptedException {
        while (i > 0) {

            i -= 1000;
            downloadLink.setStatusText(String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);

        }

    }

}