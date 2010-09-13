package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.Browser;

public class BalancedWebIPCheck implements IPCheckProvider {
    public static BalancedWebIPCheck getInstance() {
        return BalancedWebIPCheck.INSTANCE;
    }

    private final ArrayList<String>         services;

    private final Browser                   br;

    private int                             index    = 0;

    private final Pattern                   pattern;

    private static final BalancedWebIPCheck INSTANCE = new BalancedWebIPCheck();

    private BalancedWebIPCheck() {

        this.services = new ArrayList<String>();

        this.services.add("http://ipcheck3.jdownloader.org");
        this.services.add("http://ipcheck2.jdownloader.org");
        this.services.add("http://ipcheck1.jdownloader.org");
        this.services.add("http://ipcheck0.jdownloader.org");
        this.pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Collections.shuffle(this.services);
        this.br = new Browser();
        this.br.setConnectTimeout(15000);
        this.br.setReadTimeout(15000);
    }

    public String getInfo() {
        return null;
    }

    public IP getIP() throws IPCheckException {
        try {
            Exception e = null;
            for (int i = 0; i < this.services.size(); i++) {
                try {
                    this.index = (this.index + 1) % this.services.size();
                    final String service = this.services.get(this.index);
                    /* call website and check for ip */
                    final Matcher matcher = this.pattern.matcher(this.br.getPage(service));
                    if (matcher.find()) {
                        if (matcher.groupCount() > 0) {

                        return IP.getInstance(matcher.group(1)); }
                    }
                } catch (final Exception e2) {
                    e = e2;
                }

            }

            if (e != null) {
                throw e;
            } else {
                throw new IPCheckException("Could not get IP");
            }
        } catch (final Exception e) {
            throw new IPCheckException(e);
        }
    }

    public int getIpCheckInterval() {
        return 0;
    }

}
