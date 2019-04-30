package utility;

import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListenerAdapter;
import org.jppf.node.protocol.Task;

public class MyJobListener extends JobListenerAdapter {

	private static final int INCREMENT = 10;
	
	private int totalSubmittedTasks = 0;
	private int totalExecutedTasks = 0;
	private int cumExecutedTasks = 0;
	private int cumIncrement = 0;
	
	private Logger logger = Logger.getLogger( MyJobListener.class );
	
	
	
	@Override
	public synchronized void jobStarted( JobEvent event ) {
		JPPFJob job = event.getJob();
		cumExecutedTasks = 0;
		cumIncrement = INCREMENT;
		totalSubmittedTasks = job.getJobTasks().size();
		logger.info( job.getName() + " started with " + totalSubmittedTasks + " tasks." );
	}
	
	@Override
	public synchronized void jobReturned( JobEvent event ) {
		List<Task<?>> tasks = event.getJobTasks();
		// add the number of tasks received
		totalExecutedTasks += tasks.size();

		for ( int n=cumExecutedTasks; n <= totalExecutedTasks; n++ ) {
			int cumPercent = (int)( 100 * n / totalSubmittedTasks );
			if ( cumPercent >= cumIncrement) {
				logger.info( "\t" + ((int)(cumPercent/INCREMENT))*INCREMENT  + "% of tasks completed." );
				cumIncrement += INCREMENT;
				break;
			}
		}
		
		cumExecutedTasks = totalExecutedTasks;

	}
	
}
