package jd.controlling.reconnect.pluginsinc.liveheader.validate;

import java.net.URLEncoder;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;

public class RetryWithReplacedScript extends Exception {
    private String newScript;
    private String description;

    public String getNewScript() {
        return newScript;
    }

    public RetryWithReplacedScript(String description, String replace) {
        this.newScript = replace;
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getDescription() {
        return description;
    }

    public RetryWithReplacedScript(String script, String key, String value, String replace) throws Exception {
        this("replace(\"" + value.replace("\"", "\\\"") + "\",\"" + replace.replace("\"", "\\\"") + "\")", replace(script, key, value, replace));
    }

    public RetryWithReplacedScript(String script, String value, String replace) throws Exception {
        this("replace(\"" + value.replace("\"", "\\\"") + "\",\"" + replace.replace("\"", "\\\"") + "\")", replace(script, null, value, replace));
    }

    private static String replace(String script, String key, String value, String replace) throws Exception {
        if (script.contains(replace) && key != null && !"\"%%%routerip%%%\"".equals(replace)) {
            throw new Exception("Multiple Fields detected: " + replace);
        }
        String newS = script;
        String s = null;
        if (key != null) {
            newS = script.replace(key + "=" + value, key + "=" + replace);
            // if (StringUtils.equals(newS, script)) {
            s = key + "=" + URLEncode.encodeRFC2396(value);

            newS = script.replace(s, key + "=" + replace);
            // }

            // if (StringUtils.equals(newS, script)) {
            // idInputPppPassword=%21nopass%24
            s = URLEncode.encodeRFC2396(key) + "=" + URLEncode.encodeRFC2396(value);
            newS = script.replace(s, URLEncode.encodeRFC2396(key) + "=" + replace);
            // }
            // if (StringUtils.equals(newS, script)) {

            s = key + "=" + URLEncoder.encode(value, "ASCII");
            newS = script.replace(s, key + "=" + replace);
            // }

            // if (StringUtils.equals(newS, script)) {
            // idInputPppPassword=%21nopass%24
            s = URLEncoder.encode(key, "ASCII") + "=" + URLEncoder.encode(value, "ASCII");
            newS = script.replace(s, URLEncode.encodeRFC2396(key) + "=" + replace);
            // }

        }
        if (StringUtils.equals(newS, script)) {
            newS = script.replace(value, replace);
        }

        if (StringUtils.equals(newS, script)) {
            newS = script.replace(URLEncode.encodeRFC2396(value), replace);
        }
        if (StringUtils.equals(newS, script)) {
            newS = script.replace(URLEncoder.encode(value, "ASCII"), replace);
        }
        if (StringUtils.equals(newS, script)) {
            throw new Exception("Could not replace " + replace);
        }
        return newS;
    }

}
