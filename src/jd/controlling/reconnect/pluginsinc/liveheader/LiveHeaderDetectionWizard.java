package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import jd.controlling.faviconcontroller.FavIcons;
import jd.controlling.reconnect.ProcessCallBack;
import jd.controlling.reconnect.ProcessCallBackAdapter;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.pluginsinc.liveheader.recoll.AddRouterResponse;
import jd.controlling.reconnect.pluginsinc.liveheader.recoll.RecollController;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript;
import jd.controlling.reconnect.pluginsinc.liveheader.validate.ScriptValidationExeption;
import jd.controlling.reconnect.pluginsinc.liveheader.validate.Scriptvalidator;
import jd.controlling.reconnect.pluginsinc.upnp.UPNPRouterPlugin;
import jd.controlling.reconnect.pluginsinc.upnp.cling.UpnpRouterDevice;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectDialog;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public class LiveHeaderDetectionWizard {
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
    private String                  sslTagFootprint;
    private String                  tagFootprint;
    private LogSource               logger;

    public LiveHeaderDetectionWizard() {
        logger = LogController.getInstance().getLogger(LiveHeaderDetectionWizard.class.getName());
    }

    // public static void main(String[] args) {
    //
    // try {
    // RecollInterface r = new
    // RemoteClient(LiveHeaderDetectionWizard.UPDATE3_JDOWNLOADER_ORG_RECOLL).getFactory().newInstance(RecollInterface.class);
    //
    // r.isAlive();
    //
    // System.out.println(r.isReconnectPossible(null));
    // // System.out.println(rd.getAvgScD());
    // } catch (IllegalArgumentException e) {
    // e.printStackTrace();
    // } catch (ParsingException e) {
    // e.printStackTrace();
    // } catch (Throwable e) {
    // e.printStackTrace();
    // }
    //
    // }
    private LiveHeaderReconnect getPlugin() {
        return (LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID);
    }

    // public int runOfflineScan() throws InterruptedException {
    // int ret = -1;
    // // final java.util.List<String[]> scripts =
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
    // NewUIO.I().showDialog(routerInfo);
    // } catch (DialogNoAnswerException e) {
    // return -1;
    // }
    // try {
    // if (this.txtUser.getText().trim().length() < 2 ||
    // this.txtPass.getText().trim().length() < 2) {
    //
    // NewUIO.I().showConfirmDialog(0, "Warning",
    // "Username and Password are not set. In most cases, \r\nthese information is required for a successful reconnection.\r\n\r\nContinue
    // anyway?");
    //
    // }
    //
    // if (!RouterUtils.checkPort(this.txtIP.getText().trim())) {
    // NewUIO.I().showConfirmDialog(0, "Warning",
    // "There is no Webinterface at http://" + this.txtIP.getText() +
    // "\r\nAre you sure that the Router IP is correct?\r\nA correct Router IP is required to find the correct settings.\r\n\r\nContinue
    // anyway?");
    // }
    // String man = this.txtManufactor.getText().trim();
    // String name = this.txtName.getText().trim();
    // java.util.List<RouterData> tests = this.scanOfflineRouters(name.length() > 0 ?
    // ".*" + name.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null,
    // man.toLowerCase().length() > 0 ? ".*" +
    // man.toLowerCase().replaceAll("\\W", ".*?") + ".*" : null);
    // ret = runTests(tests);
    // if (ret <= 0) {
    // try {
    // NewUIO.I().showConfirmDialog(0, "Warning",
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
    private java.util.List<ReconnectResult> runTests(java.util.List<RouterData> tests, ProcessCallBack processCallBack) throws InterruptedException, DialogClosedException, DialogCanceledException {
        boolean pre = JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled();
        try {
            if (pre) {
                UIOManager.I().showConfirmDialog(0, _GUI.T.literally_warning(), T.T.ipcheck());
                JsonConfig.create(ReconnectConfig.class).setIPCheckGloballyDisabled(false);
            }
            java.util.List<RouterData> bestMatches = filterBestMatches(tests);
            java.util.List<ReconnectResult> ret = testList(bestMatches, processCallBack);
            if (ret != null && ret.size() > 0 && JsonConfig.create(LiveHeaderReconnectSettings.class).isAutoSearchBestMatchFilterEnabled()) {
                return ret;
            }
            // test all if best matches did not succeed
            ret = testList(tests, processCallBack);
            return ret;
        } finally {
            JsonConfig.create(ReconnectConfig.class).setIPCheckGloballyDisabled(pre);
            if (pre) {
                UIOManager.I().showMessageDialog(T.T.ipcheckreverted());
            }
        }
    }

    private java.util.List<RouterData> filterBestMatches(java.util.List<RouterData> tests) {
        int total = 0;
        for (Iterator<RouterData> it = tests.iterator(); it.hasNext();) {
            total += it.next().getPriorityIndicator();
        }
        float avg = total / (float) tests.size();
        java.util.List<RouterData> ret = new ArrayList<RouterData>();
        for (Iterator<RouterData> it = tests.iterator(); it.hasNext();) {
            RouterData next = it.next();
            if (isTopMatch(next, avg)) {
                ret.add(next);
                it.remove();
            }
        }
        return ret;
    }

    private String getRegex(final String routerName) {
        if (routerName == null || routerName.trim().length() == 0) {
            return null;
        }
        final String ret = routerName.replaceAll("[^a-zA-Z0-9]+", ".*");
        // add wildcard between number and non-numbers
        final StringBuilder r = new StringBuilder();
        r.append(ret.charAt(0));
        for (int i = 1; i < ret.length(); i++) {
            final char c = ret.charAt(i - 1);
            final char c1 = ret.charAt(i);
            if (Character.isDigit(c) && !Character.isDigit(c1) || !Character.isDigit(c) && Character.isDigit(c1)) {
                r.append(".*");
            }
            r.append(c1);
        }
        return r.toString();
    }

    private boolean isTopMatch(RouterData rd, float avg) {
        // many matches with this script
        if (rd.getPriorityIndicator() > avg * 2) {
            return true;
        }
        if (!matches(firmware, rd.getFirmware())) {
            return false;
        }
        if (!matches(routerName, rd.getRouterName())) {
            return false;
        }
        if (!matches(manufactor, rd.getManufactor())) {
            return false;
        }
        return true;
    }

    private boolean matches(String userInput, String database) {
        String regex = getRegex(userInput);
        if (regex != null) {
            //
            if (database == null) {
                return false;
            }
            String m = new Regex(userInput, regex).getMatch(-1);
            return database.equals(m);
        }
        // no userinput for this value
        return true;
    }

    public java.util.List<ReconnectResult> testList(java.util.List<RouterData> tests, ProcessCallBack processCallBack) throws InterruptedException, DialogClosedException, DialogCanceledException {
        if (username.trim().length() < 2 || password.trim().length() < 2) {
            for (int i = 0; i < tests.size(); i++) {
                String sc = tests.get(i).getScript().toLowerCase(Locale.ENGLISH);
                if (sc.contains("%%%username%%%") || sc.contains("%%%user%%%") || sc.contains("%%%pass%%%") || sc.contains("%%%password%%%")) {
                    // TODO
                    DataCompareDialog dcd = new DataCompareDialog(gatewayAdressHost, firmware, manufactor, routerName, JsonConfig.create(LiveHeaderReconnectSettings.class).getUserName(), JsonConfig.create(LiveHeaderReconnectSettings.class).getPassword());
                    dcd.setLoginsOnly(true);
                    DataCompareDialog impl = UIOManager.I().show(null, dcd);
                    username = impl.getUsername();
                    password = impl.getPassword();
                    break;
                }
            }
        }
        java.util.List<ReconnectResult> ret = new ArrayList<ReconnectResult>();
        for (int i = 0; i < tests.size(); i++) {
            RouterData test = tests.get(i);
            processCallBack.setStatusString(getPlugin(), T.T.jd_controlling_reconnect_plugins_liveheader_LiveHeaderDetectionWizard_runTests((i + 1), tests.size(), test.getManufactor() + " - " + test.getRouterName()));
            String sc = tests.get(i).getScript().toLowerCase(Locale.ENGLISH);
            if ((username.trim().length() < 2 && (sc.contains("%%%username%%%") || sc.contains("%%%user%%%"))) || (password.trim().length() < 2 && (sc.contains("%%%pass%%%") || sc.contains("%%%password%%%")))) {
                // script requires data which is not available
                processCallBack.setProgress(getPlugin(), Math.min(99, (i + 1) * 100 / tests.size()));
                Thread.sleep(1000);
                continue;
                // System.out.println("WILL FAIL");
            }
            ReconnectResult res;
            try {
                if (processCallBack.isMethodConfirmEnabled()) {
                    LiveHeaderScriptConfirmDialog d = new LiveHeaderScriptConfirmDialog(test, gatewayAdressHost, test.getRouterName()) {
                        // @Override
                        protected String getDontShowAgainLabelText() {
                            return _GUI.T.UPNPRouterPlugin_accept_all();
                        }

                        @Override
                        public String getDontShowAgainKey() {
                            return null;
                        }
                    };
                    OKCancelCloseUserIODefinition answer = UIOManager.I().show(OKCancelCloseUserIODefinition.class, d);
                    if (answer.getCloseReason() == CloseReason.OK) {
                        if (answer.isDontShowAgainSelected()) {
                            processCallBack.setMethodConfirmEnabled(false);
                        }
                    } else if (answer.getCloseReason() == CloseReason.CLOSE) {
                        break;
                    } else if (answer.getCloseReason() == CloseReason.CANCEL) {
                        if (answer.isDontShowAgainSelected()) {
                            break;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                LiveHeaderInvoker inv = new LiveHeaderInvoker(getPlugin(), test.getScript(), username, password, gatewayAdressHost, routerName != null && routerName.trim().length() > 0 ? routerName : test.getRouterName());
                res = inv.validate(test);
                if (res != null && res.isSuccess()) {
                    ret.add(res);
                    processCallBack.setStatus(this, ret);
                    if (i < tests.size() - 1) {
                        if (ret.size() == 1) {
                            if (!UIOManager.I().showConfirmDialog(0, _GUI.T.LiveHeaderDetectionWizard_testList_firstSuccess_title(), _GUI.T.LiveHeaderDetectionWizard_testList_firstsuccess_msg(TimeFormatter.formatMilliSeconds(res.getSuccessDuration(), 0)), new AbstractIcon(IconKey.ICON_OK, 32), _GUI.T.LiveHeaderDetectionWizard_testList_ok(), _GUI.T.LiveHeaderDetectionWizard_testList_use())) {
                                break;
                            }
                        }
                        return ret;
                    }
                } else {
                }
            } catch (ReconnectException e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            processCallBack.setProgress(getPlugin(), Math.min(99, (i + 1) * 100 / tests.size()));
        }
        // JsonConfig.create(ReconnectConfig.class).setGlobalFailedCounter(0);
        // JsonConfig.create(ReconnectConfig.class).setFailedCounter(0);
        // JsonConfig.create(ReconnectConfig.class).setGlobalSuccessCounter(0);
        // JsonConfig.create(ReconnectConfig.class).setSuccessCounter(0);
        // if (fastestScript != null) {
        // JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterIP(fastestScript.getRouterIP());
        // JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterData(fastestScript);
        // JsonConfig.create(LiveHeaderReconnectSettings.class).setUserName(fastestScript.getUsername());
        // JsonConfig.create(LiveHeaderReconnectSettings.class).setPassword(fastestScript.getPassword());
        // JsonConfig.create(LiveHeaderReconnectSettings.class).setScript(fastestScript.getScript());
        // JsonConfig.create(ReconnectConfig.class).setSecondsBeforeFirstIPCheck((int)
        // Math.min(5, fastestScript.getOfflineDuration() + 1000));
        // JsonConfig.create(ReconnectConfig.class).setSecondsToWaitForIPChange(Math.max(fastestScript.getTestDuration()
        // * 4, 30));
        // processCallBack.setProgress(99);
        // processCallBack.showMessage(T.T.autodetection_success(TimeFormatter.formatMilliSeconds(fastestScript.getTestDuration(),
        // 0)));
        // return fastestTime;
        // }
        return ret;
    }

    public java.util.List<ReconnectResult> runOnlineScan(ProcessCallBack processCallBack) throws InterruptedException, UnknownHostException {
        try {
            // wait until we are online
            processCallBack.setStatusString(this, _GUI.T.LiveaheaderDetection_wait_for_online());
            IPController.getInstance().waitUntilWeAreOnline();
            processCallBack.setStatusString(getPlugin(), T.T.LiveHeaderDetectionWizard_runOnlineScan_collect());
            if (!RecollController.getInstance().isAlive()) {
                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, T.T.LiveHeaderDetectionWizard_runOnlineScan_notavailable_t(), T.T.LiveHeaderDetectionWizard_runOnlineScan_notavailable_mm(), new AbstractIcon(IconKey.ICON_ERROR, 32), null, _JDT.T.lit_hide());
                throw new InterruptedException("RecollServ not available");
            }
            collectInfo();
            specialCollectInfo();
            while (true) {
                userConfirm();
                if (!IP.isValidRouterIP(gatewayAdressIP)) {
                    if (!UIOManager.I().showConfirmDialog(0, _GUI.T.literally_warning(), T.T.LiveHeaderDetectionWizard_runOnlineScan_warning_badip(gatewayAdressHost), new AbstractIcon(IconKey.ICON_WARNING, 32), _GUI.T.literally_yes(), _GUI.T.literally_edit())) {
                        continue;
                    }
                }
                break;
            }
            scanRemoteInfo();
            specials();
            java.util.List<RouterData> list = downloadRouterDatasByAutoDetectValues();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("amount", list.size() + "");
            return runTests(list, processCallBack);
        } catch (DialogNoAnswerException e) {
            throw new InterruptedException();
        } catch (InterruptedException e) {
            throw e;
        }
    }

    private void specialCollectInfo() throws InterruptedException {
        // speedports
        if ("speedport.ip".equalsIgnoreCase(gatewayAdressHost)) {
            try {
                String status = getBrowser().getPage("http://speedport.ip/top_status.stm");
                String fw = new Regex(status, "runtime_code_version=\"(.*?)\"").getMatch(0);
                if (fw != null) {
                    firmware = fw;
                }
                String name = new Regex(status, "var prodname=\"(.*?)\"").getMatch(0);
                if (name != null) {
                    routerName = name.replace("_", " ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
                // speedports
            }
            // extract name from sourc
            try {
                String status = getBrowser().getPage("https://speedport.ip/top_status.stm");
                String name = new Regex(status, "var prodname=\"(.*?)\"").getMatch(0);
                if (name != null) {
                    routerName = name.replace("_", " ");
                }
                String fw = new Regex(status, "runtime_code_version=\"(.*?)\"").getMatch(0);
                if (fw != null) {
                    firmware = fw;
                }
            } catch (Exception e) {
            }
        }
    }

    private void specials() {
    }

    private String trim(final String stringToTrim) {
        return stringToTrim == null ? null : stringToTrim.trim();
    }

    protected void scanRemoteInfo() {
        final Browser br = getBrowser();
        try {
            this.sslResponse = br.getPage("https://" + this.gatewayAdressHost);
            sslTagFootprint = createHtmlFootprint(sslResponse.toLowerCase(Locale.ENGLISH));
            this.sslTitle = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.sslPTagsCount = sslResponse.split("<p>").length;
            this.sslFrameTagCount = sslResponse.split("<frame").length;
            // get favicon and build hash
            FileOutputStream fos = null;
            try {
                final BufferedImage image = FavIcons.downloadFavIcon(this.gatewayAdressHost);
                final File imageFile = Application.getTempResource("routerfav.png");
                FileCreationManager.getInstance().delete(imageFile, null);
                imageFile.deleteOnExit();
                fos = new FileOutputStream(imageFile);
                ImageIO.write(image, "png", fos);
                fos.flush();
                fos.close();
                this.sslFavIconHash = Hash.getMD5(imageFile);
            } catch (final Exception e) {
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e) {
            this.sslException = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        // ToDO: filter dynamic contents in headers
        URLConnectionAdapter con = br.getHttpConnection();
        if (con != null) {
            this.sslResponseCode = con.getResponseCode();
            this.sslResponseHeaders = new HashMap<String, String>();
            for (final Entry<String, List<String>> next : con.getHeaderFields().entrySet()) {
                for (final String value : next.getValue()) {
                    this.sslResponseHeaders.put(next.getKey().toLowerCase(), value);
                }
            }
            filterHeaders(sslResponseHeaders);
        }
        try {
            this.response = br.getPage("http://" + this.gatewayAdressHost);
            tagFootprint = createHtmlFootprint(response.toLowerCase(Locale.ENGLISH));
            this.title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.pTagsCount = response.split("<p>").length;
            this.frameTagCount = response.split("<frame").length;
            // get favicon and build hash
            FileOutputStream fos = null;
            try {
                final BufferedImage image = FavIcons.downloadFavIcon(this.gatewayAdressHost);
                final File imageFile = Application.getTempResource("routerfav.png");
                FileCreationManager.getInstance().delete(imageFile, null);
                imageFile.deleteOnExit();
                fos = new FileOutputStream(imageFile);
                ImageIO.write(image, "png", fos);
                fos.flush();
                fos.close();
                this.favIconHash = Hash.getMD5(imageFile);
            } catch (final Exception e) {
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final IOException e) {
            this.exception = e.getClass().getSimpleName() + ": " + e.getMessage();
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
            filterHeaders(responseHeaders);
        }
    }

    final static private HashSet<String> ALLOWED_HEADER_KEYS = new HashSet<String>();
    static {
        ALLOWED_HEADER_KEYS.add("Accept-Ranges".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Allow".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Cache-Control".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Connection".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Encoding".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Language".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Length".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Location".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-MD5".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Disposition".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Range".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Content-Type".toLowerCase());
        ALLOWED_HEADER_KEYS.add("ETag".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Link".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Location".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Pragma".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Refresh".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Retry-After".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Server".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Trailer".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Transfer-Encoding".toLowerCase());
        ALLOWED_HEADER_KEYS.add("Warning".toLowerCase());
    }

    private void filterHeaders(HashMap<String, String> headerMap) {
        Entry<String, String> next;
        for (Iterator<Entry<String, String>> it = headerMap.entrySet().iterator(); it.hasNext();) {
            next = it.next();
            if (!ALLOWED_HEADER_KEYS.contains(next.getKey().toLowerCase())) {
                it.remove();
            }
        }
    }

    private Browser getBrowser() {
        Browser br = new Browser();
        br.setProxy(HTTPProxy.NONE);
        return br;
    }

    private String createHtmlFootprint(String sslResponse2) {
        if (sslResponse2 == null) {
            return null;
        }
        String str = Regex.replace(sslResponse2, "<\\!\\-\\-.*?\\-\\->", "");
        str = Regex.replace(str, ">.*?<", "><");
        str = Regex.replace(str, "<(\\w+) .*?>", "<$1>");
        return str;
    }

    private java.util.List<RouterData> downloadRouterDatasByAutoDetectValues() throws InterruptedException {
        RouterData rd = getRouterData();
        java.util.List<RouterData> scripts = RecollController.getInstance().findRouter(rd);
        // final java.util.List<RouterData> unique = toUnique(scripts);
        return scripts;
    }

    public static ArrayList<RouterData> toUnique(java.util.List<RouterData> scripts) {
        final HashMap<String, RouterData> helper = new HashMap<String, RouterData>();
        final ArrayList<RouterData> unique = new ArrayList<RouterData>(scripts.size());
        for (final RouterData e : scripts) {
            if (e.getScript() != null) {
                String script = prepScript(e.getScript());
                System.out.println("____________________________________________________________");
                System.out.println(Hash.getMD5(script) + " : " + script + " " + script.toCharArray().length);
                if (!helper.containsKey(script)) {
                    System.out.println("ADD");
                    helper.put(script, e);
                    unique.add(e);
                    e.setPriorityIndicator(e.getSuccess() - e.getFailed() + e.getCommitDupe());
                } else {
                    System.out.println("DUPE");
                    final RouterData m = helper.get(script);
                    m.setPriorityIndicator(m.getPriorityIndicator() + 1 + e.getSuccess() - e.getFailed() + e.getCommitDupe());
                }
            }
        }
        return unique;
    }

    private static String prepScript(String script) {
        script = script.toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "");
        // script = Pattern.compile("\\[\\[\\[(/?)hsrc\\]\\]\\]",
        // Pattern.CASE_INSENSITIVE).matcher(script).replaceAll("[[[$1HSRC]]]");
        // script = Pattern.compile("\\[\\[\\[(/?)request",
        // Pattern.CASE_INSENSITIVE).matcher(script).replaceAll("[[[$1REQUEST");
        // script = Pattern.compile("\\[\\[\\[(/?)step",
        // Pattern.CASE_INSENSITIVE).matcher(script).replaceAll("[[[$1STEP");
        // script = Pattern.compile("\\[\\[\\[(/?)response",
        // Pattern.CASE_INSENSITIVE).matcher(script).replaceAll("[[[$1RESPONSE");
        // script = Pattern.compile("\\[\\[\\[(/?)parse",
        // Pattern.CASE_INSENSITIVE).matcher(script).replaceAll("[[[$1PARSE");
        return script;
    }

    private RouterData getRouterData() {
        RouterData search = new RouterData();
        search.setSslTagFootprint(sslTagFootprint);
        search.setTagFootprint(tagFootprint);
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

    private void userConfirm() throws DialogClosedException, DialogCanceledException {
        while (true) {
            System.out.println("UserConfirm");
            DataCompareDialog dcd = new DataCompareDialog(gatewayAdressHost, firmware, manufactor, routerName, JsonConfig.create(LiveHeaderReconnectSettings.class).getUserName(), JsonConfig.create(LiveHeaderReconnectSettings.class).getPassword());
            // dcd.setLoginsText(T.T.LiveHeaderDetectionWizard_userConfirm_loginstext());
            DataCompareDialog impl = UIOManager.I().show(null, dcd);
            dcd.throwCloseExceptions();
            username = impl.getUsername();
            password = impl.getPassword();
            manufactor = impl.getManufactor();
            routerName = impl.getRouterName();
            firmware = impl.getFirmware();
            try {
                gatewayAdress = InetAddress.getByName(impl.getHostName());
                gatewayAdressIP = gatewayAdress.getHostAddress();
                gatewayAdressHost = gatewayAdress.getHostName();
                break;
            } catch (IOException e) {
                new ConfirmDialog(0, _GUI.T.literall_error(), T.T.LiveHeaderDetectionWizard_runOnlineScan_warning_badhost(dcd.getHostName()), new AbstractIcon(IconKey.ICON_ERROR, 32), _GUI.T.literally_edit(), null).show().throwCloseExceptions();
            }
        }
    }

    private UpnpRouterDevice myUpnpDevice = null;
    private String           routerName;
    private String           gatewayAdressHost;
    private String           gatewayAdressIP;

    private void collectInfo() throws UnknownHostException, InterruptedException {
        final UPNPRouterPlugin upnp = (UPNPRouterPlugin) ReconnectPluginController.getInstance().getPluginByID(UPNPRouterPlugin.ID);
        java.util.List<UpnpRouterDevice> devices = upnp.getCachedDevices();
        if (devices == null) {
            devices = upnp.getDevices();
        }
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
            manufactor = RecollController.getInstance().getManufactor(mac);
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
        if (myUpnpDevice != null) {
            if (myUpnpDevice.getModelname() != null) {
                routerName = myUpnpDevice.getModelname();
            } else if (myUpnpDevice.getFriendlyname() != null) {
                routerName = myUpnpDevice.getFriendlyname();
            }
        }
    }

    public RouterData collectRouterDataInfo() throws InterruptedException, UnknownHostException {
        try {
            collectInfo();
            specialCollectInfo();
            scanRemoteInfo();
            specials();
            if (routerName == null || routerName.trim().length() == 0 || "unknown".equalsIgnoreCase(routerName)) {
                // try to convert domain to routername
                if (!gatewayAdressHost.equals(gatewayAdressIP)) {
                    routerName = gatewayAdressHost;
                    int i = routerName.lastIndexOf(".");
                    if (i > 0) {
                        routerName = routerName.substring(0, i);
                    }
                }
            }
            RouterData rd = getRouterData();
            return rd;
        } catch (InterruptedException e) {
            logger.log(e);
            throw e;
        }
    }

    public void sendRouter(ProcessCallBackAdapter processCallBack) throws UnknownHostException, InterruptedException {
        try {
            LiveHeaderReconnectSettings settings = JsonConfig.create(LiveHeaderReconnectSettings.class);
            String script = settings.getScript();
            // wait until we are online
            processCallBack.setProgress(this, -1);
            processCallBack.setStatusString(this, _GUI.T.LiveaheaderDetection_wait_for_online());
            IPController.getInstance().waitUntilWeAreOnline();
            if (!RecollController.getInstance().isAlive()) {
                UIOManager.I().showErrorMessage(_GUI.T.LiveHeaderDetectionWizard_sendRouter_na());
                return;
            }
            // if (JsonConfig.create(ReconnectConfig.class).getSuccessCounter() < 3 && false) {
            // // we have to validate the script first
            //
            // processCallBack.setStatusString(this, _GUI.T.LiveHeaderDetectionWizard_sendRouter_havetovalidate());
            // ReconnectResult res = getPlugin().getReconnectInvoker().validate();
            // if (!res.isSuccess()) {
            // throw new ReconnectException("Reconnect Failed");
            // }
            //
            // }
            processCallBack.setStatusString(getPlugin(), T.T.LiveHeaderDetectionWizard_runOnlineScan_collect());
            collectInfo();
            specialCollectInfo();
            while (true) {
                try {
                    while (true) {
                        while (true) {
                            DataCompareDialog dcd = new DataCompareDialog(gatewayAdressHost, firmware, manufactor, routerName, JsonConfig.create(LiveHeaderReconnectSettings.class).getUserName(), JsonConfig.create(LiveHeaderReconnectSettings.class).getPassword());
                            dcd.setLoginsText(T.T.LiveHeaderDetectionWizard_userConfirm_loginstext());
                            DataCompareDialog impl = UIOManager.I().show(null, dcd);
                            dcd.throwCloseExceptions();
                            username = impl.getUsername();
                            password = impl.getPassword();
                            manufactor = impl.getManufactor();
                            routerName = impl.getRouterName();
                            firmware = impl.getFirmware();
                            try {
                                gatewayAdress = InetAddress.getByName(impl.getHostName());
                                gatewayAdressIP = gatewayAdress.getHostAddress();
                                gatewayAdressHost = gatewayAdress.getHostName();
                                break;
                            } catch (IOException e) {
                                if (UIOManager.I().showConfirmDialog(0, _GUI.T.literall_error(), T.T.LiveHeaderDetectionWizard_runOnlineScan_warning_badhost(dcd.getHostName()), new AbstractIcon(IconKey.ICON_ERROR, 32), _GUI.T.literally_edit(), null)) {
                                    continue;
                                } else {
                                    return;
                                }
                            }
                        }
                        if (!IP.isValidRouterIP(gatewayAdressIP)) {
                            if (!UIOManager.I().showConfirmDialog(0, _GUI.T.literally_warning(), T.T.LiveHeaderDetectionWizard_runOnlineScan_warning_badip(gatewayAdressHost), new AbstractIcon(IconKey.ICON_WARNING, 32), _GUI.T.literally_yes(), _GUI.T.literally_edit())) {
                                continue;
                            }
                        }
                        break;
                    }
                    script = validateBeforeSend(script);
                    RouterData rd = getRouterData();
                    rd.setScript(script);
                    LiveHeaderScriptConfirmUploadDialog confirm = new LiveHeaderScriptConfirmUploadDialog(rd, rd.getRouterIP(), rd.getRouterName());
                    UIOManager.I().show(null, confirm).throwCloseExceptions();
                    script = rd.getScript();
                    test(processCallBack, script);
                    if (StringUtils.isNotEmpty(password)) {
                        settings.setPassword(password);
                    }
                    if (StringUtils.isNotEmpty(username)) {
                        settings.setUserName(username);
                    }
                    settings.setScript(script);
                    settings.setRouterData(rd);
                    break;
                } catch (ScriptValidationExeption e) {
                    logger.log(e);
                    if (StringUtils.isNotEmpty(e.getMessage())) {
                        ConfirmDialog d = new ConfirmDialog(0, _AWU.T.DIALOG_ERROR_TITLE(), T.T.LiveHeaderDetectionWizard_share_reconnectFailed2(e.getMessage()), new AbstractIcon(IconKey.ICON_WARNING_RED, 32), T.T.try_again(), null);
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                    } else {
                        ConfirmDialog d = new ConfirmDialog(0, _AWU.T.DIALOG_ERROR_TITLE(), T.T.LiveHeaderDetectionWizard_share_reconnectFailed(), new AbstractIcon(IconKey.ICON_WARNING_RED, 32), T.T.try_again(), null);
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                    }
                } catch (ReconnectException e) {
                    logger.log(e);
                    if (StringUtils.isNotEmpty(e.getMessage())) {
                        ConfirmDialog d = new ConfirmDialog(0, _AWU.T.DIALOG_ERROR_TITLE(), T.T.LiveHeaderDetectionWizard_share_reconnectFailed2(e.getMessage()), new AbstractIcon(IconKey.ICON_WARNING_RED, 32), T.T.try_again(), null);
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                    } else {
                        ConfirmDialog d = new ConfirmDialog(0, _AWU.T.DIALOG_ERROR_TITLE(), T.T.LiveHeaderDetectionWizard_share_reconnectFailed(), new AbstractIcon(IconKey.ICON_WARNING_RED, 32), T.T.try_again(), null);
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                    }
                }
            }
            scanRemoteInfo();
            specials();
            uploadData(script);
        } catch (DialogNoAnswerException e) {
            throw new InterruptedException();
        } catch (InterruptedException e) {
            logger.log(e);
            throw e;
        } catch (Exception e) {
            logger.log(e);
            Dialog.I().showExceptionDialog(_GUI.T.lit_error_occured(), e.getMessage(), e);
        }
    }

    private void test(ProcessCallBackAdapter processCallBack, String script) throws ReconnectException, InterruptedException, DialogClosedException, DialogCanceledException {
        ConfirmDialog d = new ConfirmDialog(0, T.T.test_required(), T.T.test_required_msg(), null, _GUI.T.lit_continue(), null);
        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
        processCallBack.setStatusString(this, _GUI.T.LiveHeaderDetectionWizard_sendRouter_havetovalidate());
        LiveHeaderInvoker ret = new LiveHeaderInvoker(getPlugin(), script, this.username, this.password, StringUtils.isEmpty(this.gatewayAdressHost) ? gatewayAdressIP : gatewayAdressHost, routerName);
        ReconnectDialog reconnect = new ReconnectDialog() {
            @Override
            protected void reportException(ReconnectException e) {
                // super.reportException(e);
            }

            protected void onFinished() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        dispose();
                    }
                };
            }
        };
        reconnect.setInvoker(ret);
        UIOManager.I().show(null, reconnect);
        if (reconnect.getException() != null) {
            throw reconnect.getException();
        }
        if (reconnect.getResult() == null || !reconnect.getResult().isSuccess()) {
            throw new ReconnectException("Reconnect Failed");
        }
        ConfirmDialog confirm = new ConfirmDialog(0, T.T.confirm_success_title(), T.T.confirm_success_message(), new AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI.T.lit_yes(), _GUI.T.lit_no());
        confirm.setPreferredWidth(500);
        UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
    }

    private String validateBeforeSend(String script) throws ScriptValidationExeption {
        RouterData rd = new RouterData();
        rd.setScript(script);
        try {
            new Scriptvalidator(rd) {
                private HashSet<String> confirmed = new HashSet<String>();

                protected void replaceAuthHeader(String authorization, String lUsername, String lPassword) throws jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript, Exception {
                    if (defaultPasswords.contains(lPassword.toLowerCase(Locale.ENGLISH)) && defaultUsernames.contains(lUsername.toLowerCase(Locale.ENGLISH))) {
                        return;
                    }
                    if (StringUtils.isEmpty(password)) {
                        password = (lPassword);
                    }
                    if (StringUtils.isEmpty(username)) {
                        username = (lUsername);
                    }
                    if (StringUtils.isNotEmpty(password) && !StringUtils.equals(password, lPassword)) {
                        if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_password_change(authorization, lPassword), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                            password = (lPassword);
                        } else {
                            throw new Exception("Password Mismatch " + LiveHeaderDetectionWizard.this.password + "!=" + lPassword);
                        }
                    }
                    if (StringUtils.isNotEmpty(username) && !StringUtils.equals(username, lUsername)) {
                        if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_username_change(authorization, lUsername), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                            password = (lPassword);
                        } else {
                            throw new Exception("Username Mismatch " + LiveHeaderDetectionWizard.this.username + "!=" + lUsername);
                        }
                    }
                    throw new RetryWithReplacedScript(this.rd.getScript(), authorization.substring("Basic ".length()), "%%%basicauth%%%");
                };

                protected void replacePasswordParameter(String key, String value) throws jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript, Exception {
                    if (defaultPasswords.contains(value.toLowerCase(Locale.ENGLISH))) {
                        return;
                    }
                    if (confirmed.contains(key + "=" + value)) {
                        return;
                    }
                    if (StringUtils.equals(value, password)) {
                        super.replacePasswordParameter(key, value);
                    } else if (confirm(key, value)) {
                        if (StringUtils.isNotEmpty(LiveHeaderDetectionWizard.this.password) && !StringUtils.equals(LiveHeaderDetectionWizard.this.password, value)) {
                            if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_password_change_parameter(value), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                password = value;
                            } else {
                                throw new Exception("Password Mismatch " + LiveHeaderDetectionWizard.this.password + "!=" + value);
                            }
                        }
                        if (StringUtils.isEmpty(password)) {
                            password = value;
                        }
                        super.replacePasswordParameter(key, value);
                    } else {
                        confirmed.add(key + "=" + value);
                    }
                }

                protected boolean confirm(String key, String value) {
                    ConfirmDialog d = new ConfirmDialog(0, T.T.please_check(), T.T.please_check_sensitive_data_before_share(key + "=" + value), new AbstractIcon(IconKey.ICON_QUESTION, 32), T.T.yes_replace(), T.T.no_keep());
                    d.setPreferredWidth(500);
                    try {
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                        return true;
                    } catch (DialogClosedException e) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e) {
                        e.printStackTrace();
                    }
                    return false;
                };

                protected void onHost(String host) throws Exception {
                    // do not check LiveHeaderReconnectSettings.isAutoReplaceIPEnabled, always replace
                    if (!host.startsWith("your.router.ip")) {
                        throw new RetryWithReplacedScript(rd.getScript(), host, "%%%routerip%%%");
                    }
                };

                protected void replaceUsernameParameter(String key, String value) throws jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript, Exception {
                    if (defaultUsernames.contains(value.toLowerCase(Locale.ENGLISH))) {
                        return;
                    }
                    if (confirmed.contains(key + "=" + value)) {
                        return;
                    }
                    if (StringUtils.equals(value, username)) {
                        super.replaceUsernameParameter(key, value);
                    } else if (confirm(key, value)) {
                        if (StringUtils.isNotEmpty(LiveHeaderDetectionWizard.this.username) && !StringUtils.equals(LiveHeaderDetectionWizard.this.username, value)) {
                            if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_username_change_parameter(value), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                username = value;
                            } else {
                                throw new Exception("Username Mismatch " + LiveHeaderDetectionWizard.this.username + "!=" + value);
                            }
                        }
                        if (StringUtils.isEmpty(username)) {
                            username = value;
                        }
                        super.replaceUsernameParameter(key, value);
                    } else {
                        confirmed.add(key + "=" + value);
                    }
                };
            }.run();
            return rd.getScript();
        } catch (Exception e) {
            throw new ScriptValidationExeption(e);
            // UIOManager.I().show(ExceptionDialogInterface.class, new ExceptionDialog(UIOManager.BUTTONS_HIDE_CANCEL,
            // T.T.share_invalid_title(), T.T.share_invalid_msg(), e, _GUI.T.lit_close(), null));
            // return;
        }
    }

    private void uploadData(String script) {
        RouterData rd = getRouterData();
        rd.setScript(script);
        AddRouterResponse resp = RecollController.getInstance().addRouter(rd);
        if (resp == null) {
            UIOManager.I().showMessageDialog(T.T.LiveHeaderDetectionWizard_runOnlineScan_notavailable_mm());
            return;
        }
        rd.setScriptID(resp.getScriptID());
        JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterData(rd);
        JsonConfig.create(LiveHeaderReconnectSettings.class).setScript(rd.getScript());
        JsonConfig.create(LiveHeaderReconnectSettings.class).setUserName(username);
        JsonConfig.create(LiveHeaderReconnectSettings.class).setPassword(password);
        if (!resp.isDupe()) {
            UIOManager.I().showMessageDialog(T.T.LiveHeaderDetectionWizard_uploadData_sent_ok());
        } else {
            UIOManager.I().showMessageDialog(T.T.LiveHeaderDetectionWizard_uploadData_sent_failed());
        }
    }
}