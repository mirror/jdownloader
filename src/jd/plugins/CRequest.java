package jd.plugins;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * CRequest verwaltet Cookies und Referrer automatisch
 * 
 * @author DwD
 * 
 */
public class CRequest {

	private RequestInfo requestInfo;
	/**
	 * ist für getRequest und postRequest
	 */
	public boolean withHtmlCode = true;
	/**
	 * Interner Cookie bestehend aus host, CookieString
	 */
	private HashMap<String, LinkedList<String>> cookie = new HashMap<String, LinkedList<String>>();

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}
	/**
	 * hier kann man die RequestInfo z.B. cRequest.setRequestInfo(cRequest.getForm().getForm().getRequestInfo())
	 * @param requestInfo
	 */
	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
		LinkedList<String> clist = new LinkedList<String>();
		String host = getURL().getHost().replaceFirst("^www\\.", "");
		try {
			clist.addAll(cookie.get(host));
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			String[] bCookie = requestInfo.getCookie().split("; ");
			for (int i = 0; i < bCookie.length; i++) {
				if(!clist.contains(bCookie[i])) clist.add(bCookie[i]);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		if(this.cookie.containsKey(host))this.cookie.remove(host);
		this.cookie.put(host, clist);

	}
	/**
	 * getRequest gibt sich selbst aus
	 * Cookie und Referrer werden automatisch gesetzt
	 * @param url
	 * @return
	 */
	public CRequest getRequest(String url) {
		return getRequest(url, true);
	}
	/**
	 * getRequest gibt sich selbst aus
	 * Cookie und Referrer werden automatisch gesetzt
	 * @param url
	 * @param redirect
	 * @return
	 */
	public CRequest getRequest(String url, boolean redirect) {

		try {
			URL mURL = new URL(url);
			if (withHtmlCode)
				setRequestInfo(Plugin.getRequest(mURL,
						getCookie(mURL.getHost()), urlToString(), redirect));
			else
				setRequestInfo(Plugin.getRequestWithoutHtmlCode(mURL,
						getCookie(mURL.getHost()), urlToString(), redirect));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}
	/**
	 * postRequest gibt sich selbst aus
	 * Cookie und Referrer werden automatisch gesetzt
	 * @param url
	 * @param parameter
	 * @return
	 */
	public CRequest postRequest(String url, String parameter) {
		return postRequest(url, parameter, true);
	}
	/**
	 * postRequest gibt sich selbst aus
	 * Cookie und Referrer werden automatisch gesetzt
	 * @param url
	 * @param parameter
	 * @param redirect
	 * @return
	 */
	public CRequest postRequest(String url, String parameter, boolean redirect) {

		try {
			URL mURL = new URL(url);
			if (withHtmlCode)
				setRequestInfo(Plugin.postRequest(mURL, getCookie(mURL
						.getHost()), urlToString(), null, parameter, redirect));
			else
				setRequestInfo(Plugin.postRequestWithoutHtmlCode(mURL,
						getCookie(mURL.getHost()), urlToString(), parameter,
						redirect));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}
    /**
     * gibt die Forms der requestInfo aus
     * @param pattern
     * @return
     */
	public Form[] getforms() {
		try {
			return requestInfo.getForms();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
    /**
     * gibt die erste Form der requestInfo aus
     * @return
     */
	public Form getform() {
		try {
			return requestInfo.getForm();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
    /**
     * Macht einen Regexp auf die requestInfo
     * @param pattern
     * @return
     */
	public Regexp getRegexp(String pattern) {
		try {
			return requestInfo.getRegexp(pattern);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
    /**
     * @return the connection
     */
	public HttpURLConnection getConnection() {
		return requestInfo.getConnection();
	}
    /**
     * @return the URL
     */
	public URL getURL() {
		try {
			return getConnection().getURL();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
    /**
     * @return URL.toString();
     */
	public String urlToString() {
		try {
			return getURL().toString();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	/**
	 * gibt den Cookie aus
	 * @return
	 */
	public String getCookie() {
		try {
			return getCookie(getURL().getHost());
		} catch (Exception e) {
		}
		try {
			return requestInfo.getCookie();
		} catch (Exception ec) {
		}
		return null;
	}
	/**
	 * gibt den Cookie für einen bestimmten Host aus
	 * @param host
	 * @return
	 */
	public String getCookie(String host) {
		try {
			LinkedList<String> c = new LinkedList<String>();
			try {
				c.addAll(cookie.get(host));
			} catch (Exception e) {
				// TODO: handle exception
			}
			try {
				if (host.matches(".*?\\..*?\\..*?")) {
					host = host.replaceFirst(".*?\\.", "");
					c.addAll(cookie.get(host));
				}
			} catch (Exception e) {
			}
        	ListIterator<String> iter = c.listIterator();
        	LinkedList<String> baklist = new LinkedList<String>();
        	String cookie = "";
        	boolean last = false;
        	while (iter.hasNext()) {
        		String string = iter.next();
        		if(!baklist.contains(string))
        		{
        			baklist.add(string);
        		cookie += (last? "; ":"") + iter.next();
        		last = true;
        		}
			}
			return cookie;
		} catch (Exception e) {
		}
		return null;
	}

	public String getHtmlCode() {
		return toString();
	}

	public String toString() {
		try {
			return requestInfo.getHtmlCode();
		} catch (Exception ec) {
		}
		return null;
	}
}
