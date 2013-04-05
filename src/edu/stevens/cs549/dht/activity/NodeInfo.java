package edu.stevens.cs549.dht.activity;

import java.net.URI;

public class NodeInfo implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * Our node value in the key space.
	 */
	public int id;
	
	/*
	 * URI for Web service requests.
	 */	
	public URI addr;
	
	public NodeInfo (int id, URI addr) {
		this.id = id;
		this.addr = addr;
	}
	
	public NodeInfo () { }
	
	@Override
	public String toString() {
		return "[id="+id+",addr="+addr+"]";
	}

}
