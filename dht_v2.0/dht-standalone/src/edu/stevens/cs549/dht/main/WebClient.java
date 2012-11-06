package edu.stevens.cs549.dht.main;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBElement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

import edu.stevens.cs549.dht.activity.DHTBase;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.resource.NodeResource;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.resource.TableRow;

public class WebClient {
	
	/*
	 * Encapsulate Web client operations here.
	 * 
	 * TODO: Fill in missing operations.  The missing operations are
	 * obvious once the DHT code is finished.
	 * 
	 */
	
	/*
	 * Creation of client instances is expensive, so just create one.
	 */
	protected Client client;
	
	public WebClient () {
		client = Client.create();
	}
	
	private LoggingFilter clientLogger = new LoggingFilter();
	
	public void toggleLogging() {
		if (client.isFilterPreset(clientLogger)) {
			client.removeFilter(clientLogger)
;		} else {
			client.addFilter(clientLogger);
		}
	}
	
	
	/*
	 * Jersey way of dealing with the JAXBlement wrapper class on the client side: 
	 * wrap with run-time type information.
	 */
	private GenericType<JAXBElement<NodeInfo>> nodeInfoType = new GenericType<JAXBElement<NodeInfo>>() { };
	
	private GenericType<JAXBElement<TableRow>> tableRowType = new GenericType<JAXBElement<TableRow>>() { };


	/*
	 * Ping a remote site to see if it is still available.
	 */
	public boolean isFailed (URI base) {
		URI uri = UriBuilder.fromUri(base).path("info").build();
		WebResource r = client.resource(uri);
		ClientResponse c = r.get(ClientResponse.class);
		return c.getClientResponseStatus().getStatusCode()>=300;
	}
	
	
	/*
	 * Notify node that we (think we) are its predecessor.
	 */
	public TableRep notify(NodeInfo node, NodeInfo thisNode)
			throws DHTBase.Error, DHTBase.Failed {
    	/*
    	 * The protocol here is more complex than for other operations.
    	 * We notify a new successor that we are its predecessor, and expect
    	 * its bindings as a result.  But if it fails to accept us as its
    	 * predecessor (someone else has become intermediate predecessor since
    	 * we found out this node is our successor i.e. race condition that we
    	 * don't try to avoid because to do so is infeasible), it notifies us by 
    	 * returning null.  This is represented in HTTP by RC=304 (Not Modified).
    	 */
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notify");
		URI notifyPath = ub.queryParam("id", thisNode.id).build();
		JAXBElement<NodeInfo> thisRep = NodeResource.nodeInfoRep(thisNode);
		WebResource r = client.resource(notifyPath);
		ClientResponse response = r.put(ClientResponse.class, thisRep);
		if (response.getClientResponseStatus()==ClientResponse.Status.NOT_MODIFIED) {
			/*
			 * Do nothing, the successor did not accept us as its predecessor.
			 */
			return null;
		} else if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /notify?id=ID");
		} else {
			TableRep bindings = response.getEntity(TableRep.class);
			return bindings;
		}
	}
	
	
	/*
	 * Find successor of an id.  Used by join protocol
	 */
	public NodeInfo findSuccessor(URI addr, int id) throws DHTBase.Error, DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("find");
		URI findPath = ub.queryParam("id", id).build();
		WebResource r = client.resource(findPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /find?id=ID");
		} else {
			return (NodeInfo)response.getEntity(nodeInfoType).getValue();
		}
	}
	
}
