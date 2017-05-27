//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.appwork.utils.formatter.SizeFormatter;
import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freshfile.pl" }, urls = { "https?://freshfile\\.pl/dl/(.*)" })
public class FreshfilePl extends PluginForHost {

    public FreshfilePl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://freshfile.pl/premium/");
    }

    private String        MAINPAGE = "http://freshfile.pl";
    private static Object LOCK     = new Object();

    @Override
    public String getAGBLink() {
        return "http://freshfile.pl/terms/";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        // correct link stuff goes here, stable is lame!
        for (DownloadLink link : urls) {
            if (link.getProperty("FILEID") == null) {
                String downloadUrl = link.getDownloadURL();
                String fileID = new Regex(downloadUrl, "https?://freshfile\\.pl/dl/([A-Za-z0-9]+)/?").getMatch(0);
                link.setProperty("FILEID", fileID);
            }
        }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                boolean first = true;
                for (final DownloadLink dl : links) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append(dl.getProperty("FILEID"));
                    first = false;
                }
                // API: http://freshfile.pl/api/doFile/ - info about links
                // input POST: id = fileId1, fileID2,...,fileIDn
                br.postPage("http://freshfile.pl/api/doFile/", "id=" + sb.toString());
                // Response:
                // id: file id
                // url: URL of file
                // name: file name
                // size: file size in bytes
                // status: 1 - ok, 0 - file not available
                // emptyData: No input data
                String response = br.toString();
                int fileNumber = 0;
                for (final DownloadLink dllink : links) {
                    final String source = new Regex(response, "\"" + fileNumber + "\":\\{(.+?)\\}").getMatch(0);
                    if (source == null) {
                        logger.warning("Availablecheck broken for freshfile.pl");
                        return false;
                    }
                    String fileStatus = getJson("status", source);
                    String fileName = getJson("name", source);
                    String fileUrl = getJson("url", source);
                    if (fileName == null && fileUrl != null) {
                        fileUrl = fileUrl.replace("\\", "");
                        fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                    }
                    if (fileName == null) {
                        fileName = dllink.getName();
                    }
                    String fileSize = getJson("size", source);
                    if (fileStatus.equals("0")) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker returns not available for: " + getHost() + " and link: " + dllink.getPluginPatternMatcher());
                    } else {
                        fileName = Encoding.htmlDecode(fileName.trim());
                        fileName = Encoding.unicodeDecode(fileName);
                        dllink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
                        dllink.setDownloadSize(SizeFormatter.getSize(fileSize));
                        dllink.setAvailable(true);
                    }
                    fileNumber++;
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doDownloads(downloadLink, null);
    }

    void setLoginData(final Account account) throws Exception {
        br.getPage("http://freshfile.pl/");
        br.setCookiesExclusive(true);
        final Object ret = account.getProperty("cookies", null);
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        if (account.isValid()) {
            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                this.br.setCookie("http://freshfile.pl/", key, value);
            }
        }
    }

    void doDownloads(final DownloadLink downloadLink, final Account account) throws PluginException, Exception {
        boolean retry;
        boolean accountFound = !(account == null);
        String loginInfo = "";
        setMainPage(downloadLink.getPluginPatternMatcher());
        String response = "";
        // loop because wrong handling of
        // LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE when Free user has waittime for the next
        // downloads
        do {
            retry = false;
            requestFileInformation(downloadLink);
            if (accountFound) {
                // thid in case we will need something from account
                loginInfo = login(account, false);
            }
            br.setFollowRedirects(true);
            // API: http://freshfile.pl/api/doDownloadFile/ - file download
            // Input POST:
            // for Registered: login - user login, password - user password, id - file id
            // for Unregistered: login=null, password= null, id - file id
            if (accountFound) {
                br.postPage("http://freshfile.pl/api/doDownloadFile/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&id=" + downloadLink.getProperty("FILEID"));
            } else {
                br.postPage("http://freshfile.pl/api/doDownloadFile/", "login=&password=&id=" + downloadLink.getProperty("FILEID"));
            }
            response = br.toString();
            retry = handleErrors(response, downloadLink);
        } while (retry);
        String fileLocation = getJson("downloadUrl", response);
        if (fileLocation == null) {
            logger.info("Hoster: FreshFile.pl reports: filelocation not found with link: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "filelocation not found");
        }
        if (accountFound) {
            setLoginData(account);
        }
        String dllink = fileLocation.replace("\\", "");
        if (accountFound) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!" + "Response: " + dl.getConnection().getResponseMessage() + ", code: " + dl.getConnection().getResponseCode() + "\n" + dl.getConnection().getContentType());
            br.followConnection();
            logger.warning("br returns:" + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        doDownloads(downloadLink, account);
    }

    private boolean handleErrors(String response, DownloadLink downloadLink) throws Exception, PluginException {
        // Response for: API: http://freshfile.pl/api/doDownloadFile/ - file download
        // trafficEmpty: no traffic left
        // notFound: file not found
        // emptyData: no input data
        // nextDownload: free user: next download date
        String errors = checkForErrors(response, "error");
        boolean retry = false;
        if (errors != null) {
            if (errors.contains("nextDownload")) {
                String nextDownload = checkForErrors(response, "nextDownload");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date newStartDate = df.parse(nextDownload);
                Date actualDate = new Date();
                long leftToWait = newStartDate.getTime() - actualDate.getTime();
                if (leftToWait > 0) {
                    // doesn't work correctly
                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, leftToWait); }
                    // temporary solution
                    sleep(leftToWait, downloadLink);
                    retry = true;
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Downloads not possible!");
                }
            } else if (errors.contains("trafficEmpty")) {
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "No traffic left!");
            } else if (errors.contains("notFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File not found!");
            } else {
                logger.info("Hoster: FreshFile.pl reports:" + errors + " with link: " + downloadLink.getPluginPatternMatcher());
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errors);
            }
        } else {
            retry = false;
        }
        return retry;
    }

    private String login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            // final Object ret = account.getProperty("cookies", null);
            // Acount Info:
            // http://freshfile.pl/api/doUserAccount/
            // Input POST:
            // login
            // password
            br.postPage(MAINPAGE + "/api/doUserAccount/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            // Response:
            // login: user login
            // email: user email
            // premium: 1 - Premium, 0 - Free Account
            // premiumDate: Premium Expire Date
            // trafficDay: daily transfer limit (no_limit = without limit)
            // trafficRem: daily remained transfer limit (no_limit = no limit)
            String response = br.toString();
            String error = checkForErrors(response, "error");
            if (error != null) {
                logger.info("Hoster FreshFile.pl reports: " + error);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, error);
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("cookies", cookies);
            return response;
        }
    }

    private String checkForErrors(String source, String searchString) {
        if (source.contains("message")) {
            String errorMessage = getJson(searchString, source);
            if (errorMessage == null) {
                errorMessage = getJson("message", source);
            }
            return errorMessage;
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        String accountResponse;
        try {
            accountResponse = login(account, true);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.setProperty("cookies", Property.NULL);
                final String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Maintenance")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, e.getMessage(), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE, e).localizedMessage(e.getLocalizedMessage());
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed: " + errorMessage, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
                }
            }
            throw e;
        }
        int userPremium = Integer.parseInt(getJson("premium", accountResponse));
        String userPremiumDateEnd = getJson("premiumDate", accountResponse);
        String dailyTraffic = getJson("trafficDay", accountResponse);
        String trafficRemainded = getJson("trafficRem", accountResponse);
        if (userPremium == 0) {
            ai.setTrafficMax(SizeFormatter.getSize("5 GB"));
            ai.setTrafficLeft(SizeFormatter.getSize(trafficRemainded));
            ai.setStatus("Registered (free) user");
        } else {
            if (dailyTraffic.equals("no_limit")) {
                ai.setStatus("Premium user");
            } else {
                ai.setStatus("Premium user with limit");
                ai.setTrafficLeft(SizeFormatter.getSize(trafficRemainded));
                ai.setTrafficLeft(SizeFormatter.getSize(dailyTraffic));
            }
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date;
        if (userPremium == 1) {
            try {
                date = dateFormat.parse(userPremiumDateEnd);
                ai.setValidUntil(date.getTime());
            } catch (final Exception e) {
                logger.log(e);
            }
        }
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":[{]?\"(.+?)\"[}]?").getMatch(0);
        }
        return result;
    }

    void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://freshfile.pl";
        } else {
            MAINPAGE = "http://freshfile.pl";
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}