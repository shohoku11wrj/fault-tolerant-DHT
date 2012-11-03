package edu.stevens.cs549.dht.resource;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import edu.stevens.cs549.dht.activity.DHT;
import edu.stevens.cs549.dht.activity.IDHTResource;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.DHTBase.Invalid;


/*
 * Additional resource logic.  The Web resource operations call
 * into wrapper operations here.  The main thing these operations do
 * is to call into the DHT service object, and wrap internal exceptions
 * as HTTP response codes (throwing WebApplicationException where necessary).
 * 
 * This should be merged into NodeResource, then that would be the only
 * place in the app where server-side is dependent on JAX-RS.
 * Client dependencies are in WebClient.
 * 
 * The activity (business) logic is in the dht object, which exposes
 * the IDHTResource interface to the Web service.
 */

public class NodeService {
	
	IDHTResource dht;
	
	public NodeService (UriInfo uri) {
		dht = new DHT(uri);
	}
	
	public NodeInfo getNodeInfo() throws WebApplicationException {
		try {
			return dht.getNodeInfo();
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}
	}
	
	
	public TableRep notify(NodeInfo pred) throws WebApplicationException {
		try {
			return dht.notify(pred);
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);			
		}
	}
	

	public NodeInfo findSuccessor(int id) throws WebApplicationException {
		try {
			return dht.findSuccessor(id);
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);	
		} catch (Failed e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}
	}
	
	/* 
	 * TODO
	 * getSucc, getPred, closestPrecedingFinger, getKey, add
	 * ADD, Ranger, Otc 16
	 */
	/*public NodeInfo findPredessor(int id) throws WebApplicationException, Failed {
		try {
//			return dht.;
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);	
		} catch (Failed e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}
	}*/
	
	public NodeInfo getSucc() throws WebApplicationException, Failed {
		try {
			return dht.getSucc();
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);	
		}
	}
	
	public NodeInfo getPred() throws WebApplicationException, Failed {
		try {
			return dht.getPred();
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);	
		}
	}
	
	public NodeInfo closestPrecedingFinger(int id) throws WebApplicationException, Failed {
		try {
			return dht.closestPrecedingFinger(id);
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);	
		}
	}
	
	// TODO, Ranger
	// review it, for steps after dht.get(k)
	public List<String> getKey(String k) throws Invalid {
		try{
			List<String> keys = new ArrayList<String>();
			String[] keyStrings = dht.get(k);
			if(keyStrings != null) {
				for(String item:keyStrings){
					keys.add(item);
				}
			}
			return keys;
		} catch (Error e) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}
	}
	
	public void add(String k, String v) throws Error, Invalid {
		dht.add(k, v);
	}
	
	public void delete(String k, String v) throws Error, Invalid {
		dht.delete(k, v);
	}
	
	/*public void setSucc(NodeInfo succ) throws Error, Failed {
		dht.setSucc(succ);
	}*/
}
