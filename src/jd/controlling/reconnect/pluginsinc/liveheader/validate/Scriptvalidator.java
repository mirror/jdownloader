package jd.controlling.reconnect.pluginsinc.liveheader.validate;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.nutils.Formatter;
import jd.nutils.encoding.Base64;

import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.jdownloader.logging.LogController;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Scriptvalidator {

    protected RouterData rd;

    private LogSource    logger;

    public Scriptvalidator(RouterData rd) {
        this.rd = rd;
        logger = LogController.getInstance().getLogger("Scriptvalidator");
        logger.info("Validate:\r\n" + JSonStorage.serializeToJson(rd));
    }

    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    public static Document parseXmlString(final String xmlString, final boolean validating) throws SAXException, IOException, ParserConfigurationException {
        if (xmlString == null) {
            return null;
        }

        // Create a builder factory
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        final InputSource inSource = new InputSource(new StringReader(xmlString));

        // Create the builder and parse the file
        final Document doc = factory.newDocumentBuilder().parse(inSource);

        return doc;

    }

    public String toOverView(String script) throws Exception {
        String lc = script.toLowerCase(Locale.ENGLISH);
        if (!lc.contains("[[[request")) {
            throw new Exception("No Request Tag");
        }

        final HashMap<String, String> map = new HashMap<String, String>();
        // map.put("user", Encoding.urlEncode("<Username required>"));
        // map.put("pass", Encoding.urlEncode("<Password required>"));
        // map.put("username", Encoding.urlEncode("<Username required>"));
        // map.put("password", Encoding.urlEncode("<Password required>"));
        // map.put("basicauth", Encoding.urlEncode("<Basic Authentication. Username and Password required>"));
        // map.put("auth", Encoding.urlEncode("<Basic Authentication. Username and Password required>"));
        //
        map.put("ip", "your.router.ip");
        map.put("routerip", "your.router.ip");
        map.put("host", "your.router.ip");

        this.internalVariables = Collections.unmodifiableMap(map);

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

        final Document xmlScript = parseXmlString(script, false);
        if (xmlScript == null) {

            throw new Exception("Error while parsing the xml string");
        }
        final Node root = xmlScript.getChildNodes().item(0);
        if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {

            throw new Exception("Error while parsing the xml string. Root Node must be [[[HSRC]]]*[/HSRC]");
        }
        final NodeList steps = root.getChildNodes();
        for (int step = 0; step < steps.getLength(); step++) {

            final Node current = steps.item(step);
            if (current.getNodeType() == 3) {
                continue;
            }
            if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                throw new Exception("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
            }
            final NodeList toDos = current.getChildNodes();
            final int toDosLength = toDos.getLength();
            for (int toDoStep = 0; toDoStep < toDosLength; toDoStep++) {
                final Node toDo = toDos.item(toDoStep);

                if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {
                    final NamedNodeMap attributes = toDo.getAttributes();
                    for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                        final String key = attributes.item(attribute).getNodeName();
                        if (key.toLowerCase().equals("router")) {
                            continue;
                        }
                        if (key.toLowerCase().equals("routername")) {
                            continue;
                        }

                        String value = attributes.item(attribute).getNodeValue();
                        if (key.matches("ip\\d+") && !value.contains("%%%")) {
                            throw new RetryWithReplacedScript(rd.getScript(), key, "\"" + value + "\"", "\"%%%routerip%%%\"");

                        }
                        final String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%", -1);
                        final String[] params = new Regex(value, "%%%(.*?)%%%").getColumn(-1);
                        if (params.length > 0) {
                            final StringBuilder newValue;
                            newValue = new StringBuilder(tmp[0]);

                            final int tmpLength = tmp.length;
                            for (int i = 1; i <= tmpLength; i++) {
                                if (i > params.length) {
                                    continue;
                                }
                                // logger.finer("Replace variable: *********(" + params[i - 1] + ")");
                                newValue.append(this.getModifiedVariable(params[i - 1]));
                                if (i < tmpLength) {
                                    newValue.append(tmp[i]);
                                }
                            }
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
                            putVariable(varname, URLEncoder.encode("<Variable: " + varname + ">", "ASCII"));
                            append(sb, "\t-> Search in HTML Response:  " + varname + " = Regex:" + pattern);

                        }
                    }
                }
                if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                    if (toDo.getChildNodes().getLength() != 1) {
                        // logger.severe("A REQUEST Tag is not allowed to have childTags.");
                        throw new Exception("A REQUEST Tag is not allowed to have childTags.");
                    }
                    final NamedNodeMap attributes = toDo.getAttributes();
                    // Browser retbr = null;

                    try {
                        this.doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), sb, attributes.getNamedItem("https") != null, attributes.getNamedItem("raw") != null);
                    } catch (final Exception e) {
                        if (e instanceof Exception) {
                            throw e;
                        }
                        // retbr = null;
                    }
                    /* DDoS Schutz */

                }
                if (StringUtils.equalsIgnoreCase(toDo.getNodeName(), "RESPONSE")) {
                    // logger.finer("get Response");
                    if (toDo.getChildNodes().getLength() != 1) {
                        // logger.severe("A RESPONSE Tag is not allowed to have childTags.");
                        throw new Exception("A RESPONSE Tag is not allowed to have childTags.");
                    }
                    final NamedNodeMap attributes = toDo.getAttributes();
                    if (attributes.getNamedItem("keys") == null) {
                        // logger.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                        throw new Exception("A RESPONSE Node needs a Keys Attribute: " + toDo);
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
                        // logger.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                        throw new Exception("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
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
                        // logger.severe("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                        throw new Exception("A valid timeout must be set: e.g.: <TIMEOUT seconds=\"15\"/>");
                    }
                    final int seconds = Formatter.filterInt(item.getNodeValue());
                    if (seconds > 0) {
                        // logger.finer("Timeout set to " + seconds + " seconds");
                        append(sb, "Set HTTP Timeout to " + TimeFormatter.formatMilliSeconds(seconds * 1000, 0));

                    }
                }
            }
        }
        return sb.toString();
    }

    private void doRequest(String request, StringBuilder sb, boolean ishttps, boolean israw) throws Exception, IOException {

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
                    // logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
                    req.append(modifiedVariable);
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
                    // logger.finer("Replace variable: " + modifiedVariable + "(" + key + ")");
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
            throw new Exception("Parse Fehler:" + request);

        }
        // RequestType
        tmp = requestLines[0].split(" ");
        if (tmp.length < 2) {
            throw new Exception("Bad Request Type: " + requestLines[0] + "\r\n" + request);

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
                // logger.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
                headersEnd = true;
                li--;
                continue;
            }
            requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());

            if (StringUtils.equalsIgnoreCase(p[0].trim(), "Referer") || StringUtils.equalsIgnoreCase(p[0].trim(), "Referrer")) {
                onHost(new URL(requestProperties.get(p[0].trim())).getHost());
            }
            if (p[0].trim().equalsIgnoreCase("HOST")) {
                host = requestLines[li].substring(p[0].length() + 1).trim();
            }
        }
        if (host == null) {
            throw new Exception("Host not available: " + request);
        } else {

            // verifyHost(host);
            if (requestProperties != null) {
                // sb.getHeaders().putAll(requestProperties);
            }
            final String protocoll = ishttps ? "https://" : "http://";
            if (StringUtils.equalsIgnoreCase(requestType, "AUTH")) {
                // logger.finer("Convert AUTH->GET");
            }
            onHost(host);
            if (StringUtils.equalsIgnoreCase(requestType, "GET") || StringUtils.equalsIgnoreCase(requestType, "AUTH")) {

                URL url = new URL(protocoll + host + path);

                append(sb, "HTTP Request " + requestType + " " + protocoll + host + url.getPath());
                String cookie = requestProperties.get("Cookie");
                if (StringUtils.isNotEmpty(cookie)) {
                    onCookie(cookie);
                    append(sb, "\tCookie:\t\t" + cookie);
                }
                String authorization = requestProperties.get("Authorization");
                if (StringUtils.isNotEmpty(authorization)) {
                    onAuth(authorization);
                    append(sb, "\tAuthorization:\t\t" + authorization);
                }
                int i = 1;
                try {
                    for (KeyValuePair pa : HttpConnection.parseParameterList(url.getQuery())) {

                        append(sb, "\tParameter #" + (i++) + ": \t" + pa.key + "\t=\t" + pa.value);
                        onParameter(pa.key, pa.value);
                    }
                } catch (Exception e) {
                    throw e;
                }
                // sb.getPage(protocoll + host + path);
            } else if (StringUtils.equalsIgnoreCase(requestType, "POST")) {
                final String poster = post.toString().trim();

                URL url = new URL(protocoll + host + path);

                append(sb, "HTTP Request " + requestType + " " + protocoll + host + path);
                String cookie = requestProperties.get("Cookie");
                if (StringUtils.isNotEmpty(cookie)) {
                    onCookie(cookie);
                    append(sb, "\tCookie:\t\t" + cookie);
                }
                String authorization = requestProperties.get("Authorization");
                if (StringUtils.isNotEmpty(authorization)) {
                    onAuth(authorization);
                    append(sb, "\tAuthorization:\t\t" + authorization);
                }
                int i = 1;
                try {
                    for (KeyValuePair pa : HttpConnection.parseParameterList(url.getQuery())) {
                        append(sb, "\tParameter #" + (i++) + ": \t" + pa.key + "\t=\t" + pa.value);
                        onParameter(pa.key, pa.value);
                    }
                    for (KeyValuePair pa : HttpConnection.parseParameterList(poster)) {
                        append(sb, "\tParameter #" + (i++) + ": \t" + pa.key + "\t=\t" + pa.value);
                        onParameter(pa.key, pa.value);
                    }
                } catch (Exception e) {
                    throw e;
                }
                // sb.postPageRaw(protocoll + host + path, poster);
            } else {
                // logger.severe("Unknown/Unsupported requestType: " + requestType);
                throw new Exception("Unknown/Unsupported requestType: " + requestType);
            }

        }

    }

    private void onHost(String host) throws Exception {
        if (!host.startsWith("your.router.ip")) {

            throw new RetryWithReplacedScript(rd.getScript(), host, "%%%routerip%%%");
            // throw new Exception("Request to bad url " + host);
        }

    }

    protected HashSet<String> exceptionsParameterKeys  = new HashSet<String>();
    {
        exceptionsParameterKeys.add("var:pagename".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("var:errorpagename".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("pppName".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("serviceName".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("wanName".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("ifname".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("device_name".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("logger:settings/facility/user".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("EmWeb_ns:vim:3.passthrough".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("wan_login".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("hostName".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("encaps0:pppoa:settings/auth_type".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("EmWeb_ns:vim:4.ImServices.ipwan0.1:webcontrol".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("mbg_webname".toLowerCase(Locale.ENGLISH));
        exceptionsParameterKeys.add("intfName".toLowerCase(Locale.ENGLISH));

    }
    protected HashSet<String> defaultPasswords         = new HashSet<String>();
    {
        defaultPasswords.add("administrator");
        defaultPasswords.add("admin");
        defaultPasswords.add("123456");
        defaultPasswords.add("1111");
        defaultPasswords.add("1234");
        defaultPasswords.add("0000");
        defaultPasswords.add("password");
        defaultPasswords.add("pass");
    }
    protected HashSet<String> defaultUsernames         = new HashSet<String>();
    {
        defaultUsernames.add("admin");
        defaultUsernames.add("user");
        defaultUsernames.add("name");
        defaultUsernames.add("username");
        defaultUsernames.add("root");
        defaultUsernames.add("0000");
        defaultUsernames.add("123456");
        defaultUsernames.add("1234");
        defaultUsernames.add("administrator");

    }
    protected HashSet<String> whitelistValues          = new HashSet<String>();
    {
        whitelistValues.add("true");
        whitelistValues.add("false");
        whitelistValues.add("0");
        whitelistValues.add("1");
        whitelistValues.add("2");
        whitelistValues.add("3");
        whitelistValues.add("4");
        whitelistValues.add("5");
        whitelistValues.add("6");
        whitelistValues.add("7");
        whitelistValues.add("8");
        whitelistValues.add("9");
        whitelistValues.add("Internet");
        whitelistValues.add("");
        whitelistValues.add("");
        whitelistValues.add("");
        whitelistValues.add("");
        whitelistValues.add("");
        whitelistValues.add("");
    }
    protected HashSet<String> replacedDefaultPasswords = new HashSet<String>();
    protected HashSet<String> replacedDefaultUsernames = new HashSet<String>();
    protected HashSet<String> replacedDefaulAuth       = new HashSet<String>();

    private void onParameter(String key, String value) throws Exception {
        if (key == null || value == null) {

            return;
        }
        if (exceptionsParameterKeys.contains(key.toLowerCase(Locale.ENGLISH))) {
            return;
        }
        if (whitelistValues.contains(value.toLowerCase(Locale.ENGLISH))) {
            return;
        }

        if (StringUtils.isEmpty(value) || value.length() < 3) {
            return;
        }
        if (value.contains("<Variable:") && value.contains(">")) {
            return;
        }

        if (value.contains(URLEncoder.encode("<Variable:", "ASCII")) && value.contains(URLEncoder.encode(">", "ASCII"))) {
            return;
        }
        if (isPasswordParameter(key, value)) {
            System.out.println(key + "=" + value);

            replacePasswordParameter(key, value);

        }

        if (isUsernameParameter(key, value)) {
            replaceUsernameParameter(key, value);

        }

    }

    protected void replaceUsernameParameter(String key, String value) throws RetryWithReplacedScript, Exception {
        System.out.println(key + "=" + value);
        // if (!UIOManager.I().showConfirmDialog(0, T._.please_check(), T._.please_check_sensitive_data_before_share(key + "=" + value), new
        // AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI._.lit_yes(), _GUI._.lit_no())) {

        if (defaultUsernames.contains(value.toLowerCase(Locale.ENGLISH))) {
            replacedDefaultUsernames.add(value);
        }
        throw new RetryWithReplacedScript(rd.getScript(), key, value, "%%%username%%%");
        // }
    }

    protected void replacePasswordParameter(String key, String value) throws RetryWithReplacedScript, Exception {
        if (defaultPasswords.contains(value.toLowerCase(Locale.ENGLISH))) {
            replacedDefaultPasswords.add(value);
        }
        throw new RetryWithReplacedScript(rd.getScript(), key, value, "%%%password%%%");

    }

    private boolean isUsernameParameter(String key, String value) {

        if (key.toLowerCase().contains("usr")) {
            return true;
        }
        if (key.toLowerCase().contains("use")) {
            return true;
        }
        // if (key.toLowerCase().contains("login")) {
        // return true;
        // }
        if (key.toLowerCase().contains("name")) {
            return true;
        }
        return false;
    }

    private boolean isPasswordParameter(String key, String value) {
        if (key.toLowerCase().contains("pw")) {
            return true;
        }
        if (key.toLowerCase().contains("pas")) {
            return true;
        }
        if (key.toLowerCase().contains("auth")) {
            return true;
        }
        if (key.toLowerCase().contains("crede")) {
            return true;
        }
        return false;
    }

    private void onAuth(String authorization) throws RetryWithReplacedScript, Exception {

        if (!"Basic <Variable:basicauth>".equals(authorization)) {

            if (authorization.startsWith("Basic ")) {
                String dec = new String(Base64.decode(authorization.substring("Basic ".length())));
                int first = dec.indexOf(":");
                String username = dec.substring(0, first);
                String password = dec.substring(first + 1);
                replaceAuthHeader(authorization, username, password);
            }
            System.out.println(authorization);
        }

    }

    protected void replaceAuthHeader(String authorization, String username, String password) throws RetryWithReplacedScript, Exception {
        if (defaultPasswords.contains(password.toLowerCase(Locale.ENGLISH)) && defaultUsernames.contains(username.toLowerCase(Locale.ENGLISH))) {
            replacedDefaulAuth.add(authorization.substring("Basic ".length()));
        }
        throw new RetryWithReplacedScript(rd.getScript(), authorization.substring("Basic ".length()), "%%%basicauth%%%");
    }

    private void onCookie(String cookie) throws RetryWithReplacedScript, Exception {
        String encd = URLEncoder.encode("<Variable:", "ASCII");
        if (!cookie.contains("<Variable:") && !cookie.contains(encd)) {
            System.out.println(cookie);
            throw new RetryWithReplacedScript(rd.getScript(), null, cookie, "%%%Set-Cookie%%%");
        }
    }

    private void append(StringBuilder sb, String string) {
        if (sb.length() > 0) {
            sb.append("\r\n");
        }
        sb.append(string);
    }

    private void putVariable(String key, String value) throws Exception {
        if (key != null) {
            final String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (internalVariables.containsKey(lowerKey)) {
                throw new Exception("Cannot change internal varbiable:" + lowerKey);
            } else {
                if (value == null) {
                } else {
                    parsedVariables.put(lowerKey, value);

                }

            }
        }

    }

    private Map<String, String> internalVariables = null;
    private Map<String, String> parsedVariables   = null;

    private String getVariable(final String key) throws Exception {
        if (key != null) {
            final String lowerKey = key.toLowerCase(Locale.ENGLISH);
            if (internalVariables.containsKey(lowerKey)) {
                return internalVariables.get(lowerKey);
            } else if (parsedVariables.containsKey(lowerKey)) {
                return parsedVariables.get(lowerKey);
            } else {

            }
        }
        return null;
    }

    private String getModifiedVariable(String key) throws Exception {
        if (StringUtils.equalsIgnoreCase("timestamp", key)) {
            return "<Variable: " + "CurrentTime" + ">";
        }
        if (StringUtils.containsIgnoreCase(key, "random:")) {
            return "<Variable: " + key + ">";
        }
        int index = key.indexOf(":::");
        String value = null;
        if (index == -1) {
            value = getVariable(key);
            return value == null ? "<Variable:" + key + ">" : value;
        }

        while ((index = key.indexOf(":::")) >= 0) {

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
                throw new Exception("Unsupported Type: " + method);

            }
        }
        return value;

    }

    protected ArrayList<Exception> changes = new ArrayList<Exception>();

    public void run() throws Exception {
        String oldScriptID = rd.getScriptID();
        String oldScript = rd.getScript();
        while (true) {
            try {
                toOverView(rd.getScript());
                if (changes.size() == 0) {
                    // setStatus(oldScriptID, ScriptStatus.VALIDATED);
                    return;
                } else {
                    rd.setScriptID(null);
                    String newScriptID;
                    setScript(oldScriptID);
                    // setStatus(rd.getScriptID(), ScriptStatus.VALIDATED);
                    setChanges(rd.getScriptID());

                }
                if (replacedDefaultPasswords.size() > 0 || replacedDefaultUsernames.size() > 0 || replacedDefaulAuth.size() > 0) {
                    String s = rd.getScript();
                    for (String e : replacedDefaultPasswords) {
                        s = s.replace("%%%password%%%", e);
                    }
                    for (String e : replacedDefaultUsernames) {
                        s = s.replace("%%%username%%%", e);
                    }
                    for (String e : replacedDefaulAuth) {
                        s = s.replace("%%%basicauth%%%", e);
                    }
                    RouterData copy = JSonStorage.restoreFromString(JSonStorage.serializeToJson(rd), RouterData.TYPE_REF);
                    copy.setScript(s);
                    copy.setScriptID(null);
                    // copy.setStatus(ScriptStatus.VALIDATED_DEFAULT);
                    String id = copy.getScriptID();
                    addCopy(copy);
                }
                return;
            } catch (RetryWithReplacedScript e) {
                if (rd.getScript().equals(e.getNewScript())) {

                    // setStatus(oldScriptID, ScriptStatus.EXCEPTION);
                    return;
                } else {
                    changes.add(e);
                    rd.setScript(e.getNewScript());
                    rd.setScriptID(null);
                }
            }
        }
    }

    protected void addCopy(RouterData copy) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InternalApiException {
        // try {
        // if (db.addEntry(copy)) {
        // for (Exception e : changes) {
        // db.appendChanges(copy.getScriptID(), e.toString());
        // }
        // db.appendChanges(copy.getScriptID(), "Kept Default Data " + replacedDefaultPasswords + " - " + replacedDefaultUsernames + " - " +
        // replacedDefaulAuth);
        // }
        // } catch (Throwable e) {
        // e.printStackTrace();
        // }
    }

    protected void setChanges(String scriptID) throws InternalApiException {
        // for (Exception e : changes) {
        // db.appendChanges(rd.getScriptID(), e.toString());
        // }
    }

    protected void setScript(String oldScriptID) throws InternalApiException {
        // try {
        // String newScriptID;
        // db.setScript(oldScriptID, newScriptID = rd.getScriptID(), rd.getScript());
        // } catch (InternalApiException e) {
        // if (Exceptions.containsInstanceOf(e, DuplicateKey.class)) {
        // db.remove(oldScriptID);
        // } else {
        // e.printStackTrace();
        // }
        //
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    // protected void setStatus(String oldScriptID, ScriptStatus validated) throws InternalApiException {
    // db.setStatus(oldScriptID, validated);
    // }

}
