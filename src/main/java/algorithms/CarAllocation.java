package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import accessibility.GeographyManager;
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import appLayer.CarAllocatorMain;


import fileProcessing.GlobalProperties;
import fileProcessing.ParameterReader;
import fileProcessing.PurposeCategories;
import fileProcessing.VehicleTypePreferences;
import fileProcessing.WorksheetWriter;
import objects.AutoTrip;
import objects.Household;
import objects.Person;
import objects.Trip;
import utility.Util;

public class CarAllocation
{

    private Logger logger = Logger.getLogger(CarAllocation.class);

    //private static final String SOLVER_TYPE = "GLPK_MIXED_INTEGER_PROGRAMMING";
    // public static String SOLVER_TYPE = "CBC_MIXED_INTEGER_PROGRAMMING";
    //private static final String SOLVER_TYPE = "CLP_LINEAR_PROGRAMMING";
    //private static final String SOLVER_TYPE = "GLPK_LINEAR_PROGRAMMING";



	private static final int HOME_INDEX = 0;
	private static final int HOME = 0;
	private static final int WORK_INDEX = 1;
	private static final int WORK = 1;
	private static final int UNIV_INDEX = 2;
	private static final int UNIV = 2;
	private static final int SCHOOL_INDEX = 3;
	private static final int SCHOOL = 3;
	private static final int ESCORT_INDEX = 42;
	private static final int ESCORT = 4;
	private static final int PURE_ESCORT_INDEX = 411;
	private static final int RIDE_SHARING_INDEX = 412;
	private static final int SHOP_INDEX = 5;
	private static final int SHOP = 5;
	private static final int MAINT_INDEX = 6;
	private static final int MAINT = 6;
	private static final int MAINT_HH_INDEX = 61;
	private static final int MAINT_PERS_INDEX = 62;
	private static final int EAT_OUT_INDEX = 7;
	private static final int EAT_OUT = 7;
	private static final int EAT_BRKFST_INDEX = 71;
	private static final int EAT_LUNCH_INDEX = 72;
	private static final int EAT_DINNER_INDEX = 73;
	private static final int VISITING_INDEX = 8;
	private static final int VISITING = 8;
	private static final int DISCR_INDEX = 9;
	private static final int DISCR = 9;
	private static final int AT_WORK_INDEX = 11;
	private static final int AT_WORK = 11;
	private static final int AT_WORK_BUS_INDEX = 12;
	private static final int AT_WORK_LUN_INDEX = 13;
	private static final int AT_WORK_OTH_INDEX = 14;
	private static final int BUSINESS_INDEX = 15;
	private static final int BUSINESS_ASU_INDEX = 16;

	private static final int PERSON_TYPE_UNIV_STUDENT = 3;
	private static final int PERSON_TYPE_DRV_AGE_STUDENT = 6;

	private static final int SOV_MODE = 1;
	private static final int HOV2_MODE = 2;
	private static final int HOV3p_MODE = 3;
	private static final int TNC_TAXI_MODE = 37;

	private static final String SMALL_CAR_SIZE = "sm";
	
	private static final List<Integer> MANDATORY_PURPOSE_INDICES = Arrays.asList( PurposeCategories.WORK.getIndex(), PurposeCategories.BUSINESS.getIndex(), 
					PurposeCategories.SCHOOL.getIndex(), PurposeCategories.UNIVERSITY.getIndex(), PurposeCategories.CAMPUS_BUSINESS.getIndex() );

	private static final List<Integer> ESCORTING_PURPOSE_INDICES = Arrays.asList( PurposeCategories.ESCORT.getIndex(), PurposeCategories.ESCORT_OTHER.getIndex(), 
					PurposeCategories.ESCORT_SCHOOL.getIndex(), PurposeCategories.ESCORT_PE.getIndex(), PurposeCategories.ESCORT_RS.getIndex() );

	private static final List<Integer> STUDENT_PERSON_TYPE_INDICES = Arrays.asList( PERSON_TYPE_UNIV_STUDENT, PERSON_TYPE_DRV_AGE_STUDENT );

	//private static final List<Integer> HOV_MODE_INDICES = Arrays.asList( HOV2_MODE, HOV3p_MODE );


    public static final int DEP_EARLY= 0;
    public static final int DEP_LATE = 1;
    public static final int DEP_ARR_INDICES = 2;
    public static final int NEGATIVE_DEPART_TIME_ADJUSTMENT = 720;



    //private static final int M_BIG_CONSTANT = Integer.MAX_VALUE;
    private static final long M_BIG_CONSTANT = (long) (Math.pow(2, 20)-1);
    private static final long NON_AV_REPO_COST = (long) (Math.pow(2, 15)-1);

    
	private float parkingScale;


    private static final int MAX_ACT_INDEX = 9;

    private static int varIndexIJ = 0;
    private static int varIndexI = 0;
    private static int varIndexIKJ = 0;
    private static int varIndexP = 0;
    private static int varIndexJ = 0;

    public static final int INDEX_CarAllo = varIndexIJ++;  // index for car allocation
    public static final int INDEX_UnSatis = varIndexI++; // index for unsatisfied demand
    public static final int INDEX_CarLink = varIndexIKJ++; // Car linking variable
    public static final int INDEX_SameTripParkDi = varIndexIKJ++; // variable for Sik (pairs of trips seved by same car with parking at D
    public static final int INDEX_SameTripParkOk = varIndexIKJ++; // variable for Gik (pairs of trips seved by same car with parking at O of k
    public static final int INDEX_SameTripParH = varIndexIKJ++; // variable for Hik (pairs of trips seved by same car with parking at home
    public static final int INDEX_DepEarly = varIndexP++;
    public static final int INDEX_DepLate = varIndexP++;
    public static final int INDEX_FirstCarTrip = varIndexIJ++; // First trip of car from home
    public static final int INDEX_LastCarTrip = varIndexIJ++; // Last trip of car to home
    public static final int INDEX_UnusedCar = varIndexJ++; // Unused car



    private static final int NUM_IJ_VARIABLE_INDICES = varIndexIJ;
    private static final int NUM_IJK_VARIABLE_INDICES = varIndexIKJ;
    private static final int NUM_I_VARIABLE_INDICES = varIndexI;
    private static final int NUM_P_VARIABLE_INDICES = varIndexP;
    private static final int NUM_J_VARIABLE_INDICES = varIndexJ;

    private static int constraintRepoIndex = 0;
    private static final int INDEX_3_1 = constraintRepoIndex++;
    private static final int INDEX_4_1 = constraintRepoIndex++;
    private static final int INDEX_4_2 = constraintRepoIndex++;
//    private static final int INDEX_4_3 = constraintRepoIndex++;
//    private static final int INDEX_4_4 = constraintRepoIndex++;
    private static final int NUM_CONSTRAINT_REPO_INDICES = constraintRepoIndex;

    private static int constraintTripIndex = 0;
    private static final int INDEX_1_1 = constraintTripIndex++;
    private static final int NUM_CONSTRAINT_TRIP_INDICES = constraintTripIndex;

    private static int constraintScheduleIndex = 0;
    private static final int INDEX_5_0 = constraintScheduleIndex++;
    private static final int INDEX_6_0 = constraintScheduleIndex++;
    private static final int NUM_CONSTRAINT_SCHEDULE_INDICES = constraintScheduleIndex;

    private static int constraintIJIndex = 0;
    private static final int INDEX_2_1 = constraintIJIndex++;
    private static final int INDEX_2_2 = constraintIJIndex++;
//    private static final int INDEX_2_3 = constraintIJIndex++;
//    private static final int INDEX_2_4 = constraintIJIndex++;
    private static final int NUM_CONSTRAINT_IJ_INDICES = constraintIJIndex;

    private static int constraintJIndex = 0;
    private static final int INDEX_5_1 = constraintJIndex++;
    private static final int INDEX_5_2 = constraintJIndex++;
    private static final int NUM_CONSTRAINT_J_INDICES = constraintJIndex;

    // Activity duration penalties and thresholds
    private float[][] UNSAT_DEM;
    private float[][] D_EARLY_FOR;
    private float[][] D_LATE_FOR;
    private float[][] D_EARLY_FROM;
    private float[][] D_LATE_FROM;

    private MPObjective objective;

	private MPVariable[][][][] ofVarsIJK;
	private MPVariable[][][] ofVarsIJ;
	private MPVariable[][] ofVarsI;
	private MPVariable[][][] ofVarsP;
	private MPVariable[][] ofVarsJ;

	private List<Float> ofCoeffList;
	private List<String> variableNameList;
	private List<String> constraintNameList;

	private Map<Integer, Integer> purposeMap;

	private SocioEconomicDataManager socec;
	private GeographyManager geogManager;
	private SharedDistanceMatrixData sharedDistanceObject;
	private HashMap<String,String> propertyMap;

	private VehicleTypePreferences vehicleTypePreferences;
	
	private float unsatisfiedDemandDistancePenalty = 0;
	//private float repositionCostPerMile = 0;
	private float usualDrBonus = 0;
	private float unusedCarBonus = 0;
	private float minutesPerMile = 0;
	private float intraZonalGikSamePersonPenalty;
	private int logHhId = -1;
	private float minimumActivityDuration;
	boolean reportFinalSolution = false;

    static {
    	synchronized( CarAllocatorMain.class ) {
        	if (! CarAllocatorMain.ortoolsLibLoaded  ) {
//        		
//                Properties sysProps = System.getProperties();
//                String value = (String) sysProps.get("java.class.path");
//        		
//                Loader.loadNativeLibraries();
        	      System.loadLibrary("jniortools");
            	CarAllocatorMain.ortoolsLibLoaded = true;
        	}
    	}
    }

	

    public CarAllocation( ParameterReader parameterInstance, HashMap<String,String> propertyMap, SocioEconomicDataManager socec, GeographyManager geogManager, VehicleTypePreferences vehicleTypePreferences ) {
    	this.propertyMap = propertyMap;
    	this.socec = socec;
    	this.geogManager = geogManager;
    	this.vehicleTypePreferences = vehicleTypePreferences;
    	parkingScale = Float.parseFloat(propertyMap.get("longterm.parking.daily.scale"));
    	sharedDistanceObject= SharedDistanceMatrixData.getInstance(propertyMap, geogManager);

    	setParameterArrayValues( parameterInstance );

		// create a Map to translate purpose indices in the datastore to purpose indices in the Paramters table
		createPurposeTranslationMap();
		String test = propertyMap.get(GlobalProperties.UNSAT_DEMAND_DISTANCE_PENALTY.toString());
		unsatisfiedDemandDistancePenalty = Float.parseFloat(propertyMap.get(GlobalProperties.UNSAT_DEMAND_DISTANCE_PENALTY.toString()));
		//repositionCostPerMile = Float.parseFloat(propertyMap.get(GlobalProperties.CAR_REPOSITION_COST_PER_MILE.toString()));
		usualDrBonus = Float.parseFloat(propertyMap.get(GlobalProperties.USUAL_DRIVER_BONUS.toString()));
		minutesPerMile = Float.parseFloat(propertyMap.get(GlobalProperties.MINUTES_PER_MILE.toString()));
		unusedCarBonus = Float.parseFloat(propertyMap.get(GlobalProperties.UNUSED_CAR_BONUS.toString()));
		intraZonalGikSamePersonPenalty = Float.parseFloat(propertyMap.get(GlobalProperties.INTRA_ZONAL_GIK_SAME_PERSON.toString()));
		logHhId = Integer.valueOf( propertyMap.get( GlobalProperties.HHID_LOG_REPORT_KEY.toString() ) );
		minimumActivityDuration= Float.parseFloat(propertyMap.get("min.activity.duration"));

    }


    public MPSolver setupLp( Household hh, Boolean logProgress, int endOfSimulationMinute,int[][] xijIntergerization, int[][] xijFixFlag,int[][][] sikjIntergerization, int[][][] sikjFixFlag,int[][][] gikjIntergerization, int[][][] gikjFixFlag, String solverType, int iterNumForIntegerizing ) {

        MPSolver solver = null;
        try {
            solver = new MPSolver( "SolveCbc", MPSolver.OptimizationProblemType.valueOf( solverType ) );
        }

        catch (java.lang.IllegalArgumentException e) {
            throw new Error(e);
        }

        variableNameList = new ArrayList<String>();
        constraintNameList = new ArrayList<String>();
        ofCoeffList = new ArrayList<Float>();
        if(logProgress)
        	logger.info("household id = "+hh.getId());

    	setupObjectiveFunctionVariablesAndCoefficients( hh, solver, endOfSimulationMinute,xijIntergerization,xijFixFlag,sikjIntergerization, sikjFixFlag,gikjIntergerization, gikjFixFlag);

    	setupContraints( hh, solver, endOfSimulationMinute );
    	reportFinalSolution = false;
    	if(logHhId == hh.getId()){
    		reportInitialLp( solver, iterNumForIntegerizing );
    		reportFinalSolution= true;
    	}

    	return solver;

    }



    private void setupObjectiveFunctionVariablesAndCoefficients( Household hh, MPSolver solver, int endOfSimulationMinute, int[][] xijIntegerization,
    		int[][] xijFixFlag,int[][][] sikjIntegerization, int[][][] sikjFixFlag,int[][][] gikjIntegerization, int[][][] gikjFixFlag ) {

    	try {

        	String name = null;

        	objective = solver.objective();


        	Person[] persons = hh.getPersons();
        	List<Trip> trips = hh.getTrips();
        	List<AutoTrip> autoTrips = hh.getAutoTrips();

        	int[] persTripCounts = new int[persons.length];
        	int[] tripIdForDriver = new int[autoTrips.size()];
        	for ( int i=0; i < autoTrips.size();i++ ) {
        		int pnum = autoTrips.get(i).getPnum();
        		tripIdForDriver[i] = persTripCounts[pnum]; 
        		persTripCounts[pnum]++;
        	}
        	
        	int numAutos = hh.getNumAutos();
        	int homeMaz = hh.getHomeMaz();
        	int[] hhVehFuelTypes = hh.getHhVehFuelTypes();
        	int[] hhVehBodyTypes = hh.getHhVehBodyTypes();

        	int homeTaz = geogManager.getMazTazValue(homeMaz);

        	float[] distanceFromHome = sharedDistanceObject.getOffpeakDistanceFromTaz(homeTaz);
        	float[] distanceToHome = sharedDistanceObject.getOffpeakDistanceToTaz(homeTaz);

        	double[] parkingRateHr = socec.getDoubleFieldByMazValue(propertyMap.get(GlobalProperties.PARKING_HOURLY_FIELD.toString()));
        	double[] parkingRateMonth = socec.getDoubleFieldByMazValue(propertyMap.get(GlobalProperties.PARKING_MONTHLY_FIELD.toString()));


        	ofVarsIJ = new MPVariable[NUM_IJ_VARIABLE_INDICES][autoTrips.size()][numAutos];
        	ofVarsIJK = new MPVariable[NUM_IJK_VARIABLE_INDICES][autoTrips.size()][autoTrips.size()][numAutos];
        	ofVarsI = new MPVariable[NUM_I_VARIABLE_INDICES][autoTrips.size()];
        	ofVarsP = new MPVariable[NUM_P_VARIABLE_INDICES][persons.length][];
        	ofVarsJ = new MPVariable[NUM_J_VARIABLE_INDICES][numAutos];


        	
    		for ( int j=0; j < numAutos; j++ )     {
    			ofVarsJ[INDEX_UnusedCar ][j] = solver.makeNumVar( 0.0, 1, ( name = "CarUnused_"+j ) );
                objective.setCoefficient( ofVarsJ[INDEX_UnusedCar][j], unusedCarBonus);
                ofCoeffList.add(unusedCarBonus );
        		variableNameList.add( name );
    		}
        	for ( int i=0; i < autoTrips.size();i++ ) {

        		AutoTrip aTrip = autoTrips.get(i);
        		int origMaz = aTrip.getOrigMaz();
        		int destMaz = aTrip.getDestMaz();

        		int origTaz = geogManager.getMazTazValue(origMaz);
        		int destTaz = geogManager.getMazTazValue(destMaz);

        		float tripDistance = aTrip.getDistance();

        		int ao = purposeMap.get( aTrip.getOrigAct() );
        		int ad = purposeMap.get( aTrip.getDestAct() );

        		int pt = persons[aTrip.getPnum()].getPersonType();

        		double parkCostSik = 0;
        		double parkCostGik = 0;

        		int personUsualCarId = persons[aTrip.getPnum()].getUsualCarId();

        		float[] distanceFromEndTrip = sharedDistanceObject.getOffpeakDistanceFromTaz(destTaz);




        		// IJ Variables (trip and auto)
        		for ( int j=0; j < numAutos; j++ ) {
        			
        			int usualDrDummy = personUsualCarId == j ? 1 : 0;

        			float diffPnumAuto = Math.abs(j-aTrip.getPnum());
        			// F6
        			float lowerBound = 0;
        			float uppperBound = 1;

        			if(xijIntegerization!= null){
        				if(xijFixFlag[i][j] == 1){
	        				lowerBound = xijIntegerization[i][j];
	        				uppperBound = xijIntegerization[i][j];
        				}
        			}
        			
//        			double[] disutils = {-1,0,0,0,0};
        			int fuelType = hhVehFuelTypes[j];
        			int bodyType = hhVehBodyTypes[j];
        			
        			float udDisutil = vehicleTypePreferences.getUsualDriverDisutil( fuelType, bodyType );
        			List<Float> purpDisutil = vehicleTypePreferences.getPurposeDisutil( fuelType, bodyType );
        			List<Float> modeDisutil = vehicleTypePreferences.getModeDisutil( fuelType, bodyType );
        			List<Float> drvPersTypeDisutil = vehicleTypePreferences.getDrvPersTypePrefDisutil( fuelType, bodyType );
        			float distSqDisutil = vehicleTypePreferences.getDistanceSquaredDisutil( fuelType, bodyType );
        			
        			int dummy = 0;
        			if ( ad < 0 || pt <= 0 || aTrip.getMode() <= 0 )
        				dummy = 1;
        			
        			double carAllocationPreference = 0;
					try {
						carAllocationPreference = udDisutil + purpDisutil.get(ad) + modeDisutil.get(aTrip.getMode()-1) + 
										drvPersTypeDisutil.get(pt-1) + Math.pow(aTrip.getDistance(),2)*distSqDisutil;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        							
					int vehTypeCategory = vehicleTypePreferences.getCategory(fuelType, bodyType);

					
	        		float repoCostForHh = vehicleTypePreferences.getOperatingCostDisutil(fuelType, bodyType);
	        		
	        		float autoOperatingCost = repoCostForHh;
	        		float costOfCurrentTrip = aTrip.getDistance()*autoOperatingCost;
	        		
	        		float reposCostHD = repoCostForHh*(distanceFromHome[origTaz]);
	        		float reposCostOH = repoCostForHh*(distanceToHome[destTaz]);

	        		if(hh.getIfAvHousehold() == 0 ){
	        			reposCostHD = NON_AV_REPO_COST;
	        			reposCostOH = NON_AV_REPO_COST;
	        			repoCostForHh = NON_AV_REPO_COST;
	        		}


	        		//Set repo cost to 0 for first/last trip with person
	        		if(ao == PurposeCategories.HOME.getIndex())
	        			reposCostHD= costOfCurrentTrip;

	        		if(ad == PurposeCategories.HOME.getIndex())
	        			reposCostOH= costOfCurrentTrip;

	        		
					double coeffValue = -usualDrBonus*usualDrDummy + carAllocationPreference + (-0.01*diffPnumAuto) + (-0.001*vehTypeCategory) + (-0.1*costOfCurrentTrip);

					if(logHhId == hh.getId() )
						logger.info( String.format( "i=%d, j=%d, pnum=%d, usualDrBonus=%.3f, usualDrDummy=%d, carAllocationPreference=%.3f, -0.01*diffPnumAuto=%.0f, -0.001*vehTypeCategory=%d, costOfCurrentTrip=%.3f, coeffValue=%.3f", i, j, aTrip.getPnum(), usualDrBonus, usualDrDummy, carAllocationPreference, diffPnumAuto, vehTypeCategory, costOfCurrentTrip, coeffValue ) );
					
	            	ofVarsIJ[INDEX_CarAllo ][i][j] = solver.makeNumVar( lowerBound, uppperBound, ( name = "CarAlloc_"+i+"_"+j ) );
                    objective.setCoefficient( ofVarsIJ[INDEX_CarAllo][i][j], coeffValue ); //j is added for singularity
                    ofCoeffList.add( (float) coeffValue );
            		variableNameList.add( name );
            		//ofVarsIJ[INDEX_CarAllo ][i][j].setInteger(true);
            		//F2
            		ofVarsIJ[INDEX_FirstCarTrip][i][j] = solver.makeNumVar( 0.0, 1, ( name = "FirstCarTrip_"+i+"_"+j ) );
                    objective.setCoefficient( ofVarsIJ[INDEX_FirstCarTrip][i][j],reposCostHD );
                    ofCoeffList.add( reposCostHD);
            		variableNameList.add( name );
            		//ofVarsIJ[INDEX_FirstCarTrip ][i][j].setInteger(true);
            		//F2
            		ofVarsIJ[INDEX_LastCarTrip][i][j] = solver.makeNumVar( 0.0, 1, ( name = "LastCarTrip_"+i+"_"+j ) );
                    objective.setCoefficient( ofVarsIJ[INDEX_LastCarTrip][i][j],reposCostOH );
                    ofCoeffList.add( reposCostOH);
            		variableNameList.add( name );
            		//ofVarsIJ[INDEX_LastCarTrip ][i][j].setInteger(true);

        		}


        		//F1
        		if( UNSAT_DEM == null)
        			logger.info(ad+"_"+pt);
        		ofVarsI[INDEX_UnSatis ][i] = solver.makeNumVar( 0.0, 1, ( name = "UnSat_"+i ) );
                double unsatDemCoeff = (UNSAT_DEM[ad][pt] + (tripDistance*unsatisfiedDemandDistancePenalty) + 1000);
                objective.setCoefficient( ofVarsI[INDEX_UnSatis ][i], unsatDemCoeff );
                ofCoeffList.add( (float)unsatDemCoeff );
        		variableNameList.add( name );

        		//IK variables
        		for ( int k = i+1; k < autoTrips.size(); k++){
        			
	        		// get next auto trip
        			AutoTrip nextATrip = autoTrips.get(k);
	        		int origMazNextTrip = -1;
	        		int destMazNextTrip = -1;
	        		int origTazNextTrip = -1;
	        		int destTazNextTrip = -1;

        			origMazNextTrip = nextATrip.getOrigMaz();
        			destMazNextTrip = nextATrip.getDestMaz();

        			int nextAo = nextATrip.getOrigAct();

        			origTazNextTrip = geogManager.getMazTazValue(origMazNextTrip);
        			destTazNextTrip = geogManager.getMazTazValue(destMazNextTrip);



	        		Boolean pairLi = false;
	        		Boolean samePerson = false;
	        		if(destTaz == origTazNextTrip)
	        			pairLi = true;

        			boolean subsequentTrip = false;
	        		if(aTrip.getPnum() == nextATrip.getPnum()) {
	        			samePerson = true;
	        			if ( tripIdForDriver[k] - tripIdForDriver[i] == 1 )
	        				subsequentTrip = true;
	        		}

        				

        			float distanceToNextOrig = distanceFromEndTrip[origTazNextTrip];
	        		float distanceHomeToNextOrig = distanceFromHome[origTazNextTrip];
	        		float distanceToHomeFromEndOfCurrent = distanceToHome[destTaz];


	        		float penaltyForGik = 0;
	        		if(pairLi && samePerson)
	        			penaltyForGik= intraZonalGikSamePersonPenalty;

	        		
	        		// calculate parking cost at destination (for Sik)
	        		if( MANDATORY_PURPOSE_INDICES.contains( ad ) )
	        			parkCostSik = (parkingRateMonth[destMaz]/parkingScale)/100; // parking in cents
	        		else
	        			parkCostSik = (parkingRateHr[destMaz]/100)*(nextATrip.getSchedDepart() - (aTrip.getSchedDepart()+aTrip.getSchedTime()))/60; // divided by 60 to convert minutes into hour

	        		// calculate parking cost at destination (for Gik)
	        		if( MANDATORY_PURPOSE_INDICES.contains( nextAo ) )
	        			parkCostGik = (parkingRateMonth[origMazNextTrip]/parkingScale)/100; // parking in cents
	        		else
	        			parkCostGik = (parkingRateHr[origMazNextTrip]/100)*(nextATrip.getSchedDepart() - (aTrip.getSchedDepart()+aTrip.getSchedTime()))/60;

	        		// no parking cost for going home
	        		if(ad == 0){
	        			parkCostSik = 0;

	        		}
	        		if(nextAo == 0)
	        			parkCostGik = 0;

	        		for ( int j=0; j < numAutos; j++ )     {
		        		//F2, F4

	        			float lowerBoundS = 0;
	        			float uppperBoundS = 1;

	        			if(sikjIntegerization!= null){
	        				if(sikjFixFlag[j][i][k] == 1){
		        				lowerBoundS = sikjIntegerization[j][i][k];
		        				uppperBoundS = sikjIntegerization[j][i][k];
	        				}
	        			}

	        			float lowerBoundG= 0;
	        			float uppperBoundG = 1;

	        			if(gikjIntegerization!= null){
	        				if(gikjFixFlag[j][i][k] == 1){
		        				lowerBoundG = gikjIntegerization[j][i][k];
		        				uppperBoundG = gikjIntegerization[j][i][k];
	        				}
	        			}


	        			int fuelType = hhVehFuelTypes[j];
	        			int bodyType = hhVehBodyTypes[j];
		        		float repoCostForHh = vehicleTypePreferences.getOperatingCostDisutil(fuelType, bodyType);
	        			
		        		float reposCostToHome = repoCostForHh*(distanceHomeToNextOrig+distanceToHomeFromEndOfCurrent);
		        		float reposCostToNextTripOrigSik = 0;
		        		float reposCostToNextTripOrigGik = 0;
		        		// update repositioning cost if not pair Li
		        		if(!pairLi){
		        			reposCostToNextTripOrigSik=repoCostForHh*distanceToNextOrig;
		        			reposCostToNextTripOrigGik=repoCostForHh*distanceToNextOrig;

		        		}

		        		//reposition cost to avoid switches for intra-zonal
		        		if(pairLi && ((ad == PurposeCategories.HOME.getIndex()&& nextAo != PurposeCategories.HOME.getIndex())|| nextAo == PurposeCategories.HOME.getIndex()&& ad != PurposeCategories.HOME.getIndex()) ){
		        			reposCostToNextTripOrigSik=repoCostForHh*distanceToNextOrig;
		        			reposCostToNextTripOrigGik=repoCostForHh*distanceToNextOrig;
		        		}

		        		// set repositioning cost to very high value if non-AV hh and not starting in the same taz and if the persons are not same for two trips
		        		if(hh.getIfAvHousehold() == 0 ){
		        			reposCostToNextTripOrigGik = NON_AV_REPO_COST;
		        			reposCostToHome = NON_AV_REPO_COST;
		        			if(!pairLi || !samePerson || !subsequentTrip)
		        				reposCostToNextTripOrigSik = NON_AV_REPO_COST;
		        		}

		                if(hh.getIfAvHousehold() == 0 ){
		            		reposCostToNextTripOrigGik = NON_AV_REPO_COST;
		            		reposCostToHome = NON_AV_REPO_COST;
		            		if(!pairLi || !samePerson || !subsequentTrip)
		            			reposCostToNextTripOrigSik = NON_AV_REPO_COST;

		            		// see if at home swap should be allowed
			        	    int current_trip_destination_purpose = ad;
			                int next_trip_origin_purpose = purposeMap.get( nextATrip.getOrigAct() );
			                if (current_trip_destination_purpose == 0 && next_trip_origin_purpose == 0)
		            			reposCostToNextTripOrigSik = 0.0f;
		                }

		               
	        			ofVarsIJK[INDEX_SameTripParkDi ][i][k][j] = solver.makeNumVar( lowerBoundS, uppperBoundS, ( name = "ParkDi_"+i+"_"+k+"_"+j ) );
	                    objective.setCoefficient( ofVarsIJK[INDEX_SameTripParkDi][i][k][j], reposCostToNextTripOrigSik + parkCostSik);
	                    ofCoeffList.add( (float) (reposCostToNextTripOrigSik + parkCostSik) );
	            		variableNameList.add( name );
	            		//ofVarsIJK[INDEX_SameTripParkDi ][i][k][j].setInteger(true);

	            		//F2, F4
	            		ofVarsIJK[INDEX_SameTripParkOk ][i][k][j] = solver.makeNumVar( lowerBoundG, uppperBoundG, ( name = "ParkOk_"+i+"_"+k+"_"+j ) );
	                    objective.setCoefficient( ofVarsIJK[INDEX_SameTripParkOk][i][k][j], reposCostToNextTripOrigGik + parkCostGik + penaltyForGik);
	                    ofCoeffList.add( (float) (reposCostToNextTripOrigGik + parkCostGik + penaltyForGik) );
	            		variableNameList.add( name );

	            		//ofVarsIJK[INDEX_SameTripParkOk ][i][k][j].setInteger(true);
	            		//F2, F4
	            		ofVarsIJK[INDEX_SameTripParH  ][i][k][j] = solver.makeNumVar( 0.0, 1, ( name = "ParkH_"+i+"_"+k+"_"+j ) );
	                    objective.setCoefficient( ofVarsIJK[INDEX_SameTripParH ][i][k][j], reposCostToHome );
	                    ofCoeffList.add( reposCostToHome );
	            		variableNameList.add( name );
	            		//ofVarsIJK[INDEX_SameTripParH ][i][k][j].setInteger(true);

	            		//F2
	            		ofVarsIJK[INDEX_CarLink ][i][k][j] = solver.makeNumVar( 0.0, 1, ( name = "CarLink_"+i+"_"+k+"_"+j ) );
	                    //objective.setCoefficient( ofVarsIJK[INDEX_CarLink ][i][k][j], 0.0);
	                    //ofCoeffList.add( 0.0f );
	            		float carLinkCost = NON_AV_REPO_COST;
	            		if(hh.getIfAvHousehold() == 0 ){
	            			if(pairLi && samePerson) {
	            				carLinkCost = 0.0f;
	            			}
	            			else {
	    	                    int current_trip_destination_purpose = ad;
	    	                    int next_trip_origin_purpose = purposeMap.get( nextATrip.getOrigAct() );
	            				if (current_trip_destination_purpose == 0 && next_trip_origin_purpose == 0)
		            				carLinkCost = 0.0f;
	            			}
	            		}
	            		else {
	            			carLinkCost = 0.0f;
	            		}
	            		
			    		objective.setCoefficient( ofVarsIJK[INDEX_CarLink ][i][k][j], (-0.2f*(samePerson ? 1 : 0) + carLinkCost));
			    		ofCoeffList.add( (-0.2f*(samePerson ? 1 : 0)) + carLinkCost );
	            		
	            		
//	                    objective.setCoefficient( ofVarsIJK[INDEX_CarLink ][i][k][j], (-0.2f*(samePerson ? 1 : 0)) );
//	                    ofCoeffList.add( (-0.2f*(samePerson ? 1 : 0)) );
	            		variableNameList.add( name );
	        		}
        		}

        	}


//        	if(autoTrips.size()>0){
	        	for ( int m=1; m < persons.length; m++ ) {

	        		for ( int j=0; j < NUM_P_VARIABLE_INDICES; j++ )
	        			ofVarsP[j][m] = new MPVariable[trips.size()+1];

		        	for ( int q=0; q < trips.size();q++ ) {

		        		Trip trip = trips.get(q);
		        		int i = trip.getIndivTripId();

		        		int ao = purposeMap.get( trip.getOrigAct() );
		        		int ad = purposeMap.get( trip.getDestAct() );
		        		int pt = persons[trip.getPnum()].getPersonType();
		        		if(trip.getPnum() != m)
		        			continue;
		        		// Scheduling variables
		        		//F5
		        		float upperBound = trip.getSchedDepart() + NEGATIVE_DEPART_TIME_ADJUSTMENT;
		        		ofVarsP[INDEX_DepEarly ][m][i] = solver.makeNumVar( 0.0d, upperBound, ( name = "DepEarly_"+m+"_"+i ) );
		                objective.setCoefficient( ofVarsP[INDEX_DepEarly ][m][i], D_EARLY_FOR[ad][pt] + D_EARLY_FROM[ao][pt]);
		                ofCoeffList.add( D_EARLY_FOR[ad][pt] + D_EARLY_FROM[ao][pt]);
		        		variableNameList.add( name );

		        		//F5
		        		upperBound = endOfSimulationMinute-trip.getSchedArrive();
		        		ofVarsP[INDEX_DepLate ][m][i] = solver.makeNumVar( 0.0d, upperBound, ( name = "DepLate_"+m+"_"+i ) );
		                objective.setCoefficient( ofVarsP[INDEX_DepLate ][m][i], D_LATE_FOR[ad][pt] + D_LATE_FROM[ao][pt]);
		                ofCoeffList.add( D_LATE_FOR[ad][pt] + D_LATE_FROM[ao][pt]);
		        		variableNameList.add( name );
		        	}


		    	}
        	}
//    	}
    	catch ( Exception e ) {
    		logger.error( "exception caught for hhid=" + hh.getId(), e );
    		System.exit(-1);
    	}

    }


    private void setupContraints( Household hh, MPSolver solver, int endOfSimulationMinute ) {

    	String name = null;

    	Person[] persons = hh.getPersons();
    	List<Trip> trips = hh.getTrips();

    	int maxTripNumPerson = 0;

    	for(int p = 1; p < persons.length; p++){
    		int numT = persons[p].getTripIds().size();
    		if(numT>maxTripNumPerson)
    			maxTripNumPerson = numT;
    	}
    	List<AutoTrip> autoTrips = hh.getAutoTrips();

    	int numAutos = hh.getNumAutos();
    	int homeMaz = hh.getHomeMaz();

    	int homeTaz = geogManager.getMazTazValue(homeMaz);
    	int numAutoTrips = hh.getNumAutoTrips();

    	int numAutoTripPairs = (numAutoTrips)*(numAutoTrips-1)/2;

    	float[] distanceFromHome = sharedDistanceObject.getOffpeakDistanceFromTaz(homeTaz);
    	float[] distanceToHome = sharedDistanceObject.getOffpeakDistanceToTaz(homeTaz);


    	MPConstraint[][] constraintsLP2 = new MPConstraint[NUM_CONSTRAINT_TRIP_INDICES][autoTrips.size()];
    	double[][] rhsLP2 = new double[NUM_CONSTRAINT_TRIP_INDICES][autoTrips.size()];

    	MPConstraint[][][][] constraintsLP3= new MPConstraint[NUM_CONSTRAINT_REPO_INDICES][autoTrips.size()][autoTrips.size()][numAutos];
    	double[][][][] rhsLP3= new double[NUM_CONSTRAINT_REPO_INDICES][autoTrips.size()][autoTrips.size()][numAutos];

    	MPConstraint[][][] constraintsLP4 = new MPConstraint[NUM_CONSTRAINT_SCHEDULE_INDICES][persons.length+1][];
    	double[][][] rhsLP4 = new double[NUM_CONSTRAINT_SCHEDULE_INDICES][persons.length+1][];

    	MPConstraint[][][] constraintsLP5 = new MPConstraint[NUM_CONSTRAINT_IJ_INDICES][autoTrips.size()][numAutos];;
    	double[][][] rhsLP5= new double[NUM_CONSTRAINT_IJ_INDICES][autoTrips.size()][numAutos];

    	MPConstraint[][] constraintsLP6 = new MPConstraint[NUM_CONSTRAINT_IJ_INDICES][numAutos];;
    	double[][] rhsLP6= new double[NUM_CONSTRAINT_IJ_INDICES][numAutos];


    	int autoTripPairNum = 0;

    	for ( int i=0; i < autoTrips.size();i++ ) {

    		AutoTrip aTrip = autoTrips.get(i);

    		rhsLP2[INDEX_1_1][i] = 1;
    		constraintsLP2[INDEX_1_1][i] = solver.makeConstraint(rhsLP2[INDEX_1_1][i], rhsLP2[INDEX_1_1][i], (name = "Const_1_1"+i));
    		constraintNameList.add( name );
    		for ( int j=0; j < numAutos; j++ )     {
    			constraintsLP2[INDEX_1_1][i].setCoefficient( ofVarsIJ[INDEX_CarAllo][i][j], 1.0);
    		}
    		constraintsLP2[INDEX_1_1][i].setCoefficient( ofVarsI[INDEX_UnSatis][i], 1.0);



    		float autoTripPlanDepTime = aTrip.getSchedDepart() + NEGATIVE_DEPART_TIME_ADJUSTMENT;
    		float autoTravelTime = aTrip.getSchedTime();

    		int origMaz = aTrip.getOrigMaz();
    		int destMaz = aTrip.getDestMaz();

    		int origTaz = geogManager.getMazTazValue(origMaz);
    		int destTaz = geogManager.getMazTazValue(destMaz);

    		float tripDistance = aTrip.getDistance();

    		float[] distanceFromEndTrip = sharedDistanceObject.getOffpeakDistanceFromTaz(destTaz);

    		int ao = purposeMap.get( aTrip.getOrigAct() );
    		int ad = purposeMap.get( aTrip.getDestAct() );

    		int pt = persons[aTrip.getPnum()].getPersonType();
    		int hhTripNum = aTrip.getHhTripId();

    		int pnum = trips.get(hhTripNum).getPnum();
    		int personTripId = trips.get(hhTripNum).getIndivTripId();
    		int emptyRepoConstraint = 1-hh.getIfAvHousehold();
    		for ( int j=0; j < numAutos; j++ )     {
        		rhsLP5[INDEX_2_1][i][j] = 0;
        		constraintsLP5[INDEX_2_1][i][j] = solver.makeConstraint(rhsLP5[INDEX_2_1][i][j], rhsLP5[INDEX_2_1][i][j], (name = "Const_2_1"+"_"+i+"_"+j));
        		constraintNameList.add( name );

        		rhsLP5[INDEX_2_2][i][j] = 0;
        		constraintsLP5[INDEX_2_2][i][j] = solver.makeConstraint(rhsLP5[INDEX_2_2][i][j], rhsLP5[INDEX_2_2][i][j], (name = "Const_2_2"+"_"+i+"_"+j));
        		constraintNameList.add( name );



        		constraintsLP5[INDEX_2_1][i][j].setCoefficient(ofVarsIJ[INDEX_CarAllo][i][j], 1);
        		constraintsLP5[INDEX_2_1][i][j].setCoefficient(ofVarsIJ[INDEX_LastCarTrip][i][j], -1);

        		constraintsLP5[INDEX_2_2][i][j].setCoefficient(ofVarsIJ[INDEX_CarAllo][i][j], 1);
        		constraintsLP5[INDEX_2_2][i][j].setCoefficient(ofVarsIJ[INDEX_FirstCarTrip][i][j], -1);

        		/*
        		int origHome = ao == PurposeCategories.HOME.getIndex() ? 1 : 0;
    			int destHome = ad == PurposeCategories.HOME.getIndex() ? 1 : 0;


    			rhsLP5[INDEX_2_3][i][j]= 0;
    			constraintsLP5[INDEX_2_3][i][j] = solver.makeConstraint(rhsLP5[INDEX_2_3][i][j], rhsLP5[INDEX_2_3][i][j], (name = "Const_2_3"+"_" + i+  "_"+j));
    			constraintsLP5[INDEX_2_3][i][j].setCoefficient(ofVarsIJ[INDEX_LastCarTrip][i][j], emptyRepoConstraint*(1-destHome));
    			constraintNameList.add( name );

    			rhsLP5[INDEX_2_4][i][j]= 0;
    			constraintsLP5[INDEX_2_4][i][j] = solver.makeConstraint(rhsLP5[INDEX_2_4][i][j], rhsLP5[INDEX_2_4][i][j], (name = "Const_2_4"+"_" + i+  "_"+j));
    			constraintsLP5[INDEX_2_4][i][j].setCoefficient(ofVarsIJ[INDEX_FirstCarTrip][i][j], emptyRepoConstraint*(1-origHome));
    			constraintNameList.add( name );

    			*/

	    		for ( int k = i+1; k < autoTrips.size(); k++){
	    			AutoTrip nextATrip = autoTrips.get(k);
	        		int origMazNextTrip = -1;
	        		int destMazNextTrip = -1;
	        		int origTazNextTrip = -1;
	        		int destTazNextTrip = -1;
	        		int hhNextTripNum = nextATrip.getHhTripId();
	        		int pnumNext = trips.get(hhNextTripNum).getPnum();
	        		int personNextTripId = trips.get(hhNextTripNum).getIndivTripId();

	    			origMazNextTrip = nextATrip.getOrigMaz();
	    			destMazNextTrip = nextATrip.getDestMaz();

	    			int nextAd = nextATrip.getDestAct();

	    			origTazNextTrip = geogManager.getMazTazValue(origMazNextTrip);
	    			destTazNextTrip = geogManager.getMazTazValue(destMazNextTrip);

	        		Boolean pairLi = false;
	        		if(destTaz == origTazNextTrip)
	        			pairLi = true;


	        		float distanceToNextOrig = distanceFromEndTrip[origTazNextTrip];
	        		float distanceHomeToNextOrig = distanceFromHome[origTazNextTrip];
	        		float distanceToHomeFromEndOfCurrent = distanceToHome[destTaz];


	    			constraintsLP5[INDEX_2_1][i][j].setCoefficient(ofVarsIJK[INDEX_CarLink][i][k][j], -1);



	    			// IK constraints
	    			rhsLP3[INDEX_3_1][i][k][j]= 0;
	    			constraintsLP3[INDEX_3_1][i][k][j] = solver.makeConstraint(rhsLP3[INDEX_3_1][i][k][j], rhsLP3[INDEX_3_1][i][k][j], (name = "Const_3_1"+"_" + i+  "_"+k+"_"+j));
	    			constraintsLP3[INDEX_3_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_CarLink][i][k][j], 1);
	    			constraintsLP3[INDEX_3_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkDi][i][k][j], -1);
	    			constraintsLP3[INDEX_3_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkOk][i][k][j], -1);
	    			constraintsLP3[INDEX_3_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParH][i][k][j], -1);
	    			constraintNameList.add( name );

	    			float nextTripPlanDep = nextATrip.getSchedDepart() + NEGATIVE_DEPART_TIME_ADJUSTMENT;
	    			float nextTripTravelTime = nextATrip.getSchedTime();

	    			rhsLP3[INDEX_4_1][i][k][j]= M_BIG_CONSTANT  - autoTripPlanDepTime +nextTripPlanDep - autoTravelTime - distanceToNextOrig*minutesPerMile;
	    			constraintsLP3[INDEX_4_1][i][k][j] = solver.makeConstraint(-MPSolver.infinity(), rhsLP3[INDEX_4_1][i][k][j], (name = "Const_4_1"+"_" + i+  "_"+k+"_"+j));
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkDi][i][k][j], M_BIG_CONSTANT);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkOk][i][k][j], M_BIG_CONSTANT);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkOk][i][k][j], M_BIG_CONSTANT);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsP[INDEX_DepLate][pnum][personTripId], 1);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsP[INDEX_DepLate][pnumNext][personNextTripId], -1);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsP[INDEX_DepEarly][pnum][personTripId], -1);
	    			constraintsLP3[INDEX_4_1][i][k][j].setCoefficient(ofVarsP[INDEX_DepEarly][pnumNext][personNextTripId], 1);
	    			constraintNameList.add( name );

	    			rhsLP3[INDEX_4_2][i][k][j]= M_BIG_CONSTANT - autoTripPlanDepTime+nextTripPlanDep - autoTravelTime - (distanceToNextOrig+distanceToHomeFromEndOfCurrent + distanceHomeToNextOrig)*minutesPerMile;
	    			constraintsLP3[INDEX_4_2][i][k][j] = solver.makeConstraint(-MPSolver.infinity(), rhsLP3[INDEX_4_2][i][k][j], (name = "Const_4_2"+"_" + i+  "_"+k+"_"+j));
	    			constraintsLP3[INDEX_4_2][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParH][i][k][j], M_BIG_CONSTANT);
	    			constraintsLP3[INDEX_4_2][i][k][j].setCoefficient(ofVarsP[INDEX_DepLate][pnum][personTripId], 1);
	    			constraintsLP3[INDEX_4_2][i][k][j].setCoefficient(ofVarsP[INDEX_DepLate][pnumNext][personNextTripId], -1);
	    			constraintsLP3[INDEX_4_2][i][k][j].setCoefficient(ofVarsP[INDEX_DepEarly][pnum][personTripId], -1);
	    			constraintsLP3[INDEX_4_2][i][k][j].setCoefficient(ofVarsP[INDEX_DepEarly][pnumNext][personNextTripId], 1);
	    			constraintNameList.add( name );

	    			/*
	    			rhsLP3[INDEX_4_3][i][k][j]= 0;
	    			constraintsLP3[INDEX_4_3][i][k][j] = solver.makeConstraint(rhsLP3[INDEX_4_3][i][k][j], rhsLP3[INDEX_4_3][i][k][j], (name = "Const_4_3"+"_" + i+  "_"+k+"_"+j));
	    			constraintsLP3[INDEX_4_3][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParH][i][k][j], emptyRepoConstraint);
	    			constraintNameList.add( name );


	    			rhsLP3[INDEX_4_4][i][k][j]= 0;
	    			constraintsLP3[INDEX_4_4][i][k][j] = solver.makeConstraint(rhsLP3[INDEX_4_4][i][k][j], rhsLP3[INDEX_4_4][i][k][j], (name = "Const_4_4"+"_" + i+  "_"+k+"_"+j));
	    			constraintsLP3[INDEX_4_4][i][k][j].setCoefficient(ofVarsIJK[INDEX_SameTripParkOk][i][k][j], emptyRepoConstraint);
	    			constraintNameList.add( name );
	    			*/


	    			autoTripPairNum++;
	    		}

	    		for ( int k=0; k < i; k++){
	    			constraintsLP5[INDEX_2_2][i][j].setCoefficient(ofVarsIJK[INDEX_CarLink][k][i][j], -1);
	    		}
	    	}



    	}

    	// J related constraints
    	for ( int j=0; j < numAutos; j++ )     {
    		rhsLP6[INDEX_5_1][j] = 1;
    		constraintsLP6[INDEX_5_1][j] = solver.makeConstraint(rhsLP6[INDEX_5_1][j] , rhsLP6[INDEX_5_1][j], (name = "Const_5_1"+"_"+j));

    		rhsLP6[INDEX_5_2][j] = 1;
    		constraintsLP6[INDEX_5_2][j] = solver.makeConstraint(rhsLP6[INDEX_5_1][j] , rhsLP6[INDEX_5_2][j], (name = "Const_5_2"+"_"+j));

    		for ( int i=0; i < autoTrips.size();i++ ) {
    			constraintsLP6[INDEX_5_1][j].setCoefficient(ofVarsIJ[INDEX_FirstCarTrip][i][j], 1);
    			constraintsLP6[INDEX_5_2][j].setCoefficient(ofVarsIJ[INDEX_LastCarTrip][i][j], 1);
    		}
    		constraintsLP6[INDEX_5_1][j].setCoefficient(ofVarsJ[INDEX_UnusedCar][j], 1);
    		constraintsLP6[INDEX_5_2][j].setCoefficient(ofVarsJ[INDEX_UnusedCar][j], 1);

    	}



    	//if(autoTrips.size()>0){
    	for ( int m=1; m < persons.length; m++ ) {


    		List<Integer> tripIds = persons[m].getTripIds();


    		for ( int j=0; j < NUM_CONSTRAINT_SCHEDULE_INDICES; j++ ){
    			rhsLP4[j][m] = new double[tripIds.size()+1];;
    			constraintsLP4[j][m] = new MPConstraint[tripIds.size()+1];

    		}

    		if(tripIds.size()>0){
	    		int autoTripId = trips.get( tripIds.get( 0) ).getHhAutoTripId();
	    		Trip trip = trips.get( tripIds.get( 0) );

	    		int driverTripNum = -1;
        		int driverPnum = m;
        		if(autoTripId >=0){
        			AutoTrip aTrip = autoTrips.get(autoTripId);
        			driverTripNum = aTrip.getHhTripId();
        			driverPnum = aTrip.getPnum();
        		}



	    		if(driverPnum != m ){
	    			if(driverTripNum == -1)
		    			driverTripNum = tripIds.get( 0);

		    		Trip linkedTrip = trips.get(  driverTripNum );

		    		int pq = trip.getIndivTripId();
		    		int ipq = linkedTrip.getIndivTripId();
		    		// 1st trip
		    		rhsLP4[INDEX_6_0][m][0] = linkedTrip.getSchedDepart()-trip.getSchedDepart();
		    		constraintsLP4[INDEX_6_0][m][0] = solver.makeConstraint(rhsLP4[INDEX_6_0][m][0], rhsLP4[INDEX_6_0][m][0], (name = "Const_6_0"+"_"+m+"_"+0));
		    		constraintsLP4[INDEX_6_0][m][0].setCoefficient(ofVarsP[INDEX_DepLate ][m][pq], 1);
		    		constraintsLP4[INDEX_6_0][m][0].setCoefficient(ofVarsP[INDEX_DepEarly][m][pq], -1);
		    		constraintsLP4[INDEX_6_0][m][0].setCoefficient(ofVarsP[INDEX_DepLate ][driverPnum][ipq], -1);
		    		constraintsLP4[INDEX_6_0][m][0].setCoefficient(ofVarsP[INDEX_DepEarly][driverPnum][ipq], 1);

		    		constraintNameList.add( name );
	    		}

	    		// start from 2nd trip of person
	        	for ( int q=1; q < tripIds.size();q++ ) {
	        		trip = trips.get( tripIds.get( q) );
	        		float schedDep = trip.getSchedDepart();
	        		float prevDep = trips.get( tripIds.get( q-1 ) ).getSchedDepart();
	        		float prevTravelTime = trips.get( tripIds.get( q-1 ) ).getSchedTime();

	    			rhsLP4[INDEX_5_0][m][q] = prevDep-schedDep+minimumActivityDuration+prevTravelTime;
	        		constraintsLP4[INDEX_5_0][m][q] = solver.makeConstraint(rhsLP4[INDEX_5_0][m][q], MPSolver.infinity(), (name = "Const_5_0"+"_"+m+"_"+q));
	        		constraintsLP4[INDEX_5_0][m][q].setCoefficient(ofVarsP[INDEX_DepLate ][m][trip.getIndivTripId()], 1);
	        		constraintsLP4[INDEX_5_0][m][q].setCoefficient(ofVarsP[INDEX_DepEarly][m][trip.getIndivTripId()], -1);
	        		constraintsLP4[INDEX_5_0][m][q].setCoefficient(ofVarsP[INDEX_DepLate][m][trips.get( tripIds.get( q-1 ) ).getIndivTripId()], -1);
	        		constraintsLP4[INDEX_5_0][m][q].setCoefficient(ofVarsP[INDEX_DepEarly][m][trips.get( tripIds.get( q-1 ) ).getIndivTripId()], 1);
	        		constraintNameList.add( name );

	        		autoTripId = trips.get( tripIds.get( q) ).getHhAutoTripId();
	        		driverTripNum = -1;
	        		driverPnum = m;
	        		if(autoTripId >=0){
	        			AutoTrip aTrip = autoTrips.get(autoTripId);
	        			driverTripNum = aTrip.getHhTripId();
	        			driverPnum = aTrip.getPnum();
	        		}

		    		if(driverTripNum == -1)
		    			driverTripNum = tripIds.get( q);



		    		if(driverPnum != m ){
		    			Trip linkedTrip = trips.get(  driverTripNum );

			    		int pq = trip.getIndivTripId();
			    		int ipq = linkedTrip.getIndivTripId();
		        		rhsLP4[INDEX_6_0][m][q] = linkedTrip.getSchedDepart()-trip.getSchedDepart();
		        		constraintsLP4[INDEX_6_0][m][q] = solver.makeConstraint(rhsLP4[INDEX_6_0][m][q], rhsLP4[INDEX_6_0][m][q], (name = "Const_6_0"+"_"+m+"_"+q));
		        		constraintsLP4[INDEX_6_0][m][q].setCoefficient(ofVarsP[INDEX_DepLate ][m][pq], 1);
		        		constraintsLP4[INDEX_6_0][m][q].setCoefficient(ofVarsP[INDEX_DepEarly][m][pq], -1);
		        		constraintsLP4[INDEX_6_0][m][q].setCoefficient(ofVarsP[INDEX_DepLate ][driverPnum][ipq], -1);
		        		constraintsLP4[INDEX_6_0][m][q].setCoefficient(ofVarsP[INDEX_DepEarly][driverPnum][ipq], 1);
		        		constraintNameList.add( name );
		    		}
	        	}

//    		}
    		}
    	}





    }


    public boolean solveLp( MPSolver solver, int integerizingIterationNum ) {

        //solver.setTimeLimit( CPU_TIME_LIMIT );
        MPSolver.ResultStatus resultStatus = solver.solve();
        //logger.info( "Objective Function = " + solver.objective().value() );

//    	String lpOutput = solver.exportModelAsLpFormat(true);
//    	String mpsOutput = solver.exportModelAsMpsFormat(true, true);
//    	logger.info("start of lp format");
//    	logger.info(lpOutput);
//    	logger.info("start of mps format");
//    	logger.info(mpsOutput);
//    	logger.info("end lp logging");
    	
        // Check that the problem has an optimal solution.
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
          //logger.error("The solver did not find an optimal solution!");
          return false;
        }
        if(reportFinalSolution)
        	reportFinalLpSolution(solver,integerizingIterationNum);

        return true;

    }


    public void reportInitialLp( MPSolver solver,int iterNumForIntegerizing ) {

    	logger.info( "LP problem has " + solver.numVariables() + " variables and " + solver.numConstraints() +" constraints." );


    	double[][] coeffsArray = new double[constraintNameList.size()][variableNameList.size()];

    	int i = 0;
    	for ( String cname : constraintNameList ) {

    		MPConstraint constraint = solver.lookupConstraintOrNull( cname );

    		int j = 0;
        	for ( String vname : variableNameList ) {

        		MPVariable variable = solver.lookupVariableOrNull( vname );
        		coeffsArray[i][j] = constraint.getCoefficient( variable );
        		j++;

        	}

        	i++;

    	}

    	Util.writeArrayDataToCsv( "./coeffsArray"+String.valueOf(iterNumForIntegerizing)+".csv", "constraint", constraintNameList, variableNameList, coeffsArray );



    	double[][] boundsArray = new double[constraintNameList.size()][2];

    	List<String> boundsNames = new ArrayList<String>();
    	boundsNames.add( "lower" );
    	boundsNames.add( "upper" );

    	i = 0;
    	for ( String cname : constraintNameList ) {

    		MPConstraint constraint = solver.lookupConstraintOrNull( cname );
       		boundsArray[i][0] = constraint.lb();
       		boundsArray[i][1] = constraint.ub();

       		i++;
    	}

    	Util.writeArrayDataToCsv( "./boundsArray.csv", "constraint", constraintNameList, boundsNames, boundsArray );

    }



    public void reportFinalLpSolution( MPSolver solver, int integerizingIterationNum ) {

    	double[][] resultsArray = new double[2][variableNameList.size()];

    	List<String> rowNames = new ArrayList<String>();
    	rowNames.add( "ofCoeffs" );
    	rowNames.add( "results" );

    	int i = 0;
    	for ( String vname : variableNameList ) {

    		MPVariable variable = solver.lookupVariableOrNull( vname );
    		resultsArray[0][i] = ofCoeffList.get( i );
    		resultsArray[1][i] = variable.solutionValue();

       		i++;
    	}

    	Util.writeArrayDataToCsv( "./resultsArray"+String.valueOf(integerizingIterationNum)+".csv", "solution", rowNames, variableNameList, resultsArray );


    }



    private void setParameterArrayValues( ParameterReader parameterInstance ) {

        UNSAT_DEM = parameterInstance.getOfParameters( ParameterReader.UNSAT_DEM );
        D_EARLY_FOR = parameterInstance.getOfParameters( ParameterReader.D_EARLY_FOR );
        D_LATE_FOR = parameterInstance.getOfParameters( ParameterReader.D_LATE_FOR );
        D_EARLY_FROM = parameterInstance.getOfParameters( ParameterReader.D_EARLY_FROM);
        D_LATE_FROM = parameterInstance.getOfParameters( ParameterReader.D_LATE_FROM);


    }


	private void createPurposeTranslationMap() {
		purposeMap = new HashMap<Integer, Integer>();
		purposeMap.put( HOME_INDEX, HOME );
		purposeMap.put( WORK_INDEX, WORK );
		purposeMap.put( UNIV_INDEX, UNIV );
		purposeMap.put( SCHOOL_INDEX, SCHOOL );
		purposeMap.put( ESCORT, ESCORT );
		purposeMap.put( ESCORT_INDEX, ESCORT );
		purposeMap.put( PURE_ESCORT_INDEX, ESCORT );
		purposeMap.put( RIDE_SHARING_INDEX, ESCORT );
		purposeMap.put( SHOP_INDEX, SHOP );
		purposeMap.put( MAINT_INDEX, MAINT );
		purposeMap.put( MAINT_HH_INDEX, MAINT );
		purposeMap.put( MAINT_PERS_INDEX, MAINT );
		purposeMap.put( EAT_OUT_INDEX, EAT_OUT );
		purposeMap.put( EAT_BRKFST_INDEX, EAT_OUT );
		purposeMap.put( EAT_LUNCH_INDEX, EAT_OUT );
		purposeMap.put( EAT_DINNER_INDEX, EAT_OUT );
		purposeMap.put( VISITING_INDEX, VISITING );
		purposeMap.put( DISCR_INDEX, DISCR );
		purposeMap.put( AT_WORK_INDEX, AT_WORK );
		purposeMap.put( AT_WORK_BUS_INDEX, WORK );
		purposeMap.put( AT_WORK_LUN_INDEX, EAT_OUT );
		purposeMap.put( AT_WORK_OTH_INDEX, DISCR );
		purposeMap.put( BUSINESS_INDEX, WORK );
		purposeMap.put( BUSINESS_ASU_INDEX, UNIV );
	}


	private void writeResultSheet( Household hh, MPSolver solver ) {


    	String oldName = "C:/Users/Jim/Documents/projects/cmap/ScheduleAdjustment_Output.xls";
    	String newName = "C:/Users/Jim/Documents/projects/cmap/ScheduleAdjustment_ResultNew.xls";

    	WorksheetWriter writer = WorksheetWriter.getInstance( newName, oldName );

    	List<Trip> trips = hh.getTrips();
    	Person[] persons = hh.getPersons();

    	int[] trip0Rows = new int[]{ -1, 3, 20, 3 };
    	int[] colOffsets = new int[]{ -1, 0, 0, 15 };

    	int[][] tripRows = new int[persons.length][];
    	tripRows[1] = new int[]{ 4, 6, 11 };
    	tripRows[2] = new int[]{ 22, 24, 25, 26, 27, 29 };
    	tripRows[3] = new int[]{ 4, 8, 9, 10, 12 };

    	// write scheduled and actual depart times
    	for ( int m=1; m < persons.length; m++ ) {
        	List<Integer> tripIds = persons[m].getTripIds();

        	writer.writeValue( trip0Rows[m], colOffsets[m]+12, Util.getHourMinuteStringFromMinutes( tripIds.size() > 0 ? trips.get( tripIds.get(0) ).getSchedDepart() : HhCarAllocator.MIN_SIMULATION_TIME ) );
        	writer.writeValue( trip0Rows[m], colOffsets[m]+13, Util.getHourMinuteStringFromMinutes( (int)( solver.lookupVariableOrNull( "Dur_"+m+"_0" ).solutionValue() + 0.5 ) ) );

        	for ( int i=0; i < tripIds.size(); i++ ) {

        		writer.writeValue( tripRows[m][i], colOffsets[m]+4, Util.getHourMinuteStringFromMinutes( trips.get( tripIds.get(i) ).getSchedDepart() + 180 ) );
            	double dep = solver.lookupVariableOrNull( "Dep_"+m+"_"+(i+1) ).solutionValue();
            	writer.writeValue( tripRows[m][i], colOffsets[m]+5, Util.getHourMinuteStringFromMinutes( (int)( dep + 180 + 0.5 ) ) );

            	writer.writeValue( tripRows[m][i], colOffsets[m]+7, Util.getHourMinuteStringFromMinutes( trips.get( tripIds.get(i) ).getSchedArrive() + 180 ) );
            	double arr = solver.lookupVariableOrNull( "Arr_"+m+"_"+(i+1) ).solutionValue();
            	writer.writeValue( tripRows[m][i], colOffsets[m]+8, Util.getHourMinuteStringFromMinutes( (int)( arr + 180 + 0.5 ) ) );

            	writer.writeValue( tripRows[m][i], colOffsets[m]+9, Util.getHourMinuteStringFromMinutes( (int)( trips.get( tripIds.get(i) ).getSimulatedTime() + 0.5 ) ) );
            	writer.writeValue( tripRows[m][i], colOffsets[m]+10, Util.getHourMinuteStringFromMinutes( (int)( arr - dep + 0.5 ) ) );

            	writer.writeValue( tripRows[m][i], colOffsets[m]+12, Util.getHourMinuteStringFromMinutes( (int)( (i < tripIds.size()-1 ? trips.get( tripIds.get(i+1) ).getSchedDepart() : 1440) - arr + 0.5 ) ) );
            	writer.writeValue( tripRows[m][i], colOffsets[m]+13, Util.getHourMinuteStringFromMinutes( (int)( solver.lookupVariableOrNull( "Dur_"+m+"_"+(i+1) ).solutionValue() + 0.5 ) ) );

        	}
    	}

    	writer.closeWorkbook();

    }


    public double[][][] getDepartArriveResults( Household hh, MPSolver solver ) {

    	// declare an array to hold person-trip depart times in the 0 index and person-trip arrive times in the 1 index.
    	double[][][] departArriveResults = new double[DEP_ARR_INDICES][][];

    	Person[] persons = hh.getPersons();
    	List<Trip> trips = hh.getTrips();


		departArriveResults[DEP_EARLY] = new double[persons.length][];
		departArriveResults[DEP_EARLY][0] = new double[1];
		for ( int m=1; m < persons.length; m++ )
    		departArriveResults[DEP_EARLY][m] = new double[persons[m].getTripIds().size()+1];

		departArriveResults[DEP_LATE] = new double[persons.length][];
		departArriveResults[DEP_LATE][0] = new double[1];
		for ( int m=1; m < persons.length; m++ )
    		departArriveResults[DEP_LATE][m] = new double[persons[m].getTripIds().size()+1];


    	for ( int m=1; m < persons.length; m++ ) {
    		List<Integer> tripIds = persons[m].getTripIds();
        	for ( int k=0; k < tripIds.size(); k++ ) {

        		Trip trip = trips.get( tripIds.get( k ) );
        		int i = trip.getIndivTripId();
        		departArriveResults[DEP_EARLY][m][i] = solver.lookupVariableOrNull( "DepEarly_"+m+"_"+i ).solutionValue();
        		departArriveResults[DEP_LATE][m][i] = solver.lookupVariableOrNull( "DepLate_"+m+"_"+i ).solutionValue();
            }
    	}

    	return departArriveResults;

    }

public double[] getUnsatisfiedRemandResults( Household hh, MPSolver solver ) {

    	List<AutoTrip> aTrips = hh.getAutoTrips();
    	double[] carAllocationResults = new double[aTrips.size()];

    	for ( int i=0; i < aTrips.size(); i++ ) {
   			carAllocationResults[i] = solver.lookupVariableOrNull(  "UnSat_"+i).solutionValue();
    	}

    	return carAllocationResults;

    }
 public double[][][] getCarAllocationResults( Household hh, MPSolver solver ) {

    	double[][][] carAllocationResults = new double[NUM_IJ_VARIABLE_INDICES][][];

    	List<AutoTrip> aTrips = hh.getAutoTrips();
    	int numAuto = hh.getNumAutos();

    	carAllocationResults[INDEX_CarAllo] = new double[aTrips.size()][numAuto];
    	//carAllocationResults[INDEX_CarAllo][0] = new double[1];


    	carAllocationResults[INDEX_FirstCarTrip] = new double[aTrips.size()][numAuto];
    	//carAllocationResults[INDEX_FirstCarTrip][0] = new double[1];

    	carAllocationResults[INDEX_LastCarTrip] = new double[aTrips.size()][numAuto];
    	//carAllocationResults[INDEX_LastCarTrip][0] = new double[1];

    	for ( int i=0; i < aTrips.size(); i++ ) {
        	for ( int j=0; j < numAuto; j++ ) {
        		carAllocationResults[INDEX_CarAllo][i][j] = solver.lookupVariableOrNull(  "CarAlloc_"+i+"_"+j ).solutionValue();
        		carAllocationResults[INDEX_FirstCarTrip][i][j] = solver.lookupVariableOrNull( "FirstCarTrip_"+i+"_"+j ).solutionValue();
        		carAllocationResults[INDEX_LastCarTrip][i][j] = solver.lookupVariableOrNull( "LastCarTrip_"+i+"_"+j ).solutionValue();
            }
    	}

    	return carAllocationResults;

    }
 public double[][][][] getCarLinkingResults( Household hh, MPSolver solver ) {

 	// declare an array to hold person-trip depart times in the 0 index and person-trip arrive times in the 1 index.
 	double[][][][] carLinkingResults = new double[NUM_IJK_VARIABLE_INDICES][][][];

 	Person[] persons = hh.getPersons();
 	List<AutoTrip> aTrips = hh.getAutoTrips();
 	int numAuto = hh.getNumAutos();

 	carLinkingResults[INDEX_SameTripParkDi] = new double[numAuto][aTrips.size()][aTrips.size()];
 	//carLinkingResults[INDEX_SameTripParkDi][0] = new double[1][1];

 	carLinkingResults[INDEX_SameTripParkOk] = new double[numAuto][aTrips.size()][aTrips.size()];
 	//carLinkingResults[INDEX_SameTripParkOk][0] = new double[1][1];

 	carLinkingResults[INDEX_SameTripParH] = new double[numAuto][aTrips.size()][aTrips.size()];
 	//carLinkingResults[INDEX_SameTripParH][0] = new double[1][1];

 	carLinkingResults[INDEX_CarLink] = new double[numAuto][aTrips.size()][aTrips.size()];
 	//carLinkingResults[INDEX_CarLink][0] = new double[1][1];


 	for ( int i=0; i < aTrips.size(); i++ ) {
     	for ( int j=0; j < numAuto; j++ ) {
     		for(int k = i+1; k < aTrips.size(); k++){
     			carLinkingResults[INDEX_SameTripParkDi][j][i][k] = solver.lookupVariableOrNull(  "ParkDi_"+i+"_"+k+"_"+j ).solutionValue();
     			carLinkingResults[INDEX_SameTripParkOk][j][i][k] = solver.lookupVariableOrNull( "ParkOk_"+i+"_"+k+"_"+j ).solutionValue();
     			carLinkingResults[INDEX_SameTripParH][j][i][k] = solver.lookupVariableOrNull(  "ParkH_"+i+"_"+k+"_"+j ).solutionValue();
     			carLinkingResults[INDEX_CarLink][j][i][k] = solver.lookupVariableOrNull("CarLink_"+i+"_"+k+"_"+j ).solutionValue();
     		}
     	}
 	}

 	return carLinkingResults;

 }
}
