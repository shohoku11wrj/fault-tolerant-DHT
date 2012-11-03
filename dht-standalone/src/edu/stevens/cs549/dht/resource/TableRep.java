package edu.stevens.cs549.dht.resource;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

import edu.stevens.cs549.dht.activity.DHTBase;
import edu.stevens.cs549.dht.activity.NodeInfo;


@XmlRootElement(name="table")
// @XmlAccessorType(XmlAccessType)
public class TableRep implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	Logger log = Logger.getLogger(TableRep.class.getCanonicalName());
	
	@XmlElement
	public TableRow[] entry;
	
	/*
	 * Node id needed for transferring bindings to predecessor.
	 */
	@XmlElement
	public NodeInfo info;
	
	/*
	 * Successor (transmitted to pred for backup).
	 */
	@XmlElement
	public NodeInfo succ;
	
	public TableRep() { 
		/* 
		 * JAXB unmarshals empty entries as null.
		 */
		entry = new TableRow[0];
	}
	
	public TableRep(NodeInfo info, NodeInfo succ, int nrecs) {
		entry = new TableRow[nrecs];
//		this.info = new NodeInfoRep(info);
//		this.succ = new NodeInfoRep(succ);
		this.info = info;
		this.succ = succ;
	}
	
	public NodeInfo getInfo() throws DHTBase.Error {
//		return info.getNodeInfo();
		return info;
	}
	
	public NodeInfo getSucc() throws DHTBase.Error {
//		return succ.getNodeInfo();
		return succ;
	}

}
