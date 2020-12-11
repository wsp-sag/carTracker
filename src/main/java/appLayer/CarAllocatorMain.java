package appLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import org.jppf.client.JPPFClient;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;

import com.pb.common.util.ResourceUtil;

import accessibility.GeographyManager;
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import algorithms.CarAllocation;
import algorithms.CarAllocationTask;
import algorithms.HhCarAllocator;

import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import fileProcessing.ParameterReader;
import objectMapping.HhObjectMapper;
import objects.HouseholdCarAllocation;
import utility.ConstantsIf;
import utility.ConstantsMag;
import utility.ConstantsOhio;
import utility.ParallelHelper;


public class CarAllocatorMain {

    private static boolean runDistributed = false;
	
	
    public Map<Long, double[]> runCarAllocator( ResourceBundle rb, Logger logger, GeographyManager geogManager, SocioEconomicDataManager socec ) {
        
    	if ( logger == null )
    		logger = Logger.getLogger( CarAllocatorMain.class );

        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        String parametersFile = propertyMap.get( GlobalProperties.PARAMETER_FILE_KEY.toString() );       

        int debugHhId = -1;
        try {
            debugHhId = Integer.valueOf( propertyMap.get( GlobalProperties.HHID_LOG_REPORT_KEY.toString() ) );
        }
        catch( MissingResourceException e ) {         
        }
        
        
        
        int startOfDayMinute = 0; 
        debugHhId = -1;
        // get distance matrix
      
        List<HouseholdCarAllocation> carAllocationResults = null;
        ParameterReader parameterInstance = ParameterReader.getInstance();
        parameterInstance.readParameters(parametersFile);
        if(runDistributed)
        	carAllocationResults = runCarAllocation_v2_distributed( propertyMap, logger, parameterInstance ,  geogManager, socec);
        else        	
        carAllocationResults= runCarAllocation_v2_mono(propertyMap,logger,parameterInstance,  geogManager, socec);
        
        //if ( debugHhId >= 0 )
        //    debugCarAllocation_v2( propertyMap, logger, parametersFile, debugHhId, startOfDayMinute );       
        
        // get the region property.  this is set for Ohio3C and not set for MAG.
        // Use it to distinguish which implementation to use.
        ConstantsIf constants = null;
        WriteCarAllocationOutputFilesIf writer = null; 
        String mpoRegion = propertyMap.get( WriteCarAllocationOutputFilesIf.MODEL_REGION_PROPERTY );
        if ( mpoRegion == null ) {
	        constants = new ConstantsMag();
	        writer = new WriteCarAllocationOutputFilesMag();
        }
        else {
        	constants = new ConstantsOhio();
        	writer = new WriteCarAllocationOutputFilesOhio(); 
        }
        
        SharedDistanceMatrixData sharedDistanceObject = SharedDistanceMatrixData.getInstance(propertyMap, geogManager);
        String outputProbCarChangeFileName = propertyMap.get("output.hh.car.change.prob.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv";
        writer.writeCarAllocationOutputFile(logger, propertyMap, 
        	propertyMap.get("output.trip.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",
        	propertyMap.get("output.car.use.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",
        	outputProbCarChangeFileName, carAllocationResults, geogManager, sharedDistanceObject, socec, constants);
                
        return null;
    }   
    


	

	
	private List<HouseholdCarAllocation> runCarAllocation_v2_distributed( Map<String, String> propertyMap, 
			Logger logger, ParameterReader parameterInstance, GeographyManager geogManager, SocioEconomicDataManager socec ) {
		
//        JPPFNodeForwardingMBean forwarder = null;
//        NodeSelector masterSelector = null;
        JPPFClient myClient =  new JPPFClient();        
//        if ( ! myClient.isLocalExecutionEnabled() ) {
//            
//            try {
//                JPPFConnectionPool connectionPool = myClient.awaitActiveConnectionPool();
//                // wait until at least one JMX connection wrapper is established
//                JMXDriverConnectionWrapper jmx = connectionPool.awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
//                forwarder = jmx.getNodeForwarder();
//            
//                // create a node selector that only selects master nodes
//                ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true);
//                masterSelector = new ExecutionPolicySelector(masterPolicy);
//
//                TypedProperties slaveConfig = new TypedProperties()
//                                // request 1 processing threads
//                                .setInt( "jppf.processing.threads", 1 )
//                                // specify a server JVM with 48 GB of heap
//                                .setString( "jppf.jvm.options", "-server -Xmx8g -Djava.library.path=c:/jim/projects/mag/isam/lib/ortools" );
//                				//.setString( "jppf.jvm.options", "-server -Xmx32g" );
//                forwarder.provisionSlaveNodes(masterSelector, 4, slaveConfig);
//            }
//            catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            
//        }
	    

		int minHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MIN_ABM_HH_ID_KEY.toString() ) );
		int maxHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MAX_ABM_HH_ID_KEY.toString() ) );
        AbmDataStore abmDataStore = new AbmDataStore( propertyMap );
		minHhId = minHhId >= 0 ? minHhId : abmDataStore.getMinHhId();
		maxHhId = maxHhId >= 0 ? maxHhId : abmDataStore.getMaxHhId();
		
        
		int numHhsPerJob = Integer.valueOf( propertyMap.get( GlobalProperties.NUM_HHS_PER_JOB.toString() ) );

		logger.info( "running Car allocation for hhids: [" + minHhId + "," + maxHhId + "] ..." );

		DataProvider dataProvider = new MemoryMapDataProvider();
        dataProvider.setParameter( "parameterInstance", parameterInstance );
        dataProvider.setParameter( "propertyMap", propertyMap );
        dataProvider.setParameter( "geographyManager", geogManager );
        dataProvider.setParameter( "socioEconomicDataManager", socec );
        
  		List<HouseholdCarAllocation> hhAllocationResultsList = new ArrayList<>();
        List<List<HouseholdCarAllocation>> lpFailedList = new ArrayList<>();
       	for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       		lpFailedList.add( new ArrayList<>() );
        
		// solve schedule adjustment problems for the indicated household ids.
		// save in a separate list the HouseholdAdjustment objects for households where the LP failed
        List<Object> resultList = ParallelHelper.PARALLEL_HELPER_DISTRIBUTER.solveDistributed( CarAllocationTask.MODEL_LABEL, new CarAllocationTask(0,0), myClient, dataProvider, minHhId, maxHhId, numHhsPerJob );
       	for ( Object result : (List<Object>)resultList ) {
       		
       		List<Object> taskResultList = (List<Object>)result;       		
       		List<HouseholdCarAllocation> taskResultsList = (List<HouseholdCarAllocation>)taskResultList.get(0);
       		
       		hhAllocationResultsList.addAll( taskResultsList );
       		
       		for ( HouseholdCarAllocation hhAdj : taskResultsList )
     			if ( hhAdj.getOptimalSolutionIterations() > 0 )
     				lpFailedList.get( hhAdj.getOptimalSolutionIterations()-1 ).add( hhAdj );
       		
       	}
		
       	for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       		lpFailedList.get(i).sort( (v1, v2) -> v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() < 0.0 ? -1 : v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() > 0.0 ? 1 : 0 );
       	
       	//for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       	//	logger.info( "number of LP failures for end hour: " + HhCarAllocator.MAX_SIMULATION_HOURS[i] + " = " + lpFailedList.get(i).size() + "." );
        logger.info( "Car Allocator finished." );

        
        
        try {
//            if ( ! myClient.isLocalExecutionEnabled() )
//                forwarder.provisionSlaveNodes(masterSelector, 0, null);
            myClient.close();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }           
   		
        return hhAllocationResultsList;
        
	}
	


	private List<HouseholdCarAllocation> runCarAllocation_v2_mono( HashMap<String, String> propertyMap, Logger logger, ParameterReader parameterInstance, 
			GeographyManager geogManager, SocioEconomicDataManager socec) {
	
		
		int minHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MIN_ABM_HH_ID_KEY.toString() ) );
		int maxHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MAX_ABM_HH_ID_KEY.toString() ) );
		
			
		logger.info( System.getProperty("java.library.path"));
	    CarAllocation carAllocation = new CarAllocation( parameterInstance, propertyMap, socec, geogManager );
	    
	    logger.info( "reading ABM data files ..." );
	    AbmDataStore abmDataStore = new AbmDataStore( propertyMap );
	    
	    minHhId = minHhId >= 0 ? minHhId : abmDataStore.getMinHhId();
		maxHhId = maxHhId >= 0 ? maxHhId : abmDataStore.getMaxHhId();
	
		
	    abmDataStore.populateDataStore( minHhId, maxHhId+1 );
	    
	    Map<Long, Double> minActDurMap = null;
	    
	    logger.info( "starting Car Allocation procedure ..." );
	    HhObjectMapper hhObjectMapper = new HhObjectMapper( propertyMap, abmDataStore, null, null, minActDurMap );
	    HhCarAllocator carAllocator = new HhCarAllocator( propertyMap,carAllocation, abmDataStore, null,  logger );
	    List<HouseholdCarAllocation> carAllocationResults =
	    	Arrays.stream( abmDataStore.getHhIdArrayInRange( minHhId, maxHhId+1 ) )
				  .filter( x -> abmDataStore.getTripRecords(x) != null ) 
				  .filter( x -> abmDataStore.getNumAutoTripRecords(x).size() >0 )
	    		  .mapToObj( hhid -> hhObjectMapper.createHouseholdObjectFromFiles(propertyMap,hhid, null, false) )	    		    
	  		      .map( carAllocator::getCarAllocationWithSchedulesForHh )
	    		  .collect( Collectors.toList() );
			 
	    /*
	    List<List<HouseholdCarAllocation>> lpFailedList = new ArrayList<>();
	   	for ( int i=0; i < carAllocator.MAX_ITERATIONS; i++ )
	   		lpFailedList.add( new ArrayList<>() );
	    
			for ( HouseholdCarAllocation hhAdj : carAllocationResults )
				if ( hhAdj.getOptimalSolutionIterations() > 0 )
					lpFailedList.get( hhAdj.getOptimalSolutionIterations()-1 ).add( hhAdj );
	
	   	for ( int i=0; i < HhSchedAdjuster.MAX_ITERATIONS; i++ )
	   		lpFailedList.get(i).sort( (v1, v2) -> v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() < 0.0 ? -1 : v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() > 0.0 ? 1 : 0 );
	   	
	   	for ( int i=0; i < HhSchedAdjuster.MAX_ITERATIONS; i++ )
	   		logger.info( "number of LP failures for end hour: " + HhSchedAdjuster.MAX_SIMULATION_HOURS[i] + " = " + lpFailedList.get(i).size() + "." );
	    logger.info( "schedule adjustment finished." );
	    
	    */
	    return carAllocationResults;
	    
	}



	public static void main( String[] args ) {
	
		long start = System.currentTimeMillis();
		
	    CarAllocatorMain mainObj = new CarAllocatorMain();
	
		System.out.println ( "CarTracker, 11Dec2020, v1.12, starting." );
	    
		ResourceBundle rb = null;
		if ( args.length >=0 ) {
			rb = ResourceBundle.getBundle( args[0] );			
			if ( args.length > 1 ) 
				runDistributed = Boolean.parseBoolean( args[1] );
		}
		else {
			System.out.println( "\ninvalid number of command line arguments - 1 expected." );
			System.out.println( "\t 1) String argument with the name of the properties file (without the .properties extension)." );
			System.exit(-1);
		}
		
		// set up geography manager
		HashMap<String,String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
		GeographyManager geogManager = GeographyManager.getInstance();
	    geogManager.setupGeographyManager(propertyMap);
	    
	    // set up socio economic data manager
	    SocioEconomicDataManager socec = SocioEconomicDataManager.getInstance();
	    socec.loadDataFromCsvFile(propertyMap.get("socec.data.file.name"), propertyMap.get("socec.data.maz.field"));
	    
	
		mainObj.runCarAllocator( rb, null, geogManager, socec );
		
		System.out.println ( "Car Tracker finished in " + (int)((System.currentTimeMillis() - start)/1000.0) + " seconds." );
	    System.out.println ( "\n" );
	
	    System.exit(0);
	}


}

