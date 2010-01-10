//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RequestHeader {

    /**
     *  For more header fields see @link(http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14).
     */

    // request headers
    public static final String ACCEPT               = "Accept";
    public static final String ACCEPT_CHARSET       = "Accept-Charset";
    public static final String ACCEPT_ENCODING      = "Accept-Encoding";
    public static final String ACCEPT_LANGUAGE      = "Accept-Language";
    public static final String AUTHORIZATION        = "Authorization";
    public static final String CONNECTION           = "Connection";
    public static final String COOKIE               = "Cookie";
    public static final String EXPECT               = "Expect";
    public static final String FROM                 = "From";
    public static final String HOST                 = "Host";
    public static final String IF_MODIFIED_SINCE    = "If-Modified-Since";
    public static final String MAX_FORWARDS         = "Max-Forwards";
    public static final String RANGE                = "Range";
    public static final String REFERER              = "Referer";
    public static final String TE                   = "TE";
    public static final String USER_AGENT           = "User-Agent";

    // response headers
    public static final String ACCEPT_RANGES        = "Accept-Ranges";
    public static final String AGE                  = "Age";
    public static final String LOCATION             = "Location";
    public static final String SERVER               = "Server";
    public static final String RETRY_AFTER          = "Retry-After";
    
    // entity headers
    public static final String ALLOW                = "Allow";
    public static final String CACHE_CONTROL        = "Cache-Control";
    public static final String CONTENT_ENCODING     = "Content-Encoding";
    public static final String CONTENT_LANGUAGE     = "Content-Language";
    public static final String CONTENT_LENGTH       = "Content-Length";
    public static final String CONTENT_LOCATION     = "Content-Location";
    public static final String CONTENT_MD5          = "Content-MD5";
    public static final String CONTENT_RANGE        = "Content-Range";
    public static final String CONTENT_TYPE         = "Content-Type";
    public static final String DATE                 = "Date";
    public static final String EXPIRES              = "Expires";
    public static final String LAST_MODIFIED        = "Last-Modified";
    public static final String PRAGMA               = "Pragma";

    // AJAX headers
    public static final String X_REQUESTED_WITH     = "X-Requested-With";

    // members
    private final List<String> keys;
    private final List<String> values;
    private boolean dominant = false;

    public RequestHeader() {
        this.keys = new ArrayList<String>();
        this.values = new ArrayList<String>();
    }

    public RequestHeader(final Map<String, String> headers) {
        this();
        this.putAll(headers);
    }

    /**
     * if a header is dominant, it will not get merged with existing headers. It
     * will replace it completely
     * 
     * @param dominant
     */
    public void setDominant(final boolean dominant) {
        this.dominant = dominant;
    }

    public void put(final String key, final String value) {
        final int keysSize = keys.size();
        final String trim = key.trim();
        for (int i = 0; i < keysSize; i++) {
            if (keys.get(i).equalsIgnoreCase(trim)) {
                keys.set(i, key);
                values.set(i, value);
                return;
            }
        }
        keys.add(key);
        values.add(value);
    }

    public String getKey(final int index) {
        return keys.get(index);
    }

    public String getValue(final int index) {
        return values.get(index);
    }

    public int size() {
        return keys.size();
    }

    public Object clone() {
        final RequestHeader newObj = new RequestHeader();
        newObj.keys.addAll(keys);
        newObj.values.addAll(values);
        return newObj;
    }

    public String remove(final String key) {
        final int index = keys.indexOf(key);

        if (index >= 0) {
            keys.remove(index);
            return values.remove(index);
        }

        return null;
    }

    public void putAll(final Map<String, String> properties) {
        for (final Entry<String, String> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        }
    }

    public void putAll(final RequestHeader headers) {
        final int size = headers.size();
        for (int i = 0; i < size; i++) {
            final String key = headers.getKey(i);
            final String value = headers.getValue(i);
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        }
    }

    public String get(final String key) {
        final int index = keys.indexOf(key);
        return (index >= 0) ? values.get(index) : null;
    }

    public void clear() {
        keys.clear();
        values.clear();
    }

    public boolean contains(final String string) {
        return keys.contains(string);
    }

    public void setAt(final int index, final String key, final String value) {
        if (keys.isEmpty()) {
            this.put(key, value);
            return;
        }
        keys.add(keys.get(keys.size() - 1));
        values.add(values.get(values.size() - 1));
        for (int e = keys.size() - 3; e >= index; e--) {
            keys.set(e + 1, keys.get(e));
            values.set(e + 1, values.get(e));
        }
        keys.set(index, key);
        values.set(index, value);
    }

    public boolean isDominant() {
        return dominant;
    }
}
