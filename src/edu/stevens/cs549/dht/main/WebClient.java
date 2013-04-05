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
import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.resource.NodeResource;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.resource.TableRow;

public class WebClient {

	/*
	 * Encapsulate Web client operations here.
	 * 
	 * TODO: Fill in missing operations.
	 */

	/*
	 * Creation of client instances is expensive, so just create one.
	 */
	protected Client client;

	public WebClient() {
		client = Client.create();
	}

	private LoggingFilter clientLogger = new LoggingFilter();

	public void toggleLogging() {
		if (client.isFilterPreset(clientLogger)) {
			client.removeFilter(clientLogger);
		} else {
			client.addFilter(clientLogger);
		}
	}

	/*
	 * Jersey way of dealing with JAXB client-side: wrap with run-time type
	 * information.
	 */
	private GenericType<JAXBElement<NodeInfo>> nodeInfoType = new GenericType<JAXBElement<NodeInfo>>() {
	};

	private GenericType<JAXBElement<TableRow>> tableRowType = new GenericType<JAXBElement<TableRow>>() {
	};
	
//	private GenericType<JAXBElement<String>> stringType = new GenericType<JAXBElement<String>>() { };

	/*
	 * Ping a remote site to see if it is still available.
	 */
	public boolean isFailed(URI base) {
		URI uri = UriBuilder.fromUri(base).path("info").build();
		WebResource r = client.resource(uri);
		ClientResponse c = r.get(ClientResponse.class);
		return c.getClientResponseStatus().getStatusCode() >= 300;
	}

	/*
	 * Notify node that we (think we) are its predecessor.
	 */
	public TableRep notify(NodeInfo node, TableRep predDb)
			throws DHTBase.Error, DHTBase.Failed {
		/*
		 * The protocol here is more complex than for other operations. We
		 * notify a new successor that we are its predecessor, and expect its
		 * bindings as a result. But if it fails to accept us as its predecessor
		 * (someone else has become intermediate predecessor since we found out
		 * this node is our successor i.e. race condition that we don't try to
		 * avoid because to do so is infeasible), it notifies us by returning
		 * null. This is represented in HTTP by RC=304 (Not Modified).
		 */
		NodeInfo thisNode = predDb.getInfo();
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notify");
		URI notifyPath = ub.queryParam("id", thisNode.id).build();
//		System.out.println("WEBCLIENT: -- "+notifyPath.toString()+" --");
//		System.out.println("WEBCLIENT: -- succ="+predDb.getSucc().id+" --");
		// JAXBElement<NodeInfo> thisRep = NodeResource.nodeInfoRep(thisNode);
		WebResource r = client.resource(notifyPath);
		ClientResponse response = r.put(ClientResponse.class, predDb);
		if (response.getClientResponseStatus() == ClientResponse.Status.NOT_MODIFIED) {
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
	 * Notify to succ's succ about failover.
	 */
	public TableRep notify(Boolean failover, NodeInfo node, TableRep predDb)
			throws DHTBase.Error, DHTBase.Failed {
		/*
		 * The protocol here is more complex than for other operations. We
		 * notify a new successor that we are its predecessor, and expect its
		 * bindings as a result. But if it fails to accept us as its predecessor
		 * (someone else has become intermediate predecessor since we found out
		 * this node is our successor i.e. race condition that we don't try to
		 * avoid because to do so is infeasible), it notifies us by returning
		 * null. This is represented in HTTP by RC=304 (Not Modified).
		 */
		NodeInfo thisNode = predDb.getInfo();
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notify");
		URI notifyPath = ub.queryParam("id", thisNode.id).build();
		// JAXBElement<NodeInfo> thisRep = NodeResource.nodeInfoRep(thisNode);
		WebResource r = client.resource(notifyPath);
		ClientResponse response = r.put(ClientResponse.class, predDb);
		if (response.getClientResponseStatus() == ClientResponse.Status.NOT_MODIFIED) {
			/*
			 * Do nothing, the successor did not accept us as its predecessor.
			 */
			return null;
		} else if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /notify?failover=true");
		} else {
			TableRep bindings = response.getEntity(TableRep.class);
			return bindings;
		}
	}

	/*
	 * Find successor of an id. Used by join protocol
	 */
	public NodeInfo findSuccessor(URI addr, int id) throws DHTBase.Error, DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("find");
		URI findPath = ub.queryParam("id", id).build();
		WebResource r = client.resource(findPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /find?id=ID");
		} else {
			return (NodeInfo) response.getEntity(nodeInfoType).getValue();
		}
	}

	/*
	 * TODO, Ranger Web Service call getSucc, getPred,
	 */

	public NodeInfo getSucc(URI addr) throws DHTBase.Error, DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("getSucc");
		URI findPath = ub.build();
		WebResource r = client.resource(findPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /getSucc");
		} else {
			return (NodeInfo) response.getEntity(nodeInfoType).getValue();
		}
	}

	public NodeInfo getPred(URI addr) throws DHTBase.Error, DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("getPred");
		URI findPath = ub.build();
		WebResource r = client.resource(findPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /getPred");
		} else {
			return (NodeInfo) response.getEntity(nodeInfoType).getValue();
		}
	}

	public NodeInfo closestPrecedingFinger(URI addr, int id)
			throws DHTBase.Error, DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("clostPredFinger");
		URI getPath = ub.queryParam("id", id).build();
		WebResource r = client.resource(getPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /clostPredFinger?id=ID");
		} else {
			return (NodeInfo) response.getEntity(nodeInfoType).getValue();
		}
	}

	public String[] get(URI addr, String k) throws DHTBase.Error,
			DHTBase.Failed {
		// List<String> keys = null;
		// GenericType<List<String>> genericType = new
		// GenericType<List<String>>(){};
		TableRow keysFromWebService;
		UriBuilder ub = UriBuilder.fromUri(addr);
		URI getPath = ub.queryParam("key", k).build();
		WebResource r = client.resource(getPath);
		ClientResponse response = r.get(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET ?key=KEY");
		} else {
			// ?how to retrieve a List<String> from Web service
			// keys = (List<String>)response.getEntity(genericType);
			keysFromWebService = (TableRow) response.getEntity(tableRowType)
					.getValue();
		}
		// ArrayList<String> keyArray = new ArrayList<String>();
		if (keysFromWebService == null) {
			return null;
		} else {
			String[] result = (String[]) keysFromWebService.vals;
			return result;
		}
	}

	public void add(URI addr, String k, String v) throws DHTBase.Error,
			DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr);
		URI putPath = ub.queryParam("key", k).queryParam("val", v).build();
		WebResource r = client.resource(putPath);
		ClientResponse response = r.put(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT ?key=KEY&val=VAL");
		}
	}

	public void delete(URI addr, String k, String v) throws DHTBase.Error,
			DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr);
		URI deletePath = ub.queryParam("key", k).queryParam("val", v).build();
		WebResource r = client.resource(deletePath);
		ClientResponse response = r.delete(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("DELETE ?key=KEY&val=VAL");
		}
	}
	
	// TODO, Ranger
	// notify, fail over
	public TableRep notifyFailover(NodeInfo node, TableRep backedupDb) throws Failed {
		// NodeInfo thisNode = backedupDb.getInfo();
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notifyFailover");
		URI notifyPath = ub.build();
		// JAXBElement<NodeInfo> thisRep = NodeResource.nodeInfoRep(thisNode);
		WebResource r = client.resource(notifyPath);
		ClientResponse response = r.put(ClientResponse.class, backedupDb);
		if (response.getClientResponseStatus() == ClientResponse.Status.NOT_MODIFIED) {
			/*
			 * Do nothing, the successor did not accept us as its predecessor.
			 */
			return null;
		} else if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /notify?failover=true");
		} else {
			TableRep bindings = response.getEntity(TableRep.class);
			return bindings;
		}
	}

	public void addBackup(URI addr, String k, String v) throws DHTBase.Error,
	DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr);
		URI putPath = ub.queryParam("key", k).queryParam("val", v).path("backup").build();
		WebResource r = client.resource(putPath);
		ClientResponse response = r.put(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /backup?key=KEY&val=VAL");
		}
	}

	public void delBackup(URI addr, String k, String v) throws DHTBase.Error,
	DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr);
		URI deletePath = ub.queryParam("key", k).queryParam("val", v).path("backup").build();
		WebResource r = client.resource(deletePath);
		ClientResponse response = r.delete(ClientResponse.class);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("DELETE /backup?key=KEY&val=VAL");
		}
	}

}
