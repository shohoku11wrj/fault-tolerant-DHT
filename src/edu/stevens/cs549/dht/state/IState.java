package edu.stevens.cs549.dht.state;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.state.Persist.Table;

/*
 * The interface for a state server that maintains the
 * local (key,value)-pair bindings for a DHT node.
 * This should ONLY be accessed locally by the DHT Web service.
 * Think of it as a database server for a DHT node.
 */
public interface IState extends Remote {
	
	/*
	 * Simulate node failure.
	 */
	public void setFailed() throws RemoteException;
	
	/*
	 * The key for a node.
	 */
	public NodeInfo getNodeInfo() throws RemoteException, Error;
	
	/*
	 * Get all values stored under a key.
	 */
	public String[] get(String k) throws RemoteException, Error;

	/*
	 * Add a binding under a key (always cumulative).
	 */
	public void add(String k, String v) throws RemoteException, Error;

	/*
	 * Delete a binding under a key.
	 */
	public void delete(String k, String v) throws RemoteException, Error;
	
	/*
	 * Clear all bindings (necessary if a node joins a network with pre-existing bindings).
	 */
	public void clear() throws RemoteException, Error;
	
	
	/*
	 * These operations support the protocol for data transfers between pred & succ.
	 */
	
	/*
	 * The successor uses this operation to create a table of some of its bindings,
	 * for transmission to its predecessor.  TableRep also contains succ pointer,
	 * so predecessor can alert successor to take over if this node fails.
	 */
	public TableRep extractBindings(int predId) throws RemoteException, Error;
	
	public TableRep extractBindings() throws RemoteException, Error;

	/*
	 * The successor uses this to remove bindings it has transferred to the predecessor.
	 */
	public void dropBindings(int id) throws RemoteException, Error;
	
	/*
	 * The predecessor uses this operation to install all bindings 
	 * into its internal hash table, as part of its own initialization.
	 */
	public void installBindings(TableRep bindings) throws RemoteException, Error;
	
	/*
	 * The successor uses this operation to install the predecessor's bindings
	 * into backup storage.  Each node is also a hot standby for its predecessor.
	 */
	public void backupBindings(TableRep bindings) throws RemoteException, Error;
	
	/*
	 * Each node backs up the successor pointer of its successor,
	 * and notifies the succ of the succ when it should take over from succ.
	 */
	public void backupSucc(TableRep bindings) throws RemoteException, Error;

	
	/*
	 * Backup up the bindings in the table to a file.
	 */
	public void backup(String filename) throws IOException, RemoteException, Error;

	/*
	 * Reload the bindings from a file.
	 */
	public void reload(String filename) throws IOException, RemoteException, Error;
	
	/*
	 * Display the contents of the local hash table.
	 */
	public void display() throws RemoteException;

	// TODO, Ranger
	
	public NodeInfo getBackupSucc() throws RemoteException, Error;

	public void setBackupSucc(NodeInfo succOfSucc) throws RemoteException, Error;

	public void addBackup(String k, String v) throws RemoteException, Error;

	public void delBackup(String k, String v) throws RemoteException, Error;

	public TableRep failoverBindings() throws RemoteException, Error;

	public void display(Table tb) throws RemoteException;

	public void setPred(NodeInfo info) throws RemoteException, Error;

}
