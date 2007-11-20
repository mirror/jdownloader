package jd;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

/**
 * Diese Klasse registriert dieses Objekt in der Lokalen Registrierung (läuft auf einem Port) Damit kann überwacht werden, ob jDownloader schon gestartet ist oder nicht
 * 
 * @author astaldo
 */
public class SingleInstanceController extends RemoteObject {
    /**
     * 1534646354564113976L
     */
    private static final long  serialVersionUID = -1534646354564113976L;
    /**
     * RMI Port
     */
    public static int          RMI_PORT         = 6789;
    /**
     * RMI Name
     */
    public final static String RMI_NAME         = "JD_SINGLE_INSTANCE_CONTROLLER";
    private static Logger      logger           = JDUtilities.getLogger();
    /**
     * Zeigt an, ob diese Applikation bereits gestartet ist oder nicht. Wurde die Applikation bereits gestartet, ist bereits ein Objekt in der Registrierung verknüpft.
     * 
     * @return wahr, wenn die Applikation bereits gestartet ist.
     */
    public static boolean isApplicationRunning() {
        // Mit dieser Einstellung hält daß Programm höchstens xx Millisekunden an dieser Stelle an
        // um eine Verbindung mit einem RMI Service herzustellen.
        // Sun-Default war 60000 Millisekunden !
        // Das passiert vor allem dann, wenn dieser Port bereits belegt ist.
        Properties p = System.getProperties();
        p.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "1000");
        Remote app = SingleInstanceController.getRMIObject(SingleInstanceController.RMI_PORT, SingleInstanceController.RMI_NAME);
        if (app == null)
            return false;
        else
            return true;
    }
    /**
     * Mit dieser Funktion wird ein lokaler RMI Service gestartet. Dieser horcht auf einem bestimmten Port.
     * 
     * @param remoteObject Das Objekt, daß zu diesem RMI Port und Namen gehören soll
     * @return Das erstellte Remoteobjekt
     * @throws SecurityException
     */
    public static Remote bindRMIObject(Remote remoteObject) throws SecurityException {
        Remote remoteStub = null;
        Properties p = System.getProperties();
        // Mit dieser Einstellung hält daß Programm höchstens xx Millisekunden an dieser Stelle an
        // um eine Verbindung mit einem RMI Service herzustellen.
        // Sun-Default war 60000 Millisekunden !
        // Das passiert vor allem dann, wenn dieser Port bereits belegt ist.
        p.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "1000");
        remoteStub = SingleInstanceController.getRMIObject(RMI_PORT, RMI_NAME);
        if (remoteStub != null) {
            logger.info("Das RMI Objekt (" + RMI_NAME + ") ist bereits vorhanden");
        }
        else {
            try {
                // Lokale Registrierung
                Registry localRegistry;
                // Versuche, die Registry auf diesem Port zu erstellen
                try {
                    localRegistry = LocateRegistry.createRegistry(SingleInstanceController.RMI_PORT);
                }
                catch (RemoteException e) {
                    // Falls diese bereits vorhanden ist, wird sie von diesem Port geholt
                    localRegistry = LocateRegistry.getRegistry(SingleInstanceController.RMI_PORT);
                }
                // RMI Interface wird verknüpft
                remoteStub = (Remote) UnicastRemoteObject.exportObject(remoteObject, 0);
                // ..und gespeichert
                localRegistry.bind(RMI_NAME, remoteStub);
                logger.info("RMI Objekt (" + RMI_NAME + ") auf Port " + SingleInstanceController.RMI_PORT);
            }
            catch (RemoteException e) {
                logger.warning("Die Remotefunktionen konnten nicht gestartet werden: " + e.toString());
            }
            catch (AlreadyBoundException e) {
                logger.warning("Das RMI Objekt (" + RMI_NAME + ") ist bereits vorhanden. " + e.toString());
            }
        }
        return remoteStub;
    }
    /**
     * Diese Methode liefert ein RMI Objekt zurück.
     * 
     * @param rmiPort Port des RMI Services
     * @param rmiName Name des gewünschten RMI Objektes
     * @return Das gewünschte RMI Objekt oder null, falls Probleme auftauchten
     */
    public static Remote getRMIObject(int rmiPort, String rmiName) {
        Remote remoteStub = null;
        try {
            remoteStub = Naming.lookup("//localhost:" + rmiPort + "/" + rmiName + "");
        }
        catch (MalformedURLException e) {
        }
        catch (RemoteException e) {
        }
        catch (NotBoundException e) {
        }
        return remoteStub;
    }
    /**
     * Sorgt dafür, daß das RMI Interface geschlossen wird
     */
    public static void unbindRMIObject() {
        try {
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.unbind(RMI_NAME);
            logger.info("RMI Objekt (" + RMI_NAME + ") deaktiviert");
        }
        catch (AccessException e) {
            logger.severe("Keine Rechte für diese Aktion. " + e.toString());
        }
        catch (RemoteException e) {
            logger.warning("RMI Objekt (" + RMI_NAME + ") nicht vorhanden. " + e.toString());
        }
        catch (NotBoundException e) {
            logger.severe("Registry nicht verknüpft. " + e.toString());
        }
    }
}
