//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

    public Router getRouterdata(int i) {
        return router.get(i);
    }

    public void delRouter(Router data) {
        router.remove(data);
    }

    public void addRouter(Router data) {
        router.add(data);
    }

    public void cleanRouter() {
        router.clear();
    }

    public void loadRouter(Vector<?> loadObject) {
        router.clear();
        Iterator<?> itr = loadObject.iterator();

        while (itr.hasNext()) {
            String[] data = (String[]) itr.next();
            router.add(new Router(data[0], data[1], data[4], data[5], data[3], data[2]));
        }
    }

    public Vector<String[]> prepareToSave() {
        Vector<String[]> v = new Vector<String[]>();
        Iterator<Router> itr = router.iterator();
        while (itr.hasNext()) {
            Router temp = itr.next();
            String[] temps = { temp.getHersteller(), temp.getName(), temp.getScript(), temp.getRegex(), temp.getUsername(), temp.getPass() };
            v.add(temps);
        }
        return v;

    }
}
