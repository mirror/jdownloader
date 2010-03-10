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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.http.RequestHeader;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.CLRLoader;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 */
public class HTTPLiveHeader extends ReconnectMethod {

    private static final String JDL_PREFIX = "jd.controlling.reconnect.HTTPLiveHeader.";

    private final Configuration configuration;

    private HashMap<String, String> headerProperties;

    private HashMap<String, String> variables;

    public HTTPLiveHeader() {
        configuration = JDUtilities.getConfiguration();
    }

    /*
     * DO NOT REMOVE THIS OR REPLACE BY Regex.getLines()
     * 
     * REGEX ARE COMPLETE DIFFERENT AND DO NOT TRIM
     */
    private static String[] splitLines(final String source) {
        return source.split("\r\n|\r|\n");
    }

    // @Override
    protected boolean runCommands(final ProgressController progress) {
        String script;
        if (configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.CLR) {
            /* konvertiert CLR zu Liveheader */
            final String[] ret = CLRLoader.createLiveHeader(configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR));
            if (ret != null) {
                script = ret[1];
            } else {
                script = null;
            }
        } else {
            script = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
        }
        final String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        final String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        final String ip = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP);

        if (script == null || script.length() == 0) {
            progress.doFinalize();
            LOG.severe("No LiveHeader Script found");
            return false;
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
        variables = new HashMap<String, String>();
        variables.put("user", user);
        variables.put("pass", pass);
        variables.put("basicauth", Encoding.Base64Encode(user + ":" + pass));
        variables.put("routerip", ip);
        headerProperties = new HashMap<String, String>();

        Browser br = new Browser();
        /* set custom timeouts here because 10secs is a LONG time ;) */
        br.setReadTimeout(10000);
        br.setConnectTimeout(10000);
        br.setProxy(JDProxy.NO_PROXY);
        if (user != null && pass != null) {
            br.setAuth(ip, user, pass);
        }
        try {
            xmlScript = JDUtilities.parseXmlString(script, false);
            if (xmlScript == null) {
                progress.doFinalize();
                LOG.severe("Error while parsing the xml string: " + script);
                return false;
            }
            final Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                progress.doFinalize();
                LOG.severe("Root Node must be [[[HSRC]]]*[/HSRC]");
                return false;
            }

            final NodeList steps = root.getChildNodes();
            progress.addToMax(steps.getLength());
            for (int step = 0; step < steps.getLength(); step++) {
                progress.setStatusText(JDL.LF(JDL_PREFIX + "step", "HTTPLiveHeader: Step %s", step));
                progress.increase(1);
                final Node current = steps.item(step);

                if (current.getNodeType() == 3) continue;

                if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                    progress.doFinalize();
                    LOG.severe("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                    return false;
                }
                final NodeList toDos = current.getChildNodes();
                final int toDosLength = toDos.getLength();
                for (int toDoStep = 0; toDoStep < toDosLength; toDoStep++) {
                    final Node toDo = toDos.item(toDoStep);

                    progress.setStatusText(JDL.LF(JDL_PREFIX + "step", "HTTPLiveHeader: Step %s", toDo.getNodeName()));

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
                                    LOG.finer("Variables: " + variables);
                                    LOG.finer("Headerproperties: " + headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 0; i <= tmpLength; i++) {
                                        LOG.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmpLength) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                } else {
                                    req = new StringBuilder(tmp[0]);
                                    LOG.finer("Variables: " + variables);
                                    LOG.finer("Headerproperties: " + headerProperties);
                                    final int tmpLength = tmp.length;
                                    for (int i = 1; i <= tmpLength; i++) {
                                        if (i > params.length) {
                                            continue;
                                        }
                                        LOG.finer("Replace variable: *********(" + params[i - 1] + ")");
                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmpLength) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                }

                                value = req.toString();
                            }

                            variables.put(key, value);
                        }

                        LOG.finer("Variables set: " + variables);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
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
                                    LOG.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    variables.put(varname, found);
                                } else {
                                    LOG.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                }
                            }
                        }
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        boolean ishttps = false;
                        boolean israw = false;
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.doFinalize();
                            LOG.severe("A REQUEST Tag is not allowed to have childTags.");
                            return false;
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
                            retbr = doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), br, ishttps, israw);
                        } catch (Exception e2) {
                            retbr = null;
                        }
                        try {
                            /* DDoS Schutz */
                            Thread.sleep(350);
                        } catch (Exception e) {
                        }
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            LOG.severe("Request error!");
                        } else {
                            br = retbr;
                        }

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
                        LOG.finer("get Response");
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.doFinalize();
                            LOG.severe("A RESPONSE Tag is not allowed to have childTags.");
                            return false;
                        }

                        final NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {
                            progress.doFinalize();
                            LOG.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                            return false;
                        }

                        final String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                        getVariables(toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {
                        final NamedNodeMap attributes = toDo.getAttributes();
                        final Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            LOG.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                            return false;
                        }
                        LOG.finer("Wait " + item.getNodeValue() + " seconds");
                        final int seconds = Formatter.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);
                    }
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            progress.doFinalize();
            LOG.severe(e.getCause() + " : " + e.getMessage());
            return false;
        }

        return true;
    }

    private Browser doRequest(String request, final Browser br, final boolean ishttps, final boolean israw) {
        try {
            final String requestType;
            final String path;
            final StringBuilder post = new StringBuilder();
            String host = null;
            final String http = (ishttps) ? "https://" : "http://";
            if (LOG.isLoggable(Level.FINEST)) {
                br.forceDebug(true);
            } else {
                br.forceDebug(false);
            }
            final HashMap<String, String> requestProperties = new HashMap<String, String>();
            if (israw) {
                br.setHeaders(new RequestHeader());
            }
            String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
            // ArrayList<String> params =
            // SimpleMatches.getAllSimpleMatches(request,
            // "%%%°%%%", 1);
            final String[] params = new Regex(request, "%%%(.*?)%%%").getColumn(0);
            if (params.length > 0) {
                final StringBuilder req;
                if (request.startsWith(params[0])) {
                    req = new StringBuilder();
                    LOG.finer("Variables: " + variables);
                    LOG.finer("Headerproperties: " + headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 0; i <= tmpLength; i++) {
                        LOG.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    LOG.finer("Variables: " + variables);
                    LOG.finer("Headerproperties: " + headerProperties);
                    final int tmpLength = tmp.length;
                    for (int i = 1; i <= tmpLength; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        LOG.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmpLength) {
                            req.append(tmp[i]);
                        }
                    }
                }
                request = req.toString();
            }
            final String[] requestLines = splitLines(request);
            if (requestLines.length == 0) {
                LOG.severe("Parse Fehler:" + request);
                return null;
            }
            // RequestType
            tmp = requestLines[0].split(" ");
            if (tmp.length < 2) {
                LOG.severe("Konnte Requesttyp nicht finden: " + requestLines[0]);
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
                String[] p = requestLines[li].split("\\:");
                if (p.length < 2) {
                    LOG.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
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
                LOG.severe("Host nicht gefunden: " + request);
                return null;
            }
            try {
                br.setConnectTimeout(5000);
                br.setReadTimeout(5000);
                if (requestProperties != null) {
                    br.getHeaders().putAll((Map<String, String>) requestProperties);
                }
                if (requestType.equalsIgnoreCase("GET")) {
                    br.getPage(http + host + path);
                } else if (requestType.equalsIgnoreCase("POST")) {
                    final String poster = post.toString().trim();
                    br.postPageRaw(http + host + path, poster);
                } else if (requestType.equalsIgnoreCase("AUTH")) {
                    LOG.finer("Convert AUTH->GET");
                    br.getPage(http + host + path);
                } else {
                    LOG.severe("Unknown requesttyp: " + requestType);
                    return null;
                }
                return br;
            } catch (IOException e) {
                LOG.severe("IO Error: " + e.getLocalizedMessage());
                JDLogger.exception(e);
                return null;
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String[]> getLHScripts() {
        final File[] list = new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), "router").listFiles();
        final ArrayList<String[]> ret = new ArrayList<String[]>();
        for (final File element : list) {
            if (element.isFile() && element.getName().toLowerCase().matches(".*\\.xml$")) {
                ret.addAll((Collection<? extends String[]>) JDIO.loadObject(new JFrame(), element, true));
            }
        }

        return ret;
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
                ret = Encoding.Base64Encode(ret);
            }
        }
        return ret;
    }

    private void getVariables(final String patStr, final String[] keys, final Browser br) {
        LOG.info("GetVariables");
        if (br == null) return;
        // patStr="<title>(.*?)</title>";
        LOG.finer(patStr);
        Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // logger.info(requestInfo.getHtmlCode());
        final Matcher matcher = pattern.matcher(br + "");
        LOG.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {
            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                variables.put(keys[i], matcher.group(i + 1));
                LOG.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }
        } else {
            LOG.severe("Regular Expression without matches: " + patStr);
        }

    }

    protected void initConfig() {
    }

}
