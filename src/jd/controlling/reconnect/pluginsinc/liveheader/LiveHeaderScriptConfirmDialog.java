package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript;
import jd.controlling.reconnect.pluginsinc.liveheader.validate.Scriptvalidator;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.ExceptionDialogInterface;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExceptionDialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LiveHeaderScriptConfirmDialog extends AbstractDialog<Object> {
    protected RouterData routerData;
    private String       gateway;
    private String       name;
    private JTextPane    textpane;
    private LogSource    logger;

    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        LookAndFeelController.getInstance().init();
        RouterData rd = new RouterData();
        rd.setScript(UIOManager.I().show(InputDialogInterface.class, new InputDialog(Dialog.STYLE_LARGE, "", "", "")).getText());
        UIOManager.I().show(null, new LiveHeaderScriptConfirmDialog(rd, "myip.ne", "My Router"));
    }

    @Override
    protected int getPreferredHeight() {
        int pr = super.getPreferredHeight();
        if (pr > 0) {
            return Math.min(750, pr);
        }
        return Math.min(getRawPreferredSize().height + 20, 750);
    }

    @Override
    protected int getPreferredWidth() {
        int pr = super.getPreferredHeight();
        if (pr > 0) {
            return Math.min(1000, pr);
        }
        return Math.min(getRawPreferredSize().width + 20, 1000);
    }

    public LiveHeaderScriptConfirmDialog(RouterData test, String gatewayAdressHost, String name) {
        this(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.STYLE_HIDE_ICON, _GUI.T.runDetectionWizard_confirm_title(), new AbstractIcon("reconnect", 32), _GUI.T.lit_continue(), _GUI.T.lit_skip(), test, gatewayAdressHost, name);
    }

    public LiveHeaderScriptConfirmDialog(final int flag, final String title, final Icon icon, final String okOption, final String cancelOption, RouterData test, String gatewayAdressHost, String name) {
        super(flag, title, icon, okOption, cancelOption);
        this.routerData = test;
        logger = LogController.getInstance().getLogger("LiveHeaderScriptConfirmDialog");
        this.gateway = gatewayAdressHost;
        this.name = name;
        addEditAction();
    }

    /**
     *
     */
    private HashSet<String> confirmed = new HashSet<String>();
    private AppAction       editAction;

    public void addEditAction() {
        setLeftActions(editAction = new AppAction() {
            {
                setName(_GUI.T.LiveHeaderScriptConfirmDialog_LiveHeaderScriptConfirmDialog_edit());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, T.T.script(routerData.getRouterName()), T.T.script_check_modify(), routerData.getScript(), new AbstractIcon("edit", 32), T.T.jd_controlling_reconnect_plugins_liveheader_LiveHeaderReconnect_actionPerformed_save(), null) {
                    @Override
                    public boolean isRemoteAPIEnabled() {
                        return super.isRemoteAPIEnabled();
                    }
                };
                dialog.setPreferredSize(new Dimension(700, 400));
                InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, dialog);
                try {
                    d.throwCloseExceptions();
                    routerData.setScript(d.getText());
                    routerData.setScriptID(null);
                    try {
                        final LiveHeaderReconnectSettings settings = JsonConfig.create(LiveHeaderReconnectSettings.class);
                        final RouterData rd = routerData;
                        new Scriptvalidator(rd) {
                            protected void replaceAuthHeader(String authorization, String lUsername, String lPassword) throws jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript, Exception {
                                if (defaultPasswords.contains(lPassword.toLowerCase(Locale.ENGLISH)) && defaultUsernames.contains(lUsername.toLowerCase(Locale.ENGLISH))) {
                                    return;
                                }
                                if (StringUtils.isEmpty(settings.getPassword())) {
                                    settings.setPassword(lPassword);
                                }
                                if (StringUtils.isEmpty(settings.getUserName())) {
                                    settings.setUserName(lPassword);
                                }
                                if (StringUtils.isNotEmpty(settings.getPassword()) && !StringUtils.equals(settings.getPassword(), lPassword)) {
                                    if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_password_change(authorization, lPassword), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                        settings.setPassword(lPassword);
                                    } else {
                                        return;
                                    }
                                }
                                if (StringUtils.isNotEmpty(settings.getUserName()) && !StringUtils.equals(settings.getUserName(), lUsername)) {
                                    if (UIOManager.I().showConfirmDialog(0, T.T.please_check(), T.T.please_confirm_username_change(authorization, lUsername), null, _GUI.T.lit_yes(), _GUI.T.lit_no())) {
                                        settings.setUserName(lUsername);
                                    } else {
                                        return;
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
                                if (StringUtils.equals(value, settings.getPassword())) {
                                    super.replacePasswordParameter(key, value);
                                } else if (confirm(key, value)) {
                                    if (StringUtils.isNotEmpty(settings.getPassword()) && !StringUtils.equals(settings.getPassword(), value)) {
                                        return;
                                    }
                                    if (StringUtils.isEmpty(settings.getPassword())) {
                                        settings.setPassword(value);
                                    }
                                    super.replacePasswordParameter(key, value);
                                } else {
                                    confirmed.add(key + "=" + value);
                                }
                            };

                            protected void replaceUsernameParameter(String key, String value) throws jd.controlling.reconnect.pluginsinc.liveheader.validate.RetryWithReplacedScript, Exception {
                                if (defaultUsernames.contains(value.toLowerCase(Locale.ENGLISH))) {
                                    return;
                                }
                                if (confirmed.contains(key + "=" + value)) {
                                    return;
                                }
                                if (StringUtils.equals(value, settings.getUserName())) {
                                    super.replaceUsernameParameter(key, value);
                                } else if (confirm(key, value)) {
                                    if (StringUtils.isNotEmpty(settings.getUserName()) && !StringUtils.equals(settings.getUserName(), value)) {
                                        return;
                                    }
                                    if (StringUtils.isEmpty(settings.getUserName())) {
                                        settings.setUserName(value);
                                    }
                                    super.replaceUsernameParameter(key, value);
                                } else {
                                    confirmed.add(key + "=" + value);
                                }
                            }

                            protected boolean confirm(String key, String value) {
                                ConfirmDialog d = new ConfirmDialog(0, T.T.please_check(), T.T.please_check_sensitive_data_after_edit(key + "=" + value), new AbstractIcon(IconKey.ICON_QUESTION, 32), T.T.yes_replace(), T.T.no_keep());
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
                        }.run();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.lit_warning(), _GUI.T.LiveHeaderReconnect_validateAndSet_object_(), null, null, null));
                    }
                    updateScriptInfo();
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 2", "[align right][grow,fill]", "[]");
        addMessage(p);
        p.add(getLabel(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_routername()));
        p.add(new JLabel(StringUtils.isEmpty(name) ? T.T.unknown() : name));
        if (StringUtils.isNotEmpty(routerData.getManufactor())) {
            p.add(getLabel(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_Manufactor()));
            p.add(new JLabel(routerData.getManufactor()));
        }
        if (routerData.getAvgScD() > 0) {
            p.add(getLabel(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_time()));
            p.add(new JLabel(TimeFormatter.formatMilliSeconds(routerData.getAvgScD(), 0)));
        }
        if (StringUtils.isNotEmpty(gateway)) {
            p.add(getLabel(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_routerip()));
            p.add(new JLabel(gateway));
        }
        p.add(getLabel(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_script_overview()));
        p.add(Box.createHorizontalGlue());
        textpane = addMessageComponent();
        p.add(new JScrollPane(textpane), "pushx,growx,spanx,pushy,growy");
        updateScriptInfo();
        if (StringUtils.isEmpty(textpane.toString())) {
            editAction.actionPerformed(null);
        }
        return p;
    }

    protected void addMessage(MigPanel p) {
        JTextPane textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return true;
            }
        };
        final Font font = textField.getFont();
        textField.setContentType("text/plain");
        textField.setFont(font);
        textField.setText(getMessage());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        p.add(textField, "spanx,alignx left");
    }

    /**
     * @return
     */
    public String getMessage() {
        return T.T.confirm_script();
    }

    /**
     *
     */
    public void updateScriptInfo() {
        try {
            textpane.setText(toOverView(routerData.getScript()));
        } catch (Throwable e) {
            textpane.setText(_GUI.T.LiveHeaderScriptConfirmDialog_layoutDialogContent_invalidscript());
            UIOManager.I().show(ExceptionDialogInterface.class, new ExceptionDialog(UIOManager.BUTTONS_HIDE_OK, e.getMessage(), e.getMessage(), e, null, _GUI.T.lit_close()));
        }
    }

    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    private String toOverView(String script) throws Exception {
        if (StringUtils.isEmpty(script)) {
            return "";
        }
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("user", URLEncode.encodeRFC2396("<Username required>"));
        map.put("pass", URLEncode.encodeRFC2396("<Password required>"));
        map.put("username", URLEncode.encodeRFC2396("<Username required>"));
        map.put("password", URLEncode.encodeRFC2396("<Password required>"));
        map.put("basicauth", URLEncode.encodeRFC2396("<Basic Authentication. Username and Password required>"));
        map.put("auth", URLEncode.encodeRFC2396("<Basic Authentication. Username and Password required>"));
        if (StringUtils.isEmpty(gateway)) {
            map.put("ip", "your.router.ip");
            map.put("routerip", "your.router.ip");
            map.put("host", "your.router.ip");
        } else {
            map.put("ip", gateway);
            map.put("routerip", gateway);
            map.put("host", gateway);
        }
        this.internalVariables = Collections.unmodifiableMap(map);
        logger.info("Internal Variables: " + internalVariables);
        this.parsedVariables = new HashMap<String, String>();
        if (script != null) {
            script = script.replaceAll("\\[\\[\\[", "<");
            script = script.replaceAll("\\]\\]\\]", ">");
            script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
            script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
            script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
            script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        }
        StringBuilder sb = new StringBuilder();
        final Document xmlScript = JDUtilities.parseXmlString(script, false);
        if (xmlScript == null) {
            logger.severe("Error while parsing the xml string: " + script);
            throw new ReconnectException("Error while parsing the xml string");
        }
        final Node root = xmlScript.getChildNodes().item(0);
        if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
            logger.severe("Root Node must be [[[HSRC]]]*[/HSRC]");
            throw new ReconnectException("Error while parsing the xml string. Root Node must be [[[HSRC]]]*[/HSRC]");
        }
        final NodeList steps = root.getChildNodes();
        for (int step = 0; step < steps.getLength(); step++) {
            final Node current = steps.item(step);
            if (current.getNodeType() == 3) {
                continue;
            }
            if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                logger.severe("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                throw new ReconnectException("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
            }
            final NodeList toDos = current.getChildNodes();
            final int toDosLength = toDos.getLength();
            for (int toDoStep = 0; toDoStep < toDosLength; toDoStep++) {
                final Node toDo = toDos.item(toDoStep);
                if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {
                    final NamedNodeMap attributes = toDo.getAttributes();
                    for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                        final String key = attributes.item(attribute).getNodeName();
                        String value = attributes.item(attribute).getNodeValue();
                        final String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%", -1);
                        final String[] params = new Regex(value, "%%%(.*?)%%%").getColumn(0);
                        if (params.length > 0) {
                            final StringBuilder newValue;
                            newValue = new StringBuilder(tmp[0]);
                            final int tmpLength = tmp.length;
                            for (int i = 1; i <= tmpLength; i++) {
                                if (i > params.length) {
                                    continue;
                                }
                                logger.finer("Replace variable: *********(" + params[i - 1] + ")");
                                newValue.append(this.getModifiedVariable(params[i - 1]));
                                if (i < tmpLength) {
                                    newValue.append(tmp[i]);
                                }
                            }
                            // }
                            value = newValue.toString();
                        }
                        append(sb, "Define Variable " + key + "\t=\t" + value);
                        putVariable(key, value);
                    }
                }
                if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
                    // logger.info("Parse response: \r\n" + br.getRequest());
                    final String[] parseLines = splitLines(toDo.getChildNodes().item(0).getNodeValue().trim());
                    for (final String parseLine : parseLines) {
                        String varname = new Regex(parseLine, "(.*?):").getMatch(0);
                        String pattern = new Regex(parseLine, ".*?:(.+)").getMatch(0);
                        if (varname != null && pattern != null) {
                            varname = varname.trim();
                            pattern = pattern.trim();
                            putVariable(varname, URLEncode.encodeRFC2396("<Variable " + varname + ">"));
                            append(sb, "\t-> Search in HTML Response:  " + varname + " = Regex:" + pattern);
                        }
                    }
                }
                if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                    if (toDo.getChildNodes().getLength() != 1) {
                        logger.severe("A REQUEST Tag is not allowed to have childTags.");
                        throw new ReconnectException("A REQUEST Tag is not allowed to have childTags.");
                    }
                    final NamedNodeMap attributes = toDo.getAttributes();
                    Browser retbr = null;
                    try {
                        this.doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), sb, attributes.getNamedItem("https") != null, attributes.getNamedItem("raw") != null);
                    } catch (final Exception e) {
                        if (e instanceof ReconnectException) {
                            throw e;
                        }
                        retbr = null;
                    }
                    /* DDoS Schutz */
                }
                if (StringUtils.equalsIgnoreCase(toDo.getNodeName(), "RESPONSE")) {
                    logger.finer("get Response");
                    if (toDo.getChildNodes().getLength() != 1) {
                        logger.severe("A RESPONSE Tag is not allowed to have childTags.");
                        throw new ReconnectException("A RESPONSE Tag is not allowed to have childTags.");
                    }
                    final NamedNodeMap attributes = toDo.getAttributes();
                    if (attributes.getNamedItem("keys") == null) {
                        logger.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                        throw new ReconnectException("A RESPONSE Node needs a Keys Attribute: " + toDo);
                    }
                    final String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                    for (String s : keys) {
                        append(sb, "\t-> Search Variable in HTML Response:  " + s);
                    }
                    // this.parseVariables(feedback, toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);
                }
                if (StringUtils.equalsIgnoreCase(toDo.getNodeName(), "WAIT")) {
                    final NamedNodeMap attributes = toDo.getAttributes();
                    final Node item = attributes.getNamedItem("seconds");
                    if (item == null) {
                        logger.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                        throw new ReconnectException("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                    }
                    final int seconds = Formatter.filterInt(item.getNodeValue());
                    if (seconds > 0) {
                        append(sb, "Wait " + TimeFormatter.formatMilliSeconds(seconds * 1000, 0));
                    }
                }
                if (StringUtils.equalsIgnoreCase(toDo.getNodeName(), "TIMEOUT")) {
                    final NamedNodeMap attributes = toDo.getAttributes();
                    final Node item = attributes.getNamedItem("seconds");
                    if (item == null) {
                        logger.severe("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                        throw new ReconnectException("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                    }
                    final int seconds = Formatter.filterInt(item.getNodeValue());
                    if (seconds > 0) {
                        logger.finer("Timeout set to " + seconds + " seconds");
                        append(sb, "Set HTTP Timeout to " + TimeFormatter.formatMilliSeconds(seconds * 1000, 0));
                    }
                }
            }
        }
        return sb.toString();
    }

    private void doRequest(String request, StringBuilder sb, boolean ishttps, boolean israw) throws ReconnectException, IOException {
        final String requestType;
        final String path;
        final StringBuilder post = new StringBuilder();
        final HashMap<String, String> requestProperties = new HashMap<String, String>();
        if (israw) {
            // sb.setHeaders(new RequestHeader());
        }
        String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
        final String[] params = new Regex(request, "%%%(.*?)%%%").getColumn(0);
        if (params.length > 0) {
            final StringBuilder req;
            if (request.startsWith(params[0])) {
                req = new StringBuilder();
                // showParsedVariables();
                final int tmpLength = tmp.length;
                for (int i = 0; i <= tmpLength; i++) {
                    final String key = params[i - 1];
                    final String modifiedVariable = this.getModifiedVariable(key);
                    logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
                    req.append(URLEncode.encodeRFC2396(modifiedVariable));
                    if (i < tmpLength) {
                        req.append(tmp[i]);
                    }
                }
            } else {
                req = new StringBuilder(tmp[0]);
                // showParsedVariables();
                final int tmpLength = tmp.length;
                for (int i = 1; i <= tmpLength; i++) {
                    if (i > params.length) {
                        continue;
                    }
                    final String key = params[i - 1];
                    final String modifiedVariable = this.getModifiedVariable(key);
                    logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
                    req.append(URLEncode.encodeRFC2396(modifiedVariable));
                    if (i < tmpLength) {
                        req.append(tmp[i]);
                    }
                }
            }
            request = req.toString();
        }
        final String[] requestLines = splitLines(request);
        if (requestLines.length == 0) {
            throw new ReconnectException("Parse Fehler:" + request);
        }
        // RequestType
        tmp = requestLines[0].split(" ");
        if (tmp.length < 2) {
            throw new ReconnectException("Konnte Requesttyp nicht finden: " + requestLines[0]);
        }
        requestType = tmp[0];
        path = tmp[1];
        boolean headersEnd = false;
        // Zerlege request
        String host = null;
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
                logger.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
                headersEnd = true;
                li--;
                continue;
            }
            requestProperties.put(p[0].trim().toLowerCase(Locale.ENGLISH), requestLines[li].substring(p[0].length() + 1).trim());
            if (p[0].trim().equalsIgnoreCase("HOST")) {
                host = requestLines[li].substring(p[0].length() + 1).trim();
            }
        }
        if (host == null) {
            throw new ReconnectException("Host not available: " + request);
        } else {
            // verifyHost(host);
            if (requestProperties != null) {
                // sb.getHeaders().putAll(requestProperties);
            }
            final String protocoll = ishttps ? "https://" : "http://";
            if (StringUtils.equalsIgnoreCase(requestType, "AUTH")) {
                logger.finer("Convert AUTH->GET");
            }
            if (StringUtils.equalsIgnoreCase(requestType, "GET") || StringUtils.equalsIgnoreCase(requestType, "AUTH")) {
                URL url = new URL(protocoll + host + path);
                append(sb, "\r\nHTTP Request " + requestType + " " + protocoll + host + url.getPath());
                String cookie = requestProperties.get("cookie");
                if (StringUtils.isNotEmpty(cookie)) {
                    append(sb, "\tCookie:\t" + decode(cookie));
                }
                String authorization = requestProperties.get("authorization");
                if (StringUtils.isNotEmpty(authorization)) {
                    append(sb, "\tAuthorization:\t" + decode(authorization));
                }
                int i = 1;
                for (KeyValuePair pa : HttpConnection.parseParameterList(url.getQuery())) {
                    append(sb, "\tParameter #" + (i++) + ": \t" + decode(pa.key) + "\t=\t" + decode(pa.value));
                }
                // sb.getPage(protocoll + host + path);
            } else if (StringUtils.equalsIgnoreCase(requestType, "POST")) {
                final String poster = post.toString().trim();
                URL url = new URL(protocoll + host + path);
                append(sb, "\r\nHTTP Request " + requestType + " " + protocoll + host + path);
                String cookie = requestProperties.get("cookie");
                if (StringUtils.isNotEmpty(cookie)) {
                    append(sb, "\tCookie:\t" + decode(cookie));
                }
                String authorization = requestProperties.get("authorization");
                if (StringUtils.isNotEmpty(authorization)) {
                    append(sb, "\tAuthorization:\t" + decode(authorization));
                }
                int i = 1;
                for (KeyValuePair pa : HttpConnection.parseParameterList(url.getQuery())) {
                    append(sb, "\tParameter #" + (i++) + ": \t" + decode(pa.key) + "\t=\t" + decode(pa.value));
                }
                for (KeyValuePair pa : HttpConnection.parseParameterList(poster)) {
                    append(sb, "\tParameter #" + (i++) + ": \t" + decode(pa.key) + "\t=\t" + decode(pa.value));
                }
                // sb.postPageRaw(protocoll + host + path, poster);
            } else {
                logger.severe("Unknown/Unsupported requestType: " + requestType);
                throw new ReconnectException("Host not available: " + request);
            }
        }
    }

    private String decode(String key) {
        while (true) {
            try {
                String newKey = Encoding.htmlDecode(key);
                if (StringUtils.isNotEmpty(newKey) && !StringUtils.equals(newKey, key)) {
                    key = newKey;
                } else {
                    break;
                }
            } catch (Throwable e) {
                break;
            }
        }
        return key;
    }

    private void append(StringBuilder sb, String string) {
        if (sb.length() > 0) {
            sb.append("\r\n");
        }
        sb.append(string);
    }

    private void putVariable(String key, String value) throws ReconnectFailedException {
        if (key != null) {
            final String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (internalVariables.containsKey(lowerKey)) {
                throw new ReconnectFailedException("Cannot change internal varbiable:" + lowerKey);
            } else {
                if (value == null) {
                    logger.info("Remove Variable:" + lowerKey + "=" + parsedVariables.remove(lowerKey));
                } else {
                    parsedVariables.put(lowerKey, value);
                    logger.info("Set Variable:" + lowerKey + "->" + value);
                }
            }
        }
    }

    private Map<String, String> internalVariables = null;
    private Map<String, String> parsedVariables   = null;

    private String getVariable(final String key) throws ReconnectFailedException {
        if (key != null) {
            final String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (internalVariables.containsKey(lowerKey)) {
                return internalVariables.get(lowerKey);
            } else if (parsedVariables.containsKey(lowerKey)) {
                return parsedVariables.get(lowerKey);
            } else {
                logger.info("Variable not set:" + lowerKey);
            }
        }
        return null;
    }

    private String getModifiedVariable(String key) throws ReconnectException {
        if (StringUtils.equalsIgnoreCase("timestamp", key)) {
            return Long.toString(System.currentTimeMillis());
        }
        if (StringUtils.containsIgnoreCase(key, "random:")) {
            try {
                // random value
                String[] params = new Regex(key, "random\\:(\\d+):(.+)").getRow(0);
                String possiblechars = params[1];
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Integer.parseInt(params[0]); i++) {
                    sb.append(possiblechars.charAt((int) (Math.random() * possiblechars.length())));
                }
                return sb.toString();
            } catch (Exception e) {
                throw new ReconnectException(e);
            }
        }
        int index = key.indexOf(":::");
        String value = null;
        if (index == -1) {
            value = getVariable(key);
            return value == null ? "" : value;
        }
        final String keyValue = key.substring(key.lastIndexOf(":::") + 3);
        value = getVariable(keyValue);
        if (value == null) {
            return "";
        } else {
            while ((index = key.indexOf(":::")) >= 0) {
                if (value == null) {
                    logger.info("Modified Variable broken: " + key);
                    return "";
                }
                final String method = key.substring(0, index);
                key = key.substring(index + 3);
                if (StringUtils.equalsIgnoreCase(method, "URLENCODE")) {
                    value = "<Variable: " + "UrlEncode(\"" + key + "\")" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "URLDECODE")) {
                    value = "<Variable: " + "UrlDecode(\"" + key + "\")" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "UTF8DECODE")) {
                    value = "<Variable: " + "UTF8Decode(\"" + key + "\")" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "UTF8ENCODE")) {
                    value = "<Variable: " + "UTF8Encode(\"" + key + "\")" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "MD5")) {
                    value = "<Variable: " + "MD5(\"" + key + "\")" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "SHA256")) {
                    value = "<Variable: " + "SHA256(\"" + key + "\")" + ">";
                    // required by a huwai router that uses base64(sha256(pass))
                } else if (StringUtils.equalsIgnoreCase(method, "BASE64_SHA256")) {
                    value = "<Variable: " + "Base64(SHA256(\"" + key + "\"))" + ">";
                } else if (StringUtils.equalsIgnoreCase(method, "BASE64")) {
                    value = "<Variable: " + "Base64(\"" + key + "\")" + ">";
                } else {
                    throw new ReconnectException("Unsupported Type: " + method);
                }
            }
            return value;
        }
    }

    protected JTextPane addMessageComponent() {
        JTextPane textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
        final Font font = textField.getFont();
        textField.setContentType("text/plain");
        textField.setFont(font);
        textField.setText(getMessage());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        TabStop[] tabs = new TabStop[4];
        tabs[0] = new TabStop(20, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        tabs[1] = new TabStop(120, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        tabs[2] = new TabStop(300, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        tabs[3] = new TabStop(320, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        TabSet tabset = new TabSet(tabs);
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
        textField.setParagraphAttributes(aset, false);
        return textField;
    }

    private Component getLabel(String str) {
        JLabel ret = new JLabel(str);
        ret.setEnabled(false);
        return ret;
    }
}
