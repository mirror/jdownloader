package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 * 
 */
public class HTTPSendReconnect extends Interaction {
    private transient static boolean enabled     = true;

    /**
     * serialVersionUID
     */
    private static final String      NAME        = "HTTP Sender (cURL)";

    /**
     * Maximal 10 versuche
     */
    private static final int         MAX_RETRIES = 10;

    private int

                                     retries     = 0;

    public HTTPSendReconnect() {

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
        String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        String requests = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
        if (requests == null) {

            return parseError("Kein RequestText gesetzt");
        }
       
        if (user != null && pass != null) Authenticator.setDefault(new InternalAuthenticator(user, pass));
        
        Vector<String> requestList = Plugin.getAllSimpleMatches(requests, "###REQUESTSTART###°###REQUESTEND###", 1);
   
        retries++;
        logger.info("Starting HTTP Sender t #" + retries);
        String ipBefore;
        String ipAfter;
        // IP auslesen
        ipBefore = getIPAddress();
        logger.fine("IP before:" + ipBefore);
        String[] requestString;
        String request;
        String response = null;
        String[] tmp;

        HashMap<String, String> fields = new HashMap<String, String>();
        HashMap<String, String> requestProperties = new HashMap<String, String>();
        String requestType;
        String path;
        String host = null;
        String post = "";
        RequestInfo requestInfo;
        try {
            for (int i = 0; i < requestList.size(); i++) {

                requestString = requestList.get(i).split("\\#\\#\\#ANSWER\\#\\#\\#");
                request = requestString[0];
                if (requestString.length > 1) response = requestString[1];
                Vector<String> params = Plugin.getAllSimpleMatches(request, "###PARAM:°###", 1);
                // Ersetzt die parameter im requests mit denen aus der Map
                for (int pi = 0; pi < params.size(); pi++) {
                    logger.info("rep " + "\\#\\#\\#PARAM\\:" + params.get(pi) + "\\#\\#\\#" + " mit " + (fields.get(params.get(pi)) == null ? "null" : fields.get(params.get(pi)) + "(" + params.get(pi)));
                    request = request.replaceAll("\\#\\#\\#PARAM\\:" + params.get(pi) + "\\#\\#\\#", fields.get(params.get(pi)) == null ? "null" : fields.get(params.get(pi)));
                    if (fields.get(params.get(pi)) == null) {
                        logger.warning("ACHTUNG: Parameter " + params.get(pi) + " Konnte nicht ermittelt werden");
                    }

                }

                String[] requestLines = request.split("\r\n|\r|\n");
                if (requestLines.length == 0) {
                    return parseError(request);
                }
                // RequestType
                tmp = requestLines[0].split(" ");
                if (tmp.length < 2) return parseError("Konnte Requesttyp nicht finden: " + requestLines[0]);
                requestType = tmp[0];
                path = tmp[1];
                logger.finer("RequestType: " + requestType);
                logger.finer("Path: " + path);
                boolean headersEnd = false;
                // Zerlege request

                for (int li = 1; li < requestLines.length; li++) {
                    logger.finer(requestLines[li]);
                    if (headersEnd) {
                        post += requestLines[li] + "\r\n";
                        continue;
                    }
                    if (requestLines[li].trim().length() == 0) {
                        headersEnd = true;
                        logger.finer("Rest ist POST");
                        continue;
                    }
                    String[] p = requestLines[li].split("\\:");
                    if (p.length < 2) return this.parseError("Syntax Fehler in: " + requestLines[li]);
                    requestProperties.put(p[0], requestLines[li].substring(p[0].length() + 1).trim());

                    if (p[0].equalsIgnoreCase("HOST")) host = requestLines[li].substring(p[0].length() + 1).trim();
                }
                if (host == null) return parseError("Host nicht gefunden: " + request);
                if (requestType.equalsIgnoreCase("GET")) {
                    requestInfo = Plugin.getRequest(new URL("http://" + host + path), null, null, false);
                }
                else {
                    requestInfo = Plugin.postRequest(new URL("http://" + host + path), null, null, requestProperties, post, false);
                }
            
                if (requestString.length > 1) {
                    // Antwort auswerten

                    response = requestString[1];
                    String[] regexes = response.split("###NEXT###");
                    for (int rgi = 0; rgi < regexes.length; rgi++) {
                        params = Plugin.getAllSimpleMatches(regexes[rgi], "###PARAM:°###", 1);
                      
                        regexes[rgi] = regexes[rgi].trim().replaceAll("\\#\\#\\#PARAM:(.*?)\\#\\#\\#", "°");

                   
                        if (regexes[rgi].length() > 1) {
                            String[] responseParams = Plugin.getSimpleMatches(requestInfo.getHtmlCode(), regexes[rgi]);
                            if (responseParams != null) {
                                logger.finer("suche nach:" + regexes[rgi] + "treffer: " + responseParams.length);
                                for (int ri = 0; ri < responseParams.length; ri++) {
                                    if (responseParams[ri].trim().length() > 0) {
                                        fields.put(params.get(ri), responseParams[ri]);
                                        logger.finer("GOT Param: " + params.get(ri) + " = " + responseParams[ri]);
                                    }
                                }
                            }
                        }
                        for (int ip = 0; ip < params.size(); ip++) {
                            if (requestInfo.getConnection().getHeaderField(params.get(i)) != null) {
                                fields.put(params.get(ip), requestInfo.getConnection().getHeaderField(params.get(i)));
                                logger.finer("GOT Param(header): " + params.get(ip) + " = " + requestInfo.getConnection().getHeaderField(params.get(i)));
                            }
                        }

                    }

                }

                logger.finer("Parameter: " + fields);

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return parseError("Fehler: " + e.getMessage());
        }
        ipAfter = getIPAddress();
        logger.fine("IP after reconnect:" + ipAfter);

        if (ipBefore == null || ipAfter == null || ipBefore.equals(ipAfter)) {
            logger.severe("IP address did not change");
            if (retries < HTTPSendReconnect.MAX_RETRIES && (retries < configuration.getReconnectRetries() || configuration.getReconnectRetries() <= 0)) {
                return doInteraction(arg);
            }
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            retries = 0;
            return false;
        }
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        retries = 0;
        return true;
    }

    private boolean parseError(String string) {
        this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        logger.severe(string);
        return false;
    }

    private String getIPAddress() {

        try {
            logger.finer("IP CHeck Via http://meineip.de");
            RequestInfo requestInfo = Plugin.getRequest(new URL("http://meineip.de"),null,null,true);
            Pattern pattern = Pattern.compile("\\Q<td><b>\\E([0-9.]*)\\Q</b></td>\\E");
            Matcher matcher = pattern.matcher(requestInfo.getHtmlCode());
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }
        catch (IOException e1) {
            logger.severe("url not found. " + e1.toString());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Interner HTTP Sender";
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    /**
     * @author coalado
     * @param url
     * @return Prüft ob eine URL absolut ist.
     */
    private boolean isAbsolute(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            return uri.isAbsolute();
        }
        catch (URISyntaxException e) {
            return false;
        }
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, "Login User"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, "Login Passwort"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, "HTTP Script"));
        logger.info("init config " + getConfig());

    }

    @Override
    public void resetInteraction() {
        retries = 0;
    }
}
