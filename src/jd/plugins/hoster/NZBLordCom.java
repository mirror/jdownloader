package jd.plugins.hoster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetFile;
import org.jdownloader.plugins.components.usenet.UsenetFileSegment;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nzblord.com" }, urls = { "" })
public class NZBLordCom extends PluginForHost {

    public NZBLordCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://nzblord.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://nzblord.com/terms/";
    }

    private boolean isUsenetLink(DownloadLink link) {
        return link != null && "usenet".equals(link.getHost());
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null || !isUsenetLink(downloadLink)) {
            return false;
        } else {
            return super.canHandle(downloadLink, account);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (isIncomplete(parameter)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    private boolean isIncomplete(DownloadLink link) {
        return link.getBooleanProperty("incomplete", Boolean.FALSE);
    }

    private void setIncomplete(DownloadLink link, boolean b) {
        link.setProperty("incomplete", Boolean.valueOf(b));
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void login(AccountInfo ai, Account account) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            final Cookies cookies = account.loadCookies("");
            try {
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for nzblord.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.getPage("http://nzblord.com");
                    if (!StringUtils.containsIgnoreCase(br.toString(), "<b>" + userName + "</b>")) {
                        br.getCookies(getHost()).clear();
                    } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                        br.getCookies(getHost()).clear();
                    }
                }
                if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    account.clearCookies("");
                    br.getPage("http://nzblord.com/login/THc9PQ==/");
                    Form login = br.getForm(0);
                    login.put("email", Encoding.urlEncode(userName));
                    login.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(login);
                    if (!StringUtils.containsIgnoreCase(br.toString(), "<b>" + userName + "</b>")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    login = br.getForm(0);
                    if (login != null && login.containsHTML("email") && login.containsHTML("password")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
                final String validUntil = br.getRegex("Until\\s*<b><span\\s*title=\"(\\d+:\\d+:\\d+)\">(\\d+-\\d+-\\d+)").getMatch(1);
                if (validUntil != null) {
                    if (ai != null) {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!br.containsHTML("∞</span>UNLIMITED")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (ai != null) {
                    ai.setUnlimitedTraffic();
                    ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        login(ai, account);
        return ai;
    }

    private String buildTaskID(UsenetFile usenetFile) {
        final StringBuilder sb = new StringBuilder();
        for (UsenetFileSegment segment : usenetFile.getSegments()) {
            sb.append(segment.getSize());
            sb.append(segment.getMessageID());
        }
        return Hash.getSHA256(sb.toString());
    }

    private String getTask(UsenetFile usenetFile) throws Exception {
        final String taskID = buildTaskID(usenetFile);
        br.setFollowRedirects(true);
        br.getPage("http://nzblord.com/downloads/");
        final String tasks[][] = br.getRegex("<a href=\"(/downloads/browse/\\d+/?)\" class=\".*?\">(.*?)<").getMatches();
        for (final String task[] : tasks) {
            if (taskID.equals(task[1].replaceAll("\\s*", ""))) {
                return task[0];
            }
        }
        return null;
    }

    private String createTask(UsenetFile usenetFile) throws Exception {
        final String taskID = buildTaskID(usenetFile);
        final PostFormDataRequest nzbForm = new PostFormDataRequest("http://nzblord.com/task/content/");
        final StringBuilder nzbData = new StringBuilder();
        nzbData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        nzbData.append("<nzb>\r\n");
        nzbData.append("<file poster=\"Download via JDownloader\" date=\"" + (System.currentTimeMillis() / 1000) + "\" subject=\"" + taskID + "\">\r\n");
        nzbData.append("<groups>\r\n<group>alt.binaries</group>\r\n</groups>\r\n");
        nzbData.append("<segments>\r\n");
        for (UsenetFileSegment segment : usenetFile.getSegments()) {
            // & -> &amp; because of XML encoding
            nzbData.append("<segment bytes=\"" + segment.getSize() + "\" number=\"" + segment.getIndex() + "\">" + segment.getMessageID().replace("&", "&amp;") + "</segment>\r\n");
        }
        nzbData.append("</segments>\r\n</file>\r\n</nzb>");
        nzbForm.addFormData(new FormData("nzb_content", nzbData.toString()));
        br.setFollowRedirects(true);
        br.getPage("http://nzblord.com/task/");
        br.setFollowRedirects(false);
        br.getPage(nzbForm);
        final String redirect = br.getRedirectLocation();
        if (redirect != null && redirect.matches(".*/downloads/browse/\\d+/?$")) {
            return redirect;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private Number parseNumber(Object number) throws PluginException {
        if (number instanceof Number) {
            return (Number) number;
        } else if (number instanceof String) {
            return Long.parseLong(number.toString());
        } else if (number != null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return null;
        }
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        login(null, account);
        final UsenetFile usenetFile = UsenetFile._read(downloadLink);
        String task = getTask(usenetFile);
        if (task == null) {
            task = createTask(usenetFile);
        }
        String filesURL = null;
        br.setFollowRedirects(true);
        while (true) {
            br.getPage(task);
            if (br.containsHTML(">Finished with error")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String statusURL = br.getRegex("statusURL\\s*=\\s*\"(/downloads/status/\\?id=[a-f0-9]+)").getMatch(0);
            filesURL = br.getRegex("filesURL\\s*=\\s*\"(/js/files/\\?id=\\d+)").getMatch(0);
            if (filesURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (statusURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HashMap<String, Object> statusInfo = JSonStorage.restoreFromString(br.getPage(statusURL), TypeRef.HASHMAP);
            final Number finished = parseNumber(statusInfo.get("finished"));
            final Number files_amount = parseNumber(statusInfo.get("files_amount"));
            if (files_amount == null || files_amount.longValue() != 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (finished == null || finished.longValue() > 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (finished.longValue() == 0) {
                sleep(60 * 1000l, downloadLink, "Wait for DownloadLink");
            } else {
                break;
            }
        }
        final HashMap<String, Object> fileInfo = JSonStorage.restoreFromString(br.getPage(filesURL), TypeRef.HASHMAP);
        final List<Map<String, Object>> files = (List<Map<String, Object>>) fileInfo.get("files");
        if (files == null || files.size() != 1) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> file = files.get(0);
        final Number error = parseNumber(file.get("error"));
        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String fileName = (String) file.get("title");
        final Number fileID = parseNumber(file.get("id"));
        if (fileID != null) {
            if (fileName != null) {
                downloadLink.setFinalFileName(fileName);
            }
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, "http://nzblord.com/downloads/file/" + fileID.longValue() + "/", true, 0);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dl.startDownload();
            return;
        }
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        return account != null && isUsenetLink(link);
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.USENET };
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
