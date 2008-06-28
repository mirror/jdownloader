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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.GetRequest;
import jd.http.HeadRequest;
import jd.http.PostRequest;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
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
//    private static final int MAX_SIMULTAN_DOWNLOADS = 1;

    public MeinUpload() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        setConfigElements();
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
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 2;
        }
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
            String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("meinupload.com/dl/([\\d]*?)/", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if(id==null)return false;
           // http://meinupload.com/infos.api?get_id=3794082988
        
            String page= new GetRequest("http://meinupload.com/infos.api?get_id="+id).load();
            
           String status=new Regex(page,"<status>([\\d]*?)</status>").getFirstMatch();
           String filesize=new Regex(page,"<filesize>([\\d]*?)</filesize>").getFirstMatch();
           String name=new Regex(page,"<name>(.*?)</name>").getFirstMatch();
           if(status==null||!status.equals("1"))return false;
           
            if(filesize==null||name==null)return false;
            
            downloadLink.setDownloadMax(Integer.parseInt(filesize));
            downloadLink.setName(name);
            return true;
         
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // unbekannter fehler
        return false;

    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {

            return doPremiumStep(step, downloadLink);
        } else {
            return doFreeStep(step, downloadLink);
        }

    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {
        String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);
        downloadLink.setStatusText(JDLocale.L("downloadstatus.premiumload", "Premiumdownload"));
        downloadLink.requestGuiUpdate();
        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("meinupload.com/dl/([\\d]*?)/", Pattern.CASE_INSENSITIVE)).getFirstMatch();
        if (id == null) {
            step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            return step;
        }
        try {
            GetRequest r = new GetRequest("http://MeinUpload.com/server.api?id=" + id);
            r.getHeaders().put("Accept", "text/html, */*");
            r.getHeaders().put("Accept-Encoding", "identity");
            r.getHeaders().put("Referer", "http://MeinUpload.com/");
            r.getHeaders().put("User-Agent", " MeinUpload Tool - v2.2");

            String server = r.load();
            if (server == null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return step;
            }
            server = server.trim();
            HeadRequest hr = new HeadRequest(downloadLink.getDownloadURL());
            hr.getHeaders().put("Accept", "text/html, */*");
            hr.getHeaders().put("Accept-Encoding", "identity");
            hr.getHeaders().put("Referer", "http://MeinUpload.com/");
            hr.getHeaders().put("User-Agent", " MeinUpload Tool - v2.2");
            hr.load();
            r = new GetRequest("http://" + server + ".MeinUpload.com/download.api?user=" + user + "&pass=" + JDUtilities.getMD5(pass) + "&id=" + id);
            r.getHeaders().put("Accept", "text/html, */*");
            r.getHeaders().put("Accept-Encoding", "identity");
            r.getHeaders().put("Referer", "http://MeinUpload.com/");
            r.getHeaders().put("User-Agent", " MeinUpload Tool - v2.2");
            r.connect();
            // http://dl2.MeinUpload.com/download.api?user=23729405&pass=0865a2801d938ce3e59024b4ef1d6d30&id=3407292519
            // GET
            // /download.api?user=23729405&pass=0865a2801d938ce3e59024b4ef1d6d30&id=9923945611
            // HTTP/1.1
            // v
            if (r.getResponseHeader("Content-Disposition") == null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                return step;
            }
            int length = r.getHttpConnection().getContentLength();
            downloadLink.setDownloadMax(length);
            dl = new RAFDownload(this, downloadLink, r.getHttpConnection());
            dl.setChunkNum(1);
            dl.setResume(false);
            dl.startDownload();
            dl.getFile();
            if (dl.getFile().length() < 6000) {
                String page = JDUtilities.getLocalFile(dl.getFile());
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                step.setParameter(JDLocale.L("errors.interbalhostererror", "Internal Hoster Error"));
                logger.severe(page);
                return step;
            }
            return step;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            return step;
        }

    }

    public PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) {
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
            sleep(15000, downloadLink);
            r = (PostRequest) new PostRequest(forms[0]).connect();

            if (r.getResponseHeader("Content-Disposition") == null) {
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

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.host.premium.account", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.host.premium.user", "Benutzer")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.host.premium.password", "Passwort")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.host.premium.useAccount", "Premium Account verwenden")));
        cfg.setDefaultValue(false);

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