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

package jd.parser.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Property;
import jd.controlling.JDLogger;
import jd.http.requests.RequestVariable;
import jd.parser.Regex;
import jd.utils.EditDistance;

public class Form extends Property {

    /**
     * 
     */
    private static final long serialVersionUID = 5837247484638868257L;

    public enum MethodType {
        GET, POST, PUT, UNKNOWN
    }

    /**
     * Action der Form entspricht auch oft einer URL
     */
    private String action;

    private ArrayList<InputField> inputfields;

    private String htmlcode = null;

    private MethodType method = MethodType.GET;

    private String encoding;

    private InputField preferredSubmit;

    public Form(String total) {
        this();
        parse(total);
    }

    public Form() {
        this.inputfields = new ArrayList<InputField>();
    }

    private void parse(String total) {
        htmlcode = total;

        // form.baseRequest = requestInfo;
        String header = new Regex(total, "<[\\s]*form(.*?)>").getMatch(0);
        //
        // <[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>|<[\\s]*form(.*?)>(.+)
        String[][] headerEntries = new Regex(header, "[\"' ](\\w+?)[ ]*=[ ]*[\"'](.*?)[\"']").getMatches();
        String[][] headerEntries2 = new Regex(header, "[\"' ](\\w+?)[ ]*=[ ]*([^>^ ^\"^']+)").getMatches();

        parseHeader(headerEntries);
        parseHeader(headerEntries2);

        this.parseInputFields();

        // if (form.action == null) {
        // form.action =
        // requestInfo.getConnection().getURL().toString();
        // }
        // form.vars.add(form.getInputFields(inForm));

    }

    private void parseHeader(String[][] headerEntries) {
        String key;
        String value;
        String lowvalue;
        for (String[] entry : headerEntries) {
            key = entry[0];
            value = entry[1];
            lowvalue = value.toLowerCase();
            if (key.equalsIgnoreCase("action")) {
                setAction(value);
            } else if (key.equalsIgnoreCase("enctype")) {
                this.setEncoding(value);

            } else if (key.equalsIgnoreCase("method")) {

                if (lowvalue.matches(".*post.*")) {
                    setMethod(MethodType.POST);
                } else if (lowvalue.matches(".*get.*")) {
                    setMethod(MethodType.GET);
                } else if (lowvalue.matches(".*put.*")) {
                    setMethod(MethodType.PUT);
                } else {
                    setMethod(MethodType.POST);
                }
            } else {
                this.setProperty(key, value);
            }
        }

    }

    public ArrayList<InputField> getInputFields() {
        return inputfields;
    }

    public void setMethod(MethodType method) {
        this.method = method;

    }

    public MethodType getMethod() {
        return method;
    }

    public String getHtmlCode() {
        return htmlcode;
    }

    public boolean equalsIgnoreCase(Form f) {
        return this.toString().equalsIgnoreCase(f.toString());
    }

    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. Dafür gibt es die formProperties
     */
    public static Form[] getForms(Object requestInfo) {
        LinkedList<Form> forms = new LinkedList<Form>();

        Pattern pattern = Pattern.compile("<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>|<[\\s]*form(.*?)>(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formmatcher = pattern.matcher(requestInfo.toString().replaceAll("(?s)<!--.*?-->", ""));
        while (formmatcher.find()) {
            String total = formmatcher.group(0);
            // System.out.println(inForm);

            Form form = new Form(total);

            forms.add(form);

        }
        return forms.toArray(new Form[forms.size()]);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // public boolean hasSubmitValue(String value) {
    // for (String submit : this.submitValues) {
    // try {
    // if (submit == value || submit.equalsIgnoreCase(value)) return true;
    // } catch (NullPointerException e) {
    // //
    // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
    // "Exception occurred",e);
    // }
    // }
    // return false;
    //
    // }

    public String getAction(String baseURL) {
        URL baseurl = null;
        if (baseURL == null) {
            baseurl = null;
        } else {
            try {
                baseurl = new URL(baseURL);
            } catch (MalformedURLException e) {
                JDLogger.exception(e);
            }
        }
        String ret = action;
        if (action == null || action.matches("[\\s]*")) {
            if (baseurl == null) return null;
            ret = baseurl.toString();
        } else if (!ret.matches("https?://.*")) {
            if (baseurl == null) { return null; }
            if (ret.charAt(0) == '/') {
                if (baseurl.getPort() > 0 && baseurl.getPort() != 80) {
                    ret = "http://" + baseurl.getHost() + ":" + baseurl.getPort() + ret;
                } else {
                    ret = "http://" + baseurl.getHost() + ret;
                }
            } else if (ret.charAt(0) == '&') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else if (ret.charAt(0) == '?') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.replaceFirst("\\?.*", "") + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else if (ret.charAt(0) == '#') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.substring(0, base.lastIndexOf("/")) + "/" + ret;
                } else {
                    ret = base + "/" + ret;
                }
            }
        }
        return ret;
    }

    private void parseInputFields() {
        inputfields = new ArrayList<InputField>();
        Matcher matcher = Pattern.compile("(?s)(<[\\s]*(input|textarea|select).*?>)", Pattern.CASE_INSENSITIVE).matcher(this.htmlcode);
        while (matcher.find()) {
            InputField nv = InputField.parse(matcher.group(1));
            if (nv != null) {
                this.addInputField(nv);

            }
        }

    }

    public void addInputField(InputField nv) {

        inputfields.add(nv);

    }

    public void addInputFieldAt(InputField nv, int i) {

        inputfields.add(i, nv);

    }

    /**
     * Changes the value of the first filed with the key key to value. if no
     * field exists, a new one is created.
     * 
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        InputField ipf = getInputField(key);
        if (ipf != null) {
            ipf.setValue(value);
        } else {
            this.inputfields.add(new InputField(key, value));
        }
    }

    /**
     * Gets the first inputfiled with this key. REMEMBER. There can be more than
     * one file with this key
     * 
     * @param key
     * @return
     */
    public InputField getInputField(String key) {
        for (InputField ipf : this.inputfields) {
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(key)) return ipf;
        }
        return null;
    }

    /**
     * Removes the first inputfiled with this key. REMEMBER. There can be more
     * than one file with this key
     * 
     * @param key
     * @return
     */
    public void remove(String key) {
        for (InputField ipf : this.inputfields) {
            if (ipf.getKey() == null && key == null) {
                inputfields.remove(ipf);
                return;
            }
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(key)) {
                inputfields.remove(ipf);
                return;
            }
        }
    }

    /**
     * Gibt den variablennamen der am besten zu varname passt zurück.
     * 
     * @param varname
     * @return
     */
    public String getBestVariable(String varname) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (InputField ipf : this.inputfields) {
            int dist = EditDistance.getLevenshteinDistance(varname, ipf.getKey());
            if (dist < bestDist) {
                best = ipf.getKey();
                bestDist = dist;
            }
        }
        return best;

    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("Action: ");
        ret.append(action);
        ret.append('\n');
        if (method == MethodType.POST) {
            ret.append("Method: POST\n");
        } else if (method == MethodType.GET) {
            ret.append("Method: GET\n");
        } else if (method == MethodType.PUT) {
            ret.append("Method: PUT is not supported\n");

        } else if (method == MethodType.UNKNOWN) {
            ret.append("Method: Unknown\n");
        }
        for (InputField ipf : this.inputfields) {

            ret.append(ipf.toString());
            ret.append('\n');
        }

        ret.append(super.toString());
        return ret.toString();
    }

    /**
     * GIbt alle variablen als propertyString zurück
     * 
     * @return
     */
    public String getPropertyString() {
        StringBuilder stbuffer = new StringBuilder();
        boolean first = true;
        for (InputField ipf : this.inputfields) {
            /* nameless key-value are not being sent, see firefox */
            if (ipf.getKey() == null) continue;
            if (first) {
                first = false;
            } else {
                stbuffer.append("&");
            }
            stbuffer.append(ipf.getKey());
            stbuffer.append("=");
            stbuffer.append(ipf.getValue());
        }
        return stbuffer.toString();

    }

    public ArrayList<InputField> getInputFieldsByType(String type) {
        ArrayList<InputField> ret = new ArrayList<InputField>();
        for (InputField ipf : this.inputfields) {
            if (ipf.getType() != null && Regex.matches(ipf.getType(), type)) {
                ret.add(ipf);
            }
        }
        return ret;
    }

    /**
     * Gibt ein RegexObject bezüglich des Form htmltextes zurück
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(String string) {
        return new Regex(htmlcode, string);
    }

    /**
     * Gibt ein RegexObject bezüglich des Form htmltextes zurück
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(Pattern compile) {
        return new Regex(htmlcode, compile);
    }

    /**
     * Gibt zurück ob der gesuchte needle String im html Text bgefunden wurde
     * 
     * @param fileNotFound
     * @return
     */
    public boolean containsHTML(String needle) {
        return new Regex(htmlcode, needle).matches();
    }

    public HashMap<String, String> getVarsMap() {
        HashMap<String, String> ret = new HashMap<String, String>();
        for (InputField ipf : this.inputfields) {
            /* nameless key-value are not being sent, see firefox */
            if (ipf.getKey() == null) continue;
            ret.put(ipf.getKey(), ipf.getValue());
        }
        return ret;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public InputField getInputFieldByName(String name) {
        for (InputField ipf : this.inputfields) {
            if (ipf.getKey() != null && ipf.getKey().equalsIgnoreCase(name)) return ipf;
        }
        return null;

    }

    public InputField getInputFieldByProperty(String key) {
        for (InputField ipf : this.inputfields) {
            if (ipf.getStringProperty(key) != null && ipf.getStringProperty(key).equalsIgnoreCase(key)) return ipf;
        }
        return null;

    }

    public InputField getPreferredSubmit() {

        return preferredSubmit;
    }

    /**
     * Us the i-th submit field when submitted
     * 
     * @param i
     */
    public void setPreferredSubmit(int i) {
        this.preferredSubmit = null;
        for (InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && i-- <= 0) {
                this.preferredSubmit = ipf;
                return;
            }
        }

        throw new IllegalArgumentException("No such Submitfield: " + i);

    }

    /**
     * Tell the form which submit field to use
     * 
     * @param preferredSubmit
     */
    public void setPreferredSubmit(String preferredSubmit) {
        this.preferredSubmit = null;
        for (InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && ipf.getValue().equalsIgnoreCase(preferredSubmit)) {
                this.preferredSubmit = ipf;
                return;
            }
        }
        logger.warning("No exact match for submit found! Trying to find best match now!");
        for (InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getValue() != null && ipf.getType().equalsIgnoreCase("submit") && ipf.getValue().contains(preferredSubmit)) {
                this.preferredSubmit = ipf;
                return;
            }
        }
        throw new IllegalArgumentException("No such Submitfield: " + preferredSubmit);

    }

    public InputField getInputFieldByType(String type) {
        for (InputField ipf : this.inputfields) {
            if (ipf.getType() != null && ipf.getType().equalsIgnoreCase(type)) return ipf;
        }
        return null;

    }

    public boolean hasInputFieldByName(String name) {
        return this.getInputFieldByName(name) != null;
    }

    /**
     * Returns a list of requestvariables
     * 
     * @return
     */
    public ArrayList<RequestVariable> getRequestVariables() {
        ArrayList<RequestVariable> ret = new ArrayList<RequestVariable>();
        for (InputField ipf : this.inputfields) {
            // Do not send not prefered Submit types
            if (this.getPreferredSubmit() != null && ipf.getType().equalsIgnoreCase("submit") && getPreferredSubmit() != ipf) continue;
            if (ipf.getKey() == null) continue;/*
                                                * nameless key-value are not
                                                * being sent, see firefox
                                                */
            if (ipf.getValue() == null) continue;
            if (ipf.getType() != null && ipf.getType().equalsIgnoreCase("image")) {
                ret.add(new RequestVariable(ipf.getKey() + ".x", new Random().nextInt(100) + ""));
                ret.add(new RequestVariable(ipf.getKey() + ".y", new Random().nextInt(100) + ""));
            } else {
                ret.add(new RequestVariable(ipf.getKey(), ipf.getValue()));
            }

        }
        return ret;
    }

}
