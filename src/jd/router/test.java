package jd.router;

import java.io.File;
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
            HashMap<Integer, RInfo> routers = new HashMap<Integer, RInfo>();
            for (RInfo info : ra) {
                Integer b = info.compare(infos);
                routers.put(b, info);
            }
            routers=(HashMap<Integer, RInfo>) revSortByValue(routers);

             for (Entry<Integer, RInfo> rfo : routers.entrySet()) {
                System.out.println(rfo.getKey()+":"+rfo.getValue().getRouterName());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
