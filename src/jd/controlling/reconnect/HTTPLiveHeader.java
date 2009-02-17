//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.controlling.reconnect;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.JDProxy;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.CLRLoader;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sun.misc.BASE64Encoder;

/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 */
public class HTTPLiveHeader extends ReconnectMethod {

    private HashMap<String, String> headerProperties;

    private HashMap<String, String> variables;

    public HTTPLiveHeader() {
        configuration = JDUtilities.getConfiguration();
    }

    @Override
    protected boolean runCommands(ProgressController progress) {
        String script;
        if (configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, 0) == 3) {
            /* konvertiert CLR zu Liveheader */
            String[] ret = CLRLoader.createLiveHeader(configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR));
            if (ret != null) {
                script = ret[1];
            } else {
                script = null;
            }
        } else {
            script = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
        }
        String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        String ip = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP);

        if (script == null || script.length() == 0) {
            progress.finalize();
            logger.severe("No LiveHeader Script found");
            return false;
        }

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST>", "<REQUEST><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        Document xmlScript;
        variables = new HashMap<String, String>();
        variables.put("user", user);
        variables.put("pass", pass);
        variables.put("basicauth", new BASE64Encoder().encode((user + ":" + pass).getBytes()));
        variables.put("routerip", ip);
        headerProperties = new HashMap<String, String>();

        Browser br = new Browser();

        br.setProxy(JDProxy.NO_PROXY);
        if (user != null && pass != null) {
            br.setAuth(ip, user, pass);
        }
        try {
            xmlScript = HTTPLiveHeader.parseXmlString(script, false);
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                progress.finalize();
                logger.severe("Root Node must be [[[HSRC]]]*[/HSRC]");
                return false;
            }

            NodeList steps = root.getChildNodes();
            progress.addToMax(steps.getLength());
            for (int step = 0; step < steps.getLength(); step++) {
                progress.setStatusText(JDLocale.L("interaction.liveHeader.progress.3_step", "(STEP)HTTPLiveHeader :") + step);
                progress.increase(1);
                Node current = steps.item(step);

                if (current.getNodeType() == 3) continue;

                if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                    progress.finalize();
                    logger.severe("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                    return false;
                }
                NodeList toDos = current.getChildNodes();
                for (int toDoStep = 0; toDoStep < toDos.getLength(); toDoStep++) {
                    Node toDo = toDos.item(toDoStep);

                    progress.setStatusText(String.format(JDLocale.L("interaction.liveHeader.progress.4_step", "(%s)HTTPLiveHeader"), toDo.getNodeName()));

                    if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {

                        NamedNodeMap attributes = toDo.getAttributes();
                        for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                            String key = attributes.item(attribute).getNodeName();
                            String value = attributes.item(attribute).getNodeValue();
                            String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%");
                            String[] params = new Regex(value, "%%%(.*?)%%%").getColumn(-1);
                            if (params.length > 0) {
                                StringBuilder req;
                                if (value.startsWith(params[0])) {
                                    req = new StringBuilder();
                                    logger.finer("Variables: " + variables);
                                    logger.finer("Headerproperties: " + headerProperties);
                                    for (int i = 0; i <= tmp.length; i++) {
                                        logger.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmp.length) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                } else {
                                    req = new StringBuilder(tmp[0]);
                                    logger.finer("Variables: " + variables);
                                    logger.finer("Headerproperties: " + headerProperties);
                                    for (int i = 1; i <= tmp.length; i++) {
                                        if (i > params.length) {
                                            continue;
                                        }
                                        logger.finer("Replace variable: *********(" + params[i - 1] + ")");
                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmp.length) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                }

                                value = req.toString();
                            }

                            variables.put(key, value);
                        }

                        logger.finer("Variables set: " + variables);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
                        String[] parseLines = HTTPLiveHeader.splitLines(toDo.getChildNodes().item(0).getNodeValue().trim());
                        for (String parseLine : parseLines) {
                            String varname = new Regex(parseLine, "(.*?):").getMatch(0);
                            String pattern = new Regex(parseLine, ".*?:(.+)").getMatch(0);
                            if (varname != null && pattern != null) {
                                varname = varname.trim();
                                pattern = pattern.trim();
                                String found = br.getRegex(pattern).getMatch(0);
                                if (found != null) {
                                    found = found.trim();
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    variables.put(varname, found);
                                } else {
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                }
                            }
                        }
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        boolean ishttps = false;
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.finalize();
                            logger.severe("A REQUEST Tag is not allowed to have childTags.");
                            return false;
                        }
                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("https") != null) {
                            ishttps = true;
                        }
                        Browser retbr = null;
                        try {
                            retbr = doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), br, ishttps);
                        } catch (Exception e2) {
                            retbr = null;
                        }
                        try {
                            /* DDoS Schutz */
                            Thread.sleep(150);
                        } catch (Exception e) {
                        }
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            logger.severe("Request error!");
                        } else {
                            br = retbr;
                        }

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
                        logger.finer("get Response");
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.finalize();
                            logger.severe("A RESPONSE Tag is not allowed to have childTags.");
                            return false;
                        }

                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {
                            progress.finalize();
                            logger.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                            return false;
                        }

                        String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                        getVariables(toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {
                        NamedNodeMap attributes = toDo.getAttributes();
                        Node item = attributes.getNamedItem("seconds");
                        logger.finer("Wait " + item.getNodeValue() + " seconds");
                        if (item == null) {
                            logger.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                            return false;
                        }
                        int seconds = JDUtilities.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);
                    }
                }
            }

        } catch (SAXException e) {
            progress.finalize();
            logger.severe(e.getMessage());
            return false;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            progress.finalize();
            logger.severe(e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            progress.finalize();
            logger.severe(e.getCause() + " : " + e.getMessage());
            return false;
        }

        return true;
    }

    private Browser doRequest(String request, Browser br, boolean ishttps) {
        try {
            String requestType;
            String path;
            StringBuilder post = new StringBuilder();
            String host = null;
            String http = "http://";
            if (ishttps) http = "https://";

            HashMap<String, String> requestProperties = new HashMap<String, String>();
            br.setHeaders(new HashMap<String, String>());
            String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
            // ArrayList<String> params =
            // SimpleMatches.getAllSimpleMatches(request,
            // "%%%°%%%", 1);
            String[] params = new Regex(request, "%%%(.*?)%%%").getColumn(0);
            if (params.length > 0) {
                StringBuilder req;
                if (request.startsWith(params[0])) {
                    req = new StringBuilder();
                    logger.finer("Variables: " + variables);
                    logger.finer("Headerproperties: " + headerProperties);
                    for (int i = 0; i <= tmp.length; i++) {
                        logger.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmp.length) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    logger.finer("Variables: " + variables);
                    logger.finer("Headerproperties: " + headerProperties);
                    for (int i = 1; i <= tmp.length; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        logger.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmp.length) {
                            req.append(tmp[i]);
                        }
                    }
                }

                request = req.toString();
            }
            String[] requestLines = HTTPLiveHeader.splitLines(request);
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
                    post.append(requestLines[li]);
                    post.append(new char[] { '\r', '\n' });
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

                if (p[0].trim().equalsIgnoreCase("HOST")) {
                    host = requestLines[li].substring(p[0].length() + 1).trim();
                }
            }

            if (host == null) {
                logger.severe("Host nicht gefunden: " + request);
                return null;
            }
            try {
                br.setConnectTimeout(5000);
                br.setReadTimeout(5000);
                if (requestProperties != null) {
                    br.getHeaders().putAll(requestProperties);
                }
                if (requestType.equalsIgnoreCase("GET")) {
                    logger.finer("GET " + "http://" + host + path);
                    br.getPage(http + host + path);
                } else if (requestType.equalsIgnoreCase("POST")) {
                    String poster = post.toString().trim();
                    logger.finer("POST " + "http://" + host + path + " " + poster);
                    br.postPageRaw(http + host + path, poster);
                } else if (requestType.equalsIgnoreCase("AUTH")) {
                    logger.finer("Convert AUTH->GET");
                    br.getPage(http + host + path);
                } else {
                    logger.severe("Unknown requesttyp: " + requestType);
                    return null;
                }
                logger.finer("Answer: ");
                for (Map.Entry<String, List<String>> me : br.getRequest().getResponseHeaders().entrySet()) {
                    if (me.getValue() != null && me.getValue().size() > 0) {
                        headerProperties.put(me.getKey(), me.getValue().get(0));
                        logger.finer(me.getKey() + " : " + me.getValue().get(0));
                    }
                }
                return br;
            } catch (IOException e) {

                logger.severe("IO Error: " + e.getLocalizedMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public Vector<String[]> getLHScripts() {
        File[] list = new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), "router").listFiles();
        Vector<String[]> ret = new Vector<String[]>();
        for (File element : list) {
            if (element.isFile() && element.getName().toLowerCase().matches(".*\\.xml$")) {
                ret.addAll((Collection<? extends String[]>) JDIO.loadObject(new JFrame(), element, true));
            }
        }

        return ret;
    }

    private static Document parseXmlString(String xmlString, boolean validating) throws SAXException, IOException, ParserConfigurationException {
        // Create a builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        InputSource inSource = new InputSource(new StringReader(xmlString));

        // Create the builder and parse the file
        Document doc = factory.newDocumentBuilder().parse(inSource);

        return doc;
    }

    private static String[] splitLines(String source) {
        return source.split("\r\n|\r|\n");
    }

    private String getModifiedVariable(String key) {

        if (key.indexOf(":::") == -1 && headerProperties.containsKey(key)) { return headerProperties.get(key); }
        if (key.indexOf(":::") == -1) { return variables.get(key); }
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
                ret = new BASE64Encoder().encode(ret.getBytes());
            }
        }
        return ret;
    }

    private void getVariables(String patStr, String[] keys, Browser br) {
        logger.info("GetVariables");
        if (br == null) return;
        // patStr="<title>(.*?)</title>";
        logger.finer(patStr);
        Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // logger.info(requestInfo.getHtmlCode());
        Matcher matcher = pattern.matcher(br + "");
        logger.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {
            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                variables.put(keys[i], matcher.group(i + 1));
                logger.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }
        } else {
            logger.severe("Regular Expression without matches: " + patStr);
        }

    }

    @Override
    public void initConfig() {
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.liveHeader.name", "HTTP Live Header");
    }

}
