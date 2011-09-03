package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectWizardProgress;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.plugins.liveheader.recorder.Gui;
import jd.controlling.reconnect.plugins.liveheader.translate.T;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.http.Browser;
import jd.http.RequestHeader;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Regex;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.TextComponentChangeListener;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LiveHeaderReconnect extends RouterPlugin implements ControlListener, ConfigEventListener {

    @SuppressWarnings("unchecked")
    public static ArrayList<String[]> getLHScripts() {
        final File[] list = new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), "router").listFiles();
        final ArrayList<String[]> ret = new ArrayList<String[]>();
        for (final File element : list) {
            if (element.isFile() && element.getName().toLowerCase().matches(".*\\.xml$")) {
                ret.addAll((Collection<? extends String[]>) JDIO.loadObject(element, true));
            }
        }

        return ret;
    }

    private HashMap<String, String> variables;
    private HashMap<String, String> headerProperties;

    private ExtTextField            txtUser;
    private ExtPasswordField        txtPassword;

    private ExtTextField            txtIP;

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    private ExtTextField                txtName;
    private ImageIcon                   icon;
    protected boolean                   dosession = true;
    private LiveHeaderReconnectSettings settings;

    protected static final Logger       LOG       = JDLogger.getLogger();
    public static final String          ID        = "httpliveheader";

    /**
     * DO NOT REMOVE THIS OR REPLACE BY Regex.getLines()
     * 
     * REGEX ARE COMPLETE DIFFERENT AND DO NOT TRIM
     */
    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    public LiveHeaderReconnect() {
        super();
        this.icon = NewTheme.I().getIcon("modem", 16);

        // only listen to system to autosend script
        JDController.getInstance().addControlListener(this);
        // Send routerscript if there were 3 successful recoinnects in a row
        JsonConfig.create(ReconnectConfig.class).getStorageHandler().getEventSender().addListener(this);
        settings = JsonConfig.create(LiveHeaderReconnectSettings.class);
        settings.getStorageHandler().getEventSender().addListener(this);
        AdvancedConfigManager.getInstance().register(JsonConfig.create(LiveHeaderReconnectSettings.class));

    }

    @Override
    public int runAutoDetection(ReconnectWizardProgress progress) {
        // final long start = System.currentTimeMillis();
        return -1;
    }

    public void controlEvent(final ControlEvent event) {
        // if (event.getEventID() == ControlEvent.CONTROL_AFTER_RECONNECT &&
        // ReconnectPluginController.getInstance().getActivePlugin() == this &&
        // !this.getStorage().get("SENT", false)) {
        // final boolean rcOK =
        // JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RECONNECT_OKAY,
        // true);
        // final int failCount =
        // JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
        // 0);
        // if (failCount == 0 && rcOK) {
        // final int count = this.getStorage().get("SUCCESSCOUNT", 0) + 1;
        // this.getStorage().put("SUCCESSCOUNT", count);
        // if (count > 2) {
        // try {
        // RouterSender.getInstance().run();
        //
        // this.getStorage().put("SENT", true);
        //
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        // }
        // } else {
        // this.getStorage().put("SUCCESSCOUNT", 0);
        // }
        //
        // }
    }

    private Browser doRequest(String request, final Browser br, final boolean ishttps, final boolean israw) {
        try {
            final String requestType;
            final String path;
            final StringBuilder post = new StringBuilder();
            String host = null;
            final String http = ishttps ? "https://" : "http://";
            if (LiveHeaderReconnect.LOG.isLoggable(Level.FINEST)) {
                br.forceDebug(true);
            } else {
                br.forceDebug(false);
            }
            final HashMap<String, String> requestProperties = new HashMap<String, String>();
            if (israw) {
                br.setHeaders(new RequestHeader());
            }
            String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
            final String[] params = new Regex(request, "%%%(.*?)%%%").getColumn(0);
            if (params.length > 0) {
                final StringBuilder req;
                if (request.startsWith(params[0])) {
                    req = new StringBuilder();
                    LiveHeaderReconnect.LOG.finer("Variables: " + this.variables);
                    LiveHeaderReconnect.LOG.finer("Headerproperties: " + this.headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 0; i <= tmpLength; i++) {
                        LiveHeaderReconnect.LOG.finer("Replace variable: " + this.getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(this.getModifiedVariable(params[i - 1]));
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    LiveHeaderReconnect.LOG.finer("Variables: " + this.variables);
                    LiveHeaderReconnect.LOG.finer("Headerproperties: " + this.headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 1; i <= tmpLength; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        LiveHeaderReconnect.LOG.finer("Replace variable: " + this.getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(this.getModifiedVariable(params[i - 1]));
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                }
                request = req.toString();
            }
            final String[] requestLines = LiveHeaderReconnect.splitLines(request);
            if (requestLines.length == 0) {
                LiveHeaderReconnect.LOG.severe("Parse Fehler:" + request);
                return null;
            }
            // RequestType
            tmp = requestLines[0].split(" ");
            if (tmp.length < 2) {
                LiveHeaderReconnect.LOG.severe("Konnte Requesttyp nicht finden: " + requestLines[0]);
                return null;
            }
            requestType = tmp[0];
            path = tmp[1];
            boolean headersEnd = false;
            // Zerlege request

            final int requestLinesLength = requestLines.length;
            for (int li = 1; li < requestLinesLength; li++) {

                if (headersEnd) {
                    post.append(requestLines[li]);
                    post.append(new char[] { '\r', '\n' });
                    continue;
                }
                if (requestLines[li].trim().length() == 0) {
                    headersEnd = true;
                    continue;
                }
                final String[] p = requestLines[li].split("\\:");
                if (p.length < 2) {
                    LiveHeaderReconnect.LOG.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
                    headersEnd = true;
                    li--;
                    continue;
                }
                requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());

                if (p[0].trim().equalsIgnoreCase("HOST")) {
                    host = requestLines[li].substring(p[0].length() + 1).trim();
                }
            }

            if (host == null) {
                LiveHeaderReconnect.LOG.severe("Host nicht gefunden: " + request);
                return null;
            }
            try {
                br.setConnectTimeout(5000);
                br.setReadTimeout(5000);
                if (requestProperties != null) {
                    br.getHeaders().putAll(requestProperties);
                }
                if (requestType.equalsIgnoreCase("GET")) {
                    br.getPage(http + host + path);
                } else if (requestType.equalsIgnoreCase("POST")) {
                    final String poster = post.toString().trim();
                    br.postPageRaw(http + host + path, poster);
                } else if (requestType.equalsIgnoreCase("AUTH")) {
                    LiveHeaderReconnect.LOG.finer("Convert AUTH->GET");
                    br.getPage(http + host + path);
                } else {
                    LiveHeaderReconnect.LOG.severe("Unknown requesttyp: " + requestType);
                    return null;
                }
                return br;
            } catch (final IOException e) {
                LiveHeaderReconnect.LOG.severe("IO Error: " + e.getLocalizedMessage());
                JDLogger.exception(e);
                return null;
            }
        } catch (final Exception e) {
            JDLogger.exception(e);
            return null;
        }

    }

    void editScript() {

        final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, "Script Editor", "Please enter a Liveheader script below.", settings.getScript(), null, T._.jd_controlling_reconnect_plugins_liveheader_LiveHeaderReconnect_actionPerformed_save(), null);
        dialog.setPreferredSize(new Dimension(700, 400));
        // CLR Import
        dialog.setLeftActions(new AbstractAction("Browser Scripts") {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {

                final ImportRouterDialog importDialog = new ImportRouterDialog(LiveHeaderReconnect.getLHScripts());
                try {
                    Dialog.getInstance().showDialog(importDialog);
                    final String[] data = importDialog.getResult();

                    if (data != null) {

                        if (data[2].toLowerCase().indexOf("curl") >= 0) {
                            UserIO.getInstance().requestMessageDialog(T._.gui_config_liveHeader_warning_noCURLConvert());
                        }

                        dialog.setDefaultMessage(data[2]);
                        settings.setRouterName(data[0] + " - " + data[1]);

                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }
        }, new AbstractAction("Import CLR Script") {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {

                final InputDialog clrDialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, "CLR Import", "Please enter a Liveheader script below.", "", null, null, null);
                clrDialog.setPreferredSize(new Dimension(500, 400));
                try {
                    final String clr = Dialog.getInstance().showDialog(clrDialog);
                    if (clr == null) { return; }

                    final String[] ret = CLRConverter.createLiveHeader(clr);
                    if (ret != null) {
                        settings.setRouterName(ret[0]);
                        dialog.setDefaultMessage(ret[1]);
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }
        });
        String newScript;
        try {
            newScript = Dialog.getInstance().showDialog(dialog);
            if (newScript != null) {
                settings.setScript(newScript);
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 3", "[][][grow,fill]", "[]"));
        JButton btnAuto = new JButton(new AutoDetectAction());

        // auto search is not ready yet
        // this.btnAuto.setEnabled(false);
        JButton btnRecord = new JButton(new ReconnectRecorderAction(this));

        JButton btnFindIP = new JButton(new GetIPAction(this));

        JButton btnEditScript = new JButton(new EditScriptAction(this));

        this.txtUser = new ExtTextField();
        txtUser.setHelpText(T._.LiveHeaderReconnect_getGUI_help_user());
        new TextComponentChangeListener(this.txtUser) {
            @Override
            protected void onChanged(final DocumentEvent e) {
                settings.setUserName(LiveHeaderReconnect.this.txtUser.getText());
            }
        };
        this.txtPassword = new ExtPasswordField() {
            protected void onChanged() {
                settings.setPassword(new String(LiveHeaderReconnect.this.txtPassword.getPassword()));
            }
        };
        txtPassword.setHelpText(T._.LiveHeaderReconnect_getGUI_help_password());
        this.txtIP = new ExtTextField();
        txtIP.setHelpText(T._.LiveHeaderReconnect_getGUI_help_ip());
        new TextComponentChangeListener(this.txtIP) {
            @Override
            protected void onChanged(final DocumentEvent e) {
                settings.setRouterIP(LiveHeaderReconnect.this.txtIP.getText());
            }
        };
        this.txtName = new ExtTextField();
        txtName.setEditable(false);
        txtName.setBorder(null);
        SwingUtils.setOpaque(txtName, false);
        new TextComponentChangeListener(this.txtName) {
            @Override
            protected void onChanged(final DocumentEvent e) {
                settings.setRouterName(LiveHeaderReconnect.this.txtName.getText());
            }
        };
        //

        p.add(btnAuto, "sg buttons,aligny top,newline,gapright 15");

        p.add(new JLabel(T._.literally_router_model()), "");
        p.add(this.txtName, "spanx");
        //
        p.add(btnFindIP, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_router_ip()), "");
        p.add(this.txtIP, "spanx");
        //
        p.add(btnRecord, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_username()), "");
        p.add(this.txtUser, "spanx");
        //
        p.add(btnEditScript, "sg buttons,aligny top,newline");
        p.add(new JLabel(T._.literally_password()), "");
        p.add(this.txtPassword, "spanx");
        //

        // p.add(new JLabel("Control URL"), "newline,skip");
        // p.add(this.controlURLTxt);
        // p.add(Box.createGlue(), "pushy,growy");
        this.updateGUI();
        return p;
    }

    @Override
    public String getID() {
        return LiveHeaderReconnect.ID;
    }

    private String getModifiedVariable(String key) {

        if (key.indexOf(":::") == -1 && this.headerProperties.containsKey(key)) { return this.headerProperties.get(key); }
        if (key.indexOf(":::") == -1) { return this.variables.get(key); }
        String ret = this.variables.get(key.substring(key.lastIndexOf(":::") + 3));
        if (this.headerProperties.containsKey(key.substring(key.lastIndexOf(":::") + 3))) {
            ret = this.headerProperties.get(key.substring(key.lastIndexOf(":::") + 3));
        }
        if (ret == null) { return ""; }
        int id;
        String fnc;
        while ((id = key.indexOf(":::")) >= 0) {
            fnc = key.substring(0, id);
            key = key.substring(id + 3);

            if (fnc.equalsIgnoreCase("URLENCODE")) {
                ret = Encoding.urlEncode(ret);
            } else if (fnc.equalsIgnoreCase("URLDECODE")) {
                ret = Encoding.htmlDecode(ret);
            } else if (fnc.equalsIgnoreCase("UTF8DECODE")) {
                ret = Encoding.UTF8Decode(ret);
            } else if (fnc.equalsIgnoreCase("UTF8ENCODE")) {
                ret = Encoding.UTF8Encode(ret);
            } else if (fnc.equalsIgnoreCase("MD5")) {
                ret = JDHash.getMD5(ret);
            } else if (fnc.equalsIgnoreCase("BASE64")) {
                ret = Encoding.Base64Encode(ret);
            }
        }
        return ret;
    }

    @Override
    public String getName() {
        return "LiveHeader";
    }

    private void getVariables(final String patStr, final String[] keys, final Browser br) {
        LiveHeaderReconnect.LOG.info("GetVariables");
        if (br == null) { return; }
        // patStr="<title>(.*?)</title>";
        LiveHeaderReconnect.LOG.finer(patStr);
        final Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // logger.info(requestInfo.getHtmlCode());
        final Matcher matcher = pattern.matcher(br + "");
        LiveHeaderReconnect.LOG.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {
            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                this.variables.put(keys[i], matcher.group(i + 1));
                LiveHeaderReconnect.LOG.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }
        } else {
            LiveHeaderReconnect.LOG.severe("Regular Expression without matches: " + patStr);
        }
    }

    @Override
    public boolean hasAutoDetection() {
        return true;
    }

    public boolean hasDetectionWizard() {
        return true;
    }

    @Override
    public boolean isReconnectionEnabled() {
        return true;
    }

    @Override
    protected void performReconnect() throws ReconnectException, InterruptedException {
        String script;

        script = settings.getScript();

        final String user = settings.getUserName();
        final String pass = settings.getPassword();
        final String ip = settings.getRouterIP();

        if (script == null || script.length() == 0) {

            LiveHeaderReconnect.LOG.severe("No LiveHeader Script found");
            throw new ReconnectException("No LiveHeader Script found");
        }

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        final Document xmlScript;
        this.variables = new HashMap<String, String>();
        this.variables.put("user", user);
        this.variables.put("pass", pass);
        this.variables.put("basicauth", Encoding.Base64Encode(user + ":" + pass));
        this.variables.put("routerip", ip);
        this.headerProperties = new HashMap<String, String>();

        Browser br = new Browser();
        /* set custom timeouts here because 10secs is a LONG time ;) */
        br.setReadTimeout(10000);
        br.setConnectTimeout(10000);
        br.setProxy(HTTPProxy.NONE);
        if (user != null && pass != null) {
            br.setAuth(ip, user, pass);
        }
        try {
            xmlScript = JDUtilities.parseXmlString(script, false);
            if (xmlScript == null) {

                LiveHeaderReconnect.LOG.severe("Error while parsing the xml string: " + script);
                throw new ReconnectException("Error while parsing the xml string");

            }
            final Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {

                LiveHeaderReconnect.LOG.severe("Root Node must be [[[HSRC]]]*[/HSRC]");
                throw new ReconnectException("Error while parsing the xml string. Root Node must be [[[HSRC]]]*[/HSRC]");
            }

            final NodeList steps = root.getChildNodes();

            for (int step = 0; step < steps.getLength(); step++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

                final Node current = steps.item(step);

                if (current.getNodeType() == 3) {
                    continue;
                }

                if (!current.getNodeName().equalsIgnoreCase("STEP")) {

                    LiveHeaderReconnect.LOG.severe("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                    throw new ReconnectException("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());

                }
                final NodeList toDos = current.getChildNodes();
                final int toDosLength = toDos.getLength();
                for (int toDoStep = 0; toDoStep < toDosLength; toDoStep++) {
                    final Node toDo = toDos.item(toDoStep);
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {

                        final NamedNodeMap attributes = toDo.getAttributes();
                        for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                            final String key = attributes.item(attribute).getNodeName();
                            String value = attributes.item(attribute).getNodeValue();
                            final String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%");
                            final String[] params = new Regex(value, "%%%(.*?)%%%").getColumn(-1);
                            if (params.length > 0) {
                                final StringBuilder req;
                                if (value.startsWith(params[0])) {
                                    req = new StringBuilder();
                                    LiveHeaderReconnect.LOG.finer("Variables: " + this.variables);
                                    LiveHeaderReconnect.LOG.finer("Headerproperties: " + this.headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 0; i <= tmpLength; i++) {
                                        LiveHeaderReconnect.LOG.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req.append(this.getModifiedVariable(params[i - 1]));
                                        if (i < tmpLength) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                } else {
                                    req = new StringBuilder(tmp[0]);
                                    LiveHeaderReconnect.LOG.finer("Variables: " + this.variables);
                                    LiveHeaderReconnect.LOG.finer("Headerproperties: " + this.headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 1; i <= tmpLength; i++) {
                                        if (i > params.length) {
                                            continue;
                                        }
                                        LiveHeaderReconnect.LOG.finer("Replace variable: *********(" + params[i - 1] + ")");
                                        req.append(this.getModifiedVariable(params[i - 1]));
                                        if (i < tmpLength) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                }

                                value = req.toString();
                            }

                            this.variables.put(key, value);
                        }

                        LiveHeaderReconnect.LOG.finer("Variables set: " + this.variables);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
                        final String[] parseLines = LiveHeaderReconnect.splitLines(toDo.getChildNodes().item(0).getNodeValue().trim());
                        for (final String parseLine : parseLines) {
                            String varname = new Regex(parseLine, "(.*?):").getMatch(0);
                            String pattern = new Regex(parseLine, ".*?:(.+)").getMatch(0);
                            if (varname != null && pattern != null) {
                                varname = varname.trim();
                                pattern = pattern.trim();
                                String found = br.getRegex(pattern).getMatch(0);
                                if (found != null) {
                                    found = found.trim();
                                    LiveHeaderReconnect.LOG.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    this.variables.put(varname, found);
                                } else {
                                    LiveHeaderReconnect.LOG.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                }
                            }
                        }
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        boolean ishttps = false;
                        boolean israw = false;
                        if (toDo.getChildNodes().getLength() != 1) {

                            LiveHeaderReconnect.LOG.severe("A REQUEST Tag is not allowed to have childTags.");
                            throw new ReconnectException("A REQUEST Tag is not allowed to have childTags.");

                        }
                        final NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("https") != null) {
                            ishttps = true;
                        }
                        if (attributes.getNamedItem("raw") != null) {
                            israw = true;
                        }
                        Browser retbr = null;
                        try {
                            retbr = this.doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), br, ishttps, israw);
                        } catch (final Exception e2) {
                            retbr = null;
                        }
                        try {
                            /* DDoS Schutz */
                            Thread.sleep(350);
                        } catch (final Exception e) {
                        }
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            LiveHeaderReconnect.LOG.severe("Request error!");
                        } else {
                            br = retbr;
                        }

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
                        LiveHeaderReconnect.LOG.finer("get Response");
                        if (toDo.getChildNodes().getLength() != 1) {

                            LiveHeaderReconnect.LOG.severe("A RESPONSE Tag is not allowed to have childTags.");
                            throw new ReconnectException("A RESPONSE Tag is not allowed to have childTags.");

                        }

                        final NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {

                            LiveHeaderReconnect.LOG.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                            throw new ReconnectException("A RESPONSE Node needs a Keys Attribute: " + toDo);

                        }

                        final String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                        this.getVariables(toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {
                        final NamedNodeMap attributes = toDo.getAttributes();
                        final Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            LiveHeaderReconnect.LOG.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                            throw new ReconnectException("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");

                        }
                        LiveHeaderReconnect.LOG.finer("Wait " + item.getNodeValue() + " seconds");
                        final int seconds = Formatter.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("TIMEOUT")) {
                        final NamedNodeMap attributes = toDo.getAttributes();
                        final Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            LiveHeaderReconnect.LOG.severe("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                            throw new ReconnectException("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");

                        }
                        int seconds = Formatter.filterInt(item.getNodeValue());
                        if (seconds < 0) {
                            seconds = 0;
                        }
                        LiveHeaderReconnect.LOG.finer("Timeout set to " + seconds + " seconds");
                        if (br != null) {
                            br.setReadTimeout(seconds * 1000);
                            br.setConnectTimeout(seconds * 1000);
                        }
                    }

                }
            }
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            JDLogger.exception(e);

            LiveHeaderReconnect.LOG.severe(e.getCause() + " : " + e.getMessage());
            throw new ReconnectException(e);
        }

    }

    public void routerRecord() {

        if (JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled()) {
            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, T._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_title(), T._.jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message());
        } else {
            new Thread() {
                @Override
                public void run() {
                    final String text = LiveHeaderReconnect.this.txtIP.getText().toString();
                    if (text == null || !LiveHeaderDetectionWizard.isValidRouterIP(text)) {
                        new GetIPAction(LiveHeaderReconnect.this).actionPerformed(null);
                    }

                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {

                            final Gui jd = new Gui(settings.getRouterIP());
                            try {
                                Dialog.getInstance().showDialog(jd);
                                if (jd.saved) {
                                    settings.setRouterIP(jd.ip);

                                    if (jd.user != null) {
                                        settings.setUserName(jd.user);
                                    }
                                    if (jd.pass != null) {
                                        settings.setPassword(jd.pass);

                                    }
                                    settings.setScript(jd.methode);
                                    setName("Router Recorder Custom Script");

                                }
                            } catch (DialogClosedException e) {
                                e.printStackTrace();
                            } catch (DialogCanceledException e) {
                                e.printStackTrace();
                            }

                            return null;
                        }

                    }.start();

                }
            }.start();
        }
    }

    @Override
    public int runDetectionWizard(ReconnectWizardProgress progress) throws InterruptedException {
        final LiveHeaderDetectionWizard wizard = new LiveHeaderDetectionWizard(progress);
        // int ret = wizard.runOnlineScan();
        // if (ret < 0) {
        // ret = wizard.runOfflineScan();
        // }
        // if (ret < 0) {
        // // TODO
        // // ret = wizard.runRouterRecorder();
        // }
        return 0;
    }

    void updateGUI() {
        new EDTRunner() {
            protected void runInEDT() {
                try {
                    LiveHeaderReconnect.this.txtName.setText(settings.getRouterName());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtIP.setText(settings.getRouterIP());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtPassword.setPassword(settings.getPassword().toCharArray());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }
                try {
                    LiveHeaderReconnect.this.txtUser.setText(settings.getUserName());
                } catch (final Throwable e) {
                    // throws an Throwable if the caller
                    // is a changelistener of this field's document
                }

            }

        };

    }

    public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        if (config == settings) {
            updateGUI();
        } else {
            if (dosession && ReconnectPluginController.getInstance().getActivePlugin() == this) {
                if (JsonConfig.create(ReconnectConfig.class).getSuccessCounter() > 3) {
                    new RouterSendAction(this).actionPerformed(null);
                    dosession = false;
                }
            }
        }

    }
}