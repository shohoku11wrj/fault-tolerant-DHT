package edu.stevens.cs549.dht.activity;

import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.DHTBase.Invalid;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.state.IState;


/*
 * The part of the DHT business logic that is used in the NodeActivity
 * class (the business logic for a node resource).
 */

public interface IDHTResource {
	
	public NodeInfo getNodeInfo() throws Error;
	
	public NodeInfo getSucc() throws Error;
	
	public NodeInfo getPred() throws Error;
	
	/*
	 * Called externally during the join protocol.
	 */
	public NodeInfo findSuccessor(int id) throws Error, Failed;
	
	/*
	 * Exposing the finger table in the node API.
	 * Alternatively search for predecessor tail-recursively,
	 * but be sure to redirect HTTP to the next node.
	 */
	public NodeInfo closestPrecedingFinger (int id) throws Error;
	
	/*
	 * Called by a node to notify another node that it believes it is the latter
	 * node's new predecessor.  If acknowledged by the successor, then a range of
	 * key-value pairs is transferred from the successor  (all bindings 
	 * at the successor up to and including the key of the new predecessor).
	 */
	public TableRep notify (NodeInfo pred) throws Error;
	
	/*
	 * The operations for actually accessing the underlying hash table for a node.
	 */
	public String[] get(String k) throws Error, Invalid;
	
	public void add(String k, String v) throws Error, Invalid;
	
	public void delete(String k, String v) throws Error, Invalid;

	/*
	 * ADD, Ranger
	 * setPred, setSucc
	 */
	public void setPred(NodeInfo pred) throws Error;
	
}
