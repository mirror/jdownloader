package org.jdownloader.captcha.v2.challenge.cloudflareturnstile;

import org.appwork.utils.Regex;

import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.Plugin;

/** https://www.cloudflare.com/products/turnstile/ */
public abstract class AbstractCloudflareTurnstileCaptcha<T extends Plugin> {
    public static boolean containsCloudflareTurnstileClass(final Browser br) {
        return br != null && containsCloudflareTurnstileClass(br.toString());
    }

    public static boolean containsCloudflareTurnstileClass(final String string) {
        return string != null && (new Regex(string, "challenges\\.cloudflare\\.com/turnstile/").patternFind() || new Regex(string, "class=\"cf-turnstile\"").patternFind());
    }

    public static boolean containsCloudflareTurnstileClass(final Form form) {
        return form != null && containsCloudflareTurnstileClass(form.getHtmlCode());
    }

    public AbstractCloudflareTurnstileCaptcha(final T plugin) {
    }
}
