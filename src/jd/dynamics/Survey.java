package jd.dynamics;

import java.net.MalformedURLException;
import java.net.URL;

import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.gui.UserIO;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.utils.locale.JDL;

public class Survey extends DynamicPluginInterface {

    @Override
    public void execute() {
//        JDController.getInstance().addControlListener(new ControlListener() {

//            public void controlEvent(ControlEvent event) {
//                if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE) {
                    String id = "4";
                    if (SubConfiguration.getConfig("survey").getGenericProperty(id, false)) {
                        // Schon durchgeführt
                        return;
                    }
                    SubConfiguration.getConfig("survey").setProperty(id, true);
                    if (JDL.isGerman()) {
                        UserIO.getInstance().requestMessageDialog(UserIO.NO_COUNTDOWN,"Umfrage", "Um JDownloader noch besser zu machen, führen wir eine anonyme Umfrage zur Nutzerzufriedenheit durch. Es wäre sehr schön wenn du dir einige Minuten Zeit nehmen könntest.\r\nDein JD-Team\r\n\r\n");
                    } else {
                        UserIO.getInstance().requestMessageDialog(UserIO.NO_COUNTDOWN,"Survey", "To make JDownloader event better, we do a Survey about user satisfaction. We would be very happy if you could spend a few minutes.\r\nGreetings, your JD-Team");
                    }
                    try {
                        LocalBrowser.openDefaultURL(new URL("http://jdownloader.org/survey"));
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

//                }
//            }

//        });
    }

}
