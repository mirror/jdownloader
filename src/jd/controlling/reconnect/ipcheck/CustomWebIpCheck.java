package jd.controlling.reconnect.ipcheck;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.http.Browser;

/**
 * Allows the user to define his own ip check rules.
 * 
 * @author thomas
 * 
 */
public class CustomWebIpCheck implements IPCheckProvider {

    private final Browser                 br       = new Browser();

    private static final CustomWebIpCheck INSTANCE = new CustomWebIpCheck();

    public static CustomWebIpCheck getInstance() {
        return CustomWebIpCheck.INSTANCE;
    }

    private CustomWebIpCheck() {

    }

    /**
     * gets the external IP.
     * 
     * @throws IPCheckException
     *             if there is no valid external IP
     */
    public IP getExternalIP() throws IPCheckException {

        final String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "Please enter Website for IPCheck here");
        final String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "Please enter Regex for IPCheck here");

        try {
            /* check for valid website */
            new URL(site);
            /* call website and check for ip */
            this.br.setConnectTimeout(15000);
            this.br.setReadTimeout(15000);
            final Matcher matcher = Pattern.compile(patt, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(this.br.getPage(site));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) {

                return IP.getInstance(matcher.group(1)); }
            }

        } catch (final Exception e) {
            throw new IPCheckException(e);
        }
        throw new IPCheckException("NO IP found");

    }

    public int getIpCheckInterval() {
        return 3;
    }
}