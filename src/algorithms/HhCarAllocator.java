package algorithms;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.ortools.linearsolver.MPSolver;

import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import objects.Household;
import objects.HouseholdCarAllocation;;


public class HhCarAllocator implements HhCarAllocatorIf, Serializable {

	private static final long serialVersionUID = 7642821018914163242L;

	//public static final int[] MAX_SIMULATION_HOURS = { 27+3, 33+3, 100 };
    //public static final int MIN_SIMULATION_TIME = 3*60;
	public static final int[] MAX_SIMULATION_HOURS = { 27, 30, 100 };
    public static final int MIN_SIMULATION_TIME = 0;
    public static final float MIN_LOW_DECIMAL_POSITIVE = 0.00001f;
	public static final int[] MAX_SIMULATION_TIME = { MAX_SIMULATION_HOURS[0]*60, MAX_SIMULATION_HOURS[1]*60, MAX_SIMULATION_HOURS[2]*60 };
	public static final int MAX_ITERATIONS = 3;
	
	private CarAllocation allocator;
	private int[] numLpFailures = new int[MAX_ITERATIONS];
	private float roundUpThreshold = 0.99f;;
	private Logger logger;
	HashMap<String,String> propertyMap;
	boolean runMixedIntergerLP = false;
	private static String solverType = "CLP_LINEAR_PROGRAMMING";
	
	public HhCarAllocator( HashMap<String,String> propertyMap,CarAllocation allocator, AbmDataStore dataStore, Map<Integer, Float> experiencedVehicleTravelTimesMap , Logger logger) {
		this.allocator = allocator;
		this.logger = logger;
		this.propertyMap = propertyMap;
		runMixedIntergerLP= Boolean.getBoolean(propertyMap.get(GlobalProperties.RUN_MIXED_INTEGER_LP.toString()));
		if(runMixedIntergerLP)
			solverType = "CBC_MIXED_INTEGER_PROGRAMMING";
	}
	
	
	@Override
	public HouseholdCarAllocation getCarAllocationWithSchedulesForHh( Household hh ) {

		MPSolver solver = null;
		
		int iterNum = 0;
		int iterNumForIntegerizing = 0;
        boolean optimalSolutionFound = false;
        double[][][] carAllocationResults  = null;
        int[][] xijIntergerization = null;
        int[][] xijFixFlag = null;
        while ( ! optimalSolutionFound && iterNum < MAX_ITERATIONS ) {
        	
            solver = allocator.setupLp( hh, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,solverType);
            optimalSolutionFound = allocator.solveLp( solver );
            iterNumForIntegerizing++;
            int numAllocParamters = hh.getAutoTrips().size()*hh.getNumAutos();
            // set solver type to linear if mixed integer was selected and LP failed
            if(!optimalSolutionFound && iterNum == MAX_ITERATIONS -1 && solverType == "CBC_MIXED_INTEGER_PROGRAMMING"){
            	iterNum = 0;
            	solverType = "CLP_LINEAR_PROGRAMMING";
            }
            	
            if ( optimalSolutionFound || iterNum == MAX_ITERATIONS -1 ){
            	
            	//Integerizing the car allocation (Xij)
            	// first bulk integerizing
            	carAllocationResults= allocator.getCarAllocationResults( hh, solver );
            	double[] unsatisDemandResultsIter = allocator.getUnsatisfiedRemandResults( hh, solver );
            	xijIntergerization = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
            	xijFixFlag = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
            	int numVarChanged = 0;
            	for(int i = 0; i < hh.getAutoTrips().size(); i++){
            		for(int j = 0; j < hh.getNumAutos(); j++){
            			
            			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > roundUpThreshold){
            				xijFixFlag[i][j] = 1;
            				xijIntergerization[i][j] = 1;
            			}
            			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0  && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE ) 
            				numVarChanged++;
            		}
            	}
            	boolean optimalSolutionBulkIntegerizedFound = false;
            	boolean optimalSolutionIntegerizedFound = false;
            	boolean allXijIntegers = false;
            	if(numVarChanged==0)
            		allXijIntegers = true;
            	
            	//no integerizing required
            	if(allXijIntegers)
            		break;
            	
            	//Run solver with bulk integerizing
            	solver = allocator.setupLp( hh, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,solverType);
            	optimalSolutionBulkIntegerizedFound = allocator.solveLp( solver );
            	
            	// no need to iteratively integerize if after converting all >threshold to 1 doesnt break the constraints
            	if(optimalSolutionBulkIntegerizedFound){
            		carAllocationResults= allocator.getCarAllocationResults( hh, solver );
            		numVarChanged = 0;
                	for(int i = 0; i < hh.getAutoTrips().size(); i++){
                		for(int j = 0; j < hh.getNumAutos(); j++){
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0  && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE ) 
                				numVarChanged++;
                		}
                	}
                	if(numVarChanged == 0){
                		allXijIntegers = true;
                		break;
                	}
            	
            	}
            	//Iterate if LP fails due to bulk integerizing
            	if(!optimalSolutionBulkIntegerizedFound){
            		xijIntergerization = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
                	xijFixFlag = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
            	}
            	//Run LP again with intergerized XIJ/
            	while ( ! allXijIntegers && iterNumForIntegerizing<=MAX_ITERATIONS*numAllocParamters){           		
            		int maxI = -1;
            		int maxJ = -1;
            		double maxXij = -1;
            		numVarChanged = 0;
            		if(iterNumForIntegerizing == 1){
	            		for(int i = 0; i < hh.getAutoTrips().size(); i++){
	                		for(int j = 0; j < hh.getNumAutos(); j++){                			
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j]>maxXij && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && xijFixFlag[i][j] != 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && unsatisDemandResultsIter[i]<roundUpThreshold){
	                				maxXij = carAllocationResults[CarAllocation.INDEX_CarAllo][i][j];
	                				maxI = i;
	                				maxJ = j;
	                			} 
	                			// if already integer then keep them fixed
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] == 1){
	                				xijFixFlag[i][j] = 1;
	                        		xijIntergerization[i][j] = 1;
	                			}
	                				
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && unsatisDemandResultsIter[i]<roundUpThreshold ) 
	                				numVarChanged++;
	                		}
	                	}
	            		// no need to run if all Xij are integers
	            		if(numVarChanged==0)
	            			allXijIntegers = true;
	            		
	            		if(maxI>=0 && maxJ >=0){
		            		xijFixFlag[maxI][maxJ] = 1;
		            		xijIntergerization[maxI][maxJ] = 1;
	            		}
	            		
            		}
            		solver = allocator.setupLp( hh, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,solverType);
                	optimalSolutionIntegerizedFound = allocator.solveLp( solver );
                	iterNumForIntegerizing++;
                	
                	carAllocationResults= allocator.getCarAllocationResults( hh, solver );
                	unsatisDemandResultsIter = allocator.getUnsatisfiedRemandResults( hh, solver );
                	
                	//Check again how many non-integer allocation
                	maxI = -1;
            		maxJ = -1;
            		maxXij = -1;
            		numVarChanged = 0;
                	numVarChanged = 0;
                	for(int i = 0; i < hh.getAutoTrips().size(); i++){
                		for(int j = 0; j < hh.getNumAutos(); j++){                			
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j]>maxXij && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && xijFixFlag[i][j] != 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1  && unsatisDemandResultsIter[i]<roundUpThreshold){
                				maxXij = carAllocationResults[CarAllocation.INDEX_CarAllo][i][j];
                				maxI = i;
                				maxJ = j;
                			} 
                			// if already integer then keep them fixed
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] == 1){
                				xijFixFlag[i][j] = 1;
                        		xijIntergerization[i][j] = 1;
                			}
                				
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE  && unsatisDemandResultsIter[i]<roundUpThreshold ) 
                				numVarChanged++;
                		}
                	}
                	// no need to run if all Xij are integers
            		if(numVarChanged==0)
            			allXijIntegers = true;
            		
            		if(maxI>=0 && maxJ >=0){
	            		xijFixFlag[maxI][maxJ] = 1;
	            		xijIntergerization[maxI][maxJ] = 1;
            		}
            		
            		//System.out.print(numVarChanged);
            		//System.out.print(hh.getId());
            		//System.out.print("\n");
            	}
            	if(allXijIntegers)
            		break;
            	
            }
            
            numLpFailures[iterNum]++;
            
            logger.info(hh.getId());
            logger.info(" " );
            
            iterNum++;
        	
        }
		
        
                
        // get results from the solver - double[0][person][trip departs] and double[1][persons][trip arrives]
        double[][][] depArrResults = allocator.getDepartArriveResults( hh, solver );
        
        carAllocationResults= allocator.getCarAllocationResults( hh, solver );
        double[][][][] carLinkingResults = allocator.getCarLinkingResults( hh, solver );
        double[] unsatisDemandResults = allocator.getUnsatisfiedRemandResults( hh, solver );
        
        
        HouseholdCarAllocation result = new HouseholdCarAllocation( hh, unsatisDemandResults,depArrResults, carAllocationResults,carLinkingResults,iterNum,iterNumForIntegerizing );
        return result;

	}

	
    public int[] getNumLpFailures() {
    	return numLpFailures;
    }
     
}
