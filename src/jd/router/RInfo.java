package jd.router;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import jd.nutils.JDHash;

import jd.nutils.io.JDIO;

import jd.utils.EditDistance;



import jd.utils.JDUtilities;

public class RInfo implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = -2119228262137830055L;

    public String getRouterIP() {
        return RouterIP;
    }

    public void setRouterIP(String routerIP) {
        RouterIP = routerIP;
    }

    public String getRouterHost() {
        return RouterHost;
    }

    public void setRouterHost(String routerHost) {
        RouterHost = routerHost;
    }

    public String getRouterMAC() {
        return RouterMAC;
    }
    public void setRouterMAC(String routerMAC) {
        if(RouterMAC==null)
        RouterMAC = routerMAC.replaceAll(" ", "0");
    }

    public String getPageHeader() {
        return PageHeader;
    }

    public void setPageHeader(String pageHeader) {
        PageHeader = SQLRouterData.replaceTimeStamps(pageHeader);
    }

    public String getRouterPage() {
        return RouterPage;
    }

    public void setRouterPage(String routerPage) {
        RouterPage = SQLRouterData.replaceTimeStamps(routerPage);
    }

    @SuppressWarnings("unchecked")
    public void setUPnPSCPDs(String pnPSCPDs) {
        if(pnPSCPDs==null)
        {
            return;
        }
        HashMap<String, String> UPnPSCPDs = null;
        try {
            if(pnPSCPDs!=null && pnPSCPDs.length()>0)
            UPnPSCPDs=(HashMap<String, String>) JDUtilities.xmlStringToObjekt(pnPSCPDs);
        } catch (Exception e) {
            // TODO: handle exception
        }

        if(UPnPSCPDs!=null)
        {
            haveUpnp=true;
            haveUpnpReconnect=SQLRouterData.haveUpnpReconnect(UPnPSCPDs);
            String[] infoupnp = SQLRouterData.getNameFormUPnPSCPDs(UPnPSCPDs);
            String name = null;
            if(infoupnp!=null)
            {
                name= infoupnp[0];
                if(infoupnp[1]!=null)
                name+=" "+infoupnp[1];
            }
            if(name!=null)
            setRouterNames(name);
            if(getRouterMAC()==null|getRouterMAC().length()==0)
            {
                RouterMAC=infoupnp[2].replaceAll(" ", "0");
            }
        }
    }

    public String getRouterErrorPage() {
        return RouterErrorPage;
    }

    public void setRouterErrorPage(String routerErrorPage) {
        RouterErrorPage = SQLRouterData.replaceTimeStamps(routerErrorPage);
    }

    public String getReconnectMethode() {
        return ReconnectMethode;
    }

    public void setReconnectMethode(String reconnectMethode) {
        ReconnectMethode = reconnectMethode;
    }

    public String getReconnectMethodeClr() {
        return ReconnectMethodeClr;
    }

    public void setReconnectMethodeClr(String reconnectMethodeClr) {
        ReconnectMethodeClr = reconnectMethodeClr;
    }

    public String[] getRouterNames() {
        return RouterNames;
    }
    public void setRouterNames(String[] routerNames) {
        RouterNames=routerNames;
    }
    public void setRouterNames(String routerNames) {
        if(routerNames==null || routerNames.length()==0 || routerNames.equals("Reconnect Recorder Methode") )
            setPlaceholder=true;
        if(RouterNames==null)
        {
        try {
            if(routerNames!=null && routerNames.length()>0)
            {
                if(routerNames.startsWith("<?xml version"))
                {
                RouterNames= (String[]) JDUtilities.xmlStringToObjekt(routerNames);
                if(RouterNames instanceof String[])
                    RouterNames = new String[] {routerNames};
                else
                    RouterNames = new String[] {routerNames};
                }
                else
                RouterNames = new String[] {routerNames};
            }
            else 
                RouterNames=null;
                    
        } catch (Exception e) {
            RouterNames = new String[] {routerNames};
        }
        }
    }

    public String getRouterPageLoggedIn() {
        return RouterPageLoggedIn;
    }

    public void setRouterPageLoggedIn(String routerPageLoggedIn) {
        RouterPageLoggedIn = SQLRouterData.replaceTimeStamps(routerPageLoggedIn);
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    public boolean isHaveUpnpReconnect() {
        return haveUpnpReconnect;
    }

    public void setHaveUpnpReconnect(boolean haveUpnpReconnect) {
        this.haveUpnpReconnect = haveUpnpReconnect;
    }

    public boolean isHaveUpnp() {
        return haveUpnp;
    }

    public void setHaveUpnp(boolean haveUpnp) {
        this.haveUpnp = haveUpnp;
    }
    private int id;
    private String[] RouterNames = null;
    private boolean haveUpnpReconnect = false;
    private boolean haveUpnp = false;
    public transient boolean setPlaceholder = false;
    private String RouterIP, RouterHost, RouterMAC = null, PageHeader, RouterPage, 
    RouterErrorPage, ReconnectMethode, ReconnectMethodeClr, RouterPageLoggedIn;
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RInfo)
        {
            RInfo ob = (RInfo) obj;
            if(RouterIP.equals(ob.RouterIP) && RouterHost.equals(ob.RouterHost) && (ob.RouterMAC==null || RouterMAC==null || RouterMAC.equals(ob.RouterMAC)) && ReconnectMethode.equals(ReconnectMethode) && ReconnectMethodeClr.equals(ReconnectMethodeClr) )
            {
                if(EditDistance.getLevenshteinDifference(PageHeader, ob.PageHeader)<5 && EditDistance.getLevenshteinDifference(RouterErrorPage, ob.RouterErrorPage)<10 && EditDistance.getLevenshteinDifference(RouterPage, ob.RouterPage)<10 && EditDistance.getLevenshteinDifference(RouterPageLoggedIn, ob.RouterPageLoggedIn)<10)
                {
                    if(RouterMAC==null)
                    {
                        RouterMAC=ob.RouterMAC;
                    }
                    else if(ob.RouterMAC==null)
                    {
                        ob.RouterMAC=RouterMAC;
                    }
                    if(RouterNames==null)
                    {
                        if(ob.RouterNames!=null)
                            RouterNames=ob.RouterNames;
                    }
                    else if(ob.RouterNames==null)
                    {
                        ob.RouterNames=RouterNames;
                    }
                    if(RouterNames !=null && ob.RouterNames !=null)
                    {
                        if(ob.RouterNames.length>1 && RouterNames.length==1)
                            ob.RouterNames=RouterNames;
                        else if(RouterNames.length>1 && ob.RouterNames.length==1)
                            RouterNames=ob.RouterNames;
                    }
                    if(haveUpnp && !ob.haveUpnp)
                    {
                        ob.haveUpnp=true;
                        ob.haveUpnpReconnect=haveUpnpReconnect;
                    }
                    else if(ob.haveUpnp && !haveUpnp)
                    {
                        haveUpnp=true;
                        haveUpnpReconnect=ob.haveUpnpReconnect;
                    }
                    return true;
                }
                    
                    
            }
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    public void save() {
        String md5 = "";
        if(ReconnectMethode!=null && ReconnectMethode.length()>0)
        {
            md5=JDHash.getMD5(ReconnectMethode);
        }
        else
        {
            md5="clr_"+JDHash.getMD5(ReconnectMethode);
        }
        File file = new File("/home/dwd/www/router/rd/test",md5+".xml");
        ArrayList<RInfo> infos = new ArrayList<RInfo>();
        infos.add(this);

        if(file.exists())
        {
            infos =(ArrayList<RInfo>)JDIO.loadObject(SQLRouterData.frame, file, true);
            if(!infos.contains(this))
                infos.add(this);
            file.delete();
        }
        JDIO.saveObject(SQLRouterData.frame, infos, file, md5+".xml", "xml", true);

    }
}
