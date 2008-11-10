package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class EasyShareCom extends PluginForHost {

    public EasyShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.easy-share.com/tos.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("<title>Download(.*?), upload your files and earn money.</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        String followurl = br.getRegex(Pattern.compile("<div id=\"dwait\">.*?<br>.*?<script type=\"text/javascript\">.*?u='(.*?)'", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (followurl == null) {
            String filesize = br.getRegex("File size:(.*?)\\.").getMatch(0);
            if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
            return true;
        }
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        if (!br.getRegex("File size:(.*?)\\.").matches()) {
            String wait = br.getRegex(Pattern.compile("<script type=\"text/javascript\">.*?u='.*?';.*?w='(.*?)';.*?setTimeout", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            long waitfor = new Long(wait) * 1000;
            if (waitfor > 40000l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitfor); }
            sleep(waitfor, downloadLink);
            br.getPage(downloadLink.getDownloadURL());
        }
        Form form = br.getForm(0);
        String captchaUrl = br.getRegex("<form action=\".*?\".*?method=\"POST\">.*?<br>.*?<img src=\"(.*?)\">").getMatch(0);
        File captchaFile = this.getLocalCaptchaFile(this);
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaUrl));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        form.put("captcha", captchaCode);
        /* Datei herunterladen */
        dl = RAFDownload.download(downloadLink, br.createFormRequest(form), true, 1);
        if (!dl.connect(br).isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
