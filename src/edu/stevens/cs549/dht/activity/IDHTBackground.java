package edu.stevens.cs549.dht.activity;

import edu.stevens.cs549.dht.activity.DHTBase.Error;
import edu.stevens.cs549.dht.activity.DHTBase.Failed;

/*
 * There are the DHT operations performed in the background.
 */
public interface IDHTBackground {
	
	/*
	 * Boolean result of stabilize tells us if successor node
	 * has accepted us as its predecessor.  
	 */
	public boolean stabilize() throws Error, Failed;
	
	/*
	 * If our predecessor has failed, set the pred pointer to nil.
	 */
	public void checkPredecessor() throws Error;
		
	/*
	 * Fix finger pointers (based on changes to successor pointers).
	 */
	public void fixFingers(int ntimes) throws Error, Failed;
	
	/*// TODO
	// ADD, Ranger, Nov 25
	
	 * If our successor has failed, set the succ pointer to succ's succ.
	 
	public void checkSuccessor() throws Error;*/

}
