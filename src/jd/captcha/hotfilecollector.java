package jd.captcha;

import java.io.File;
import java.io.IOException;

import jd.controlling.JDLogger;
import jd.http.Browser;

import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class hotfilecollector {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        final String dir = "/home/dwd/.jd_home/captchas/hotfile.com/", link = "http://hotfile.com/dl/5875/821d4ea/Gora_20czarownic.up.by.DNH.part4.rar.html";
        for (int d = 0; d < 20000000; d++) {
                    Browser br = new Browser();
                    
                    try {
                        br.getPage(link);
                        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                        if (br.containsHTML("starthtimer\\(\\)")) {
                            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
                            if (Long.parseLong(waittime.trim()) > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim())); }
                        }

                        int i = 0;
                        Form form = new Form();
                        while(true) {
                            Boolean error = false;
                            try {
                                Form[] forms = br.getForms();
                                form = forms[1];
                            } catch(ArrayIndexOutOfBoundsException e) {
                                error = true;
                            }

                            if(!error){
                                break;
                            }else if(++i == 3){
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                            }
                        }

                        Thread.sleep(30000l);
                        br.submitForm(form);

                        // captcha
                        if (!br.containsHTML("Click here to download")) {
                            form = br.getForm(1);
                            String captchaUrl = "http://www.hotfile.com" + br.getRegex("<img src=\"(/captcha.php.*?)\">").getMatch(0);
                            File f = new File(dir + System.currentTimeMillis()+ ".jpg");
                            br.getDownload(f, captchaUrl);
                            System.out.println(f);
                        }
                        System.out.println(d);
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
        }

	}

}
