package org.jdownloader.captcha.v2.challenge.areyouahuman;

import java.io.IOException;
import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;

public abstract class AreYouAHumanChallenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    public AreYouAHumanChallenge(String siteKey, Plugin pluginForHost) {
        super("areyouahuman", pluginForHost);
        this.siteKey = siteKey;
        if (siteKey == null || !siteKey.matches("^[a-f0-9]{40}$")) {
            throw new WTFException("Bad SiteKey");
        }

    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = AreYouAHumanChallenge.class.getResource("areyouahuman.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }
}
