package jd.controlling.reconnect.plugins.liveheader;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.controlling.FavIconController;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterUtils;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Hash;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;

public class RouterSender {
    private static final RouterSender INSTANCE           = new RouterSender();
    private static final String       ROUTER_COL_SERVICE = "http://update3.jdownloader.org:44444";

    public static RouterSender getInstance() {
        return RouterSender.INSTANCE;
    }

    private static String getManufactor(String mc) {
        if (mc == null) { return null; }
        // do not use IO.readFile to save mem
        mc = mc.substring(0, 6);
        BufferedReader f = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        try {
            f = new BufferedReader(isr = new InputStreamReader(fis = new FileInputStream(JDUtilities.getResourceFile("jd/router/manlist.txt")), "UTF8"));
            String line;

            while ((line = f.readLine()) != null) {
                if (line.startsWith(mc)) { return line.substring(7); }
            }

        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                f.close();
            } catch (final Throwable e) {
            }
            try {
                isr.close();
            } catch (final Throwable e) {
            }
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public static void main(final String[] args) throws Exception {
        final String mc = RouterUtils.getMacAddress("192.168.0.1");
        System.out.println(mc + " => " + RouterSender.getManufactor(mc));
    }

    private String                  routerIP;
    private String                  script;
    private String                  routerName;

    private String                  mac;
    private String                  manufactor;
    private int                     responseCode;
    private HashMap<String, String> responseHeaders;

    private String                  title;

    private int                     pTagsCount;

    private int                     frameTagCount;

    private String                  favIconHash;
    private JTextField              txtName;
    private JTextField              txtManufactor;
    private JTextField              txtUser;
    private JTextField              txtPass;
    private JTextField              txtIP;
    private JTextField              txtFirmware;
    private String                  firmware;

    private RouterSender() {

    }

    private void collectData() throws Exception {
        try {
            this.mac = RouterUtils.getMacAddress(this.getPlugin().getRouterIP());
            this.manufactor = RouterSender.getManufactor(this.mac);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        this.txtName = new JTextField(this.routerName);
        this.txtManufactor = new JTextField(this.manufactor);
        this.txtFirmware = new JTextField();
        this.txtUser = new JTextField(this.getPlugin().getUser());
        this.txtPass = new JTextField(this.getPlugin().getPassword());
        this.txtIP = new JTextField(this.getPlugin().getRouterIP());

        final JPanel p = new JPanel(new MigLayout("ins 5,wrap 2", "[][grow,fill]", "[grow,fill]"));
        p.add(new JLabel("Please enter your router information as far as possible."), "spanx");
        p.add(new JLabel("We will not transfer username or password!"), "spanx");
        p.add(new JLabel("Model Name"));
        p.add(this.txtName);

        p.add(new JLabel("Manufactor"));
        p.add(this.txtManufactor);
        p.add(new JLabel("Firmware"));
        p.add(this.txtFirmware);
        // p.add(new JLabel("Firmware"));
        // p.add(this.txtFirmware);

        p.add(new JLabel("Webinterface IP"));
        p.add(this.txtIP);

        p.add(new JLabel("Webinterface User"));
        p.add(this.txtUser);
        p.add(new JLabel("Webinterface Password"));
        p.add(this.txtPass);
        this.txtUser.setText(this.getPlugin().getUser());
        this.txtPass.setText(this.getPlugin().getPassword());
        this.txtName.setText(this.getPlugin().getRouterName());
        this.txtIP.setText(this.getPlugin().getRouterIP());

        final ContainerDialog routerInfo = new ContainerDialog(0, "Enter Router Information", p, null, "Continue", null);

        this.firmware = this.txtFirmware.getText();
        this.manufactor = this.txtManufactor.getText();
        this.routerName = this.txtName.getText();
        this.routerIP = this.txtIP.getText();

        try {
            this.mac = RouterUtils.getMacAddress(this.routerIP);

        } catch (final Exception e) {
            e.printStackTrace();
        }
        if (!Dialog.isOK(Dialog.getInstance().showDialog(routerInfo))) { throw new Exception("User canceled"); }
        final String userName = this.txtUser.getText();
        final String password = this.txtPass.getText();

        this.script = this.trim(this.getPlugin().getScript());
        if (userName != null && userName.length() > 2) {
            this.script = Pattern.compile(Pattern.quote(userName), Pattern.CASE_INSENSITIVE).matcher(this.script).replaceAll("%%%user%%%");
        }
        if (password != null && password.length() > 2) {
            this.script = Pattern.compile(Pattern.quote(password), Pattern.CASE_INSENSITIVE).matcher(this.script).replaceAll("%%%pass%%%");
        }

        final Browser br = new Browser();
        try {
            br.getPage("http://" + this.routerIP);
            final URLConnectionAdapter con = br.getHttpConnection();
            this.responseCode = con.getResponseCode();
            this.responseHeaders = new HashMap<String, String>();
            for (final Entry<String, List<String>> next : con.getHeaderFields().entrySet()) {
                for (final String value : next.getValue()) {
                    this.responseHeaders.put(next.getKey().toLowerCase(), value);
                }
            }
            this.title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.pTagsCount = br.toString().split("<p>").length;
            this.frameTagCount = br.toString().split("<frame").length;
            // get favicon and build hash
            try {
                final BufferedImage image = FavIconController.getInstance().downloadFavIcon(this.routerIP);
                final File imageFile = JDUtilities.getResourceFile("tmp/routerfav.png", true);
                imageFile.delete();
                imageFile.deleteOnExit();
                ImageIO.write(image, "png", imageFile);
                this.favIconHash = Hash.getMD5(imageFile);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public String getFavIconHash() {
        return this.favIconHash;
    }

    public String getFirmware() {
        return this.firmware;
    }

    public int getFrameTagCount() {
        return this.frameTagCount;
    }

    public String getMac() {
        return this.mac;
    }

    public String getManufactor() {
        return this.manufactor;
    }

    private LiveHeaderReconnect getPlugin() {
        return (LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID);
    }

    public int getpTagsCount() {
        return this.pTagsCount;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public HashMap<String, String> getResponseHeaders() {
        return this.responseHeaders;
    }

    public String getRouterIP() {
        return this.routerIP;
    }

    public String getRouterName() {
        return this.routerName;
    }

    public String getScript() {
        return this.script;
    }

    public String getTitle() {
        return this.title;
    }

    public void run() throws Exception {
        final int ret = Dialog.getInstance().showConfirmDialog(0, "Router Sender", "We need your help to improve our reconnect database.\r\nPlease contribute to the 'JD Project' and send in our reconnect script.\r\nThis wizard will guide you through all required steps.", null, null, null);

        if (!Dialog.isOK(ret)) {
            Dialog.getInstance().showMessageDialog("You can send your reconnect script at any time by clicking the 'Send Button' in your reconnect settings panel");
            return;

        }
        this.collectData();

        final String dataString = JSonStorage.toString(this);

        final Browser br = new Browser();
        br.forceDebug(true);
        final String data = URLEncoder.encode(dataString, "UTF-8");
        URLDecoder.decode(data.trim(), "UTF-8");
        br.postPage(RouterSender.ROUTER_COL_SERVICE, "action=add&data=" + data);
        if (br.getRegex(".*?exists.*?").matches()) {
            Dialog.getInstance().showMessageDialog("We noticed, that your script already exists in our database.\r\nThanks anyway.");

        } else {
            Dialog.getInstance().showMessageDialog("Thank you!\r\nWe added your script to our router reconnect database.");
        }
    }

    public void setFavIconHash(final String favIconHash) {
        this.favIconHash = favIconHash;
    }

    public void setFrameTagCount(final int frameTagCount) {
        this.frameTagCount = frameTagCount;
    }

    public void setMac(final String mac) {
        this.mac = mac;
    }

    public void setManufactor(final String manufactor) {
        this.manufactor = manufactor;
    }

    public void setpTagsCount(final int pTagsCount) {
        this.pTagsCount = pTagsCount;
    }

    public void setResponseCode(final int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseHeaders(final HashMap<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setRouterIP(final String routerIP) {
        this.routerIP = routerIP;
    }

    public void setRouterName(final String routerName) {
        this.routerName = routerName;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    private String trim(final String stringToTrim) {

        return stringToTrim == null ? null : stringToTrim.trim();
    }

}
