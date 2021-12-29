package utility;

import java.util.ArrayList;
import java.util.List;


import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.Task;

import objects.HouseholdCarAllocation;



public class ParallelHelper {

	private static Logger taskLogger = Logger.getLogger( "taskLogger" );
    
    public static final ParallelHelper INSTANCE = new ParallelHelper(); 
	
	private  ParallelHelper() {
	}
	
	public static final SolveDistributedIf PARALLEL_HELPER_DISTRIBUTER = new Distributer();
	
	
	private static class Distributer implements SolveDistributedIf {
	
	    public List<Object> solveDistributed( String modelName, DistributableIf<AbstractTask<?>> task, JPPFClient myClient, DataProvider dataProvider, int firstTask, int lastTask, int packetSize ) {
	    	
			List<Object> resultList = new ArrayList<Object>();
			
			
	    	try {
	
		        // create a JPPF job
		        JPPFJob myJob = new JPPFJob();
		        myJob.setDataProvider( dataProvider );
				        
		        // give this job a name that we can use to monitor and manage it.
		        myJob.setName( modelName );
	
		        List<int[]> packetList = getTaskRanges( firstTask, lastTask, packetSize );	        
		        
		    	for ( int[] list : packetList ) {	    		
	
			        // add a task to the job.
		    		AbstractTask<?> myTask = (AbstractTask<?>)task.newInstance( list[0], list[1] );
		    		myTask.setId( list[0] + ":" + list[1] );
		    		myJob.add( myTask );    
	
		    	}
			    	
				myJob.setBlocking( false );

				myJob.addJobListener( new MyJobListener() );
				
			        
			    myClient.submitJob( myJob ); 
			    List<Task<?>> results = myJob.awaitResults();

				taskLogger.info( "" );
				taskLogger.info( "" );
				taskLogger.info( modelName + " tasks:" );
			    
			    // process the results
			    for ( Task<?> resultTask: results ) {
		
			    	if ( resultTask == null ) {
			    		System.out.println( "returned task was null" );
			    		new RuntimeException().printStackTrace();
			    		System.exit(-1);
			    	}
			    	
			        // process the task resultBundle here ...
			    	try {
			    		Object resultObject = resultTask.getResult();
			    		List<Object> resultBundle = null;
			    		try {
			    			resultBundle = (List<Object>)resultObject;
							String resultName = (String)resultBundle.get( 0 );
							Object result = resultBundle.get( 1 );

							if ( result != null ) {
								List<Integer> runtimes =  (List<Integer>)resultBundle.get( 2 );
						        //int numItersIntegrizing = (int)resultBundle.get( 3 );
						        //int numItersOptimalSolution = (int)resultBundle.get( 4 );
								//taskLogger.info( modelName + " task completed: " + resultName + ", runtimes:" + Parsing.getIntListAsString(runtimes) + " seconds, numItersIntegrizing=" + numItersIntegrizing + ", numItersOptimalSolution=" + numItersOptimalSolution );
								taskLogger.info( modelName + " task completed: " + resultName + " in " + runtimes.get(3) + " seconds." );

								resultList.add( result );
							}
							else {
				    			System.out.println( "null resultBundle" );
								System.exit( -1 );
							}
							
			    		}
			    		catch(ClassCastException e) {
			    			System.out.println( "no resultObject, message = " + (String)resultObject );
							System.exit( -1 );
			    		}
						
			    	}
			    	catch( Exception e ) {
			    		System.out.println( "Exception caught in returned task:" );
			    		e.printStackTrace();
			    	}
			    	
		        }
			    
	        }
	        catch ( Exception e ) {
	        	System.out.println( "Exception caught in client running JPPF job for " + modelName + "." );
	    		e.printStackTrace();
	        }
	
	    	taskLogger.info( "done with " + modelName + "." );
	        
	        return resultList;
	
	    }

	    
	    
	    private List<int[]> getTaskRanges( int firstTask, int lastTask, int packetSize ) {

	        List<int[]> startEndIndexList = new ArrayList<int[]>();

	        int startIndex = 0;
	        int endIndex = 0;
	        int numberOfElements = lastTask - firstTask + 1;

	        if ( numberOfElements == 1 ) {
	        	
	            int[] startEndIndices = new int[2];
	            startEndIndices[0] = firstTask + startIndex;
	            startEndIndices[1] = firstTask + endIndex;
	            startEndIndexList.add(startEndIndices);

	        }
	        else {
	        	
		        while( endIndex < numberOfElements - 1 ) {
		        	
		            endIndex = startIndex + packetSize - 1;
		            if ( endIndex + packetSize > numberOfElements )
		                endIndex = numberOfElements - 1;

		            int[] startEndIndices = new int[2];
		            startEndIndices[0] = firstTask + startIndex;
		            startEndIndices[1] = firstTask + endIndex;
		            startEndIndexList.add(startEndIndices);

		            startIndex += packetSize;
		        }

	        }

	        return startEndIndexList;

	    }
	    
	}
		
}
