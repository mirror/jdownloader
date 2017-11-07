package jd.plugins.components;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

public class PluginJSonUtils {
    public static String escape(final String s) {
        final StringBuilder sb = new StringBuilder();
        char ch;
        String ss;
        for (int i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            switch (ch) {
            case '"':
                sb.append("\\\"");
                continue;
            case '\\':
                sb.append("\\\\");
                continue;
            case '\b':
                sb.append("\\b");
                continue;
            case '\f':
                sb.append("\\f");
                continue;
            case '\n':
                sb.append("\\n");
                continue;
            case '\r':
                sb.append("\\r");
                continue;
            case '\t':
                sb.append("\\t");
                continue;
            }
            if (ch >= '\u0000' && ch <= '\u001F' || ch >= '\u007F' && ch <= '\u009F' || ch >= '\u2000' && ch <= '\u20FF') {
                ss = Integer.toHexString(ch);
                sb.append("\\u");
                for (int k = 0; k < 4 - ss.length(); k++) {
                    sb.append('0');
                }
                sb.append(ss.toUpperCase(Locale.ENGLISH));
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * @param string
     * @return
     */
    public static String unescape(final String s) {
        char ch;
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        int ii;
        int i;
        for (i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            try {
                switch (ch) {
                case '\\':
                    ch = s.charAt(++i);
                    switch (ch) {
                    case '"':
                        sb.append('"');
                        continue;
                    case '\\':
                        sb.append('\\');
                        continue;
                    case 'r':
                        sb.append('\r');
                        continue;
                    case 'n':
                        sb.append('\n');
                        continue;
                    case 't':
                        sb.append('\t');
                        continue;
                    case 'f':
                        sb.append('\f');
                        continue;
                    case 'b':
                        sb.append('\b');
                        continue;
                    case 'u':
                        sb2.delete(0, sb2.length());
                        i++;
                        ii = i + 4;
                        for (; i < ii; i++) {
                            ch = s.charAt(i);
                            if (sb2.length() > 0 || ch != '0') {
                                sb2.append(ch);
                            }
                        }
                        i--;
                        // can not use short....
                        // java.lang.NumberFormatException: Value out of range. Value:"8abf" Radix:16
                        sb.append((char) Long.parseLong(sb2.toString(), 16));
                        continue;
                    default:
                        sb.append(ch);
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    public static final String NULL = new String("null");

    public static String getJsonValue(final String source, final String key) {
        return getJson(source, key, false);
    }

    public static String getJsonValue(final Browser browser, final String key) {
        return getJson(browser.toString(), key, false);
    }

    public static String getJson(final String source, final String key, final boolean returnNullAsString) {
        if (source == null || key == null) {
            return null;
        }
        // Standard json based
        String result = new Regex(source, "\"" + Pattern.quote(key) + "\"[ \t]*:[ \t]*(![01]|-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if ("null".equals(result)) {
            if (returnNullAsString) {
                return NULL;
            } else {
                // null here means (Object/Value)null, so lets return null
                return null;
            }
        }
        if (result == null) {
            result = new Regex(source, "\"" + Pattern.quote(key) + "\"[ \t]*:[ \t]*\"([^\"]*)\"").getMatch(0);
            if (result != null) {
                // some rudimentary detection if we have break'd at the wrong place.
                while (result.endsWith("\\")) {
                    String xtraResult = new Regex(source, "\"" + Pattern.quote(key) + "\"[ \t]*:[ \t]*\"(" + Pattern.quote(result) + "\"[^\"]*\"?)\"").getMatch(0);
                    if (xtraResult != null) {
                        result = xtraResult;
                    } else {
                        break;
                    }
                }
            }
        }
        if (result == null) {
            // javascript doesn't always encase keyname with quotation
            result = new Regex(source, "[^a-zA-Z0-9_\\-]+" + Pattern.quote(key) + "[ \t]*:[ \t]*(![01]|-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
            if ("null".equals(result)) {
                if (returnNullAsString) {
                    return NULL;
                } else {
                    // null here means (Object/Value)null, so lets return null
                    return null;
                }
            }
            if (result == null) {
                result = new Regex(source, "[^a-zA-Z0-9_\\-]+" + Pattern.quote(key) + "[ \t]*:[ \t]*\"([^\"]*)\"").getMatch(0);
                if (result != null) {
                    // some rudimentary detection if we have break'd at the wrong place.
                    while (result.endsWith("\\")) {
                        String xtraResult = new Regex(source, "[^a-zA-Z0-9_\\-]+" + Pattern.quote(key) + "[ \t]*:[ \t]*\"(" + Pattern.quote(result) + "\"[^\"]*\"?)\"").getMatch(0);
                        if (xtraResult != null) {
                            result = xtraResult;
                        } else {
                            break;
                        }
                    }
                }
            }
            // javascript also doesn't always use "
            // js with '
            if (result == null) {
                result = new Regex(source, "'" + Pattern.quote(key) + "'[ \t]*:[ \t]*(![01]|-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
                if ("null".equals(result)) {
                    if (returnNullAsString) {
                        return NULL;
                    } else {
                        // null here means (Object/Value)null, so lets return null
                        return null;
                    }
                }
                if (result == null) {
                    result = new Regex(source, "'" + Pattern.quote(key) + "'[ \t]*:[ \t]*'([^']*)'").getMatch(0);
                    if (result != null) {
                        // some rudimentary detection if we have break'd at the wrong place.
                        while (result.endsWith("'")) {
                            String xtraResult = new Regex(source, "\"" + Pattern.quote(key) + "\"[ \t]*:[ \t]*'(" + Pattern.quote(result) + "'[^']*'?)'").getMatch(0);
                            if (xtraResult != null) {
                                result = xtraResult;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }
        result = validateResultForArrays(source, result);
        if (result != null) {
            result = unescape(result);
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * @param source
     * @param key
     * @return
     */
    public static String getJson(final String source, final String key) {
        return getJson(source, key, true);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * @param ibr
     * @param key
     * @return
     */
    public static String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key, true);
    }

    public static String getJson(final Browser ibr, final String key, final boolean returnNullAsString) {
        return getJson(ibr.toString(), key, returnNullAsString);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided Browser.
     *
     * @author raztoki
     */
    public static String getJsonArray(final Browser ibr, final String key) {
        return getJsonArray(ibr.toString(), key);
    }

    /**
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     */
    public static String getJsonArray(final String source, final String key) {
        if (source == null || key == null) {
            return null;
        }
        String result = new Regex(source, "\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\[\\s*\\{.*?\\}\\s*\\]|\\[.*?\\])\\s*(?:,|\\})").getMatch(0);
        if (result == null) {
            // javascript doesn't always encase keyname with quotation
            result = new Regex(source, Pattern.quote(key) + "\\s*:\\s*(\\[\\s*\\{.*?\\}\\s*\\]|\\[.*?\\])\\s*(?:,|\\})").getMatch(0);
        }
        result = validateResultForArrays(source, result);
        if (result != null) {
            result = unescape(result);
        }
        return result;
    }

    /**
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    public static String[] getJsonResultsFromArray(final String source) {
        if (source == null) {
            return null;
        }
        try {
            // use json parser, because regex can easily fail, see commit message
            final List<Object> jsonParsed = JSonStorage.restoreFromString(source, TypeRef.LIST);
            if (jsonParsed != null) {
                final String ret[] = new String[jsonParsed.size()];
                for (int i = 0; i < ret.length; i++) {
                    ret[0] = JSonStorage.toString(jsonParsed.get(i));
                }
                return ret;
            }
        } catch (final Throwable e) {
        }
        final String[] result;
        // two types of actions can happen here. it could be series of [{"blah":"blah1"},{"blah":"blah2"}] or series of ["blah","blah2"]
        if (new Regex(source, "^\\s*\\[\\s*\\{.+$").matches()) {
            result = new Regex(source, "\\s*(?:\\[|,)\\s*(\\{.*?\\})\\s*").getColumn(0);
        } else {
            result = new Regex(source, "\\s*(?:\\[|,)\\s*\"([^\"]*)\"\\s*").getColumn(0);
        }
        if (result != null) {
            // some rudimentary detection if we have break'd at the wrong place.
            for (int i = 0; i < result.length; i++) {
                result[i] = validateResultForArrays(source, result[i]);
            }
        }
        return result;
    }

    /**
     * Wrapper<br/>
     * Tries to gather nested \"key\":{.*?} from specified Browser
     *
     * @author raztoki
     * @param key
     * @return
     */
    public static String getJsonNested(final Browser ibr, final String key) {
        return getJsonNested(ibr.toString(), key);
    }

    /**
     * pulls nested { } when object has key
     *
     * @author raztoki
     * @param source
     * @param key
     * @return
     */
    public static String getJsonNested(final String source, final String key) {
        if (source == null || key == null) {
            return null;
        }
        String result = new Regex(source, "\"" + Pattern.quote(key) + "\"[ \t]*:[ \t]*\\{(.*?)\\}(?:,|\\})").getMatch(0);
        if (result == null) {
            // javascript doesn't always encase keyname with quotation
            result = new Regex(source, Pattern.quote(key) + "[ \t]*:[ \t]*\\{(.*?)\\}(?:,|\\})").getMatch(0);
        }
        result = validateResultForArrays(source, result);
        return result;
    }

    /**
     * Creates and or Amends Strings ready for JSon requests, with the correct JSon formatting and escaping.
     *
     * @author raztoki
     * @param source
     * @param key
     * @param value
     * @return
     */
    public static String ammendJson(final String source, final String key, final Object value) {
        if (key == null || value == null) {
            return null;
        }
        String result = source;
        if (result == null) {
            result = "{";
        } else {
            result = result.substring(0, result.length() - 1) + ",";
        }
        final boolean useBracket = value instanceof String;
        result = result.concat("\"" + key + "\":" + (useBracket ? "\"" : "") + (useBracket ? escape(value.toString()) : value) + (useBracket ? "\"" : "") + "}");
        return result;
    }

    /**
     * Applies correction when result contains incorrect array brackets endings. <br />
     * Why is this needed? Using Regex to find values true ending is next to impossible. This will correct it!
     *
     * @author raztoki
     * @param source
     * @param result
     * @return
     */
    public static String validateResultForArrays(final String source, final String result) {
        if (result == null) {
            return result;
        }
        String i = result;
        // validate json; count opening { and }, [ and ], correct when misallignment
        while (true) {
            final String[] bracketA = new Regex(i, "(?!\\\\\\{)\\{").getColumn(-1);
            final String[] bracketB = new Regex(i, "(?!\\\\\\})\\}").getColumn(-1);
            if (bracketA != null && bracketB != null) {
                if (bracketA.length == bracketB.length) {
                    break;
                }
                final String newi = new Regex(source, Pattern.quote(i) + "(?!\\\\\\})[^\\}]*\\}").getMatch(-1);
                if (newi == null) {
                    return i;
                }
                i = newi;
                continue;
            }
            break;
        }
        while (true) {
            final String[] bracketC = new Regex(i, "(?!\\\\\\[)\\[").getColumn(-1);
            final String[] bracketD = new Regex(i, "(?!\\\\\\])\\]").getColumn(-1);
            if (bracketC != null && bracketD != null) {
                if (bracketC.length == bracketD.length) {
                    break;
                }
                final String newi = new Regex(source, Pattern.quote(i) + "(?!\\\\\\])[^\\]]*\\]").getMatch(-1);
                if (newi == null) {
                    return i;
                }
                i = newi;
                continue;
            }
            break;
        }
        return i;
    }

    /**
     * JSon boolean parser, which also evaluates int values as boolean.
     *
     * @author raztoki
     * @param input
     * @return
     */
    public static boolean parseBoolean(final String input) {
        if (input == null) {
            return false;
        }
        // true : false
        // 0 : 1 = false : true
        // 0 : !0 = false : true
        if ("true".equalsIgnoreCase(input)) {
            return true;
        } else if ("false".equalsIgnoreCase(input)) {
            return false;
        } else if ("0".equalsIgnoreCase(input)) {
            return false;
        } else if ("1".equalsIgnoreCase(input)) {
            return true;
        } else if ("!0".equalsIgnoreCase(input)) {
            return true;
        } else if ("!1".equalsIgnoreCase(input)) {
            return false;
        }
        return false;
    }
}
