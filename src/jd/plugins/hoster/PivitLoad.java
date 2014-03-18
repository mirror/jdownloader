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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pivit.load" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-zjwqfddsgertewrafsdfsdt" }, flags = { 2 })
public class PivitLoad extends PluginForHost {
    /************************************************************************
     * IMPORTANT: please contact flubshi first before deleting this plugin!!!
     ************************************************************************/

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    // please do no changes here:!
    private String                                         mName              = "pivit.load";
    private final String                                   mProt              = "https://";

    private static final String                            NOCHUNKS           = "NOCHUNKS";

    public PivitLoad(PluginWrapper wrapper) {
        super(wrapper);
        prepare();
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        HashMap<String, String> accDetails = new HashMap<String, String>();
        AccountInfo ai = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String page = null;
        String hosts = null;
        try {
            page = br.getPage(mProt + mName + "/api/details.php?user=" + username + "&pw=" + pass);
            hosts = br.getPage(mProt + mName + "/api/hosts.php?user=" + username + "&pw=" + pass);
        } catch (Exception e) {
            account.setTempDisabled(true);
            account.setValid(true);
            ai.setProperty("multiHostSupport", Property.NULL);
            ai.setStatus("PivitLoad Server Error, temp disabled");
            return ai;
        }

        /* parse api response in easy2handle hashmap */
        String info[][] = new Regex(page, "<([^<>]*?)>([^<]*?)</.*?>").getMatches();
        for (String data[] : info) {
            accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
        }

        if (page.startsWith("<error>") && page.endsWith("</error>")) {
            String nr = accDetails.get("nr");
            if (nr.equals("1")) {
                account.setValid(false);
                ai.setProperty("multiHostSupport", Property.NULL);
                ai.setStatus(accDetails.get("msg"));
                return ai;
            }
        }

        String expire = accDetails.get("expire");
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", null));
        }

        for (String data[] : info) {
            accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            String hoster[] = new Regex(hosts, "\"(.*?)\"").getColumn(0);
            for (String host : hoster) {
                supportedHosts.add(host.trim());
            }
        }

        if (account.isValid()) {
            ai.setStatus("Account valid");
            ai.setProperty("multiHostSupport", supportedHosts);
        } else {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            ai.setStatus("Account invalid");
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        showMessage(link, "Phase 1/2: Generating link");

        // here we can get a 503 error page, which causes an exception
        String genlink = br.getPage(mProt + mName + "/api/dl.php?user=" + user + "&pw=" + pw + "&link=" + url);
        if (genlink.startsWith("<error>") && genlink.endsWith("</error>")) {
            if (genlink.contains("<nr>2</nr>")) { // invalid url
                // disable hoster for 2min
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l);
            } else if (genlink.contains("<nr>1</nr>")) { // invalid login
                throw new PluginException(PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (!genlink.startsWith("http://")) {
            logger.severe("PivitLoad(Error): " + genlink);
            if (genlink.contains("Hoster unsupported or under maintenance.")) {
                // disable host for 4h
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            }
            if (genlink.contains("_limit")) {
                /* limit reached for this host, wait 4h */
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            }
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                // disable hoster for 30min
                tempUnavailableHoster(acc, link, 30 * 60 * 1000l);

            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        int maxChunks = 0;
        if (link.getBooleanProperty(PivitLoad.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML(">An error occured while processing your request<")) {
                logger.info("Retrying: Failed to generate pivitload link because API connection failed for host link: " + link.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            /* unknown error */
            logger.severe("PivitLoad(Error): " + br.toString());
            // disable hoster for 5min
            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        /* save generated link */
        link.setProperty("genLinkPivitLoad", genlink);
        showMessage(link, "Phase 2/2: Download begins!");
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
                {
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(PivitLoad.NOCHUNKS, false) == false) {
                        link.setProperty(PivitLoad.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            }
        }
    }

    private void prepare() {
        try {
            // please contact flubshi (flubshi<at>web<.>de) before you replace this:
            java.lang.reflect.Field field = this.getClass().getDeclaredField(encodeParam(hexStringToByteArray("6D4E616D65")));
            field.setAccessible(true);
            field.set(this, encodeParam(hexStringToByteArray("70697669742E74696D6B72616E7A2E6465")));
        } catch (Exception e) {
        }
        setStartIntervall(4 * 1000l);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private String encodeParam(byte[] data) {

        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}