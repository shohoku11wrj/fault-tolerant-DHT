package edu.stevens.cs549.dht.state;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import edu.stevens.cs549.dht.activity.DHTBase;
import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.resource.TableRep;
import edu.stevens.cs549.dht.resource.TableRow;

/**
 * 
 * @author dduggan
 */
public class State extends UnicastRemoteObject implements IState, IRouting {

	static final long serialVersionUID = 0L;

	public static Logger log = Logger
			.getLogger("edu.stevens.cs.cs549.dht.state.State");

	protected NodeInfo info;

	private boolean failed = false;

	public State(NodeInfo info) throws RemoteException {
		super();
		this.info = info;
		this.predecessor = null;
		this.successor = info;

		this.finger = new NodeInfo[NKEYS];
		for (int i = 0; i < NKEYS; i++) {
			finger[i] = info;
		}

	}

	/*
	 * Use the "failed" flag to simulate node failures.
	 */
	public void setFailed() throws RemoteException {
		failed = true;
	}

	protected void checkFailed() throws Error {
		if (failed)
			throw new Error("Simulated internal error.");
	}

	/*
	 * Get the info for this DHT node.
	 */
	public NodeInfo getNodeInfo() throws RemoteException {
		return info;
	}

	/*
	 * Local table operations.
	 */
	private Persist.Table dict = Persist.newTable();

	private Persist.Table backup = Persist.newTable();

	private NodeInfo backupSucc = null;

	public synchronized String[] get(String k) throws RemoteException, Error {
		checkFailed();
		List<String> vl = dict.get(k);
		if (vl == null) {
			return null;
		} else {
			String[] va = new String[vl.size()];
			return vl.toArray(va);
		}
	}

	public synchronized void add(String k, String v) throws RemoteException,
			Error {
		checkFailed();
		List<String> vl = dict.get(k);
		if (vl == null) {
			vl = new ArrayList<String>();
		}
		vl.add(v);
		dict.put(k, vl);
	}

	public synchronized void delete(String k, String v) throws RemoteException,
			Error {
		checkFailed();
		List<String> vs = dict.get(k);
		if (vs != null)
			vs.remove(v);
	}

	public synchronized void clear() throws RemoteException, Error {
		dict.clear();
	}

	/*
	 * Operations for transferring state between predecessor and successor.
	 */

	/*
	 * Successor: Extract the bindings from the successor node.
	 */
	public synchronized TableRep extractBindings(int predId)
			throws RemoteException, Error {
		checkFailed();
		return Persist.extractBindings(predId, info, successor, dict);
	}

	public synchronized TableRep extractBindings() throws RemoteException,
			Error {
		checkFailed();
		return Persist.extractBindings(info, successor, dict);
	}

	/*
	 * Successor: Drop the bindings that are transferred to the predecessor.
	 */
	public synchronized void dropBindings(int predId) throws RemoteException,
			Error {
		checkFailed();
		Persist.dropBindings(dict, predId, getNodeInfo().id);
	}

	/*
	 * Predecessor: Install the transferred bindings.
	 */
	public synchronized void installBindings(TableRep db) throws Error {
		checkFailed();
//		dict = Persist.installBindings(db);
		// TODO, Ranger
		dict = Persist.installBindings(dict, db);
	}

	/*
	 * Predecessor: Back up bindings from the successor.
	 */
	public synchronized void backupBindings(TableRep db) throws Error {
		checkFailed();
		backup = Persist.backupBindings(db);
//		backupSucc = db.getSucc(); // TODO, Uncomment, Ranger, Because backupSucc(TableRep db) have not been called.
//		System.out.println("/-- in State.backipBindings: Succ's succ="+backupSucc.id+". --/");
	}

	public synchronized void backupSucc(TableRep db) throws Error {
		checkFailed();
		backupSucc = db.getSucc();
		System.out.println("BackupSucc: succ="+db.getSucc());
	}

	/*
	 * A never-used operation for storing state in a file.
	 */
	public synchronized void backup(String filename) throws IOException,
			RemoteException, Error {
		checkFailed();
		Persist.save(info, successor, dict, filename);
	}

	public synchronized void reload(String filename) throws IOException,
			RemoteException, Error {
		checkFailed();
		dict = Persist.load(filename);
	}

	public synchronized void display() throws RemoteException {
		PrintWriter wr = new PrintWriter(System.out);
		Persist.display(dict, wr);
		System.out.println("---- Show backup bindings: ");
		Persist.display(backup, wr);
	}
	
	// TODO, test
	public synchronized void display(Persist.Table tb) throws RemoteException {
		PrintWriter wr = new PrintWriter(System.out);
		System.out.println("Display table test...");
		Persist.display(tb, wr);
	}

	/*
	 * Routing operations.
	 */

	private NodeInfo predecessor = null;
	private NodeInfo successor = null;

	private NodeInfo[] finger;

	public synchronized void setPred(NodeInfo pred) throws RemoteException,
			Error {
		checkFailed();
		predecessor = pred;
	}

	public NodeInfo getPred() throws RemoteException, Error {
		checkFailed();
		return predecessor;
	}

	public synchronized void setSucc(NodeInfo succ) throws RemoteException,
			Error {
		checkFailed();
		successor = succ;
	}

	public NodeInfo getSucc() throws RemoteException, Error {
		checkFailed();
		return successor;
	}

	public void setFinger(int i, NodeInfo info) throws RemoteException, Error {
		checkFailed();
		/*
		 * TODO: Set the ith finger.
		 */
		finger[i] = info;
	}

	public NodeInfo getFinger(int i) throws RemoteException, Error {
		checkFailed();
		/*
		 * TODO: Get the ith finger.
		 */
		return this.finger[i];
	}

	public synchronized NodeInfo closestPrecedingFinger(int id) throws Error, RemoteException {
		checkFailed();
		/*
		 * TODO: Get closest preceding finger for id, to continue search at that
		 * node. Hint: See DHTBase.inInterval()
		 * 
		 * ********************************************** -m: number of bits in
		 * the key/node identifiers -finger table: each node n maintains a
		 * routing table with at most m entries
		 * 
		 * (non-Javadoc)
		 * 
		 * @see
		 * edu.stevens.cs549.dht.activity.IDHTResource#closestPrecedingFinger
		 * (int) n.closest_preceding_finger(id) //return closest finger
		 * preceding id for i=m downto 1 if(finger[i].node ∈(n, id)) return
		 * finger[i].node; return n;
		 * **********************************************
		 */
		for (int i = IRouting.NFINGERS; i > 0; i--) {
			int UB = finger[i].id % IRouting.NKEYS;
			int LB = finger[i-1].id % IRouting.NKEYS;
			if (id == UB)
				return getFinger(i-1);
			if (LB < UB) {
				UB = (UB - LB);
				LB = (id - LB);
				if (0 <= LB && LB < UB) { // TODO, or (0 <= LB && LB < UB)
					return getFinger(i-1);
				}
			} else if (UB < LB) {
				UB = (UB + (IRouting.NKEYS - LB)) % IRouting.NKEYS;
				LB = (id + (IRouting.NKEYS - LB)) % IRouting.NKEYS;
				if (0 <= LB && LB < UB) { // TODO, or (0 <= LB && LB < UB)
					return getFinger(i-1);
				}
			}
		}
		return this.info;
	}

	public synchronized void routes() {
		PrintWriter wr = new PrintWriter(System.out);
		wr.println("Predecessor: " + predecessor);
		wr.println("Successor  : " + successor);
		wr.println("Succ's Succ: " + backupSucc);
		wr.println("Fingers:");
		wr.printf("%7s  %3s  %s\n", "Formula", "Key", "Succ");
		wr.printf("%7s  %3s  %s\n", "-------", "---", "----");
		for (int i = 0, exp = 1; i < IRouting.NFINGERS; i++, exp = 2 * exp) {
			wr.printf(" %2d+2^%1d  %3d  [id=%2d,uri=%s]%n", info.id, i,
					(info.id + exp) % IRouting.NKEYS, finger[i].id,
					finger[i].addr);
		}
		wr.flush();
	}
	
	// TODO, Ranger
	
	public NodeInfo getBackupSucc() throws RemoteException, Error {
		checkFailed();
		return backupSucc;
	}
	
	public void setBackupSucc(NodeInfo succOfSucc) {
		this.backupSucc = succOfSucc;
	}
	
	public synchronized void addBackup(String k, String v) throws RemoteException, Error {
		checkFailed();
		List<String> vl = backup.get(k);
		if (vl == null) {
			vl = new ArrayList<String>();
		}
		vl.add(v);
		backup.put(k, vl);
	}
	
	public synchronized void delBackup(String k, String v) throws RemoteException, Error {
		checkFailed();
		List<String> vs = backup.get(k);
		if (vs != null)
			vs.remove(v);
	}
	
	public synchronized TableRep failoverBindings() throws RemoteException, Error{
		checkFailed();
		// add backup to dict
		Persist.Table resultTb=Persist.newTable();
		
		Enumeration<String> keys = backup.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement();
			List<String> v = backup.get(k);
			dict.put(k, v);
			resultTb.put(k, v);
		}
		// clear backup
		backup.clear();

		// return current dict to notifyFailover
		// display();
		display(resultTb);
		TableRep result = Persist.extractBindings(info, successor, resultTb);
		return result;
		
	}

}
