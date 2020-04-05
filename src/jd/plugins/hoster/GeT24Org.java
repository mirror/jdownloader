package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 41665 $", interfaceVersion = 3, names = { "get24.org" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class GeT24Org extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NICE_HOST          = "get24.org";
    private static final String                            NICE_HOSTproperty  = "get24org";
    private static final String                            VERSION            = "0.0.1";
    private static final Integer                           MAXSIM             = 3;

    public GeT24Org(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://get24.org/pricing");
    }

    @Override
    public String getAGBLink() {
        return "https://get24.org/terms";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "Jdownloader " + VERSION);
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        // TODO: status
        final AccountInfo acc_info = new AccountInfo();
        this.br = newBrowser();
        String response = br.postPage("https://get24.org/api/login", "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + JDHash.getSHA256(account.getPass()));
        Long date_expire = TimeFormatter.getMilliSeconds(PluginJSonUtils.getJson(response, "date_expire"), "yyyy-MM-dd", Locale.ENGLISH);
        acc_info.setValidUntil(date_expire);
        long transfer_left = (long) (Float.parseFloat(PluginJSonUtils.getJson(response, "transfer_left")) * 1024 * 1024 * 1024);
        acc_info.setTrafficLeft(transfer_left);
        long transfer_max = (long) (Float.parseFloat(PluginJSonUtils.getJson(response, "transfer_max")) * 1024 * 1024 * 1024);
        acc_info.setTrafficMax(transfer_max);
        account.setMaxSimultanDownloads(MAXSIM);
        account.setConcurrentUsePossible(true);
        // hosts list
        response = br.getPage("https://get24.org/api/hosts/enabled");
        ArrayList<String> supportedHosts = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(response);
        acc_info.setMultiHostSupport(this, supportedHosts);
        account.setType(AccountType.PREMIUM);
        // acc_info.setStatus("Premium User");
        return acc_info;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        String post_data = "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + JDHash.getSHA256(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL());
        // "&url=" + Encoding.urlEncode(link.getDownloadURL());
        String response = br.postPage("https://get24.org/api/debrid/geturl", post_data); // security
        if (!Boolean.parseBoolean(PluginJSonUtils.getJson(response, "ok"))) {
            String reason = PluginJSonUtils.getJson(response, "reason");
            if (reason.equals("invalid credentials")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong login or password");
            } else if (reason.equals("user not activated")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Email not activated");
            } else if (reason.equals("premium required")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "premium required");
            } else if (reason.equals("premiumX required")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "premiumX required");
            } else if (reason.equals("no transfer")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "daily limit exceeded");
            } else if (reason.equals("host daily limit exceeded")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "daily limit exceeded for this host"); // TODO: allow launching other
                                                                                                           // links/hosts
            } else if (reason.equals("file removed")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (reason.equals("host not supported")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "host not supported");
            } else if (reason.equals("host disabled")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "host disabled");
            } else if (reason.equals("temporary error")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "temporary error", 60 * 60 * 1000l); // we can try
                                                                                                                         // another links or
                                                                                                                         // hosts probably
            } else if (reason.equals("unknown error")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "unknown error", 60 * 60 * 1000l); // we can try another
                                                                                                                       // hosts or maybe
                                                                                                                       // even links
            } else {
                throw new PluginException(LinkStatus.ERROR_RETRY, "unknown error");
            }
        }
        String url = PluginJSonUtils.getJson(response, "url");
        // TODO: resume support
        // but jdownloader is not saving final link and allways asks for new so it has to be implemented server side first to not waste traffic
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, url, false, 1);
        // TODO: validate status code
        dl.startDownload();
        // link_hash
        // url
        // filename
        // filesize
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
