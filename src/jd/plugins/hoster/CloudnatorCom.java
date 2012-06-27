package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shragle.com", "cloudnator.com" }, urls = { "BLABLANEVERUSEMEBLABLA", "http://[\\w\\.]*?(shragle|cloudnator)\\.(com|de)/files/[\\w]+/.*" }, flags = { 2, 2 })
public class CloudnatorCom extends PluginForHost {

    static String         apikey = "078e5ca290d728fd874121030efb4a0d";

    private static String AGENT  = RandomUserAgent.generate();

    private String        url;

    public CloudnatorCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.cloudnator.com/index.php?p=accounts&ref=386");
        this.setStartIntervall(5000l);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.de/", "\\.com/").replace("http://shragle", "http://www.shragle"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("shragle.com/", "cloudnator.com/"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        this.br.getPage("http://www.cloudnator.com/api.php?key=" + CloudnatorCom.apikey + "&action=checkUser&useMD5=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(JDHash.getMD5(account.getPass())));
        final String accountinfos[] = this.br.getRegex("(.*?)\\|(.*?)\\|(.+)").getRow(0);
        if (accountinfos == null) {
            account.setValid(false);
            return ai;
        }
        String points = accountinfos[2];
        if (points.contains(".")) {
            points = points.replaceFirst("\\..+", "");
        }
        ai.setPremiumPoints(Long.parseLong(points.trim()));
        if (accountinfos[0].trim().equalsIgnoreCase("1")) {
            account.setValid(false);
            ai.setStatus("No Premium Account");
        } else if (accountinfos[0].trim().equalsIgnoreCase("2")) {
            account.setValid(true);
            ai.setStatus("Premium Account");
        }
        ai.setValidUntil(Long.parseLong(accountinfos[1].trim()) * 1000l);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.cloudnator.com/about/imprint";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 1000;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.correctDownloadLink(downloadLink);
        this.br.setFollowRedirects(true);
        this.requestFileInformation(downloadLink);
        this.br.setCookie("http://www.cloudnator.com", "lang", "de_DE");
        this.br.getPage(url);
        final boolean mayfail = this.br.getRegex("Download-Server ist unter").matches();
        handleErrors();
        String wait = this.br.getRegex("var downloadWait = (\\d+);").getMatch(0);
        Form form = this.br.getFormbyProperty("name", "download");
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (wait == null) {
            wait = "30";
        }
        String id = this.br.getRegex("challenge\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        final long timeBefore = System.currentTimeMillis();
        int waitT = Integer.parseInt(wait);
        if (id != null) {
            /* captcha available */
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(form);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            Form rcform = rc.getForm();
            rcform.put("recaptcha_challenge_field", rc.getChallenge());
            rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
            form = rc.getForm();
        }
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        waitT -= passedTime;
        if (waitT > 0) this.sleep(waitT * 1001l, downloadLink);
        form.setAction(form.getAction());
        form.remove("submit");
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, form, true, 1);
        final URLConnectionAdapter con = this.dl.getConnection();
        if ((con.getContentType() != null) && con.getContentType().contains("html")) {
            this.br.followConnection();
            if (this.br.containsHTML("Sicherheitscode falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if ((this.br.containsHTML("Die von Ihnen angeforderte Datei") && this.br.containsHTML("Bitte versuchen Sie es")) || mayfail) {
                if (downloadLink.getLinkStatus().getRetryCount() > 2) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            if (br.getURL().contains("p=error503")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 10 * 60 * 1000l);
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("(<b>Warning</b>|mysql_)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
        if (this.br.containsHTML("(Ihre Session-ID ist|tige Session-ID.)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "SESSION-ID Invalid", 10 * 60 * 1000l);
        if (this.br.containsHTML("bereits eine Datei herunter")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already loading, please wait!", 10 * 60 * 1000l); }
        if (this.br.containsHTML("The selected file was not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String wait = this.br.getRegex(Pattern.compile("Bitte warten Sie(.*?)Minuten", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        // Possible language bug
        if (wait == null) wait = this.br.getRegex(Pattern.compile("Please wait (\\d+) minutes or buy a Premium account<", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l); }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.correctDownloadLink(downloadLink);
        this.requestFileInformation(downloadLink);
        this.login(account);
        br.forceDebug(true);
        this.br.setCookie("http://www.cloudnator.com", "lang", "de_DE");
        this.br.setFollowRedirects(false);
        this.br.getPage(url);
        if ((this.br.getRedirectLocation() != null) && this.br.getRedirectLocation().contains("index.php")) {
            this.br.getPage(this.br.getRedirectLocation());
        }
        if (this.br.getRedirectLocation() != null) {
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, this.br.getRedirectLocation(), true, -4);
        } else {
            final Form form = this.br.getFormbyProperty("name", "download");
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, form, true, 0);
        }
        final URLConnectionAdapter con = this.dl.getConnection();
        if ((con.getContentType() != null) && con.getContentType().contains("html")) {
            this.br.followConnection();
            if ((this.br.containsHTML("Die von Ihnen angeforderte Datei") && this.br.containsHTML("Bitte versuchen Sie es"))) {
                if (downloadLink.getLinkStatus().getRetryCount() > 2) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void init() {
        this.br.setRequestIntervalLimit(this.getHost(), 800);
    }

    private void login(final Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", AGENT);
        this.br.setFollowRedirects(true);
        this.br.getPage("http://www.cloudnator.com/");
        this.br.postPage("http://www.cloudnator.com/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&cookie=1&submit=Login");
        String Cookie = this.br.getCookie("http://www.cloudnator.com", "userID");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = this.br.getCookie("http://www.cloudnator.com", "username");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = this.br.getCookie("http://www.cloudnator.com", "password");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        this.br.getPage("http://www.cloudnator.com/?cat=user");
        if (this.br.containsHTML(">Premium-Upgrade<")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", AGENT);
        final String id = new Regex(downloadLink.getDownloadURL(), "cloudnator.com/files/(.*?)/").getMatch(0);
        final String[] data = Regex.getLines(this.br.getPage("http://www.cloudnator.com/api.php?key=" + CloudnatorCom.apikey + "&action=getStatus&fileID=" + id));
        if (data.length != 4) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String name = data[0];
        this.url = "http://www.cloudnator.com/files/" + id + "/" + Encoding.urlEncode(name);
        final String size = data[1];
        final String md5 = data[2];
        // status 0: all ok 1: abused
        final String status = data[3];
        if (!status.equals("0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(name.trim());
        downloadLink.setDownloadSize(Long.parseLong(size));
        downloadLink.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}