package org.jdownloader.api.jdanywhere.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jdownloader.api.jdanywhere.api.interfaces.IEventsApi;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class EventsAPI implements IEventsApi {

    public EventsAPI() {

    }

    static Map<String, Long> captchaPushList = new HashMap<String, Long>();

    public boolean RegisterCaptchaPush(String host, String path, String query) {
        URI uri = null;
        try {
            uri = new URI("http", host, path, query, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String request = uri.toASCIIString();
        if (!captchaPushList.containsKey(request)) {
            captchaPushList.put(request, (long) 0);
        }
        return true;
    }

    public void sendNewCaptcha(final SolverJob<?> job) {
        new Thread() {
            public void run() {
                long startTime = System.currentTimeMillis();
                long captchCount = 0;
                for (SolverJob<?> entry : ChallengeResponseController.getInstance().listJobs()) {
                    if (entry.isDone()) continue;
                    if (entry.getChallenge() instanceof ImageCaptchaChallenge) {
                        captchCount++;
                    }
                }
                for (Map.Entry<String, Long> entry : captchaPushList.entrySet()) {
                    if (startTime - entry.getValue() > 5 * 60 * 1000) {
                        try {
                            String request = entry.getKey();
                            ImageCaptchaChallenge<?> challenge = (ImageCaptchaChallenge<?>) job.getChallenge();
                            Long count = new Long(captchCount);
                            request = request.replace("%7BCaptchaCount%7D", count.toString());
                            getHTML(request);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        captchaPushList.put(entry.getKey(), startTime);
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
