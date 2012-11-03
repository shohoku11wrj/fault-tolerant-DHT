package edu.stevens.cs549.dht.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import edu.stevens.cs549.dht.main.Main;
import edu.stevens.cs549.dht.main.WebClient;
import edu.stevens.cs549.dht.resource.NodeResource;
import edu.stevens.cs549.dht.resource.NodeService;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.state.IRouting;
import edu.stevens.cs549.dht.state.IState;

public class DHT extends DHTBase 
	implements IDHTResource, IDHTNode, IDHTBackground {

	/*
	 * DHT logic.
	 * 
	 * This logic may be invoked from a RESTful Web service or from the command
	 * line, as reflected by the interfaces.
	 */

	/*
	 * Client stub for Web service calls.
	 */
	protected WebClient client = new WebClient();
	
	public WebClient getClient() {
		return client;
	}
	
	private boolean debug() {
		return Main.DEBUG;
	}
	
	private void debug(String msg) {
		if (debug()) info(msg);
	}

	
	/*
	 * Logging operations.
	 */

	protected Logger log = Logger.getLogger(DHT.class.getCanonicalName());
	
	protected void info(String s) {
		log.info(s);
	}

	protected void warning(String s) {
		log.warning(s);
	}

	protected void severe(String s) {
		log.severe(s);
	}

	/*
	 * The URL for this node in the DHT. Although the info is stored in state,
	 * we should be able to cache it locally since it does not change.
	 */
	protected NodeInfo info;

	/*
	 * Remote clients may call this when joining the network and they
	 * know only our URI and need the node identifier (key) as well.
	 */
	// WebMethod
	public NodeInfo getNodeInfo() throws Error {
		 try {
			 if (info == null)
			 info = state.getNodeInfo();
			 return info;
		 } catch (RemoteException e) {
			 severe("RMI error: " + e);
			 throw new Error(e);
		 }
		 // COMMENT, Ranger, Otc 26
//		assert this.info != null;
//		return this.info;
	}

	/*
	 * Key-value pairs stored in this node (in a local RMI server).
	 */
	private IState state;

	/*
	 * Finger tables, and predecessor and successor pointers, are also stored in
	 * a local RMI server, to be retrieved by the business logic for a Web
	 * service.
	 */
	private IRouting routing;

	/*
	 * This constructor is called when a DHT object is created for the CLI.
	 */
	public DHT(NodeInfo info, IState s, IRouting r) {
		this.info = info;
		this.state = s;
		this.routing = r;
	}

	/*
	 * This constructor is called when a DHT object is created in a Web service.
	 */
	public DHT(UriInfo uri) {
		try {
			/*
			 * Load server properties
			 */
			Properties props = new Properties();
			InputStream in = getClass().getResourceAsStream(Main.serverPropsFile);
			props.load(in);
			in.close();
			
			int rmiPort = Integer.parseInt((String) props.getProperty("server.port.rmi"));
			
			/*
			 * A resource retrives its state from the RMI state server for this
			 * resource. Use the URI as a key for looking uo in RMI registry.
			 */
			Registry reg = LocateRegistry.getRegistry(rmiPort); //EDIT, Main.rmiPort() --> rmiPort
			URI baseURI = Main.getURI(uri.getBaseUri());
			Object svr = reg.lookup(uri.toString());
			state = Main.stateServer();
			routing = Main.routingServer();
			this.info = state.getNodeInfo();
		} catch (RemoteException e) {
			severe("DHT(URI): RMI error: " + e);
		} catch (IOException e) {
			severe("DHT(URI): IO Exception: " + e);
		} catch (NotBoundException e) {
			severe("DHT(URI): RMI object not found: " + e);
		} catch (Error e) {
			severe("DHT(URI): Internal error: " + e);
		}
	}
	
	/*
	 * Jersey way of dealing with the JAXBlement wrapper class on the client side: 
	 * wrap with run-time type information.
	 */
	private GenericType<JAXBElement<NodeInfo>> nodeInfoType = new GenericType<JAXBElement<NodeInfo>>() { };
	

	/*
	 * Get the successor of a node. Need to perform a Web service call to that
	 * node, and it then retrieves its routing state from its local RMI object.
	 * 
	 * Make a special case for when this is the local node, i.e.,
	 * info.addr.equals(localInfo.addr), otherwise get an infinite loop.
	 */	
	private NodeInfo getSucc(NodeInfo info) throws Error, Failed {
		NodeInfo localInfo = this.getNodeInfo();
		if (localInfo.addr.equals(info.addr)) {
			return getSucc();
		} else {
			// TODO: Do the Web service call
			NodeInfo infoSucc = new NodeInfo();
			// ?how to call web service by using known Uri?
			Client c = Client.create();
			UriBuilder ub = UriBuilder.fromUri(info.addr).path("find");
			URI findPath = ub.queryParam("id", info.id).build();
			WebResource r = c.resource(findPath);
			ClientResponse response = r.get(ClientResponse.class);
			if (response.getStatus() >= 300) {
				throw new DHTBase.Failed("GET /find?id=ID");
			} else {
				infoSucc = (NodeInfo)response.getEntity(nodeInfoType).getValue();
			}
			return infoSucc;
		}
	}

	/*
	 * This version gets the local successor from RMI server.
	 */
	// WebMethod
	public NodeInfo getSucc() throws Error {
		try {
			return routing.getSucc();
		} catch (RemoteException e) {
			severe("GetSucc: RMI error in getSucc: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Set the local successor pointer on the RMI server.
	 */
	public void setSucc(NodeInfo succ) throws Error {
		try {
			routing.setSucc(succ);
		} catch (RemoteException e) {
			severe("SetSucc: RMI error in setSucc: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Get the predecessor of a node. Need to perform a Web service call to that
	 * node, and it then retrieves its routing state from its local RMI object.
	 * 
	 * Make a special case for when this is the local node, i.e.,
	 * info.addr.equals(localInfo.addr), otherwise get an infinite loop.
	 */
	protected NodeInfo getPred(NodeInfo info) throws Error,
			Failed {
		NodeInfo localInfo = this.getNodeInfo();
		if (localInfo.addr.equals(info.addr)) {
			return getPred();
		} else {
			/*
			 * TODO: Do the Web service call
			 */
			NodeInfo infoPred = new NodeInfo();
			NodeInfo infoSucc = new NodeInfo();
			infoSucc = getSucc(info);
			Client c = Client.create();
			UriBuilder ub = UriBuilder.fromUri(infoSucc.addr).path("getPred");
			URI findPath = ub.build();
			WebResource r = c.resource(findPath);
			ClientResponse response = r.get(ClientResponse.class);
			if (response.getStatus() >= 300) {
				throw new DHTBase.Failed("GET /getPred");
			} else {
				infoPred = (NodeInfo)response.getEntity(nodeInfoType).getValue();
			}
			return infoPred;
		}
	}

	/*
	 * This version gets the local predecessor from RMI server.
	 */
	// WebMethod
	public NodeInfo getPred() throws Error {
		try {
			return routing.getPred();
		} catch (RemoteException e) {
			severe("GetPred: RMI error: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Set the local predecessor pointer on the RMI server.
	 */
	protected void setPred(NodeInfo pred) throws Error {
		try {
			routing.setPred(pred);
		} catch (RemoteException e) {
			severe("SetPred: RMI error in setPred: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Perform a Web service call to get the closest preceding finger in the
	 * finger table of the argument node.
	 */
	protected NodeInfo closestPrecedingFinger(NodeInfo info, int id)
			throws Error, Failed {
		NodeInfo localInfo = this.getNodeInfo();
		if (localInfo.equals(info)) {
			return closestPrecedingFinger(id);
		} else {
			if (IRouting.USE_FINGER_TABLE) {
				/*
				 * TODO: Do the Web service call to the remote node.
				 */
				NodeInfo cloestPredNode = new NodeInfo();
				Client c = Client.create();
				UriBuilder ub = UriBuilder.fromUri(info.addr).path("finger");
				URI getPath = ub.queryParam("id", info.id).build();
				WebResource r = c.resource(getPath);
				ClientResponse response = r.get(ClientResponse.class);
				if (response.getStatus() >= 300) {
					throw new DHTBase.Failed("GET /finger?id=ID");
				} else {
					cloestPredNode = (NodeInfo)response.getEntity(nodeInfoType).getValue();
				}
				return cloestPredNode;
			} else {
				/*
				 * Without finger tables, just use the successor pointer.
				 */
				return getSucc(info);
			}
		}
	}

	/*
	 * For the local version, call the RMI server.
	 */
	// WebMethod
	public NodeInfo closestPrecedingFinger(int id) throws Error {
		try {
			return routing.closestPrecedingFinger(id);
		} catch (RemoteException e) {
			severe("ClosestPrecedingFinger: RMI error: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Set a finger pointer.
	 */
	protected void setFinger(int ix, NodeInfo node) throws Error {
		try {
			routing.setFinger(ix, node);
		} catch (RemoteException e) {
			severe("SetFinger: RMI error: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Find the node that will hold bindings for a key k. Search the circular
	 * list to find the node. Stop when the node's successor stores values
	 * greater than k.
	 */
	protected NodeInfo findPredecessor(int id) throws Error, Failed {
		NodeInfo info = getNodeInfo();
		NodeInfo succ = getSucc(info);
		if (info.id != succ.id) {
			while (!inInterval(id, info.id, succ.id)) {
				info = closestPrecedingFinger(info, id);
				succ = getSucc(info);
			}
		}
		return info;
	}

	/*
	 * Find the successor of k, starting at the current node.
	 * Called internally and (server-side) in join protocol.
	 */
	// WebMethod
	public NodeInfo findSuccessor(int id) throws Error, Failed {
		NodeInfo predInfo = findPredecessor(id);
		return getSucc(predInfo);
	}

	/*
	 * Find the successor of k, given the URI for a node.
	 * Used as client part of the join protocol.
	 */
	protected NodeInfo findSuccessor(URI addr, int id) throws Error, Failed {
		return client.findSuccessor(addr, id);
	}

	/*
	 * Stabilization logic.
	 * 
	 * Called by background thread & in join protocol by joining node (as new pred)
	 */
	public synchronized boolean stabilize() throws Error, Failed {
		NodeInfo info = getNodeInfo();
		NodeInfo succ = getSucc();
		if (info.equals(succ)) return true;
		
		NodeInfo predOfSucc = getPred(succ);
		if (predOfSucc == null) {
			/*
			 * Successor's predecessor is not set, so we will become pred.
			 * Notify succ that we believe we are its predecessor. 
			 * Expect transfer of bindings from succ as ack.
			 * 
			 * Do the Web service call.
			 */
			debug("Joining succ with null pred.");
			TableRep db = client.notify(succ, info);
			return notifyContinue(db);
		} else if (inInterval(predOfSucc.id, info.id, succ.id, false)) {
			setSucc(predOfSucc);
			/*
			 * Successor's predecessor should be our predecessor.
			 * Notify pred of succ that we believe we are its predecessor. 
			 * Expect transfer of bindings from succ as ack.
			 * 
			 * Do the Web service call.
			 */
			debug("Joining succ as new, closer pred.");
			TableRep db = client.notify(succ, info);
			return notifyContinue(db);
		} else if (inInterval(info.id, predOfSucc.id, succ.id, false)) {
			/*
			 * Has some node inserted itself between us and the successor?
			 * This could happen due to a race condition between setting
			 * our successor pointer and notifying the successor.
			 */
			debug("Notifying succ that we are its pred.");
			TableRep db = client.notify(succ, info);
			return notifyContinue(db);
		} else {
			/*
			 * We come here if we are already the predecessor, so no change.
			 */
			return false;
		}
	}

	// WebMethod
	public synchronized TableRep notify(NodeInfo cand) throws Error {
		/*
		 * Node cand ("candidate") believes it is our predecessor.
		 */
		NodeInfo pred = getPred();
		NodeInfo info = getNodeInfo();
		if (pred == null || inInterval(cand.id, pred.id, info.id, false)) {
			setPred(cand);
			/*
			 * If this is currently a single-node network, close the loop
			 * by setting our successor to our pred.  Thereafter the network
			 * should update automatically as new predecessors are detected by
			 * old predecessors.
			 */
			if (getSucc().equals(info)) {
				setSucc(cand);
			}
			/*
			 * Transfer our bindings to our pred.  They will take over
			 * some bindings and back up the others for us.
			 */
			TableRep db = transferBindings(cand.id);
			debug("Transferring bindings to node id="+cand.id);
			return db;
		} else {
			/*
			 * Null indicates that we did not accept new pred.
			 * This may be because cand==pred.
			 */
			return null;
		}
	}
	
	/*
	 * Process the result of a notify to a potential successor node.
	 */
	protected boolean notifyContinue(TableRep db) throws Error {
		if (db == null) {
			/*
			 * We are already the predecessor?
			 */
			return false;
		} else {
			/*
			 * db is bindings we take from the successor.
			 */
			try {
				NodeInfo info = getNodeInfo();
				/*
				 * Install succ bindings as our local state,
				 * and backup bindings of succ in case it fails.
				 */
				state.installBindings(db, info.id);
				state.backupBindings(db, info.id);
				return true;
			} catch (RemoteException e) {
				severe("NotifyContinue: RMI exception: " + e);
				throw new Error(e);
			}
		}
	}
	
	/*
	 * Transfer our bindings to a new predecessor (some to be backed up)
	 */
	protected TableRep transferBindings(int predId) throws Error {
		try {
			TableRep db = state.extractBindings();
			state.dropBindings(predId);
			return db;
		} catch (RemoteException e) {
			severe("TransferBindings: RMI error: " + e);
			throw new Error(e);
		}
	}

	private int next = 0;

	/*
	 * Periodic refresh of finger table based on changed successor.
	 */
	protected void fixFinger() throws Error {
		next = (next + 1) % IRouting.NFINGERS;
		/*
		 * Compute offset = 2^next
		 */
		int offset = 1;
		for (int i = 0; i < next; i++)
			offset = offset * 2;
		/*
		 * finger[next] = findSuccessor (n + 2^next)
		 */
		int nextId = (getNodeInfo().id + offset)%IRouting.NKEYS;
		try {
			setFinger(next, findSuccessor(nextId));
		} catch (Failed e) {
			warning("FixFinger: findSuccessor("+nextId+") failed.");
		}
	}

	/*
	 * Speed up the finger table refresh.
	 */
	// Called by background thread.
	public void fixFingers(int ntimes) throws Error {
		for (int i = 0; i < ntimes; i++) {
			fixFinger();
		}
	}

	/*
	 * Check if the predecessor has failed.
	 */
	// Called by background thread.
	public void checkPredecessor() throws Error {
		/*
		 * Ping the predecessor node by asking for its successor.
		 */
		NodeInfo pred = getPred();
		if(pred != null) {
			try {
				getSucc(pred);
			} catch (Failed e) {
				info("CheckPredecessor: Predecessor has failed (id=" + pred.id + ")");
				setPred(null);
			}
		}
	}

	/*
	 * Get the values under a key at the specified node. If the node is the
	 * current one, go to the RMI server.
	 */
	protected String[] get(NodeInfo n, String k) throws Error, Failed {
		if (n.addr.equals(info.addr)) {
			try {
				return this.get(k);
			} catch (Invalid e) {
				severe("Get: invalid internal inputs: " + e);
				throw new Error(e);
			}
		} else {
			/*
			 * Retrieve the bindings at the specified node. 
			 * 
			 * TODO: Do the Web service call.
			 */
			// just do a Web service call, no RMI following, --Ranger
			List<String> keys = null;
			GenericType<List<String>> genericType = new GenericType<List<String>>(){};
			Client c = Client.create();
			UriBuilder ub = UriBuilder.fromUri(n.addr);
			URI getPath = ub.queryParam("key", k).build();
			WebResource r = c.resource(getPath);
			ClientResponse response = r.get(ClientResponse.class);
			if (response.getStatus() >= 300) {
				throw new DHTBase.Failed("GET ?key=KEY");
			} else {
				//?how to retrieve a List<String> from Web service
				keys = (List<String>)response.getEntity(genericType);
			}
			ArrayList<String> keyArray = new ArrayList<String>();
			String[] result = (String[]) keys.toArray();
			return result;
		}
	}

	/*
	 * Retrieve values under the key at the current node.
	 */
	// WebMethod
	public String[] get(String k) throws Error, Invalid {
		try {
			return state.get(k);
		} catch (RemoteException e) {
			severe("Get: RMI error: " + e);
			throw new Error(e);
		}
	}

	/*
	 * Add a value under a key.
	 */
	public void add(NodeInfo n, String k, String v) throws Error, Failed {
		if (n.addr.equals(info.addr)) {
			try {
				add(k, v);
			} catch (Invalid e) {
				severe("Add: invalid internal inputs: " + e);
				throw new Error(e);
			}
		} else {
			/*
			 * TODO: Do the Web service call.
			 */
			Client c = Client.create();
			UriBuilder ub = UriBuilder.fromUri(n.addr);
			URI putPath = ub.queryParam("key", k).queryParam("val", v).build();
			WebResource r = c.resource(putPath);
			ClientResponse response = r.put(ClientResponse.class);
			if (response.getStatus() >= 300) {
				throw new DHTBase.Failed("PUT ?key=KEY&val=VAL");
			}
		}
	}
	
	/*
	 * Store a value under a key at the local node.
	 */
	// WebMethod
	public void add(String k, String v) throws Error, Invalid {
		try {
			/*
			 * Validate that this binding can be stored here.
			 */
			int kid = DHTBase.NodeKey(k);
			NodeInfo info = getNodeInfo();
			NodeInfo pred = getPred();

			if (pred != null && inInterval(kid, pred.id, info.id, true)) {
				/*
				 * This node covers the interval in which k should be stored.
				 */
				state.add(k, v);
			} else if (pred == null && info.equals(getSucc())) {
				/*
				 * Single-node network.
				 */
				state.add(k, v);
			} else if (pred == null && info.equals(getSucc())) {
				severe("Add: predecessor is null but not a single-node network.");
			} else {
				throw new Invalid("Invalid key: "+k+" (id="+kid+")");
			}
		} catch (RemoteException e) {
			severe("Add: RMI error: " + e);
			throw new Error(e);
		}		
	}

	/*
	 * Delete value under a key.
	 */
	public void delete(NodeInfo n, String k, String v) throws Error,
			Failed {
		if (n.addr.equals(info.addr)) {
			try {
				delete(k, v);
			} catch (Invalid e) {
				severe("Delete: invalid internal inputs: " + e);
				throw new Error(e);
			}
		} else {
			/*
			 * TODO: Do the Web service call.
			 */
			Client c = Client.create();
			UriBuilder ub = UriBuilder.fromUri(n.addr);
			URI deletePath = ub.queryParam("key", k).queryParam("val", v).build();
			WebResource r = c.resource(deletePath);
			ClientResponse response = r.delete(ClientResponse.class);
			if (response.getStatus() >= 300) {
				throw new DHTBase.Failed("DELETE ?key=KEY&val=VAL");
			}
		}
	}

	/*
	 * Delete value under a key at the local node.
	 */
	// WebMethod
	public void delete(String k, String v) throws Error, Invalid {
		try {
			state.delete(k, v);
		} catch (RemoteException e) {
			severe("Delete: RMI error: " + e);
			throw new Error(e);
		}
	}
	
	
	/*
	 * These operations perform the CRUD logic at the network level.
	 */
	
	/*
	 * Store a value under a key in the network.
	 */
	public void addNet (String skey, String v) throws Error, Failed {
		int id = NodeKey(skey);
		NodeInfo succ = this.findSuccessor(id);
		add(succ, skey, v);
	}

	/*
	 * Store a value under a key in the network.
	 */
	public String[] getNet (String skey) throws Error, Failed {
		int id = NodeKey(skey);
		NodeInfo succ = this.findSuccessor(id);
		return get(succ, skey);
	}
	
	/*
	 * Delete a value under a key in the network.
	 */
	public void deleteNet (String skey, String v) throws Error, Failed {
		int id = NodeKey(skey);
		NodeInfo succ = this.findSuccessor(id);
		delete(succ, skey, v);
	}

	
	/*
	 * Join logic.
	 */
	
	/*
	 * Insert this node into the DHT identified by uri.
	 */
	public void join(String uri) throws Error, Failed, Invalid {
		setPred(null);
		NodeInfo info = getNodeInfo();
		NodeInfo succ;
		/*
		 * TODO: Do a web service call to the node identified by "uri"
		 * and find the successor of info.id, then setSucc(succ).
		 * Make sure to clear any local bindings first of all, to maintain
		 * consistency of the ring.  We start afresh with the bindings
		 * that are transferred from the new successor.
		 */
		// findSuccessor, use the method definded before
		URI remoteUri = URI.create(uri);
		succ = findSuccessor(remoteUri, info.id);
		setSucc(succ);
		//?or call a remote setSucc(succ)?
		/*Client c = Client.create();
		UriBuilder ub = UriBuilder.fromUri(uri).path("setSucc");
		URI putPath = ub.build();
		WebResource r = c.resource(putPath);
		r.setProperty("Succ", "application/xml");
		// learned from WebClient.notify(NodeInfo node, NodeInfo thisNode) 
		//? how to pass xml consumes in Web service call?, Ranger
		JAXBElement<NodeInfo> thisRep = NodeResource.nodeInfoRep(succ);
		//? is it necessary to remain the following line to get ClientResponse?
		ClientResponse response = r.put(ClientResponse.class, thisRep);
		if (response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /setSucc");
		}*/
	}
	
	
	/*
	 * State display operations for CLI.
	 */

	public void display() {
		try {
			state.display();
		} catch (RemoteException e) {
			severe("Display: RMI error: " + e);
			e.printStackTrace();
		}
	}

	public void routes() {
		try {
			routing.routes();
		} catch (RemoteException e) {
			severe("Routes: RMI error: " + e);
			e.printStackTrace();
		}
	}

	/*
	 * Set the current node to be failed.
	 */
	public void setFailed() {
		try {
			state.setFailed();
		} catch (RemoteException e) {
			severe("SetFailed: RMI error: " + e);
			e.printStackTrace();
		}
	}

}
