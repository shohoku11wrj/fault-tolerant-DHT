package edu.stevens.cs549.dht.state;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import edu.stevens.cs549.dht.activity.DHTBase;
import edu.stevens.cs549.dht.activity.NodeInfo;
import edu.stevens.cs549.dht.resource.TableRow;
import edu.stevens.cs549.dht.resource.TableRep;

public class Persist {

	/*
	 * This class provides operations for persisting the contents of a DHT node
	 * (key-value pairs only) to disk or network.
	 * 
	 * We use JAXB for marshaling hash table contents as XML.
	 */

	protected static Logger log = Logger.getLogger("edu.stevens.cs549.dht.state.Persist");

	private static void severe(String msg) {
		log.severe(msg);
	}
	
	public static class Table extends Hashtable<String,List<String>> {
		private static final long serialVersionUID = 1L; 
	}

	public static Table newTable() {
		return new Table();
	}

 
	/********************************************************************************/
	/* Operations for mapping from TableRep to Hashtable                             */
	
	protected synchronized static void fromTableDB(Visitor v, TableRep db) {
		for (TableRow r : db.entry) {
			v.visit(r.key, r.vals);
		}
	}
	
	protected static Table fromTableDBPred(TableRep db, int predId) {
		Table table = newTable();
		fromTableDB(new PredInsert(table, predId, db.info.id), db);
		return table;
	}
	
	protected static Table fromTableDBSucc(TableRep db, int predId) {
		Table table = newTable();
		fromTableDB(new SuccInsert(table, predId, db.info.id), db);
		return table;
	}
	
	protected static Table fromTableDBAll(TableRep db) {
		Table table = newTable();
		fromTableDB(new AllInsert(table), db);
		return table;
	}
	
	protected static abstract class Visitor {
		/*
		 * Interface for processing a TableRep rwo.
		 */
		public abstract void visit (String k, String[] v);
	}
	
	protected static abstract class Insert extends Visitor {
		/*
		 * Logic for adding a TableRep row to a hash table.
		 * Relies on filter logic defined in concrete subclasses.
		 */
		protected Table table;
		public Insert(Table ht) { table=ht; }
		protected abstract boolean filter(String k);
		public void visit(String k, String[] v) {
			if (filter(k)) {
				List<String> xs = new ArrayList<String>();
				for (String s : v) {
					xs.add(s);
				}
				table.put(k, xs);
			}
		}
	}
	
	protected static class PredInsert extends Insert {
		/*
		 * Transferring bindings from successor to predecessor.
		 * Logic for including bindings that are in the predecessor.
		 */
		protected int predId, id;
		protected boolean filter(String k) {
			// return DHTBase.NodeKey(k) <= predId;
			return !DHTBase.inInterval(DHTBase.NodeKey(k), predId, id);
		}
		public PredInsert(Table ht, int predId, int id) { 
			super(ht);
			this.predId = predId;
			this.id = id;
		}
	}
	
	protected static class SuccInsert extends Insert {
		/*
		 * Transferring bindings from successor to predecessor.
		 * Logic for including bindings that are in the successor.
		 */
		protected int predId, id;
		protected boolean filter(String k) {
			// return DHTBase.NodeKey(k) > predId;
			return DHTBase.inInterval(DHTBase.NodeKey(k), predId, id);
		}
		public SuccInsert(Table ht, int predId, int id) { 
			super(ht);
			this.predId = predId;
			this.id = id;
		}
	}
	
	protected static class AllInsert extends Insert {
		protected boolean filter(String k) {
			return true;
		}
		public AllInsert(Table ht) {
			super(ht);
		}
	}
	
	/*******************************************************************************/
	/* Operations for mapping from Hashtable to TableDB                            */
	
	/*
	 * ADD, Ranger, Oct 26
	 */
	public interface Filter {
		public boolean check(String k);
	}
	
	protected synchronized static TableRep toTableDB(NodeInfo info, NodeInfo succ, Table table) {
		TableRep db = new TableRep(info, succ, table.size());

		Enumeration<String> keys = table.keys();
		for (int i = 0; keys.hasMoreElements(); i++) {
			String k = keys.nextElement();
			List<String> vl = table.get(k);
			String[] al = new String[vl.size()];
			vl.toArray(al);
			db.entry[i] = new TableRow(k,al);
		}
		return db;
	}	

	/*******************************************************************************/
	/* Applications                                                                */
	
	public static Table load(String filename) {
		try {
			JAXBContext context = JAXBContext.newInstance(TableRep.class);
			Unmarshaller um = context.createUnmarshaller();
			InputStream is = new FileInputStream(filename);
			TableRep db = (TableRep) um.unmarshal(is);
			is.close();

			return fromTableDBAll(db);
		} catch (JAXBException e) {
			severe("Load: JAXB error: " + e);
		} catch (FileNotFoundException e) {
			severe("Load: File not found: " + filename);
		} catch (IOException e) {
			severe("Load: IO Exception closing " + filename);
		}
		return null;

	}
	
	/*
	 * ADD, Ranger, Oct 16
	 */
	protected static Filter all = new Filter() {
		public boolean check (String k) { return true; }
	};

	public static void save(NodeInfo info, NodeInfo succ, Table table, String filename) {
		try {
			OutputStream os = new FileOutputStream(filename);
			TableRep db = toTableDB(info, succ, table);
			save(info, succ, db, os);
			os.close();
		} catch (FileNotFoundException e) {
			severe("Save: File not found: " + filename);
		} catch (IOException e) {
			severe("Save: IO Exception closing " + filename);
		}
	}
	
	public static void save(NodeInfo info, NodeInfo succ, TableRep db, OutputStream os) {
		try {
			JAXBContext context = JAXBContext.newInstance(TableRep.class);
			Marshaller m = context.createMarshaller();
			m.marshal(db, os);
		} catch (JAXBException e) {
			severe("Save: JAXB error: " + e);
		} 
	}
	
	public static String displayVals(String[] vs) {
		String vals = "{";
		if (vs.length > 0) {
			for (int i = 0; i < vs.length - 1; i++) {
				vals += vs[i];
				vals += ",";
			}
			vals += vs[vs.length - 1];
		}
		return vals+"}";
	}

	public static String displayVals(List<String> vs) {
		String vals = "{";
		if (vs.size() > 0) {
			for (int i = 0; i < vs.size() - 1; i++) {
				vals += vs.get(i);
				vals += ",";
			}
			vals += vs.get(vs.size() - 1);
		}
		return vals+"}";
	}

	public static void display(Table table, PrintWriter wr) {		
		if (table.size()==0) {
			wr.println("No entries.");
		} else {
			Enumeration<String> keys = table.keys();
			wr.printf("%9s  %2s  %s\n", "KEYSTRING", "ID", "VALUES");
			while (keys.hasMoreElements()) {
				String k = keys.nextElement();
				List<String> v = table.get(k);
				wr.printf("%9s  %2d  %s\n", k, DHTBase.NodeKey(k), displayVals(v));
			}
		}
		wr.flush();
	}
	
	public static TableRep extractBindings(NodeInfo info, NodeInfo succ, Table table) {
		return toTableDB(info, succ, table);
	}
	
	public static void dropBindings(Table table, int predId, int id) {
		/*
		 * Drop the bindings <= pred id from the successor node.
		 */
		Enumeration<String> keys = table.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement();
			// if (DHTBase.NodeKey(k) <= predId) {
			if (!DHTBase.inInterval(DHTBase.NodeKey(k), predId, id)) {
				table.remove(k);
			}
		}
	}
	
	public static Table installBindings(TableRep db, int predId) {
		/*
		 * Install bindings at predecessor with key value up to and including id.
		 */
		return fromTableDBPred(db, predId);
	}

	public static Table backupBindings(TableRep db, int predId) {
		/*
		 * Install backup bindings at the predecessor node.
		 */
		return fromTableDBSucc(db, predId);
	}

}
