package edu.stevens.cs549.dht.resource;

import java.util.ArrayList;
import java.util.List;
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

import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.DHTBase.Failed;
import edu.stevens.cs549.dht.activity.DHTBase.Invalid;
import edu.stevens.cs549.dht.activity.NodeInfo;

@Path("/dht")
public class NodeResource {
	
	/*
	 * Web service API.
	 * 
	 * TODO: Fill in the missing operations.  There is a one-to-one relationship
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
	public Response putNotify(JAXBElement<NodeInfo> pred) {
		/*
		 * See the comment for WebClient::notify (the client side of this
		 * logic).
		 */
		NodeInfo p = pred.getValue();
		TableRep db = new NodeService(uriInfo).notify(p);
		if (db == null) {
			return Response.notModified().build();
		} else {
			return Response.ok(db).build();
		}
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
	
	// TODO
	/*
	 * ADD, Ranger, Otc 31 2012
	 * getSucc, getPred, closestPrecedingFinger, getKey, PUT, DELETE
	 */
	@GET
	@Path("getSucc")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> getSucc() throws WebApplicationException, Failed {
		return nodeInfoRep(new NodeService(uriInfo).getSucc());
	}
	
	@GET
	@Path("getPred")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> getPred() throws WebApplicationException, Failed {
		return nodeInfoRep(new NodeService(uriInfo).getPred());
	}
	
	@GET
	@Path("finger")
	@Produces("application/xml")
	public JAXBElement<NodeInfo> closestPrecedingFinger(@QueryParam("id") String index)
			throws WebApplicationException, Failed {
		int id = Integer.parseInt(index);
		return nodeInfoRep(new NodeService(uriInfo).closestPrecedingFinger(id));
	}
	
	@GET
	@Produces("application/xml")
	public List<String> getKey(@QueryParam("key") String key) throws Invalid {
		List<String> keys = new ArrayList<String>();
		keys = new NodeService(uriInfo).getKey(key);
		
		if(keys != null) {
			return keys;
		} else {
			return null;
		}
	}
	
	@PUT
	public void add(@QueryParam("key") String k, @QueryParam("val") String v) 
			throws Error, Invalid {
		new NodeService(uriInfo).add(k, v);
	}
	
	@DELETE
	public void delete(@QueryParam("key") String k, @QueryParam("val") String v) 
			throws Error, Invalid {
		new NodeService(uriInfo).delete(k, v);
	}
	
	/*@PUT
	@Path("setSucc")
	@Consumes("application/xml")
	public void setSucc(JAXBElement<NodeInfo> succ) throws Error, Failed {
		NodeInfo s = succ.getValue();
		new NodeService(uriInfo).setSucc(s);
	}*/
	
}
