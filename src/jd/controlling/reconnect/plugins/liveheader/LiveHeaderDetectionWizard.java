package jd.controlling.reconnect.plugins.liveheader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JTextField;

import jd.controlling.FavIconController;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectWizardProgress;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RecollInterface;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.plugins.liveheader.translate.T;
import jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin;
import jd.controlling.reconnect.plugins.upnp.UpnpRouterDevice;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.remotecall.RemoteClient;

public class LiveHeaderDetectionWizard {
    // "update3.jdownloader.org/recoll";
    private static final String     UPDATE3_JDOWNLOADER_ORG_RECOLL = "192.168.2.250/ces";

    private JTextField              txtName;

    private JTextField              txtManufactor;
    private JTextField              txtIP;
    private JTextField              txtUser;
    private JTextField              txtPass;

    private ReconnectWizardProgress progress;

    private String                  mac;

    private String                  manufactor;

    private InetAddress             gatewayAdress;

    private String                  username;

    private String                  password;

    private String                  firmware;

    private String                  sslResponse;

    private String                  sslTitle;

    private int                     sslPTagsCount;

    private int                     sslFrameTagCount;

    private String                  sslFavIconHash;

    private String                  sslException;

    private int                     sslResponseCode;

    private HashMap<String, String> sslResponseHeaders;

    private String                  response;

    private String                  title;

    private int                     pTagsCount;

    private int                     frameTagCount;

    private String                  favIconHash;

    private String                  exception;

    private int                     responseCode;

    private HashMap<String, String> responseHeaders;

    private RecollInterface         recoll;

    public LiveHeaderDetectionWizard(ReconnectWizardProgress progress) {
        this.progress = progress;

        recoll = new RemoteClient(LiveHeaderDetectionWizard.UPDATE3_JDOWNLOADER_ORG_RECOLL).getFactory().newInstance(RecollInterface.class);
    }

    private LiveHeaderReconnect getPlugin() {
        return (LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID);
    }

    // public int runOfflineScan() throws InterruptedException {
    // int ret = -1;
    // // final ArrayList<String[]> scripts =
    // // LiveHeaderReconnect.getLHScripts();
    //
    // this.txtName = new JTextField();
    // this.txtManufactor = new JTextField();
    // this.txtUser = new JTextField();
    // this.txtPass = new JTextField();
    // this.txtIP = new JTextField();
    //
    // final JPanel p = new JPanel(new MigLayout("ins 5,wrap 2",
    // "[][grow,fill]", "[grow,fill]"));
    // p.add(new
    // JLabel("Please enter your router information as far as possible."),
    // "spanx");
    // p.add(new JLabel("Model Name"));
    // p.add(this.txtName);
    //
    // p.add(new JLabel("Manufactor"));
    // p.add(this.txtManufactor);
    //
    // // p.add(new JLabel("Firmware"));
    // // p.add(this.txtFirmware);
    //
    // p.add(new JLabel("Webinterface IP"));
    // p.add(this.txtIP);
    //
    // p.add(new JLabel("Webinterface User"));
    // p.add(this.txtUser);
    // p.add(new JLabel("Webinterface Password"));
    // p.add(this.txtPass);
    // this.txtUser.setText(this.getPlugin().getUser());
    // this.txtPass.setText(this.getPlugin().getPassword());
    // this.txtName.setText(this.getPlugin().getRouterName());
    // this.txtIP.setText(this.getPlugin().getRouterIP());
    // while (true) {
    //
    // final ContainerDialog routerInfo = new ContainerDialog(0,
    // "Enter Router Information", p, null, "Continue", null);
    //
    // try {
    // Dialog.getInstance().showDialog(routerInfo);
    // } catch (DialogNoAnswerException e) {
    // return -1;
    // }
    // try {
    // if (this.txtUser.getText().trim().length() < 2 ||
    // this.txtPass.getText().trim().length() < 2) {
    //
    // Dialog.getInstance().showConfirmDialog(0, "Warning",
    // "Username and Password are not set. In most cases, \r\nthese information is required for a successful reconnection.\r\n\r\nContinue anyway?");
    //
    // }
    //
    // if (!RouterUtils.checkPort(this.txtIP.getText().trim())) {
    // Dialog.getInstance().showConfirmDialog(0, "Warning",
    // "There is no Webinterface at http://" + this.txtIP.getText() +
    // "\r\nAre you sure that the Router IP is correct?\r\nA correct Router IP is required to find the correct settings.\r\n\r\nContinue anyway?");
    // }
    // String man = this.txtManufactor.getText().trim();
    // String name = this.txtName.getText().trim();
    // ArrayList<TestScript> tests = this.scanOfflineRouters(name.length() > 0 ?
    // ".*" + name.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null,
    // man.toLowerCase().length() > 0 ? ".*" +
    // man.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null);
    // ret = runTests(tests);
    // if (ret <= 0) {
    // try {
    // Dialog.getInstance().showConfirmDialog(0, "Warning",
    // "Could not find correct settings based on your inputs?\r\n\r\nTry again?");
    // continue;
    // } catch (DialogClosedException e) {
    // return -1;
    // } catch (DialogCanceledException e) {
    // return -1;
    // }
    // } else {
    // return ret;
    // }
    // } catch (DialogClosedException e) {
    // return -1;
    // } catch (DialogCanceledException e) {
    // continue;
    // }
    //
    // }
    //
    // }

    private int runTests(ArrayList<TestScript> tests, ProcessCallBack processCallBack) throws InterruptedException {
        int fastestTime = -1;
        TestScript fastestScript = null;
        for (int i = 0; i < tests.size(); i++) {

            TestScript test = tests.get(i);
            processCallBack.setStatusString(T._.jd_controlling_reconnect_plugins_liveheader_LiveHeaderDetectionWizard_runTests((i + 1), tests.size(), test.getManufactor() + " - " + test.getModel()));
            JsonConfig.create(ReconnectConfig.class).setGlobalFailedCounter(0);
            JsonConfig.create(ReconnectConfig.class).setFailedCounter(0);
            JsonConfig.create(ReconnectConfig.class).setGlobalSuccessCounter(0);
            JsonConfig.create(ReconnectConfig.class).setSuccessCounter(0);

            if (test.run(getPlugin())) {
                // return first Script found
                System.out.println("Succeedtime: " + test.getTestDuration());
                if (test.getTestDuration() < fastestTime || fastestTime < 0) {
                    fastestScript = test;
                    fastestTime = test.getTestDuration();
                }
            }
            processCallBack.setProgress((i + 1) * 100 / tests.size());
        }
        JsonConfig.create(ReconnectConfig.class).setGlobalFailedCounter(0);
        JsonConfig.create(ReconnectConfig.class).setFailedCounter(0);
        JsonConfig.create(ReconnectConfig.class).setGlobalSuccessCounter(0);
        JsonConfig.create(ReconnectConfig.class).setSuccessCounter(0);
        if (fastestScript != null) {
            JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterIP(fastestScript.getRouterIP());
            JsonConfig.create(LiveHeaderReconnectSettings.class).setUserName(fastestScript.getUser());
            JsonConfig.create(LiveHeaderReconnectSettings.class).setPassword(fastestScript.getPassword());
            JsonConfig.create(LiveHeaderReconnectSettings.class).setScript(fastestScript.getScript());
            JsonConfig.create(ReconnectConfig.class).setSecondsBeforeFirstIPCheck(Math.min(5, fastestScript.getTestDuration() / 2));
            JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForIPChange(Math.max(fastestScript.getTestDuration() * 2, 30));
        }
        return fastestTime;
    }

    public int runOnlineScan(ProcessCallBack processCallBack) {
        try {
            try {
                recoll.isAlive();
            } catch (Throwable e) {
                Dialog.getInstance().showErrorDialog(T._.LiveHeaderDetectionWizard_runOnlineScan_notalive());
                return -1;
            }
            processCallBack.setStatusString(T._.LiveHeaderDetectionWizard_runOnlineScan_collect());
            collectInfo();
            specialCollectInfo();
            while (true) {
                userConfirm();
                try {
                    if (username.trim().length() < 2 || password.trim().length() < 2) {

                        Dialog.getInstance().showConfirmDialog(0, _GUI._.literall_warning(), T._.LiveHeaderDetectionWizard_runOnlineScan_warning_logins(), NewTheme.I().getIcon("warning", 32), _GUI._.literally_yes(), _GUI._.literally_edit());
                    }

                    if (!isValidRouterIP(gatewayAdressIP)) {

                        Dialog.getInstance().showConfirmDialog(0, _GUI._.literall_warning(), T._.LiveHeaderDetectionWizard_runOnlineScan_warning_badip(gatewayAdressHost), NewTheme.I().getIcon("warning", 32), _GUI._.literally_yes(), _GUI._.literally_edit());

                    }
                    break;
                } catch (DialogCanceledException e) {
                    continue;
                }
            }
            scanRemoteInfo();
            specials();
            ArrayList<TestScript> list = downloadTestScripts();

            return runTests(list, processCallBack);

        } catch (Exception e) {
            e.printStackTrace();
            Dialog.getInstance().showExceptionDialog(_GUI._.literall_error(), e.getMessage(), e);
        }

        return -1;
    }

    private void specialCollectInfo() {

        // speedports
        if ("speedport.ip".equalsIgnoreCase(gatewayAdressHost)) {

            try {
                String status = new Browser().getPage("http://speedport.ip/top_status.stm");
                String fw = new Regex(status, "runtime_code_version=\"(.*?)\"").getMatch(0);
                if (fw != null) firmware = fw;
                String name = new Regex(status, "var prodname=\"(.*?)\"").getMatch(0);
                if (name != null) {
                    routerName = name.replace("_", " ");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // speedports

            // extract name from sourc

            try {
                String status = new Browser().getPage("https://speedport.ip/top_status.stm");
                String name = new Regex(status, "var prodname=\"(.*?)\"").getMatch(0);
                if (name != null) {
                    routerName = name.replace("_", " ");
                }
                String fw = new Regex(status, "runtime_code_version=\"(.*?)\"").getMatch(0);
                if (fw != null) firmware = fw;
            } catch (Exception e) {

            }

        }
    }

    private void specials() {

    }

    private String trim(final String stringToTrim) {

        return stringToTrim == null ? null : stringToTrim.trim();
    }

    private void scanRemoteInfo() {

        final Browser br = new Browser();
        try {
            this.sslResponse = br.getPage("https://" + this.gatewayAdressHost);

            this.sslTitle = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.sslPTagsCount = br.toString().split("<p>").length;
            this.sslFrameTagCount = br.toString().split("<frame").length;
            // get favicon and build hash
            try {
                final BufferedImage image = FavIconController.getInstance().downloadFavIcon(this.gatewayAdressHost);
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
            this.response = br.getPage("http://" + this.gatewayAdressHost);

            this.title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.pTagsCount = br.toString().split("<p>").length;
            this.frameTagCount = br.toString().split("<frame").length;
            // get favicon and build hash
            try {
                final BufferedImage image = FavIconController.getInstance().downloadFavIcon(this.gatewayAdressHost);
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

    private ArrayList<TestScript> downloadTestScripts() {
        ArrayList<TestScript> ret = new ArrayList<TestScript>();
        ret = downloadTestScriptsByAutoDetectValues();
        return ret;
    }

    private ArrayList<TestScript> downloadTestScriptsByAutoDetectValues() {

        RouterData rd = getRouterData();
        // debug
        rd.setMac(null);
        ArrayList<RouterData> scripts = recoll.findRouter(rd);

        return convert(scripts);

    }

    private RouterData getRouterData() {
        RouterData search = new RouterData();
        search.setException(exception);
        search.setFavIconHash(favIconHash);
        search.setFirmware(firmware);
        search.setFrameTagCount(frameTagCount);
        search.setMac(mac);
        search.setManufactor(manufactor);
        search.setpTagsCount(pTagsCount);
        search.setResponse(response);
        search.setResponseCode(responseCode);
        search.setResponseHeaders(responseHeaders);
        search.setRouterIP(gatewayAdressIP);
        search.setRouterHost(gatewayAdressHost);
        search.setRouterName(routerName);
        search.setSslException(sslException);
        search.setSslFavIconHash(sslFavIconHash);
        search.setSslFrameTagCount(sslFrameTagCount);
        search.setSslPTagsCount(sslPTagsCount);
        search.setSslResponse(sslResponse);
        search.setSslResponseCode(sslResponseCode);
        search.setSslResponseHeaders(sslResponseHeaders);
        search.setSslTitle(sslTitle);
        search.setTitle(title);
        return search;
    }

    private ArrayList<TestScript> convert(ArrayList<RouterData> scripts) {

        ArrayList<TestScript> ret = new ArrayList<TestScript>(scripts.size());
        for (RouterData dat : scripts) {
            TestScript s = new TestScript(dat.getManufactor(), dat.getRouterName());
            s.setPassword(password);
            s.setRouterIP(gatewayAdressHost);
            s.setScript(dat.getScript());
            s.setUser(username);
            ret.add(s);
        }
        return ret;
    }

    private void userConfirm() throws DialogClosedException, DialogCanceledException {
        while (true) {
            DataCompareDialog dcd = new DataCompareDialog(gatewayAdressHost, firmware, manufactor, routerName, JsonConfig.create(LiveHeaderReconnectSettings.class).getUserName(), JsonConfig.create(LiveHeaderReconnectSettings.class).getPassword());
            Dialog.getInstance().showDialog(dcd);
            username = dcd.getUsername();
            password = dcd.getPassword();
            manufactor = dcd.getManufactor();
            routerName = dcd.getRouterName();
            firmware = dcd.getFirmware();
            try {
                gatewayAdress = InetAddress.getByName(dcd.getHostName());
                gatewayAdressIP = gatewayAdress.getHostAddress();
                gatewayAdressHost = gatewayAdress.getHostName();
                break;
            } catch (IOException e) {
                Dialog.getInstance().showConfirmDialog(0, _GUI._.literall_error(), T._.LiveHeaderDetectionWizard_runOnlineScan_warning_badhost(dcd.getHostName()), NewTheme.I().getIcon("error", 32), _GUI._.literally_edit(), null);

            }
        }
    }

    private UpnpRouterDevice myUpnpDevice = null;

    private String           routerName;

    private String           gatewayAdressHost;

    private String           gatewayAdressIP;

    private void collectInfo() throws UnknownHostException {
        final UPNPRouterPlugin upnp = (UPNPRouterPlugin) ReconnectPluginController.getInstance().getPluginByID(UPNPRouterPlugin.ID);
        ArrayList<UpnpRouterDevice> devices = new ArrayList<UpnpRouterDevice>();
        try {
            devices = upnp.scanDevices();
        } catch (final IOException e) {
        }
        String gatewayIP = JsonConfig.create(LiveHeaderReconnectSettings.class).getRouterIP();

        if (!isValidRouterIP(gatewayIP)) {
            final InetAddress ia = RouterUtils.getAddress(true);
            if (ia != null && isValidRouterIP(ia.getHostAddress())) {
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
        routerName = JsonConfig.create(LiveHeaderReconnectSettings.class).getRouterName();
        if (routerName.trim().length() == 0 || "unknown".equalsIgnoreCase(routerName)) {
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
    }

    public static boolean isValidRouterIP(String gatewayIP) {
        boolean localip = isLocalIP(gatewayIP);
        if (!localip) {
            try {
                localip = isLocalIP(InetAddress.getByName(gatewayIP).getHostAddress());
            } catch (UnknownHostException e) {

            }
        }
        if (!localip) return false;
        return RouterUtils.checkPort(gatewayIP);
    }

    public static boolean isLocalIP(String ip) {
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            final String parts[] = ip.split("\\.");
            if (parts.length == 4) {
                /* filter private networks */
                final int n1 = Integer.parseInt(parts[0]);
                final int n2 = Integer.parseInt(parts[1]);
                // final int n3 = Integer.parseInt(parts[2]);
                // final int n4 = Integer.parseInt(parts[3]);
                /* 10.0.0.0-10.255.255.255 */
                if (n1 == 10) { return true; }
                /* 192.168.0.0 - 192.168.255.255 */
                if (n1 == 192 && n2 == 168) { return true; }
                /* 172.16.0.0 - 172.31.255.255 */
                if (n1 == 172 && n2 >= 16 && n2 <= 31) { return true; }

            }
        }
        return false;
    }

    private ArrayList<TestScript> scanOfflineRouters(final String name, final String manufactor) throws InterruptedException {
        progress.setStatusMessage("Scan available Scripts");
        final ArrayList<String[]> scripts = LiveHeaderReconnect.getLHScripts();
        final ArrayList<String[]> filtered = new ArrayList<String[]>();
        String man, mod;
        for (final String[] script : scripts) {
            man = script[0].trim().toLowerCase();
            mod = script[1].trim().toLowerCase();
            if (name != null && name.trim().length() > 2) {
                if (!mod.matches(name)) {
                    continue;
                }
            }
            if (manufactor != null && manufactor.trim().length() > 2) {
                if (!man.matches(manufactor)) {
                    continue;
                }
            }
            filtered.add(script);
        }
        ArrayList<TestScript> ret = new ArrayList<TestScript>();
        for (final String[] script : filtered) {
            TestScript test = new TestScript(script[0], script[1]);
            ret.add(test);

            test.setRouterIP(this.txtIP.getText());
            test.setUser(this.txtUser.getText());
            test.setPassword(this.txtPass.getText());
            test.setScript(script[2]);

        }

        return ret;
    }

}