package edu.stevens.cs549.dht.state;

import java.io.PrintWriter;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.stevens.cs549.dht.activity.DHTBase;
import edu.stevens.cs549.dht.activity.NodeInfo;

/*
 * Interface for the RMI server that exposes the state of the routing tables.
 * ONLY to be invoked by the local business logic of the DHT node.
 */

public interface IRouting extends Remote {

	public static final boolean USE_FINGER_TABLE = false;
	
	public static final int NFINGERS = 6;
	
	public static final int NKEYS = 64;
	
	public NodeInfo getPred() throws RemoteException, DHTBase.Error;
	
	public void setPred(NodeInfo pred) throws RemoteException, DHTBase.Error;
	
	public NodeInfo getSucc() throws RemoteException, DHTBase.Error;
	
	public void setSucc(NodeInfo succ) throws RemoteException, DHTBase.Error;
	
	public void setFinger (int i, NodeInfo info) throws RemoteException, DHTBase.Error;
	
	public NodeInfo getFinger (int i) throws RemoteException, DHTBase.Error;
	
	public NodeInfo closestPrecedingFinger (int id) throws RemoteException, DHTBase.Error;
	
	public void routes() throws RemoteException;

}
