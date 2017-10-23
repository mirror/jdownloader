package jd.controlling.reconnect.pluginsinc.liveheader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.IPVERSION;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jd.controlling.proxy.NoProxySelector;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.pluginsinc.liveheader.recoll.RecollController;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;
import jd.http.Browser;
import jd.http.Request;
import jd.http.RequestHeader;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

public class LiveHeaderInvoker extends ReconnectInvoker {
    private String script;

    public String getScript() {
        return orgScript;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getRouter() {
        return router;
    }

    @Override
    public ReconnectResult validate(ReconnectResult r) throws InterruptedException, ReconnectException {
        try {
            r = super.validate(r);
            if (r instanceof LiveHeaderReconnectResult) {
                RouterData rd = ((LiveHeaderReconnectResult) r).getRouterData();
                if (rd != null && rd.getScriptID() != null) {
                    if (r.isSuccess()) {
                        RecollController.getInstance().trackWorking(rd.getScriptID(), r.getSuccessDuration(), r.getOfflineDuration());
                    } else {
                        RecollController.getInstance().trackNotWorking(rd.getScriptID());
                    }
                }
            }
            return r;
        } catch (ReconnectException e) {
            RouterData rd = ((LiveHeaderReconnectResult) r).getRouterData();
            if (rd != null && rd.getScriptID() != null) {
                RecollController.getInstance().trackNotWorking(rd.getScriptID());
            }
            throw e;
        } finally {
        }
    }

    private final String      user;
    private final String      pass;
    private final String      router;
    private LHProcessFeedback feedback = null;
    private final String      orgScript;
    private final String      name;

    public LHProcessFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(LHProcessFeedback feedback) {
        this.feedback = feedback;
    }

    protected ReconnectResult createReconnectResult() {
        return new LiveHeaderReconnectResult(routerData);
    }

    public String getName() {
        return T.T.LiveHeaderInvoker_getName_(name);
    }

    public LiveHeaderInvoker(LiveHeaderReconnect liveHeaderReconnect, String script, String user, String pass, String ip, String name) {
        super(liveHeaderReconnect);
        this.orgScript = script;
        this.name = name;
        this.script = prepareScript(script);
        this.user = user;
        this.pass = pass;
        if (ip != null) {
            this.router = ip.trim();
        } else {
            this.router = ip;
        }
    }

    private final String getRouterIP() {
        return internalVariables.get("ip");
    }

    private boolean isAttributeSet(NamedNodeMap attributes, String key) {
        final Node node = attributes.getNamedItem(key);
        return node != null && (StringUtils.equalsIgnoreCase(node.getNodeValue(), "true") || StringUtils.equalsIgnoreCase(node.getTextContent(), "true"));
    }

    @Override
    public void run() throws ReconnectException, InterruptedException {
        if (script == null || script.length() == 0) {
            throw new ReconnectException("No LiveHeader Script found");
        }
        if (!IP.isValidRouterIP(getRouter())) {
            throw new ReconnectException("Invalid Router IP:\"" + getRouter() + "\"");
        }
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("user", user);
        map.put("pass", pass);
        map.put("username", user);
        map.put("password", pass);
        map.put("basicauth", Encoding.Base64Encode(user + ":" + pass));
        map.put("auth", Encoding.Base64Encode(user + ":" + pass));
        if (IP.isLocalIP(getRouter())) {
            map.put("ip", getRouter());
            map.put("routerip", getRouter());
        } else {
            try {
                final String ip = InetAddress.getByName(getRouter()).getHostAddress();
                map.put("ip", ip);
                map.put("routerip", ip);
            } catch (UnknownHostException e) {
                throw new ReconnectException(e);
            }
        }
        map.put("host", getRouter());
        this.internalVariables = Collections.unmodifiableMap(map);
        logger.info("Internal Variables: " + internalVariables);
        this.parsedVariables = new HashMap<String, String>();
        this.verifiedIPs = new HashSet<String>();
        Browser br = new Browser();
        br.setDebug(true);
        br.setVerbose(true);
        /* set custom timeouts here because 10secs is a LONG time ;) */
        br.setCookiesExclusive(true);
        br.setReadTimeout(JsonConfig.create(ReconnectConfig.class).getReconnectBrowserReadTimeout());
        br.setConnectTimeout(JsonConfig.create(ReconnectConfig.class).getReconnectBrowserConnectTimeout());
        br.setProxySelector(new NoProxySelector());
        br.setLogger(logger);
        /* we have to handle 401 special */
        br.setAllowedResponseCodes(new int[] { 401 });
        try {
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
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
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
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    if (feedback != null) {
                        feedback.onNewStep(toDo.getNodeName(), toDo);
                    }
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
                                value = newValue.toString();
                            }
                            putVariable(key, value);
                        }
                        if (feedback != null) {
                            feedback.onVariablesUpdated(internalVariables);
                        }
                    }
                    if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
                        logger.info("Parse response: \r\n" + br.getRequest());
                        final String[] parseLines = splitLines(toDo.getChildNodes().item(0).getNodeValue().trim());
                        for (final String parseLine : parseLines) {
                            String varname = new Regex(parseLine, "(.*?):").getMatch(0);
                            String pattern = new Regex(parseLine, ".*?:(.+)").getMatch(0);
                            if (varname != null && pattern != null) {
                                varname = varname.trim();
                                pattern = pattern.trim();
                                String found = br.getRegex(pattern).getMatch(0);
                                if (found != null) {
                                    found = found.trim();
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    putVariable(varname, found);
                                    if (feedback != null) {
                                        feedback.onVariablesUpdated(internalVariables);
                                    }
                                } else {
                                    found = new Regex(br.getRequest().getHttpConnection() + "", pattern).getMatch(0);
                                    if (found != null) {
                                        found = found.trim();
                                        logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                        putVariable(varname, found);
                                        if (feedback != null) {
                                            feedback.onVariablesUpdated(internalVariables);
                                        }
                                    } else {
                                        logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                        if (feedback != null) {
                                            feedback.onVariableParserFailed(pattern, br.getRequest());
                                        }
                                    }
                                }
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
                            retbr = this.doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), br, isAttributeSet(attributes, "https"), isAttributeSet(attributes, "raw"), isAttributeSet(attributes, "postraw"));
                        } catch (final Exception e) {
                            if (e instanceof ReconnectException) {
                                throw e;
                            }
                            retbr = null;
                        }
                        /* DDoS Schutz */
                        Thread.sleep(350);
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            logger.severe("Request error!");
                            if (feedback != null && retbr != null) {
                                feedback.onRequesterror(retbr.getRequest());
                            }
                        } else {
                            if (retbr.getHttpConnection().getResponseCode() == 401) {
                                /* basic auth error */
                                if (retbr.getHttpConnection().getRequestProperty("Authorization") != null) {
                                    /* auth was send but is wrong */
                                    if (feedback != null) {
                                        feedback.onRequesterror(retbr.getRequest());
                                    }
                                } else {
                                    /* no auth was send */
                                    if (feedback != null) {
                                        feedback.onRequestOK(retbr.getRequest());
                                    }
                                }
                            } else {
                                if (feedback != null) {
                                    feedback.onRequestOK(retbr.getRequest());
                                }
                            }
                            br = retbr;
                        }
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
                        this.parseVariables(feedback, toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);
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
                            logger.finer("Wait " + seconds + " seconds");
                            Thread.sleep(seconds * 1000);
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
                        if (seconds > 0 && br != null) {
                            logger.finer("Timeout set to " + seconds + " seconds");
                            br.setReadTimeout(seconds * 1000);
                            br.setConnectTimeout(seconds * 1000);
                        }
                    }
                }
            }
        } catch (final InterruptedException e) {
            throw e;
        } catch (final ReconnectException e) {
            throw e;
        } catch (final Exception e) {
            logger.log(e);
            throw new ReconnectException(e);
        }
    }

    private Map<String, String> internalVariables = null;
    private Map<String, String> parsedVariables   = null;
    private Set<String>         verifiedIPs;
    private RouterData          routerData;

    public static String prepareScript(String script) {
        if (script != null) {
            script = script.replaceAll("\\[\\[\\[", "<");
            script = script.replaceAll("\\]\\]\\]", ">");
            script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
            script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
            script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
            script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        }
        return script;
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
                    value = Encoding.urlEncode(value);
                } else if (StringUtils.equalsIgnoreCase(method, "URLDECODE")) {
                    value = Encoding.htmlDecode(value);
                } else if (StringUtils.equalsIgnoreCase(method, "UTF8DECODE")) {
                    value = Encoding.UTF8Decode(value);
                } else if (StringUtils.equalsIgnoreCase(method, "UTF8ENCODE")) {
                    value = Encoding.UTF8Encode(value);
                } else if (StringUtils.equalsIgnoreCase(method, "MD5")) {
                    value = JDHash.getMD5(value);
                } else if (StringUtils.equalsIgnoreCase(method, "SHA256")) {
                    value = Hash.getSHA256(value);
                    // required by a huwai router that uses base64(sha256(pass))
                } else if (StringUtils.equalsIgnoreCase(method, "BASE64_SHA256")) {
                    value = Encoding.Base64Encode(Hash.getSHA256(value));
                } else if (StringUtils.equalsIgnoreCase(method, "BASE64")) {
                    value = Encoding.Base64Encode(value);
                } else {
                    logger.info("Unknown/Unsupported method:" + method);
                }
            }
            return value;
        }
    }

    /**
     * DO NOT REMOVE THIS OR REPLACE BY Regex.getLines()
     *
     * REGEX ARE COMPLETE DIFFERENT AND DO NOT TRIM
     */
    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    private void showParsedVariables() {
        logger.finer("Parsed Variables: " + this.parsedVariables);
    }

    private Browser doRequest(String request, final Browser br, final boolean ishttps, final boolean israw, final boolean ispostraw) throws ReconnectException {
        try {
            final String requestType;
            final String path;
            final StringBuilder postContent = new StringBuilder();
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
                    showParsedVariables();
                    final int tmpLength = tmp.length;
                    for (int i = 0; i <= tmpLength; i++) {
                        final String key = params[i - 1];
                        final String modifiedVariable = this.getModifiedVariable(key);
                        logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
                        req.append(modifiedVariable);
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    showParsedVariables();
                    final int tmpLength = tmp.length;
                    for (int i = 1; i <= tmpLength; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        final String key = params[i - 1];
                        final String modifiedVariable = this.getModifiedVariable(key);
                        logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
                        req.append(modifiedVariable);
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                }
                request = req.toString();
            }
            final String[] requestLines = splitLines(request);
            if (requestLines.length == 0) {
                logger.severe("Parse Fehler:" + request);
                return null;
            }
            // RequestType
            tmp = requestLines[0].split(" ");
            if (tmp.length < 2) {
                logger.severe("Konnte Requesttyp nicht finden: " + requestLines[0]);
                return null;
            }
            requestType = tmp[0];
            path = tmp[1];
            boolean headersEnd = false;
            // Zerlege request
            String host = null;
            final int requestLinesLength = requestLines.length;
            for (int li = 1; li < requestLinesLength; li++) {
                if (headersEnd) {
                    if (ispostraw) {
                        postContent.append(requestLines[li].trim());
                    } else {
                        postContent.append(requestLines[li]);
                        postContent.append(new char[] { '\r', '\n' });
                    }
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
                requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());
                if (p[0].trim().equalsIgnoreCase("HOST")) {
                    host = requestLines[li].substring(p[0].length() + 1).trim();
                }
            }
            if (host == null) {
                throw new ReconnectException("Host not available: " + request);
            } else {
                try {
                    verifyHost(host);
                    if (requestProperties != null) {
                        br.getHeaders().putAll(requestProperties);
                    }
                    final String protocoll = ishttps ? "https://" : "http://";
                    if (StringUtils.equalsIgnoreCase(requestType, "AUTH")) {
                        logger.finer("Convert AUTH->GET");
                    }
                    if (StringUtils.equalsIgnoreCase(requestType, "GET") || StringUtils.equalsIgnoreCase(requestType, "AUTH")) {
                        br.getPage(protocoll + host + path);
                    } else if (StringUtils.equalsIgnoreCase(requestType, "POST")) {
                        if (ispostraw) {
                            br.postPageRaw(protocoll + host + path, HexFormatter.hexToByteArray(postContent.toString()));
                        } else {
                            br.postPageRaw(protocoll + host + path, postContent.toString().trim());
                        }
                    } else {
                        logger.severe("Unknown/Unsupported requestType: " + requestType);
                        return null;
                    }
                    return br;
                } catch (final IOException e) {
                    logger.log(e);
                    if (feedback != null) {
                        feedback.onBasicRemoteAPIExceptionOccured(e, br.getRequest());
                    }
                    logger.severe("IO Error: " + e.getLocalizedMessage());
                    return null;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            if (e instanceof ReconnectFailedException) {
                // feedbakc callbacks may throw these
                throw (ReconnectException) e;
            }
            return null;
        }
    }

    private void verifyHost(final String verifyHost) throws ReconnectException {
        final String verify = Browser.getHost(verifyHost, false);
        if (verifiedIPs == null || !verifiedIPs.contains(verify)) {
            try {
                final String[] whiteListArray = JsonConfig.create(LiveHeaderReconnectSettings.class).getHostWhiteList();
                final List<String> whiteList;
                if (whiteListArray != null) {
                    whiteList = Arrays.asList(whiteListArray);
                } else {
                    whiteList = new ArrayList<String>(0);
                }
                if (whiteList.contains(verify)) {
                    if (verifiedIPs != null) {
                        verifiedIPs.add(verify);
                    }
                    return;
                }
                final String verifyIP = HTTPConnectionUtils.resolvHostIP(verify, IPVERSION.IPV4_ONLY)[0].getHostAddress();
                // TODO: Check/Add IPv6 Support. We speak IPv4-Only with Router
                if (whiteList.contains(verifyIP)) {
                    if (verifiedIPs != null) {
                        verifiedIPs.add(verify);
                        verifiedIPs.add(verifyIP);
                    }
                    return;
                }
                if (!IP.isLocalIP(verifyIP)) {
                    throw new ReconnectException("Invalid Router Host:" + verify + "->" + verifyIP);
                }
                final String routerIP = getRouterIP();
                if (!StringUtils.equals(routerIP, verifyIP)) {
                    throw new ReconnectException("IP missmatch! (HOST)" + verifyIP + "!=" + routerIP + "(ROUTER)");
                }
                if (verifiedIPs != null) {
                    verifiedIPs.add(verify);
                }
            } catch (ReconnectException e) {
                throw e;
            } catch (Throwable e) {
                throw new ReconnectException("Invalid Router Host:" + verify, e);
            }
        }
    }

    private boolean putVariable(final String key, final String value) throws ReconnectFailedException {
        if (key != null) {
            final String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (internalVariables.containsKey(lowerKey)) {
                throw new ReconnectFailedException(T.T.failure_variable_overwrite_variable(lowerKey));
            } else {
                if (value == null) {
                    logger.info("Remove Variable:" + lowerKey + "=" + parsedVariables.remove(lowerKey));
                } else {
                    parsedVariables.put(lowerKey, value);
                    logger.info("Set Variable:" + lowerKey + "->" + value);
                }
                return true;
            }
        }
        return false;
    }

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

    private void parseVariables(final LHProcessFeedback feedback, final String patStr, final String[] keys, final Browser br) throws ReconnectFailedException {
        if (br != null && StringUtils.isNotEmpty(patStr)) {
            logger.info("Parse Variables:" + patStr);
            final Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(br + "");
            logger.info("Matches: " + matcher.groupCount());
            if (matcher.find() && matcher.groupCount() > 0) {
                for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                    putVariable(keys[i], matcher.group(i + 1));
                }
                if (feedback != null) {
                    feedback.onVariablesUpdated(parsedVariables);
                }
                return;
            } else {
                for (Entry<String, List<String>> e : br.getRequest().getResponseHeaders().entrySet()) {
                    for (String s : e.getValue()) {
                        String txtx = e.getKey() + ": " + s;
                        matcher = pattern.matcher(txtx);
                        logger.info("Matches: " + matcher.groupCount());
                        if (matcher.find() && matcher.groupCount() > 0) {
                            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                                putVariable(keys[i], matcher.group(i + 1));
                            }
                            if (feedback != null) {
                                feedback.onVariablesUpdated(parsedVariables);
                            }
                            return;
                        }
                    }
                }
                logger.severe("Regular Expression without matches: " + patStr);
                if (feedback != null) {
                    feedback.onVariableParserFailed(patStr, br.getRequest());
                }
            }
        }
    }

    @Override
    protected void testRun() throws ReconnectException, InterruptedException {
        feedback = new LHProcessFeedback() {
            private int successRequests = 0;
            private int failedRequests  = 0;

            public void onBasicRemoteAPIExceptionOccured(IOException e, Request request) throws ReconnectFailedException {
                failedRequests++;
                if (failedRequests > successRequests) {
                    throw new ReconnectFailedException(T.T.failure_variable_request_exception(e.getMessage(), request.getClass().getSimpleName(), request.getUrl()));
                }
            }

            public void onVariablesUpdated(Map<String, String> variables) throws ReconnectFailedException {
            }

            public void onVariableParserFailed(String pattern, Request request) throws ReconnectFailedException {
                throw new ReconnectFailedException(T.T.failure_variable_parser(pattern));
            }

            public void onRequesterror(Request request) throws ReconnectFailedException {
                failedRequests++;
                if (failedRequests > successRequests) {
                    throw new ReconnectFailedException(T.T.failure_variable_request(request.getClass().getSimpleName(), request.getUrl()));
                }
            }

            public void onNewStep(String nodeName, Node toDo) throws ReconnectFailedException {
            }

            public void onRequestOK(Request request) throws ReconnectFailedException {
                successRequests++;
            }
        };
        try {
            run();
        } finally {
            feedback = null;
        }
    }

    public ReconnectResult validate(RouterData test) throws InterruptedException, ReconnectException {
        this.routerData = test;
        return validate();
    }

    public void setRouterData(RouterData rd) {
        this.routerData = rd;
    }
}
