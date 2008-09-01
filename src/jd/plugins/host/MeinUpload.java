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

import jd.http.Browser;
import jd.http.GetRequest;
import jd.http.HeadRequest;
import jd.http.PostRequest;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://www.xup.in/dl,43227676/YourFilesBiz.java/

public class MeinUpload extends PluginForHost {
    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getMatch(0).*= "0.1.0";
    private static final String AGB_LINK = "http://meinupload.com/#help.html";
    // 
    private static final String CODER = "jD-Team";
    private static final String HOST = "meinupload.com";

    static private final Pattern PATTERN_SUPPORTED = Pattern.compile("(http://[\\w\\.]*?meinupload.com/{1,}dl/.+/.+)|(http://[\\w\\.]*?meinupload\\.com/\\?d=.*)", Pattern.CASE_INSENSITIVE);

    // private static final int MAX_SIMULTAN_DOWNLOADS = 1;

    public MeinUpload() {
        super();
        this.enablePremium();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        PostRequest r = new PostRequest(downloadLink.getDownloadURL());
        r.setPostVariable("submit", "Kostenlos");
        r.setPostVariable("sent", "1");
        r.load();
        Form[] forms = Form.getForms(r.getHtmlCode());
        if (forms.length != 1 || !forms[0].getVars().containsKey("download")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        sleep(15000, downloadLink);
        r = (PostRequest) new PostRequest(forms[0]).connect();

        if (r.getResponseHeader("Content-Disposition") == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        dl = new RAFDownload(this, downloadLink, r.getHttpConnection());
        dl.startDownload();
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setCookiesExclusive(true);br.clearCookies(HOST);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("http://meinupload.com/status.html");
        Form login = br.getForm(0);
        login.put("user", account.getUser());
        login.put("pass", account.getPass());
        br.submitForm(login);

        br.getPage("http://meinupload.com/account.html?aktion=status");

        String expire = br.getRegex("Account g&uuml;ltig bis: </td><td align=.*?>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setValid(false);
            ai.setStatus("Account invalid. Logins wrong?");
            return ai;
        }
        String trafficLeft = br.getRegex("Verbleibender Traffic: </td><td align=.*?>(.*?)/td>").getMatch(0);
        String points = br.getRegex("<td>Gesammelte Punkte: </td>.*?<td align=.*?>([\\d]*?)</td>").getMatch(0);
        String cash = br.getRegex(" <td><b>Guthaben:</b> </td>.*?<td align=.*?><b>([\\d]*?) \\&euro\\;</b></td>").getMatch(0);
        String files = br.getRegex("<td>Hochgeladene Dateien: </td>.*?<td align=.*?>([\\d]*?)</td>").getMatch(0);

        ai.setStatus("Account is ok.");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd.MM.yyyy", null));

        ai.setTrafficLeft(Regex.getSize(trafficLeft));
        ai.setPremiumPoints(Integer.parseInt(points));
        ai.setAccountBalance(Integer.parseInt(cash) * 100);
        ai.setFilesNum(Integer.parseInt(files));

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        downloadLink.getLinkStatus().setStatusText(JDLocale.L("downloadstatus.premiumload", "Premiumdownload"));
        downloadLink.requestGuiUpdate();
        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("meinupload.com/{1,}dl/([\\d]*?)/", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (id == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        try {
            GetRequest r = new GetRequest("http://MeinUpload.com/server.api?id=" + id);
            r.getHeaders().put("Accept", "text/html, */*");
            r.getHeaders().put("Accept-Encoding", "identity");
            r.getHeaders().put("Referer", "http://MeinUpload.com/");
            r.getHeaders().put("User-Agent", " MeinUpload Tool - v2.2");

            String server = r.load();
            if (server == null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
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
            // http://dl2.MeinUpload.com/download.api?user=23729405&pass=0865
            // a2801d938ce3e59024b4ef1d6d30&id=3407292519
            // GET
            ///download.api?user=23729405&pass=0865a2801d938ce3e59024b4ef1d6d30&
            // id=9923945611
            // HTTP/1.1
            // v
            if (r.getResponseHeader("Content-Disposition") == null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }

            dl = new RAFDownload(this, downloadLink, r.getHttpConnection());
            dl.setChunkNum(1);
            dl.setResume(false);
            dl.startDownload();
            dl.getFile();
            if (dl.getFile().length() < 6000) {
                String page = JDUtilities.getLocalFile(dl.getFile());
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setParameter(JDLocale.L("errors.interbalhostererror",
                // "Internal Hoster Error"));
                logger.severe(page);
                return;
            }
            return;
        } catch (IOException e) {

            e.printStackTrace();
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
   
        
        br.getPage(downloadLink.getDownloadURL());
        
        if(br.getRedirectLocation()!=null){
            String error=br.getRegex("code=(.*)").getMatch(0);
           downloadLink.getLinkStatus().setStatusText(error);
           return false;
        }
        String filename=br.getRegex("<title>(.*?)</title>").getMatch(0);
        downloadLink.setName(filename);
       Form form = br.getForm("Free");
      br.submitForm(form);
      try{
          String s=br.getRegex("Dateigr.*e:</b></td>.*<td align=left>(.*?[MB|KB|B])</td>").getMatch(0);
      long size = Regex.getSize(s);
      if(size>0)downloadLink.setDownloadSize(size);
      }catch(Exception e ){}
       return true;
//            String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("meinupload.com/{1,}dl/([\\d]*?)/", Pattern.CASE_INSENSITIVE)).getMatch(0);
//            if(id==null){
//               id= new Regex(downloadLink.getDownloadURL(), Pattern.compile("meinupload.com/\\?d=(.*)", Pattern.CASE_INSENSITIVE)).getMatch(0);
//                
//            }
//            if (id == null) { return false;
//            // http://meinupload.com/infos.api?get_id=3794082988
//            }
//
//            String page = new GetRequest("http://meinupload.com/infos.api?get_id=" + id).load();
//
//            String status = new Regex(page, "<status>([\\d]*?)</status>").getMatch(0);
//            String filesize = new Regex(page, "<filesize>([\\d]*?)</filesize>").getMatch(0);
//            String name = new Regex(page, "<name>(.*?)</name>").getMatch(0);
//            if (status == null || !status.equals("1")) { return false; }
//
//            if (filesize == null || name == null) { return false; }
//
//            downloadLink.setDownloadSize(Integer.parseInt(filesize));
//            downloadLink.setName(name);
//            return true;

    
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }
    
    @Override
    public void reset() {

    }

    @Override
    public void resetPluginGlobals() {
    }

}