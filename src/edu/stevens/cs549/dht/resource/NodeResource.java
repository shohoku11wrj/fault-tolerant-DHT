package edu.stevens.cs549.dht.resource;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.DHTBase.Invalid;
import edu.stevens.cs549.dht.activity.NodeInfo;

@Path("/dht")
public class NodeResource {

	/*
	 * Web service API.
	 * 
	 * TODO: Fill in the missing operations. There is a one-to-one relationship
	 * between server-side operations defined here and client-side operations
	 * defined in WebClient.
	 */

	Logger log = Logger.getLogger(NodeResource.class.getCanonicalName());

	@Context
	UriInfo uriInfo;

	private static final String ns = "http://www.stevens.edu/cs549/dht";

	public static final QName nsNodeInfo = new QName(ns, "NodeInfo");

	public static JAXBElement<NodeInfo> nodeInfoRep(NodeInfo n) {
		return new JAXBElement<NodeInfo>(nsNodeInfo, NodeInfo.class, n);
	}

	@GET
	@Path("info")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> getNodeInfoXML() {
		return nodeInfoRep(new NodeService(uriInfo).getNodeInfo());
	}

	@GET
	@Path("info")
	@Produces("application/json")
	public JAXBElement<NodeInfo> getNodeInfoJSON() {
		return nodeInfoRep(new NodeService(uriInfo).getNodeInfo());
	}

	@PUT
	@Path("notify")
	@Consumes("application/xml")
	@Produces("application/xml")
	/*
	 * Actually returns a TableRep (annotated with @XmlRootElement)
	 */
	public Response putNotify(TableRep predDb) {
		/*
		 * See the comment for WebClient::notify (the client side of this
		 * logic).
		 */
		TableRep db = new NodeService(uriInfo).notify(predDb);
		if (db == null) {
			return Response.notModified().build();
		} else {
			return Response.ok(db).build();
		}
	}
	
	@PUT
	@Path("notifyFailover")
	@Consumes("application/xml")
	/*
	 * Trigger failover in succ of succ
	 */
	public Response notifyFailover(TableRep predDb) {
		/*
		 * See the comment for WebClient::notify (the client side of this
		 * logic).
		 */
		//if(failover==true){
		new NodeService(uriInfo).notifyFailover(predDb);
		//}
		return Response.notModified().build();
	}

	public static final QName nsTableRow = new QName(ns, "TableRow");

	public static JAXBElement<TableRow> tableRowRep(TableRow tr) {
		return new JAXBElement<TableRow>(nsTableRow, TableRow.class, tr);
	}

	@GET
	@Path("find")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> findSuccessor(@QueryParam("id") String index) {
		int id = Integer.parseInt(index);
		return nodeInfoRep(new NodeService(uriInfo).findSuccessor(id));
	}

	// TODO, Ranger
	/*
	 * ADD, Ranger, Otc 31 2012 getSucc, getPred, closestPrecedingFinger,
	 * getKey, PUT, DELETE
	 */

	@GET
	@Path("getSucc")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> getSucc() throws WebApplicationException,
			Failed {
		return nodeInfoRep(new NodeService(uriInfo).getSucc());
	}

	@GET
	@Path("getPred")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> getPred() throws WebApplicationException,
			Failed {
		return nodeInfoRep(new NodeService(uriInfo).getPred());
	}

	@GET
	@Path("clostPredFinger")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> closestPrecedingFinger(
			@QueryParam("id") String index) throws WebApplicationException,
			Failed {
		int id = Integer.parseInt(index);
		return nodeInfoRep(new NodeService(uriInfo).closestPrecedingFinger(id));
	}

	public static final QName qnListString = new QName(ns, "ListString");

	// public static List<JAXBElement<String>> listStringRep(String key) {
	// return new JAXBElement<NodeInfo>(qnListString, NodeInfo.class, n);
	// }

	@GET
	@Produces("application/xml")
	public JAXBElement<TableRow> getKey(@QueryParam("key") String key)
			throws Invalid {
		String[] keys = new NodeService(uriInfo).getKey(key);
//		String keyToString = new String();
//		if (keys == null) {
//			keyToString = null;
//		} else {
//			for (int i = 0; i < keys.length; i++) {
//				keyToString += keys[i] + ";";
//			}
//		}
//		if (keyToString.length() > 1) {
//			keyToString = keyToString.substring(0, keyToString.length() - 1);
//		}
		if (keys != null) {
			TableRow tr = new TableRow(key, keys);
//			System.out.println("JAXBElement<String>= " + keys.toString());
			return new JAXBElement<TableRow>(nsTableRow, TableRow.class, tr);

//			return new JAXBElement<String>(qnListString, String.class,
//					keyToString);
		} else {
			return null;
		}
	}

	@PUT
	public void add(@QueryParam("key") String k, @QueryParam("val") String v)
			throws Error, Invalid, edu.stevens.cs549.dht.activity.DHTBase.Error {
		new NodeService(uriInfo).add(k, v);
	}

	@DELETE
	public void delete(@QueryParam("key") String k, @QueryParam("val") String v)
			throws Error, Invalid, edu.stevens.cs549.dht.activity.DHTBase.Error {
		new NodeService(uriInfo).delete(k, v);
	}
	
	/*
	 * TODO, Ranger
	 * Assignment4, fault-tolerant-dht
	 */
	
	/*
	 * addBackup, delteBackup
	 * log updates of key bindings at a node, at its successor
	 */
	@PUT
	@Path("backup")
	public void addBackup(@QueryParam("key") String k, @QueryParam("val") String v)
			throws Error, Invalid, edu.stevens.cs549.dht.activity.DHTBase.Error {
		new NodeService(uriInfo).addBackup(k, v);
	}
	
	@DELETE
	@Path("backup")
	public void deleteBackup(@QueryParam("key") String k, @QueryParam("val") String v)
			throws Error, Invalid, edu.stevens.cs549.dht.activity.DHTBase.Error {
		new NodeService(uriInfo).delBackup(k, v);
	}
	
	/*
	 * uses a Boolean flag to signal to the successor node that it should failover 
	 * and start providing the bindings that it backed up from its old predecessor
	 */
	/*@PUT
	@Path("notify")
	@Consumes("application/xml")
	@Produces("application/xml")
	public Response putNotify(@QueryParam("failover") Boolean fl) {
		
		 * See the comment for WebClient::notify (the client side of this
		 * logic).
		 
		// TODO, Ranger
		//?what's the difference between notify and notify?failover=true?
		if(fl==true) {
			
		} else {
			return Response.notModified().build();
		}
	}*/

}
