package jd.dynamics;

import jd.controlling.DynamicPluginInterface;

public class Seeking extends DynamicPluginInterface {

    @Override
    public void execute() {
        //
        // try {
        // Thread.sleep(20000);
        // } catch (final InterruptedException e1) {
        // e1.printStackTrace();
        // }
        // final String id = "1";
        // if (SubConfiguration.getConfig("seeking").getGenericProperty(id,
        // false) && JDInitFlags.SWITCH_DEBUG == false) {
        // // Schon durchgefuehrt
        // return;
        // }
        // SubConfiguration.getConfig("seeking").setProperty(id, true);
        //
        // if
        // (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN,
        // "Seeking for web developer",
        // "We are seeking talented front-end web developers to join our team.\r\nWe are planing a new Web Frontent Interface. Thus, we need people with experience \r\nand strong knowledge of HTML, XHTML, CSS, JavaScript, Ajax, JQuery, JSON(P).\r\n\r\nGreetings, your JD-Team",
        // null, "Read more", "Cancel"))) {
        // try {
        // LocalBrowser.openDefaultURL(new
        // URL("http://jdownloader.org/news/blog/x20101111-121030looking-for-web-developer"));
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        // }
        //
        // SubConfiguration.getConfig("seeking").save();
    }

}
