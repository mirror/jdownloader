//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
//    along with program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision: 31871 $", interfaceVersion = 2, names = { "upnito.sk" }, urls = { "https?://[\\w\\.]*?upnito\\.sk/(download\\.php\\?(dwToken=[a-z0-9]+|file=.+)|subor/[a-z0-9]+\\.html)" })
public class UpNitoSk extends PluginForHost {

    private static AtomicBoolean wthack = new AtomicBoolean(true);

    public UpNitoSk(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.upnito.sk/kredit.php");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fileId = new Regex(link.getDownloadURL(), "upnito\\.sk/subor/([a-z0-9]+)\\.html").getMatch(0);
        if (fileId != null) {
            link.setUrlDownload("http://www.upnito.sk/download.php?dwToken=" + fileId);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://upnito.sk/account.php");
        account.setValid(true);
        final String files = br.getRegex("<strong>Počet súborov:</strong> (\\d+)<br>").getMatch(0);
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }
        String trafficLeft = br.getRegex(">Aktuálny kredit:</strong>(.*?)\\(").getMatch(0);
        if (trafficLeft != null) {
            trafficLeft = trafficLeft.trim().replace(",", "");
            trafficLeft = new Regex(trafficLeft, "(\\d+)").getMatch(0);
            if (trafficLeft != null) {
                int traffic = Integer.parseInt(trafficLeft);
                traffic = traffic * 1024;
                trafficLeft = traffic + "KB";
                ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
            }
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.upnito.sk/pravidla.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Nemozete tolkokrat za sebou stahovat ten isty subor!")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        // dlpage
        final String DLPAGE = br.getBaseURL();
        final StringBuilder sb = new StringBuilder();
        // Mozilla Firefox == 4B72D719A28ADD813104D45F414EFECF.cache.html
        final Browser br2 = br.cloneBrowser();
        br2.getPage(DLPAGE + "odpocitavac/" + "4B72D719A28ADD813104D45F414EFECF.cache.html");
        final String dl2 = br2.getRegex("_A='(.*?)'").getMatch(0);
        if (br.containsHTML("function|var")) {
            // js-Funktionen parsen
            final String fn[] = br2.getRegex("((;f|f)unction|(;v|var{1}))(.*?)\n").getColumn(-1);
            if (dl2 == null || fn == null || fn.length < 662) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (int i = 647; i < fn.length - 1; i++) {
                sb.append(fn[i].replace("hz();", ""));
            }
            // js-Variablen parsen
            final String var = fn[fn.length - 1];
            if (var.startsWith("var") && var.contains(";")) {
                final String _var[] = var.replace(";var", ";\nvar").split("\n");
                sb.append(_var[0]);
                for (int i = 1; i < _var.length - 2; i++) {
                    sb.append(new Regex(_var[i], "^(.*?);").getMatch(-1));
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            logger.warning("Javascript regex seems to be broken. This could cause errors...");
        }
        // dwToken
        final String damnToken = new Regex(downloadLink.getDownloadURL(), "dwToken=([a-z0-9]+)").getMatch(0);
        // Wartezeit und Schlüssel besorgen
        br2.getPage(DLPAGE + "getwait.php?dwToken=" + damnToken);
        String gwtValidate = br2.toString().trim();
        if (gwtValidate == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (UpNitoSk.wthack.get() == false) {
            int sleepTime = 600;
            final String ttt = br2.getRegex("(\\d+);").getMatch(0);
            if (ttt != null) {
                sleepTime = Integer.parseInt(ttt);
            } else {
                logger.warning("Sleeptime regex seems to be broken. This could cause errors...");
            }
            sleep(sleepTime * 1001l, downloadLink);
        }
        // Button nach Wartezeit
        Form dlform = br.getForm(1);
        dlform.put("dl2", dl2);
        dlform.put("gwt_validate", Encoding.urlEncode(generateGWT(sb, gwtValidate, damnToken)));
        br.submitForm(dlform);
        // Downloadbutton
        dlform = br.getForm(0);
        final String verifytext = br.getCookie("http://upnito.sk/", "verifytext");
        if (verifytext != null) {
            dlform.put("verifytext", verifytext);
        }
        if (dlform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dlform.put("tahaj", "Stiahnu%9D");
        /* Convert form to post String because some values might be in there twice (by purpose)! */
        final List<InputField> fields = dlform.getInputFields();
        String postdata = "";
        for (final InputField field : fields) {
            if (!postdata.equals("")) {
                postdata += "&";
            }
            postdata += field.getKey() + "=" + field.getValue();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform.getAction(), postdata, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Neplatne GWT overenie!")) {
                logger.warning("Waittimehack is outdated!");
            }
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        final String whySoComplicated = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/download\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (whySoComplicated != null) {
            br.getPage(whySoComplicated);
        }
        final String dllink = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/ddl\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    private String jsAlgo(final String function, final String fun, final String gwt, final String token) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        final String injectFn = "function encrypt(g,y){gz=g.split(';');_y=y;mz(bB);b=gz[0]+aB+gz[1]+aB+mz(gz[1]+gz[0]+bB+_y+mz(bB));return b;};function decrypt(g,y){gz=g.split(';');_y=y;mz(DD);c=mz(gz[0]+_y+DD+gz[1]+ED+mz(DD));return c;}";
        try {
            engine.eval(fun + injectFn);
            result = inv.invokeFunction(function, gwt, token);
        } catch (final Exception e) {
            logger.log(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return result.toString();
    }

    private String generateGWT(StringBuilder sb, String... s) throws Exception {
        final String gwt[] = s[0].split(";");
        if (UpNitoSk.wthack.get() == true) {
            gwt[1] = String.valueOf(Integer.parseInt(gwt[1]) - Integer.parseInt(gwt[0]));
        }
        s[0] = gwt[0] + ";" + gwt[1] + ";" + gwt[2];
        final String key = jsAlgo("decrypt", sb.toString(), s[0], s[1]);
        s[0] = gwt[0] + ";" + gwt[1] + ";" + key;
        s[0] = jsAlgo("encrypt", sb.toString(), s[0], s[1]);
        return s[0];
    }

    private void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.getPage("http://www.upnito.sk/badlogin.php");
        br.postPage("http://www.upnito.sk/?action=doLogin", "meno=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()));
        if ((br.getCookie("http://www.upnito.sk", "uid") == null) || (br.getCookie("http://www.upnito.sk", "pass") == null)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setCustomCharset("windows-1250");
        br.getPage(link.getDownloadURL());
        final String whySoComplicated = br.getRegex("'(http://dl[0-9]+\\.upnito\\.sk/download\\.php\\?dwToken=[a-z0-9]+)'").getMatch(0);
        if (whySoComplicated != null) {
            br.getPage(whySoComplicated);
        }
        if (br.containsHTML("location\\.href=\\'/notfound\\.php\\'")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("Nemozete tolkokrat za sebou stahovat ten isty subor!")) {
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("Ahoj, chystáš sa stiahnuť súbor.*?>(.*?)</em>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<strong style=\"color: #663333;\">(.*?)</strong>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Súbor:</strong>(.*?)<br").getMatch(0);
            }
        }
        final String filesize = br.getRegex("Veľkosť:</strong>(.*?)<br>").getMatch(0);
        if ((filename == null) || (filesize == null)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Set final filename here because server sometimes sends wrong names
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}