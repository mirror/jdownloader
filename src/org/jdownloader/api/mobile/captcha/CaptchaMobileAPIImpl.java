package org.jdownloader.api.mobile.captcha;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import jd.controlling.IOPermission;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaEventListener;
import jd.controlling.captcha.CaptchaEventSender;
import jd.controlling.captcha.CaptchaResult;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.api.captcha.CaptchaJob;

public class CaptchaMobileAPIImpl implements CaptchaMobileAPI, CaptchaEventListener {

    CaptchaAPIImpl cpAPI = new CaptchaAPIImpl();

    public CaptchaMobileAPIImpl() {
        CaptchaEventSender.getInstance().addListener(this);
    }

    public List<CaptchaJob> list() {
        return cpAPI.list();
    }

    public void get(RemoteAPIRequest request, RemoteAPIResponse response, long id, final boolean returnAsDataURL) {
        cpAPI.get(request, response, id, returnAsDataURL);
    }

    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) {
        cpAPI.get(request, response, id, false);
    }

    public boolean solve(long id, CaptchaResult result) {
        return cpAPI.solve(id, result);
    }

    public boolean abort(long id, IOPermission.CAPTCHA what) {
        return cpAPI.abort(id, what);
    }

    public void captchaTodo(CaptchaController controller) {
        cpAPI.captchaTodo(controller);
        sendNewCaptcha(controller);
    }

    public void captchaFinish(CaptchaController controller) {
        cpAPI.captchaFinish(controller);
    }

    private void sendNewCaptcha(CaptchaController controller) {
        new Thread() {
            public void run() {
                try {
                    java.lang.String contentToPost = "{\"DeviceToken\": \"367B0809EA6A3CC65A240026D9970B0DFD8CD12F006A6A9A72D12E492DB4EB2C\",\"Badge\": \"" + CaptchaDialogQueue.getInstance().getJobs().size() + 1 + "\",\"Message\": \"New Captcha arrived\"}";
                    String request = "http://jdanywherenotification.cloudapp.net/api/notification/notification";
                    java.net.URLConnection connection = new java.net.URL(request).openConnection();
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Length", "" + contentToPost.length());
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    java.io.OutputStream stream = connection.getOutputStream();
                    stream.write(contentToPost.getBytes());
                    stream.close();

                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    java.lang.StringBuffer sb = new java.lang.StringBuffer();
                    java.lang.String str = br.readLine();
                    while (str != null) {
                        sb.append(str);
                        str = br.readLine();
                    }
                    br.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
