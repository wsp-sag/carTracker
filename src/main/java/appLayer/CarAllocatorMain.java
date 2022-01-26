package appLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import org.jppf.client.JPPFClient;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;

import com.google.ortools.Loader;
//import com.google.ortools.Loader;
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
import fileProcessing.VehicleTypePreferences;
import objectMapping.HhObjectMapper;
import objects.HouseholdCarAllocation;
import utility.ConstantsIf;
import utility.ConstantsMag;
import utility.ConstantsOhio;
import utility.ParallelHelper;


public class CarAllocatorMain {

    private static boolean runDistributed = false;
	public static boolean ortoolsLibLoaded = false;
	
	private Logger runTimeLogger =  Logger.getLogger("runTime");
	
    public Map<Long, double[]> runCarAllocator( ResourceBundle rb, Logger logger, GeographyManager geogManager, SocioEconomicDataManager socec ) {
        
    	if ( logger == null )
    		logger = Logger.getLogger( CarAllocatorMain.class );

        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        String parametersFile = propertyMap.get( GlobalProperties.PARAMETER_FILE_KEY.toString() );       
        ParameterReader parameterInstance = ParameterReader.getInstance();
        parameterInstance.readParameters(parametersFile);
        
    	String filename = propertyMap.get("vehicle.type.preferences.filename");
    	VehicleTypePreferences vehicleTypePreferences = new VehicleTypePreferences();
    	vehicleTypePreferences.readPreferences( filename );
    	        
        // get the region property.  this is set for Ohio3C and not set for MAG.
        // Use it to distinguish which implementation to use.
        ConstantsIf constants = null;
        WriteCarAllocationOutputFilesIf writer = null; 
        String mpoRegion = propertyMap.get( WriteCarAllocationOutputFilesIf.MODEL_REGION_PROPERTY );
        if ( mpoRegion == null ) {
	        constants = new ConstantsMag();
	        SharedDistanceMatrixData sharedDistanceObject = SharedDistanceMatrixData.getInstance(propertyMap, geogManager);
	        writer = new WriteCarAllocationOutputFilesMag( propertyMap, geogManager, sharedDistanceObject, socec, constants, vehicleTypePreferences );
        }
        else {
        	constants = new ConstantsOhio();
        	writer = new WriteCarAllocationOutputFilesOhio(); 
        }

	    logger.info( "creating ABM data store ..." );
	    AbmDataStore abmDataStore = new AbmDataStore( propertyMap );

	    int minHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MIN_ABM_HH_ID_KEY.toString() ) );
		int maxHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MAX_ABM_HH_ID_KEY.toString() ) );
		minHhId = minHhId >= 0 ? minHhId : abmDataStore.getMinHhId();
		maxHhId = maxHhId >= 0 ? maxHhId : abmDataStore.getMaxHhId();		
        
	    logger.info( "finished creating ABM data store ..." );
	    
        if(runDistributed)
        	runCarAllocation_v2_distributed( propertyMap, logger, parameterInstance, abmDataStore, minHhId, maxHhId, geogManager, socec, vehicleTypePreferences, writer);
        else        	
        	runCarAllocation_v2_mono(propertyMap, logger, parameterInstance, abmDataStore, minHhId, maxHhId, geogManager, socec, vehicleTypePreferences, writer);
        
        writer.writeProcessedCarAllocationResults( logger );
                
        return null;
    }   
    

	
	private void runCarAllocation_v2_distributed( Map<String, String> propertyMap, Logger logger, ParameterReader parameterInstance,
			AbmDataStore abmDataStore, int minHhId, int maxHhId, GeographyManager geogManager, SocioEconomicDataManager socec,
			VehicleTypePreferences vehicleTypePreferences, WriteCarAllocationOutputFilesIf writer ) {
					    

		int numHhsPerJob = Integer.valueOf( propertyMap.get( GlobalProperties.NUM_HHS_PER_JOB.toString() ) );
		int numHhPartitions = Integer.valueOf( propertyMap.get( GlobalProperties.NUM_HH_PARTITIONS.toString() ) );
		int numHhsPerPartition = (maxHhId - minHhId)/numHhPartitions;


		DataProvider dataProvider = new MemoryMapDataProvider();
        dataProvider.setParameter( "parameterInstance", parameterInstance );
        dataProvider.setParameter( "propertyMap", propertyMap );
        dataProvider.setParameter( "geographyManager", geogManager );
        dataProvider.setParameter( "socioEconomicDataManager", socec );
        dataProvider.setParameter( "vehicleTypePreferences", vehicleTypePreferences );
        dataProvider.setParameter( "abmDataStore", abmDataStore );
        
        JPPFClient myClient =  new JPPFClient();        

        
        List<List<HouseholdCarAllocation>> lpFailedList = new ArrayList<>();
       	for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       		lpFailedList.add( new ArrayList<>() );
        
		// solve schedule adjustment problems for the indicated household ids.
		// save in a separate list the HouseholdAdjustment objects for households where the LP failed
   		int startHh = minHhId;
   		int endHh = minHhId + numHhsPerPartition;
       	for ( int p=0; p < numHhPartitions; p++ ) {

    		long start = System.currentTimeMillis();

    		logger.info( "running Car allocation for partition " + (p+1) + " of " + numHhPartitions + ", hhids: [" + startHh + "," + endHh + "] ..." );
       		
	        List<Object> resultList = ParallelHelper.PARALLEL_HELPER_DISTRIBUTER.solveDistributed( CarAllocationTask.MODEL_LABEL, new CarAllocationTask(0,0), myClient, dataProvider, startHh, endHh, numHhsPerJob );
    		logger.info( "recieved task resultList." );

    		for ( Object result : (List<Object>)resultList ) {
	       		
	       		List<Object> taskResultList = (List<Object>)result;       		
	       		List<HouseholdCarAllocation> taskResultsList = (List<HouseholdCarAllocation>)taskResultList.get(0);
	       		
	    	    writer.processPartitionResults ( logger, taskResultsList );

	       		for ( HouseholdCarAllocation hhAdj : taskResultsList )
	     			if ( hhAdj.getOptimalSolutionIterations() > 0 )
	     				lpFailedList.get( hhAdj.getOptimalSolutionIterations()-1 ).add( hhAdj );
	       		
	       	}

			logger.info ( "partition " + (p+1) + " of " + numHhPartitions + " finished in " + (int)((System.currentTimeMillis() - start)/1000.0) + " seconds." );

	   		startHh = endHh;
	   		if ( startHh == maxHhId )
	   			break;

	   		endHh += numHhsPerPartition;
	   		if ( (endHh > maxHhId) || (p == (numHhPartitions-2)) )
	   			endHh = maxHhId;

       	}
       	
       	
       	for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       		lpFailedList.get(i).sort( (v1, v2) -> v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() < 0.0 ? -1 : v1.getHousehold().getNumIndivTripRecords() - v2.getHousehold().getNumIndivTripRecords() > 0.0 ? 1 : 0 );
       	
        try {
            myClient.close();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }           
   		        
		
       	//for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       	//	logger.info( "number of LP failures for end hour: " + HhCarAllocator.MAX_SIMULATION_HOURS[i] + " = " + lpFailedList.get(i).size() + "." );
        logger.info( "Car Allocator finished." );
        
	}
	


	private void runCarAllocation_v2_mono( HashMap<String, String> propertyMap, Logger logger, ParameterReader parameterInstance, 
		AbmDataStore abmDataStore, int minHhId, int maxHhId, GeographyManager geogManager, SocioEconomicDataManager socec,
		VehicleTypePreferences vehicleTypePreferences, WriteCarAllocationOutputFilesIf writer) {
		
        abmDataStore.populateDataStore( minHhId, maxHhId+1 );
		
		logger.info( System.getProperty("java.library.path"));
	    CarAllocation carAllocation = new CarAllocation( parameterInstance, propertyMap, socec, geogManager, vehicleTypePreferences );
	    
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
				  //.filter( x -> x != null )
	    		  .collect( Collectors.toList() );
			 
	    writer.processPartitionResults ( logger, carAllocationResults );

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
	    
	}



	public static void main( String[] args ) {
	
		Loader.loadNativeLibraries();
		
		long start = System.currentTimeMillis();
				
	    CarAllocatorMain mainObj = new CarAllocatorMain();
	
		System.out.println ( "CarTracker, 26Oct2022, v4.00, starting." );
	    
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
		
		String logStatement = String.format( "%s, %.1f seconds.", "CarTracker", ( ( System.currentTimeMillis() - start ) / 1000.0 ) );
		mainObj.runTimeLogger.info ( logStatement );

		System.out.println ( "Car Tracker finished in " + (int)((System.currentTimeMillis() - start)/1000.0) + " seconds." );
	    System.out.println ( "\n" );
	
	    System.exit(0);
	}

}
