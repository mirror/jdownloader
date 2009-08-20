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

package jd.nrouter;

import java.net.InetAddress;

import jd.controlling.JDLogger;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

public class UPnP {
    private InetAddress host;
    private SSDPPacket ssdpPacket;

    // private Document deviceDescription;
    // private Browser deviceBrowser;
    private UpnpDevice device;

    public UPnP(InetAddress ipaddress) {

        this.host = ipaddress;

        // if (ssdpP == null) return;
        //        
        // // ---- Parse XML file ----
        //
        // if (ssdpP.getLocation() == null) return;
        //
        // getSCPDURLs(ssdpP.getLocation());
        // met = createUpnpReconnect(SCPDs, ssdpP.getLocation());
        // //System.out.println(ndList.item(0).getParentNode().getChildNodes().
        // // item(1).getFirstChild());
        // // ---- Error handling ----
        // } catch (SAXParseException spe) {
        // System.out.println("\n** Parsing error, line " + spe.getLineNumber()
        // + ", uri " + spe.getSystemId());
        // System.out.println("   " + spe.getMessage());
        // Exception e = (spe.getException() != null) ? spe.getException() :
        // spe;
        // JDLogger.exception(e);
        // } catch (SAXException sxe) {
        // Exception e = (sxe.getException() != null) ? sxe.getException() :
        // sxe;
        // JDLogger.exception(e);
        // } catch (ParserConfigurationException pce) {
        // JDLogger.exception(pce);

    }

    /**
     * Loads the upnp device
     * 
     * @param timeout
     * @return false if no upnp device has been found
     */
    public boolean load(final long timeout) {
        final ControlPoint c = new ControlPoint();
        c.start();

        final Threader th = new Threader();

        c.addSearchResponseListener(new SearchResponseListener() {

            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
                InetAddress ia = ssdpPacket.getRemoteInetAddress();
                if (ia.getHostAddress().equals(host.getHostAddress())) {
                    // SSDPPacket ssdpP = ssdpPacket;
                    parseSSDPPacket(ssdpPacket);
                    c.stop();
                    th.interrupt();
                } else {
                    JDLogger.getLogger().info("Received foreign package: " + ssdpPacket);
                }
            }
        });

        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                }
                c.stop();
                // wait for
                parseSSDPPacket(null);
                th.interrupt();

            }
        });

        th.startAndWait();
        if (ssdpPacket == null) {
            JDLogger.getLogger().info("No ssdpPackage on " + host + " received. no upnp?");
            return false;
        }
        return true;

    }

    protected synchronized void parseSSDPPacket(SSDPPacket ssdpPacket) {
        if (ssdpPacket == null) return;
        this.ssdpPacket = ssdpPacket;
        this.device = new UpnpDevice(ssdpPacket.getLocation());

    }

    public UpnpDevice getDevice() {
        return device;
    }

}
