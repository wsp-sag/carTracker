package appLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.Operator;
import org.jppf.management.ExecutionPolicySelector;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.policy.ExecutionPolicy;


import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;

//import org.jppf.node.protocol.DataProvider;
//import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.utils.TypedProperties;

import accessibility.GeographyManager;
import accessibility.MatrixDataHandlerIf;
import accessibility.MatrixDataLocalHandler;
import accessibility.MatrixDataRemoteHandler;
import accessibility.MatrixDataServer;
import accessibility.MatrixDataServerRmi;
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import algorithms.CarAllocation;
import algorithms.CarAllocationTask;
import algorithms.HhCarAllocator;
import utility.CommonProcedures;
import utility.Parsing;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.IndexSort;
import com.pb.common.util.ResourceUtil;













import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import fileProcessing.ParameterReader;
import fileProcessing.PurposeCategories;
import objectMapping.AbmObjectTranslater;
import objectMapping.HhObjectMapper;
import objects.AbmDataVehicleIdMap;
import objects.AutoTrip;
import objects.Household;
import objects.HouseholdCarAllocation;
import objects.Trip;
import utility.ParallelHelper;

public class CarAllocatorMain {

    private static final int TRIP_REPOSITIONING_PURPOSE = 20;
    private static boolean runDistributed = false;
    private static float threhsoldRoundUp = 0.7f;
    
	private static final String OUTPUT_TRIP_TABLES_KEY = "output.trip.matrices";
	private static final String OUTPUT_TRIP_TABLE_FORMAT_KEY = "output.trip.matrices.format";
	private static final String OUTPUT_TRIP_TABLE_CSV_FILENAME_KEY = "output.trip.matrices.csv.file";
	private static final String MODE_TABLE_NAMES_KEY = "output.trip.matrix.mode.tables";
	private static final String AUTO_MODE_TABLE_NAMES_KEY = "output.trip.matrix.auto.mode.tables";
	
	private static final String TRIP_MATRIX_EA_FILE_KEY = "output.trip.matrix.early";
	private static final String TRIP_MATRIX_AM_FILE_KEY = "output.trip.matrix.am";
	private static final String TRIP_MATRIX_MD_FILE_KEY = "output.trip.matrix.midday";
	private static final String TRIP_MATRIX_PM_FILE_KEY = "output.trip.matrix.pm";
	private static final String TRIP_MATRIX_EV_FILE_KEY = "output.trip.matrix.late";
	
	public static final String NUM_OUTPUT_PERIODS_KEY = "number.periods";
	public static final String EA_PERIOD_START_KEY = "early.period.start.interval";
	public static final String EA_PERIOD_END_KEY = "early.period.end.interval";
	public static final String AM_PERIOD_START_KEY = "am.period.start.interval";
	public static final String AM_PERIOD_END_KEY = "am.period.end.interval";
	public static final String MD_PERIOD_START_KEY = "midday.period.start.interval";
	public static final String MD_PERIOD_END_KEY = "midday.period.end.interval";
	public static final String PM_PERIOD_START_KEY = "pm.period.start.interval";
	public static final String PM_PERIOD_END_KEY = "pm.period.end.interval";
	public static final String EV_PERIOD_START_KEY = "late.period.start.interval";
	public static final String EV_PERIOD_END_KEY = "late.period.end.interval";
	

	private static final String NUM_USER_CLASS_KEY = "num.user.classes";
	
	private boolean outputTripTables;
	private boolean outputTripTableSeparatedByAutoTransit;
	private int numberOfPeriods;
	
	private boolean ifUserClasses;
	private int numberOfVotCategories;
	
	private String[] autoTripTableNames;
	private String[] autoTripTableNamesWithVot;
	private String[] autoTripTableFiles;
	private String[] transitTripTableNames;
	private String[] transitTripTableFiles;
	private String[] otherTripTableNames;
	private String[] otherTripTableFiles;
	

	private float[][][][] autoTripTables;
	private int[][] periodIntervals;
	
	private int[] periodTotalTrips;
	private int[] autoModeCodes;
	private int[] transitModeCodes;
	private int[] otherModeCodes;
	private int[] votThresholds;
	

	private MatrixDataHandlerIf matrixHandler;
	float tripExpansionFactor = 1.0f;
	private boolean ifExternalStationsIncluded = false;
	
    public Map<Long, double[]> runCarAllocator( HashMap<String,String> propertyMap, Logger logger, GeographyManager geogManager, SocioEconomicDataManager socec ) {
        
    	if ( logger == null )
    		logger = Logger.getLogger( CarAllocatorMain.class );


        String parametersFile = propertyMap.get( GlobalProperties.PARAMETER_FILE_KEY.toString() );       

        int debugHhId = -1;
        try {
            debugHhId = Integer.valueOf( propertyMap.get( GlobalProperties.HHID_LOG_REPORT_KEY.toString() ) );
        }
        catch( MissingResourceException e ) {         
        }
        
        threhsoldRoundUp = Float.parseFloat(propertyMap.get(GlobalProperties.ROUND_UP_THRESHOLD.toString()));
        tripExpansionFactor = 1.0f/Float.parseFloat(propertyMap.get("global.proportion"));
        ifExternalStationsIncluded = Boolean.valueOf(propertyMap.get("include.external.stations")); 
        MatrixDataManager mdm = MatrixDataManager.getInstance();
        MatrixDataServer matrixDataServer = new MatrixDataServer();
        matrixHandler = new MatrixDataLocalHandler(matrixDataServer, MatrixType.TPPLUS);
        
        
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
        
        
        SharedDistanceMatrixData sharedDistanceObject = SharedDistanceMatrixData.getInstance(propertyMap, geogManager);
        String outputProbCarChangeFileName = propertyMap.get("output.hh.car.change.prob.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv";
        writeCarAllocationOutputFile(logger,propertyMap, propertyMap.get("output.trip.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",
        		propertyMap.get("output.car.use.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",outputProbCarChangeFileName,
        		carAllocationResults,geogManager, sharedDistanceObject,socec);
        
        
        return null;
    }   
    


	

	
	private List<HouseholdCarAllocation> runCarAllocation_v2_distributed( Map<String, String> propertyMap, 
			Logger logger, ParameterReader parameterInstance, GeographyManager geogManager, SocioEconomicDataManager socec ) {
		
//        JPPFNodeForwardingMBean forwarder = null;
//        NodeSelector masterSelector = null;
		
		int minHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MIN_ABM_HH_ID_KEY.toString() ) );
		int maxHhId = Integer.valueOf( propertyMap.get( GlobalProperties.MAX_ABM_HH_ID_KEY.toString() ) );
		
		
		
          
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
	    


        AbmDataStore abmDataStore = new AbmDataStore( propertyMap );

		minHhId = minHhId >= 0 ? minHhId : abmDataStore.getMinHhId();
		maxHhId = maxHhId >= 0 ? maxHhId : abmDataStore.getMaxHhId();
        
		     
		
		
		int partitionNumber = Integer.valueOf( propertyMap.get( "partition.number" ) );
		int numPartition = Integer.valueOf( propertyMap.get( "num.partitions" ) );
		
		
		int numHhPerPartition = (maxHhId-minHhId)/numPartition;
		List<HouseholdCarAllocation> hhAllocationResultsList = new ArrayList<>();
		JPPFClient myClient =  new JPPFClient(); 
		
		int minHhIdPartition = (partitionNumber-1)*numHhPerPartition;
		int maxHhIdPartition = (partitionNumber == numPartition)? maxHhId: (partitionNumber)*numHhPerPartition-1;
		
		int numHhsPerJob = Integer.valueOf( propertyMap.get( GlobalProperties.NUM_HHS_PER_JOB.toString() ) );

		logger.info( "running Car allocation for hhids: [" + minHhIdPartition + "," + maxHhIdPartition + "] ..." );

		DataProvider dataProvider = new MemoryMapDataProvider();
        dataProvider.setParameter( "parameterInstance", parameterInstance );
        dataProvider.setParameter( "propertyMap", propertyMap );
        dataProvider.setParameter( "geographyManager", geogManager );
        dataProvider.setParameter( "socioEconomicDataManager", socec );
        
        List<List<HouseholdCarAllocation>> lpFailedList = new ArrayList<>();
       	for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       		lpFailedList.add( new ArrayList<>() );
        
		List<Object> resultList = ParallelHelper.PARALLEL_HELPER_DISTRIBUTER.solveDistributed( CarAllocationTask.MODEL_LABEL, new CarAllocationTask(0,0), myClient, dataProvider, minHhIdPartition, maxHhIdPartition, numHhsPerJob );
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
       	
        try {
//	            if ( ! myClient.isLocalExecutionEnabled() )
//	                forwarder.provisionSlaveNodes(masterSelector, 0, null);
            myClient.close();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }   
		
       	//for ( int i=0; i < HhCarAllocator.MAX_ITERATIONS; i++ )
       	//	logger.info( "number of LP failures for end hour: " + HhCarAllocator.MAX_SIMULATION_HOURS[i] + " = " + lpFailedList.get(i).size() + "." );
        logger.info( "Car Allocator finished." );

        
        
        
   		
        return hhAllocationResultsList;
        
	}
	

    public void writeCarAllocationOutputFile( Logger logger, HashMap<String, String> propertyMap, String outputTripListFilename, 
    		String outputDisaggregateCarUseFileName, String outputProbCarChangeFileName,
    		List<HouseholdCarAllocation> hhCarAllocationResultsList,GeographyManager geogManager, SharedDistanceMatrixData sharedDistanceObject,
    		SocioEconomicDataManager socec) {
    
	String outputFlag = propertyMap.get( OUTPUT_TRIP_TABLES_KEY );
	if ( outputFlag.equalsIgnoreCase( "true" ) )
		outputTripTables = true;
	
	numberOfPeriods = Integer.valueOf( propertyMap.get(NUM_OUTPUT_PERIODS_KEY));
	
	String[] str =  propertyMap.get( "output.trip.matrix.auto.mode.codes" ).trim().split(",");
	autoModeCodes = new int[str.length];
	for(int i=0;i<str.length;i++)
		autoModeCodes[i]=Integer.parseInt(str[i]);
	
	String tripTableNamesString = propertyMap.get( AUTO_MODE_TABLE_NAMES_KEY );
	autoTripTableNames = utility.Parsing.getStringArrayFromCsvString( tripTableNamesString );
	
	autoTripTableFiles = new String[numberOfPeriods];
	otherTripTableFiles = new String[numberOfPeriods];		
	
	autoTripTableFiles[0] = propertyMap.get("result.path") + "/"+propertyMap.get( TRIP_MATRIX_EA_FILE_KEY );
	autoTripTableFiles[1] = propertyMap.get("result.path") + "/"+propertyMap.get( TRIP_MATRIX_AM_FILE_KEY );
	autoTripTableFiles[2] = propertyMap.get("result.path") + "/"+propertyMap.get( TRIP_MATRIX_MD_FILE_KEY );
	autoTripTableFiles[3] = propertyMap.get("result.path") + "/"+propertyMap.get( TRIP_MATRIX_PM_FILE_KEY );
	autoTripTableFiles[4] = propertyMap.get("result.path") + "/"+propertyMap.get( TRIP_MATRIX_EV_FILE_KEY );
	
	autoTripTables = new float[numberOfPeriods][autoTripTableNames.length][geogManager.getTazValues().length][geogManager.getTazValues().length];
	
	periodIntervals = new int[5][2];
	periodIntervals[0][0] = Integer.valueOf( propertyMap.get( EA_PERIOD_START_KEY ) );
	periodIntervals[0][1] = Integer.valueOf( propertyMap.get( EA_PERIOD_END_KEY ) );
	periodIntervals[1][0] = Integer.valueOf( propertyMap.get( AM_PERIOD_START_KEY ) );
	periodIntervals[1][1] = Integer.valueOf( propertyMap.get( AM_PERIOD_END_KEY ) );
	periodIntervals[2][0] = Integer.valueOf( propertyMap.get( MD_PERIOD_START_KEY ) );
	periodIntervals[2][1] = Integer.valueOf( propertyMap.get( MD_PERIOD_END_KEY ) );
	periodIntervals[3][0] = Integer.valueOf( propertyMap.get( PM_PERIOD_START_KEY ) );
	periodIntervals[3][1] = Integer.valueOf( propertyMap.get( PM_PERIOD_END_KEY ) );
	periodIntervals[4][0] = Integer.valueOf( propertyMap.get( EV_PERIOD_START_KEY ) );
	periodIntervals[4][1] = Integer.valueOf( propertyMap.get( EV_PERIOD_END_KEY ) );
	
	logger.info( "writing trip matrix files." );

	String formatString = propertyMap.get( OUTPUT_TRIP_TABLE_FORMAT_KEY );
	MatrixType matrixType = MatrixType.lookUpMatrixType( formatString );
	
	int[] tazIndices = geogManager.getTazIndices();
	
	int[] tazValues = geogManager.getTazValues();
	int[] tazValuesOrder = IndexSort.indexSort( tazValues );
	int[] extNumbers = new int[tazValues.length+1];
	
	for ( int i=0; i < tazValues.length; i++ ) {
			int k = tazValuesOrder[i];
			extNumbers[i+1] = tazValues[k];
	}		
	
	String[] periodLabels = new String[]{ "early", "am", "midday", "pm", "late" };		
	
	float globalLoop = Float.parseFloat(propertyMap.get("global.loop"));
	
    double[] probChagingCarOwnership = null;
    if(globalLoop > 1)
    	probChagingCarOwnership = getProbabilityOfChangingCarOwnership( propertyMap.get("hh.car.ownership.correction.file"),propertyMap.get("hh.id.field"),propertyMap.get("hh.prob.car.change.field")  );

	
    PrintWriter outStreamTrip = null;
    PrintWriter outStreamCar = null;
    PrintWriter outStreamHh = null;
    try {
    	outStreamTrip = new PrintWriter( new BufferedWriter( new FileWriter( outputTripListFilename ) ) );
        String header1 = "hhid,pnum,tripid,tripRecNum,mode,vehId,origPurp,destPurp,origMaz,destMaz,origTaz,destTaz,distanceFromHome,plannedDepartureTime,departureEarly,departureLate,finalDeparture, finalArrival,x1,x2,x3,x4,unsatisfiedResult,numIterationIntegerizing";
        outStreamTrip.println( header1 );   
        outStreamCar = new PrintWriter( new BufferedWriter( new FileWriter( outputDisaggregateCarUseFileName ) ) );
        String header2 = "hhid,carId,aVStatus,autoTripId,autoHhTripId,driverId,origPurp,destPurp,origMaz,destMaz,origTaz,destTaz,carRepositionType,origHome,destHome,tripDistance,distanceFromHomeToOrig,distanceFromHomeToDest,plannedDeparture,finalDeparture,finalArrival,departureEarly,departureLate,parkDurationAtDestination,ParkCostAtDestination,parkDurationAtNextOrigin,parkCostAtNextOrigin";
        outStreamCar.println( header2 );   
        outStreamHh = new PrintWriter( new BufferedWriter( new FileWriter( outputProbCarChangeFileName ) ) );
        String header3 = "hhid,hidAcrossSample,prevIterationCarChangeProb,probCarOwnershipChange,msaFactor";
        outStreamHh.println( header3 );   
    }
    catch (IOException e) {
        System.out.println( "IO Exception writing adjusted trip schedules file: " + outputTripListFilename );
        e.printStackTrace();
        System.exit(-1);;
    }       

    int totalAutoTrips = 0;
    int totalAutoNonAVTrips = 0;
    int totalAutoAVTrips = 0;
    int totalLoadedTripsAV = 0;
    int totalEmptyTripsAV = 0;
    float totalVMT = 0;
    float totalAVVMT = 0;
    float loadedAVVMT = 0;
    float emptyAVVMT = 0;
    float totalExtraWaitTime = 0;
    float totalEarlyDepTime = 0;
    int totalCars = 0;
    int totalNonAVCars = 0;
    int totalAVCars = 0;
    int totalUsedCars = 0;
    int[] totalUnusedCars = new int[3];
    int totalUsedNonAVCars = 0;
    int totalUsedAVCars = 0;
    int totalDemand = 0;
    int totalNonSingularCases = 0;
    int carSufficiencyLevel = -1;
    
    
    
    
    int totalDemandMet = 0;
    double addCarFactor = Double.parseDouble(propertyMap.get("add.car.factor"));
    double subtractCarFactor = Double.parseDouble(propertyMap.get("subtract.car.factor"));
    
    
    float msaFactor = 1/globalLoop;
    
    for ( HouseholdCarAllocation depArrObj : hhCarAllocationResultsList ) {
        
        Household hh = depArrObj.getHousehold();
        double[][][] depArrResults = depArrObj.getScheduleAdjustmentResults();
        double[][][] carAllocationResults = depArrObj.getAllocationResult();
        double[]  unsatisDemandResults = depArrObj.getUnsatisfiedDemandResults();
        double[][][][] carLinkingResults = depArrObj.getCarLinkingResult();
        List<Trip> trips = hh.getTrips();
        List<AutoTrip> aTrips = hh.getAutoTrips();
        int numAuto = hh.getNumAutos();
        
        if(numAuto > 0 && numAuto < hh.getPersons().length   )
        	carSufficiencyLevel = 0;
        else if(numAuto > 0 && numAuto == hh.getPersons().length   )
        	carSufficiencyLevel = 1;
        else 
        	carSufficiencyLevel = 2;
        	
        int homeMaz = hh.getHomeMaz();
    	
    	int homeTaz = geogManager.getMazTazValue(homeMaz);
    	
    	float[] distanceFromHome = sharedDistanceObject.getOffpeakDistanceFromTaz(homeTaz);
    	float[] distanceToHome = sharedDistanceObject.getOffpeakDistanceToTaz(homeTaz);
    	
    	double[] parkingRateHr = socec.getDoubleFieldByMazValue(propertyMap.get(GlobalProperties.PARKING_HOURLY_FIELD.toString()));
    	double[] parkingRateMonth = socec.getDoubleFieldByMazValue(propertyMap.get(GlobalProperties.PARKING_MONTHLY_FIELD.toString()));
    	
        
        int hhNumUnsatisfiedDemand = 0;
   
        for ( int n=0; n < trips.size(); n++ ) {
            Trip trip = trips.get( n );   
            int autoId = -1;
            int singular = 0;
            double unsatisRes = -1;
            int mode = trip.getMode();
            double[] xij = {-1,-1,-1,-1};
           
            //Assign auto ID for each auto trip
            if((mode == AbmObjectTranslater.SOV_MODE || mode == AbmObjectTranslater.HOV2_DR_MODE || mode == AbmObjectTranslater.HOV3_DR_MODE) ){
            	double maxAllocation = 0;
            	unsatisRes = unsatisDemandResults[trip.getHhAutoTripId()];
            	double[] carAllocationForTrip = carAllocationResults[CarAllocation.INDEX_CarAllo][trip.getHhAutoTripId()];                	
            	for( int a = 0; a < numAuto; a ++){
            		xij[a] = carAllocationForTrip[a];
                	if(carAllocationForTrip[a]>threhsoldRoundUp){
                		autoId = a + 1;
                	}
                }
            }

       	 	//Calculate stats
           
            if(autoId <0)
            	hhNumUnsatisfiedDemand++;
            
            trip.setAllocatedAutoId(autoId);
            
            float depEarly = (float)depArrResults[CarAllocation.DEP_EARLY][trip.getPnum()][trip.getIndivTripId()];
            float depLate = (float)depArrResults[CarAllocation.DEP_LATE][trip.getPnum()][trip.getIndivTripId()];
            
            totalExtraWaitTime +=depLate;
            totalEarlyDepTime +=depEarly;
            if(unsatisRes>=0 && unsatisRes <=0.1 && autoId == -1){
            	totalNonSingularCases++;
            	singular = 1;
            }
            //String record = String.format( "%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f", hh.getId(), trip.getPnum(), trip.getUniqueTripId(), trip.getTripRecNum(),
            //trip.getVehId(),trip.getOrigAct(), trip.getDestAct(), trip.getOrigMaz(), trip.getDestMaz(), trip.getSchedDepart(), depEarly, depLate );
            float finalDeparture = trip.getSchedDepart()- depEarly+depLate;
            float finalArrival = finalDeparture + trip.getSchedTime();
            int origTaz = geogManager.getMazTazValue(trip.getOrigMaz());
            int destTaz = geogManager.getMazTazValue(trip.getDestMaz());
            float tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
            String record = hh.getId()+","+ trip.getPnum()+","+ trip.getUniqueTripId()+","+ trip.getTripRecNum()+","+trip.getMode()+","+
                    		trip.getVehId()+","+trip.getOrigAct()+","+ trip.getDestAct()+","+ trip.getOrigMaz()+","+ trip.getDestMaz()+","+ origTaz + ","+ destTaz+","+tripDistanceFromHome+","+trip.getSchedDepart()+","+ depEarly+","+ depLate +","+finalDeparture+","+finalArrival+","+
                    		xij[0]+","+xij[1]+","+xij[2]+","+xij[3] + ","+ unsatisRes + ","+ depArrObj.getNumIterationsForIntegerizing();
                    
            outStreamTrip.println( record );     
            int period = CommonProcedures.getInterval((int)((trip.getSchedDepart()+depLate-depEarly)/60),(int)(trip.getSchedDepart()+depLate-depEarly) % 60); 
                    		
			if(mode == Integer.parseInt(propertyMap.get("taxi.mode.code"))){
                if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz()))))
    				autoTripTables[getTripTablePeriod(period)][4-1][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
			}
            
           
        }
        
        int ifAvHh = hh.getIfAvHousehold();
        totalCars+=numAuto;
        if(ifAvHh == 1){
        	totalAVCars+=numAuto;
        }
        else{
        	totalNonAVCars+=numAuto;
        }
        int[] carUsed = new int[numAuto];
        int autoTripID = 1;
        int destHome = 0;
        int origHome = 0;
        double prevCarChangeProb = 0;
        if(globalLoop > 1  && hh.getHidAcrossSample() < probChagingCarOwnership.length)
        	prevCarChangeProb = probChagingCarOwnership[hh.getHidAcrossSample()];
        for ( int i=0; i < aTrips.size(); i++ ) {
        	totalDemand++;
        	AutoTrip trip = aTrips.get(i);
        	int carRepoType = -1;
        	int origMaz = trip.getOrigMaz();
    		int destMaz = trip.getDestMaz();
    		int hhTripId = trip.getHhTripId();
    		int origTaz = geogManager.getMazTazValue(origMaz);
    		int destTaz = geogManager.getMazTazValue(destMaz);      		
    		
    		float tripDistance = trip.getDistance();
    		
    		int tripMode = trip.getMode();
    		
        	double[] carAllocationForTrip = carAllocationResults[CarAllocation.INDEX_CarAllo][i];
        	double[] carAllocationFirstTrip = carAllocationResults[CarAllocation.INDEX_FirstCarTrip][i];
        	double[] carAllocationLastTrip = carAllocationResults[CarAllocation.INDEX_LastCarTrip][i];
        	
            float depEarly = (float)depArrResults[CarAllocation.DEP_EARLY][trip.getPnum()][trips.get(trip.getHhTripId()).getIndivTripId()];
            float depLate = (float)depArrResults[CarAllocation.DEP_LATE][trip.getPnum()][trips.get(trip.getHhTripId()).getIndivTripId()];
            float scheduleDepart = trip.getSchedDepart();
            float scheduleArrive = scheduleDepart+trip.getSchedTime();
            float tripDistanceFromHome = distanceFromHome[destTaz];
            float tripDistanceFromHomeToOrig = distanceFromHome[origTaz];
            String record =null;
            int tripSatisfied = 0;
            int votCat = 1;
           
            double occupancy = 1.0;
			
			int period = CommonProcedures.getInterval((int)(scheduleDepart+depLate-depEarly)/60,(int)((scheduleDepart+depLate-depEarly) % 60)); 
			
			int emptyCarVotCat = 1;
			int emptyTripMode= 1;
        	for( int j = 0; j < carAllocationForTrip.length; j ++){
        	   	double[] SikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i];
            	double[] GikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i];
            	double[] HikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParH][j][i];
        		int carTripAllocation =0;
        		if(carAllocationForTrip[j]>threhsoldRoundUp)
        			carTripAllocation = 1;
        		float autoRecordDistance = 0;
        		
        		for(int s = i+1; s <aTrips.size(); s++){
        			if(SikForTrip[s] > threhsoldRoundUp){
        				carRepoType = 1;
        				break;
        			}
        			else if(GikForTrip[s] > threhsoldRoundUp){
        				carRepoType = 2;
        				break;
        			}
        			else if(HikForTrip[s] > threhsoldRoundUp){
        				carRepoType = 3;
        				break;
        			}
        			
        		}
        		
        		if(carTripAllocation == 1){
        			tripSatisfied= 1;
        			carUsed[j] = 1;
        			// add first empty trip (if any)
            		if(carAllocationFirstTrip[j]>threhsoldRoundUp && trip.getOrigAct() > 0){
            			autoRecordDistance = distanceFromHome[origTaz];
            			float departureTime = (scheduleDepart + depLate - depEarly) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
            			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
            			tripDistanceFromHomeToOrig = distanceFromHome[homeTaz];
            			destHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
            			origHome = 1;
            			record = hh.getId()+","+
            					(j+1)+","+
            					ifAvHh+","+
            					autoTripID +","+
            					0+","+
            					0+","+
            					0+","+
            					TRIP_REPOSITIONING_PURPOSE+","+
            					hh.getHomeMaz()+","+
            					trip.getOrigMaz()+","+
            					geogManager.getMazTazValue(hh.getHomeMaz())+","+
            					geogManager.getMazTazValue(trip.getOrigMaz())+","+
            					"-1"+","+
            					origHome+","+
            					destHome+","+
            					autoRecordDistance +","+
            					tripDistanceFromHomeToOrig+","+
            					tripDistanceFromHome+","+
            					departureTime+","+
            					departureTime + ","+
            					(scheduleDepart + depLate - depEarly)+","+
            					0+","+
            					0;
            			outStreamCar.println( record );  
            			autoTripID++;
            			totalAutoTrips++;
            			totalVMT+=autoRecordDistance;
            			if(ifAvHh == 1){
            				totalAutoAVTrips++;
            				totalEmptyTripsAV++;
            				totalAVVMT+=autoRecordDistance;
            				emptyAVVMT+=autoRecordDistance;
            			}
            			else{
            				totalAutoNonAVTrips++;                			
            			}
            			period = CommonProcedures.getInterval((int)(departureTime)/60,(int)((departureTime) % 60)); 
            			
            			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getOrigMaz()))))
            				autoTripTables[getTripTablePeriod(period)][(emptyTripMode-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]] += tripExpansionFactor;
        				
            			
        			}
            		//Add actual person trip
            		tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
            		tripDistanceFromHomeToOrig = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
            		destHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
            		origHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
        			record = hh.getId()+","+
        					(j+1)+","+
        					ifAvHh+","+
        					autoTripID +","+
        					(hhTripId+1)+","+
        					trip.getPnum()+","+
        					trip.getOrigAct()+","+
        					trip.getDestAct()+","+
        					trip.getOrigMaz()+","+
        					trip.getDestMaz()+","+
        					geogManager.getMazTazValue(trip.getOrigMaz())+","+
        					geogManager.getMazTazValue(trip.getDestMaz())+","+
        					carRepoType  + ","+
        					origHome+","+
        					destHome  + ","+
        					tripDistance + ","+
        					tripDistanceFromHomeToOrig + ","+
        					tripDistanceFromHome+","+
        					trip.getSchedDepart()+","+
        					(trip.getSchedDepart()+depLate-depEarly)+","+
        					(trip.getSchedDepart()+depLate-depEarly+trip.getSchedTime())+","+
        					depEarly+","+
        					depLate;
        			outStreamCar.println( record );  
        			autoTripID++;
        			totalAutoTrips++;
        			totalVMT+=tripDistance;
        			if(ifAvHh == 1){
        				totalAutoAVTrips++;
        				totalLoadedTripsAV++;
        				totalAVVMT+=tripDistance;
        				loadedAVVMT+=tripDistance;
        			}
        			else{
        				totalAutoNonAVTrips++;                			
        			}
        			
        			period = CommonProcedures.getInterval((int)(scheduleDepart+depLate-depEarly)/60,(int)((scheduleDepart+depLate-depEarly) % 60)); 
        			
        			//logger.info(tripMode + " " + votCat + " " + period + " " + origTaz + " " + destTaz);
        			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),destTaz)))
        				autoTripTables[getTripTablePeriod(period)][(tripMode-1)][tazIndices[origTaz]][tazIndices[destTaz]] += tripExpansionFactor;
        			
        			//Add empty trips
        			for(int k =i+1; k<aTrips.size();k++){
        				AutoTrip nextTrip = aTrips.get(k);
        				float nextDepEarly = (float)depArrResults[CarAllocation.DEP_EARLY][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                        float nextDepLate = (float)depArrResults[CarAllocation.DEP_LATE][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                        int nextOrigTaz = geogManager.getMazTazValue(nextTrip.getOrigMaz());
                        int nextDestTaz = geogManager.getMazTazValue(nextTrip.getDestMaz());
                        destHome = nextTrip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
                        origHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
        				if(((SikForTrip[k]>threhsoldRoundUp & trip.getPnum() != nextTrip.getPnum())||GikForTrip[k]>threhsoldRoundUp) && destTaz != nextOrigTaz){ // no need to write intra-zonal car reposition trip
        					autoRecordDistance = sharedDistanceObject.getOffpeakDistanceFromTaz(origTaz)[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
                			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly) ;
                			float arrivalTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly)+ Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
                			if(SikForTrip[k]>threhsoldRoundUp){
                				float depEarlyNext = (float)depArrResults[CarAllocation.DEP_EARLY][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                	            float depLateNext  = (float)depArrResults[CarAllocation.DEP_LATE][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                				departureTime = (nextTrip.getSchedDepart() + depLateNext - depEarlyNext) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
                				arrivalTime = (nextTrip.getSchedDepart() + depLateNext - depEarlyNext);
                			}
                			tripDistanceFromHome = distanceFromHome[nextOrigTaz];
                			tripDistanceFromHomeToOrig = distanceFromHome[destTaz];
        					record = hh.getId()+","+
                					(j+1)+","+
                					ifAvHh+","+
                					autoTripID +","+
                					0+","+
                					0+","+
                					trip.getDestAct()+","+
                					TRIP_REPOSITIONING_PURPOSE+","+
                					trip.getDestMaz()+","+
                					nextTrip.getOrigMaz()+","+
                					origTaz+","+
                					nextOrigTaz+","+
                					"-1"+","+
                					origHome+","+
                					destHome+","+
                					autoRecordDistance+","+
                					tripDistanceFromHomeToOrig+","+
                					tripDistanceFromHome+","+
                					departureTime+","+
                					departureTime+","+
                					arrivalTime+ ","+
                					0+","+
                					0;
                			outStreamCar.println( record ); 
                			autoTripID++;
                			totalAutoTrips++;
                			totalVMT+=autoRecordDistance;
                			if(ifAvHh == 1){
                				totalAutoAVTrips++;
                				totalEmptyTripsAV++;
                				totalAVVMT+=autoRecordDistance;
                				emptyAVVMT+=autoRecordDistance;
                			}
                			else{
                				totalAutoNonAVTrips++;                			
                			}
                			 
                			period = CommonProcedures.getInterval((int)(departureTime)/60,(int)((departureTime) % 60)); 
                			
                			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),nextOrigTaz)))
                				autoTripTables[getTripTablePeriod(period)][(emptyTripMode-1)][tazIndices[origTaz]][tazIndices[nextOrigTaz]] += tripExpansionFactor;
            				
                			
                			break;
        				}
        				//Add two empty trips if car went to home
        				if(HikForTrip[k]>threhsoldRoundUp){
        					autoRecordDistance =distanceToHome[destTaz];
                			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly);
                			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(hh.getHomeMaz())];
                			tripDistanceFromHomeToOrig = distanceFromHome[destTaz];
                			destHome = 1;
                			origHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
        					record = hh.getId()+","+
                					(j+1)+","+
                					ifAvHh+","+
                					autoTripID +","+
                					0+","+
                					0+","+
                					trip.getDestAct()+","+
                					TRIP_REPOSITIONING_PURPOSE+","+
                					trip.getDestMaz()+","+
                					hh.getHomeMaz()+","+
                					geogManager.getMazTazValue(trip.getDestMaz())+","+
                					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                					"-1"+","+
                					origHome+","+
                					destHome+","+
                					autoRecordDistance + ","+
                					tripDistanceFromHomeToOrig+ ","+
                					tripDistanceFromHome+","+
                					departureTime+","+
                					departureTime+","+
                					(departureTime+autoRecordDistance*Float.parseFloat(propertyMap.get("minutes.per.mile")))+","+
                					0+","+
                					0;
                			outStreamCar.println( record );  
                			autoTripID++;
                			totalAutoTrips++;
                			totalVMT+=autoRecordDistance;
                			if(ifAvHh == 1){
                				totalAutoAVTrips++;
                				totalEmptyTripsAV++;
                				totalAVVMT+=autoRecordDistance;
                				emptyAVVMT+=autoRecordDistance;
                			}
                			else{
                				totalAutoNonAVTrips++;                			
                			}
                			
                			                			 
                			period = CommonProcedures.getInterval((int)(departureTime)/60,(int)((departureTime) % 60)); 
                			
                			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz()))))
                    			autoTripTables[getTripTablePeriod(period)][(emptyTripMode-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
            				
                			
                			
                			autoRecordDistance =distanceFromHome[nextOrigTaz];
                			departureTime = (nextTrip.getSchedDepart() +  nextDepLate - nextDepEarly) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
                			
                			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
                			tripDistanceFromHomeToOrig = distanceFromHome[homeTaz];
                			destHome = nextTrip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
                			origHome = 1;
                			record = hh.getId()+","+
                    					(j+1)+","+
                    					ifAvHh+","+
                    					autoTripID +","+
                    					(i+1)+","+
                    					0+","+
                    					0+","+
                    					TRIP_REPOSITIONING_PURPOSE+","+
                    					hh.getHomeMaz()+","+
                    					nextTrip.getOrigMaz()+","+
                    					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                    					geogManager.getMazTazValue(nextTrip.getOrigMaz())+","+
                    					"-1"+","+
                    					origHome+","+
                    					destHome+","+
                    					autoRecordDistance + ","+
                    					tripDistanceFromHomeToOrig+ ","+
                    					tripDistanceFromHome+","+
                    					departureTime+","+
                    					departureTime+","+
                    					(nextTrip.getSchedDepart() +  nextDepLate - nextDepEarly) +","+
                    					0+","+
                    					0;
                    			outStreamCar.println( record );  
                    			autoTripID++;
                    			
                    			totalAutoTrips++;
                    			totalVMT+=autoRecordDistance;
                    			if(ifAvHh == 1){
                    				totalAutoAVTrips++;
                    				totalEmptyTripsAV++;
                    				totalAVVMT+=autoRecordDistance;
                    				emptyAVVMT+=autoRecordDistance;
                    			}
                    			else{
                    				totalAutoNonAVTrips++;                			
                    			}
                    		
                    			period = CommonProcedures.getInterval((int)(departureTime)/60,(int)((departureTime) % 60)); 
                    			
                    			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(nextTrip.getOrigMaz()))))
                    				autoTripTables[getTripTablePeriod(period)][(emptyTripMode-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(nextTrip.getOrigMaz())]] += tripExpansionFactor;
                				
                    			break;
                			}
        				
        				}
        		
        			
        			//Add last empty trip
            		if(carAllocationLastTrip[j]>threhsoldRoundUp && trip.getDestAct() > 0){
            			autoRecordDistance =distanceToHome[destTaz];
            			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly);
            			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(hh.getHomeMaz())];
            			tripDistanceFromHomeToOrig = distanceFromHome[origTaz];
            			destHome = 1;
            			origHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
            			record = hh.getId()+","+
            					(j+1)+","+
            					ifAvHh+","+
            					autoTripID +","+
            					0+","+
            					0+","+
            					trip.getOrigAct()+ ","+
            					TRIP_REPOSITIONING_PURPOSE+","+    
            					trip.getDestMaz()+","+
            					hh.getHomeMaz()+","+
            					geogManager.getMazTazValue(trip.getDestMaz())+","+
            					geogManager.getMazTazValue(hh.getHomeMaz())+","+
            					"-1"+","+ 
            					origHome+","+
            					destHome +","+   
            					autoRecordDistance + ","+
            					tripDistanceFromHomeToOrig+ ","+
            					tripDistanceFromHome+","+
            					departureTime+","+
            					departureTime+","+
            					(departureTime+autoRecordDistance*Float.parseFloat(propertyMap.get("minutes.per.mile")))+","+
            					0+","+
            					0;
            			outStreamCar.println( record );  
            			autoTripID++;
            			totalAutoTrips++;
            			totalVMT+=autoRecordDistance;
            			if(ifAvHh == 1){
            				totalAutoAVTrips++;
            				totalEmptyTripsAV++;
            				totalAVVMT+=autoRecordDistance;
            				emptyAVVMT+=autoRecordDistance;
            			}
            			else{
            				totalAutoNonAVTrips++;                			
            			}
            			
            			
            			 
            			period = CommonProcedures.getInterval((int)(departureTime)/60,(int)((departureTime) % 60)); 
            			
            			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz()))))
            				autoTripTables[getTripTablePeriod(period)][(emptyTripMode-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
        				
            			
            			break;
        			}
        		}
        		            		
        	}
        	//Add unmet demand
        	if(tripSatisfied == 0){
        		//Add actual person trip
        		tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
        		tripDistanceFromHomeToOrig = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
        		destHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
        		origHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
    			record = hh.getId()+","+
    					"-1"+","+
    					ifAvHh+","+
    					autoTripID +","+
    					(hhTripId+1)+","+
    					trip.getPnum()+","+
    					trip.getOrigAct()+","+
    					trip.getDestAct()+","+
    					trip.getOrigMaz()+","+
    					trip.getDestMaz()+","+
    					geogManager.getMazTazValue(trip.getOrigMaz())+","+
    					geogManager.getMazTazValue(trip.getDestMaz())+","+
    					carRepoType  + ","+
    					origHome+","+
    					destHome  + ","+
    					tripDistance + ","+
    					tripDistanceFromHomeToOrig + ","+
    					tripDistanceFromHome+","+
    					trip.getSchedDepart()+","+
    					(trip.getSchedDepart()+depLate-depEarly)+","+
    					(trip.getSchedDepart()+depLate-depEarly+trip.getSchedTime())+","+
    					depEarly+","+
    					depLate;
    			outStreamCar.println( record );  
    			autoTripID++;
    			//add them as taxi
    			
    			 
    			period = CommonProcedures.getInterval((int)(trip.getSchedDepart()+depLate-depEarly)/60,(int)((trip.getSchedDepart()+depLate-depEarly) % 60)); 
    			
    			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),destTaz)))
        			autoTripTables[getTripTablePeriod(period)][(4-1)][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]] += tripExpansionFactor;
				
    			
        	}
      	}
        int hhUnusedCar = 0;
        for(int j = 0; j < numAuto; j++){
        	if(carUsed[j] == 1){
        		totalUsedCars++;
        	}
        	else{
        		totalUnusedCars[carSufficiencyLevel]++;
        		if(ifAvHh == 1){
        			totalUsedAVCars++;
    				hhUnusedCar++;
        		}
        		else{
        			totalUsedNonAVCars++;
        			hhUnusedCar++;
        		}
        		
        	}
        		
        }
        
        // car change probability
        double probCarChange = 0;
        
        probCarChange = (-1)*hhUnusedCar*subtractCarFactor;            
        if(hhNumUnsatisfiedDemand>0){
        	probCarChange = addCarFactor;
        }
        double msaProb = prevCarChangeProb*(1-msaFactor) + probCarChange*msaFactor;
        
        String outRecordHh = hh.getId()+","+hh.getHidAcrossSample()+","+prevCarChangeProb+","+msaProb+","+msaFactor;
        outStreamHh.println(outRecordHh);
                
    }
    outStreamCar.close();    
    outStreamTrip.close();
    outStreamHh.close();
    logger.info(" ");
    logger.info(" -------------------- Car Allocation Report ------------------------");
    logger.info(" ");
    logger.info(String.format( "%-30s","Total Auto Trips Demand = ") + String.format("%,9d",totalDemand));
    logger.info(String.format( "%-30s","Total Auto Unmet Demand = ") + String.format("%,9d",totalDemand - totalLoadedTripsAV - totalAutoNonAVTrips));
    logger.info(String.format( "%-30s","Total Auto Trips = ") + String.format("%,9d",totalAutoTrips));
    logger.info(String.format( "%-30s","Total Auto Non-AV Trips = " )+ String.format("%,9d",totalAutoNonAVTrips));
    logger.info(String.format( "%-30s","Total Auto AV Trips = ") + String.format("%,9d",totalAutoAVTrips));
    logger.info(String.format( "%-30s","Total Loaded AV Trips = ") + String.format("%,9d",totalLoadedTripsAV));
    logger.info(String.format( "%-30s","Total Empty AV Trips = ") + String.format("%,9d",totalEmptyTripsAV));
    logger.info(String.format( "%-30s","Total VMT = ") + String.format("%,.1f",totalVMT));
    logger.info(String.format( "%-30s","Total AV VMT = ") + String.format("%,.1f",totalAVVMT));
    logger.info(String.format( "%-30s","Total Loaded AV VMT = ") + String.format("%,.1f",loadedAVVMT));
    logger.info(String.format( "%-30s","Total Empty AV VMT = ") + String.format("%,.1f",emptyAVVMT));
    logger.info(String.format( "%-30s","Total Non-singular cases = ") + String.format("%,9d", totalNonSingularCases));
    logger.info(" ");
    logger.info(" -------------------- Schedule Adjustment Report ------------------------");
    logger.info(" ");
    logger.info(String.format( "%-30s","Total Extra Wait Time = ") + String.format("%,.1f",totalExtraWaitTime));
    logger.info(String.format( "%-30s","Total Early Departure Time = ") + String.format("%,.1f",totalEarlyDepTime));
    logger.info(" ");
    logger.info(" -------------------- Car Use Report ------------------------");
    logger.info(" ");
    logger.info(String.format( "%-60s","Total Number of Cars = ") + String.format("%,15d",totalCars));
    logger.info(String.format( "%-60s","Total Non-AV Cars = ") + String.format("%,15d",totalNonAVCars));
    logger.info(String.format( "%-60s","Total AV Cars = ") + String.format("%,15d",totalAVCars));
    logger.info(String.format( "%-60s","Total Used Cars = ") + String.format("%,15d",totalUsedCars));
    logger.info(String.format( "%-60s","Total Unused Cars (Car insufficient, car < hhsize) = ") + String.format("%,15d",totalUnusedCars[0]));
    logger.info(String.format( "%-60s","Total Unused Cars (Car sufficient, car = hhsize) = ") + String.format("%,15d",totalUnusedCars[1]));
    logger.info(String.format( "%-60s","Total Unused Cars (Car oversufficient, car > hhsize) = ") + String.format("%,15d",totalUnusedCars[2]));
    logger.info(String.format( "%-60s","Total Unused Non-AV Cars = ") + String.format("%,15d",totalUsedNonAVCars));
    logger.info(String.format( "%-60s","Total Unused AV Cars = ") + String.format("%,15d",totalUsedAVCars));

    for ( int i=0; i < periodLabels.length; i++ ) {
		
		Matrix[] matrices = new Matrix[autoTripTableNames.length];
		for ( int j=0; j < autoTripTableNames.length; j++ ) {
				String description = periodLabels[i] + " period " + autoTripTableNames[j]  ;
				float[][] orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, autoTripTables[i][j] );
				matrices[j] = new Matrix( autoTripTableNames[j], description, orderedTable );
				matrices[j].setExternalNumbers( extNumbers );
			}		
    
		
		matrixHandler.writeMatrixFile( autoTripTableFiles[ i], matrices, autoTripTableNames, matrixType );

	}





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



private float[][] getTripTableOrderedByExternalTazValues( int[] externalTazOrder, float[][] tripTable ) {
	
	float[][] orderedTripTable = new float[tripTable.length][tripTable.length];
	
	for ( int i=0; i < externalTazOrder.length; i++ ) {
		int k = externalTazOrder[i];
		
		for ( int j=0; j < externalTazOrder.length; j++ ) {
			int m = externalTazOrder[j];
			
			orderedTripTable[i][j] = tripTable[k][m]; 
		}

	}
	
	return orderedTripTable;
}

private int getTripTablePeriod(int departureInterval){
	int nextPeriodIndex = 1;
	while ( departureInterval >= periodIntervals[nextPeriodIndex][0] ) {
		nextPeriodIndex++;
		if ( nextPeriodIndex == 5 )
			break;
	}
	
	return (nextPeriodIndex - 1);
}

public double[] getProbabilityOfChangingCarOwnership(String probFileName,String hhidName,String probField) {    	
	
	int[] hidWoSampleValues = Parsing.getIntArrayFromCsvFile( probFileName, hhidName );
	double[] probChange = Parsing.getDoubleArrayFromCsvFile( probFileName, probField );
	
	int maxHidWoSample = 0;
	for ( int i=0; i < hidWoSampleValues.length; i++  ){
		if ( (int)hidWoSampleValues[i] > maxHidWoSample )
			maxHidWoSample = (int)hidWoSampleValues[i];
	}
	double[] hhProbChangeCarOwnership = new double[maxHidWoSample+1];
	
	for ( int i=0; i < hidWoSampleValues.length; i++ ) {
		int hid =(int)hidWoSampleValues[i];
		double prob = probChange[i];
		
		hhProbChangeCarOwnership[hid] = prob;
	}
		    	
	return hhProbChangeCarOwnership;
	
}


public static void main( String[] args ) {

	long start = System.currentTimeMillis();
	
    CarAllocatorMain mainObj = new CarAllocatorMain();

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
	
	HashMap<String,String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
	
	// set up geography manager
	GeographyManager geogManager = GeographyManager.getInstance();
    geogManager.setupGeographyManager(propertyMap);
    
    // set up socio economic data manager
    SocioEconomicDataManager socec = SocioEconomicDataManager.getInstance();
    socec.loadDataFromCsvFile(propertyMap.get("socec.data.file.name"), propertyMap.get("socec.data.maz.field"));
    

	mainObj.runCarAllocator( propertyMap, null, geogManager, socec );
	
	System.out.println ( "Car Tracker finished in " + (int)((System.currentTimeMillis() - start)/1000.0) + " seconds." );
    System.out.println ( "\n" );

    System.exit(0);
}


}

