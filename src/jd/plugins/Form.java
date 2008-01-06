package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Form {
    public static final int METHODE_POST = 0;
    public static final int METHODE_GET = 1;
    /**
     * Methode der Form POST = 0 oder GET = 1 PUT fällt weg da es ein
     * Ausnahmefall ist
     */
    public int methode;
    /**
     * Action der Form entspricht auch oft einer URL
     */
    public String action;
    /**
     * Die eigenschaften der Form z.B. id oder name (ohne methode und action)
     * kann zur Identifikation verwendet werden
     */
    public HashMap<String, String> formProperties = new HashMap<String, String>();
    /**
     * Value und name von Inputs/Textareas/Selectoren
     * HashMap<name, value>
     * Achtung müssen zum teil noch ausgefüllt werden
     */
    public HashMap<String, String> vars = new HashMap<String, String>();
    /**
     * Wird bei der Benutzung von getInputFields automatisch gesetzt
     */
    private RequestInfo baseRequest;
    private static String[] getNameValue(String data) {
        Matcher matcher = Pattern.compile("name=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(data);
        String key, value;
        key = value = null;
        if (matcher.find()) {
            key = matcher.group(1);
        } else {
            matcher = Pattern.compile("name=(.*)", Pattern.CASE_INSENSITIVE).matcher(data + " ");
            if (matcher.find())
                key = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
        }
        if (key == null)
            return null;
        matcher = Pattern.compile("value=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(data);


        if (matcher.find())
            value = matcher.group(1);
        else
        {        
            matcher = Pattern.compile("value=(.*)", Pattern.CASE_INSENSITIVE).matcher(data + " ");
            if (matcher.find())
            value = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
        }
        if (value != null && value.matches("[\\s]*"))
            value = null;
        return new String[]{key, value};

    }
    /**
     * Gibt alle Input fields zurück Object[0]=vars Object[1]=varsWithoutValue
     */
    private static HashMap<String, String> getInputFields(String data) {
        HashMap<String, String> ret = new HashMap<String, String>();
        Matcher matcher = Pattern.compile("(?s)<[\\s]*(input|textarea|select)(.*?)>", Pattern.CASE_INSENSITIVE).matcher(data);
        while (matcher.find()) {
            String[] nv = getNameValue(matcher.group(2));
            if(nv!=null)
            {
                if(!ret.containsKey(nv[0]))
                {
                    ret.put(nv[0], ((nv[1]==null)?"":nv[1]));
                }
                else if(ret.get(nv[0]).equals(""))
                    ret.put(nv[0], ((nv[1]==null)?"":nv[1]));

            }
        }

        return ret;
    }
    /**
     * Ein Array mit allen Forms einer Seite
     */
    public static Form[] getForms(RequestInfo requestInfo) {
        return getForms(requestInfo, ".*");
    }
    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. Dafür gibt es die formProperties
     */
    public static Form[] getForms(RequestInfo requestInfo, String matcher) {
        ArrayList<Form> forms = new ArrayList<Form>();
        Pattern pattern = Pattern.compile("<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formmatcher = pattern.matcher(requestInfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", ""));

        while (formmatcher.find()) {
            String formPropertie = formmatcher.group(1);
            String inForm = formmatcher.group(2);
            // System.out.println(inForm);
            if (inForm.matches("(?s)" + matcher)) {
                Form form = new Form();
                form.baseRequest = requestInfo;
                form.methode = 1;
                Pattern patternfp = Pattern.compile(" ([^\\s]+)\\=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE);
                Matcher matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action"))
                        form.action = matcherfp.group(2);
                    else if (pname.toLowerCase().equals("method")) {
                        if (matcherfp.group(2).toLowerCase().matches(".*post.*"))
                            form.methode = 0;
                    } else
                        form.formProperties.put(pname, matcherfp.group(2));
                }
                patternfp = Pattern.compile(" ([^\\s]+)\\=[^\"'](.*?)[\\s>]", Pattern.CASE_INSENSITIVE);
                matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action"))
                        form.action = matcherfp.group(2);
                    else if (pname.toLowerCase().equals("method")) {
                        if (matcherfp.group(2).toLowerCase().matches(".*post.*"))
                            form.methode = 0;
                    } else
                        form.formProperties.put(pname, matcherfp.group(2));
                }
                if (form.action == null)
                    form.action = requestInfo.getConnection().getURL().toString();
                form.vars.putAll(getInputFields(inForm));
                forms.add(form);
            }

        }
        return forms.toArray(new Form[forms.size()]);
    }
    /**
     * Erzeugt aus der Form eine RequestInfo
     */
    public RequestInfo getRequestInfo() {
        return getRequestInfo(true);
    }
    @SuppressWarnings("deprecation")
    public RequestInfo getRequestInfo(boolean redirect) {
        if (baseRequest == null)
            return null;
        URL baseurl = baseRequest.getConnection().getURL();
        if (action == null || action.matches("[\\s]*")) {
            if (baseurl == null)
                return null;
            action = baseurl.toString();
        } else if (!action.matches("http://.*")) {
            if (baseurl == null)
                return null;
            if (action.charAt(0) == '/')
                action = "http://" + baseurl.getHost() + action;
            else {
                String base = baseurl.toString();
                if (base.matches("http://.*/"))
                    action = base.substring(0, base.lastIndexOf("/")) + "/" + action;
                else
                    action = base + "/" + action;
            }
        }
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (first)
                first = false;
            else
                buffer.append("&");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(URLEncoder.encode(entry.getValue()));
        }
        String varString = buffer.toString();
        if (methode == METHODE_GET) {
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches("\\?.+"))
                    action += "&";
                else if (action.matches("[^\\?]*"))
                    action += "?";
                action += varString;
            }
            try {
                return Plugin.getRequest(new URL(action), baseRequest.getCookie(), baseurl.toString(), redirect);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                return Plugin.postRequest(new URL(action), baseRequest.getCookie(), baseurl.toString(), null, varString, redirect);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return null;

    }
    public String toString() {
        String ret = "";
        ret += "Action: " + action + "\n";
        if (methode == METHODE_POST)
            ret += "Type: POST\n";
        else
            ret += "Type: GET\n";
        
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            ret += "var: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }
        for (Map.Entry<String, String> entry : formProperties.entrySet()) {
            ret += "formPropertie: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }
        return ret;

    }
}
