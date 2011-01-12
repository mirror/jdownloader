package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.controlling.FavIconController;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin;
import jd.controlling.reconnect.plugins.upnp.UpnpRouterDevice;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Hash;
import org.appwork.utils.locale.Loc;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;

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

    private String                      routerIP;
    private String                      script;
    private String                      routerName;

    private String                      mac;
    private String                      manufactor;
    private int                         responseCode;
    private HashMap<String, String>     responseHeaders;

    private String                      title;

    private int                         pTagsCount;

    private int                         frameTagCount;

    private String                      favIconHash;
    private JTextField                  txtName;
    private JTextField                  txtManufactor;
    private JTextField                  txtUser;
    private JTextField                  txtPass;
    private JTextField                  txtIP;
    private JTextField                  txtFirmware;
    private String                      firmware;
    private ArrayList<UpnpRouterDevice> devices;
    private String                      response;
    private String                      exception;

    private String                      sslException;

    private String                      sslResponse;

    private int                         sslResponseCode;

    private HashMap<String, String>     sslResponseHeaders;

    private String                      sslTitle;

    private int                         sslPTagsCount;

    private int                         sslFrameTagCount;

    private String                      sslFavIconHash;
    private final Storage               storage;

    private RouterSender() {
        this.storage = JSonStorage.getPlainStorage("ROUTERSENDER");
    }

    private Component addHelpButton(final String name, final String tooltip, final String dialog) {
        final JButton but = new JButton(JDTheme.II("gui.images.help", 20, 20));
        but.setContentAreaFilled(false);
        but.setToolTipText(tooltip);
        but.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {

                Dialog.getInstance().showMessageDialog(0, name + ":" + tooltip, dialog);

            }

        });
        return but;

    }

    /**
     * let the user Choose between 2 alternatives
     * 
     * @param routerName2
     * @param upnpName
     * @param what
     * @return
     */
    private String choose(final String routerName2, final String upnpName, final String what) {
        final String[] options = new String[] { routerName2, upnpName };
        int ret;
        try {
            ret = Dialog.getInstance().showComboDialog(Dialog.STYLE_HIDE_ICON, "Choose correct " + what, "Please choose the correct " + what, options, 0, null, null, null, null);
        } catch (DialogClosedException e) {
            return routerName2;

        } catch (DialogCanceledException e) {
            return routerName2;
        }
        if (ret < 0) { return routerName2; }
        return options[ret];

    }

    private void collectData() throws Exception {

        UpnpRouterDevice myDevice = this.getUPNPDevice(this.getPlugin().getRouterIP());

        try {
            this.mac = RouterUtils.getMacAddress(this.getPlugin().getRouterIP());
            this.manufactor = RouterSender.getManufactor(this.mac);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        // Use upnp manufactor if given
        if (myDevice != null && myDevice.getManufactor() != null) {
            this.manufactor = myDevice.getManufactor();
        }
        this.routerName = this.getPlugin().getRouterName();
        if (myDevice != null) {
            if (myDevice.getModelname() != null) {
                this.routerName = myDevice.getModelname();
            } else if (myDevice.getFriendlyname() != null) {
                this.routerName = myDevice.getFriendlyname();
            }

        }

        this.txtName = new JTextField(this.routerName);
        this.txtManufactor = new JTextField(this.manufactor);
        this.txtFirmware = new JTextField();
        this.txtUser = new JTextField(this.getPlugin().getUser());
        this.txtPass = new JTextField(this.getPlugin().getPassword());
        this.txtIP = new JTextField(this.getPlugin().getRouterIP());

        final JPanel p = new JPanel(new MigLayout("ins 5,wrap 3", "[][][grow,fill]", "[grow,fill]"));
        p.add(new JLabel("Please enter your router information as far as possible."), "spanx");
        p.add(new JLabel("We will not transfer username or password!"), "spanx");
        p.add(new JLabel("Model Name"));
        p.add(this.addHelpButton("Model Name", "...is written on your router", "Find the name of your router:\r\n  1. In your router's manual.\r\n  2. Written on your router device.\r\n  3. Or open the router's webinterface"));
        p.add(this.txtName);

        p.add(new JLabel("Manufactor"));
        p.add(this.addHelpButton("Manufactor", "...if this field is empty, look at your router", "Find the manufactor of your router:\r\n  1. In your router's manual.\r\n  2. Written on your router device.\r\n  3. Or open the router's webinterface"));

        p.add(this.txtManufactor);
        p.add(new JLabel("Firmware"));
        p.add(this.addHelpButton("Firmware", "...find it in your router's webinterface", "Find the firmware of your router:\r\nOpen your router's webinterface. You should find the firmware on the page or in the system informations page.\r\nIf you don't find it, just leave this field blank."));

        p.add(this.txtFirmware);
        // p.add(new JLabel("Firmware"));
        // p.add(this.txtFirmware);

        p.add(new JLabel("Webinterface IP"));
        p.add(this.addHelpButton("Webinterface IP", "...can be found in the router's manual", "If this field is blank, consult your router's manual to find it's up or its host name.\r\nThe IP or hostname is the name, you have to use to connect to the router webinterface (http://HOSTNAME)"));

        p.add(this.txtIP);

        p.add(new JLabel("Webinterface User"));
        p.add(this.addHelpButton("Webinterface User", "...will not be transfered.", "This value will not be sent to us. \r\nWe need this to make sure that your reconnect script does not contain sensitive data."));

        p.add(this.txtUser);
        p.add(new JLabel("Webinterface Password"));
        p.add(this.addHelpButton("Webinterface Password", "...will not be transfered.", "This value will not be sent to us. \r\nWe need this to make sure that your reconnect script does not contain sensitive data."));

        p.add(this.txtPass);
        this.txtUser.setText(this.getPlugin().getUser());
        this.txtPass.setText(this.getPlugin().getPassword());
        this.txtName.setText(this.routerName);
        this.txtIP.setText(this.getPlugin().getRouterIP());

        final ContainerDialog routerInfo = new ContainerDialog(0, "Enter Router Information", p, null, "Continue", null);

        if (!Dialog.isOK(Dialog.getInstance().showDialog(routerInfo))) { throw new Exception("User canceled"); }

        this.firmware = this.txtFirmware.getText();
        this.manufactor = this.txtManufactor.getText().trim();
        this.routerName = this.txtName.getText().trim();
        this.routerIP = this.txtIP.getText();

        myDevice = this.getUPNPDevice(this.getPlugin().getRouterIP());
        if (myDevice != null) {
            String upnpName = myDevice.getModelname();
            if (upnpName == null) {
                upnpName = myDevice.getFriendlyname();
            }

            if (upnpName != null && !upnpName.trim().equalsIgnoreCase(this.routerName)) {
                this.routerName = this.choose(this.routerName, upnpName.trim(), "model name");
            }

            if (myDevice.getManufactor() != null && !myDevice.getManufactor().trim().equalsIgnoreCase(this.manufactor)) {
                this.manufactor = this.choose(this.manufactor, myDevice.getManufactor().trim(), "manufactor");
            }
        }

        try {
            this.mac = RouterUtils.getMacAddress(this.routerIP);

        } catch (final Exception e) {
            e.printStackTrace();
        }

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
            this.sslResponse = br.getPage("https://" + this.routerIP);

            this.sslTitle = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.sslPTagsCount = br.toString().split("<p>").length;
            this.sslFrameTagCount = br.toString().split("<frame").length;
            // get favicon and build hash
            try {
                final BufferedImage image = FavIconController.getInstance().downloadFavIcon(this.routerIP);
                final File imageFile = JDUtilities.getResourceFile("tmp/routerfav.png", true);
                imageFile.delete();
                imageFile.deleteOnExit();
                ImageIO.write(image, "png", imageFile);
                this.sslFavIconHash = Hash.getMD5(imageFile);
            } catch (final Exception e) {
                e.printStackTrace();
            }

        } catch (final Throwable e) {

            this.sslException = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        URLConnectionAdapter con = br.getHttpConnection();
        if (con != null) {
            this.sslResponseCode = con.getResponseCode();
            this.sslResponseHeaders = new HashMap<String, String>();

            for (final Entry<String, List<String>> next : con.getHeaderFields().entrySet()) {
                for (final String value : next.getValue()) {
                    this.sslResponseHeaders.put(next.getKey().toLowerCase(), value);
                }
            }
        }

        try {
            this.response = br.getPage("http://" + this.routerIP);

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
            this.exception = e.getClass().getSimpleName() + ": " + e.getMessage();
            e.printStackTrace();
        }

        con = br.getHttpConnection();
        if (con != null) {
            this.responseCode = con.getResponseCode();
            this.responseHeaders = new HashMap<String, String>();
            for (final Entry<String, List<String>> next : con.getHeaderFields().entrySet()) {
                for (final String value : next.getValue()) {
                    this.responseHeaders.put(next.getKey().toLowerCase(), value);
                }
            }
        }

    }

    public String getException() {
        return this.exception;
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

    public String getResponse() {
        return this.response;
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

    public String getSslException() {
        return this.sslException;
    }

    public String getSslFavIconHash() {
        return this.sslFavIconHash;
    }

    public int getSslFrameTagCount() {
        return this.sslFrameTagCount;
    }

    public int getSslPTagsCount() {
        return this.sslPTagsCount;
    }

    public String getSslResponse() {
        return this.sslResponse;
    }

    public int getSslResponseCode() {
        return this.sslResponseCode;
    }

    public HashMap<String, String> getSslResponseHeaders() {
        return this.sslResponseHeaders;
    }

    public String getSslTitle() {
        return this.sslTitle;
    }

    public String getTitle() {
        return this.title;
    }

    private UpnpRouterDevice getUPNPDevice(final String routerIP2) {
        if (this.devices == null) {
            final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                public int getProgress() {
                    return -1;
                }

                public String getString() {
                    return null;
                }

                public void run() throws Exception {
                    final UPNPRouterPlugin upnp = (UPNPRouterPlugin) ReconnectPluginController.getInstance().getPluginByID(UPNPRouterPlugin.ID);
                    try {
                        RouterSender.this.devices = upnp.scanDevices();
                    } catch (final IOException e) {
                        RouterSender.this.devices = new ArrayList<UpnpRouterDevice>();
                    }

                }

            }, 0, Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.title", "UPNP Router Wizard"), Loc.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.find.message", "Scanning all network interfaces"), null);
            try {
                Dialog.getInstance().showDialog(dialog);
            } catch (DialogClosedException e) {
                e.printStackTrace();
            } catch (DialogCanceledException e) {
                e.printStackTrace();
            }
        }

        for (final UpnpRouterDevice d : this.devices) {
            if (d.getHost() != null) {
                try {
                    if (InetAddress.getByName(routerIP2).equals(InetAddress.getByName(d.getHost()))) { return d; }
                } catch (final UnknownHostException e) {
                    // nothing
                }
            }
        }
        return null;

    }

    /**
     * returns true of the user has already been asked to send his script. do
     * not ask him again
     * 
     * @return
     */
    public boolean isRequested() {
        // we use md5 of script. if user changes script, he should be able to
        // send it again
        return this.storage.get(Hash.getMD5(this.getPlugin().getScript()), false);
    }

    public void run() throws Exception {

        final Browser br = new Browser();
        // is services available. throws exception if server is down
        br.getPage(RouterSender.ROUTER_COL_SERVICE);

        if (br.getRequest().getHttpConnection().getResponseCode() != 200) { throw new Exception("Service is currently not available. Please try again later"); }
        final int ret = Dialog.getInstance().showConfirmDialog(0, "Router Sender", "We need your help to improve our reconnect database.\r\nPlease contribute to the 'JD Project' and send in our reconnect script.\r\nThis wizard will guide you through all required steps.", null, null, null);

        if (!Dialog.isOK(ret)) {
            Dialog.getInstance().showMessageDialog("You can send your reconnect script at any time by clicking the 'Send Button' in your reconnect settings panel");
            return;

        }
        this.collectData();

        final String dataString = JSonStorage.toString(this);

        br.forceDebug(true);
        final String data = URLEncoder.encode(dataString, "UTF-8");
        URLDecoder.decode(data.trim(), "UTF-8");
        br.postPage(RouterSender.ROUTER_COL_SERVICE, "action=add&data=" + data);
        if (br.getRequest().getHttpConnection().getResponseCode() != 200) { throw new Exception("Service is currently not available. Please try again later"); }
        if (br.getRegex(".*?exists.*?").matches()) {
            Dialog.getInstance().showMessageDialog("We noticed, that your script already exists in our database.\r\nThanks anyway.");

        } else {
            Dialog.getInstance().showMessageDialog("Thank you!\r\nWe added your script to our router reconnect database.");
        }
    }

    public void setException(final String exception) {
        this.exception = exception;
    }

    public void setFavIconHash(final String favIconHash) {
        this.favIconHash = favIconHash;
    }

    public void setFirmware(final String firmware) {
        this.firmware = firmware;
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

    /**
     * set this to true if the user has been asked to send his script.
     * 
     * @param b
     */
    public void setRequested(final boolean b) {
        this.storage.put(Hash.getMD5(this.getPlugin().getScript()), b);
    }

    public void setResponse(final String response) {
        this.response = response;
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

    public void setSslException(final String sslException) {
        this.sslException = sslException;
    }

    public void setSslFavIconHash(final String sslFavIconHash) {
        this.sslFavIconHash = sslFavIconHash;
    }

    public void setSslFrameTagCount(final int sslFrameTagCount) {
        this.sslFrameTagCount = sslFrameTagCount;
    }

    public void setSslPTagsCount(final int sslPTagsCount) {
        this.sslPTagsCount = sslPTagsCount;
    }

    public void setSslResponse(final String sslResponse) {
        this.sslResponse = sslResponse;
    }

    public void setSslResponseCode(final int sslResponseCode) {
        this.sslResponseCode = sslResponseCode;
    }

    public void setSslResponseHeaders(final HashMap<String, String> sslResponseHeaders) {
        this.sslResponseHeaders = sslResponseHeaders;
    }

    public void setSslTitle(final String sslTitle) {
        this.sslTitle = sslTitle;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    private String trim(final String stringToTrim) {

        return stringToTrim == null ? null : stringToTrim.trim();
    }

}
