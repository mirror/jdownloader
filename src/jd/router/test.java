package jd.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import jd.utils.JDUtilities;

import jd.http.Browser;

public class test {
    @SuppressWarnings("unchecked")
    public static Map sortByIntegrety(Map<RInfo, Integer> map) {
        LinkedList list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (((Comparable) ((Map.Entry<RInfo, Integer>) (o1)).getValue()).equals(((Map.Entry<RInfo, Integer>) (o2)).getValue())) {
                    return ((Comparable) ((Map.Entry<RInfo, Integer>) (o2)).getKey().getIntegrety()).compareTo(((Map.Entry<RInfo, Integer>) (o1)).getKey().getIntegrety());
                } else
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        LinkedHashMap result = new LinkedHashMap();
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

            HashMap<RInfo, Integer> routers = new HashMap<RInfo, Integer>();
            HashMap<RInfo, Integer> experimentalRouters = new HashMap<RInfo, Integer>();
            int upnp = 0;
            ArrayList<RInfo> ra = getPossibleRinfos(infos);
            for (RInfo info : ra) {

                if (info.getReconnectMethode() != null && info.getReconnectMethode().toLowerCase().contains("schemas-upnp-org:service:wanipconnection:1#forcetermination")) {
                    upnp++;
                } else {
                    if (info.isHaveUpnpReconnect()) upnp++;
                    

                    if (info.getReconnectMethodeClr() != null) {
                        Integer b = info.compare(infos);
                        info.setIntegrety(200);
                        routers.put(info, b);
                    }
                    else if (info.getReconnectMethode()!=null)
                    {
                    Integer b = info.compare(infos);
                    // System.out.println(info.getRouterName());
                    if (info.getIntegrety() > 3) {
                        routers.put(info, b);
                    } else {
                        experimentalRouters.put(info, b);
                    }
                    }
                }
            }
            
            routers = (HashMap<RInfo, Integer>) sortByIntegrety(routers);
            HashMap<String, RInfo> methodes = new HashMap<String, RInfo>();
            Iterator<Entry<RInfo, Integer>> inter = routers.entrySet().iterator();
            while (inter.hasNext()) {
                Map.Entry<jd.router.RInfo, java.lang.Integer> entry = (Map.Entry<jd.router.RInfo, java.lang.Integer>) inter.next();
                RInfo meth = methodes.get(entry.getKey().getReconnectMethode());
                if(meth!=null)
                {
                    meth.setIntegrety(meth.getIntegrety()+entry.getKey().getIntegrety());
                    inter.remove();
                }
                else
                    methodes.put(entry.getKey().getReconnectMethode(), entry.getKey());
            }
            routers = (HashMap<RInfo, Integer>) sortByIntegrety(routers);
            experimentalRouters = (HashMap<RInfo, Integer>) sortByIntegrety(experimentalRouters);
            methodes = new HashMap<String, RInfo>();
            inter = experimentalRouters.entrySet().iterator();
            while (inter.hasNext()) {
                Map.Entry<jd.router.RInfo, java.lang.Integer> entry = (Map.Entry<jd.router.RInfo, java.lang.Integer>) inter.next();
                RInfo meth = methodes.get(entry.getKey().getReconnectMethode());
                if(meth!=null)
                {
                    meth.setIntegrety(meth.getIntegrety()+entry.getKey().getIntegrety());
                    inter.remove();
                }
                else
                    methodes.put(entry.getKey().getReconnectMethode(), entry.getKey());
            }
            experimentalRouters = (HashMap<RInfo, Integer>) sortByIntegrety(experimentalRouters);
            System.out.println(upnp + " Upnp Router ------------------------------------");
            System.out.println(routers.size() + " normale Router ------------------------------------");
            for (Entry<RInfo, Integer> rfo : routers.entrySet()) {
                System.out.println("Routervergleichswert:" + rfo.getValue() + " Integrität:" + rfo.getKey().getIntegrety() + ":" + rfo.getKey().getRouterName());
                System.out.println(rfo.getKey().getReconnectMethode());
                System.out.println("-------------------------------------------");
            }
            System.out.println(experimentalRouters.size() + " experimentelle Router  ------------------------------------");
            for (Entry<RInfo, Integer> rfo : experimentalRouters.entrySet()) {
                System.out.println("Routervergleichswert:" + rfo.getValue() + " Integrität:" + rfo.getKey().getIntegrety() + ":" + rfo.getKey().getRouterName());
                System.out.println(rfo.getKey().getReconnectMethode());
                System.out.println("-------------------------------------------");
            }
            System.out.println("Router gefunden in " + (System.currentTimeMillis() - time2) + " Millisekunden");
            System.out.println("Es wurden " + ra.size() + " Router gefunden");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
