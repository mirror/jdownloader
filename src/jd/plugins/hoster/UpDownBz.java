package jd.plugins.hoster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "updown.bz" }, urls = { "https?://(?:www\\.)?updown\\.bz/[a-zA-Z0-9]+" }, flags = { 0 })
public class UpDownBz extends PluginForHost {

    /*
     * hoster will be launched soon. https://updown.bz
     */

    private static final boolean FORCE_SSL = true; // ssl only

    public UpDownBz(PluginWrapper wrapper) throws IOException {
        super(wrapper);
        setConfigElements();
        this.enablePremium(getHttpProtocol() + getWebHost() + "/premium");
    }

    @Override
    public String getAGBLink() {
        return getHttpProtocol() + getWebHost() + "/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        String protocol = getHttpProtocol();
        String id = getFileId(link.getDownloadURL());
        link.setUrlDownload(protocol + "updown.bz/" + id);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        account.setProperty("sessionid", null);
        account.setProperty("expires_at", null);
        ai.setStatus("Free");
        account.setValid(false);

        // login to service
        HashMap<String, String> getsid_data = api_getsid(account.getUser(), account.getPass());
        if (getsid_data == null) { return ai; }

        // receive userdata
        HashMap<String, Object> data = api_getdetails(getsid_data.get("sessionid"));
        if (data == null) { return ai; }

        // account is available and ok
        account.setValid(true);

        // store login data globally
        account.setProperty("sessionid", getsid_data.get("sessionid"));
        account.setProperty("expires_at", getsid_data.get("expires_at"));

        // eval traffic data
        if ((Long) data.get("traffic_max") == -1)
            ai.setUnlimitedTraffic();
        else {
            ai.setTrafficLeft((Long) data.get("traffic_max") - (Long) data.get("traffic_used"));
            ai.setTrafficMax((Long) data.get("traffic_max"));
        }

        // set premium state data
        Long premium_until = (Long) data.get("premium_until") * 1000L;
        ai.setValidUntil(premium_until);
        ai.setStatus("Premium");
        if (premium_until > System.currentTimeMillis()) {
            ai.setExpired(false);
        } else {
            ai.setExpired(true);
        }

        return ai;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        // TODO process a specified number of links at once
        if (urls == null || urls.length == 0) { return false; }

        // extract file ids to check
        String[] ids = new String[urls.length];
        int i = 0;
        for (DownloadLink url : urls)
            ids[i++] = getFileId(url.getDownloadURL());

        // check given ids
        HashMap<String, HashMap<String, Object>> filedata = api_checklinks(ids);
        if (filedata == null) return false;

        // update download links
        int j = 0;
        Boolean check_result = true;
        for (DownloadLink url : urls) {
            String id = ids[j++];
            if (filedata.containsKey(id)) {
                Boolean online = (Boolean) filedata.get(id).get("online");
                url.setAvailable(online);
                url.setAvailableStatus(AvailableStatus.TRUE);
                url.setFinalFileName((String) filedata.get(id).get("name"));
                url.setDownloadSize((Long) filedata.get(id).get("size"));
                url.setProperty("VERIFIEDFILESIZE", (Long) filedata.get(id).get("size"));
                url.setMD5Hash((String) filedata.get(id).get("md5"));
                url.setProperty("id", id);
                url.setProperty("protected", (Boolean) filedata.get(id).get("protected"));
                check_result &= online;
            } else {
                url.setAvailable(false);
                url.setAvailableStatus(AvailableStatus.FALSE);
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
        // TODO share password support
        // TODO reuse session data of fetched login data if any

        // set share password if file is protected
        String share_password = null;
        boolean is_protected = downloadLink.getBooleanProperty("protected");
        if (is_protected) {
            String dl_pass = downloadLink.getDownloadPassword();
            if (dl_pass != null) {
                if (dl_pass.length() > 0) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] share_pass_bytes = md.digest((dl_pass).getBytes());
                    share_password = HexFormatter.byteArrayToHex(share_pass_bytes);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "no download password set");
                }
            }
        }

        // login for premium download
        HashMap<String, String> getsid_data = api_getsid(account.getUser(), account.getPass());
        if (getsid_data == null) throw new PluginException(LinkStatus.ERROR_RETRY, "login failed", 60 * 1000);

        // request download
        HashMap<String, String> prvdl_data = api_privatedownload(getsid_data.get("sessionid"));
        if (prvdl_data == null) throw new PluginException(LinkStatus.ERROR_RETRY, "requesting download failed", 60 * 1000);

        // TODO: verfiy download file

        // is enough traffic available
        if (downloadLink.getDownloadSize() > Long.parseLong(prvdl_data.get("traffic_left"))) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "traffic limit reached");

        // construct the download url relating to the login data
        String url = createStorageUrl(downloadLink, getsid_data.get("sessionid"), prvdl_data.get("host"), share_password);

        // try to open download connection and check its response
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, (-1) * 2);

        // file is not available
        if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "requested file is not available");

        // your are not authorized
        if (dl.getConnection().getResponseCode() == 401) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError(508)", 30 * 60 * 1000l);
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "invalid login data" + (is_protected ?
        // " or wrong link password" : ""), 6 * 60 * 1000L);

        // wrong request format
        if (dl.getConnection().getResponseCode() == 400) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "wrong requet format"); }

        // wrong request method
        if (dl.getConnection().getResponseCode() == 405) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "wrong request method"); }

        // no connections available
        if (dl.getConnection().getResponseCode() == 421) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "connection limit reached", 1 * 60 * 1000L); }

        // is data to download available
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no data to download found");

        // nothing wrong, thus start the download
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        return;
    }

    @Override
    public void reset() {
        return;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        return;
    }

    private HashMap<String, HashMap<String, Object>> api_checklinks(String[] ids) {
        // output data. surrounding hashmap selects by id. inner map is the data
        // per file. only found files will be available.
        HashMap<String, HashMap<String, Object>> data = null;

        // create comma seperated file ids
        StringBuilder sb = new StringBuilder();
        for (String id : ids)
            sb.append("\"" + id + "\",");
        sb.deleteCharAt(sb.length() - 1);

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"chkr\",\"a\":\"chk\",\"d\":{\"id\":[" + sb.toString() + "]}}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(getHttpProtocol() + getApiHost(), query);

            // try to parse json response
            JSonFactory json_factory = new JSonFactory(response);
            JSonObject json_response = (JSonObject) json_factory.parse();
            Long code = (Long) (((JSonValue) json_response.get("c")).getValue());
            if (code != null && code > 0) {
                data = new HashMap<String, HashMap<String, Object>>();
                if (json_response.containsKey("d")) {
                    JSonArray json_data_array = (JSonArray) json_response.get("d");
                    for (JSonNode json_data_tmp : json_data_array) {
                        JSonObject json_data = (JSonObject) json_data_tmp;

                        // extract values
                        String name = (String) ((JSonValue) json_data.get("name")).getValue();
                        String id = (String) ((JSonValue) json_data.get("id")).getValue();
                        Boolean online = (Boolean) ((JSonValue) json_data.get("online")).getValue();
                        Boolean protect = (Boolean) ((JSonValue) json_data.get("protected")).getValue();
                        Long size = (Long) ((JSonValue) json_data.get("size")).getValue();
                        String md5 = (String) ((JSonValue) json_data.get("md5")).getValue();

                        // add data to output store entry
                        HashMap<String, Object> fileinfo = new HashMap<String, Object>();
                        fileinfo.put("online", online);
                        fileinfo.put("name", name);
                        fileinfo.put("size", size);
                        fileinfo.put("md5", md5);
                        fileinfo.put("protected", protect);

                        // add values to output store
                        data.put(id, fileinfo);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private String computePassword(final String username, final String password) {
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

    private HashMap<String, String> api_privatedownload(String sessionid) {
        // output data. host, traffic_left
        HashMap<String, String> data = null;

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"prv\",\"a\":\"dl\",\"s\":\"" + sessionid + "\"}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(getHttpProtocol() + getApiHost(), query);

            // try to parse json response
            JSonFactory json_factory = new JSonFactory(response);
            JSonObject json_response = (JSonObject) json_factory.parse();
            Long code = (Long) (((JSonValue) json_response.get("c")).getValue());
            if (code != null && code > 0) {
                data = new HashMap<String, String>();
                if (json_response.containsKey("d")) {
                    JSonObject json_data = (JSonObject) json_response.get("d");

                    // extract the actual data values
                    String host = (String) ((JSonValue) json_data.get("h")).getValue();
                    Long traffic_left = (Long) ((JSonValue) json_data.get("l")).getValue();

                    // add values to output store
                    data.put("host", host);
                    data.put("traffic_left", traffic_left.toString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private HashMap<String, String> api_getsid(String username, String password) {
        // output data. sessionid, expires_at
        HashMap<String, String> data = null;

        // compute password hash
        String computed_password = computePassword(username, password);
        if (computed_password == null) return null;

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"auth\",\"a\":\"getsid\",\"d\":{\"u\":\"" + username + "\",\"p\":\"" + computed_password + "\"}}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(getHttpProtocol() + getApiHost(), query);

            // try to parse json response
            JSonFactory json_factory = new JSonFactory(response);
            JSonObject json_response = (JSonObject) json_factory.parse();
            Long code = (Long) (((JSonValue) json_response.get("c")).getValue());
            if (code != null && code > 0) {
                data = new HashMap<String, String>();
                if (json_response.containsKey("d")) {
                    JSonObject json_data = (JSonObject) json_response.get("d");

                    // extract the actual data values
                    String sessionid = (String) ((JSonValue) json_data.get("s")).getValue();
                    Long expires_at = (Long) ((JSonValue) json_data.get("e")).getValue();

                    // add values to output
                    data.put("sessionid", sessionid);
                    data.put("expires_at", expires_at.toString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private HashMap<String, Object> api_getdetails(String sessionid) {
        // output data. premium_until, traffic_use, traffic_max
        HashMap<String, Object> data = null;

        // build json api query
        String query = "{\"i\":\"" + generateRandomId() + "\",\"m\":\"acc\",\"a\":\"info\",\"d\":{\"w\":\"jdl\"},\"s\":\"" + sessionid + "\"}";

        try {
            // do the actual api call
            br.setHeader("Content-Type", "application/json");
            String response = br.postPageRaw(getHttpProtocol() + getApiHost(), query);

            // try to parse json response
            JSonFactory json_factory = new JSonFactory(response);
            JSonObject json_response = (JSonObject) json_factory.parse();
            Long code = (Long) (((JSonValue) json_response.get("c")).getValue());
            if (code != null && code > 0) {
                if (json_response.containsKey("d")) {
                    JSonObject json_data = (JSonObject) json_response.get("d");

                    // extract the actual data values
                    Long premium_until = (Long) ((JSonValue) json_data.get("premium_until")).getValue();
                    Long traffic_used = (Long) ((JSonValue) json_data.get("traffic_used")).getValue();
                    Long traffic_max = (Long) ((JSonValue) json_data.get("traffic_max")).getValue();

                    // add values to output store
                    data = new HashMap<String, Object>();
                    data.put("premium_until", premium_until);
                    data.put("traffic_used", traffic_used);
                    data.put("traffic_max", traffic_max);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return data;
    }

    private String createStorageUrl(DownloadLink dl_link, String sessionid, String host, String share_password) {
        String url = getHttpProtocol() + host + "/d/?file=" + dl_link.getProperty("id") + "&sid=" + sessionid;
        if (share_password != null && Boolean.TRUE.equals((Boolean) dl_link.getProperty("protected"))) url += "&pass=" + share_password;
        return url;
    }

    private String generateRandomId() {
        return (0 + (int) (Math.random() * ((1024 * 1024 * 1024 - 0) + 1))) + "";
    }

    private String getFileId(String link) {
        String id = new Regex(link, "https?://(?:.+\\.)?updown\\.bz/([a-zA-Z0-9]+)$").getMatch(0);
        return id;
    }

    private String getHttpProtocol() {
        if (FORCE_SSL)
            return "https://";
        else
            return "http://";
    }

    private String getApiHost() {
        return "api.updown.bz";
    }

    private String getWebHost() {
        return "updown.bz";
    }

    private void setConfigElements() {
        return;
    }
}