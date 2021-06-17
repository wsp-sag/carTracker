package appLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import accessibility.GeographyManager;
import accessibility.MatrixDataHandlerIf;
import accessibility.MatrixDataLocalHandler;
import accessibility.MatrixDataServer;
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import algorithms.CarAllocation;
import utility.CommonProcedures;
import utility.ConstantsIf;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.IndexSort;

import fileProcessing.GlobalProperties;
import fileProcessing.PurposeCategories;
import fileProcessing.VehicleTypePreferences;
import objectMapping.AbmObjectTranslater;
import objects.AutoTrip;
import objects.Household;
import objects.HouseholdCarAllocation;
import objects.Trip;


public class WriteCarAllocationOutputFilesMag implements WriteCarAllocationOutputFilesIf {

	public static final int EMPTY_TRIP_MODE = 5;
	
	private static final int MAX_PERSON_TYPE = 8;
	private static final double[] distanceBoundaries = { 2.0, 5.0, 10.0, 20.0, 30.0, 50.0, 75.0, 100.0, 9999 };
	private static final String[] distanceBins = { "0-2", "2-5", "5-10", "10-20", "20-30", "30-50", "50-75", "75-100", "100+" };
	private static final String[] modes = { "sov", "hov2", "hov3+" };

	private static final int TAXI_TNC_MODE_INDEX = 4;

    public static float threhsoldRoundUp = 0.7f;
	private float tripExpansionFactor = 1.0f;
		
//	private int[] periodTotalTrips;
	private int[] autoModeCodes;
//	private int[] transitModeCodes;
//	private int[] otherModeCodes;
	private int[] votThresholds;
	

	private MatrixDataHandlerIf matrixHandler;
	private boolean ifExternalStationsIncluded = false;
//	private boolean outputTripTableSeparatedByAutoTransit;
	private int numberOfPeriods;
	
//	private boolean ifUserClasses;
	private int numberOfVotCategories = 1;
	
	private String[] autoTripTableNames;
	private String[] tripTableFiles;
//	private String[] transitTripTableNames;
//	private String[] transitTripTableFiles;
//	private String[] otherTripTableNames;
//	private String[] otherTripTableFiles;
	

	private float[][][][] cavTripTables;
	private float[][][][] nonCavTripTables;

	private boolean separateCavFiles = false;

	
    public void writeCarAllocationOutputFile( Logger logger, HashMap<String, String> propertyMap, 
    	String outputTripListFilename, String outputDisaggregateCarUseFileName, String outputProbCarChangeFileName,
    	String outputVehTypePurposeSummaryFileName, String outputVehTypePersTypeSummaryFileName, String outputVehTypeDistanceSummaryFileName,
    	List<HouseholdCarAllocation> hhCarAllocationResultsList, GeographyManager geogManager,
    	SharedDistanceMatrixData sharedDistanceObject, SocioEconomicDataManager socec, ConstantsIf constants, VehicleTypePreferences vehicleTypePreferences) {


    	Map<Integer, Integer> purposeIndexMap = new HashMap<>();
    	List<Integer> tripPurposes = PurposeCategories.getPurposeIndices();
    	int index = 0;
    	for ( int purp : tripPurposes )
    		purposeIndexMap.put( purp, (index++) );
    	
    	Map<Integer, Integer> vehTypeIndexMap = new HashMap<>();
    	List<Integer> vehTypes = vehicleTypePreferences.getCategories();
    	index = 0;
    	for ( int type : vehTypes )
   			vehTypeIndexMap.put( type, (index++) );
    	
    	int[][][] tripsByVehTypeByPurpose = new int[modes.length][vehTypeIndexMap.values().size()][tripPurposes.size()];
    	int[][][] tripsByVehTypeByPersonType = new int[modes.length][vehTypeIndexMap.values().size()][MAX_PERSON_TYPE+1];
    	int[][][] tripsByVehTypeByDistanceBin = new int[modes.length][vehTypeIndexMap.values().size()][distanceBoundaries.length];
    	              
        threhsoldRoundUp = Float.parseFloat(propertyMap.get(GlobalProperties.ROUND_UP_THRESHOLD.toString()));
        tripExpansionFactor = 1.0f/Float.parseFloat(propertyMap.get("global.proportion"));
        ifExternalStationsIncluded = Boolean.valueOf(propertyMap.get("include.external.stations")); 


		String separateCavFilesString = propertyMap.get( SEPARATE_CAV_TRIP_TABLES_KEY );
		if ( separateCavFilesString != null )
			separateCavFiles = Boolean.valueOf( separateCavFilesString );

		numberOfPeriods = Integer.valueOf( propertyMap.get(NUM_OUTPUT_PERIODS_KEY));
		
		String[] str =  propertyMap.get( "output.trip.matrix.auto.mode.codes" ).trim().split(",");
		autoModeCodes = new int[str.length];
		for(int i=0;i<str.length;i++)
			autoModeCodes[i]=Integer.parseInt(str[i]);
		
		String tripTableNamesString = propertyMap.get( AUTO_MODE_TABLE_NAMES_KEY );
		autoTripTableNames = utility.Parsing.getStringArrayFromCsvString( tripTableNamesString );
		
		tripTableFiles = new String[numberOfPeriods];

//		int numVotSegments = Integer.valueOf( propertyMap.get( "output.number.vot.segments" ) );
//		if ( numVotSegments > 1 ) {
//			votThresholds = new int[numVotSegments-1];
//			for(int i=0;i<numVotSegments-1;i++){
//				String tempKey = "output.trip.matrices.vot"+String.valueOf(i+1)+".threshold";
//				votThresholds[i]=Integer.valueOf( propertyMap.get(tempKey) );
//			}
//			numberOfVotCategories = numVotSegments;
//		}
		



		tripTableFiles[0] = propertyMap.get("result.path") + propertyMap.get( TRIP_MATRIX_EA_FILE_KEY );
		tripTableFiles[1] = propertyMap.get("result.path") + propertyMap.get( TRIP_MATRIX_AM_FILE_KEY );
		tripTableFiles[2] = propertyMap.get("result.path") + propertyMap.get( TRIP_MATRIX_MD_FILE_KEY );
		tripTableFiles[3] = propertyMap.get("result.path") + propertyMap.get( TRIP_MATRIX_PM_FILE_KEY );
		tripTableFiles[4] = propertyMap.get("result.path") + propertyMap.get( TRIP_MATRIX_EV_FILE_KEY );
		
		// number of modes in output tables = autoTripTableNames.length (typically sov,hov2,hov3+,taxi) + empty trips = 4 + 1.
		int numModeTables = autoTripTableNames.length;
		if ( separateCavFiles ) {
			numModeTables =	numModeTables + 1;
			cavTripTables = new float[numberOfPeriods][numModeTables*numberOfVotCategories][geogManager.getTazValues().length][geogManager.getTazValues().length];
		}
		nonCavTripTables = new float[numberOfPeriods][numModeTables*numberOfVotCategories][geogManager.getTazValues().length][geogManager.getTazValues().length];
		
		int[][] periodIntervals = new int[5][2];
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
		

		String matrixFilesOutputString = propertyMap.get( OUTPUT_TRIP_TABLES_KEY );
		boolean matrixFilesOutput = Boolean.valueOf( matrixFilesOutputString );
		

		String formatString = propertyMap.get( OUTPUT_TRIP_TABLE_FORMAT_KEY );
		MatrixType matrixType = MatrixType.lookUpMatrixType( formatString );
        MatrixDataServer matrixDataServer = new MatrixDataServer();
        matrixHandler = new MatrixDataLocalHandler(matrixDataServer, matrixType);

    int[] tazIndices = geogManager.getTazIndices();

    int[] tazValues = geogManager.getTazValues();
    int[] tazValuesOrder = IndexSort.indexSort( tazValues );
    int[] extNumbers = new int[tazValues.length+1];
   	
    for ( int i=0; i < tazValues.length; i++ ) {
        int k = tazValuesOrder[i];
        extNumbers[i+1] = tazValues[k];
    }		

    String[] periodLabels = new String[]{ "early", "am", "midday", "pm", "late" };


		int globalLoop = Integer.parseInt(propertyMap.get("global.loop"));
		
	    double[] probChagingCarOwnership = null;
	    if(globalLoop > 1)
	    	probChagingCarOwnership = getProbabilityOfChangingCarOwnership( propertyMap.get("hh.car.ownership.correction.file"),propertyMap.get("hh.id.field"),propertyMap.get("hh.prob.car.change.field")  );


      PrintWriter outStreamTrip = null;
      PrintWriter outStreamCar = null;
      PrintWriter outStreamHh = null;
      PrintWriter outStreamPurpSummary = null;
      PrintWriter outStreamPersTypeSummary = null;
      PrintWriter outStreamDistSummary = null;
	    try {
	    	outStreamTrip = new PrintWriter( new BufferedWriter( new FileWriter( outputTripListFilename ) ) );
	        String header1 = "hhid,pnum,tripid,tripRecNum,mode,hhAutoId,vehNum,vehTypeCategory,vehFuelType,vehBodyType,origPurp,destPurp,origMaz,destMaz,origTaz,destTaz,tripDistance,tripDistanceFromHome,tripDistanceToHome,plannedDepartureTime,departureEarly,departureLate,finalDeparture, finalArrival,x1,x2,x3,x4,unsatisfiedResult,singular,numIterationIntegerizing";
	        outStreamTrip.println( header1 );   
	        outStreamCar = new PrintWriter( new BufferedWriter( new FileWriter( outputDisaggregateCarUseFileName ) ) );
	        String header2 = "totalDemand,hhid,carId,aVStatus,autoTripId,autoHhTripId,tripRecNum,tripVehNum,driverId,origPurp,destPurp,origMaz,destMaz,origTaz,destTaz,tripMode,finalMode,carRepositionType,origHome,destHome,tripDistance,distanceFromHomeToOrig,distanceFromHomeToDest,plannedDeparture,finalDeparture,finalArrival,departureEarly,departureLate,recType,recnum";
	        //String header2 = "totalDemand,hhid,carId,aVStatus,autoTripId,autoHhTripId,tripRecNum,tripVehNum,driverId,origPurp,destPurp,origMaz,destMaz,origTaz,destTaz,tripMode,finalMode,carRepositionType,origHome,destHome,tripDistance,distanceFromHomeToOrig,distanceFromHomeToDest,plannedDeparture,finalDeparture,finalArrival,departureEarly,departureLate,parkDurationAtDestination,ParkCostAtDestination,parkDurationAtNextOrigin,parkCostAtNextOrigin";
	        outStreamCar.println( header2 );   
	        outStreamHh = new PrintWriter( new BufferedWriter( new FileWriter( outputProbCarChangeFileName ) ) );
	        String header3 = "hhid,hidAcrossSample,prevIterationCarChangeProb,probCarOwnershipChange,msaFactor";
	        outStreamHh.println( header3 );
	        
	        if ( outputVehTypePurposeSummaryFileName != null && outputVehTypePurposeSummaryFileName.length() > 0 )
	        	outStreamPurpSummary = new PrintWriter( new BufferedWriter( new FileWriter( outputVehTypePurposeSummaryFileName ) ) );
	        if ( outputVehTypePersTypeSummaryFileName != null && outputVehTypePersTypeSummaryFileName.length() > 0 )
	        	outStreamPersTypeSummary = new PrintWriter( new BufferedWriter( new FileWriter( outputVehTypePersTypeSummaryFileName ) ) );
	        if ( outputVehTypeDistanceSummaryFileName != null && outputVehTypeDistanceSummaryFileName.length() > 0 )
	        	outStreamDistSummary = new PrintWriter( new BufferedWriter( new FileWriter( outputVehTypeDistanceSummaryFileName ) ) );
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
	    
	    
	    
	    int sumAutos = 0;
        int sumTripSatisfied = 0;
        int sumTripNotSatisfied = 0;
        
	    
	    int totalDemandMet = 0;	    
	    double addCarFactor = Double.parseDouble(propertyMap.get("add.car.factor"));
	    double subtractCarFactor = Double.parseDouble(propertyMap.get("subtract.car.factor"));
	    
		int taxiModeCode = Integer.parseInt(propertyMap.get("taxi.mode.code"));

	    
	    float msaFactor = 1.0f/globalLoop;
	    
	    for ( HouseholdCarAllocation depArrObj : hhCarAllocationResultsList ) {
	        
	        Household hh = depArrObj.getHousehold();
	        double[][][] depArrResults = depArrObj.getScheduleAdjustmentResults();
	        double[][][] carAllocationResults = depArrObj.getAllocationResult();
	        double[]  unsatisDemandResults = depArrObj.getUnsatisfiedDemandResults();
	        double[][][][] carLinkingResults = depArrObj.getCarLinkingResult();
	        List<Trip> trips = hh.getTrips();
	        List<AutoTrip> aTrips = hh.getAutoTrips();
	        int numAuto = hh.getNumAutos();
	        
	        if(numAuto > 0 && numAuto < hh.getPersons().length )
	        	carSufficiencyLevel = 0;
	        else if(numAuto > 0 && numAuto == hh.getPersons().length   )
	        	carSufficiencyLevel = 1;
	        else 
	        	carSufficiencyLevel = 2;
	        	
	        int homeMaz = hh.getHomeMaz();
        	int[] hhVehFuelTypes = hh.getHhVehFuelTypes();
        	int[] hhVehBodyTypes = hh.getHhVehBodyTypes();
        	int[] hhVehNums = hh.getHhVehNums();
	    	
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
	            float tripDistance = trip.getDistance();
	            float tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
	            float tripDistanceToHome = distanceToHome[geogManager.getMazTazValue(trip.getDestMaz())];
	            int tripVehFuelType = -1;
	            int tripVehBodyType = -1;
	            int tripVehNum = -1;
	            int tripVehTypeCategory = -1;
	            if ( trip.getVehId() > 0 ) {
	            	tripVehFuelType = hhVehFuelTypes[ trip.getVehId()-1 ];
	            	tripVehBodyType = hhVehBodyTypes[ trip.getVehId()-1 ];
	            	tripVehNum = hhVehNums[ trip.getVehId()-1 ];
	            	tripVehTypeCategory = vehicleTypePreferences.getCategory(tripVehFuelType, tripVehBodyType);
	            }
	            String record = hh.getId()+","+ trip.getPnum()+","+ trip.getUniqueTripId()+","+ trip.getTripRecNum()+","+trip.getMode()+","+
            		trip.getVehId()+","+tripVehNum+","+tripVehTypeCategory+","+tripVehFuelType+","+tripVehBodyType+","+
    				trip.getOrigAct()+","+ trip.getDestAct()+","+trip.getOrigMaz()+","+ trip.getDestMaz()+","+ origTaz + ","+ destTaz+","+
    				tripDistance+","+tripDistanceFromHome+","+tripDistanceToHome+","+
            		trip.getSchedDepart()+","+ depEarly+","+ depLate +","+finalDeparture+","+finalArrival+","+
            		xij[0]+","+xij[1]+","+xij[2]+","+xij[3] + ","+ unsatisRes + ","+ singular + ","+ depArrObj.getNumIterationsForIntegerizing();
	                    
	            outStreamTrip.println( record );     
	            int period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(trip.getSchedDepart()+depLate-depEarly, constants), periodIntervals);
  	          //float vot = trip.getValueOfTime();
              //int votCat = getVotCategoryIndex( float vot );
	
				//if(mode == Integer.parseInt(propertyMap.get("taxi.mode.code"))){
	      //    if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz()))))
	    	//        autoTripTables[getTripTablePeriod(period)][4-1][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
				//}	            
	        
	            if ( tripVehTypeCategory > 0 ) {
	            	int vtIndex = vehTypeIndexMap.get(tripVehTypeCategory);
	            	int purpIndex = purposeIndexMap.get(trip.getDestAct());
	            	int ptIndex = hh.getPersons()[trip.getPnum()].getPersonType();
	            	int distBin = getDistanceBinIndex(tripDistance);
	            	int modeIndex = trip.getMode()-1;
	            	tripsByVehTypeByPurpose[modeIndex][vtIndex][purpIndex]++;
	            	tripsByVehTypeByPersonType[modeIndex][vtIndex][ptIndex]++;
	            	tripsByVehTypeByDistanceBin[modeIndex][vtIndex][distBin]++;
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
	    		
	    		Trip hhTrip = trips.get(hhTripId);
	    		int tripRecNum = hhTrip.getTripRecNum();
	    		
	    		float tripDistance = trip.getDistance();
	    		
	    		int tripMode = trip.getMode();
	    		int tripVehNum = -1;
	            if ( hhTrip.getVehId() > 0 ) {
	            	tripVehNum = hhVehNums[ hhTrip.getVehId()-1 ];
	            }
	    		
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
	           
	            double occupancy = 1.0;
				float vot= trip.getValueOfTime();
				int votCat = getVotCategoryIndex( vot );


				int period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(scheduleDepart+depLate-depEarly, constants), periodIntervals); 
				
				int emptyCarVotCat = 1;
				int emptyTripMode= 1;
				int sumCarTripAllocation = 0;
				if ( separateCavFiles )
					emptyTripMode= EMPTY_TRIP_MODE;				
	        	for( int j = 0; j < carAllocationForTrip.length; j ++){
	        	   	double[] SikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i];
	            	double[] GikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i];
	            	double[] HikForTrip = carLinkingResults[CarAllocation.INDEX_SameTripParH][j][i];
	        		int carTripAllocation =0;
	        		if(carAllocationForTrip[j]>threhsoldRoundUp)
	        			carTripAllocation = 1;
	        		sumCarTripAllocation += carTripAllocation;
	        		
	        		if ( sumCarTripAllocation > 1 ) {
		        		String carAllocResult = "\"[" + String.valueOf(carAllocationForTrip[0]);
		        		for( int jj=1; jj < carAllocationForTrip.length; jj++ )
		        			carAllocResult += "," + String.valueOf(carAllocationForTrip[jj]);
		        		carAllocResult += "]\"";
		        		String errorMsg = "*** " + String.format( "hhid=%d, i=%d, j=%d, carAllocationForTrip=%s, pnum=%d", hh.getId(), i, j, carAllocResult, trip.getPnum() ) + " ***";
		        		logger.error(errorMsg);
	        		}
	        		
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
	        			sumTripSatisfied++;
	        			sumAutos++;
	        			carUsed[j] = 1;
	        			// add first empty trip (if any)
	            		if(carAllocationFirstTrip[j]>threhsoldRoundUp && trip.getOrigAct() > 0){
	            			autoRecordDistance = distanceFromHome[origTaz];
	            			float departureTime = (scheduleDepart + depLate - depEarly) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
	            			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
	            			tripDistanceFromHomeToOrig = distanceFromHome[homeTaz];
	            			destHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	            			origHome = 1;
	            			record = totalDemand+","+
	            					hh.getId()+","+
	            					(j+1)+","+
	            					ifAvHh+","+
	            					autoTripID +","+
	            					0+","+
	            					0+","+
	            					tripVehNum+","+
	            					0+","+
	            					0+","+
	            					TRIP_REPOSITIONING_PURPOSE+","+
	            					hh.getHomeMaz()+","+
	            					trip.getOrigMaz()+","+
	            					geogManager.getMazTazValue(hh.getHomeMaz())+","+
	            					geogManager.getMazTazValue(trip.getOrigMaz())+","+
	            					"-1"+","+
	            					emptyTripMode+","+
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
	            					0+","+
	            					1+","+
	            					sumAutos;
	            			
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
	
	            			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(departureTime, constants), periodIntervals); 
	            			
	            			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getOrigMaz())))) {
	            				if ( separateCavFiles )
	            					cavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]] += tripExpansionFactor;
	            				else
	            					nonCavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]] += tripExpansionFactor;
	            			}
	            			
	        			}
	            		//Add actual person trip
	            		tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
	            		tripDistanceFromHomeToOrig = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
	            		destHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	            		origHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
            			record = totalDemand+","+
            					hh.getId()+","+
	        					(j+1)+","+
	        					ifAvHh+","+
	        					autoTripID +","+
	        					(hhTripId+1)+","+
            					tripRecNum+","+
            					tripVehNum+","+
	        					trip.getPnum()+","+
	        					trip.getOrigAct()+","+
	        					trip.getDestAct()+","+
	        					trip.getOrigMaz()+","+
	        					trip.getDestMaz()+","+
	        					geogManager.getMazTazValue(trip.getOrigMaz())+","+
	        					geogManager.getMazTazValue(trip.getDestMaz())+","+
            					tripMode+","+
            					tripMode+","+
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
	        					depLate+","+
		    					2+","+
            					sumAutos;
            			
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
	        			
	        			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(scheduleDepart+depLate-depEarly, constants), periodIntervals);
	        			//logger.info(tripMode + " " + votCat + " " + period + " " + origTaz + " " + destTaz);
	        			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),destTaz))) {
	        				if(ifAvHh == 1 && separateCavFiles)
	        					cavTripTables[period][(tripMode-1)*numberOfVotCategories+(votCat-1)][tazIndices[origTaz]][tazIndices[destTaz]] += tripExpansionFactor;
	        				else
	        					nonCavTripTables[period][(tripMode-1)*numberOfVotCategories+(votCat-1)][tazIndices[origTaz]][tazIndices[destTaz]] += tripExpansionFactor;
	        			}
	        			
	        			//Add empty trips
	        			int e=1;
	        			int ee=1;
	        			for(int k =i+1; k<aTrips.size();k++){
	        				AutoTrip nextTrip = aTrips.get(k);
	        				float nextDepEarly = (float)depArrResults[CarAllocation.DEP_EARLY][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
	                        float nextDepLate = (float)depArrResults[CarAllocation.DEP_LATE][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
	                        int nextOrigTaz = geogManager.getMazTazValue(nextTrip.getOrigMaz());
	                        int nextDestTaz = geogManager.getMazTazValue(nextTrip.getDestMaz());
	                        destHome = nextTrip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	                        origHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	        				if(((SikForTrip[k]>threhsoldRoundUp & trip.getPnum() != nextTrip.getPnum())||GikForTrip[k]>threhsoldRoundUp) && destTaz != nextOrigTaz){ // no need to write intra-zonal car reposition trip
	        					autoRecordDistance = sharedDistanceObject.getOffpeakDistanceFromTaz(geogManager.getMazTazValue(trip.getDestMaz()))[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
	        					//autoRecordDistance = sharedDistanceObject.getOffpeakDistanceFromTaz(origTaz)[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
	                			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly) ;
	                			float arrivalTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly)+ Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
	                			if(SikForTrip[k]>threhsoldRoundUp){
	                				float depEarlyNext = (float)depArrResults[CarAllocation.DEP_EARLY][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
	                	            float depLateNext  = (float)depArrResults[CarAllocation.DEP_LATE][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
	                				departureTime = (nextTrip.getSchedDepart() + depLateNext - depEarlyNext) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
	                				arrivalTime = (nextTrip.getSchedDepart() + depLateNext - depEarlyNext);
	                			}
	                			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
	                			tripDistanceFromHomeToOrig = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
	                			//tripDistanceFromHome = distanceFromHome[nextOrigTaz];
	                			//tripDistanceFromHomeToOrig = distanceFromHome[destTaz];
		            			record = totalDemand+","+
    	            					hh.getId()+","+
	                					(j+1)+","+
	                					ifAvHh+","+
	                					autoTripID +","+
	                					0+","+
	                					0+","+
		            					tripVehNum+","+
	                					0+","+
	                					trip.getDestAct()+","+
	                					TRIP_REPOSITIONING_PURPOSE+","+
	                					trip.getDestMaz()+","+
	                					nextTrip.getOrigMaz()+","+
		            					geogManager.getMazTazValue(trip.getDestMaz())+","+
		            					geogManager.getMazTazValue(nextTrip.getOrigMaz())+","+
	                					//origTaz+","+
	                					//nextOrigTaz+","+
	                					"-1"+","+
		            					emptyTripMode+","+
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
		            					0+","+
		            					(30+e)+","+
		            					sumAutos;
		            			
		            			e++;
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

	                			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(departureTime, constants), periodIntervals); 
	                			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),nextOrigTaz))) {
	                				if ( separateCavFiles )
	                					cavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[origTaz]][tazIndices[nextOrigTaz]] += tripExpansionFactor;
	                				else
	                					nonCavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[origTaz]][tazIndices[nextOrigTaz]] += tripExpansionFactor;
	                			}
	                			
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
		            			record = totalDemand+","+
    	            					hh.getId()+","+
	                					(j+1)+","+
	                					ifAvHh+","+
	                					autoTripID +","+
	                					0+","+
	                					0+","+
		            					tripVehNum+","+
	                					0+","+
	                					trip.getDestAct()+","+
	                					TRIP_REPOSITIONING_PURPOSE+","+
	                					trip.getDestMaz()+","+
	                					hh.getHomeMaz()+","+
	                					geogManager.getMazTazValue(trip.getDestMaz())+","+
	                					geogManager.getMazTazValue(hh.getHomeMaz())+","+
	                					"-1"+","+
		            					emptyTripMode+","+
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
		            					0+","+
		            					(40+ee)+","+
		            					sumAutos;
		            			
		            			
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


	                			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(departureTime, constants), periodIntervals); 
	                			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())))) {
	                				if ( separateCavFiles )
	                					cavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
	                				else
	                					nonCavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
	                			}

	                			autoRecordDistance =distanceFromHome[nextOrigTaz];
	                			departureTime = (nextTrip.getSchedDepart() +  nextDepLate - nextDepEarly) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
	                			
	                			tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(nextTrip.getOrigMaz())];
	                			tripDistanceFromHomeToOrig = distanceFromHome[homeTaz];
	                			destHome = nextTrip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	                			origHome = 1;
		            			record = totalDemand+","+
    	            					hh.getId()+","+
                    					(j+1)+","+
                    					ifAvHh+","+
                    					autoTripID +","+
                    					//(i+1)+","+
                    					0+","+
                    					0+","+
    	            					tripVehNum+","+
	                					0+","+
                    					0+","+
                    					TRIP_REPOSITIONING_PURPOSE+","+
                    					hh.getHomeMaz()+","+
                    					nextTrip.getOrigMaz()+","+
                    					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                    					geogManager.getMazTazValue(nextTrip.getOrigMaz())+","+
	                					"-1"+","+
    	            					emptyTripMode+","+
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
		            					0+","+
		            					(50+ee)+","+
		            					sumAutos;
		            			
		            			ee++;

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

                    			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(departureTime, constants), periodIntervals); 

                    			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(nextTrip.getOrigMaz())))) {
	                				if ( separateCavFiles )
	                					cavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(nextTrip.getOrigMaz())]] += tripExpansionFactor;
	                				else
	                					nonCavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]][tazIndices[geogManager.getMazTazValue(nextTrip.getOrigMaz())]] += tripExpansionFactor;
                    			}
                    			
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
	            			record = totalDemand+","+
   	            					hh.getId()+","+
	            					(j+1)+","+
	            					ifAvHh+","+
	            					autoTripID +","+
	            					0+","+
                					0+","+
	            					tripVehNum+","+
	            					0+","+
	            					trip.getOrigAct()+ ","+
	            					TRIP_REPOSITIONING_PURPOSE+","+    
	            					trip.getDestMaz()+","+
	            					hh.getHomeMaz()+","+
	            					geogManager.getMazTazValue(trip.getDestMaz())+","+
	            					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                					"-1"+","+
	            					emptyTripMode+","+
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
	            					0+","+
	            					6+","+
	            					sumAutos;
	            			
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

	            			period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(departureTime, constants), periodIntervals); 

	            			if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(trip.getDestMaz())) && !ArrayUtils.contains(geogManager.getExternalStations(),geogManager.getMazTazValue(hh.getHomeMaz())))) {
	            				if ( separateCavFiles )
	            					cavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
	            				else
	            					nonCavTripTables[period][(emptyTripMode-1)*numberOfVotCategories+(emptyCarVotCat-1)][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]][tazIndices[geogManager.getMazTazValue(hh.getHomeMaz())]] += tripExpansionFactor;
	            			}
		                    
			        		break;
			        	}
			        	
	        		}
	        		            		
	        	}
	        	//Add unmet demand
	        	if(tripSatisfied == 0){
	        		sumTripNotSatisfied++;
	        		sumAutos++;
	        		//Add actual person trip
	        		tripDistanceFromHome = distanceFromHome[geogManager.getMazTazValue(trip.getDestMaz())];
	        		tripDistanceFromHomeToOrig = distanceFromHome[geogManager.getMazTazValue(trip.getOrigMaz())];
	        		destHome = trip.getDestAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
	        		origHome = trip.getOrigAct() == PurposeCategories.HOME.getIndex() ? 1 : 0;
        			record = totalDemand+","+
           					hh.getId()+","+
	    					"-1"+","+
	    					ifAvHh+","+
	    					autoTripID +","+
	    					(hhTripId+1)+","+
	    					tripRecNum+","+
        					tripVehNum+","+
	    					trip.getPnum()+","+
	    					trip.getOrigAct()+","+
	    					trip.getDestAct()+","+
	    					trip.getOrigMaz()+","+
	    					trip.getDestMaz()+","+
	    					geogManager.getMazTazValue(trip.getOrigMaz())+","+
	    					geogManager.getMazTazValue(trip.getDestMaz())+","+
        					tripMode+","+
        					taxiModeCode+","+
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
	    					depLate+","+
							7+","+
        					sumAutos;
        			
	    			outStreamCar.println( record );  
	    			autoTripID++;
	    			//add them as taxi


//		            int tripCarType = VehicleTypeCategories.getInstance().getTncType();
//		            if ( tripCarType > 0 ) {
//		            	int vtIndex = vehTypeIndexMap.get(tripCarType);
//		            	int purpIndex = purposeIndexMap.get(trip.getDestAct());
//		            	int ptIndex = hh.getPersons()[trip.getPnum()].getPersonType();
//		            	int distBin = getDistanceBinIndex(tripDistanceFromHome);
//		            	tripsByVehTypeByPurpose[vtIndex][purpIndex]++;
//		            	tripsByVehTypeByPersonType[vtIndex][ptIndex]++;
//		            	tripsByVehTypeByDistanceBin[vtIndex][distBin]++;
//		            }
	    			
		            period = getTripTablePeriod(CommonProcedures.convertMinutesToInterval(trip.getSchedDepart()+depLate-depEarly, constants), periodIntervals); 
		            if(!ifExternalStationsIncluded ||(!ArrayUtils.contains(geogManager.getExternalStations(),origTaz) && !ArrayUtils.contains(geogManager.getExternalStations(),destTaz))) {
	    				if( ifAvHh == 1 && separateCavFiles )
	        				cavTripTables[period][(TAXI_TNC_MODE_INDEX-1)*numberOfVotCategories+(votCat-1)][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]] += tripExpansionFactor;
	    				else
	    					nonCavTripTables[period][(TAXI_TNC_MODE_INDEX-1)*numberOfVotCategories+(votCat-1)][tazIndices[geogManager.getMazTazValue(trip.getOrigMaz())]][tazIndices[geogManager.getMazTazValue(trip.getDestMaz())]] += tripExpansionFactor;
		            }
		            
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
	    
	    logger.info( "sumAutos=" + sumAutos + ", sumTripSatisfied=" + sumTripSatisfied + ", sumTripNotSatisfied=" + sumTripNotSatisfied );
	    
	    outStreamCar.close();    
	    outStreamTrip.close();
	    outStreamHh.close();

	    
        writePurposeSummaryFile(tripPurposes, vehicleTypePreferences, vehTypeIndexMap, tripsByVehTypeByPurpose, outStreamPurpSummary );
        writePersTypeSummaryFile( vehicleTypePreferences, vehTypeIndexMap, tripsByVehTypeByPersonType, outStreamPersTypeSummary );
        writeDistanceSummaryFile( vehicleTypePreferences, vehTypeIndexMap, tripsByVehTypeByDistanceBin, outStreamDistSummary );

        
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
	    logger.info(" -------------------- Car Use Report for HHs with Auto Trips ------------------------");
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


	    if ( matrixFilesOutput ) {
	    	
			logger.info( "writing trip matrix files." );
	
		    int offset = 0;
		    if ( separateCavFiles )
		    	offset = numModeTables;
		    
		    for ( int i=0; i < periodLabels.length; i++ ) {
	
				Matrix[] matrices = new Matrix[numModeTables];
				String[] tripTableNames = new String[numModeTables];
				if ( separateCavFiles ) {
					matrices = new Matrix[numModeTables*2];
					tripTableNames = new String[numModeTables*2];
				}
				
				if ( separateCavFiles ) {
					
	    			for ( int j=0; j < numModeTables-1; j++ ) {
	  					String description = periodLabels[i] + " period C/AV " + autoTripTableNames[j] + " trips";
	  					float[][] orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, cavTripTables[i][j], tazValues );
						tripTableNames[j] = autoTripTableNames[j]+"_cav";
	  					matrices[j] = new Matrix( tripTableNames[j], description, orderedTable );
	  					matrices[j].setExternalNumbers( extNumbers );
	    			}
	    
	  				String description = periodLabels[i] + " period C/AV empty trips";
	  				float[][] orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, cavTripTables[i][(numModeTables-1)], tazValues );
					tripTableNames[(numModeTables-1)] = "empty_cav";
	  				matrices[(numModeTables-1)] = new Matrix( tripTableNames[(numModeTables-1)], description, orderedTable );
	  				matrices[(numModeTables-1)].setExternalNumbers( extNumbers );
	  				
	    			for ( int j=0; j < numModeTables-1; j++ ) {
	  					description = periodLabels[i] + " period non-C/AV " + autoTripTableNames[j] + " trips";
	  					orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, nonCavTripTables[i][j], tazValues );
						tripTableNames[j+offset] = autoTripTableNames[j];
	  					matrices[j+offset] = new Matrix( tripTableNames[j+offset], description, orderedTable );
	  					matrices[j+offset].setExternalNumbers( extNumbers );
	    			}
	    
	  				description = periodLabels[i] + " period non-C/AV empty trips";
	  				orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, nonCavTripTables[i][(numModeTables-1)], tazValues );
	  				tripTableNames[(numModeTables-1)+offset] = "empty";
	  				matrices[(numModeTables-1)+offset] = new Matrix( tripTableNames[(numModeTables-1)+offset], description, orderedTable );
	  				matrices[(numModeTables-1)+offset].setExternalNumbers( extNumbers );
	
				}
				else {
					
	    			for ( int j=0; j < numModeTables; j++ ) {
	  					String description = periodLabels[i] + " period " + autoTripTableNames[j] + " trips";
	  					float[][] orderedTable = getTripTableOrderedByExternalTazValues( tazValuesOrder, nonCavTripTables[i][j], tazValues );
						tripTableNames[j] = autoTripTableNames[j];
	  					matrices[j] = new Matrix( tripTableNames[j], description, orderedTable );
	  					matrices[j].setExternalNumbers( extNumbers );
	    			}
	    
				}
	  				
	  			
	  			logger.info( "writing file: " + tripTableFiles[i] + ", matrixType: " + matrixType );
	  			for ( String table : tripTableNames )
	  	  			logger.info( "        " + table );
	
	  			matrixHandler.writeMatrixFile( tripTableFiles[i], matrices, tripTableNames, matrixType );

		    }
		    
	    }
	
	}

	private void writePurposeSummaryFile(List<Integer> tripPurposes, VehicleTypePreferences vehicleTypePreferences, Map<Integer, Integer> vehTypeIndexMap, int[][][] tripsByVehTypeByPurpose, PrintWriter outStreamPurpSummary) {
		if ( outStreamPurpSummary != null ) {
        	
	        String header = "n,category,fuelType,bodyType,carSize,tripMode";
	        for ( int purp : tripPurposes )
	        	header += ",purp_" + purp;
	        outStreamPurpSummary.println( header );
	        
	        int n = 1;
	        for ( int i=0; i < modes.length; i++   ) {

		        for ( int vehType : vehTypeIndexMap.keySet() ) {
		        	String record = (n++) +
		        					"," + vehicleTypePreferences.getCategory(vehType-1) +
		        					"," + vehicleTypePreferences.getFuelType(vehType-1) +
		        					"," + vehicleTypePreferences.getBodyType(vehType-1) +
		        					"," + vehicleTypePreferences.getVehSize(vehType-1) +
		        					"," + modes[i];
		        	int vtIndex = vehTypeIndexMap.get(vehType);
		        	int[] values = tripsByVehTypeByPurpose[i][vtIndex];
		        	for ( int p=0; p < values.length; p++ )
		        		record += "," + values[p];
			        outStreamPurpSummary.println( record );
		        }
	        	
	        }

		    outStreamPurpSummary.close();
        }
	}

	private void writePersTypeSummaryFile( VehicleTypePreferences vehicleTypePreferences, Map<Integer, Integer> vehTypeIndexMap, int[][][] tripsByVehTypeByPersonType, PrintWriter outStream) {
		if ( outStream != null ) {
        	
	        String header = "n,category,fuelType,bodyType,carSize,tripMode";
	        for ( int persType=1; persType <= MAX_PERSON_TYPE; persType++ )
	        	header += ",persType_" + persType;
	        outStream.println( header );
	        
	        int n = 1;
	        for ( int i=0; i < modes.length; i++   ) {

		        for ( int vehType : vehTypeIndexMap.keySet() ) {
		        	String record = (n++) +
		        					"," + vehicleTypePreferences.getCategory(vehType-1) +
		        					"," + vehicleTypePreferences.getFuelType(vehType-1) +
		        					"," + vehicleTypePreferences.getBodyType(vehType-1) +
		        					"," + vehicleTypePreferences.getVehSize(vehType-1) +
		        					"," + modes[i];
		        	int vtIndex = vehTypeIndexMap.get(vehType);
		        	int[] values = tripsByVehTypeByPersonType[i][vtIndex];
		        	for ( int p=1; p < values.length; p++ )
		        		record += "," + values[p];
		        	outStream.println( record );
		        }
	        	
	        }

	        outStream.close();
        }
	}

	private void writeDistanceSummaryFile( VehicleTypePreferences vehicleTypePreferences, Map<Integer, Integer> vehTypeIndexMap, int[][][] tripsByVehTypeByDistance, PrintWriter outStream) {
		if ( outStream != null ) {
        	
	        String header = "n,category,fuelType,bodyType,carSize,tripMode";
	        for ( int bin=0; bin < distanceBins.length; bin++ )
	        	header += ",\"" + distanceBins[bin] + "\"";
	        outStream.println( header );

	        int n = 1;
	        for ( int i=0; i < modes.length; i++   ) {
	        	
		        for ( int vehType : vehTypeIndexMap.keySet() ) {
		        	String record = (n++) +
		        					"," + vehicleTypePreferences.getCategory(vehType-1) +
		        					"," + vehicleTypePreferences.getFuelType(vehType-1) +
		        					"," + vehicleTypePreferences.getBodyType(vehType-1) +
		        					"," + vehicleTypePreferences.getVehSize(vehType-1) +
		        					"," + modes[i];
		        	int vtIndex = vehTypeIndexMap.get(vehType);
		        	int[] values = tripsByVehTypeByDistance[i][vtIndex];
		        	for ( int b=0; b < values.length; b++ )
		        		record += "," + values[b];
		        	outStream.println( record );
		        }
	        	
	        }

	        outStream.close();
        }
	}

	private int getVotCategoryIndex(float vot) {
		int votCat = 1;
		if (numberOfVotCategories > 1) {
			if (vot > votThresholds[numberOfVotCategories - 2]) {
				votCat = numberOfVotCategories;
			} else {
				for (int v = 1; v < numberOfVotCategories - 1; v++) {
					if (vot > votThresholds[v - 1] && vot <= votThresholds[v]) {
						votCat = v + 1;
						break;
					}
				}
			}
		}
		return votCat;
	}

	/**
	 * for the given distance, return index such that distanceBoundaries[index-1] <= distance < distanceBoundaries[index]
	 */
	private int getDistanceBinIndex(double distance) {
		int bin = 0;
		while ( distanceBoundaries[bin] <= distance )
			bin++;
		return bin;
	}
}

