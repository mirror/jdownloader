package jd.plugins.optional.routerdbeditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class RouterList {

    
    private ArrayList<Router> router = new ArrayList<Router>();
    public static final Object LOCK = new Object();

    public RouterList() {
        super();
    }

    public ArrayList<Router> getRouter() {
        return router;
    }
    
    public Router getRouterdata(int i)
    {
        return router.get(i);
    }
    
    public void delRouter(Router data)
    {
        router.remove(data);
    }
    public void addRouter(Router data)
    {
        router.add(data);
    }
    public void cleanRouter()
    {
        router.clear();
    }

    public void loadrouter(Vector<?> loadObject) {
        router.clear();
        Iterator<?> itr = loadObject.iterator();
        
        while(itr.hasNext())
        {
           String[] data = (String[]) itr.next();
           router.add(new Router(data[0], data[1], data[4], data[5], data[3], data[2]));
        }
    }
    public Vector<String[]> prepareToSave()
    {
        Vector<String[]> v = new Vector<String[]>();
        Iterator<Router> itr = router.iterator();
        while (itr.hasNext()) {
          Router temp = itr.next();
          String[] temps = {temp.getHersteller(),temp.getName(),temp.getScript(),temp.getRegex(),temp.getUsername(),temp.getPass()};
          v.add(temps);
        }
        return v;
        
    }
}
