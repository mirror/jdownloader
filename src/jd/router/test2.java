package jd.router;

import java.io.IOException;
import java.util.HashMap;

import jd.utils.JDUtilities;

public class test2 {

    /**
     * @param args
     */
    public static void main(String[] args) {
        RInfo infos = RouterInfoCollector.getRInfo(RouterInfoCollector.RInfo_ROUTERSEARCH);
        HashMap<String, String> he = new HashMap<String, String>();
        if (infos.getRouterHost() != null) he.put("RouterHost", infos.getRouterHost());
        if (infos.getRouterHost() != null) he.put("RouterMAC", infos.getRouterMAC());
        if (infos.getPageHeader() != null) he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
        if (infos.getRouterErrorPage() != null) he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
        he.put("HTMLTagCount", "" + infos.countHtmlTags());
        try {
            System.out.println(JDUtilities.objectToXml(he));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);

    }

}
