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

package jd.router;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JTextField;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.ProgressDialog;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.gui.userio.DummyFrame;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.Threader;
import jd.nutils.Threader.WorkerListener;
import jd.nutils.jobber.JDRunnable;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class GetRouterInfo {

    private Threader threader = null;
    private Threader th2 = null;
    private boolean cancel = false;

    public void cancel() {
        cancel = true;
        if (threader != null) {
            try {
                threader.interrupt();
            } catch (Exception e) {
            }
        } else if (th2 != null) {
            try {
                th2.interrupt();
            } catch (Exception e) {
            }
        }

    }

    private Logger logger = JDLogger.getLogger();

    public String username = null;

    public String password = null;

    private ProgressDialog progressBar;

    public GetRouterInfo(ProgressDialog progress) {
        progressBar = progress;
        if (progressBar != null) {
            progressBar.setMaximum(100);
        }
    }

    public static LinkedHashMap<RInfo, Integer> sortByIntegrety(Map<RInfo, Integer> map) {
        LinkedList<Entry<RInfo, Integer>> list = new LinkedList<Entry<RInfo, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<RInfo, Integer>>() {
            public int compare(Entry<RInfo, Integer> o1, Entry<RInfo, Integer> o2) {
                if (o1.getValue().equals((o2).getValue())) {
                    return o2.getKey().compareTo(o1.getKey());
                } else
                    return o1.getValue().compareTo(o2.getValue());
            }
        });
        LinkedHashMap<RInfo, Integer> result = new LinkedHashMap<RInfo, Integer>();
        for (Entry<RInfo, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private RInfo checkRouters(HashMap<RInfo, Integer> routers) {
        int retries = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RETRIES, 5);
        int wipchange = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, 20);
        JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);
        JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, 10);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, username);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, password);
        int size = routers.size();
        int i = 0;
        for (Entry<RInfo, Integer> info2 : routers.entrySet()) {
            if (cancel) return null;
            if (info2.getKey().getReconnectMethode() != null) {
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, info2.getKey().getReconnectMethode());
            } else if (info2.getKey().getReconnectMethodeClr() != null) {
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.CLR);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, info2.getKey().getReconnectMethodeClr());
            } else
                continue;
            setProgressText(JDL.L("gui.config.routeripfinder.status.testingrouter", "Testing router") + " " + info2.getKey().getRouterName() + " ...");
            setProgress(i++ * 100 / size);

            JDUtilities.getConfiguration().save();
            if (Reconnecter.waitForNewIP(1, true)) {
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, retries);
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, wipchange);
                JDUtilities.getConfiguration().save();
                setProgress(100);
                return info2.getKey();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public RInfo getRouterData() {

        setProgressText(JDL.L("gui.config.routeripfinder.status.collectingrouterinfo", "Collecting router information..."));

        final RInfo infos = RouterInfoCollector.getRInfo(RouterInfoCollector.RInfo_ROUTERSEARCH);
        infos.setReconnectMethode(null);
        infos.setReconnectMethodeClr(null);
        th2 = new Threader();
        final class isalvs {
            boolean isAlv = true;
            ArrayList<String> meths = null;
            HashMap<String, String> SCPDs = null;
        }
        final isalvs isalv = new isalvs();
        final JDRunnable jupnp = new JDRunnable() {

            public void go() throws Exception {

                try {
                    UPnPInfo upnp = new UPnPInfo(InetAddress.getByName(infos.getRouterHost()));
                    if (upnp.met != null && upnp.met.size() != 0) {
                        isalv.SCPDs = upnp.SCPDs;
                        isalv.meths = upnp.met;

                    }
                } catch (Exception e) {
                }
                isalv.isAlv = false;
            }

        };

        th2.getBroadcaster().addListener(new WorkerListener() {

            public void onThreadException(Threader th, JDRunnable job, Throwable e) {
            }

            public void onThreadFinished(Threader th, JDRunnable runnable) {
                if (runnable == jupnp) {
                    isalv.isAlv = false;
                    th2.notify();
                }
            }

            public void onThreadStarts(Threader threader, JDRunnable runnable) {
            }

        });
        th2.add(jupnp);
        th2.add(new JDRunnable() {

            public void go() throws Exception {
                try {

                    LinkedHashMap<RInfo, Integer> routers = new LinkedHashMap<RInfo, Integer>();
                    int upnp = 0;
                    Browser br = new Browser();
                    LinkedHashMap<String, String> he = new LinkedHashMap<String, String>();
                    if (infos.getRouterHost() != null) he.put("RouterHost", infos.getRouterHost());
                    if (infos.getRouterHost() != null) he.put("RouterMAC", infos.getRouterMAC());
                    if (infos.getPageHeader() != null) he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
                    if (infos.getRouterErrorPage() != null) he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
                    he.put("HTMLTagCount", "" + infos.countHtmlTags());
                    ArrayList<RInfo> ra;
                    try {
                        setProgressText(JDL.L("gui.config.routeripfinder.status.downloadlingsimilarmethods", "Downloading similar router methods..."));
                        String st = br.postPage("http://service.jdownloader.org/routerdb/getRouters.php", he);
                        ra = (ArrayList<RInfo>) JDUtilities.xmlStringToObjekt(st);
                    } catch (Exception e) {
                        return;
                    }
                    setProgressText(JDL.L("gui.config.routeripfinder.status.sortingmethods", "Sorting router methods..."));
                    for (RInfo info : ra) {
                        if (info.isHaveUpnpReconnect()) upnp++;

                        if (info.getReconnectMethodeClr() != null) {
                            Integer b = info.compare(infos);
                            info.setIntegrety(200);
                            routers.put(info, b);
                        } else if (info.getReconnectMethode() != null) {
                            Integer b = info.compare(infos);
                            if (info.getIntegrety() > 3) {
                                routers.put(info, b);
                            }
                        }
                    }

                    routers = sortByIntegrety(routers);
                    HashMap<String, RInfo> methodes = new HashMap<String, RInfo>();
                    Iterator<Entry<RInfo, Integer>> inter = routers.entrySet().iterator();
                    while (inter.hasNext()) {
                        Map.Entry<RInfo, Integer> entry = inter.next();
                        RInfo meth = methodes.get(entry.getKey().getReconnectMethode());
                        if (meth != null) {
                            meth.setIntegrety(meth.getIntegrety() + entry.getKey().getIntegrety());
                            inter.remove();
                        } else
                            methodes.put(entry.getKey().getReconnectMethode(), entry.getKey());
                    }
                    routers = sortByIntegrety(routers);
                    if (upnp > 0) {
                        while (isalv.isAlv) {
                            try {
                                wait();
                            } catch (Exception e) {
                            }

                        }
                        if (isalv.meths == null) {

                            UserIO.setCountdownTime(600);
                            int ret = UserIO.getInstance().requestConfirmDialog(0, null, JDL.LF("gui.config.liveHeader.warning.upnpinactive", "Bitte aktivieren sie fals vorhanden Upnp in den Netzwerkeinstellungen ihres Routers <br><a href=\"http://%s\">zum Router</a><br><a href=\"http://wiki.jdownloader.org/index.php?title=Router_Upnp\">Wikiartikel: Upnp Routern</a><br>drücken sie Ok wenn sie Upnp aktiviert haben oder abbrechen wenn sie fortfahren wollen!", infos.getRouterHost()), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                            UserIO.setCountdownTime(-1);
                            if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
                                try {
                                    setProgressText(JDL.L("gui.config.routeripfinder.status.testingupnp", "Testing UPnP..."));
                                    for (int i = 0; i < 30 && !cancel; i++) {
                                        setProgress(i++ * 100 / 30);
                                        UPnPInfo upnpd = new UPnPInfo(InetAddress.getByName(infos.getRouterHost()), 10000);
                                        if (upnpd.met != null) {
                                            infos.setUPnPSCPDs(upnpd.SCPDs);
                                            if (upnpd.met != null && upnpd.met.size() != 0) {

                                                isalv.SCPDs = upnpd.SCPDs;
                                                isalv.meths = upnpd.met;
                                            }
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, infos.getRouterHost());
                    RInfo router = null;
                    if (isalv.meths != null) {
                        HashMap<RInfo, Integer> upnprouters = new HashMap<RInfo, Integer>();
                        for (String info : isalv.meths) {
                            RInfo tempinfo = new RInfo();
                            tempinfo.setRouterHost(infos.getRouterHost());
                            tempinfo.setRouterIP(infos.getRouterIP());
                            tempinfo.setUPnPSCPDs(isalv.SCPDs);
                            tempinfo.setReconnectMethode(info);
                            tempinfo.setRouterName("UPNP:" + tempinfo.getRouterName());
                            upnprouters.put(tempinfo, 1);
                        }
                        router = checkRouters(upnprouters);

                    }
                    if (router == null) {
                        router = checkRouters(routers);
                    }
                    setProgress(100);
                    if (router != null) {
                        infos.setRouterName(router.getRouterName());
                        infos.setReconnectMethode(router.getReconnectMethode());
                        infos.setReconnectMethodeClr(router.getReconnectMethodeClr());
                    }
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
                setProgress(100);
            }
        });
        th2.startAndWait();
        if (infos.getReconnectMethode() != null || infos.getReconnectMethodeClr() != null)
            return infos;
        else
            return null;
    }

    private void setProgress(final int val) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                if (progressBar != null) {
                    progressBar.setValue(val);
                } else {
                    logger.info(val + "%");
                }
                return null;
            }

        }.start();

    }

    private void setProgressText(String text) {
        if (progressBar != null) {
            progressBar.setString(text);
            progressBar.setStringPainted(true);
        } else {
            logger.info(text);
        }
    }

    public static void autoConfig(final Object pass, final Object user, final Object ip, final Object routerScript) {

        final ProgressDialog progress = new ProgressDialog(DummyFrame.getDialogParent(), JDL.L("gui.config.liveHeader.progress.message", "jDownloader sucht nach Ihren Routereinstellungen"), null, false, true);
        final GetRouterInfo routerInfo = new GetRouterInfo(progress);
        final Thread th = new Thread() {
            // @Override
            public void run() {
                String pw = "";
                String username = "";
                String ipadresse = null;
                if (pass instanceof GUIConfigEntry) {
                    pw = (String) ((GUIConfigEntry) pass).getText();
                    username = (String) ((GUIConfigEntry) user).getText();
                    ipadresse = (String) ((GUIConfigEntry) ip).getText();
                } else if (pass instanceof JTextField) {
                    pw = ((JTextField) pass).getText();
                    username = ((JTextField) user).getText();
                    ipadresse = ((JTextField) ip).getText();
                }
                if (ipadresse != null && !ipadresse.matches("\\s*")) JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, ipadresse);
                if (username != null && !username.matches("[\\s]*")) {
                    routerInfo.username = username;
                }
                if (pw != null && !pw.matches("[\\s]*")) {
                    routerInfo.password = pw;
                }
                RInfo data = routerInfo.getRouterData();
                if (data == null) {
                    progress.setVisible(false);
                    progress.dispose();
                    UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.notFound", "jDownloader konnte ihre Routereinstellung nicht automatisch ermitteln."));
                    return;
                }
                if (routerScript != null && routerScript instanceof GUIConfigEntry) {
                    ((GUIConfigEntry) routerScript).setData(data.getReconnectMethode());
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, data.getRouterName());
                }

                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, data.getRouterName());
                progress.setVisible(false);
                progress.dispose();
                UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.yourRouter", "Sie haben einen") + " " + data.getRouterName());

            }
        };
        th.start();
        progress.setThread(th);
        progress.setVisible(true);
        new Thread(new Runnable() {
            public void run() {
                while (th.isAlive()) {
                    try {
                        th.wait();
                    } catch (Exception e) {
                    }
                }
                routerInfo.cancel();
            }
        }).start();

    }

}
