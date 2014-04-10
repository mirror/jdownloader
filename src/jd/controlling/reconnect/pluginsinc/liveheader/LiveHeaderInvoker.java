package jd.controlling.reconnect.pluginsinc.liveheader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
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

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    public String getIp() {
        return ip;
    }

    @Override
    public ReconnectResult validate(ReconnectResult r) throws InterruptedException, ReconnectException {

        try {
            if (r instanceof LiveHeaderReconnectResult) {
                final RouterData rd = ((LiveHeaderReconnectResult) r).getRouterData();

                if (rd != null && rd.getScriptID() != null) {

                    RecollController.getInstance().trackValidateStartAsynch(rd.getScriptID());
                }
            }

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
            if (!IPController.getInstance().getIpState().isOffline()) {
                if (r instanceof LiveHeaderReconnectResult) {
                    final RouterData rd = ((LiveHeaderReconnectResult) r).getRouterData();

                    if (rd != null && rd.getScriptID() != null) {
                        RecollController.getInstance().trackValidateEnd(rd.getScriptID());
                    }
                }
            }
        }

    }

    private String            user;
    private String            pass;
    private String            ip;
    private LHProcessFeedback feedback;
    private String            orgScript;
    private String            name;

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

        return T._.LiveHeaderInvoker_getName_(name);
    }

    public LiveHeaderInvoker(LiveHeaderReconnect liveHeaderReconnect, String script, String user, String pass, String ip, String name) {
        super(liveHeaderReconnect);
        this.orgScript = script;
        this.name = name;
        this.script = prepareScript(script);
        this.user = user;
        this.pass = pass;
        this.ip = ip;
    }

    @Override
    public void run() throws ReconnectException, InterruptedException {
        if (script == null || script.length() == 0) {

        throw new ReconnectException("No LiveHeader Script found");

        }
        if (script.toLowerCase().contains("%%%routerip%%%") && !IP.isValidRouterIP(ip)) throw new ReconnectException(ip + " is no valid routerIP");

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");

        final Document xmlScript;
        this.variables = new HashMap<String, String>();
        this.variables.put("user", user);
        this.variables.put("pass", pass);
        this.variables.put("username", user);
        this.variables.put("password", pass);
        this.variables.put("basicauth", Encoding.Base64Encode(user + ":" + pass));
        this.variables.put("auth", Encoding.Base64Encode(user + ":" + pass));
        this.variables.put("routerip", ip);
        this.variables.put("ip", ip);
        this.variables.put("host", ip);
        this.headerProperties = new HashMap<String, String>();

        Browser br = new Browser();
        br.setDebug(true);
        br.setVerbose(true);
        /* set custom timeouts here because 10secs is a LONG time ;) */
        br.setCookiesExclusive(true);
        br.setReadTimeout(JsonConfig.create(ReconnectConfig.class).getReconnectBrowserReadTimeout());
        br.setConnectTimeout(JsonConfig.create(ReconnectConfig.class).getReconnectBrowserConnectTimeout());
        br.setProxy(HTTPProxy.NONE);
        br.setLogger(logger);
        /* we have to handle 401 special */
        br.setAllowedResponseCodes(new int[] { 401 });
        try {
            xmlScript = JDUtilities.parseXmlString(script, false);
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
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

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
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

                    if (feedback != null) feedback.onNewStep(toDo.getNodeName(), toDo);
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
                                    logger.finer("Variables: " + this.variables);
                                    logger.finer("Headerproperties: " + this.headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 0; i <= tmpLength; i++) {
                                        logger.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req.append(this.getModifiedVariable(params[i - 1]));
                                        if (i < tmpLength) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                } else {
                                    req = new StringBuilder(tmp[0]);
                                    logger.finer("Variables: " + this.variables);
                                    logger.finer("Headerproperties: " + this.headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 1; i <= tmpLength; i++) {
                                        if (i > params.length) {
                                            continue;
                                        }
                                        logger.finer("Replace variable: *********(" + params[i - 1] + ")");
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

                        logger.finer("Variables set: " + this.variables);
                        if (feedback != null) feedback.onVariablesUpdated(variables);
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
                                String found = new Regex(br.getRequest(), pattern).getMatch(0);
                                if (found != null) {
                                    found = found.trim();
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    this.variables.put(varname, found);
                                    if (feedback != null) feedback.onVariablesUpdated(variables);
                                } else {
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                    if (feedback != null) feedback.onVariableParserFailed(pattern, br.getRequest());
                                }
                            }
                        }
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        boolean ishttps = false;
                        boolean israw = false;
                        if (toDo.getChildNodes().getLength() != 1) {
                            logger.severe("A REQUEST Tag is not allowed to have childTags.");
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
                            if (e2 instanceof ReconnectFailedException) { throw e2; }
                            retbr = null;
                        }
                        /* DDoS Schutz */
                        Thread.sleep(350);
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            logger.severe("Request error!");
                            if (feedback != null && retbr != null) feedback.onRequesterror(retbr.getRequest());
                        } else {
                            if (retbr.getHttpConnection().getResponseCode() == 401) {
                                /* basic auth error */
                                if (retbr.getHttpConnection().getRequestProperty("Authorization") != null) {
                                    /* auth was send but is wrong */
                                    if (feedback != null) feedback.onRequesterror(retbr.getRequest());
                                } else {
                                    /* no auth was send */
                                    if (feedback != null) feedback.onRequestOK(retbr.getRequest());
                                }
                            } else {
                                if (feedback != null) feedback.onRequestOK(retbr.getRequest());
                            }
                            br = retbr;
                        }

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
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
                        this.getVariables(feedback, toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {
                        final NamedNodeMap attributes = toDo.getAttributes();
                        final Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            logger.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                            throw new ReconnectException("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");

                        }
                        logger.finer("Wait " + item.getNodeValue() + " seconds");
                        final int seconds = Formatter.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("TIMEOUT")) {
                        final NamedNodeMap attributes = toDo.getAttributes();
                        final Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            logger.severe("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                            throw new ReconnectException("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");

                        }
                        int seconds = Formatter.filterInt(item.getNodeValue());
                        if (seconds < 0) {
                            seconds = 0;
                        }
                        logger.finer("Timeout set to " + seconds + " seconds");
                        if (br != null) {
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

    private HashMap<String, String> variables;
    private HashMap<String, String> headerProperties;
    private RouterData              routerData;

    public static String prepareScript(String script) {
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        return script;
    }

    private String getModifiedVariable(String key) throws ReconnectException {
        if ("timestamp".equalsIgnoreCase(key)) { return "" + System.currentTimeMillis(); }
        // random value
        if (key.toLowerCase(Locale.ENGLISH).startsWith("random:")) {
            try {
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
            } else if (fnc.equalsIgnoreCase("SHA256")) {
                ret = Hash.getSHA256(ret);

                // required by a huwai router that uses base64(sha256(pass))
            } else if (fnc.equalsIgnoreCase("BASE64_SHA256")) {
                ret = Encoding.Base64Encode(Hash.getSHA256(ret));
            } else if (fnc.equalsIgnoreCase("BASE64")) {
                ret = Encoding.Base64Encode(ret);
            }
        }
        return ret;
    }

    /**
     * DO NOT REMOVE THIS OR REPLACE BY Regex.getLines()
     * 
     * REGEX ARE COMPLETE DIFFERENT AND DO NOT TRIM
     */
    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    private Browser doRequest(String request, final Browser br, final boolean ishttps, final boolean israw) throws ReconnectException {
        try {
            final String requestType;
            final String path;
            final StringBuilder post = new StringBuilder();
            String host = null;
            final String http = ishttps ? "https://" : "http://";
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
                    logger.finer("Variables: " + this.variables);
                    logger.finer("Headerproperties: " + this.headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 0; i <= tmpLength; i++) {
                        logger.finer("Replace variable: " + this.getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(this.getModifiedVariable(params[i - 1]));
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    logger.finer("Variables: " + this.variables);
                    logger.finer("Headerproperties: " + this.headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 1; i <= tmpLength; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        logger.finer("Replace variable: " + this.getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(this.getModifiedVariable(params[i - 1]));
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
                requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());

                if (p[0].trim().equalsIgnoreCase("HOST")) {
                    host = requestLines[li].substring(p[0].length() + 1).trim();
                }
            }

            if (host == null) {
                logger.severe("Host nicht gefunden: " + request);
                return null;
            }
            try {
                if (requestProperties != null) {
                    br.getHeaders().putAll(requestProperties);
                }
                if (requestType.equalsIgnoreCase("GET")) {
                    br.getPage(http + host + path);
                } else if (requestType.equalsIgnoreCase("POST")) {
                    final String poster = post.toString().trim();
                    br.postPageRaw(http + host + path, poster);
                } else if (requestType.equalsIgnoreCase("AUTH")) {
                    logger.finer("Convert AUTH->GET");
                    br.getPage(http + host + path);
                } else {
                    logger.severe("Unknown requesttyp: " + requestType);
                    return null;
                }
                return br;
            } catch (final IOException e) {
                logger.log(e);
                if (feedback != null) feedback.onBasicRemoteAPIExceptionOccured(e, br.getRequest());
                logger.severe("IO Error: " + e.getLocalizedMessage());
                return null;
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

    private void getVariables(LHProcessFeedback feedback, final String patStr, final String[] keys, final Browser br) throws ReconnectFailedException {
        logger.info("GetVariables");
        if (br == null) { return; }
        // patStr="<title>(.*?)</title>";
        logger.finer(patStr);
        final Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // logger.info(requestInfo.getHtmlCode());
        Matcher matcher = pattern.matcher(br + "");
        logger.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {

            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                this.variables.put(keys[i], matcher.group(i + 1));

                logger.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }

            if (feedback != null) feedback.onVariablesUpdated(variables);
            return;
        } else {

            for (Entry<String, List<String>> e : br.getRequest().getResponseHeaders().entrySet()) {
                for (String s : e.getValue()) {
                    String txtx = e.getKey() + ": " + s;
                    matcher = pattern.matcher(txtx);
                    logger.info("Matches: " + matcher.groupCount());
                    if (matcher.find() && matcher.groupCount() > 0) {
                        for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                            this.variables.put(keys[i], matcher.group(i + 1));

                            logger.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
                        }
                        if (feedback != null) feedback.onVariablesUpdated(variables);
                        return;
                    }
                }
            }
            logger.severe("Regular Expression without matches: " + patStr);
            if (feedback != null) feedback.onVariableParserFailed(patStr, br.getRequest());
        }
    }

    @Override
    protected void testRun() throws ReconnectException, InterruptedException {
        feedback = new LHProcessFeedback() {
            private int successRequests;
            private int failedRequests;

            {
                successRequests = 0;
                failedRequests = 0;
            }

            public void onBasicRemoteAPIExceptionOccured(IOException e, Request request) throws ReconnectFailedException {
                failedRequests++;
                if (failedRequests > successRequests) throw new ReconnectFailedException("Request Failed");
            }

            public void onVariablesUpdated(HashMap<String, String> variables) throws ReconnectFailedException {
            }

            public void onVariableParserFailed(String pattern, Request request) throws ReconnectFailedException {
                throw new ReconnectFailedException("Variable Parser Failed");
            }

            public void onRequesterror(Request request) throws ReconnectFailedException {
                failedRequests++;
                if (failedRequests > successRequests) throw new ReconnectFailedException("Request Failed");
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

}
