//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.controlling.interaction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.parser.SimpleMatches;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jd.parser.Regex;

import sun.misc.BASE64Encoder;

/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 * 
 */
public class HTTPLiveHeader extends Interaction {


    /**
	 * 
	 */
	private static final long serialVersionUID = 5388179522151088255L;

	/**
     * serialVersionUID
     */
    private static final String      NAME        = JDLocale.L("interaction.liveHeader.name","HTTP Live Header");

   // private static final String      SEPARATOR   = System.getProperty("line.separator");

    /**
     * Maximal 10 versuche
     */
   // private static final int         MAX_RETRIES = 10;

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

    @SuppressWarnings("unchecked")
	public Vector<String[]> getLHScripts() {
        File[] list = new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"),"router").listFiles();
        Vector<String[]> ret = new Vector<String[]>();
        for (int i = 0; i < list.length; i++) {
            if(list[i].isFile() && list[i].getName().toLowerCase().matches(".*\\.xml$"))
            ret.addAll((Collection<? extends String[]>) JDUtilities.loadObject(new JFrame(), list[i], true));
        }

        return ret;
    }

    @Override
    public boolean doInteraction(Object arg) {
   
        // Hole die Config parameter. Über die Parameterkeys wird in der
        // initConfig auch der ConfigContainer für die Gui vorbereitet
        Configuration configuration = JDUtilities.getConfiguration();

        String script = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);

        String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        String ip = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP);
        retries++;
        logger.info("Starting  #" + retries);
        ProgressController progress = new ProgressController(JDLocale.L("interaction.liveHeader.progress.0_title","HTTPLiveHeader Reconnect"),10);
        progress.setStatusText(JDLocale.L("interaction.liveHeader.progress.1_retry","HTTPLiveHeader #" + retries));
        if (user != null || pass != null) Authenticator.setDefault(new InternalAuthenticator(user, pass));

        if (script == null) {
            progress.finalize();
            return parseError("Kein RequestText gesetzt");
        }
        String preIp = JDUtilities.getIPAddress();

        logger.finer("IP befor: " + preIp);
        progress.setStatusText(JDLocale.L("interaction.liveHeader.progress.2_ip","(IP)HTTPLiveHeader :") + preIp);
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
        variables.put("basicauth", new BASE64Encoder().encode((user + ":" + pass).getBytes()));

        variables.put("routerip", ip);
        headerProperties = new HashMap<String, String>();
        progress.increase(1);
        try {
            xmlScript = parseXmlString(script, false);
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                progress.finalize();
                return parseError("Root Node must be [[[HSRC]]]*[/HSRC]");
            }
            RequestInfo requestInfo = null;
            NodeList steps = root.getChildNodes();
            progress.addToMax(steps.getLength());
            for (int step = 0; step < steps.getLength(); step++) {
                progress.setStatusText(JDLocale.L("interaction.liveHeader.progress.3_step","(STEP)HTTPLiveHeader :") + step);
                progress.increase(1);
                Node current = steps.item(step);
               // short type = current.getNodeType();

                if (current.getNodeType() == 3) {
                    // logger.finer("Skipped: " + current.getNodeName());
                    continue;
                }
                if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                    progress.finalize();
                    return parseError("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                }
                NodeList toDos = current.getChildNodes();
                for (int toDoStep = 0; toDoStep < toDos.getLength(); toDoStep++) {
                    Node toDo = toDos.item(toDoStep);
                  
                        progress.setStatusText(String.format(JDLocale.L("interaction.liveHeader.progress.4_step","(%s)HTTPLiveHeader"), toDo.getNodeName()));
                
                    if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {

                        NamedNodeMap attributes = toDo.getAttributes();
                        for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                            String key = attributes.item(attribute).getNodeName();
                            String value = attributes.item(attribute).getNodeValue();
                            String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%");
                            String[] params = new Regex(value, "%%%(.*?)%%%").getMatches(1);
                            if (params.length > 0) {
                                String req;
                                if (value.startsWith(params[0])) {
                                    req = "";
                                    logger.finer("Variables: " + this.variables);
                                    logger.finer("Headerproperties: " + this.headerProperties);
                                    for (int i = 0; i <= tmp.length; i++) {
                                        logger.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req += getModifiedVariable(params[i - 1]);
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
                                        if (i > params.length) continue;
                                        logger.finer("Replace variable: *********(" + params[i - 1] + ")");

                                        req += getModifiedVariable(params[i - 1]);
                                        if (i < tmp.length) {
                                            req += tmp[i];
                                        }
                                    }
                                }

                                value = req;
                            }

                            variables.put(key, value);
                        }

                        logger.finer("Variables set: " + variables);
                    }
                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.finalize();
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
                            progress.finalize();
                            return parseError("A RESPONSE Tag is not allowed to have childTags.");
                        }

                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {
                            progress.finalize();
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
            progress.finalize();
            return this.parseError(e.getMessage());
        }

        catch (ParserConfigurationException e) {

            e.printStackTrace();
            progress.finalize();
            return this.parseError(e.getMessage());
        }
        catch (Exception e) {

            e.printStackTrace();
            progress.finalize();
            return this.parseError(e.getCause() + " : " + e.getMessage());
        }

        int waittime = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, 0);
        int maxretries = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
        int waitForIp = configuration.getIntegerProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
        logger.finer("Wait " + waittime + " seconds ...");
        progress.increase(1);
        progress.setStatusText(JDLocale.L("interaction.liveHeader.progress.5_wait","(WAIT)HTTPLiveHeader "));
        try {
            Thread.sleep(waittime * 1000);
        }
        catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress();
        if(!JDUtilities.validateIP(afterIP)){
            logger.warning("IP "+afterIP+" was filtered by mask: "+JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK,"\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));
            JDUtilities.getGUI().displayMiniWarning(String.format(JDLocale.L("interaction.reconnect.ipfiltered.warning.short","Die IP %s wurde als nicht erlaubt identifiziert"),afterIP), null, 20);
            afterIP="offline";
        }
        logger.finer("Ip after: " + afterIP);
        progress.increase(1);
        String pattern;
       
            pattern=JDLocale.L("interaction.liveHeader.progress.5_ipcheck","(IPCHECK)HTTPLiveHeader %s / %s");
            progress.setStatusText(String.format(pattern,  preIp ,afterIP));
       


        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        while (System.currentTimeMillis() <= endTime && (afterIP.equalsIgnoreCase("offline")||afterIP == null || afterIP.equals(preIp))) {
            try {
                Thread.sleep(5 * 1000);
            }
            catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress();
            try {
                pattern=JDLocale.L("interaction.liveHeader.progress.5_ipcheck","(IPCHECK)HTTPLiveHeader %s / %s");
                progress.setStatusText(String.format(pattern,  preIp ,afterIP));
            } catch (Exception e) {
                // TODO: handle exception
            }
 
            logger.finer("Ip Check: " + afterIP);
        }
        if (!afterIP.equals(preIp)&&!afterIP.equalsIgnoreCase("offline")) {
            progress.finalize();
            logger.info("Rec succ: "+afterIP);
            return true;
        }
        if (retries <= maxretries) {
            progress.finalize();
            return doInteraction(arg);
        }
        progress.finalize();
        logger.info("Rec fail: "+afterIP);
        return false;
    }

    public static void getDatabase() {
        Vector<String[]> db = new Vector<String[]>();
        String[] cScript = null;
        try {

            RequestInfo requestInfo = HTTP.getRequest(new URL("http://reconnect.thau-ex.de/"));
//            ArrayList<ArrayList<String>> cats = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), "<a href=?cat_select=°>");
            String[] cats = requestInfo.getRegexp("<a href=\\?cat_select=(.*?)>").getMatches(1);
            for (int i = 0; i < cats.length; i++) {
                requestInfo = HTTP.getRequest(new URL("http://reconnect.thau-ex.de/?cat_select=" + cats[i]));
//                ArrayList<ArrayList<String>> router = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), "<a class=\"link\" href=?cat_select=°&show=°>°</a>");
                String[][] router = requestInfo.getRegexp("<a class=\"link\" href=\\?cat_select=(.*?)\\&show=(.*?)>(.*?)</a>").getMatches();
                for (int t = 0; t < router.length; t++) {
                    String endURL = "http://reconnect.thau-ex.de/?cat_select=" + router[t][0] + "&show=" + router[t][1];
                    requestInfo = HTTP.getRequest(new URL(endURL));
                    // s logger.info(requestInfo.getHtmlCode() + "");

                    String code = requestInfo.getRegexp("<textarea name=\"ReconnectCode\" (.*?)>.*?</textarea").getFirstMatch();
                    
                    String script = getScriptFromCURL(code,JDUtilities.htmlDecode(router[t][2]));
                    if (script == null) {

                        cScript = new String[] { router[t][0], router[t][2], code };
                    }
                    else {
                        cScript = new String[] { router[t][0], router[t][2], script };
                    }
                    db.add(cScript);

                }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = db.size() - 1; i >= 0; i--) {
            if (ch.containsKey(db.get(i)[0] + db.get(i)[1] + db.get(i)[2])) {
                db.remove(i);
            }
            else {

                ch.put(db.get(i)[0] + db.get(i)[1] + db.get(i)[2], true);
            }
        }
        ch.clear();
        JDUtilities.saveObject(new JFrame(), db, JDUtilities.getResourceFile("lhdb.xml"), "lhdb", ".xml", true);
    }
    public static String[] getParameter(String code) {
        logger.info("st " + code);
        //boolean qOpen = false;
        Vector<String> ret = new Vector<String>();
        int c = 0;
        int last = 0;
        int url = -1;
        while (true) {

            int l = code.indexOf(" ", c);
            int s = l;

            if (s == -1) s = code.length();
            String param = " " + code.substring(last, s).trim() + " ";
            // logger.info(param);
            if ((param.split("\"").length + 1) % 2 != 0) {
                // logger.info("ERROR " + param.split("\"").length);
                c = s + 1;
                if (s == code.length()) break;
                continue;
            }
            param = param.trim();
            if (param.startsWith("\"")) param = param.substring(1, param.length());
            if (param.endsWith("\"")) param = param.substring(0, param.length() - 1);
            ret.add(param);
            if (url < 0) {
                try {
                    new URL(param);
                    url = ret.size();
                }
                catch (Exception e) {

                }
            }
            c = s + 1;
            last = c;
            if (s == code.length()) break;

        }
        logger.info("" + ret);
        if (url != 2 && url > 0) {
            String u = ret.remove(url - 1);
            ret.add(1, u);

        }
        return ret.toArray(new String[] {});

    }

    public static String getScriptFromCURL(String code, String name) {
        String SEPARATOR="\r\n";
        String ret = "[[[HSRC]]]" + SEPARATOR + "";
        try {
            ret += "    [[[STEP]]]" + SEPARATOR + "";
            ret += "        [[[DEFINE routername=\""+name+"\"/]]]" + SEPARATOR + "";
            ret += "    [[[/STEP]]]" + SEPARATOR;
            String[] lines = Regex.getLines(code);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().toLowerCase().startsWith("curl")) {
                    try {
                        String[] params = getParameter(lines[i]);
                        if (params.length < 2) continue;
                        String url = params[1];
                        logger.info(lines[i] + " : " + url);
//                        String host = new URL(url).getHost();
                        String path = new URL(url).getFile();

                        // String[] login=new URL(url).getUserInfo().split(":");

                        ret += "    [[[STEP]]]" + SEPARATOR + "";
                        ret += "        [[[REQUEST]]]" + SEPARATOR + "";
                        if (lines[i].indexOf("-d ") >= 0) {
                            ret += "            POST " + path + " HTTP/1.1" + SEPARATOR + "";
                            ret += "            Host: " + "%%%routerip%%%" + "" + SEPARATOR + "";

                        }
                        else {
                            ret += "            GET " + path + " HTTP/1.1" + SEPARATOR + "";
                            ret += "            Host: " + "%%%routerip%%%" + "" + SEPARATOR + "";
                        }
                        for (int t = 2; t < params.length; t++) {
                            if (params[t].equalsIgnoreCase("-H")) {
                                t++;
                                ret += "            "+params[t] + SEPARATOR;
                            }
                            else if (params[t].equalsIgnoreCase("-d")) {
                                t++;
                                ret += SEPARATOR +  "            "+params[t];
                            }
                            else if (params[t].equalsIgnoreCase("-u")) {
                                t++;
                                ret +=  "            "+"Authorization: Basic %%%basicauth%%%" + SEPARATOR;
                            }

                            else if (params[t].equalsIgnoreCase("-b")) {
                                t++;
                                ret +=  "            "+"Cookie: %%%Set-Cookie%%%" + SEPARATOR;
                            }
                            else if (params[t].equalsIgnoreCase("-e") || params[t].equalsIgnoreCase("--referer")) {
                                t++;
                                ret +=  "            "+"Referer: " + params[t] + SEPARATOR;
                            }
                            else {
                                logger.info("Unknown flag: " + params[t] + " - ");
                            }

                        }
                        ret += SEPARATOR + "        [[[/REQUEST]]]" + SEPARATOR;
                        ret += "    [[[/STEP]]]" + SEPARATOR;

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;

        }
        ret += "[[[/HSRC]]]" + SEPARATOR;
        return ret;
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
        ArrayList<String> params = SimpleMatches.getAllSimpleMatches(request, "%%%°%%%", 1);
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
                    if (i > params.size()) continue;
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
                HTTPConnection httpConnection = new HTTPConnection(new URL("http://" + host + path).openConnection());
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
                requestInfo = HTTP.readFromURL(httpConnection);
                requestInfo.setConnection(httpConnection);

            }
            else {
                post = post.trim();
                logger.finer("POST " + "http://" + host + path + " " + post);
                HTTPConnection httpConnection = new HTTPConnection(new URL("http://" + host + path).openConnection());
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

                httpConnection.setRequestProperty("Content-Length", post.length() + "");
                httpConnection.setDoOutput(true);
                httpConnection.connect();
                OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
                if (post != null) wr.write(post);
                wr.flush();
                requestInfo = HTTP.readFromURL(httpConnection);
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
            e.printStackTrace();
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
            else if (fnc.equalsIgnoreCase("BASE64")) {
                ret = new BASE64Encoder().encode(ret.getBytes());
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



    @Override
    public void run() {
    // Nichts zu tun. Interaction braucht keinen Thread
    }

    @Override
    public void initConfig() {



    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }

}
