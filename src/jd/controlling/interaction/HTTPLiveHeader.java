package jd.controlling.interaction;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 * 
 */
public class HTTPLiveHeader extends Interaction {
    private transient static boolean enabled     = true;

    /**
     * serialVersionUID
     */
    private static final String      NAME        = "HTTP Live Header";

    /**
     * Maximal 10 versuche
     */
    private static final int         MAX_RETRIES = 10;

    private int

                                     retries     = 0;

    private HashMap<String, String>  variables;

    private HashMap<String, String>  headerProperties;

    public static String[] splitLines(String source) {
        return source.split("\r\n|\r|\n");
    }

    /**
     * @param xmlString
     * @param validating
     * @return XML Dokument
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Document parseXmlString(String xmlString, boolean validating) throws SAXException, IOException, ParserConfigurationException {

        // Create a builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        InputSource inSource = new InputSource(new StringReader(xmlString));

        // Create the builder and parse the file
        Document doc = factory.newDocumentBuilder().parse(inSource);

        return doc;

    }

    /**
     * Gibt das Attribut zu key in childNode zurück
     * 
     * @param childNode
     * @param key
     * @return String Atribut
     */
    public static String getAttribute(Node childNode, String key) {
        NamedNodeMap att = childNode.getAttributes();
        if (att == null || att.getNamedItem(key) == null) {
            logger.severe("ERROR: XML Attribute missing: " + key);
            return null;
        }
        return att.getNamedItem(key).getNodeValue();
    }

    @Override
    public boolean doInteraction(Object arg) {
        if (!isEnabled()) {
            logger.info("Reconnect deaktiviert");
            return false;
        }
        // Hole die Config parameter. Über die Parameterkeys wird in der
        // initConfig auch der ConfigContainer für die Gui vorbereitet
        Configuration configuration = JDUtilities.getConfiguration();

        String script = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);

        String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        String ip = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP);
        retries++;
        logger.info("Starting  #" + retries);
        if (user != null || pass != null) Authenticator.setDefault(new InternalAuthenticator(user, pass));

        if (script == null) {

            return parseError("Kein RequestText gesetzt");
        }
        String preIp = JDUtilities.getIPAddress();

        logger.finer("IP befor: " + preIp);

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST>", "<REQUEST><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE>", "<RESPONSE><![CDATA[");
        script = script.replaceAll("</RESPONSE>", "]]></RESPONSE>");
        Document xmlScript;
        variables = new HashMap<String, String>();
        variables.put("user", user);
        variables.put("pass", pass);
        variables.put("routerip", ip);
        headerProperties = new HashMap<String, String>();
        try {
            xmlScript = parseXmlString(script, false);
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                return parseError("Root Node must be [[[HSRC]]]*[/HSRC]");
            }
            RequestInfo requestInfo = null;
            NodeList steps = root.getChildNodes();
            for (int step = 0; step < steps.getLength(); step++) {
                Node current = steps.item(step);
                short type = current.getNodeType();

                if (current.getNodeType() == 3) {
                    // logger.finer("Skipped: " + current.getNodeName());
                    continue;
                }
                if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                    return parseError("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                }
                NodeList toDos = current.getChildNodes();
                for (int toDoStep = 0; toDoStep < toDos.getLength(); toDoStep++) {
                    Node toDo = toDos.item(toDoStep);
                    if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {
                        NamedNodeMap attributes = toDo.getAttributes();
                        for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                            variables.put(attributes.item(attribute).getNodeName(), attributes.item(attribute).getNodeValue());
                        }

                        logger.finer("Variables set: " + variables);
                    }
                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        if (toDo.getChildNodes().getLength() != 1) {
                            return parseError("A REQUEST Tag is not allowed to have childTags.");
                        }
                        requestInfo = doRequest(toDo.getChildNodes().item(0).getNodeValue().trim());
                        if (requestInfo == null) {
                            logger.severe("Request error in " + toDo.getChildNodes().item(0).getNodeValue().trim());
                        }
                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
                        logger.finer("get Response");
                        if (toDo.getChildNodes().getLength() != 1) {
                            return parseError("A RESPONSE Tag is not allowed to have childTags.");
                        }

                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {
                            return parseError("A RESPONSE Node needs a Keys Attribute: " + toDo);
                        }
                        String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                        getVariables(toDo.getChildNodes().item(0).getNodeValue().trim(), keys, requestInfo);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {

                        NamedNodeMap attributes = toDo.getAttributes();
                        Node item = attributes.getNamedItem("seconds");
                        logger.finer("Wait " + item.getNodeValue() + " seconds");
                        if (item == null) {
                            return parseError("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                        }
                        int seconds = JDUtilities.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);

                    }
                }

            }

        }
        catch (SAXException e) {
            return this.parseError(e.getMessage());
        }

        catch (ParserConfigurationException e) {

            e.printStackTrace();
            return this.parseError(e.getMessage());
        }
        catch (Exception e) {

            e.printStackTrace();
            return this.parseError(e.getCause() + " : " + e.getMessage());
        }

        int waittime = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, 0);
        int maxretries = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
        int waitForIp = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
        logger.finer("Wait " + waittime + " seconds ...");
        try {
            Thread.sleep(waittime * 1000);
        }
        catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress();
        logger.finer("Ip after: " + afterIP);
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        while (System.currentTimeMillis() <= endTime && afterIP.equals(preIp)) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            logger.finer("Ip Check: " + afterIP);
        }
        if (!afterIP.equals(preIp)) {
            return true;
        }
        if (retries <= maxretries) {
            return doInteraction(arg);
        }
        return false;
    }

    private void getVariables(String patStr, String[] keys, RequestInfo requestInfo) {
        if (requestInfo == null) return;

        // patStr="<title>(.*?)</title>";
        Pattern pattern = Pattern.compile(patStr);

        // logger.info(requestInfo.getHtmlCode());
        Matcher matcher = pattern.matcher(requestInfo.getHtmlCode());
        logger.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {
            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                variables.put(keys[i], matcher.group(i + 1));
                logger.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }
        }
        else {
            logger.severe("Regular Expression without matches: " + patStr);
        }

    }

    private RequestInfo doRequest(String request) throws MalformedURLException {
        String requestType;
        String path;
        String post = "";
        String host = null;
        RequestInfo requestInfo;
        HashMap<String, String> requestProperties = new HashMap<String, String>();
        String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
        Vector<String> params = Plugin.getAllSimpleMatches(request, "%%%°%%%", 1);
        if (params.size() > 0) {
            String req;
            if (request.startsWith(params.get(0))) {
                req = "";
                logger.finer("Variables: " + this.variables);
                logger.finer("Headerproperties: " + this.headerProperties);
                for (int i = 0; i <= tmp.length; i++) {
                    logger.finer("Replace variable: " + getModifiedVariable(params.get(i - 1)) + "(" + params.get(i - 1) + ")");

                    req += getModifiedVariable(params.get(i - 1));
                    if (i < tmp.length) {
                        req += tmp[i];
                    }
                }
            }
            else {
                req = tmp[0];
                logger.finer("Variables: " + this.variables);
                logger.finer("Headerproperties: " + this.headerProperties);
                for (int i = 1; i <= tmp.length; i++) {
                    if(i>params.size())continue;
                    logger.finer("Replace variable: " + getModifiedVariable(params.get(i - 1)) + "(" + params.get(i - 1) + ")");

                    req += getModifiedVariable(params.get(i - 1));
                    if (i < tmp.length) {
                        req += tmp[i];
                    }
                }
            }

            request = req;
        }
        String[] requestLines = splitLines(request);
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
        logger.finer("RequestType: " + requestType);
        logger.finer("Path: " + path);
        boolean headersEnd = false;
        // Zerlege request

        for (int li = 1; li < requestLines.length; li++) {

            if (headersEnd) {
                post += requestLines[li] + "\r\n";
                continue;
            }
            if (requestLines[li].trim().length() == 0) {
                headersEnd = true;

                continue;
            }
            String[] p = requestLines[li].split("\\:");
            if (p.length < 2) {
                logger.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
                headersEnd = true;
                li--;
                continue;
            }
            requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());

            if (p[0].trim().equalsIgnoreCase("HOST")) host = requestLines[li].substring(p[0].length() + 1).trim();
        }

        if (host == null) {
            logger.severe("Host nicht gefunden: " + request);
            return null;
        }
        try {
            if (requestType.equalsIgnoreCase("GET")) {
                logger.finer("GET " + "http://" + host + path);
                HttpURLConnection httpConnection = (HttpURLConnection) new URL("http://" + host + path).openConnection();
                if (requestProperties != null) {
                    Set<String> keys = requestProperties.keySet();
                    Iterator<String> iterator = keys.iterator();
                    String key;
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        httpConnection.setRequestProperty(key, requestProperties.get(key));
                    }
                }
                httpConnection.setConnectTimeout(5000);
                httpConnection.setReadTimeout(5000);
                httpConnection.setInstanceFollowRedirects(false);
                requestInfo = Plugin.readFromURL(httpConnection);
                requestInfo.setConnection(httpConnection);

            }
            else {
                logger.finer("POST " + "http://" + host + path + " " + post);
                HttpURLConnection httpConnection = (HttpURLConnection) new URL("http://" + host + path).openConnection();
                httpConnection.setInstanceFollowRedirects(false);
                httpConnection.setConnectTimeout(5000);
                httpConnection.setReadTimeout(5000);
                if (requestProperties != null) {
                    Set<String> keys = requestProperties.keySet();
                    Iterator<String> iterator = keys.iterator();
                    String key;
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        httpConnection.setRequestProperty(key, requestProperties.get(key));
                    }
                }
                httpConnection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
                if (post != null) wr.write(post);
                wr.flush();
                requestInfo = Plugin.readFromURL(httpConnection);
                wr.close();
                requestInfo.setConnection(httpConnection);
            }

            Set<Entry<String, List<String>>> set = requestInfo.getHeaders().entrySet();
            logger.finer("Answer: ");
            for (Map.Entry<String, List<String>> me : set) {
                if (me.getValue() != null && me.getValue().size() > 0) {
                    this.headerProperties.put(me.getKey(), me.getValue().get(0));
                    logger.finer(me.getKey() + " : " + me.getValue().get(0));
                }
            }

        }
        catch (IOException e) {

            logger.severe("IO Error: " + e.getLocalizedMessage());
            return null;
        }

        return requestInfo;

    }

    private String getModifiedVariable(String key) {

        if (key.indexOf(":::") == -1 && headerProperties.containsKey(key)) return headerProperties.get(key);
        if (key.indexOf(":::") == -1) return variables.get(key);
        String ret = variables.get(key.substring(key.lastIndexOf(":::") + 3));
        if (headerProperties.containsKey(key.substring(key.lastIndexOf(":::") + 3))) {
            ret = headerProperties.get(key.substring(key.lastIndexOf(":::") + 3));
        }
        if (ret == null) return "";
        int id;
        String fnc;
        while ((id = key.indexOf(":::")) >= 0) {
            fnc = key.substring(0, id);
            key = key.substring(id + 3);

            if (fnc.equalsIgnoreCase("URLENCODE")) {
                ret = JDUtilities.urlEncode(ret);
            }
            else if (fnc.equalsIgnoreCase("URLDECODE")) {
                ret = JDUtilities.htmlDecode(ret);
            }
            else if (fnc.equalsIgnoreCase("UTF8DECODE")) {
                ret = JDUtilities.UTF8Decode(ret);
            }
            else if (fnc.equalsIgnoreCase("UTF8ENCODE")) {
                ret = JDUtilities.UTF8Encode(ret);
            }
            else if (fnc.equalsIgnoreCase("MD5")) {
                ret = JDUtilities.getMD5(ret);
            }
        }
        return ret;
    }

    private boolean parseError(String string) {
        this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        logger.severe(string);
        return false;
    }

   

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    private class InternalAuthenticator extends Authenticator {
        private String username, password;

        public InternalAuthenticator(String user, String pass) {
            username = user;
            password = pass;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean en) {
        enabled = en;
    }

    @Override
    public void run() {
    // Nichts zu tun. Interaction braucht keinen Thread
    }

    @Override
    public void initConfig() {

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, "Login User (->%%%user%%%)"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, "Login Passwort (->%%%pass%%%)"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, "RouterIP (->%%%routerip%%%)"));
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, "Wartezeit bis zum ersten IP-Check[sek]", 0, 600).setDefaultValue(0));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, "Max. Wiederholungen (-1 = unendlich)", -1, 20).setDefaultValue(0));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, "Auf neue IP warten [sek]", 0, 600).setDefaultValue(10));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, "HTTP Script"));
        logger.info("init config " + getConfig());

    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }

}
