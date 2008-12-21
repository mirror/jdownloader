package jd.router;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.utils.JDUtilities;

import jd.http.Browser;

public class test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        RInfo infos = RouterInfoCollector.getRInfo();
        Browser br = new Browser();
        HashMap<String, String> he = new HashMap<String, String>();
        he.put("RouterHost", infos.getRouterHost());
        he.put("RouterMAC", infos.getRouterMAC());
        he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
        he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
        try {
            String st = br.postPage("http://service.jdownloader.net/routerdb/getRouters.php", he);
            ArrayList<RInfo> ra = (ArrayList<RInfo>) JDUtilities.xmlStringToObjekt(st);
            RInfo best = null;
            int bestval = Integer.MAX_VALUE;
            for (RInfo info : ra) {
                int b = info.compare(infos);
                if(b<bestval)
                {
                    bestval=b;
                    best=info;
                }
            }
            if(best!=null)
            {
                System.out.println(bestval);
                System.out.println(best.getRouterName());
                System.out.println(best.getReconnectMethode());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
