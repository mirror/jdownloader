package jd.controlling.reconnect.pluginsinc.speedporthybrid;

import java.awt.Component;
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
import org.appwork.utils.Hash;
import org.appwork.utils.formatter.HexFormatter;
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
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.http.Browser;
import jd.http.QueryInfo;
import net.miginfocom.swing.MigLayout;

/**
 * Plugin to use an extern tool for reconnection
 */
public class SpeedPortHybrid extends RouterPlugin {

    public static final String             ID = "SpeedPortHybrid";

    private Icon                           icon;

    private ReconnectInvoker               invoker;

    private ExtPasswordField               txtPassword;

    private SpeedPortHybridReconnectConfig config;

    private ExtTextField                   txtIP;

    private static String PBKDF2Key(String password, String salt) throws Exception {

        final PBEKeySpec spec = new PBEKeySpec(Hash.getSHA256(password).toCharArray(), salt.getBytes("UTF-8"), 1000, 16 * 8);
        final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final byte[] hash = skf.generateSecret(spec).getEncoded();
        return HexFormatter.byteArrayToHex(hash);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, IllegalStateException, InvalidCipherTextException {
        // // Encrypt
        // var challengev = getCookie('challengev');
        // var iv = challengev.substr(16, 16);
        // var adata = challengev.substr(32, 16);
        //
        //
        // var derivedk = getCookie("derivedk");
        // var c = new sjcl.cipher.aes(sjcl.codec.hex.toBits(derivedk));
        //
        //
        // var pt = $.param(data);
        // var ct = sjcl.mode.ccm.encrypt(c, sjcl.codec.utf8String.toBits(pt), sjcl.codec.hex.toBits(iv), sjcl.codec.hex.toBits(adata));
        // ct = sjcl.codec.hex.fromBits(ct);

        String challengeev = "c5D5f37d0EC075dd1793D1eeB01fEc05eA2e4a66BF78908acDDA";
        byte[] iv = HexFormatter.hexToByteArray(challengeev.substring(16, 32));
        byte[] adata = HexFormatter.hexToByteArray(challengeev.substring(32, 32 + 16));
        byte[] derivedk = HexFormatter.hexToByteArray("b5b7e46f30820543f448eece4d20da1a");
        String pt = "req_connect=disabled&csrf_token=5TvISVht8aUo%2BZXeJv3qjXi818GAWpv";

        CCMBlockCipher chipher = new CCMBlockCipher(new AESFastEngine());
        AEADParameters params = new AEADParameters(new KeyParameter(derivedk), 64, iv);
        chipher.init(true, params);
        byte[] enc = pt.getBytes("UTF-8");
        byte[] tmp = new byte[enc.length + adata.length];
        chipher.processAADBytes(adata, 0, adata.length);
        int len = chipher.processBytes(enc, 0, enc.length, tmp, 0);
        len += chipher.doFinal(tmp, len);
        String result = HexFormatter.byteArrayToHex(tmp);
        System.out.println("77e8cfe400a2e4896ee25d2f3177a474b43e67d55268662ffa8ec2c5225934f8a6df9b11641f0c0a8aec76509a7409c6fff6ff6369a60ae302eec9814f66de92dad0bc5eec71b40f8f".equals(result));

        decrpyttest();
    }

    private static void decrpyttest() throws IllegalStateException, InvalidCipherTextException {
        // decrypt
        String challengeev = "CD39f2B7e910aFe9FFA1A2F18DFFD28fB373dbAaaFa444DaBA3A";
        String ivs;
        byte[] iv = HexFormatter.hexToByteArray(ivs = challengeev.substring(16, 16 + 16));
        String ads;
        byte[] adata = HexFormatter.hexToByteArray(ads = challengeev.substring(32, 32 + 16));
        byte[] derivedk = HexFormatter.hexToByteArray("595578fe25bca7f227bb2c84f3293fb6");
        byte[] data = HexFormatter.hexToByteArray(
                "47E742A51B14BA6A7DE0772B12C5ED2CBA6C82E3A68FE1B77777D7A2B1437592051DE2D1E53F3A79E8F00667AAB8C6E1CDF2242127C9D4210735997247DC6009FEA4D87F79161C69309585B80CEDED46B80B57A0DCF27036B3F38982AB1D4B8052841598741E494B412FF50098A22161CF3AA3E2BC56E49CDC7135D1E9276DB299EE9BE9BDE205C777FDA6C7EAF7652C6FDADA55892AA088FB017313AA6F1A9707530826389B41F5F50ED92D767423C1A7DDD88434FF372E20FF49B755359359B570CD0753FCB0CEA1B496308B9D49A442BF08A7949BE75B1AB188552B8FC46ADFDDBED4A9D0F9F12902CE9BD9BADA6448654296D65DB691562370BB979F3E844E2F1F136A4AA0CE9924CED5B9C8E25EFB85FE2ED41D275DE6418BDB747D8256AEE3642073958387B6EFB038E9EE994A58085560DE4353DF9C9406A89739CCF58462A96E627BF73C9AED3776873A2D970EF938B1CFDC9195F0C1FC8EE033E47351A6D543F0CFE89F9AD8211BA45811F03CFAB932DEA540C7C06DC92EE7BE317A7647A35904151F4B5DD86FB10F6822E0B696A823BB1B91E9AC201E78C5E2CEB35FF31AB1B8E0C0B0459E5DA85E9DB79EF4E5CD77640111249497BE11A7F2AAA410F053D0F2CEBBBA2FBF31512EE7195D081919FDBC260436A9E7241E40D41589A25FB91F214C36DC00DFF14EE012BA0245");
        AEADParameters params = new AEADParameters(new KeyParameter(derivedk), 64, iv);
        CCMBlockCipher dc = new CCMBlockCipher(new AESFastEngine());
        dc.init(false, params);

        byte[] enc = data;
        byte[] tmp = new byte[enc.length + adata.length];
        dc.processAADBytes(adata, 0, adata.length);
        int len = dc.processBytes(enc, 0, enc.length, tmp, 0);
        len += dc.doFinal(tmp, len);
        System.out.println("Decrypted: " + new String(tmp, 0, len));
    }

    public static String encrypt(String pt, String challengev, String derivedk) throws UnsupportedEncodingException, IllegalStateException, InvalidCipherTextException {
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

    public static String decrypt(String hex, String challengev, String derivedk) throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException {
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
        return new String(tmp, 0, len, "UTF-8");
    }

    private String getTimeParams() {
        return "_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000));
    }

    public SpeedPortHybrid() {
        super();
        config = JsonConfig.create(SpeedPortHybridReconnectConfig.class);
        icon = new AbstractIcon(IconKey.ICON_RECONNECT, 16);
        setIPCheckProvider(new IPCheckProvider() {

            @Override
            public int getIpCheckInterval() {
                return 1000;
            }

            @Override
            public IP getExternalIP() throws IPCheckException {
                try {
                    Browser br = new Browser();
                    br.setVerbose(true);
                    br.setDebug(true);
                    br.setProxy(new HTTPProxy(TYPE.HTTP, "localhost", 8888));

                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("challengev", "null", true));
                    String challengev = br.getRegex("\"challengev\",.*?\"varvalue\":\"(.*?)\"").getMatch(0);

                    Log.info("Challenge: " + challengev);
                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("password", Hash.getSHA256(challengev + ":" + config.getPassword()), true));

                    String derivedk;
                    br.setCookie("http://" + config.getRouterIP(), "derivedk", derivedk = PBKDF2Key(config.getPassword(), challengev.substring(0, 16)));
                    br.getPage("http://" + config.getRouterIP() + "/html/content/internet/connection.html?lang=de");
                    String csrf = br.getRegex("csrf_token\\s*=\\s*\"([^\"]+)").getMatch(0);
                    // http://192.168.2.1/data/INetIP.json?_time=1456755235240&_rand=805&_time=1456755235604&_rand=566
                    String crypted = decrypt(br.getPage("http://" + config.getRouterIP() + "/data/INetIP.json?" + getTimeParams() + "&" + getTimeParams()), challengev, derivedk);
                    // req_connect=online
                    // req_connect=disabled
                    // lte_reconn=1
                    Log.info("Decrypted IP: " + crypted);

                } catch (Throwable e) {
                    Log.log(e);
                    return null;
                }
                return null;
            }
        });
        invoker = new ReconnectInvoker(this) {
            private String derivedk;
            private String challengev;
            private String csrf;

            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
            }

            @Override
            public void run() throws ReconnectException {

                try {
                    Browser br = new Browser();
                    br.setVerbose(true);
                    br.setDebug(true);
                    br.setProxy(new HTTPProxy(TYPE.HTTP, "localhost", 8888));

                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("challengev", "null", true));
                    challengev = br.getRegex("\"challengev\",.*?\"varvalue\":\"(.*?)\"").getMatch(0);

                    Log.info("Challenge: " + challengev);
                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("password", Hash.getSHA256(challengev + ":" + config.getPassword()), true));

                    br.setCookie("http://" + config.getRouterIP(), "derivedk", derivedk = PBKDF2Key(config.getPassword(), challengev.substring(0, 16)));
                    br.getPage("http://" + config.getRouterIP() + "/html/content/internet/connection.html?lang=de");
                    csrf = br.getRegex("csrf_token\\s*=\\s*\"([^\"]+)").getMatch(0);

                    String crypted = dec(br.postPageRaw("http://" + config.getRouterIP() + "/data/Connect.json?lang=de", encrypt("req_connect=disabled&csrf_token=" + csrf)));
                    // req_connect=online
                    // req_connect=disabled
                    // lte_reconn=1
                    Log.info("Decrypted: " + new String(crypted));

                    crypted = dec(br.postPageRaw("http://" + config.getRouterIP() + "/data/modules.json?lang=de", encrypt("lte_reconn=1&csrf_token=" + csrf)));
                    // req_connect=online
                    // req_connect=disabled
                    // lte_reconn=1
                    Log.info("Decrypted: " + new String(crypted));

                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(3000);
                        String dec = dec(br.getPage("http://192.168.2.1/data/Connect.json?_time=" + System.currentTimeMillis() + "&_rand=" + ((int) (Math.random() * 1000))));
                        Log.info(dec);
                    }

                    crypted = dec(br.postPageRaw("http://" + config.getRouterIP() + "/data/Connect.json?lang=de", encrypt("lte_reconn=online&csrf_token=" + csrf)));
                    // req_connect=online
                    // req_connect=disabled
                    // lte_reconn=1
                    Log.info("Decrypted: " + new String(crypted));
                    /*
                     * var challengev = getCookie('challengev'); var iv = challengev.substr(16, 16); var adata = challengev.substr(32, 16);
                     *
                     * var derivedk = getCookie("derivedk"); var c = new sjcl.cipher.aes(sjcl.codec.hex.toBits(derivedk));
                     *
                     * var pt = sjcl.mode.ccm.decrypt(c, sjcl.codec.hex.toBits(data), sjcl.codec.hex.toBits(iv),
                     * sjcl.codec.hex.toBits(adata)); pt = sjcl.codec.utf8String.fromBits(pt); return pt;
                     */

                } catch (Throwable e) {
                    throw new ReconnectException(e);
                }
            }

            private String dec(String hex) throws IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException {
                return SpeedPortHybrid.decrypt(hex, challengev, derivedk);
            }

            private String encrypt(String pt) throws UnsupportedEncodingException, IllegalStateException, InvalidCipherTextException {
                return SpeedPortHybrid.encrypt(pt, challengev, derivedk);
            }

        };
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
        p.add(label("Router IP"));
        p.add(txtIP);
        p.add(label("Password"));
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