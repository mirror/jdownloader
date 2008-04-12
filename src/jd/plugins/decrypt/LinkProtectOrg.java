//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;
import jd.plugins.DownloadLink;

public class LinkProtectOrg extends PluginForDecrypt {
    private static final String  CODER             = "Bo0nZ";
    private static final String  HOST              = "linkprotect.org";
    private static final String  PLUGIN_NAME       = HOST;
    private static final String  PLUGIN_VERSION    = "1.0.0.0";
    private static final String  PLUGIN_ID         = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    private static final Pattern PAT_SUPPORTED     = getSupportPattern("http://[*]linkprotect.org/[+]");

    /*
     * Suchmasken (z.B. Fehler)
     */
    private static final String  FILE_URL          = "[°]°</td>°<td align=\"center\" width=\"60\">°<a href=\"°\" target=\"new\"><img src=\"img/downloadbutton.png\" border=\"0\"></a>";
    private static final String  FRAME_URL         = "<frame scrolling=\"auto\" noresize src=\"°\">";
    private static final String  ENCRYPTED_STRING  = "document.write(°('°'));";
    private static final String  REDIRECT_URL1     = "URL=°\"";
    private static final String  REDIRECT_URL2     = "document.location.replace('°');";
    private static final String  REDIRECT_URL3     = "src=\"°\"";
    private static final String  ENCRYPTED_STRING2 = ");}var °=°(°(\"°\"))";
    private static final String  ENCRYPTED_STRING3 = "));var °=°(°,\"°\")";
    private static final String  RS_LINK           = "<form action=\"http:\\/\\/°\" method=\"post\">you want to download the file <b>°</b>";
    private static final String  CAPTCHA           = "<img id=\"captcha\" src=\"°\" ° />";
    private static final String  FORM_ID           = "<input type=\"hidden\" name=\"id\" value=\"°\">";
    private static final String  FORM_HASH         = "<input type=\"hidden\" name=\"hash\" value=\"°\">";
    private static final String  FORM_SUBMIT       = "<input id=\"search-submit\" type=\"submit\" name=\"login\" value=\"°\">";
    private static final String  ERROR_CAPTCHA     = "Captcha-code wrong. Please retry.";
    private static final String  PAGES             = "<a href=\"?s=°#down\">°</a>";
    private static final String  PASSWORD          = "Visitorpassword:";
    private static final String  WRONG_PASSWORD    = "The visitorpassword you have entered is wrong.";

    /*
     * Konstruktor
     */
    public LinkProtectOrg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    /*
     * Funktionen
     */
    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            RequestInfo reqinfo; // Zum Aufrufen von WebSeiten
    		try {
    			String strURL = parameter;
    			URL url = new URL(strURL);
    			File captchaFile = null;
    			String plainCaptcha = null;
    			
    			// so lange, bis Captcha richtig eingegeben/erkannt
    			// so lange, bis Passwort richtig eingegeben
    			while (true) {
	    			reqinfo = getRequest(url); // Seite aufrufen
	    			
	    			boolean hasPass = false;
	    			boolean hasCaptcha = false;
	    			String postData = "";
	    			
	    			// formular-elemente
	    			String formID = getSimpleMatch(reqinfo.getHtmlCode(), FORM_ID, 0);
	    			String formHash = getSimpleMatch(reqinfo.getHtmlCode(), FORM_HASH, 0);
	    			String formSubmit = getSimpleMatch(reqinfo.getHtmlCode(), FORM_SUBMIT, 0);
	    			
	    			// PASSWORD
	    			if (formID != null && reqinfo.getHtmlCode().contains(PASSWORD)) {
	    				hasPass = true;
	    				// Passwort abfragen
	                	String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
	                	// Daten die spaetzer gesendet werden
	                	postData = "id=" + formID + "&besucherpasswort=" + pwd + "&login=Verify+*";
	    			}
	    			
	    			// CAPTCHA
	    			String CaptchaURL = null;
	    			CaptchaURL = getSimpleMatch(reqinfo.getHtmlCode(), CAPTCHA, 0);
	    			
	    			// Captcha/Formulardaten vorhanden?
	    			if (CaptchaURL != null && formID != null && formHash != null && formSubmit != null) {
	    				hasCaptcha = true;
	    				CaptchaURL = "http://" + HOST + "/" + CaptchaURL;
	    				
	                    captchaFile = getLocalCaptchaFile(this, ".gif");
	                    
	                    // Captcha downloaden
	                    boolean dlSuccess = JDUtilities.download(captchaFile, CaptchaURL);
	                    
	                    // Captcha-Download nicht erfolgreich?
	                    if(!dlSuccess || !captchaFile.exists() || captchaFile.length()==0){
	                        logger.severe("Captcha-Download nicht erfolgreich. Versuche erneut.");
	                        
	                        try { Thread.sleep(1000);
	                        } catch (InterruptedException e) { }
	                        
	                        continue; // retry
	                    }
	                    
	                    plainCaptcha = Plugin.getCaptchaCode(captchaFile, this);

	    				// postData enthaelt entweder Passwort-Daten oder nichts
	    				if (postData.length() > 0)
	    					postData += "&hash=" + formHash + "&code=" + plainCaptcha;
	    				else
	    					postData += "id=" + formID + "&hash=" + formHash + "&code=" + plainCaptcha + "&login=" + formSubmit;
	    			}
	    			
	    			// Passwort oder Captcha vorhanden? Dann Daten senden
	    			if (hasPass || hasCaptcha) {
	                	reqinfo = postRequest(url, reqinfo.getCookie(), strURL, null, postData, false);
	                	
	                    // Falsches Passwort
		    			if (reqinfo.getHtmlCode().contains(WRONG_PASSWORD)) {
	                        logger.severe("Passwort falsch! Versuche erneut.");
                            if(captchaFile!=null && plainCaptcha != null){
                                JDUtilities.appendInfoToFilename(this, captchaFile,plainCaptcha, false);
                            }
		    				continue; // retry
		    			}
		    			
	                    // Falscher Captcha-Code
		    			if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA)) {
	                        logger.severe("Captcha-Code falsch! Versuche erneut.");
                            if(captchaFile!=null && plainCaptcha != null){
                                JDUtilities.appendInfoToFilename(this, captchaFile,plainCaptcha, false);
                            }
		    				continue; // retry
		    			}
		    			
		    			// Passwort richtig und/oder Captcha richtig
		    			// Seite erneut mir Cookies aufrufen
	                	if (reqinfo.getLocation() != null) {
	                		reqinfo = getRequest(url, reqinfo.getCookie(), strURL, false);
	                	}
                        if(captchaFile!=null && plainCaptcha != null){
                            JDUtilities.appendInfoToFilename(this, captchaFile,plainCaptcha, true);
                        }
                   
		    			break; // Schleife abbrechen
	    			
	    			} else {
                        if(captchaFile!=null && plainCaptcha != null){
                            JDUtilities.appendInfoToFilename(this, captchaFile,plainCaptcha, true);
                        }
	    				// kein Captcha & kein Pass: Schleife abbrechen
	    				break;
	    			}
    			}

    			int countLinks = 0;
    			int countSubPages = 0;
    			
    			// Unterseiten nur einbeziehen, wenn es selbst nicht schon
                // Unterseite ist
    			if (strURL.contains("?s=")==false) {
    				
 	    			// teilen sich Links auf mehrere Seiten auf?
    				ArrayList<ArrayList<String>> pages = getAllSimpleMatches(reqinfo.getHtmlCode(), PAGES);
	    			// abzueglich der ersten Seiten, Notiz: Links kommen immer 2
                    // mal vor (oben und unten)
	    			countSubPages = (pages.size() / 2) -1;
	    			
	    			// Anzahl zusaetzlicher Seiten
	    			if (pages.isEmpty() == false && pages.size() > 1)
	    				countLinks = countSubPages; 
    			
    			}
    				
    			// Im HTML-Code nach Datei-Links suchen
    			ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), FILE_URL);
    			countLinks += links.size();
    			progress.setRange( countLinks);
    			
    			// Alle Unterseiten zu den Links hinzufuegen (werden danach
                // decryptet)
    			for (int i=0; i<countSubPages; i++) {
    				decryptedLinks.add(this.createDownloadlink(url + "?s="+(i+2)+"#down"));
    			progress.increase(1);
    			}
    			
    			// Fuer jeden gefundenen Link
    			for(int i=0; i<links.size(); i++) {
    				String currentLink = links.get(i).get(4);
        			reqinfo = getRequest(new URL(currentLink)); // Seite
                                                                // aufrufen
        			
        			String frameLink = getSimpleMatch(reqinfo.getHtmlCode(), FRAME_URL, 0);
    				URL urlFrame = new URL(frameLink);
    				
        			reqinfo = getRequest(urlFrame, null, currentLink, false);
        			
    				// hier kann schon eine Weiterleitung zum Hoster kommen
        			if (reqinfo.getLocation() != null) {
        				// Link hinzufuegen
        				decryptedLinks.add(this.createDownloadlink(reqinfo.getLocation()));
        			progress.increase(1);
        				continue; // Schleifendurchlauf vorzeitig beenden,
                                    // naechsten Link
        			}
        			

        			// URL-enkodierten Java-Script-Code dekodieren
        			String decodedHTML = null;
        			try {
        				// decodedHTML =
                        // JDUtilities.htmlDecode(reqinfo.getHtmlCode());
        				decodedHTML = URLDecoder.decode(reqinfo.getHtmlCode(), "UTF-8");
        			} catch (Exception Exc) { }
        			
        			// nur wenn decoden ok ist, mache weiter,
        			// ansonsten springe einen Schritt weiter
        			if (decodedHTML != null) {
	        			// encrypteten String auslesen
	        			String encCode = getSimpleMatch(decodedHTML, ENCRYPTED_STRING, 1);
	        			String decCode = encCode.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");

	        			// erste JS-Funktion aufrufen
	        			String b = firstJS(decCode);
	        			
	        			// encrypteten String auslesen
	        			String redirectURL = "";
	        			String redirectURL1 = getSimpleMatch(b, REDIRECT_URL1, 0);
	        			String redirectURL2 = getSimpleMatch(b, REDIRECT_URL2, 0);
	        			String redirectURL3 = getSimpleMatch(b, REDIRECT_URL3, 0);
	        			
	        			if (redirectURL1 != null)
	        				redirectURL = redirectURL1;
	        			
	        			if (redirectURL2 != null)
	        				redirectURL = redirectURL2;
	        			
	        			if (redirectURL3 != null)
	        				redirectURL = redirectURL3;
	        			
	        			reqinfo = getRequest(new URL(redirectURL), null, frameLink, false); // Seite
                                                                                            // aufrufen
        			}
        			
        			// encrypteten String auslesen (Nur bei RS encrypted ???)
        			String encCode2 = getSimpleMatch(reqinfo.getHtmlCode(), ENCRYPTED_STRING2, 3);
        			String encCode3 = getSimpleMatch(reqinfo.getHtmlCode(), ENCRYPTED_STRING3, 3);
        			
        			String newLink = "";
        			
        			// Links sind nur bei RapidShare encrypted ???
        			if (encCode2 != null && encCode3 != null) {
        				String test = d(encCode2, encCode3);
        				// RS-Link suchen
	        			newLink = "http://"+getSimpleMatch(test, RS_LINK, 0);
        				
        			} else {
        				newLink = reqinfo.getLocation();
        			}
        			
    				decryptedLinks.add(this.createDownloadlink(newLink));
    			progress.increase(1);
    			}
    			
    			// Decrypt abschliessen
    			
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    /*
     * Funktionen aus dem JS
     */

    private String firstJS(String decCode) {
        // decrypt
        String c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 /=";
        int j = 0;
        int g, h, k, l, d, e, f;
        String b = "";
        do {
            g = c.indexOf(decCode.charAt(j++));
            h = c.indexOf(decCode.charAt(j++));
            k = c.indexOf(decCode.charAt(j++));
            l = c.indexOf(decCode.charAt(j++));
            d = (g << 2) | (h >> 4);
            e = ((h & 15) << 4) | (k >> 2);
            f = ((k & 3) << 6) | l;
            b = b + (char) d;

            if (k != 64) {
                b = b + (char) e;
            }

            if (l != 64) {
                b = b + (char) f;
            }

            d = 0;
            e = 0;
            f = 0;
            g = 0;
            h = 0;
            k = 0;
            l = 0;
        }
        while (j < decCode.length());

        return b;
    }

    private String d(String t, String c) {
        // init
        String b64s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char[] b64 = new char[128];
        char[] f64 = new char[128];

        for (int i = 0; i < b64s.length(); i++) {
            b64[i] = b64s.charAt(i);
            f64[b64s.charAt(i)] = (char) i;
        }

        // function d
        char[] d = new char[t.length()];
        t = t.replaceAll("\n|\r", "");
        t = t.replaceAll("=", "");
        int i = 0;
        int k = 0;

        while ((i + 3) < t.length()) {
            d[k] = (char) ((f64[t.charAt(i)] << 2) | (f64[t.charAt(i + 1)] >> 4));
            d[k + 1] = (char) (((f64[t.charAt(i + 1)] & 15) << 4) | (f64[t.charAt(i + 2)] >> 2));
            d[k + 2] = (char) (((f64[t.charAt(i + 2)] & 3) << 6) | (f64[t.charAt(i + 3)]));
            i += 4;
            k += 3;
        }

        String fin = "";
        for (int j = 0; j < d.length; j++) {
            fin += (char) d[j];
        }

        if (t.length() % 4 == 2) fin = fin.substring(0, d.length - 2);
        if (t.length() % 4 == 3) fin = fin.substring(0, d.length - 1);

        // function e
        String r = "";
        int l = 0;
        while (l < fin.length()) {
            if (fin.charAt(l) < 128) {
                r += (char) fin.charAt(l);
                l++;
            }
            else if ((fin.charAt(l) > 191) && (fin.charAt(l) < 224)) {
                r += (char) (((fin.charAt(l) & 31) << 6) | (fin.charAt(l + 1) & 63));
                l += 2;
            }
            else {
                r += (char) (((fin.charAt(l) & 15) << 12) | ((fin.charAt(l + 1) & 63) << 6) | (fin.charAt(l + 2) & 63));
                l += 3;
            }
        }

        r = a(r, c);

        r = b(r, "|||||-", ">");
        r = b(r, "||||-", "<");
        r = b(r, "|||-", "\\/\\/");
        r = b(r, "||-", "\"");
        r = b(r, "|-", "'");

        return r;
    }

    private String b(String ab, String ac, String ad) {
        String ae = "" + ab;
        while (ae.indexOf(ac) > -1) {
            int af = ae.indexOf(ac);
            ae = "" + (ae.substring(0, af) + ad + ae.substring((af + ac.length()), ae.length()));
        }
        return ae;
    }

    private String a(String b, String c) {
        String d = "";
        String x = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        c = c.toUpperCase();
        int e = c.length();
        int f;
        String g = "";

        for (f = 0; f < e; f++) {
            d = x;
            int h = d.indexOf(c.charAt(f));
            if (h < 0) continue;
            g += d.charAt(h);
        }

        c = g;
        e = c.length();
        int i = b.length();
        String j = "";
        int k = 0;
        boolean l = false;

        for (f = 0; f < i; f++) {
            char m = b.charAt(f);
            if (m == '<')
                l = true;
            else if (m == '>') l = false;
            if (l) {
                j += m;
                continue;
            }
            int n = d.indexOf(m);
            if (n < 0) {
                j += m;
                continue;
            }
            boolean o = n >= 26 ? true : false;
            n -= d.indexOf(c.charAt(k));
            n += 26;
            if (o)
                n = n % 26 + 26;
            else
                n %= 26;
            j += d.charAt(n);
            k = (k + 1) % e;
        }

        return j;
    }
}
