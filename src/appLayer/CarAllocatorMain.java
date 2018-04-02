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
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import algorithms.CarAllocation;
import algorithms.CarAllocationTask;
import algorithms.HhCarAllocator;

import com.pb.common.util.ResourceUtil;


















import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import fileProcessing.ParameterReader;
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
    public Map<Long, double[]> runCarAllocator( ResourceBundle rb, Logger logger, GeographyManager geogManager, SocioEconomicDataManager socec ) {
        
    	if ( logger == null )
    		logger = Logger.getLogger( CarAllocatorMain.class );

        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        String parametersFile = rb.getString( GlobalProperties.PARAMETER_FILE_KEY.toString() );       

        int debugHhId = -1;
        try {
            debugHhId = Integer.valueOf( rb.getString( GlobalProperties.HHID_LOG_REPORT_KEY.toString() ) );
        }
        catch( MissingResourceException e ) {         
        }
        
        threhsoldRoundUp = Float.parseFloat(propertyMap.get(GlobalProperties.ROUND_UP_THRESHOLD.toString()));

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
        writeCarAllocationOutputFile(logger,propertyMap, propertyMap.get("output.trip.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",propertyMap.get("output.car.use.file")+"_"+propertyMap.get("scenario.suffix.name")+".csv",carAllocationResults,geogManager, sharedDistanceObject,socec);
        
        
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
	

    public void writeCarAllocationOutputFile( Logger logger, HashMap<String, String> propertyMap, String outputTripListFilename, String outputDisaggregateCarUseFileName, List<HouseholdCarAllocation> hhCarAllocationResultsList,GeographyManager geogManager, SharedDistanceMatrixData sharedDistanceObject,
    		SocioEconomicDataManager socec) {
        
        PrintWriter outStreamTrip = null;
        PrintWriter outStreamCar = null;
        try {
        	outStreamTrip = new PrintWriter( new BufferedWriter( new FileWriter( outputTripListFilename ) ) );
            String header1 = "hhid,pnum,tripid,tripRecNum,mode,vehId,origPurp,destPurp,origMaz,destMaz,plannedDepartureTime,departureEarly,departureLate,finalDeparture, finalArrival,x1,x2,x3,x4,unsatisfiedResult,numIterationIntegerizing";
            outStreamTrip.println( header1 );   
            outStreamCar = new PrintWriter( new BufferedWriter( new FileWriter( outputDisaggregateCarUseFileName ) ) );
            String header2 = "hhid,carId,aVStatus,autoTripId,autoHhTripId,driverId,destPurp,origMaz,destMaz,origTaz,destTaz,carRepositionType,tripDistance,plannedDeparture,finalDeparture,finalArrival,departureEarly,departureLate,parkDurationAtDestination,ParkCostAtDestination,parkDurationAtNextOrigin,parkCostAtNextOrigin";
            outStreamCar.println( header2 );   
            
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
                String record = hh.getId()+","+ trip.getPnum()+","+ trip.getUniqueTripId()+","+ trip.getTripRecNum()+","+trip.getMode()+","+
                        		trip.getVehId()+","+trip.getOrigAct()+","+ trip.getDestAct()+","+ trip.getOrigMaz()+","+ trip.getDestMaz()+","+ trip.getSchedDepart()+","+ depEarly+","+ depLate +","+finalDeparture+","+finalArrival+","+
                        		xij[0]+","+xij[1]+","+xij[2]+","+xij[3] + ","+ unsatisRes + ","+ depArrObj.getNumIterationsForIntegerizing();
                        
                outStreamTrip.println( record );            
                
               
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
        		
        		
            	double[] carAllocationForTrip = carAllocationResults[CarAllocation.INDEX_CarAllo][i];
            	double[] carAllocationFirstTrip = carAllocationResults[CarAllocation.INDEX_FirstCarTrip][i];
            	double[] carAllocationLastTrip = carAllocationResults[CarAllocation.INDEX_LastCarTrip][i];
            	
                float depEarly = (float)depArrResults[CarAllocation.DEP_EARLY][trip.getPnum()][trips.get(trip.getHhTripId()).getIndivTripId()];
                float depLate = (float)depArrResults[CarAllocation.DEP_LATE][trip.getPnum()][trips.get(trip.getHhTripId()).getIndivTripId()];
                float scheduleDepart = trip.getSchedDepart();
                float scheduleArrive = scheduleDepart+trip.getSchedTime();
                String record =null;
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
            			carUsed[j] = 1;
            			// add first empty trip (if any)
                		if(carAllocationFirstTrip[j]>threhsoldRoundUp && trip.getOrigAct() > 0){
                			autoRecordDistance = distanceFromHome[origTaz];
                			float departureTime = (scheduleDepart + depLate - depEarly) - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
                			record = hh.getId()+","+
                					(j+1)+","+
                					ifAvHh+","+
                					autoTripID +","+
                					0+","+
                					0+","+
                					TRIP_REPOSITIONING_PURPOSE+","+
                					hh.getHomeMaz()+","+
                					trip.getOrigMaz()+","+
                					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                					geogManager.getMazTazValue(trip.getOrigMaz())+","+
                					"-1"+","+
                					autoRecordDistance +","+
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
                			
            			}
                		//Add actual person trip
            			record = hh.getId()+","+
            					(j+1)+","+
            					ifAvHh+","+
            					autoTripID +","+
            					(hhTripId+1)+","+
            					trip.getPnum()+","+
            					trip.getDestAct()+","+
            					trip.getOrigMaz()+","+
            					trip.getDestMaz()+","+
            					geogManager.getMazTazValue(trip.getOrigMaz())+","+
            					geogManager.getMazTazValue(trip.getDestMaz())+","+
            					carRepoType  + ","+
            					tripDistance + ","+
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
            			
            			//Add empty trips
            			for(int k =i+1; k<aTrips.size();k++){
            				AutoTrip nextTrip = aTrips.get(k);
            				float nextDepEarly = (float)depArrResults[CarAllocation.DEP_EARLY][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                            float nextDepLate = (float)depArrResults[CarAllocation.DEP_LATE][nextTrip.getPnum()][trips.get(nextTrip.getHhTripId()).getIndivTripId()];
                            int nextOrigTaz = geogManager.getMazTazValue(nextTrip.getOrigMaz());
                            int nextDestTaz = geogManager.getMazTazValue(nextTrip.getDestMaz());
                            
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
            					record = hh.getId()+","+
                    					(j+1)+","+
                    					ifAvHh+","+
                    					autoTripID +","+
                    					0+","+
                    					0+","+
                    					TRIP_REPOSITIONING_PURPOSE+","+
                    					trip.getDestMaz()+","+
                    					nextTrip.getOrigMaz()+","+
                    					origTaz+","+
                    					nextOrigTaz+","+
                    					"-1"+","+
                    					autoRecordDistance+","+
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
                    			break;
            				}
            				//Add two empty trips if car went to home
            				if(HikForTrip[k]>threhsoldRoundUp){
            					autoRecordDistance =distanceToHome[destTaz];
                    			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly);
                    			
            					record = hh.getId()+","+
                    					(j+1)+","+
                    					ifAvHh+","+
                    					autoTripID +","+
                    					0+","+
                    					0+","+
                    					TRIP_REPOSITIONING_PURPOSE+","+
                    					trip.getDestMaz()+","+
                    					hh.getHomeMaz()+","+
                    					geogManager.getMazTazValue(trip.getDestMaz())+","+
                    					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                    					"-1"+","+
                    					autoRecordDistance + ","+
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
                    			
                    			autoRecordDistance =distanceFromHome[nextOrigTaz];
                    			departureTime = (nextTrip.getSchedDepart() +  nextDepLate - nextDepEarly) -  - Float.parseFloat(propertyMap.get("minutes.per.mile"))*autoRecordDistance;
                    			
                    			
                    			record = hh.getId()+","+
                        					(j+1)+","+
                        					ifAvHh+","+
                        					autoTripID +","+
                        					(i+1)+","+
                        					0+","+
                        					TRIP_REPOSITIONING_PURPOSE+","+
                        					hh.getHomeMaz()+","+
                        					nextTrip.getOrigMaz()+","+
                        					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                        					geogManager.getMazTazValue(nextTrip.getOrigMaz())+","+
                        					"-1"+","+
                        					autoRecordDistance + ","+
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
                        			
                        			break;
                    			}
            				
            				}
            		
            			
            			//Add last empty trip
                		if(carAllocationLastTrip[j]>threhsoldRoundUp && trip.getDestAct() > 0){
                			autoRecordDistance =distanceToHome[destTaz];
                			float departureTime = (scheduleDepart + trip.getSchedTime() + depLate - depEarly);
                		
                			record = hh.getId()+","+
                					(j+1)+","+
                					ifAvHh+","+
                					autoTripID +","+
                					0+","+
                					0+","+
                					TRIP_REPOSITIONING_PURPOSE+","+            					
                					trip.getDestMaz()+","+
                					hh.getHomeMaz()+","+
                					geogManager.getMazTazValue(trip.getDestMaz())+","+
                					geogManager.getMazTazValue(hh.getHomeMaz())+","+
                					"-1"+","+
                					autoRecordDistance + ","+
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
                			
                			break;
            			}
            		}
            		            		
            	}
          	}
         
            for(int j = 0; j < numAuto; j++){
            	if(carUsed[j] == 1){
            		totalUsedCars++;
            	}
            	else{
            		totalUnusedCars[carSufficiencyLevel]++;
            		if(ifAvHh == 1)
            			totalUsedAVCars++;
            		else
            			totalUsedNonAVCars++;
            		
            	}
            		
            }
                    
        }
        outStreamCar.close();    
        outStreamTrip.close();
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
		GeographyManager geogManager = GeographyManager.getInstance();
	    geogManager.setupGeographyManager(rb);
	    
	    // set up socio economic data manager
	    SocioEconomicDataManager socec = SocioEconomicDataManager.getInstance();
	    socec.loadDataFromCsvFile(rb.getString("socec.data.file.name"), rb.getString("socec.data.maz.field"));
	    
	
		mainObj.runCarAllocator( rb, null, geogManager, socec );
    	
		System.out.println ( "Car Tracker finished in " + (int)((System.currentTimeMillis() - start)/1000.0) + " seconds." );
        System.out.println ( "\n" );

        System.exit(0);
	}
	
	
}
