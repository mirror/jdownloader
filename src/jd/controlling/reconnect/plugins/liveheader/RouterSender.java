package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JButton;

import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RecollInterface;
import jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin;
import jd.controlling.reconnect.plugins.upnp.UpnpRouterDevice;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Hash;
import org.appwork.utils.Lists;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.remotecall.RemoteClient;

public class RouterSender {

    static String getManufactor(String mc) {
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
    private final RouterSenderSettings  storage;
    private RecollInterface             recoll;
    private InetAddress                 gatewayAdress;
    private String                      gatewayAdressHost;
    private String                      gatewayAdressIP;
    private UpnpRouterDevice            myUpnpDevice;

    public RouterSender() {
        this.storage = JsonConfig.create(RouterSenderSettings.class);
        recoll = new RemoteClient(LiveHeaderDetectionWizard.UPDATE3_JDOWNLOADER_ORG_RECOLL).getFactory().newInstance(RecollInterface.class);

    }

    private Component addHelpButton(final String name, final String tooltip, final String dialog) {
        final JButton but = new JButton(NewTheme.I().getIcon("help", 20));
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

    public void collectData() throws Exception {

        final UPNPRouterPlugin upnp = (UPNPRouterPlugin) ReconnectPluginController.getInstance().getPluginByID(UPNPRouterPlugin.ID);

        ArrayList<UpnpRouterDevice> devices = upnp.getDevices();

        String gatewayIP = JsonConfig.create(LiveHeaderReconnectSettings.class).getRouterIP();

        if (!IP.isValidRouterIP(gatewayIP)) {
            final InetAddress ia = RouterUtils.getAddress(true);
            if (ia != null && IP.isValidRouterIP(ia.getHostAddress())) {
                gatewayIP = ia.getHostName();
            }
        }
        mac = null;
        manufactor = null;
        gatewayAdress = InetAddress.getByName(gatewayIP);
        gatewayAdressHost = gatewayAdress.getHostName();
        gatewayAdressIP = gatewayAdress.getHostAddress();
        try {
            mac = RouterUtils.getMacAddress(gatewayAdress).replace(":", "").toUpperCase(Locale.ENGLISH);

            manufactor = RouterSender.getManufactor(mac);
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        myUpnpDevice = null;
        for (final UpnpRouterDevice d : devices) {
            if (d.getHost() != null) {
                try {
                    if (gatewayAdress.equals(InetAddress.getByName(d.getHost()))) {
                        myUpnpDevice = d;
                        break;
                    }
                } catch (final UnknownHostException e) {
                    // nothing
                }
            }
        }
        // Use upnp manufactor if given
        if (myUpnpDevice != null && myUpnpDevice.getManufactor() != null) {
            manufactor = myUpnpDevice.getManufactor();
        }
        try {
            routerName = JsonConfig.create(LiveHeaderReconnectSettings.class).getRouterData().getRouterName();
        } catch (Exception e) {
        }
        if (routerName == null || routerName.trim().length() == 0 || "unknown".equalsIgnoreCase(routerName)) {
            // try to convert domain to routername
            if (!gatewayAdressHost.equals(gatewayAdressIP)) {
                routerName = gatewayAdressHost;
                int i = routerName.lastIndexOf(".");
                if (i > 0) routerName = routerName.substring(0, i);
            }
        }
        if (myUpnpDevice != null) {
            if (myUpnpDevice.getModelname() != null) {
                routerName = myUpnpDevice.getModelname();
            } else if (myUpnpDevice.getFriendlyname() != null) {
                routerName = myUpnpDevice.getFriendlyname();
            }

        }

        //
        //
        // p.add(new JLabel("Webinterface User"));
        // p.add(this.addHelpButton("Webinterface User",
        // "...will not be transfered.",
        // "This value will not be sent to us. \r\nWe need this to make sure that your reconnect script does not contain sensitive data."));
        //
        // p.add(this.txtUser);
        // p.add(new JLabel("Webinterface Password"));
        // p.add(this.addHelpButton("Webinterface Password",
        // "...will not be transfered.",
        // "This value will not be sent to us. \r\nWe need this to make sure that your reconnect script does not contain sensitive data."));

        // p.add(this.txtPass);

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

            }, 0, JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.title", "UPNP Router Wizard"), JDL.L("jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin.actionPerformed.wizard.find.message", "Scanning all network interfaces"), null);
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
        ArrayList<String> list = storage.getSentScriptIds();
        return list.contains(Hash.getMD5(this.getPlugin().getScript()));

    }

    public void run(ProcessCallBack processCallBack) throws Exception {
        processCallBack.setStatusString(this, _GUI._.LiveaheaderDetection_wait_for_online());
        IPController.getInstance().waitUntilWeAreOnline();
        recoll.isAlive();

        this.collectData();

        final String dataString = JSonStorage.serializeToJson(this);

        // br.forceDebug(true);
        // final String data = URLEncoder.encode(dataString, "UTF-8");
        // URLDecoder.decode(data.trim(), "UTF-8");
        // br.postPage(RouterSender.ROUTER_COL_SERVICE, "action=add&data=" +
        // data);
        // if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
        // throw new
        // Exception("Service is currently not available. Please try again later");
        // }
        // if (br.getRegex(".*?exists.*?").matches()) {
        // Dialog.getInstance().showMessageDialog("We noticed, that your script already exists in our database.\r\nThanks anyway.");
        //
        // } else {
        // Dialog.getInstance().showMessageDialog("Thank you!\r\nWe added your script to our router reconnect database.");
        // }
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
        ArrayList<String> list = storage.getSentScriptIds();
        list.add(Hash.getMD5(this.getPlugin().getScript()));
        storage.setSentScriptIds(Lists.unique(list));

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
