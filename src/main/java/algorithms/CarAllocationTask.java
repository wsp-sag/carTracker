package algorithms;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.apache.log4j.Logger;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;

import accessibility.GeographyManager;
import accessibility.SocioEconomicDataManager;
import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import fileProcessing.ParameterReader;
import fileProcessing.VehicleTypePreferences;
import objectMapping.HhObjectMapper;
import objects.AbmDataVehicleIdMap;
import objects.HouseholdCarAllocation;
import utility.DistributableIf;



public class CarAllocationTask extends AbstractTask<Object> implements DistributableIf<AbstractTask<?>> {

	public static final String MODEL_LABEL = "Car Tracker";
	private static final long versionNumber = 1L;
	
	private static final long serialVersionUID = versionNumber;

	private int startRange;
	private int endRange;
	
	private String taskName;

	
    public CarAllocationTask( int minRange, int maxRange ) {
    	startRange = minRange;
    	endRange = maxRange;

    }

    
	public CarAllocationTask newInstance ( int firstTask, int lastTask ) {
    	return new CarAllocationTask( firstTask, lastTask );
    }	
 

    public void run() {
    	
		long start = System.currentTimeMillis();
		long start1 = System.currentTimeMillis();
    	
    	Logger logger = Logger.getLogger(CarAllocationTask.class);    	
		taskName = "NULL";
        try {
			taskName = "[ " + java.net.InetAddress.getLocalHost().getHostName() + "_" + Thread.currentThread().getName() + "_" + MODEL_LABEL + "_" + startRange + "_" + endRange + " ]";
			setId( taskName );
		}
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        GeographyManager geogManager = null;
        SocioEconomicDataManager socec = null;
		VehicleTypePreferences vehicleTypePreferences = null;
        		
		DataProvider dataProvider = getDataProvider();
		HashMap<String, String> propertyMap = dataProvider.getParameter("propertyMap");
        ParameterReader parameterInstance = dataProvider.getParameter( "parameterInstance" );        
        geogManager = dataProvider.getParameter("geographyManager");
        socec = dataProvider.getParameter("socioEconomicDataManager");
		vehicleTypePreferences = (VehicleTypePreferences)dataProvider.getParameter( "vehicleTypePreferences" );
        CarAllocation carAllocation = new CarAllocation( parameterInstance,propertyMap, socec,geogManager,vehicleTypePreferences );
        
		int secs1 = (int)((System.currentTimeMillis() - start1)/1000.0);
		start1 = System.currentTimeMillis();
		
    	// get the hh info and log the report for the debugHhId
	    AbmDataStore abmDataStore = new AbmDataStore( propertyMap );
        abmDataStore.populateDataStore( startRange, endRange+1 );
       	        
		int secs2 = (int)((System.currentTimeMillis() - start1)/1000.0);
		start1 = System.currentTimeMillis();
		
        Map<Integer, Float> plannedDepartTimesMap = null;
        Map<Integer, Float> experiencedTravelTimesMap = null;
        Map<Long, Double> minActDurMap = null;
        
        float threhsoldRoundUp = Float.parseFloat(propertyMap.get(GlobalProperties.ROUND_UP_THRESHOLD.toString()));
        AbmDataVehicleIdMap abmDataVehicleIdMap = null;
        HhObjectMapper hhObjectMapper = new HhObjectMapper( propertyMap, abmDataStore, experiencedTravelTimesMap, plannedDepartTimesMap, minActDurMap );        
        HhCarAllocator carAllocator = new HhCarAllocator( propertyMap,carAllocation, abmDataStore, experiencedTravelTimesMap,logger );

		int[] hhIdArray = abmDataStore.getHhIdArrayInRange( startRange, endRange+1 );
        
        List<HouseholdCarAllocation> hhCarAllocationResults =
        		Arrays.stream( hhIdArray )
  			  .filter( x -> abmDataStore.getTripRecords(x) != null )
  			  .filter( x -> abmDataStore.getNumAutoTripRecords(x).size() >0 )
      		  .mapToObj( hhid -> hhObjectMapper.createHouseholdObjectFromFiles(propertyMap,hhid, null, false) )	    		    
    		      .map( carAllocator::getCarAllocationWithSchedulesForHh )
      		  .collect( Collectors.toList() );
  		 
		int secs3 = (int)((System.currentTimeMillis() - start1)/1000.0);
		start1 = System.currentTimeMillis();
		
		int numItersIntegrizing = 0;
		int numItersOptimalSolution = 0;
		for ( HouseholdCarAllocation temp : hhCarAllocationResults ) {
			numItersIntegrizing += temp.getNumIterationsForIntegerizing();
			numItersOptimalSolution += temp.getOptimalSolutionIterations();
		}
    		 
        
        // create a result Object to hold the number of failures and the list of vehRecords.
        List<Object> resultObject = new ArrayList<Object>(1);
        resultObject.add( hhCarAllocationResults );

		int seconds = (int)((System.currentTimeMillis() - start)/1000.0);
        
		// add the taskname and the result Object to the resultBundle for this task
        List<Object> resultBundle = new ArrayList<Object>(5);
        resultBundle.add( taskName );
        resultBundle.add( resultObject );
        resultBundle.add( Arrays.asList( secs1, secs2, secs3, seconds ) );
        resultBundle.add( numItersIntegrizing );
        resultBundle.add( numItersOptimalSolution );
        setResult( resultBundle );
		
    }

    
    
}
