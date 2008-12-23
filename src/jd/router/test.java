package jd.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.utils.JDUtilities;

import jd.http.Browser;

public class test {
    @SuppressWarnings("unchecked")
    public static Map revSortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getKey()).compareTo(((Map.Entry) (o2)).getKey());
            }
        });
        // logger.info(list);
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        System.out.println("Routerinformationen werden gesammelt");
        RInfo infos = RouterInfoCollector.getRInfo();
        System.out.println("Es wurden "+(System.currentTimeMillis()-time)+" Millisekunden benötigt zum sammeln der Routerinformationen");
        Browser br = new Browser();
        HashMap<String, String> he = new HashMap<String, String>();
        he.put("RouterHost", infos.getRouterHost());
        he.put("RouterMAC", infos.getRouterMAC());
        he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
        he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
        he.put("HTMLTagCount", "" + infos.countHtmlTags());
        try {
            time = System.currentTimeMillis();
            System.out.println("Lade passende Routerdaten");
            String st = br.postPage( "http://service.jdownloader.net/routerdb/getRouters.php", he);
            System.out.println("Es wurden "+(System.currentTimeMillis()-time)+" Millisekunden benötigt zum laden der Routerdatens");
//            String st = br.postPage("http://localhost/router/getRouters.php", he);
            time = System.currentTimeMillis();
            System.out.println("Verarbeite Routerdaten");
            ArrayList<RInfo> ra = (ArrayList<RInfo>) JDUtilities.xmlStringToObjekt(st);
            System.out.println("Es wurden "+(System.currentTimeMillis()-time)+" Millisekunden benötigt zum verarbeiten der Routerdatens");
            HashMap<Integer, RInfo> routers = new HashMap<Integer, RInfo>();
            HashMap<Integer, RInfo> upnpRouters = new HashMap<Integer, RInfo>();
            time = System.currentTimeMillis();
            System.out.println("Vergleiche Routerdaten");
            for (RInfo info : ra) {
                Integer b = info.compare(infos);
                if (info.getReconnectMethode() != null && info.getReconnectMethode().contains("SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination")) {
                    upnpRouters.put(b, info);
                } else
                    routers.put(b, info);
            }
            upnpRouters = (HashMap<Integer, RInfo>) revSortByValue(upnpRouters);
            routers = (HashMap<Integer, RInfo>) revSortByValue(routers);
            System.out.println("Es wurden "+(System.currentTimeMillis()-time)+" Millisekunden benötigt zum vergleich der Routerdatens");
            System.out.println("Upnp Router ------------------------------------");
            for (Entry<Integer, RInfo> rfo : upnpRouters.entrySet()) {
                System.out.println(rfo.getKey() + ":" + rfo.getValue().getRouterName());
                System.out.println(rfo.getValue().getReconnectMethode());
                System.out.println("-------------");
            }
            System.out.println("Upnp Router end ------------------------------------");
            for (Entry<Integer, RInfo> rfo : routers.entrySet()) {
                System.out.println(rfo.getKey() + ":" + rfo.getValue().getRouterName());
                System.out.println(rfo.getValue().getReconnectMethode());
                System.out.println("-------------");
            }
            
            System.out.println("Es wurden "+ra.size()+" Router gefunden");
            System.out.println("Es wurden "+((double)br.getRequest().getContentLength())/(double)1024+" kb übertragen");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
