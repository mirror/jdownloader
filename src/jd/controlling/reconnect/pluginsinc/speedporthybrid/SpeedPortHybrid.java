package jd.controlling.reconnect.pluginsinc.speedporthybrid;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.SwingUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.proxy.NoProxySelector;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPException;
import jd.http.Browser;
import net.miginfocom.swing.MigLayout;

/**
 * Plugin to use an extern tool for reconnection
 */
public class SpeedPortHybrid extends RouterPlugin implements IPCheckProvider {

    public static final String             ID = "SpeedPortHybrid";

    private Icon                           icon;

    private ReconnectInvoker               invoker;

    private ExtPasswordField               txtPassword;

    private SpeedPortHybridReconnectConfig config;

    private ExtTextField                   txtIP;

    private Browser                        br;

    private String                         derivedk;

    private String                         csrf;

    private String                         challengev;

    public static void main(String[] args) throws StorageException, IOException {
        File file = new File("C:\\Users\\Thomas\\Desktop\\interfaces.json");
        Application.setApplication(".appwork");
        JSonStorage.setMapper(new JacksonMapper());
        // 'IPv4_address':'87.162.215.207',
        String[] lte_tunnel = new Regex(IO.readFileToString(file), "\\'IPv4_address\\'\\s*\\:\\s*\\'([^']*)").getColumn(0);
        System.out.println(lte_tunnel);
    }

    private String PBKDF2Key(String password, String salt) throws Exception {
        final PBEKeySpec spec = new PBEKeySpec(Hash.getSHA256(password).toCharArray(), salt.getBytes("UTF-8"), 1000, 16 * 8);
        final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }

    // public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException,
    // InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException,
    // IllegalStateException, InvalidCipherTextException, SessionInvalidException {
    // try {
    // LoggerInitUtils.disableFileOut();
    // LoggerInitUtils.disableConsoleOut();
    // Application.setApplication(".appwork");
    //
    // String result =
    // "\"varid\":\"always_online\",\r\n \"varvalue\":\"1\"\r\n },\r\n {\r\n \"vartype\":\"value\",\r\n \"varid\":\"public_ip_v4\",\r\n
    // \"varvalue\":\"217.241.66.150\"\r\n },\r\n {\r\n \"vartype\":\"value\",\r\n
    // \"varid\":\"gateway_ip_v4\",\r\n\"varvalue\":\"217.241.0.1\"\r\n}";
    // String ip = new SpeedPortHybrid().extractVariable(result, "public_ip_v4");
    // System.out.println("Parser: " + ("217.241.66.150".equals(ip)));
    // encryptTest();
    //
    // decrpyttest();
    //
    // } catch (Throwable e) {
    // e.printStackTrace();
    // }
    // }
    //
    // private static void encryptTest() throws UnsupportedEncodingException, InvalidCipherTextException {
    //
    // SpeedPortHybrid inst = new SpeedPortHybrid();
    // inst.challengev = "c5D5f37d0EC075dd1793D1eeB01fEc05eA2e4a66BF78908acDDA";
    // inst.derivedk = "b5b7e46f30820543f448eece4d20da1a";
    // String result = inst.encrypt("req_connect=disabled&csrf_token=5TvISVht8aUo%2BZXeJv3qjXi818GAWpv");
    //
    // System.out.println("ENcrypting Works: " +
    // "77e8cfe400a2e4896ee25d2f3177a474b43e67d55268662ffa8ec2c5225934f8a6df9b11641f0c0a8aec76509a7409c6fff6ff6369a60ae302eec9814f66de92dad0bc5eec71b40f8f".equals(result));
    // }
    //
    // private static void decrpyttest() throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException,
    // SessionInvalidException {
    // // decrypt
    // SpeedPortHybrid inst = new SpeedPortHybrid();
    // inst.challengev = "CD39f2B7e910aFe9FFA1A2F18DFFD28fB373dbAaaFa444DaBA3A";
    // inst.derivedk = "595578fe25bca7f227bb2c84f3293fb6";
    // String result = inst
    // .decryptAndHandle("47E742A51B14BA6A7DE0772B12C5ED2CBA6C82E3A68FE1B77777D7A2B1437592051DE2D1E53F3A79E8F00667AAB8C6E1CDF2242127C9D4210735997247DC6009FEA4D87F79161C69309585B80CEDED46B80B57A0DCF27036B3F38982AB1D4B8052841598741E494B412FF50098A22161CF3AA3E2BC56E49CDC7135D1E9276DB299EE9BE9BDE205C777FDA6C7EAF7652C6FDADA55892AA088FB017313AA6F1A9707530826389B41F5F50ED92D767423C1A7DDD88434FF372E20FF49B755359359B570CD0753FCB0CEA1B496308B9D49A442BF08A7949BE75B1AB188552B8FC46ADFDDBED4A9D0F9F12902CE9BD9BADA6448654296D65DB691562370BB979F3E844E2F1F136A4AA0CE9924CED5B9C8E25EFB85FE2ED41D275DE6418BDB747D8256AEE3642073958387B6EFB038E9EE994A58085560DE4353DF9C9406A89739CCF58462A96E627BF73C9AED3776873A2D970EF938B1CFDC9195F0C1FC8EE033E47351A6D543F0CFE89F9AD8211BA45811F03CFAB932DEA540C7C06DC92EE7BE317A7647A35904151F4B5DD86FB10F6822E0B696A823BB1B91E9AC201E78C5E2CEB35FF31AB1B8E0C0B0459E5DA85E9DB79EF4E5CD77640111249497BE11A7F2AAA410F053D0F2CEBBBA2FBF31512EE7195D081919FDBC260436A9E7241E40D41589A25FB91F214C36DC00DFF14EE012BA0245");
    //
    // System.out.println("Decryption Works: " + (result.contains("onlinestatus") && result.length() == 489));
    // }

    public String encrypt(String pt) throws UnsupportedEncodingException, IllegalStateException, InvalidCipherTextException {
        Log.info("Encrypt " + pt);
        byte[] iv = HexFormatter.hexToByteArray(challengev.substring(16, 32));
        byte[] adata = HexFormatter.hexToByteArray(challengev.substring(32, 32 + 16));
        CCMBlockCipher chipher = new CCMBlockCipher(new AESFastEngine());
        AEADParameters params = new AEADParameters(new KeyParameter(HexFormatter.hexToByteArray(derivedk)), 64, iv);
        chipher.init(true, params);
        byte[] enc = pt.getBytes("UTF-8");
        byte[] tmp = new byte[enc.length + adata.length];
        chipher.processAADBytes(adata, 0, adata.length);
        int len = chipher.processBytes(enc, 0, enc.length, tmp, 0);
        len += chipher.doFinal(tmp, len);
        return HexFormatter.byteArrayToHex(tmp);
    }

    public String decrypt(String hex) throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException {
        if (hex == null) {
            return null;
        }
        String ret;
        if (!hex.trim().matches("[a-fA-f0-9]+")) {
            // unencrypted answers
            ret = hex;
        } else {
            String ivs;
            byte[] iv = HexFormatter.hexToByteArray(challengev.substring(16, 16 + 16));
            String ads;
            byte[] adata = HexFormatter.hexToByteArray(ads = challengev.substring(32, 32 + 16));

            AEADParameters params = new AEADParameters(new KeyParameter(HexFormatter.hexToByteArray(derivedk)), 64, iv);
            CCMBlockCipher dc = new CCMBlockCipher(new AESFastEngine());
            dc.init(false, params);

            byte[] enc = HexFormatter.hexToByteArray(hex);
            byte[] tmp = new byte[enc.length + adata.length];
            dc.processAADBytes(adata, 0, adata.length);
            int len = dc.processBytes(enc, 0, enc.length, tmp, 0);
            len += dc.doFinal(tmp, len);
            ret = new String(tmp, 0, len, "UTF-8");
            Log.info(ret);
        }

        return ret;
    }

    // private String getTimeParams() {
    // return "_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000));
    // }

    // @Override
    public int getIpCheckInterval() {
        return 1000;
    }

    // @Override
    public IP getExternalIP() throws IPCheckException {
        synchronized (SpeedPortHybrid.this) {
            try {
                try {
                    return getExternalIPOnce();
                } catch (SessionInvalidException e) {
                    Log.info("Try again ip check");
                    // try again. session might be invalid
                    return getExternalIPOnce();
                }
            } catch (Throwable e) {
                Log.log(e);
            }
            throw new InvalidIPException("Unknown");
        }
    }

    private IP getExternalIPOnce() throws Exception, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException, IOException, IPCheckException {
        ensureSession();
        updateBonding();

        Log.info("IP: " + ipv4);
        if (ipv4 != null) {
            return IP.getInstance(ipv4);
        } else {
            throw new IPCheckException("Offline");
        }
    }

    private String extractVariable(String crypted, String key) {
        return new Regex(crypted, "\"varid\"\\s*:\\s*\"" + key + "\",\\s*\"varvalue\"\\s*:\\s*\"([^\"]+)").getMatch(0);
    }

    private String lte_tunnel;
    private String dsl_tunnel;
    private String bonding;
    private String ipv4;
    // private String ipv4Dsl;

    public SpeedPortHybrid() {
        super();
        config = JsonConfig.create(SpeedPortHybridReconnectConfig.class);
        icon = new AbstractIcon(IconKey.ICON_RECONNECT, 16);
        // setIPCheckProvider(this);
        invoker = new ReconnectInvoker(this) {

            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
            }

            @Override
            public void run() throws ReconnectException {
                synchronized (SpeedPortHybrid.this) {
                    try {
                        // for (int i = 0; i < 5; i++) {
                        try {
                            runOnce();
                        } catch (SessionInvalidException e) {
                            Log.info("Try again");
                            runOnce();
                        }
                        Log.info("dsl_tunnel " + dsl_tunnel);
                        Log.info("lte_tunnel " + lte_tunnel);
                        Log.info("bonding " + bonding);
                        // if (StringUtils.equalsIgnoreCase("Up", dsl_tunnel) && StringUtils.equalsIgnoreCase("Up", lte_tunnel) &&
                        // StringUtils.equalsIgnoreCase("Up", bonding)) {
                        // Log.info("Done");
                        // break;
                        // } else {
                        // Log.info("retry. any tunnel down");
                        // }
                        // }
                    } catch (Throwable e) {
                        throw new ReconnectException(e);
                    }
                }
            }

            private void runOnce() throws Exception, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException, IOException, InterruptedException {
                // if (System.getProperty("") != null) {
                ensureSession();
                loadFrame("/html/content/config/problem_handling.html?lang=de");
                getXHR("/data/Reboot.json", 2);
                Thread.sleep(2000);
                postXHR("/data/Reboot.json", "reboot_device=true&csrf_token=" + csrf, true);

                Thread.sleep(60000);
                // }else{
                // for (int i = 0; i < 5; i++) {
                //
                // ensureSession();
                //
                //
                //
                // // http://" + config.getRouterIP() + "/html/content/internet/connection.html?lang=de
                // setStatusString("Update Bonding");
                // updateBonding();
                // String ipBefore = ipv4;
                // if (!StringUtils.equalsIgnoreCase("Up", dsl_tunnel) || !StringUtils.equalsIgnoreCase("Up", bonding)) {
                // setStatusString("Connect");
                // // "req_connect=online&csrf_token=j2WMeJ%2BjJv4WlwLdwKuWoWxpNY6JXyC"
                // logger.info("We are not online. Estabilish Connection");
                // estabilishConnection();
                // }
                // Thread.sleep(2000);
                // setStatusString("req_connect=disabled");
                // postXHR("/data/Connect.json?lang=de", "req_connect=disabled&csrf_token=" + csrf, true);
                // setStatusString("lte_reconn=1");
                // // loadFrame("/html/content/internet/connection.html?lang=de");
                // postXHR("/data/modules.json?lang=de", "lte_reconn=1&csrf_token=" + csrf, true);
                // setStatusString("Wait 10 Secs");
                // logger.info("Wait 10000");
                // Thread.sleep(10000);
                // setStatusString("Connect");
                // estabilishConnection();
                //
                // if (!StringUtils.equals(ipBefore, ipv4)) {
                // logger.info("Done");
                // break;
                // } else {
                // br = null;
                // logger.info("IP Did not change. retry");
                // }
                // }
                // }
            }

            private void estabilishConnection() throws Exception {
                logger.info("Estabilish Connection");
                for (int i = 0; i < 5; i++) {
                    logger.info("Estabilish Connection Try " + i);
                    postXHR("/data/Connect.json?lang=de", "req_connect=online&csrf_token=" + csrf, true);
                    Thread.sleep(10000);
                    long started = System.currentTimeMillis();
                    while (System.currentTimeMillis() - started < 1 * 60 * 1000l) {
                        logger.info("Wait for onlinestatus==online");

                        String crypted = getXHR("/data/Connect.json", 1);
                        String onlineStatus = extractVariable(crypted, "onlinestatus");

                        if ("online".equalsIgnoreCase(onlineStatus)) {
                            setStatusString("Connect #" + i + " os " + onlineStatus + " bonding_ok");
                            logger.info("We are online");
                            updateBonding();
                            if (StringUtils.equalsIgnoreCase("Up", dsl_tunnel) && StringUtils.equalsIgnoreCase("Up", bonding)) {

                                return;
                            } else {
                                logger.info("Wait for Bonding");
                            }
                        } else {
                            setStatusString("Connect #" + i + " os " + onlineStatus);
                            logger.info("Not online yet");
                        }
                        Thread.sleep(6000);
                        continue;
                    }
                }
            }

        };
    }

    protected String getXHR(String rel, int timeext) throws Exception {
        for (int ii = 0; ii < 2; ii++) {
            ensureSession();
            String url = createAbsoluteUrl(rel, timeext);
            Browser clone = br.cloneBrowser();
            String json = decrypt(clone.getPage(url));

            String newCsrf = extractVariable(json, "csrf_token");
            if (StringUtils.isNotEmpty(newCsrf) && !StringUtils.equals(csrf, newCsrf)) {
                Log.info("New CSRF: " + newCsrf);
                csrf = newCsrf;
            }
            try {
                checkXHRError(clone, json);
            } catch (SessionInvalidException e) {

                if (ii > 0) {
                    throw e;
                } else {
                    continue;
                }
            }
            return json;
        }
        throw new WTFException();
    }

    private String createAbsoluteUrl(String rel, int timeext) {
        String url = "http://" + config.getRouterIP() + rel;

        for (int i = 0; i < timeext; i++) {
            if (i > 0) {
                if (timeext > 0) {
                    url += "&";
                }
            } else {
                if (timeext > 0) {
                    url += "?";
                }
            }
            url += "_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000));

        }
        return url;
    }

    private void checkXHRError(Browser clone, String json) throws SessionInvalidException {
        if (clone.getRequest().getHttpConnection().getResponseCode() == 302) {
            br = null;
            throw new SessionInvalidException("Invalid ResponseCode");
        }
        String loginstate = extractVariable(json, "loginstate");
        if (loginstate != null && !"1".equals(loginstate)) {
            br = null;
            throw new SessionInvalidException("LoginState=" + loginstate);
        }
    }

    private String postXHR(String rel, String postData, boolean encrypt) throws Exception {

        for (int ii = 0; ii < 2; ii++) {
            ensureSession();
            String url = "http://" + config.getRouterIP() + rel;

            Browser clone = br.cloneBrowser();
            String json = decrypt(clone.postPageRaw(url, encrypt ? encrypt(postData) : postData));

            String newCsrf = extractVariable(json, "csrf_token");
            if (StringUtils.isNotEmpty(newCsrf) && !StringUtils.equals(csrf, newCsrf)) {
                Log.info("New CSRF: " + newCsrf);
                csrf = newCsrf;
            }

            try {
                checkXHRError(clone, json);
            } catch (SessionInvalidException e) {

                if (ii > 0) {
                    throw e;
                } else {
                    continue;
                }
            }

            String status = extractVariable(json, "status");
            if ("fail".equals(status) && ii == 0) {
                br = null;
                continue;
            }
            return json;
        }
        throw new WTFException();

    }

    private void updateBonding() throws Exception {
        Log.info("bonding_tunnel.json");

        String json = getXHR("/data/bonding_tunnel.json", 0);
        // Log.info(br + "");
        lte_tunnel = new Regex(json, "\\'lte_tunnel\\'\\s*\\:\\s*\\'([^']*)").getMatch(0);
        dsl_tunnel = new Regex(json, "\\'dsl_tunnel\\'\\s*\\:\\s*\\'([^']*)").getMatch(0);
        bonding = new Regex(json, "\\'bonding\\'\\s*\\:\\s*\\'([^']*)").getMatch(0);
        ipv4 = new Regex(json, "\\'ipv4\\'\\s*\\:\\s*\\'([^']*)").getMatch(0);

        Log.info("Public IP: " + ipv4);
        Log.info("LTE: " + lte_tunnel);
        Log.info("DSL: " + dsl_tunnel);
        Log.info("Bonding: " + bonding);

    }

    protected synchronized void ensureSession() throws Exception {
        if (isLoggedIn()) {

            return;
        }

        try {
            br = new Browser();

            br.setVerbose(true);
            br.setDebug(true);
            br.setProxySelector(new NoProxySelector());
            if (System.getProperty("fiddler") != null) {
                br.setProxy(new HTTPProxy(HTTPProxy.TYPE.HTTP, "localhost", 8888));
            }
            // logout: logout=byby&csrf_token=eZQe%2B3%2F%2BRBOPOAHa5x4fEtlnevO9PMp
            String json = null;
            for (int i = 5; i > 0; i--) {
                UrlQuery query = new UrlQuery().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("challengev", "null", true);

                json = br.postPage(createAbsoluteUrl("/data/Login.json?lang=de", 0), query);

                if ("2".equals(extractVariable(json, "loginstate"))) {

                    br.getPage(createAbsoluteUrl("/data/Login.json", 2));
                    json = br.postPage(createAbsoluteUrl("/data/Login.json?lang=de", 0), query);

                }
                challengev = extractVariable(json, "challengev");
                if (StringUtils.isEmpty(challengev)) {
                    Thread.sleep(15000);
                    continue;
                } else {
                    break;
                }
            }
            if (StringUtils.isEmpty(challengev)) {
                UIOManager.I().showErrorMessage("Login to Speedport Failed (Challenge Missing)!");
                br = null;
                throw new SessionInvalidException("Login Failed (Challenge Missing)");
            }
            // br.setCookie("http://" + config.getRouterIP(), "challengev", challengev);
            Log.info("Challenge: " + challengev);
            long start = System.currentTimeMillis();
            do {

                json = br.postPage(createAbsoluteUrl("/data/Login.json?lang=de", 0), new UrlQuery().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("password", Hash.getSHA256(challengev + ":" + config.getPassword()), true));

                Thread.sleep(15000);
            } while ("69".equals(extractVariable(json, "login")) && (System.currentTimeMillis() - start) < 2 * 60 * 1000l);
            String session = br.getCookie("http://" + config.getRouterIP(), "SessionID_R3");
            if (StringUtils.isEmpty(session)) {
                UIOManager.I().showErrorMessage("Login to Speedport Failed!");
                br = null;
                throw new SessionInvalidException("Login Failed");
            }
            br.setCookie("http://" + config.getRouterIP(), "derivedk", derivedk = PBKDF2Key(config.getPassword(), challengev.substring(0, 16)));
            br.setCookie("http://" + config.getRouterIP(), "challengev", challengev);

            loadFrame("/html/content/internet/connection.html?lang=de");
        } finally {

        }
    }

    private void loadFrame(String string) throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException, IOException {
        br.setVerbose(false);
        br.setDebug(false);
        br.getPage("http://" + config.getRouterIP() + string);
        String newcsrf = br.getRegex("csrf_token\\s*=\\s*\"([^\"]+)").getMatch(0);
        if (StringUtils.isNotEmpty(newcsrf) && !StringUtils.equals(csrf, newcsrf)) {
            Log.info("New CSRF: " + newcsrf);
            csrf = newcsrf;
        } else {
            // Log.info("No new CSRF");
        }
        br.setVerbose(true);
        br.setDebug(true);
        Log.info(br.getRequest().getHttpConnection() + "");
    }

    private boolean isLoggedIn() {
        return br != null && br.getCookie("http://" + config.getRouterIP(), "SessionID_R3") != null;
    }

    @Override
    public Icon getIcon16() {
        return icon;
    }

    @Override
    public JComponent getGUI() {

        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "[][grow,fill]", "[][][grow,fill][]"));
        p.setOpaque(false);

        txtPassword = new ExtPasswordField() {
            public void onChanged() {
                config.setPassword(txtPassword.getText());
            };
        };

        txtIP = new ExtTextField() {
            @Override
            public void onChanged() {
                config.setRouterIP(txtIP.getText());
            }
        };
        txtPassword.setText(config.getPassword());
        txtIP.setText(config.getRouterIP());
        p.add(label(_GUI.T.lit_router_ip()));
        p.add(txtIP);
        p.add(label(_GUI.T.lit_password()));
        p.add(txtPassword);
        return p;
    }

    private Component label(String string) {
        JLabel ret = new JLabel(string);
        SwingUtils.toBold(ret);
        ret.setEnabled(false);
        ret.setHorizontalAlignment(JLabel.RIGHT);
        return ret;
    }

    @Override
    public String getID() {
        return "SpeedPortHybrid";
    }

    @Override
    public String getName() {
        return "Speed Port Hybrid Reconnect";
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return invoker;
    }

}