package utility;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;

public interface SolveDistributedIf {
	public List<Object> solveDistributed( String modelName, DistributableIf<AbstractTask<?>> task, JPPFClient myClient, DataProvider dataProvider, int firstTask, int lastTask, int packetSize );
}
