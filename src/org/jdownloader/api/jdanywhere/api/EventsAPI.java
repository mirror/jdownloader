package org.jdownloader.api.jdanywhere.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import jd.http.Browser;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.api.jdanywhere.api.interfaces.IEventsApi;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class EventsAPI implements IEventsApi {
    
    public EventsAPI() {
    }
    
    private static final JDAnywhereConfig                     cfg             = JsonConfig.create(JDAnywhereConfig.class);
    private static final Map<String, CaptchaPushRegistration> captchaPushList = getCaptchaPushList();
    
    private static Map<String, CaptchaPushRegistration> getCaptchaPushList() {
        Map<String, CaptchaPushRegistration> captchaPushList = cfg.getList();
        if (captchaPushList == null) captchaPushList = new HashMap<String, CaptchaPushRegistration>();
        return captchaPushList;
    }
    
    public boolean RegisterCaptchaPush(String host, String path, String query) {
        String deviceID = query.substring(query.indexOf("DeviceToken="), query.indexOf("&Badge=", query.indexOf("DeviceToken=")));
        return RegisterCaptchaPush_v2(deviceID, host, path, query, false);
    }
    
    public boolean RegisterCaptchaPush_v2(String deviceID, String host, String path, String query, boolean withSound) {
        URI uri = null;
        try {
            uri = new URI("http", host, path, query, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (uri == null) return false;
        String request = uri.toASCIIString();
        CaptchaPushRegistration cpr = new CaptchaPushRegistration();
        cpr.setUrl(request);
        cpr.setWithSound(withSound);
        synchronized (captchaPushList) {
            captchaPushList.put(deviceID, cpr);
            cfg.setList(captchaPushList);
        }
        return true;
    }
    
    public boolean IsRegistered(String deviceID) {
        synchronized (captchaPushList) {
            return captchaPushList.containsKey(deviceID);
        }
    }
    
    public boolean IsSoundEnabled(String deviceID) {
        synchronized (captchaPushList) {
            if (captchaPushList.containsKey(deviceID)) {
                CaptchaPushRegistration cpr = captchaPushList.get(deviceID);
                if (cpr != null)
                    return cpr.isWithSound();
                else
                    return false;
            } else
                return false;
        }
    }
    
    public boolean UnRegisterCaptchaPush(String deviceID) {
        synchronized (captchaPushList) {
            if (captchaPushList.remove(deviceID) != null) {
                cfg.setList(captchaPushList);
            }
        }
        return true;
    }
    
    public void sendNewCaptcha(final SolverJob<?> job) {
        new Thread() {
            public void run() {
                long captchCount = 0;
                for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
                    if (entry.isDone()) continue;
                    if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                        captchCount++;
                    }
                }
                synchronized (captchaPushList) {
                    for (Map.Entry<String, CaptchaPushRegistration> entry : captchaPushList.entrySet()) {
                        try {
                            CaptchaPushRegistration cpr = entry.getValue();
                            if (cpr != null) {
                                String request = cpr.getUrl();
                                // ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
                                request = request.replace("%7BCaptchaCount%7D", Long.toString(captchCount));
                                getHTML(request);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                }
            }
        }.start();
    }
    
    public String getHTML(String urlToRead) {
        Browser br = null;
        try {
            br = new Browser();
            return br.getPage(urlToRead);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.getHttpConnection().disconnect();
            } catch (final Throwable ignore) {
            }
        }
        return null;
    }
    
}
