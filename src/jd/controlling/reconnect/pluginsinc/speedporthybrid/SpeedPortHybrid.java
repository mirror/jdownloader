package jd.controlling.reconnect.pluginsinc.speedporthybrid;

import java.awt.Component;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LoggerInitUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.swing.SwingUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPException;
import jd.http.Browser;
import jd.http.QueryInfo;
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

    private String                         onlineStatus;

    private String                         session;

    private String                         externalIP;

    private String PBKDF2Key(String password, String salt) throws Exception {

        final PBEKeySpec spec = new PBEKeySpec(Hash.getSHA256(password).toCharArray(), salt.getBytes("UTF-8"), 1000, 16 * 8);
        final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, IllegalStateException, InvalidCipherTextException, SessionInvalidException {
        try {
            LoggerInitUtils.disableFileOut();
            LoggerInitUtils.disableConsoleOut();
            Application.setApplication(".appwork");

            String result = "\"varid\":\"always_online\",\r\n  \"varvalue\":\"1\"\r\n },\r\n {\r\n  \"vartype\":\"value\",\r\n  \"varid\":\"public_ip_v4\",\r\n  \"varvalue\":\"217.241.66.150\"\r\n  },\r\n  {\r\n  \"vartype\":\"value\",\r\n \"varid\":\"gateway_ip_v4\",\r\n\"varvalue\":\"217.241.0.1\"\r\n}";
            String ip = new SpeedPortHybrid().extractVariable(result, "public_ip_v4");
            System.out.println("Parser: " + ("217.241.66.150".equals(ip)));
            encryptTest();

            decrpyttest();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void encryptTest() throws UnsupportedEncodingException, InvalidCipherTextException {

        SpeedPortHybrid inst = new SpeedPortHybrid();
        inst.challengev = "c5D5f37d0EC075dd1793D1eeB01fEc05eA2e4a66BF78908acDDA";
        inst.derivedk = "b5b7e46f30820543f448eece4d20da1a";
        String result = inst.encrypt("req_connect=disabled&csrf_token=5TvISVht8aUo%2BZXeJv3qjXi818GAWpv");

        System.out.println("ENcrypting Works: " + "77e8cfe400a2e4896ee25d2f3177a474b43e67d55268662ffa8ec2c5225934f8a6df9b11641f0c0a8aec76509a7409c6fff6ff6369a60ae302eec9814f66de92dad0bc5eec71b40f8f".equals(result));
    }

    private static void decrpyttest() throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException {
        // decrypt
        SpeedPortHybrid inst = new SpeedPortHybrid();
        inst.challengev = "CD39f2B7e910aFe9FFA1A2F18DFFD28fB373dbAaaFa444DaBA3A";
        inst.derivedk = "595578fe25bca7f227bb2c84f3293fb6";
        String result = inst.decryptAndHandle(
                "47E742A51B14BA6A7DE0772B12C5ED2CBA6C82E3A68FE1B77777D7A2B1437592051DE2D1E53F3A79E8F00667AAB8C6E1CDF2242127C9D4210735997247DC6009FEA4D87F79161C69309585B80CEDED46B80B57A0DCF27036B3F38982AB1D4B8052841598741E494B412FF50098A22161CF3AA3E2BC56E49CDC7135D1E9276DB299EE9BE9BDE205C777FDA6C7EAF7652C6FDADA55892AA088FB017313AA6F1A9707530826389B41F5F50ED92D767423C1A7DDD88434FF372E20FF49B755359359B570CD0753FCB0CEA1B496308B9D49A442BF08A7949BE75B1AB188552B8FC46ADFDDBED4A9D0F9F12902CE9BD9BADA6448654296D65DB691562370BB979F3E844E2F1F136A4AA0CE9924CED5B9C8E25EFB85FE2ED41D275DE6418BDB747D8256AEE3642073958387B6EFB038E9EE994A58085560DE4353DF9C9406A89739CCF58462A96E627BF73C9AED3776873A2D970EF938B1CFDC9195F0C1FC8EE033E47351A6D543F0CFE89F9AD8211BA45811F03CFAB932DEA540C7C06DC92EE7BE317A7647A35904151F4B5DD86FB10F6822E0B696A823BB1B91E9AC201E78C5E2CEB35FF31AB1B8E0C0B0459E5DA85E9DB79EF4E5CD77640111249497BE11A7F2AAA410F053D0F2CEBBBA2FBF31512EE7195D081919FDBC260436A9E7241E40D41589A25FB91F214C36DC00DFF14EE012BA0245");

        System.out.println("Decryption Works: " + (result.contains("onlinestatus") && result.length() == 489));
    }

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

    public String decryptAndHandle(String hex) throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException {
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

        if (isLoggedIn()) {
            updateCSRF(ret);
            String loginstate = extractVariable(ret, "loginstate");
            if ("0".equals(loginstate)) {
                br.clearCookies("http://" + config.getRouterIP());
                br = null;
                throw new SessionInvalidException();
            }
        }
        return ret;
    }

    private String getTimeParams() {
        return "_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000));
    }

    @Override
    public int getIpCheckInterval() {

        return 1000;
    }

    @Override
    public IP getExternalIP() throws IPCheckException {
        synchronized (SpeedPortHybrid.this) {
            try {
                try {
                    return getExternalIPOnce();
                } catch (SessionInvalidException e) {

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

        String crypted = decryptAndHandle(br.getPage("http://" + config.getRouterIP() + "/data/INetIP.json?" + getTimeParams() + "&" + getTimeParams()));

        onlineStatus = extractVariable(crypted, "onlinestatus");
        externalIP = extractVariable(crypted, "public_ip_v4");
        Log.info("Online Status: " + onlineStatus);
        Log.info("IP: " + externalIP);
        if (externalIP != null) {
            return IP.getInstance(externalIP);
        } else {
            throw new InvalidIPException("null");
        }
    }

    private String extractVariable(String crypted, String key) {
        return new Regex(crypted, "\"varid\"\\s*:\\s*\"" + key + "\",\\s*\"varvalue\"\\s*:\\s*\"([^\"]+)").getMatch(0);
    }

    public SpeedPortHybrid() {
        super();
        config = JsonConfig.create(SpeedPortHybridReconnectConfig.class);
        icon = new AbstractIcon(IconKey.ICON_RECONNECT, 16);
        setIPCheckProvider(this);
        invoker = new ReconnectInvoker(this) {

            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
            }

            @Override
            public void run() throws ReconnectException {
                synchronized (SpeedPortHybrid.this) {
                    try {
                        try {
                            runOnce();
                        } catch (SessionInvalidException e) {
                            runOnce();
                        }
                    } catch (Throwable e) {
                        throw new ReconnectException(e);
                    }
                }
            }

            private void runOnce() throws Exception, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException, IOException, InterruptedException {
                ensureSession();

                // http://" + config.getRouterIP() + "/html/content/internet/connection.html?lang=de
                Log.info("CurrentStatus " + onlineStatus);
                boolean changeConnectionOnline = false;

                if (StringUtils.isEmpty(onlineStatus) || "disabled".equalsIgnoreCase(onlineStatus)) {
                    changeConnectionOnline = true;
                }
                if (changeConnectionOnline) {
                    // "req_connect=online&csrf_token=j2WMeJ%2BjJv4WlwLdwKuWoWxpNY6JXyC"
                    decryptAndHandle(br.postPageRaw("http://" + config.getRouterIP() + "/data/Connect.json?lang=de", encrypt("req_connect=online&csrf_token=" + csrf)));
                    waitForConnection();

                }

                // "req_connect=disabled&csrf_token=j2WMeJ%2BjJv4WlwLdwKuWoWxpNY6JXyC"
                decryptAndHandle(br.postPageRaw("http://" + config.getRouterIP() + "/data/Connect.json?lang=de", encrypt("req_connect=disabled&csrf_token=" + csrf)));

                // req_connect=online
                // req_connect=disabled
                // lte_reconn=1
                // "lte_reconn=1&csrf_token=j2WMeJ%2BjJv4WlwLdwKuWoWxpNY6JXyC"
                decryptAndHandle(br.postPageRaw("http://" + config.getRouterIP() + "/data/modules.json?lang=de", encrypt("lte_reconn=1&csrf_token=" + csrf)));

                waitForConnection();

                decryptAndHandle(br.postPageRaw("http://" + config.getRouterIP() + "/data/Connect.json?lang=de", encrypt("req_connect=online&csrf_token=" + csrf)));

                /*
                 * var challengev = getCookie('challengev'); var iv = challengev.substr(16, 16); var adata = challengev.substr(32, 16);
                 *
                 * var derivedk = getCookie("derivedk"); var c = new sjcl.cipher.aes(sjcl.codec.hex.toBits(derivedk));
                 *
                 * var pt = sjcl.mode.ccm.decrypt(c, sjcl.codec.hex.toBits(data), sjcl.codec.hex.toBits(iv), sjcl.codec.hex.toBits(adata));
                 * pt = sjcl.codec.utf8String.fromBits(pt); return pt;
                 */

            }

            private void waitForConnection() throws InterruptedException, InvalidCipherTextException, UnsupportedEncodingException, SessionInvalidException, IOException {
                for (int i = 0; i < 60 * 5; i++) {
                    Thread.sleep(1000);
                    String crypted = decryptAndHandle(br.getPage("http://" + config.getRouterIP() + "/data/Connect.json?_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000))));
                    if (!"establishing".equals(extractVariable(crypted, "onlinestatus"))) {
                        break;
                    }
                }
            }

        };
    }

    private void updateCSRF(String crypted) {
        String newCsrf = extractVariable(crypted, "csrf_token");
        if (StringUtils.isNotEmpty(newCsrf)) {
            Log.info("New CSRF: " + newCsrf);
            csrf = newCsrf;
        } else {
            Log.info("No new CSRF");
        }
        String newcsrf = br.getRegex("csrf_token\\s*=\\s*\"([^\"]+)").getMatch(0);
        if (StringUtils.isNotEmpty(newcsrf)) {
            Log.info("New CSRF: " + newcsrf);
            csrf = newcsrf;
        } else {
            Log.info("No new CSRF");
        }
    }

    protected void ensureSession() throws Exception {
        if (isLoggedIn()) {
            return;
        }
        br = new Browser();
        br.setCookiesExclusive(true);
        br.setVerbose(true);
        br.setDebug(true);
        br.setProxy(new HTTPProxy(TYPE.HTTP, "localhost", 8888));
        Log.info(config + "");
        br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("challengev", "null", true));
        challengev = br.getRegex("\"challengev\",.*?\"varvalue\":\"(.*?)\"").getMatch(0);
        // br.setCookie("http://" + config.getRouterIP(), "challengev", challengev);

        Log.info("Challenge: " + challengev);
        decryptAndHandle(br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("password", Hash.getSHA256(challengev + ":" + config.getPassword()), true)));

        session = br.getCookie("http://" + config.getRouterIP(), "SessionID_R3");
        if (StringUtils.isEmpty(session)) {
            UIOManager.I().showErrorMessage("Login to Speedport Failed!");
            br = null;
            throw new SessionInvalidException();
        }
        br.setCookie("http://" + config.getRouterIP(), "derivedk", derivedk = PBKDF2Key(config.getPassword(), challengev.substring(0, 16)));

        getPage("/html/content/internet/connection.html?lang=de");

    }

    private boolean isLoggedIn() {
        return br != null && br.getCookie("http://" + config.getRouterIP(), "SessionID_R3") != null;
    }

    private void getPage(String string) throws IOException, IllegalStateException, InvalidCipherTextException, SessionInvalidException {

        decryptAndHandle(br.getPage("http://" + config.getRouterIP() + string));

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