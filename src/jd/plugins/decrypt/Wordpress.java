package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Wordpress extends PluginForDecrypt {
    static private final String host = "Wordpress Parser";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://.*?(movie-blog.org/\\d{4}/\\d{2}/\\d{2}/.+|hoerbuch.in/blog.php\\?id=[\\d]+|doku.cc/\\d{4}/\\d{2}/\\d{2}/.+|xxx-blog.org/blog.php\\?id=[\\d]+|sky-porn.info/blog/\\?p=[\\d]+|best-movies.us/\\?p=[\\d]+|game-blog.us/game-.+\\.html).*", Pattern.CASE_INSENSITIVE);
    private ArrayList<String[]> defaultpasswords = new ArrayList<String[]>();
    private Vector<String> passwordpattern = new Vector<String>();

    public Wordpress() {
        super();
        add_defaultpasswords();
        add_passwordpatterns();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    private void add_defaultpasswords() {
        /* Die defaultpasswörter der einzelnen seiten */
        /* Host, defaultpw1, defaultpw2, usw */
        defaultpasswords.add(new String[] { "doku.cc", "doku.cc", "doku.dl.am" });
        defaultpasswords.add(new String[] { "movie-blog.org", "movie-blog.org", "movie-blog.dl.am" });
        defaultpasswords.add(new String[] { "xxx-blog.org", "xxx-blog.org", "xxx-blog.dl.am" });
    }

    private void add_passwordpatterns() {
        /* diese Pattern dienen zum auffinden des Passworts */
        /* ACHTUNG: passwort muss an erster stelle im pattern sein */
        passwordpattern.add("<b>Passwort\\:<\\/b> (.*?) \\|"); /* hörbuch,xxx-blog */
        passwordpattern.add("<b>Passwort\\:<\\/b> (.*?)<\\/p>");/* game-blog */
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?) \\|");/* sky-porn */
        passwordpattern.add("<strong>Passwort<\\/strong>\\: (.*?) <strong>"); /* movie-blog */
        passwordpattern.add("<strong>Passwort\\: <\\/strong>(.*?)<\\/p>"); /* movie-blog */
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?)<\\/p>"); /* best-movies */
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?) <\\/p>"); /* best-movies */
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Wordpress Parser";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);

                /* Defaultpasswörter der Seite setzen */
                for (int j = 0; j < defaultpasswords.size(); j++) {
                    if (url.getHost().toLowerCase().contains(defaultpasswords.get(j)[0])) {
                        for (int jj = 1; jj < defaultpasswords.get(j).length; jj++) {
                            /*
                             * logger.info("defaul PW: " +
                             * defaultpasswords.get(j)[jj]);
                             */
                            default_password.add(defaultpasswords.get(j)[jj]);
                        }
                        break;
                    }
                }
                ArrayList<String> password = null;
                /* Passwort suchen */
                for (int i = 0; i < passwordpattern.size(); i++) {
                    password = getAllSimpleMatches(reqinfo, Pattern.compile(passwordpattern.get(i), Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() != 0) {
                        for (int ii = 0; ii < password.size(); ii++) {
                            /* logger.info("PW: " + password.get(ii)); */
                            default_password.add(JDUtilities.htmlDecode(password.get(ii)));
                        }
                        break;
                    }
                }
                /* Alle Parts suchen */
                ArrayList<String> links = getAllSimpleMatches(reqinfo, Pattern.compile("(<a(.*?)</a>)", Pattern.CASE_INSENSITIVE), 2);
                for (int i = 0; i < links.size(); i++) {
                    try {
                        if (links.get(i).matches(".*(Rapidshare|CCF|RSD|CCL|DLC|RSDF|Xirror|Upload|Share-Online|Netload|Bluehost|Crypt|Parts?[\\s\\d]*?).*")) {
                            String lnk = Plugin.getHttpLinks(links.get(i), parameter)[0];
                            if (!new Regexp(lnk, patternSupported).matches()) {
                                /* logger.info("ADD: " + lnk); */
                                decryptedLinks.add(this.createDownloadlink(lnk));
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}