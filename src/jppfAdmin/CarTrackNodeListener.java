package jppfAdmin;

import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListener;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.node.protocol.JobMetadata;


public class CarTrackNodeListener implements NodeLifeCycleListener {

	@Override
	public void nodeStarting(NodeLifeCycleEvent event) {
    	System.loadLibrary("jniortools");
	}
		
	public void nodeEnding(NodeLifeCycleEvent event) {
	}
	
	public void jobStarting(NodeLifeCycleEvent event) {		
	}

	public void jobEnding(NodeLifeCycleEvent event) {
	}

	public void jobHeaderLoaded(NodeLifeCycleEvent event) {
	}

	public void beforeNextJob(NodeLifeCycleEvent arg0) {
	}
	
}
