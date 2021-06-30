package algorithms;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.ortools.linearsolver.MPSolver;

import appLayer.WriteCarAllocationOutputFilesMag;
import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import objects.Household;
import objects.HouseholdCarAllocation;
import objects.Person;;


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
	private float roundUpThresholdXij = 0.95f;
	private float roundUpThresholdZijk = 0.95f;
	private Logger logger;
	HashMap<String,String> propertyMap;
	boolean runMixedIntergerLP = false;
	private static String solverType = "CLP_LINEAR_PROGRAMMING";
	private Boolean logProgress;
	public HhCarAllocator( HashMap<String,String> propertyMap,CarAllocation allocator, AbmDataStore dataStore, Map<Integer, Float> experiencedVehicleTravelTimesMap , Logger logger) {
		this.allocator = allocator;
		this.logger = logger;
		this.propertyMap = propertyMap;
		runMixedIntergerLP= Boolean.valueOf(propertyMap.get("start.with.mixed.integer.programming"));
		logProgress = Boolean.valueOf(propertyMap.get("log.progress"));
	}
	
	
	@Override
	public HouseholdCarAllocation getCarAllocationWithSchedulesForHh( Household hh ) {

		MPSolver solver = null;
		
		int iterNum = 0;
		int iterNumForIntegerizing = 0;
        boolean optimalSolutionFound = false;

        //System.out.println("household = " + hh.getId());
        solverType = "CLP_LINEAR_PROGRAMMING";
		//solverType = "CBC_MIXED_INTEGER_PROGRAMMING";
        if(runMixedIntergerLP)
			solverType = "CBC_MIXED_INTEGER_PROGRAMMING";
        
        while ( ! optimalSolutionFound && iterNum < MAX_ITERATIONS ) {
            double[][][] carAllocationResults  = null;
            double[][][][] carLinkingResults = null;
            int[][] xijIntergerization = null;
            int[][] xijFixFlag = null;
            int[][][] sikjIntergerization = null;
            int[][][] sikjFixFlag = null;
            int[][][] gikjIntergerization = null;
            int[][][] gikjFixFlag = null;
            solver = allocator.setupLp( hh,logProgress, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,sikjIntergerization,sikjFixFlag,gikjIntergerization,gikjFixFlag,solverType,iterNumForIntegerizing);
            optimalSolutionFound = allocator.solveLp( solver,iterNumForIntegerizing );
            iterNumForIntegerizing++;
            int numAllocParamters = hh.getAutoTrips().size();
            // set solver type to linear if mixed integer was selected and LP failed
            if(!optimalSolutionFound && iterNum == MAX_ITERATIONS -1 && solverType == "CBC_MIXED_INTEGER_PROGRAMMING"){
            	iterNum = 0;
            	solverType = "CLP_LINEAR_PROGRAMMING";
            }
            	
          
            if ( optimalSolutionFound  ){
            	
            	//Integerizing the car allocation (Xij)
            	// first bulk integerizing
            	carAllocationResults= allocator.getCarAllocationResults( hh, solver );
            	carLinkingResults = allocator.getCarLinkingResults( hh, solver );
            	double[] unsatisDemandResultsIter = allocator.getUnsatisfiedRemandResults( hh, solver );
            	xijIntergerization = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
            	xijFixFlag = new int[hh.getAutoTrips().size()][hh.getNumAutos()];
            	sikjIntergerization = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
            	sikjFixFlag = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
            	gikjIntergerization = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
            	gikjFixFlag = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
            	int numVarChanged = 0;
            	
            	Person[] persons = hh.getPersons();
            	
            	// round up Xij and Hikj
            	for(int i = 0; i < hh.getAutoTrips().size(); i++){

            		for(int j = 0; j < hh.getNumAutos(); j++){
            			
            			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > roundUpThresholdXij ){
            					xijFixFlag[i][j] = 1;
            					xijIntergerization[i][j] = 1;
            			}
            			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0  && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE ) 
            				numVarChanged++;            		

            		}

            	
    				int sumCarTripAllocation = 0;
	        		int carId2 = -1; 
    	        	for( int jj=0; jj < carAllocationResults[CarAllocation.INDEX_CarAllo][i].length; jj++) {
    	        		if (carAllocationResults[CarAllocation.INDEX_CarAllo][i][jj] > WriteCarAllocationOutputFilesMag.threhsoldRoundUp) {
        	        		sumCarTripAllocation ++;
        	        		if ( carId2 < 0 )
        	        			carId2 = jj;
    	        		}
    	        	}
        			
    	        	if ( sumCarTripAllocation > 1 ) {
    	        		
	        			for( int jj=0; jj < carAllocationResults[CarAllocation.INDEX_CarAllo][i].length; jj++) {
    	        			xijFixFlag[i][jj] = 0;
        					xijIntergerization[i][jj] = 0;
	        			}

    	        		int carId1 = persons[hh.getAutoTrips().get(i).getPnum()].getUsualCarId();
    	        		int carId = carId1 >= 0 ? carId1 : carId2;
	        			xijFixFlag[i][carId] = 1;
    					xijIntergerization[i][carId] = 1;
        				numVarChanged++;            		

    	        	}
        			
            	}
            	boolean optimalSolutionBulkIntegerizedFound = false;
            	boolean optimalSolution2ndBulkIntegerizedFound = false;
            	boolean optimalSolutionIntegerizedFound = false;
            	boolean allXijIntegers = false;
            	if(numVarChanged==0)
            		allXijIntegers = true;
            	
            	//no integerizing required
            	if(allXijIntegers)
            		break;
            	
            	//Run solver with bulk integerizing
            	solver = allocator.setupLp( hh, logProgress, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,sikjIntergerization,sikjFixFlag,gikjIntergerization,gikjFixFlag,solverType,iterNumForIntegerizing);
            	optimalSolutionBulkIntegerizedFound = allocator.solveLp( solver,iterNumForIntegerizing );
            	
               	if(!optimalSolutionBulkIntegerizedFound)
            		logger.info("LP failed for after 1st bulk integerizing for hh = "+ hh.getId());
            	
               	
            	// bulk integerize Sik and Gik
            	if(optimalSolutionBulkIntegerizedFound){
            		numVarChanged = 0;
            		carAllocationResults= allocator.getCarAllocationResults( hh, solver );
                	carLinkingResults = allocator.getCarLinkingResults( hh, solver );
                	
                	// round up Xij and Hikj
                	for(int i = 0; i < hh.getAutoTrips().size(); i++){
                		for(int j = 0; j < hh.getNumAutos(); j++){
                			
                			                		
                			for(int k = i+1; k < hh.getAutoTrips().size(); k++){
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k]> roundUpThresholdZijk){
                    				sikjFixFlag[j][i][k] = 1;
                    				sikjIntergerization[j][i][k] = 1;
                    			}
                				
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] < MIN_LOW_DECIMAL_POSITIVE){
                    				sikjFixFlag[j][i][k] = 1;
                    				sikjIntergerization[j][i][k] = 0;
                    			}
                    			
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k]> roundUpThresholdZijk){
                    				gikjFixFlag[j][i][k] = 1;
                    				gikjIntergerization[j][i][k] = 1;
                    			}
                				
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] < MIN_LOW_DECIMAL_POSITIVE){
                    				gikjFixFlag[j][i][k] = 1;
                    				gikjIntergerization[j][i][k] = 0;
                    			}
                    			
                				
                				
                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > 0  
                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
                    				numVarChanged++;           			
                			
                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > 0  
                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
                    				numVarChanged++;   
                			}        
                		}
                	}
            	}
            	if(numVarChanged==0)
            		allXijIntegers = true;
            	
            	//no integerizing required
            	if(allXijIntegers)
            		break;
            	
            	solver = allocator.setupLp( hh, logProgress,MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,sikjIntergerization,sikjFixFlag,gikjIntergerization,gikjFixFlag,solverType,iterNumForIntegerizing);
            	optimalSolution2ndBulkIntegerizedFound = allocator.solveLp( solver,iterNumForIntegerizing );
          
            	
            	if(!optimalSolution2ndBulkIntegerizedFound)
            		logger.info("LP failed for after 2nd bulk integerizing for hh = "+ hh.getId());
            	
            	// no need to iteratively integerize if after converting all >threshold to 1 doesnt break the constraints
            	if(optimalSolutionBulkIntegerizedFound && optimalSolution2ndBulkIntegerizedFound){
            		carAllocationResults= allocator.getCarAllocationResults( hh, solver );
            		carLinkingResults = allocator.getCarLinkingResults( hh, solver );
            		numVarChanged = 0;
                	for(int i = 0; i < hh.getAutoTrips().size(); i++){
                		for(int j = 0; j < hh.getNumAutos(); j++){
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0  && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE ) 
                				numVarChanged++;
                			
                			for(int k = i+1; k < hh.getAutoTrips().size(); k++){                				
                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > 0  
                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
                    				numVarChanged++;           			
                			
                			}   
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
            	if(!optimalSolution2ndBulkIntegerizedFound){
                	sikjIntergerization = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
                	sikjFixFlag = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
                	gikjIntergerization = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
                	gikjFixFlag = new int[hh.getNumAutos()][hh.getAutoTrips().size()][hh.getAutoTrips().size()];
            	}
            	//Run LP again with intergerized XIJ/
        		int maxI = -1;
        		int maxJ = -1;
        		int maxIS = -1;
        		int maxJS = -1;
        		int maxKS = -1;
        		int maxIG = -1;
        		int maxJG = -1;
        		int maxKG = -1;
        		double maxXij = -1;
        		double maxSikj = -1;
        		double maxGikj = -1;
            	while ( ! allXijIntegers && iterNumForIntegerizing<=MAX_ITERATIONS*numAllocParamters){           		

            		
            		numVarChanged = 0;
            		if(iterNumForIntegerizing == 1){
	            		for(int i = 0; i < hh.getAutoTrips().size(); i++){
	                		for(int j = 0; j < hh.getNumAutos(); j++){                			
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j]>maxXij && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && xijFixFlag[i][j] != 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && unsatisDemandResultsIter[i]<roundUpThresholdXij){
	                				maxXij = carAllocationResults[CarAllocation.INDEX_CarAllo][i][j];
	                				maxI = i;
	                				maxJ = j;
	                			} 
	                			// if already integer then keep them fixed
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] == 1){
	                				xijFixFlag[i][j] = 1;
	                        		xijIntergerization[i][j] = 1;
	                			}	                			
	                				
	                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && unsatisDemandResultsIter[i]<roundUpThresholdXij ) 
	                				numVarChanged++;         			
	                			                			
	                			for(int k = i+1; k < hh.getAutoTrips().size(); k++){                				
	                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k]>maxSikj){
	                    				maxSikj = carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k];
	                    				maxIS = i;
	                    				maxJS = j;
	                    				maxKS = k;
	                    			}
	                    			if( carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] == 1){
	                    				sikjFixFlag[j][i][k] = 1;
	                    				sikjIntergerization[j][i][k] = 1;
		                			}	
	                    			
	                    			if( carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] == 1){
	                    				gikjFixFlag[j][i][k] = 1;
	                    				gikjIntergerization[j][i][k] = 1;
		                			}
	                    			
	                    			
	                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k]>maxGikj){
	                    				maxGikj = carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k];
	                    				maxIG = i;
	                    				maxJG = j;
	                    				maxKG = k;
	                    			}
	                				
	                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > 0  
	                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
	                    				numVarChanged++;      
	                				
	                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > 0  
	                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
	                    				numVarChanged++;   
	                			
	                			}  
	                			
	                		}
	                	}
	            		// no need to run if all Xij are integers
	            		if(numVarChanged==0)
	            			allXijIntegers = true;
	            		
	            		if(maxI>=0 && maxJ >=0){
		            		xijFixFlag[maxI][maxJ] = 1;
		            		xijIntergerization[maxI][maxJ] = 1;
	            		}
	            		if(maxIS>0 && maxJS > 0 && maxKS > 0){
	            			sikjFixFlag[maxJS][maxIS][maxKS] = 1;
            				sikjIntergerization[maxJS][maxIS][maxKS] = 1;
	            		}
	            		if(maxIG>0 && maxJG > 0 && maxKG > 0){
	            			gikjFixFlag[maxJG][maxIG][maxKG] = 1;
            				gikjIntergerization[maxJG][maxIG][maxKG] = 1;
	            		}

	            		
            		}
            		solver = allocator.setupLp( hh, logProgress,MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,sikjIntergerization,sikjFixFlag,gikjIntergerization,gikjFixFlag,solverType,iterNumForIntegerizing);
                	optimalSolutionIntegerizedFound = allocator.solveLp( solver,iterNumForIntegerizing );
                	iterNumForIntegerizing++;
                	
                	if(!optimalSolutionIntegerizedFound){
                		logger.info("LP failed for after iterative integerizing for hh = "+ hh.getId());
                		sikjFixFlag[maxJS][maxIS][maxKS] = 1;
        				sikjIntergerization[maxJS][maxIS][maxKS] = 0;
        				gikjFixFlag[maxJG][maxIG][maxKG] = 1;
        				gikjIntergerization[maxJG][maxIG][maxKG] = 0;
        				solver = allocator.setupLp( hh,logProgress, MAX_SIMULATION_TIME[iterNum] ,xijIntergerization,xijFixFlag,sikjIntergerization,sikjFixFlag,gikjIntergerization,gikjFixFlag,solverType,iterNumForIntegerizing);
                    	optimalSolutionIntegerizedFound = allocator.solveLp( solver,iterNumForIntegerizing );
                	}
                	
                	if(optimalSolutionIntegerizedFound){
	                	carAllocationResults= allocator.getCarAllocationResults( hh, solver );
	                	unsatisDemandResultsIter = allocator.getUnsatisfiedRemandResults( hh, solver );
	                	carLinkingResults = allocator.getCarLinkingResults( hh, solver );
	                }
                	// change solver to mixed integer as last resort in case both bulk and iterative integerizing fails
                	else{
                		solverType = "CBC_MIXED_INTEGER_PROGRAMMING"; 
                		optimalSolutionFound = false;
                		break;
                	}
                	//Check again how many non-integer allocation
                	maxI = -1;
            		maxJ = -1;
            		maxXij = -1;
            		maxIS = -1;
            		maxJS = -1;
            		maxKS = -1;
            		maxIG = -1;
            		maxJG = -1;
            		maxKG = -1;
            		maxSikj = -1;
            		maxGikj = -1;
            		
            		numVarChanged = 0;
                	for(int i = 0; i < hh.getAutoTrips().size(); i++){
                		for(int j = 0; j < hh.getNumAutos(); j++){                			
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j]>maxXij && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE && xijFixFlag[i][j] != 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1  && unsatisDemandResultsIter[i]<roundUpThresholdXij){
                				maxXij = carAllocationResults[CarAllocation.INDEX_CarAllo][i][j];
                				maxI = i;
                				maxJ = j;
                			} 
                			// if already integer then keep them fixed
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] == 1){
                				xijFixFlag[i][j] = 1;
                        		xijIntergerization[i][j] = 1;
                			}
                				
                			if(carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] < 1 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > 0 && carAllocationResults[CarAllocation.INDEX_CarAllo][i][j] > MIN_LOW_DECIMAL_POSITIVE  && unsatisDemandResultsIter[i]<roundUpThresholdXij ) 
                				numVarChanged++;
                			
                			for(int k = i+1; k < hh.getAutoTrips().size(); k++){                				
                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k]>maxSikj && sikjFixFlag[j][i][k] != 1 ){
                    				maxSikj = carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k];
                    				maxIS = i;
                    				maxJS = j;
                    				maxKS = k;
                    			}
                    			if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k]>maxGikj && gikjFixFlag[j][i][k] != 1 ){
                    				maxGikj = carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k];
                    				maxIG = i;
                    				maxJG = j;
                    				maxKG = k;
                    			}
                				
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > 0  
                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkDi][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
                    				numVarChanged++;      
                				
                				if(carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] < 1 && carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > 0  
                    					&& carLinkingResults[CarAllocation.INDEX_SameTripParkOk][j][i][k] > MIN_LOW_DECIMAL_POSITIVE ) 
                    				numVarChanged++;   
                			
                			} 
                		}
                	}
                	// no need to run if all Xij are integers
            		if(numVarChanged==0)
            			allXijIntegers = true;
            		
            		if(maxI>=0 && maxJ >=0){
	            		xijFixFlag[maxI][maxJ] = 1;
	            		xijIntergerization[maxI][maxJ] = 1;
            		}
            		if(maxIS>0 && maxJS > 0 && maxKS > 0){
            			sikjFixFlag[maxJS][maxIS][maxKS] = 1;
        				sikjIntergerization[maxJS][maxIS][maxKS] = 1;
            		}
            		if(maxIG>0 && maxJG > 0 && maxKG > 0){
            			gikjFixFlag[maxJG][maxIG][maxKG] = 1;
        				gikjIntergerization[maxJG][maxIG][maxKG] = 1;
            		}
            		//System.out.print(numVarChanged);
            		//System.out.print(hh.getId());
            		//System.out.print("\n");
            	}
            	if(allXijIntegers)
            		break;
            	
            }
            
            numLpFailures[iterNum]++;
            
            
            logger.info("Main lp failed for " + hh.getId());
            //logger.info(" " );
            
            
            iterNum++;
        	
        }
		
        
                
        // get results from the solver - double[0][person][trip departs] and double[1][persons][trip arrives]
        double[][][] depArrResults = allocator.getDepartArriveResults( hh, solver );
        
        double[][][]carAllocationResults= allocator.getCarAllocationResults( hh, solver );
        double[][][][] carLinkingResults = allocator.getCarLinkingResults( hh, solver );
        double[] unsatisDemandResults = allocator.getUnsatisfiedRemandResults( hh, solver );
        
        
        HouseholdCarAllocation result = new HouseholdCarAllocation( hh, unsatisDemandResults,depArrResults, carAllocationResults,carLinkingResults,iterNum,iterNumForIntegerizing );
        return result;

	}

	
    public int[] getNumLpFailures() {
    	return numLpFailures;
    }
     
}
