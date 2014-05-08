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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.simplejson.JSonArray;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonNode;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.storage.simplejson.JSonValue;
import org.appwork.storage.simplejson.ParserException;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "updown.bz" }, urls = { "https?://(?:www\\.)?updown\\.bz/[a-zA-Z0-9]+" }, flags = { 0 })
public class UpDownBz extends PluginForHost {

    private static final int    FORCE_LOGIN_INTERVAL_SEC = 20 * 60;
    private static final Object LOGIN_LOCK               = new Object();
    private static final String HTTP_PROTOCOL            = "https://";
    private static final String API_HOST                 = "api.updown.bz";
    private static final String WEB_HOST                 = "updown.bz";

    /*
     * Hoster is online -> https://updown.bz
     */

    public UpDownBz(PluginWrapper wrapper) throws IOException {
        super(wrapper);
        setConfigElements();
        this.enablePremium(HTTP_PROTOCOL + WEB_HOST + "/#!/premium");
    }

    @Override
    public String getAGBLink() {
        return HTTP_PROTOCOL + WEB_HOST + "/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        String id = getFileId(link.getDownloadURL());
        if (id != null && id.length() > 0) {
            link.setUrlDownload(HTTP_PROTOCOL + "updown.bz/" + id);
        } else {
            link.setAvailable(false);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        ai.setStatus("Free");
        ai.setValidUntil(0);
        account.setValid(false);

        // login to service
        String sessionid = login(account);
        if (sessionid == null) { return ai; }

        // receive userdata
        AccountData data = api_getdetails(account);
        if (data == null) { return ai; }

        // account is available and ok
        account.setValid(true);

        // eval traffic data
        long traffic_max = data.getTrafficMax();
        long traffic_used = data.getTrafficUsed();
        if (traffic_max == -1)
            ai.setUnlimitedTraffic();
        else {
            ai.setTrafficLeft(traffic_max - traffic_used);
            ai.setTrafficMax(traffic_max);
        }

        // set premium state
        ai.setValidUntil(data.getPremiumUntil() * 1000);
        ai.setStatus("Premium");

        return ai;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        // TODO process a specified number of links at once
        if (urls == null || urls.length == 0) { return false; }

        // extract file ids to check
        List<String> ids = new ArrayList<String>(urls.length);
        List<DownloadLink> links = new ArrayList<DownloadLink>(urls.length);
        for (DownloadLink url : urls) {
            String id = getFileId(url.getDownloadURL());
            if (id != null && id.length() > 0) {
                ids.add(id);
                links.add(url);
            } else {
                url.setAvailable(false);
            }
        }

        // check given ids
        Map<String, FileData> filedata = api_checkLinks(ids);
        if (filedata == null) return false;

        // update download links
        int j = 0;
        for (DownloadLink link : links) {
            String id = ids.get(j++);
            FileData data = filedata.get(id);
            if (data != null) {
                link.setAvailable(data.isOnline());
                link.setFinalFileName(data.getName());
                link.setDownloadSize(data.getSize());
                link.setVerifiedFileSize(data.getSize());
                link.setMD5Hash(data.getMd5());
                link.setProperty("id", data.getId());
                link.setProperty("protected", data.isProtected());
                link.setProperty("filedata", data);
            } else {
                link.setAvailable(false);
            }
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        DownloadLink[] dl_links = { downloadLink };
        AvailableStatus status = checkLinks(dl_links) ? AvailableStatus.TRUE : AvailableStatus.FALSE;
        return status;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        // set share password if file is protected
        String share_password = null;
        if (downloadLink.getBooleanProperty("protected", false)) {
            String dl_pass = downloadLink.getDownloadPassword();
            if (dl_pass != null && dl_pass.length() > 0) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] share_pass_bytes = md.digest((dl_pass).getBytes());
                share_password = HexFormatter.byteArrayToHex(share_pass_bytes);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "no download password set");
            }
        }

        // login for premium download request
        String sessionid = login(account);
        if (sessionid == null) throw new PluginException(LinkStatus.ERROR_RETRY, "login failed", 60 * 1000);

        // request download
        PrivateDownloadData prvdl_data = api_privatedownload(account);
        if (prvdl_data == null) throw new PluginException(LinkStatus.ERROR_RETRY, "requesting download failed", 60 * 1000);

        // TODO: verify download file

        // is enough traffic available
        if (downloadLink.getDownloadSize() > prvdl_data.getTrafficLeft()) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "traffic limit reached");

        // construct the download url
        String url = createStorageUrl(downloadLink, sessionid, prvdl_data.getHost(), share_password);

        // try to open a download connection and check first chunks response
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, (-1) * 2);

        // pass off to a shared error handling method
        handleErrors(downloadLink, account);

        // nothing wrong, thus start the download
        dl.startDownload();
    }

    private void handleErrors(final DownloadLink downloadLink, final Account account) throws PluginException {
        int code = dl.getConnection().getResponseCode();
        // file is not available
        if (code == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "requested file is not available");

        // you are not authorized
        if (code == 401) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "invalid login data" + (downloadLink.getBooleanProperty("protected", false) ? " or wrong download password" : ""), 6 * 60 * 1000L);

        // wrong request format
        if (code == 400) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "wrong request format"); }

        // wrong request method
        if (code == 405) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "wrong request method"); }

        // no connections available
        if (code == 421) {
            if (account != null)
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "connection limit reached", 1 * 60 * 1000L);
            else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "connection limit reached", 5 * 60 * 1000l);
        }

        // is data to download available
        if (code == 200) {
            long downloadsize = dl.getConnection().getLongContentLength();
            if (downloadsize != downloadLink.getDownloadSize() || !dl.getConnection().isContentDisposition()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no valid data to download found");
        } else if (code == 206) {
            long downloadsize = -1;

            String content_range = dl.getConnection().getHeaderField("Content-Range");
            if (content_range != null) {
                Matcher m = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)").matcher(content_range);
                if (m.find()) {
                    downloadsize = Long.parseLong(m.group(3));
                }
            }

            if (downloadsize != downloadLink.getDownloadSize() || !dl.getConnection().isContentDisposition()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no valid data to download found");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no valid data to download found");
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        final String fuid = getFileId(link.getDownloadURL());
        if (fuid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Can not find unique file ID");
        long ttw = 0;
        // cached links don't seem to be possible
        String dl_host = null;
        if (dl_host == null) {
            requestFileInformation(link);
            br.postPageRaw(HTTP_PROTOCOL + API_HOST, "{\"m\":\"pub\",\"a\":\"dl\",\"d\":{\"i\":\"" + fuid + "\"}}");

            // try to parse api response
            JSonObject json_response = (JSonObject) new JSonFactory(br.toString()).parse();
            ApiStatus status = ApiStatus.UNKNOWN;
            try {
                status = ApiStatus.get((Long) ((JSonValue) json_response.get("c")).getValue());
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

            // do, whats to do
            if (status.isSuccess()) {
                try {
                    // extract json data object
                    JSonObject json_data = (JSonObject) json_response.get("d");
                    if (json_data == null) return;
                    long wtime = (Long) ((JSonValue) json_data.get("t")).getValue();
                    long ctime = (Long) ((JSonValue) json_response.get("t")).getValue();
                    ttw = wtime - ctime;
                    dl_host = (String) ((JSonValue) json_data.get("h")).getValue();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Status isn't Success, some error handling required here...");
        }
        if (dl_host != null) {
            sleep(ttw * 1001, link);
            final String url = HTTP_PROTOCOL + dl_host + "/d/?file=" + fuid;
            // try to open a download connection and check first chunks response
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 1);

            // pass off to a shared error handling method
            handleErrors(link, null);

            // nothing wrong, thus start the download
            dl.startDownload();
        } else
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find dl_host");

    }

    @Override
    public void reset() {
        return;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        return;
    }

    private String login(Account account) {
        return login(account, false);
    }

    private String login(Account account, boolean force) {
        String sessionid = null;

        // line up login calls from different threads
        synchronized (LOGIN_LOCK) {

            // execute login call if session is invalid, time is up or forced by caller
            sessionid = account.getStringProperty("sessionid", null);
            boolean time_to_login = System.currentTimeMillis() - account.getLongProperty("last_login_at", 0) > FORCE_LOGIN_INTERVAL_SEC * 1000;
            if (force || sessionid == null || sessionid.length() <= 0 || time_to_login) {
                sessionid = null;
                account.setProperty("last_login_at", System.currentTimeMillis());
                SessionData data = api_getsid(account);
                if (data != null) {
                    sessionid = data.getSessionId();
                }
            }

            // finally check session id format
            if (sessionid == null || sessionid.length() <= 0) {
                sessionid = null;
            }
        }

        return sessionid;
    }

    private Map<String, FileData> api_checkLinks(List<String> ids) {
        if (ids == null) return null;
        return api_checkLinks(ids.toArray(new String[ids.size()]));
    }

    private Map<String, FileData> api_checkLinks(String[] ids) {
        if (ids == null) return null;

        // output data. surrounding map selects by id. only found files will be available in the map.
        Map<String, FileData> data = null;

        // create comma separated file ids
        StringBuilder sb = new StringBuilder();
        for (String id : ids)
            sb.append("\"" + id + "\",");
        sb.deleteCharAt(sb.length() - 1);

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"chkr\",\"a\":\"chk\",\"d\":{\"id\":[" + sb.toString() + "]}}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(HTTP_PROTOCOL + API_HOST, query);

            // try to parse api response
            JSonObject json_response = (JSonObject) new JSonFactory(response).parse();
            ApiStatus status = ApiStatus.UNKNOWN;
            try {
                status = ApiStatus.get((Long) ((JSonValue) json_response.get("c")).getValue());

            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            // do, whats to do
            if (status.isSuccess()) {

                // extract list of checked files
                JSonArray json_array = null;
                try {
                    json_array = (JSonArray) json_response.get("d");
                } catch (ClassCastException e) {
                    e.printStackTrace();
                } finally {
                    if (json_array == null) return null;
                }

                // extract values and add to output store for each file
                data = new HashMap<String, FileData>();
                for (JSonNode json_elem : json_array) {
                    try {
                        JSonObject json_data = (JSonObject) json_elem;
                        if (json_data == null) continue;

                        String name = (String) ((JSonValue) json_data.get("name")).getValue();
                        String id = (String) ((JSonValue) json_data.get("id")).getValue();
                        Boolean online = (Boolean) ((JSonValue) json_data.get("online")).getValue();
                        Boolean protect = (Boolean) ((JSonValue) json_data.get("protected")).getValue();
                        Long size = (Long) ((JSonValue) json_data.get("size")).getValue();
                        String md5 = (String) ((JSonValue) json_data.get("md5")).getValue();

                        FileData fd = new FileData(id, online, name, size, md5, protect);
                        System.err.println(id);
                        System.err.println(fd.isOnline());
                        data.put(id, fd);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        continue;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        System.err.println(ids.length);
        System.err.println(data.keySet());

        return data;
    }

    private String computePassword(final String username, final String password) {
        if (username == null || password == null) { return null; }
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update((username + password).getBytes("UTF-8"));
            final byte[] salt = digest.digest();

            final int iterations = 120;
            final int bits = 512;

            byte[] bpass = password.getBytes("UTF-8");
            char[] cpass = new char[bpass.length];
            for (int i = 0; i < cpass.length; i++) {
                cpass[i] = (char) (bpass[i] & 0xFF);
            }

            final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            final KeySpec keySpec = new PBEKeySpec(cpass, salt, iterations, bits);
            final SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            final byte[] key = secretKey.getEncoded();

            result = Base64.encodeToString(key, false);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return result;
    }

    private PrivateDownloadData api_privatedownload(Account account) {
        if (account == null) return null;

        // output data. host, traffic_left
        PrivateDownloadData data = null;

        // client needs to be logged in
        String sessionid = login(account);
        if (sessionid == null) { return null; }

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"prv\",\"a\":\"dl\",\"s\":\"" + sessionid + "\"}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(HTTP_PROTOCOL + API_HOST, query);

            // try to parse api response
            JSonObject json_response = (JSonObject) new JSonFactory(response).parse();
            ApiStatus status = ApiStatus.UNKNOWN;
            try {
                status = ApiStatus.get((Long) ((JSonValue) json_response.get("c")).getValue());
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

            // do, whats to do
            if (status.isSuccess()) {
                try {
                    // extract json data object
                    JSonObject json_data = (JSonObject) json_response.get("d");
                    if (json_data == null) return null;

                    // extract values and add to output store
                    String host = (String) ((JSonValue) json_data.get("h")).getValue();
                    Long traffic_left = (Long) ((JSonValue) json_data.get("l")).getValue();

                    data = new PrivateDownloadData(host, traffic_left);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    return null;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (status.isAuthFailed()) {
                account.setProperty("sessionid", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private SessionData api_getsid(Account account) {
        if (account == null) return null;

        // output data. sessionid, expires_at
        SessionData data = null;
        String sessionid = null;
        Long expires_at = null;

        // compute password hash
        String computed_password = computePassword(account.getUser(), account.getPass());
        if (computed_password == null) return null;

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"auth\",\"a\":\"getsid\",\"d\":{\"u\":\"" + account.getUser() + "\",\"p\":\"" + computed_password + "\"}}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(HTTP_PROTOCOL + API_HOST, query);

            // try to parse api response
            JSonObject json_response = (JSonObject) new JSonFactory(response).parse();
            ApiStatus status = ApiStatus.UNKNOWN;
            try {
                status = ApiStatus.get((Long) ((JSonValue) json_response.get("c")).getValue());
            } catch (NullPointerException e) {
                e.printStackTrace();

            } catch (ClassCastException e) {
                e.printStackTrace();
            }

            // do, whats to do
            if (status.isSuccess()) {
                try {
                    // extract json data object
                    JSonObject json_data = (JSonObject) json_response.get("d");
                    if (json_data == null) return null;

                    // extract session values
                    sessionid = (String) ((JSonValue) json_data.get("s")).getValue();
                    expires_at = (Long) ((JSonValue) json_data.get("e")).getValue();
                } catch (NullPointerException e) {

                    e.printStackTrace();
                    return null;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        } finally {
            // create data container if valid session data
            if (sessionid != null && sessionid.length() > 0 && expires_at != null) {
                data = new SessionData(sessionid, expires_at);
                account.setProperty("sessionid", sessionid);
                account.setProperty("expires_at", expires_at);
            } else {
                account.setProperty("sessionid", "");
                account.setProperty("expires_at", 0L);
            }
        }

        return data;
    }

    private AccountData api_getdetails(Account account) {
        if (account == null) return null;

        // output data. premium_until, traffic_use, traffic_max
        AccountData data = null;

        // client needs to be logged in
        String sessionid = login(account);
        if (sessionid == null) { return null; }

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"acc\",\"a\":\"info\",\"d\":{\"w\":\"jdl\"},\"s\":\"" + sessionid + "\"}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(HTTP_PROTOCOL + API_HOST, query);

            // try to parse api response
            JSonObject json_response = (JSonObject) new JSonFactory(response).parse();
            ApiStatus status = ApiStatus.UNKNOWN;
            try {
                status = ApiStatus.get((Long) ((JSonValue) json_response.get("c")).getValue());
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

            // do, whats to do
            if (status.isSuccess()) {
                try {
                    // extract json data object
                    JSonObject json_data = (JSonObject) json_response.get("d");
                    if (json_data == null) return null;

                    // extract account values
                    Long premium_until = (Long) ((JSonValue) json_data.get("premium_until")).getValue();
                    Long traffic_used = (Long) ((JSonValue) json_data.get("traffic_used")).getValue();
                    Long traffic_max = (Long) ((JSonValue) json_data.get("traffic_max")).getValue();

                    data = new AccountData(traffic_max, traffic_used, premium_until);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    return null;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (status.isAuthFailed()) {
                account.setProperty("sessionid", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private String createStorageUrl(DownloadLink dl_link, String sessionid, String host, String share_password) {
        String url = HTTP_PROTOCOL + (host != null ? host : "") + "/d/?file=" + dl_link.getStringProperty("id", "") + "&sid=" + (sessionid != null ? sessionid : "");
        if (share_password != null && Boolean.TRUE.equals((Boolean) dl_link.getBooleanProperty("protected", false))) url += "&pass=" + share_password;
        return url;
    }

    private String generateRandomId() {
        return (0 + (int) (Math.random() * ((1024 * 1024 * 1024 - 0) + 1))) + "";
    }

    private String getFileId(String link) {
        return new Regex(link, "https?://(?:www\\.)?updown\\.bz/([a-zA-Z0-9]+)$").getMatch(0);
    }

    private void setConfigElements() {
        return;
    }

    private class AccountData {
        private final long traffic_max;
        private final long traffic_used;
        private final long premium_until;

        public AccountData(Long traffic_max, Long traffic_used, Long premium_until) {
            this.traffic_max = traffic_max != null ? traffic_max : 0L;
            this.traffic_used = traffic_used != null ? traffic_used : 0L;
            this.premium_until = premium_until != null ? premium_until : 0L;
        }

        public long getPremiumUntil() {
            return premium_until;
        }

        public long getTrafficMax() {
            return traffic_max;
        }

        public long getTrafficUsed() {
            return traffic_used;
        }
    }

    private class PrivateDownloadData {
        private final String host;
        private final long   traffic_left;

        public PrivateDownloadData(String host, Long traffic_left) {
            this.host = host != null ? host : "";
            this.traffic_left = traffic_left != null ? traffic_left : 0L;
        }

        public String getHost() {
            return host;
        }

        public long getTrafficLeft() {
            return traffic_left;
        }
    }

    private class SessionData {
        private final String sessionid;
        private final long   expires_at;

        public SessionData(String sessionid, Long expires_at) {
            this.sessionid = sessionid != null ? sessionid : "";
            this.expires_at = expires_at != null ? expires_at : 0L;
        }

        public String getSessionId() {
            return sessionid;
        }

        public long getExpiresAt() {
            return expires_at;
        }

    }

    private class FileData {
        private final String  id;
        private final boolean online;
        private final String  name;
        private final long    size;
        private final String  md5;
        private final boolean protect;

        public FileData(String id, Boolean online, String name, Long size, String md5, Boolean protect) {
            this.id = id != null ? id : "";
            this.online = Boolean.TRUE.equals(online);
            this.name = name != null ? name : "";
            this.size = size != null ? size : 0L;
            this.md5 = md5 != null && md5.length() == 32 ? md5 : "";
            this.protect = Boolean.TRUE.equals(protect);
        }

        public String getId() {
            return id;
        }

        public boolean isOnline() {
            return online;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public String getMd5() {
            return md5;
        }

        public boolean isProtected() {
            return protect;
        }
    }

    private enum ApiStatus {
        SUCCESS(1),
        UNKNOWN(0),
        ERROR(-1),
        API_ERROR(-2),
        MISSING_ARGUMENT(-3),
        INVALID_ARGUMENT(-4),
        MISSING_OR_INVALID_ARGUMENT(-5),
        AUTH_FAILED(-6),
        NO_PREMIUM(-7);

        private final int code;

        private ApiStatus(int code) {
            this.code = code;
        }

        public static ApiStatus get(Long code) {
            for (ApiStatus status : ApiStatus.values()) {
                if (status.code == code) { return status; }
            }
            return ApiStatus.UNKNOWN;
        }

        public int getCode() {
            return code;
        }

        public boolean isSuccess() {
            return code >= ApiStatus.SUCCESS.code;
        }

        public boolean isError() {
            return code <= ApiStatus.UNKNOWN.code;
        }

        public boolean isUnknown() {
            return this == ApiStatus.UNKNOWN;
        }

        public boolean isAuthFailed() {
            return this == ApiStatus.AUTH_FAILED;
        }

        public boolean isNoPremium() {
            return this == ApiStatus.NO_PREMIUM;
        }
    }
}
