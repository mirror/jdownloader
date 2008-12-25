package jd.router;

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

    @SuppressWarnings("unchecked")
    public static ArrayList<RInfo> getPossibleRinfos(RInfo infos) {
        Browser br = new Browser();
        HashMap<String, String> he = new HashMap<String, String>();
        he.put("RouterHost", infos.getRouterHost());
        he.put("RouterMAC", infos.getRouterMAC());
        he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
        he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
        he.put("HTMLTagCount", "" + infos.countHtmlTags());
        try {
            String st = br.postPage("http://service.jdownloader.net/routerdb/getRouters.php", he);
            // String st = br.postPage("http://localhost/router/getRouters.php",
            // he);
            System.out.println("Es wurden " + ((double) br.getRequest().getContentLength()) / (double) 1024 + " kb übertragen");
            return (ArrayList<RInfo>) JDUtilities.xmlStringToObjekt(st);

        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        long time2 = System.currentTimeMillis();
        RInfo infos = RouterInfoCollector.getRInfo(RouterInfoCollector.RInfo_ROUTERSEARCH);
        try {

            HashMap<Integer, RInfo> routers = new HashMap<Integer, RInfo>();
            HashMap<Integer, RInfo> experimentalRouters = new HashMap<Integer, RInfo>();
            int upnp = 0;
            ArrayList<RInfo> ra = getPossibleRinfos(infos);
            int integ = 0;
            int c = 0;
            int diff = 0;
            for (RInfo info : ra) {

                if (info.getReconnectMethode() != null && info.getReconnectMethode().contains("SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination")) {
                    upnp++;
                } else {
                    Integer b = info.compare(infos);
                    if (info.getIntegrety() > 1) {
                        routers.put(b, info);
                        diff += b;
                        integ += info.getIntegrety();
                        c++;
                    } else {
                        experimentalRouters.put(b, info);
                    }
                }
            }
            HashMap<Integer, RInfo> routers2;
            if (diff != 0 && integ != 0 && c != 0) {
                double d = ((double) 100 / (double) diff) * ((double) integ) / 100;
                routers2 = new HashMap<Integer, RInfo>(routers.size());
                for (Entry<Integer, RInfo> info : routers.entrySet()) {
                    
                    routers2.put((int) (info.getKey() * d), info.getValue());
                }
            } else
                routers2 = routers;
            routers = (HashMap<Integer, RInfo>) revSortByValue(routers2);
            experimentalRouters = (HashMap<Integer, RInfo>) revSortByValue(experimentalRouters);
            System.out.println(upnp+" Upnp Router ------------------------------------");
            for (Entry<Integer, RInfo> rfo : routers.entrySet()) {
                System.out.println(rfo.getKey() + ":"+rfo.getValue().getIntegrety()+":" + rfo.getValue().getRouterName());
                System.out.println(rfo.getValue().getReconnectMethode());
                System.out.println("-------------");
            }
            System.out.println("experimentalRouters ------------------------------------");
            for (Entry<Integer, RInfo> rfo : experimentalRouters.entrySet()) {
                System.out.println(rfo.getKey() + ":"+rfo.getValue().getIntegrety()+":" + rfo.getValue().getRouterName());
                System.out.println(rfo.getValue().getReconnectMethode());
                System.out.println("-------------");
            }
            System.out.println("Router gefunden in " + (System.currentTimeMillis() - time2));
            System.out.println("Es wurden " + ra.size() + " Router gefunden");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
