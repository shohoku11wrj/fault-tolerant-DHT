package edu.stevens.cs549.dht.activity;

import java.io.PrintWriter;

import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.DHTBase.Invalid;

/*
 * The part of the DHT business logic that is used in the CLI
 * (the business logic for a command line interface).
 */

public interface IDHTNode {
	
	/*
	 * Adding and deleting content at the local node.
	 */
	public String[] get(String k) throws Error, Invalid;
	
	public void add(String k, String v) throws Error, Invalid;
	
	public void delete(String k, String v) throws Error, Invalid;
	
	/*
	 * Adding and deleting content in the network.
	 */
	public String[] getNet(String k) throws Error, Failed;
	
	public void addNet(String k, String v) throws Error, Failed;
	
	public void deleteNet(String k, String v) throws Error, Failed;
	
	/*
	 * Insert this node into a DHT identified by a node's URI.
	 */
	public void join(String uri) throws Error, Failed, Invalid;
	
	/*
	 * Display internal state at the CLI.
	 */
	public void display() throws Error;
	
	public void routes() throws Error;
	
	/*
	 * Simulate failure.
	 */
	public void setFailed();

}
