package myIrcBot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
/**
 * GNU GPL Lizenz Den offiziellen englischen Originaltext finden Sie unter http://www.gnu.org/licenses/gpl.html.
 * 
 *
 */
public class Utilities {
	public static final String ACCEPT_LANGUAGE = "de, en-gb;q=0.9, en;q=0.8";
	public static void saveObject(Object objectToSave, File fileOutput) {
		if (objectToSave == null || fileOutput == null) {
			return;
		}
		// logger.info("save file: " + fileOutput + " object: " + objectToSave);
		if (fileOutput.exists())
			fileOutput.delete();
		try {
			FileOutputStream fos = new FileOutputStream(fileOutput);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(objectToSave);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.getStackTrace();
		}
	}
    @SuppressWarnings("unchecked")
    public static Map revSortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        // logger.info(list);
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
	public static Object loadObject(File fileInput) {
		// logger.info("load file: " + fileInput + " (xml:" + asXML + ")");
		Object objectLoaded = null;
		if (fileInput == null) {
			return null;
		}
		if (fileInput != null) {
			// String hash = getLocalHash(fileInput);
			try {
				FileInputStream fis = new FileInputStream(fileInput);

				ObjectInputStream ois = new ObjectInputStream(fis);
				objectLoaded = ois.readObject();
				ois.close();
				// Object15475dea4e088fe0e9445da30604acd1
				// Object80d11614908074272d6b79abe91eeca1
				// logger.info("Loaded Object (" + hash + "): ");
				return objectLoaded;
			} catch (ClassNotFoundException e) {
				// 
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static RequestInfo readFromURL(HTTPConnection urlInput)
	throws IOException {
// Content-Encoding: gzip
BufferedReader rd;
if (urlInput.getHeaderField("Content-Encoding") != null
		&& urlInput.getHeaderField("Content-Encoding")
				.equalsIgnoreCase("gzip")) {
	rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(
			urlInput.getInputStream())));
} else {
	rd = new BufferedReader(new InputStreamReader(urlInput
			.getInputStream()));
}
String line;
StringBuffer htmlCode = new StringBuffer();
while ((line = rd.readLine()) != null) {
	htmlCode.append(line + "\r\n");
}
String location = urlInput.getHeaderField("Location");
String cookie = "";
try {
	cookie = getCookieString(urlInput);
} catch (Exception e) {
	// TODO: handle exception
}

int responseCode = 0;
responseCode = urlInput.getResponseCode();
RequestInfo requestInfo = new RequestInfo(htmlCode.toString(),
		location, cookie, urlInput.getHeaderFields(), responseCode);
rd.close();
return requestInfo;
}

public static boolean writeLocalFile(File file, String content,
	boolean append) {
try {
	if (!append && file.isFile()) {
		if (!file.delete()) {
			return false;
		}
	}
	if (file.getParent() != null && !file.getParentFile().exists()) {
		file.getParentFile().mkdirs();
	}
	file.createNewFile();
	BufferedWriter f = new BufferedWriter(new FileWriter(file, true));
	f.write(content);
	f.close();
	return true;
} catch (Exception e) {
	e.printStackTrace();
	// 
	return false;
}
}

public static String getCookieString(HTTPConnection con) {
String cookie = "";
try {
	List<String> list = con.getHeaderFields().get("Set-Cookie");
	ListIterator<String> iter = list.listIterator(list.size());
	boolean last = false;
	while (iter.hasPrevious()) {
		cookie += (last ? "; " : "")
				+ iter.previous().replaceFirst("; expires=.*", "");
		last = true;
	}
} catch (Exception e) {
	e.printStackTrace();
}
return cookie;
}

public static String getLocalFile(File file) {
if (!file.exists())
	return "";
BufferedReader f;
try {
	f = new BufferedReader(new FileReader(file));

	String line;
	StringBuffer ret = new StringBuffer();
	String sep = "\r\n";
	while ((line = f.readLine()) != null) {
		ret.append(line + sep);
	}
	f.close();
	return ret.toString();
} catch (IOException e) {

}
return "";
}
public static RequestInfo postRequest(URL url, String cookie,
		String referrer, HashMap<String, String> requestProperties,
		String parameter, boolean redirect, int readTimeout,
		int requestTimeout) throws IOException {
	// logger.finer("post: "+link+"(cookie:"+cookie+" parameter:
	// "+parameter+")");
	// long timer = System.currentTimeMillis();
	HTTPConnection httpConnection = new HTTPConnection(url.openConnection());
	httpConnection.setReadTimeout(readTimeout);
	httpConnection.setConnectTimeout(requestTimeout);
	httpConnection.setInstanceFollowRedirects(redirect);
	if (referrer != null)
		httpConnection.setRequestProperty("Referer", referrer);
	else
		httpConnection.setRequestProperty("Referer", "http://"
				+ url.getHost());
	if (cookie != null)
		httpConnection.setRequestProperty("Cookie", cookie);
	httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
	httpConnection
			.setRequestProperty(
					"User-Agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
	if (requestProperties != null) {
		Set<String> keys = requestProperties.keySet();
		Iterator<String> iterator = keys.iterator();
		String key;
		while (iterator.hasNext()) {
			key = iterator.next();
			httpConnection.setRequestProperty(key, requestProperties
					.get(key));
		}
	}
	if (parameter != null) {
		parameter = parameter.trim();
		httpConnection.setRequestProperty("Content-Length", parameter
				.length()
				+ "");
	}
	httpConnection.setDoOutput(true);
	httpConnection.connect();

	httpConnection.post(parameter);

	RequestInfo requestInfo = readFromURL(httpConnection);

	requestInfo.setConnection(httpConnection);
	// logger.finer("postRequest " + url + ": " +
	// (System.currentTimeMillis() - timer) + " ms");
	return requestInfo;
}
/**
 * @author JD-Team Macht ein urlRawEncode und spart dabei die angegebenen
 *         Zeichen aus
 * @param str
 * @return str URLCodiert
 */
@SuppressWarnings("deprecation")
public static String urlEncode(String str) {
    return URLEncoder.encode(str);

}
public static RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
    // logger.finer("get: "+link+"(cookie: "+cookie+")");
    //long timer = System.currentTimeMillis();
    HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
    httpConnection.setReadTimeout(10000);
    httpConnection.setConnectTimeout(10000);
    httpConnection.setInstanceFollowRedirects(redirect);
    // wenn referrer nicht gesetzt wurde nimmt er den host als referer
    if (referrer != null) httpConnection.setRequestProperty("Referer", referrer);

    // httpConnection.setRequestProperty("Referer", "http://" +
    // link.getHost());
    if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
    // TODO User-Agent als Option ins menu
    // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
    // lassen
    // so ist das Programm nicht so auffallig
    httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
    httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
    RequestInfo requestInfo = readFromURL(httpConnection);
    requestInfo.setConnection(httpConnection);
    // logger.finer("getRequest " + link + ": " +
    // (System.currentTimeMillis() - timer) + " ms");
    return requestInfo;
}
public static String[] getHttpLinks(String data, String url) {
    String[] protocols = new String[] { "h.{2,3}", "https", "ccf", "dlc", "ftp" };
    String protocolPattern = "(";
    for (int i = 0; i < protocols.length; i++) {
        protocolPattern += protocols[i] + ((i + 1 == protocols.length) ? ")" : "|");
    }


    url = url == null ? "" : url;
    Matcher m;
    String link;
    String basename = "";
    String host = "";
    LinkedList<String> set = new LinkedList<String>();
    Pattern[] basePattern = new Pattern[] {
   		 Pattern.compile("(?s)<[ ]?base[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE),
		 Pattern.compile("(?s)<[ ]?base[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE),
		 Pattern.compile("(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE),
    };
    for (int i = 0; i < basePattern.length; i++) {
        m = basePattern[i].matcher(data);
        if (m.find()) {
            url = m.group(1);
            break;
        }
	}
    if (url != null) {
        url = url.replace("http://", "");
        int dot = url.lastIndexOf('/');
        if (dot != -1)
            basename = url.substring(0, dot + 1);
        else
            basename = "http://" + url + "/";
        dot = url.indexOf('/');
        if (dot != -1)
            host = "http://" + url.substring(0, dot);
        else
            host = "http://" + url;
        url = "http://" + url;
    } else
        url = "";

    Pattern[] linkAndFormPattern = new Pattern[] {
      		 Pattern.compile("(?s)<[ ]?a[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE),
   		 Pattern.compile("(?s)<[ ]?a[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE),
   		 Pattern.compile("(?s)<[ ]?a[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE),
   		 Pattern.compile("(?s)<[ ]?form[^>]*?action=\"(.*?)\"", Pattern.CASE_INSENSITIVE),
   		 Pattern.compile("(?s)<[ ]?form[^>]*?action='(.*?)'", Pattern.CASE_INSENSITIVE),
   		 Pattern.compile("(?s)<[ ]?form[^>]*?action=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE),
       };
    for (int i = 0; i < linkAndFormPattern.length; i++) {
        m = linkAndFormPattern[i].matcher(data);
        while (m.find()) {
            link = m.group(1);
            link = link.replaceAll(protocols[0] + "://", "http://");
            link = link.replaceAll("https?://.*http://", "http://");
            for (int j = 1; j < protocols.length; j++) {
                link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
            }

            if ((link.length() > 6) && (link.substring(0, 7).equals("http://")))
                ;
            else if (link.length() > 0) {
                if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                    link = "http://" + link;
                }
                if (link.charAt(0) == '/') {
                    link = host + link;
                } else if (link.charAt(0) == '#') {
                    link = url + link;
                } else {
                    link = basename + link;
                }
            }
            if (!set.contains(link)) {
                set.add(link);
            }
        }
    }
    data = data.replaceAll("(?s)<.*?>", "");
    m = Pattern.compile("www\\.[^\\s\"]*", Pattern.CASE_INSENSITIVE).matcher(data);
    while (m.find()) {
        link = "http://" + m.group();
        link = link.replaceAll(protocols[0] + "://", "http://");
        link = link.replaceFirst("^www\\..*" + protocols[0] + "://", "http://");
        link = link.replaceAll("https?://.*http://", "http://");
        for (int j = 1; j < protocols.length; j++) {
            link = link.replaceFirst("^www\\..*" + protocols[j] + "://", protocols[j] + "://");
        }
        if (!set.contains(link)) {
            set.add(link);
        }
    }
    m = Pattern.compile(protocolPattern + "://[^\\s\"]*", Pattern.CASE_INSENSITIVE).matcher(data);
    while (m.find()) {
        link = m.group();
        link = link.replaceAll(protocols[0] + "://", "http://");
        link = link.replaceAll("https?://.*http://", "http://");
        for (int j = 1; j < protocols.length; j++) {
            link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
        }
        // .replaceFirst("h.*?://",
        // "http://").replaceFirst("http://.*http://", "http://");
        if (!set.contains(link)) {
            set.add(link);
        }
    }
    return (String[]) set.toArray(new String[set.size()]);
}
}
