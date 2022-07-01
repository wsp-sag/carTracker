package utility;

import java.util.ArrayList;
import java.util.List;


import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.Task;

import utility.ParallelHelper;



public class ParallelHelper {

	private static Logger logger = Logger.getLogger( ParallelHelper.class );
	//private static Logger taskLogger = Logger.getLogger( "taskLogger" );
    
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

				//taskLogger.info( "" );
				//taskLogger.info( "" );
				//taskLogger.info( modelName + " tasks:" );
			    
			    // process the results
			    for ( Task<?> resultTask: results ) {
		
			    	if ( resultTask == null ) {
			    		System.out.println( "returned job task list was null" );
			    		throw new RuntimeException();
			    	}
			    	else if ( ((Task<Object>)resultTask).getThrowable() != null ) {
		    			System.out.println( "task returned an exception instead of a valid result object." );
		    			((Task<Object>)resultTask).getThrowable().printStackTrace();
			    		throw new RuntimeException();
	    			}
			    	
			        // process the task resultBundle here ...
			    	try {
						List<Object> resultBundle = (List<Object>)resultTask.getResult();
						String resultName = (String)resultBundle.get( 0 );
						Object result = resultBundle.get( 1 );

						if ( result != null ) {
							List<Integer> runtimes =  (List<Integer>)resultBundle.get( 2 );
					        //int numItersIntegrizing = (int)resultBundle.get( 3 );
					        //int numItersOptimalSolution = (int)resultBundle.get( 4 );
							//taskLogger.info( modelName + " task completed: " + resultName + ", runtimes:" + Parsing.getIntListAsString(runtimes) + " seconds, numItersIntegrizing=" + numItersIntegrizing + ", numItersOptimalSolution=" + numItersOptimalSolution );
							resultList.add( result );
							//taskLogger.info( modelName + " task completed: " + resultName + " in " + runtimes.get(3) + " seconds." );
						}
						else {
							logger.error( "task result is null.  2nd element of resultBundle List<Object> is null." );
				    		throw new RuntimeException();
						}
							
			    	}
			    	catch( Exception e ) {
						logger.error( "Exception caught in client running JPPF job for " + modelName + ".", e );
			    		throw new RuntimeException();
			    	}
			    	
		        }
			    
	        }
	        catch ( Exception e ) {
	        	System.out.println( "Exception caught in client running JPPF job for " + modelName + "." );
	    		e.printStackTrace();
	        }
	
	    	logger.info( "done with " + modelName + "." );
	        
	        return resultList;
	
	    }
	    
	    
	    public List<Object> solveDistributedJobs( String modelName, DistributableIf<AbstractTask<?>> task, JPPFClient myClient, DataProvider dataProvider, int firstTask, int lastTask, int packetSize ) {
	    	
			List<Object> resultList = new ArrayList<Object>();

			int numJobs = 5;
			
	    	try {

		        List<int[]> packetList = getTaskRanges( firstTask, lastTask, packetSize );	        
	    			    			
		        MyJobListener jobListener = new MyJobListener();
		        List<JPPFJob> jobList = new ArrayList<>();
		        while ( jobList.size() < numJobs ) {
			        // create a JPPF job
		        	JPPFJob myJob = new JPPFJob();
			        jobList.add(myJob);
			        myJob.setDataProvider( dataProvider );
					        
			        // give this job a name that we can use to monitor and manage it.
			        myJob.setName( modelName + "_" + jobList.size() );
			        myJob.setBlocking( false );
					myJob.addJobListener( jobListener );
		        }
		        
		        
		        int packetCount = 0; 
		        for ( int[] list : packetList ) {	    		
	
		        	int jobNum = packetCount % numJobs;
		        	JPPFJob myJob = jobList.get(jobNum);
		    		
			        // add a task to the job.
		    		AbstractTask<?> myTask = (AbstractTask<?>)task.newInstance( list[0], list[1] );
		    		myTask.setId( list[0] + ":" + list[1] );
		    		myJob.add( myTask );    
	
		    		packetCount++;
		    	}
			    	

		        for ( JPPFJob job : jobList )
				    myClient.submitJob( job ); 

		        List<Task<?>> results = new ArrayList<>();
		        int n = 0;
		        for ( JPPFJob job : jobList ) {
				    List<Task<?>> myResults = job.awaitResults();
				    results.addAll(myResults);
				    logger.info( "job " + (n++) + " completed" );
		        }
			        

				//taskLogger.info( "" );
				//taskLogger.info( "" );
				//taskLogger.info( modelName + " tasks:" );
			    
			    // process the results
			    for ( Task<?> resultTask: results ) {
		
			    	if ( resultTask == null ) {
			    		System.out.println( "returned job task list was null" );
			    		throw new RuntimeException();
			    	}
			    	else if ( ((Task<Object>)resultTask).getThrowable() != null ) {
		    			System.out.println( "task returned an exception instead of a valid result object." );
		    			((Task<Object>)resultTask).getThrowable().printStackTrace();
			    		throw new RuntimeException();
	    			}
			    	
			        // process the task resultBundle here ...
			    	try {
						List<Object> resultBundle = (List<Object>)resultTask.getResult();
						String resultName = (String)resultBundle.get( 0 );
						Object result = resultBundle.get( 1 );

						if ( result != null ) {
							List<Integer> runtimes =  (List<Integer>)resultBundle.get( 2 );
					        //int numItersIntegrizing = (int)resultBundle.get( 3 );
					        //int numItersOptimalSolution = (int)resultBundle.get( 4 );
							//taskLogger.info( modelName + " task completed: " + resultName + ", runtimes:" + Parsing.getIntListAsString(runtimes) + " seconds, numItersIntegrizing=" + numItersIntegrizing + ", numItersOptimalSolution=" + numItersOptimalSolution );
							resultList.add( result );
							//taskLogger.info( modelName + " task completed: " + resultName + " in " + runtimes.get(3) + " seconds." );
						}
						else {
							logger.error( "task result is null.  2nd element of resultBundle List<Object> is null." );
				    		throw new RuntimeException();
						}
							
			    	}
			    	catch( Exception e ) {
						logger.error( "Exception caught in client running JPPF job for " + modelName + ".", e );
			    		throw new RuntimeException();
			    	}
			    	
		        }
			    
	        }
	        catch ( Exception e ) {
	        	System.out.println( "Exception caught in client running JPPF job for " + modelName + "." );
	    		e.printStackTrace();
	        }
	
	    	logger.info( "done with " + modelName + "." );
	        
	        return resultList;
	
	    }

	    
	    
	    public List<Object> solveDistributedLocal( String modelName, DistributableIf<AbstractTask<?>> task, List<JPPFClient> myClientList, DataProvider dataProvider, int firstTask, int lastTask, int packetSize ) {
	    	
			List<Object> resultList = new ArrayList<Object>();

			int numJobs = 10;
			
	    	try {

		        List<int[]> packetList = getTaskRanges( firstTask, lastTask, (int)(packetSize/numJobs) );	        
	    			    			
		        int packetCount = 0; 
		        MyJobListener jobListener = new MyJobListener();
		        List<JPPFJob> jobList = new ArrayList<>(); 
		        JPPFJob myJob = null;
		        
		        for ( int[] list : packetList ) {	    		
	
		        	int jobNum = packetCount / numJobs;
		        	
		    		if ( jobList.size() == 0 || jobNum == jobList.size() ) {
		    			
				        // create a JPPF job
				        myJob = new JPPFJob();
				        jobList.add(myJob);
				        myJob.setDataProvider( dataProvider );
						        
				        // give this job a name that we can use to monitor and manage it.
				        myJob.setName( modelName + "_" + jobList.size() );
				        myJob.setBlocking( false );
						myJob.addJobListener( jobListener );

		    		}
		    		
			        // add a task to the job.
		    		AbstractTask<?> myTask = (AbstractTask<?>)task.newInstance( list[0], list[1] );
		    		myTask.setId( list[0] + ":" + list[1] );
		    		myJob.add( myTask );    
	
		    		packetCount++;
		    	}
			    	

		        for ( int i=0; i < jobList.size(); i++ ) {
		        	JPPFClient myClient = myClientList.get(i % myClientList.size());
				    myClient.submitJob( jobList.get(i) );
				    
		        }

		        List<Task<?>> results = new ArrayList<>();
		        for ( JPPFJob job : jobList ) {
				    List<Task<?>> myResults = job.awaitResults();
				    results.addAll(myResults);
		        }
			        

				//taskLogger.info( "" );
				//taskLogger.info( "" );
				//taskLogger.info( modelName + " tasks:" );
			    
			    // process the results
			    for ( Task<?> resultTask: results ) {
		
			    	if ( resultTask == null ) {
			    		System.out.println( "returned job task list was null" );
			    		throw new RuntimeException();
			    	}
			    	else if ( ((Task<Object>)resultTask).getThrowable() != null ) {
		    			System.out.println( "task returned an exception instead of a valid result object." );
		    			((Task<Object>)resultTask).getThrowable().printStackTrace();
			    		throw new RuntimeException();
	    			}
			    	
			        // process the task resultBundle here ...
			    	try {
						List<Object> resultBundle = (List<Object>)resultTask.getResult();
						String resultName = (String)resultBundle.get( 0 );
						Object result = resultBundle.get( 1 );

						if ( result != null ) {
							List<Integer> runtimes =  (List<Integer>)resultBundle.get( 2 );
					        //int numItersIntegrizing = (int)resultBundle.get( 3 );
					        //int numItersOptimalSolution = (int)resultBundle.get( 4 );
							//taskLogger.info( modelName + " task completed: " + resultName + ", runtimes:" + Parsing.getIntListAsString(runtimes) + " seconds, numItersIntegrizing=" + numItersIntegrizing + ", numItersOptimalSolution=" + numItersOptimalSolution );
							resultList.add( result );
							//taskLogger.info( modelName + " task completed: " + resultName + " in " + runtimes.get(3) + " seconds." );
						}
						else {
							logger.error( "task result is null.  2nd element of resultBundle List<Object> is null." );
				    		throw new RuntimeException();
						}
							
			    	}
			    	catch( Exception e ) {
						logger.error( "Exception caught in client running JPPF job for " + modelName + ".", e );
			    		throw new RuntimeException();
			    	}
			    	
		        }
			    
	        }
	        catch ( Exception e ) {
	        	System.out.println( "Exception caught in client running JPPF job for " + modelName + "." );
	    		e.printStackTrace();
	        }
	
	    	logger.info( "done with " + modelName + "." );
	        
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
