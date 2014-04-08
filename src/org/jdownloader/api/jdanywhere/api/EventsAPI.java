package org.jdownloader.api.jdanywhere.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.api.jdanywhere.api.interfaces.IEventsApi;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class EventsAPI implements IEventsApi {

    public EventsAPI() {
        if (captchaPushList == null) captchaPushList = new HashMap<String, CaptchaPushRegistration>();
    }

    static JDAnywhereConfig                     cfg             = JsonConfig.create(JDAnywhereConfig.class);
    static Map<String, CaptchaPushRegistration> captchaPushList = cfg.getList();

    public boolean RegisterCaptchaPush(String host, String path, String query) {

        return RegisterCaptchaPush_v2(query, host, path, query, false);
    }

    public boolean RegisterCaptchaPush_v2(String deviceID, String host, String path, String query, boolean withSound) {
        URI uri = null;
        try {
            uri = new URI("http", host, path, query, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
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
            if (captchaPushList.containsKey(deviceID)) {
                captchaPushList.remove(deviceID);
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
                                ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
                                Long count = new Long(captchCount);
                                request = request.replace("%7BCaptchaCount%7D", count.toString());
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
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToRead);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
