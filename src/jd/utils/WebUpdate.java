//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import jd.JDInit;
import jd.controlling.JDLogger;

import org.jdownloader.update.JDUpdater;

public class WebUpdate {
    private static final Logger LOG                = JDLogger.getLogger();
    private static int          waitingUpdates     = 0;
    //
    //

    private static boolean      UPDATE_IN_PROGRESS = false;

    /**
     * @param forceguiCall
     *            : Updatemeldung soll erscheinen, auch wenn user updates
     *            deaktiviert hat
     */
    public static synchronized void doUpdateCheck(final boolean forceguiCall) {
        JDUpdater.getInstance().startUpdate(!forceguiCall);
    }

    /**
     * Checks if the class (a plugin) already has been loaded)
     * 
     * @param clazz
     * @return
     */
    private static boolean classIsLoaded(String clazz) {

        java.lang.reflect.Method m;
        try {

            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });

            m.setAccessible(true);

            Object test1 = m.invoke(JDInit.getPluginClassLoader(), clazz);
            return test1 != null;

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void setWaitingUpdates(final int i) {
        waitingUpdates = Math.max(0, i);
    }

    public static int getWaitingUpdates() {
        return waitingUpdates;
    }

}
