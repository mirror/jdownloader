package jd;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
	
	public void processParameters(String[] input) throws RemoteException;
	
}