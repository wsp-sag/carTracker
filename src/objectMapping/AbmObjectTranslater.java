package objectMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fileProcessing.AbmDataStore;
import fileProcessing.GlobalProperties;
import objects.AbmDataVehicleIdMap;
import utility.IndexSort;
import utility.Parsing;


public class AbmObjectTranslater {
	
	//MAG public static final int NUM_MINUTES_FOR_START_OF_DAY_ABM_4_AM = 4*60;
	
	public static final float MINUTES_PER_MILE = 2.0f;

	// MAG vales
	//public static final int TRIP_PARTY_FIELD_INDEX = 17;
	//public static final int TRIP_ORIG_PURP_FIELD_INDEX = 22;
	//public static final int TRIP_DEST_PURP_FIELD_INDEX = 23;
	//public static final int TRIP_ORIG_MAZ_FIELD_INDEX = 19;
	//public static final int TRIP_DEST_MAZ_FIELD_INDEX = 21;
	//public static final int TRIP_DEPART_MINUTE_FIELD_INDEX = 26;
	//public static final int TRIP_PLANNED_TRAVEL_MINUTES_FIELD_INDEX = 29;
	//public static final int TRIP_MODE_FIELD_INDEX = 24;

	// MORPC vales	
//	public static final int PERSON_TYPE_FIELD_INDEX = 3;
//	public static final int TRIP_ID_FIELD_INDEX = 0;
//	public static final int JOINT_TRIP_ID_FIELD_INDEX = 1;
//	public static final int TRIP_PNUM_FIELD_INDEX = 3;
//	public static final int TRIP_PARTY_FIELD_INDEX = 18;
//	public static final int TRIP_ORIG_PURP_FIELD_INDEX = 23;
//	public static final int TRIP_DEST_PURP_FIELD_INDEX = 24;
//	public static final int TRIP_ORIG_MAZ_FIELD_INDEX = 20;
//	public static final int TRIP_DEST_MAZ_FIELD_INDEX = 22;
//	public static final int TRIP_DEPART_MINUTE_FIELD_INDEX = 27;
//	public static final int TRIP_ARRIVE_MINUTE_FIELD_INDEX = 30;
//	public static final int TRIP_PLANNED_DISTANCE_FIELD_INDEX = 28;
//	public static final int TRIP_MODE_FIELD_INDEX = 25;
//	public static final int TRIP_VOT_FIELD_INDEX = 34;
//	public static final int NUM_TRIP_RECORD_FIELDS = 35;

	// ARC values
//	public static final int TRIP_ID_FIELD_INDEX = 42;
//	public static final int JOINT_TRIP_ID_FIELD_INDEX = 34;
//	public static final int TRIP_PNUM_FIELD_INDEX = 2;
//	public static final int TRIP_PARTY_FIELD_INDEX = 32;
//	public static final int TRIP_ORIG_PURP_FIELD_INDEX = 44;
//	public static final int TRIP_DEST_PURP_FIELD_INDEX = 45;
//	public static final int TRIP_ORIG_MAZ_FIELD_INDEX = 10;
//	public static final int TRIP_DEST_MAZ_FIELD_INDEX = 11;
//	public static final int TRIP_DEPART_MINUTE_FIELD_INDEX = 40;
//	public static final int TRIP_ARRIVE_MINUTE_FIELD_INDEX = 41;
//	public static final int TRIP_PLANNED_DISTANCE_FIELD_INDEX = 31;
//	public static final int TRIP_MODE_FIELD_INDEX = 14;
//	public static final int TRIP_VOT_FIELD_INDEX = 37;
//	public static final int NUM_TRIP_RECORD_FIELDS = 49;
	
	public static final String HH_NUM_AUTO_FIELD_KEY = "hh.num.auto.field";
	public static final String HH_MAZ_KEY = "hh.maz.field";
	public static final String HH_AV_FLAG_KEY="hh.av.flag.field";
	public static final String PERSON_TYPE_FIELD_KEY = "person.type.field";
	public static final String PERSON_USUAL_CAR_ID_FIELD_KEY = "person.usualcar.id.field";
	
	public static final String TRIP_ID_FIELD_KEY = "trip.id.field";
	public static final String JOINT_TRIP_ID_FIELD_KEY = "trip.joint.trip.id.field";
	public static final String TRIP_PNUM_FIELD_KEY = "trip.pnum.field";
	public static final String TRIP_PARTY_FIELD_KEY = "trip.party.field";
	public static final String TRIP_ORIG_PURP_FIELD_KEY = "trip.orig.purp.field";
	public static final String TRIP_DEST_PURP_FIELD_KEY = "trip.dest.purp.field";
	public static final String TRIP_ORIG_MAZ_FIELD_KEY = "trip.orig.maz.field";
	public static final String TRIP_DEST_MAZ_FIELD_KEY = "trip.dest.maz.field";
	public static final String TRIP_DEPART_MINUTE_FIELD_KEY = "trip.depart.minute.field";
	public static final String TRIP_ARRIVE_MINUTE_FIELD_KEY = "trip.arrive.minute.field";
	public static final String TRIP_PLANNED_DISTANCE_FIELD_KEY = "trip.planned.distance.field";
	public static final String TRIP_MODE_FIELD_KEY = "trip.mode.field";
	public static final String TRIP_ACTIVITY_DURATION_KEY = "abm.data.file.final.activity.duration";
	public static final String NUM_AUTOS_FIELD_KEY = "hh.num.auto.field";
	
	public static final int SOV_MODE = 1;
	public static final int HOV2_DR_MODE = 2;
	public static final int HOV3_DR_MODE = 3;
	
	private AbmDataStore dataStore;

	private int abmStartOfDayMinute;
	
	private String persTypeField;
	
	private String tripIdField;
	private String jointTripIdField;
	private String tripPnumField;
	private String tripPartyField;
	private String tripOrigPurpField;
	private String tripDestPurpField;
	private String tripOrigMazField;
	private String tripDestMazField;
	private String tripDepartMinuteField;
	private String tripArriveMinuteField;
	private String tripPlannedDistanceField;
	private String tripModeField;
	private String tripVotField;
	private String minActDurationField;
	private String actDurationField;
	private String numAutosField;
	private String homeMazField;
	private String ifAvHhField;
	private String usualCarIdField;
	
	private int[] personTrips;
	private int hhAutoTrips;

	private Integer[] chronologicalTripIndices;
	private Integer[] chronologicalAutoTripIndices;
	
	private List<Object> odArrayObjects;
	private List<Object> odAutoArrayObjects;
	
	private List<Object> tripInfo;
	private List<Object> autoTripInfo;
	
	private boolean debugging; 
	private AbmDataVehicleIdMap abmDataVehicleIdMap;
	
	private int numTripRecords;
	private int minDuration;
	private int numAutoTrips;
	
	
	public AbmObjectTranslater( Map<String, String> propertyMap, AbmDataStore dataStore, Map<Integer, Float> experiencedVehicleTravelTimesMap, Map<Integer, Float> tripAdjDepMap, AbmDataVehicleIdMap abmDataVehicleIdMap, int hhid, boolean debugging ) {

		this.dataStore = dataStore;
		this.debugging = debugging;
		this.abmDataVehicleIdMap = abmDataVehicleIdMap;
		
		// read the csv files into datastore as List<List<String>> objects. 
		int numRecords = dataStore.getNumTripRecords(hhid);
		numTripRecords = numRecords;
		
		persTypeField =  propertyMap.get( PERSON_TYPE_FIELD_KEY ) ;
		minDuration = 5;
		tripIdField = propertyMap.get( TRIP_ID_FIELD_KEY ) ;
		jointTripIdField =  propertyMap.get( JOINT_TRIP_ID_FIELD_KEY ) ;
		tripPnumField =  propertyMap.get( TRIP_PNUM_FIELD_KEY ) ;
		tripPartyField =  propertyMap.get( TRIP_PARTY_FIELD_KEY ) ;
		tripDestPurpField =  propertyMap.get( TRIP_DEST_PURP_FIELD_KEY ) ;
		tripOrigPurpField =  propertyMap.get( TRIP_ORIG_PURP_FIELD_KEY ) ;
		tripOrigMazField = propertyMap.get( TRIP_ORIG_MAZ_FIELD_KEY ) ;
		tripDestMazField =  propertyMap.get( TRIP_DEST_MAZ_FIELD_KEY ) ;
		tripDepartMinuteField =  propertyMap.get( TRIP_DEPART_MINUTE_FIELD_KEY ) ;
		tripArriveMinuteField = propertyMap.get( TRIP_ARRIVE_MINUTE_FIELD_KEY ) ;
		tripPlannedDistanceField =  propertyMap.get( TRIP_PLANNED_DISTANCE_FIELD_KEY ) ;
		tripModeField =  propertyMap.get( TRIP_MODE_FIELD_KEY ) ;
		numAutosField = propertyMap.get(NUM_AUTOS_FIELD_KEY);
		actDurationField = propertyMap.get(TRIP_ACTIVITY_DURATION_KEY);
		homeMazField = propertyMap.get(HH_MAZ_KEY);
		ifAvHhField= propertyMap.get(HH_AV_FLAG_KEY);
		usualCarIdField = propertyMap.get(PERSON_USUAL_CAR_ID_FIELD_KEY);
		autoTripInfo = new ArrayList<Object>();
		if ( numTripRecords > 0 ) {
			
			personTrips = getPersonTripCount( hhid );
			
			tripInfo=  getAllTripInformation( hhid, experiencedVehicleTravelTimesMap, tripAdjDepMap );			
			autoTripInfo =  getAutoTripInformation( hhid, experiencedVehicleTravelTimesMap, tripAdjDepMap );
			
			hhAutoTrips = numAutoTrips;
			float[] tripDeparts = null;
			if ( tripInfo.size() > 0 )
				tripDeparts = (float[])tripInfo.get(2);
			else
				tripDeparts = new float[0];
			
			float[] autoTripDeparts = null;
			if(autoTripInfo.size()>0)
				autoTripDeparts =  (float[])autoTripInfo.get(1);
			else
				autoTripDeparts = new float[0];
			
			
			
			chronologicalTripIndices = getChronologicalTripIndices( tripDeparts );
			//for ( int i=0; i < chronologicalTripIndices.length; i++ )
			//	chronologicalTripIndices[i] = i+1;
			
			if(numAutoTrips>0){
				chronologicalAutoTripIndices = getChronologicalTripIndices( autoTripDeparts );
				odAutoArrayObjects = getAutoTripOdArrays();
			}
			
			odArrayObjects = getTripOdArrays();
			
			
				//for ( int i=0; i < chronologicalAutoTripIndices.length; i++ )
				//	chronologicalAutoTripIndices[i] = i+1;
				
				
				
		}
		
	}
	
		

	private List<Object> getAllTripInformation( int hhid, Map<Integer, Float> experiencedVehicleTravelTimesMap, Map<Integer, Float> tripAdjDepMap ) {
		
		List<List<Object>> resultList = new ArrayList<List<Object>>();
		List<Object> allTripsResultList = new ArrayList<Object>();
		List<Object> autoTripsResultList = new ArrayList<Object>();
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getTripFieldIndexMap();
		

		// key is jtId, value is Set of tripNums for jtId.
		Map<Integer, Set<Integer>> linkedJointTrips = new TreeMap<Integer, Set<Integer>>();
				
		Map<Integer, int[][]> jointParties = new HashMap<Integer, int[][]>();
		
		Map<Integer,Integer> jointDriverTripId = new HashMap<Integer,Integer>();
		
		List<List<String>> tripRecords = dataStore.getTripRecords( hhid );

		if( tripRecords == null ){
			return allTripsResultList;
		}
		
		int[] tripPnums = new int[ tripRecords.size()+1 ];
		int[] tripOrigMazs = new int[ tripRecords.size()+1 ];
		int[] tripDestMazs = new int[ tripRecords.size()+1 ];
		int[] tripOrigPurps = new int[ tripRecords.size()+1 ];
		int[] tripDestPurps = new int[ tripRecords.size()+1 ];
		float[] tripDeparts = new float[ tripRecords.size()+1 ];
		float[] indivPlannedTravelTimes = new float[ tripRecords.size()+1 ];
		int[] tripModes = new int[ tripRecords.size()+1 ];
		int[] uniqueTripIds = new int[ tripRecords.size()+1 ];
		int[] jointTripIds = new int[ tripRecords.size()+1 ];
		int[][] tripParties = new int[ tripRecords.size()+1 ][];
		int[] tripRecNums = new int[ tripRecords.size()+1 ];
		int[] tripVehIds = new int[ tripRecords.size()+1 ];
		int[] assignedTripMode = new int[ tripRecords.size()+1 ];
		float[] distance = new float[ tripRecords.size()+1 ];
		int[] linkedTripIds = new int[ tripRecords.size()+1 ];
		int[] jointDriverPnum = new int[ tripRecords.size()+1 ];
		double[] tripMinActDur = new double[ tripRecords.size()+1 ];
		int[] tripsHhAutoTripId = new int[ tripRecords.size()+1 ];
		float[] activityDuration = new float[ tripRecords.size()+1 ];
		
		// Calculate the number of auto trips in hh
		numAutoTrips = 0;
		for ( List<String> record : tripRecords ) {
			String modeField = record.get( fieldIndexMap.get(tripModeField) );
			int mode = Integer.parseInt( modeField );
			if(mode == SOV_MODE || mode == HOV2_DR_MODE || mode == HOV3_DR_MODE)
				numAutoTrips++;
		}
		int[] autoTripId = new int[ numAutoTrips+1 ];
		int[] autoTripPnums = new int[ numAutoTrips+1 ];
		float[] autoTripDeparts = new float[ numAutoTrips+1 ];
		int[] autoTripOrigTazs = new int[ numAutoTrips+1 ];
		int[] autoTripDestTazs = new int[ numAutoTrips+1 ];
		int[] autoTripsPersonTripId = new int[ numAutoTrips+1 ]; // person trip ID for the hh auto trip
		int[][] autoTripParties = new int[ numAutoTrips+1 ][];
		int[] autoTripOrigPurps = new int[ numAutoTrips+1 ];
		int[] autoTripDestPurps = new int[ numAutoTrips+1 ];
		float[] autoTripDistance = new float[ numAutoTrips+1 ];
		float[] autoTripTravelTime = new float[ numAutoTrips+1 ];
		
		int tripNum = 1;
		int pnum = 0;
		int autoTripNum =1 ;
		
		
		for ( List<String> record : tripRecords ) {

			try {
				
				String id = record.get( fieldIndexMap.get(tripIdField) );
				uniqueTripIds[tripNum] = Integer.parseInt( id );
				
				String personNumValue = record.get( fieldIndexMap.get(tripPnumField) );
				pnum = Integer.parseInt( personNumValue );
				tripPnums[tripNum] = pnum;
				
				String modeValue = record.get( fieldIndexMap.get(tripModeField) );
				int mode = Integer.parseInt( modeValue );
				tripModes[tripNum] = mode;
	
			
				String jtId = record.get( fieldIndexMap.get(jointTripIdField) );
				jointTripIds[tripNum] = Integer.parseInt( jtId );
				
				jointDriverPnum[tripNum] = pnum;
				String party = "";
				if ( jointTripIds[tripNum] > 0 ) {
					party = record.get( fieldIndexMap.get(tripPartyField) );
					tripParties[tripNum] = Parsing.getOneDimensionalIntArrayValuesFromExportString( party );
					
					if ( linkedJointTrips.containsKey( jointTripIds[tripNum] ) ) {
						linkedJointTrips.get( jointTripIds[tripNum] ).add( tripNum );
					}
					else {
						Set<Integer> tempSet = new TreeSet<Integer>();
						tempSet.add( tripNum );
						linkedJointTrips.put( jointTripIds[tripNum], tempSet );
					}
					
					if ( tripParties[tripNum][0] == pnum ) {
						jointDriverTripId.put( jointTripIds[tripNum], tripNum );
					}
						
					jointDriverPnum[tripNum] = tripParties[tripNum][0];
				}
	
				String distValue = record.get( fieldIndexMap.get(tripPlannedDistanceField) );
				distance[tripNum] = Float.parseFloat( distValue );

				tripMinActDur[tripNum] = minDuration;
				
							
				String omazValue = record.get( fieldIndexMap.get(tripOrigMazField) );
				int omaz = Integer.parseInt( omazValue );
				tripOrigMazs[tripNum] = omaz;
				
				String dmazValue = record.get( fieldIndexMap.get(tripDestMazField) );
				int dmaz = Integer.parseInt( dmazValue );
				tripDestMazs[tripNum] = dmaz;
				
				String purposeValue = record.get( fieldIndexMap.get(tripOrigPurpField) );
				int oPurposeIndex = Integer.parseInt( purposeValue );
				tripOrigPurps[tripNum] = oPurposeIndex;
				
				String dPurposeValue = record.get( fieldIndexMap.get(tripDestPurpField) );
				int dPurposeIndex = Integer.parseInt( dPurposeValue );
				tripDestPurps[tripNum] = dPurposeIndex;
			

				String departValue = record.get( fieldIndexMap.get(tripDepartMinuteField) );
				float depart = Math.round(Float.parseFloat( departValue )*100)/100;
				
				String arriveValue = record.get( fieldIndexMap.get(tripArriveMinuteField) );
				float arrive =  Math.round(Float.parseFloat( arriveValue )*100)/100;

				String durationValue = record.get( fieldIndexMap.get(actDurationField) );
				activityDuration[tripNum] =  (Math.round(Float.parseFloat( durationValue )*100)/100);		
				
				//MAG String timeValue = record.get( TRIP_PLANNED_TRAVEL_MINUTES_FIELD_INDEX );
				//MAG indivPlannedTravelTimes[tripNum] = (int)( Float.parseFloat( timeValue ) );
				//indivPlannedTravelTimes[tripNum] = distance[tripNum] * 2;	// multiply by 2 to convert distance to time at 30 mph
				indivPlannedTravelTimes[tripNum] = arrive - depart;
				
				float departMinute = depart + abmStartOfDayMinute;
	
				if ( tripAdjDepMap != null ) {
					if ( debugging ) {
						departMinute = tripAdjDepMap.get( tripNum );
					}
					else {
						int vehId = abmDataVehicleIdMap.getVehicleId( hhid, pnum, uniqueTripIds[tripNum] );
						departMinute = tripAdjDepMap.get( vehId);
					}
				}
				
				tripDeparts[tripNum] = departMinute;
				tripsHhAutoTripId[tripNum] = 0;
				// get information of auto trips
				if(mode == SOV_MODE || mode == HOV2_DR_MODE || mode == HOV3_DR_MODE){
					autoTripId[autoTripNum] = autoTripNum;
					autoTripPnums[autoTripNum] = pnum;
					autoTripDeparts[autoTripNum] =  Float.parseFloat( departValue );
					autoTripOrigTazs[autoTripNum] = omaz;
					autoTripDestTazs[autoTripNum] = dmaz;
					autoTripsPersonTripId[autoTripNum] = tripNum;
					autoTripOrigPurps[autoTripNum] = oPurposeIndex;
					autoTripDestPurps[autoTripNum] = dPurposeIndex;
					autoTripParties[autoTripNum] = Parsing.getOneDimensionalIntArrayValuesFromExportString( party );
					autoTripDistance[autoTripNum] =  Float.parseFloat( distValue );
					autoTripTravelTime[autoTripNum]= arrive - depart;
					tripsHhAutoTripId[tripNum]= autoTripNum;
					autoTripNum++;
				}
				tripNum++;

			}
			catch( Exception e ) {
				System.out.println( "hhid=" + hhid + ", pnum=" + pnum + ", tripNum=" + tripNum + ", record=" + record );
				e.printStackTrace();
			}

		}


		// set the linkedTripId and auto id for every joint trip participation to the uniqueTripId of the driver
		for ( int i=1; i < tripRecNums.length; i++ ) {
			tripNum = uniqueTripIds[i];
			if ( jointTripIds[tripNum] > 0 ) {
				int id = jointDriverTripId.get( jointTripIds[tripNum] );			
				linkedTripIds[tripNum] = uniqueTripIds[id];		
				tripsHhAutoTripId[tripNum] = tripsHhAutoTripId[id];
			}
		}
		
				
		
		
		
		
		int[][] participants = new int[ linkedJointTrips.size() + 1 ][];

		for ( int jtId : linkedJointTrips.keySet() ) {
			Set<Integer> tempSet = linkedJointTrips.get( jtId );
			int[][] jointParticipations = new int[tempSet.size()][2];
			int n = 0;
			for ( int tNum : tempSet ) {
				jointParticipations[n][0] = tripPnums[tNum];
				jointParticipations[n][1] = tNum;
				n++;
			}
			jointParties.put( jtId, jointParticipations );
			
			participants[jtId] = new int[jointParticipations.length+1];
			for ( n=0; n < jointParticipations.length; n++ )
				participants[jtId][n+1] = jointParticipations[n][0];
		}
		
		allTripsResultList.add( tripOrigPurps );
		allTripsResultList.add( tripDestPurps );
		allTripsResultList.add( tripDeparts );
		allTripsResultList.add( indivPlannedTravelTimes );
		allTripsResultList.add( tripPnums );
		allTripsResultList.add( tripModes );
		allTripsResultList.add( uniqueTripIds );
		allTripsResultList.add( jointTripIds );
		allTripsResultList.add( jointParties );
		allTripsResultList.add( participants );
		allTripsResultList.add( tripOrigMazs );
		allTripsResultList.add( tripDestMazs );
		allTripsResultList.add( tripRecNums );
		allTripsResultList.add( distance );
		allTripsResultList.add( assignedTripMode );
		allTripsResultList.add( tripVehIds );
		allTripsResultList.add( linkedTripIds );
		allTripsResultList.add( jointDriverPnum );
		allTripsResultList.add( tripMinActDur );
		allTripsResultList.add(tripsHhAutoTripId);
		allTripsResultList.add(activityDuration);
		return allTripsResultList;
		
	}	
	
private List<Object> getAutoTripInformation( int hhid, Map<Integer, Float> experiencedVehicleTravelTimesMap, Map<Integer, Float> tripAdjDepMap ) {
		
		List<List<Object>> resultList = new ArrayList<List<Object>>();
		List<Object> allTripsResultList = new ArrayList<Object>();
		List<Object> autoTripsResultList = new ArrayList<Object>();
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getTripFieldIndexMap();
		

		// key is jtId, value is Set of tripNums for jtId.
		Map<Integer, Set<Integer>> linkedJointTrips = new TreeMap<Integer, Set<Integer>>();
				
		Map<Integer, int[][]> jointParties = new HashMap<Integer, int[][]>();
		
		Map<Integer,Integer> jointDriverTripId = new HashMap<Integer,Integer>();
		
		List<List<String>> tripRecords = dataStore.getTripRecords( hhid );

		
		
		int[] tripPnums = new int[ tripRecords.size()+1 ];
		int[] tripOrigMazs = new int[ tripRecords.size()+1 ];
		int[] tripDestMazs = new int[ tripRecords.size()+1 ];
		int[] tripOrigPurps = new int[ tripRecords.size()+1 ];
		int[] tripDestPurps = new int[ tripRecords.size()+1 ];
		float[] tripDeparts = new float[ tripRecords.size()+1 ];
		float[] indivPlannedTravelTimes = new float[ tripRecords.size()+1 ];
		int[] tripModes = new int[ tripRecords.size()+1 ];
		int[] uniqueTripIds = new int[ tripRecords.size()+1 ];
		int[] jointTripIds = new int[ tripRecords.size()+1 ];
		int[][] tripParties = new int[ tripRecords.size()+1 ][];
		int[] tripRecNums = new int[ tripRecords.size()+1 ];
		int[] tripVehIds = new int[ tripRecords.size()+1 ];
		int[] assignedTripMode = new int[ tripRecords.size()+1 ];
		float[] distance = new float[ tripRecords.size()+1 ];
		int[] linkedTripIds = new int[ tripRecords.size()+1 ];
		int[] jointDriverPnum = new int[ tripRecords.size()+1 ];
		double[] tripMinActDur = new double[ tripRecords.size()+1 ];
		int[] tripsHhAutoTripId = new int[ tripRecords.size()+1 ];
		
		// Calculate the number of auto trips in hh
		numAutoTrips = 0;
		for ( List<String> record : tripRecords ) {
			String modeField = record.get( fieldIndexMap.get(tripModeField) );
			int mode = Integer.parseInt( modeField );
			if(mode == SOV_MODE || mode == HOV2_DR_MODE || mode == HOV3_DR_MODE)
				numAutoTrips++;
		}
		int[] autoTripId = new int[ numAutoTrips+1 ];
		int[] autoTripPnums = new int[ numAutoTrips+1 ];
		float[] autoTripDeparts = new float[ numAutoTrips+1 ];
		int[] autoTripOrigTazs = new int[ numAutoTrips+1 ];
		int[] autoTripDestTazs = new int[ numAutoTrips+1 ];
		int[] autoTripsPersonTripId = new int[ numAutoTrips+1 ]; // person trip ID for the hh auto trip
		int[][] autoTripParties = new int[ numAutoTrips+1 ][];
		int[] autoTripOrigPurps = new int[ numAutoTrips+1 ];
		int[] autoTripDestPurps = new int[ numAutoTrips+1 ];
		float[] autoTripDistance = new float[ numAutoTrips+1 ];
		float[] autoTripTravelTime = new float[ numAutoTrips+1 ];
		
		int tripNum = 1;
		int pnum = 0;
		int autoTripNum =1 ;
		
		if( numAutoTrips == 0 ){
			return autoTripsResultList;
		}
		for ( List<String> record : tripRecords ) {

			try {
				
				String id = record.get( fieldIndexMap.get(tripIdField) );
				uniqueTripIds[tripNum] = Integer.parseInt( id );
				
				String personNumValue = record.get( fieldIndexMap.get(tripPnumField) );
				pnum = Integer.parseInt( personNumValue );
				tripPnums[tripNum] = pnum;
				
				String modeValue = record.get( fieldIndexMap.get(tripModeField) );
				int mode = Integer.parseInt( modeValue );
				tripModes[tripNum] = mode;
	
			
				String jtId = record.get( fieldIndexMap.get(jointTripIdField) );
				jointTripIds[tripNum] = Integer.parseInt( jtId );
				
				jointDriverPnum[tripNum] = pnum;
				String party = "";
				if ( jointTripIds[tripNum] > 0 ) {
					party = record.get( fieldIndexMap.get(tripPartyField) );
					tripParties[tripNum] = Parsing.getOneDimensionalIntArrayValuesFromExportString( party );
					
					if ( linkedJointTrips.containsKey( jointTripIds[tripNum] ) ) {
						linkedJointTrips.get( jointTripIds[tripNum] ).add( tripNum );
					}
					else {
						Set<Integer> tempSet = new TreeSet<Integer>();
						tempSet.add( tripNum );
						linkedJointTrips.put( jointTripIds[tripNum], tempSet );
					}
					
					if ( tripParties[tripNum][0] == pnum ) {
						jointDriverTripId.put( jointTripIds[tripNum], tripNum );
					}
						
					jointDriverPnum[tripNum] = tripParties[tripNum][0];
				}
	
				String distValue = record.get( fieldIndexMap.get(tripPlannedDistanceField) );
				distance[tripNum] = Float.parseFloat( distValue );

				tripMinActDur[tripNum] = minDuration;
				
							
				String omazValue = record.get( fieldIndexMap.get(tripOrigMazField) );
				int omaz = Integer.parseInt( omazValue );
				tripOrigMazs[tripNum] = omaz;
				
				String dmazValue = record.get( fieldIndexMap.get(tripDestMazField) );
				int dmaz = Integer.parseInt( dmazValue );
				tripDestMazs[tripNum] = dmaz;
				
				String purposeValue = record.get( fieldIndexMap.get(tripOrigPurpField) );
				int oPurposeIndex = Integer.parseInt( purposeValue );
				tripOrigPurps[tripNum] = oPurposeIndex;
				
				String dPurposeValue = record.get( fieldIndexMap.get(tripDestPurpField) );
				int dPurposeIndex = Integer.parseInt( dPurposeValue );
				tripDestPurps[tripNum] = dPurposeIndex;
			

				String departValue = record.get( fieldIndexMap.get(tripDepartMinuteField) );
				float depart = Float.parseFloat( departValue );
				
				String arriveValue = record.get( fieldIndexMap.get(tripArriveMinuteField) );
				float arrive = Float.parseFloat( arriveValue );
							
				//MAG String timeValue = record.get( TRIP_PLANNED_TRAVEL_MINUTES_FIELD_INDEX );
				//MAG indivPlannedTravelTimes[tripNum] = (int)( Float.parseFloat( timeValue ) );
				//indivPlannedTravelTimes[tripNum] = distance[tripNum] * 2;	// multiply by 2 to convert distance to time at 30 mph
				indivPlannedTravelTimes[tripNum] = arrive - depart;
				
				float departMinute = depart + abmStartOfDayMinute;
	
				if ( tripAdjDepMap != null ) {
					if ( debugging ) {
						departMinute = tripAdjDepMap.get( tripNum );
					}
					else {
						int vehId = abmDataVehicleIdMap.getVehicleId( hhid, pnum, uniqueTripIds[tripNum] );
						departMinute = tripAdjDepMap.get( vehId);
					}
				}
				
				tripDeparts[tripNum] = departMinute;
				tripsHhAutoTripId[tripNum] = 0;
				
							
					// get information of auto trips
				if(mode == SOV_MODE || mode == HOV2_DR_MODE || mode == HOV3_DR_MODE){
					autoTripId[autoTripNum] = autoTripNum;
					autoTripPnums[autoTripNum] = pnum;
					autoTripDeparts[autoTripNum] =  Float.parseFloat( departValue );
					autoTripOrigTazs[autoTripNum] = omaz;
					autoTripDestTazs[autoTripNum] = dmaz;
					autoTripsPersonTripId[autoTripNum] = tripNum;
					autoTripOrigPurps[autoTripNum] = oPurposeIndex;
					autoTripDestPurps[autoTripNum] = dPurposeIndex;
					autoTripParties[autoTripNum] = Parsing.getOneDimensionalIntArrayValuesFromExportString( party );
					autoTripDistance[autoTripNum] =  Float.parseFloat( distValue );
					autoTripTravelTime[autoTripNum]= arrive - depart;
					tripsHhAutoTripId[autoTripNum]= autoTripNum;
					autoTripNum++;
				}
				tripNum++;

			}
			catch( Exception e ) {
				System.out.println( "hhid=" + hhid + ", pnum=" + pnum + ", tripNum=" + tripNum + ", record=" + record );
				e.printStackTrace();
			}

		}



		
		autoTripsResultList.add(autoTripPnums);
		autoTripsResultList.add( autoTripDeparts );
		autoTripsResultList.add(autoTripOrigTazs);
		autoTripsResultList.add(autoTripDestTazs);
		autoTripsResultList.add(autoTripsPersonTripId);
		autoTripsResultList.add(autoTripParties);
		autoTripsResultList.add(autoTripOrigPurps);
		autoTripsResultList.add(autoTripPnums);
		autoTripsResultList.add(autoTripDestPurps);
		autoTripsResultList.add(autoTripId);
		autoTripsResultList.add(autoTripDistance);
		autoTripsResultList.add(autoTripTravelTime);
		
		return autoTripsResultList;
		
	}	

	private Integer[] getChronologicalTripIndices( float[] indivDeparts ) {

		int numTotalTrips = indivDeparts.length - 1;
		
		// joint trip indices are distinguished from individual in this array of combined indices by adding JOINT_TRIP_INDEX_OFFSET.
		Integer[] chronologicalIndices = new Integer[ numTotalTrips ];
		
			
		int[] sortData = new int[numTotalTrips];
		int[] originalIndices = new int[numTotalTrips];
		if ( indivDeparts != null ) {
			for ( int j=1; j < indivDeparts.length; j++ ) {
				sortData[j-1] = (int)(indivDeparts[j] * 1000000) + j;
				originalIndices[j-1] = j;
			}
		}

		int[] sortedIndices = IndexSort.indexSort( sortData );
		
		for ( int j=0; j < sortedIndices.length; j++ ) {
			int k = sortedIndices[j];
			chronologicalIndices[j] = originalIndices[k];
		}
		
		return chronologicalIndices;
	
	}

	
	private List<Object> getTripOdArrays() {

		int[] tripOrigMazs = null;
		int[] tripDestMazs = null;
		int[] tripOrigPurps = null;
		int[] tripDestPurps = null;
		float[] tripDeparts = null;
		float[] tripTravelTimes = null;
		int[] tripModes = null;
		int[] tripRecNums = null;
		float[] tripDistances = null;
		int[] assignedTripModes = null;
		int[] tripVehIds = null;
		int[] linkedToIds = null;
		int[] jointDriverPnum = null;
		double[] tripMinActDur = null;
		int[] tripsHhAutoTripId = null;
		float[] activityDuration = null;
		
		if ( tripInfo.size() > 0 ) {
			tripOrigPurps = (int[])tripInfo.get(0);
			tripDestPurps = (int[])tripInfo.get(1);
			tripDeparts = (float[])tripInfo.get(2);
			tripTravelTimes = (float[])tripInfo.get(3);
			tripModes = (int[])tripInfo.get(5);
			tripOrigMazs = (int[])tripInfo.get(10);
			tripDestMazs = (int[])tripInfo.get(11);
			tripDistances = (float[])tripInfo.get(13);
			assignedTripModes = (int[])tripInfo.get(14);
			tripVehIds = (int[])tripInfo.get(15);
			linkedToIds = (int[])tripInfo.get(16);
			jointDriverPnum = (int[])tripInfo.get(17);
			tripMinActDur = (double[])tripInfo.get(18);
			tripsHhAutoTripId = (int[])tripInfo.get(19);
			activityDuration = (float[])tripInfo.get(20);
		}
		

		
		int[][] personTripOrigActs = new int[ personTrips.length ][];
		int[][] personTripDestActs = new int[ personTrips.length ][];
		float[][] personTripDeparts = new float[ personTrips.length ][];
		float[][] personTripTravelTimes = new float[ personTrips.length ][];
		int[][] personTripModes = new int[ personTrips.length ][];
		int[][] personTripOrigMazs = new int[ personTrips.length ][];
		int[][] personTripDestMazs = new int[ personTrips.length ][];
		float[][] personTripValueOfTimes = new float[ personTrips.length ][];
		float[][] personTripDistances = new float[ personTrips.length ][];
		float[][] personActivityDuration = new float[personTrips.length][];
		int[][] personAssignedTripModes = new int[ personTrips.length ][];
		int[][] personTripVehIds = new int[ personTrips.length ][];
		int[][] personLinkedToIds = new int[ personTrips.length ][];
		int[][] personJointDriverPnum = new int[ personTrips.length ][];
		double[][] personTripMinActDur = new double[ personTrips.length ][];
		int[][] personTripRecNums= new int[ personTrips.length ][];
		int[][] personHhAutoTripId= new int[ personTrips.length ][];
		
		int[] tripIds = (int[])tripInfo.get(6);
		int[] tripPnums = (int[])tripInfo.get(4);

		int[] personCount = new int[personTrips.length];
		for ( int i=1; i < personTrips.length; i++ ) {
			personTripOrigActs[i] = new int[ personTrips[i]+1 ];
			personTripDestActs[i] = new int[ personTrips[i]+1 ];
			personTripDeparts[i] = new float[ personTrips[i]+1 ];
			personTripTravelTimes[i] = new float[ personTrips[i]+1 ];
			personTripModes[i] = new int[ personTrips[i]+1 ];
			personTripOrigMazs[i] = new int[ personTrips[i]+1 ];
			personTripDestMazs[i] = new int[ personTrips[i]+1 ];
			personTripValueOfTimes[i] = new float[ personTrips[i]+1 ];
			personTripDistances[i] = new float[ personTrips[i]+1 ];
			personAssignedTripModes[i] = new int[ personTrips[i]+1 ];
			personTripVehIds[i] = new int[ personTrips[i]+1 ];
			personLinkedToIds[i] = new int[ personTrips[i]+1 ];
			personJointDriverPnum[i] = new int[ personTrips[i]+1 ];
			personTripMinActDur[i] = new double[ personTrips[i]+1 ];
			personTripRecNums[i] = new int[ personTrips[i]+1 ];
			personHhAutoTripId[i] = new int[personTrips[i]+1];
			personActivityDuration[i] = new float[personTrips[i]+1];
		}

		for ( int i=1; i < tripIds.length; i++ ) {
			int index = chronologicalTripIndices[ tripIds[i]-1 ];
			int pnum = tripPnums[index];
			int k = personCount[pnum] + 1;
			personTripOrigActs[pnum][k] = tripOrigPurps[index];					
			personTripDestActs[pnum][k] = tripDestPurps[index];					
			personTripDeparts[pnum][k] = tripDeparts[index];
			personTripTravelTimes[pnum][k] = tripTravelTimes[index];
			personTripModes[pnum][k] = tripModes[index];
			personTripOrigMazs[pnum][k] = tripOrigMazs[index];					
			personTripDestMazs[pnum][k] = tripDestMazs[index];			
			personTripDistances[pnum][k] = tripDistances[index];
			personAssignedTripModes[pnum][k] = assignedTripModes[index];

			//personTripRecNums[pnum][k] = tripRecNums[index];
			personTripVehIds[pnum][k] = tripVehIds[index];
			personLinkedToIds[pnum][k] = linkedToIds[index];
			personJointDriverPnum[pnum][k] = jointDriverPnum[index];
			personTripMinActDur[pnum][k] = tripMinActDur[index];
			/*
			if(chronologicalAutoTripIndices != null && tripsHhAutoTripId[index]> 0)
				personHhAutoTripId[pnum][k] = chronologicalAutoTripIndices[tripsHhAutoTripId[index]-1]-1;
			else if(chronologicalAutoTripIndices != null)
				personHhAutoTripId[pnum][k] = -1;
			*/
			
			personHhAutoTripId[pnum][k] = Arrays.asList(chronologicalAutoTripIndices).indexOf(tripsHhAutoTripId[index]);
			personActivityDuration[pnum][k] = activityDuration[index];
			personCount[pnum]++;
		}		
		
		List<Object> resultList = new ArrayList<Object>();
		resultList.add( personTripOrigActs ); //0
		resultList.add( personTripDestActs ); //1
		resultList.add( personTripDeparts ); //2
 		resultList.add( personTripTravelTimes ); //3
 		resultList.add( personTripTravelTimes ); //4, simulated travel time same as planned travel time
		resultList.add( personTripModes ); //5
		resultList.add( personTripOrigMazs ); //6
		resultList.add( personTripDestMazs );//7
		resultList.add(personTripRecNums);//8
		resultList.add( personTripDistances ); //9
		resultList.add( personAssignedTripModes );//10
		resultList.add( personTripVehIds ); //11
		resultList.add( personLinkedToIds ); //12
		resultList.add( personJointDriverPnum ); //13
		resultList.add( personTripMinActDur ); //14
		resultList.add(personHhAutoTripId);//15
		resultList.add(personActivityDuration);
		return resultList;
		
	}
	
	private List<Object> getAutoTripOdArrays() {


		int[] tripPnums = null;
		int[] tripOrigMazs = null;
		int[] tripDestMazs = null;
		int[] tripOrigPurps = null;
		int[] tripDestPurps = null;
		float[] tripDeparts = null;
		float[] tripTravelTimes = null;
		float[] tripDistances = null;
		int[] autoTripsPersonTripId = null;
		
		
		if ( autoTripInfo.size() > 0 ) {
			tripPnums = (int[])autoTripInfo.get(0);
			tripDeparts = (float[])autoTripInfo.get(1);
			tripOrigMazs = (int[])autoTripInfo.get(2);
			tripDestMazs = (int[])autoTripInfo.get(3);
			autoTripsPersonTripId = (int[])autoTripInfo.get(4);			
			tripOrigPurps = (int[])autoTripInfo.get(6);
			tripDestPurps = (int[])autoTripInfo.get(8);
			tripDistances = (float[])autoTripInfo.get(10);
			tripTravelTimes = (float[])autoTripInfo.get(11);
			

		}	

		int[] hhTripPnum = new int[hhAutoTrips+1];
		int[] hhTripOrigActs = new int[ hhAutoTrips+1 ];
		int[] hhTripDestActs = new int[ hhAutoTrips +1];
		float[] hhTripDeparts = new float[ hhAutoTrips +1];
		float[] hhTripTravelTimes = new float[ hhAutoTrips +1];
		int[] hhTripOrigMazs = new int[ hhAutoTrips+1 ];
		int[] hhTripDestMazs = new int[ hhAutoTrips+1];
		int[] hhTripPersonTripId = new int[hhAutoTrips+1];
		float[] hhTripDistances = new float[ hhAutoTrips+1];


		int[] autoTripIds = (int[])autoTripInfo.get(9);
		int[] allTripIds = (int[])tripInfo.get(6);
		for ( int i=1; i < autoTripIds.length; i++ ) {
			int index = chronologicalAutoTripIndices[ autoTripIds[i]-1 ];
			hhTripPnum[i] = tripPnums[index];
			hhTripOrigActs[i] = tripOrigPurps[index];					
			hhTripDestActs[i] = tripDestPurps[index];					
			hhTripDeparts[i] = tripDeparts[index];
			hhTripTravelTimes[i] = tripTravelTimes[index];
			hhTripOrigMazs[i] = tripOrigMazs[index];
			hhTripDestMazs[i] = tripDestMazs[index];
			int indexAuto = autoTripsPersonTripId[index];
			//hhTripPersonTripId[i]=  chronologicalTripIndices[autoTripIds[i]-1]-1;
			hhTripPersonTripId[i]=  indexAuto-1;
			hhTripDistances[i] = tripDistances[index];
		}		
		
		List<Object> resultList = new ArrayList<Object>();
		resultList.add( hhTripPnum );
		resultList.add( hhTripOrigActs );
		resultList.add( hhTripDestActs );
		resultList.add( hhTripDeparts );
		resultList.add( hhTripTravelTimes );
		resultList.add( hhTripOrigMazs );
		resultList.add( hhTripDestMazs );
		resultList.add( hhTripPersonTripId );
		resultList.add( hhTripDistances );
		
		return resultList;
		
	}
	
	public int[] getPersonTripCount( int hhid ) {
		
		List<List<String>> personRecords = dataStore.getPersonRecords( hhid );
		int[] personTrips = new int[ personRecords.size()+1 ];
				
		List<Object> tripInfo = getAllTripInformation( hhid, null, null );
		if ( tripInfo.size() > 0 ) {
			int[] tripPnums = (int[])tripInfo.get(4);

			for ( int i=1; i < tripPnums.length; i++ ) {
				int pnum = tripPnums[i];
				personTrips[pnum]++;
			}
		}
		
		return personTrips;
		
	}
	

	
	public int[] getPersonTypeArray( int hhid ) {
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getPersonFieldIndexMap();
		int persRecordIndex = fieldIndexMap.get( persTypeField );
				
		List<List<String>> personRecords = dataStore.getPersonRecords( hhid );
		
		int[] personTypes = new int[ personRecords.size()+1 ];
		
		int pnum = 1;
		for ( List<String> record : personRecords ) {
			String persTypeFieldValue = record.get( persRecordIndex );
			int personTypeIndex = Integer.parseInt( persTypeFieldValue );
			personTypes[pnum++] = personTypeIndex; 
		}
		
		return personTypes;
		
	}

	public int[][] getUsualCarIdArray( int hhid ) {
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getPersonFieldIndexMap();
		int persRecordIndex = fieldIndexMap.get( usualCarIdField );
				
		List<List<String>> personRecords = dataStore.getPersonRecords( hhid );
		
		int[][] usualCars = new int[ personRecords.size()+1 ][];
		
		int pnum = 1;
		for ( List<String> record : personRecords ) {
			String usualCarIdFieldValue = record.get( persRecordIndex );
			int[] usualCarsForPerson = Parsing.getOneDimensionalIntArrayValuesFromExportString(usualCarIdFieldValue );
			usualCars[pnum++] = usualCarsForPerson; 
		}
		
		return usualCars;
		
	}
	
	public int getNumAutos( int hhid ) {
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getHhFieldIndexMap();
		int numAutosFieldIndex = fieldIndexMap.get(numAutosField);
				
		List<List<String>> hhecords = dataStore.getHouseholdRecords(hhid);
		int numAutos = 0;
	
		for ( List<String> record : hhecords ) {
			String autoValue = record.get( numAutosFieldIndex );
			numAutos= Integer.parseInt( autoValue );
		}
		
		return numAutos;
		
	}

	public int getIfAvHousehold( int hhid ) {
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getHhFieldIndexMap();
		int numAutosFieldIndex = fieldIndexMap.get(ifAvHhField);
				
		List<List<String>> hhecords = dataStore.getHouseholdRecords(hhid);
		int numAutos = 0;
	
		for ( List<String> record : hhecords ) {
			String autoValue = record.get( numAutosFieldIndex );
			numAutos= Integer.parseInt( autoValue );
		}
		
		return numAutos;
		
	}
	
	public int getNumberOfAutoTrips(){
		return numAutoTrips;
	}
	public int getHomeMaz( int hhid ) {
		
		// get a map of file field numbers to tripRecord field positions
		Map<String,Integer> fieldIndexMap = dataStore.getHhFieldIndexMap();
		int numAutosFieldIndex = fieldIndexMap.get(homeMazField);
				
		List<List<String>> hhecords = dataStore.getHouseholdRecords(hhid);
		int numAutos = 0;
	
		for ( List<String> record : hhecords ) {
			String autoValue = record.get( numAutosFieldIndex );
			numAutos= Integer.parseInt( autoValue );
		}
		
		return numAutos;
		
	}
	
	

		
	public int[][] getPersonTripIndices() {
		
		int[] tripIds = (int[])tripInfo.get(6);
		int[] tripPnums = (int[])tripInfo.get(4);

		int[] numPersons = new int[ personTrips.length ];
		int[][] personTripIndices = new int[ personTrips.length ][];
		
		// count persons
		for ( int i=1; i < tripPnums.length; i++ ) {
			int pnum = tripPnums[i];
			numPersons[pnum]++;
		}

		
		int oldPnum = -1;
		int offset = 1;
		for ( int i=1; i < tripIds.length; i++ ) {
			int pnum = tripPnums[i];
			if ( pnum != oldPnum ) {
				personTripIndices[pnum] = new int[numPersons[pnum]+1];
				offset = i;
				oldPnum = pnum;
			}
			personTripIndices[pnum][(i-offset)+1] = tripIds[i];
		}

		
		return personTripIndices;
		
	}

	
	public int[][] getPersonJointTripIndices() {
		
		int[] tripIds = (int[])tripInfo.get(7);
		int[] tripPnums = (int[])tripInfo.get(4);

		int[] numPersons = new int[ personTrips.length ];
		int[][] personTripIndices = new int[ personTrips.length ][];
		
		// count persons
		for ( int i=1; i < tripPnums.length; i++ ) {
			int pnum = tripPnums[i];
			numPersons[pnum]++;
		}

		
		int oldPnum = -1;
		int offset = 1;
		for ( int i=1; i < tripIds.length; i++ ) {
			int pnum = tripPnums[i];
			if ( pnum != oldPnum ) {
				personTripIndices[pnum] = new int[numPersons[pnum]+1];
				offset = i;
				oldPnum = pnum;
			}
			personTripIndices[pnum][(i-offset)+1] = tripIds[i];
		}

		
		return personTripIndices;
		
	}

	
	public int[][] getTripFileRecordIndices() {
		
		int[] tripIds = (int[])tripInfo.get(6);

		int[][] personTripRecordIndices = new int[ personTrips.length ][];
		
		int i = 1;
		for ( int pnum=1; pnum < personTrips.length; pnum++ ) {
			
			personTripRecordIndices[pnum] = new int[ personTrips[pnum]+1 ];
			
			for ( int j=0; j < personTrips[pnum]; j++ ) {
				int index = chronologicalTripIndices[ tripIds[i] - 1 ];
				personTripRecordIndices[pnum][j+1] = index;
				i++;
			}
		}
		
		return personTripRecordIndices;
		
	}
	

	
	public int[][] getTripOrigActivities() {
		return (int[][])odArrayObjects.get( 0 );
	}
	
	public int[][] getTripDestActivities() {
		return (int[][])odArrayObjects.get( 1 );
	}
	
	public float[][] getTripDeparts() {
		return (float[][])odArrayObjects.get( 2 );
	}
	
	public float[][] getTripTravelTimes() {
		return (float[][])odArrayObjects.get( 3 );
	}
	
	public float[][] getSimTravelTimes() {
		return (float[][])odArrayObjects.get( 4 );
	}
	
	public int[][] getTripModes() {
		return (int[][])odArrayObjects.get( 5 );
	}

	public int[][] getTripOrigMazs() {
		return (int[][])odArrayObjects.get( 6);
	}
	
	public int[][] getTripDestMazs() {
		return (int[][])odArrayObjects.get( 7 );
	}
	
	/*
	public float[][] getTripValueOfTimes() {
		return (float[][])odArrayObjects.get( 8 );
	}
	*/
	public int[][] getTripRecNums() {
		return (int[][])odArrayObjects.get(8 );
	}
	
	public float[][] getDistances() {
		return (float[][])odArrayObjects.get( 9 );
	}
	
	public int[][] getAssignedTripModes() {
		return (int[][])odArrayObjects.get( 10 );
	}
	
	public int[][] getTripVehIds() {
		return (int[][])odArrayObjects.get( 11 );
	}
	
	public int[][] getLinkedToIds() {
		return (int[][])odArrayObjects.get( 12 );
	}
	
	public int[][] getJointDriverPnums() {
		return (int[][])odArrayObjects.get( 13 );
	}
	
	public double[][] getTripMinActDur() {
		return (double[][])odArrayObjects.get(14);
	}
	

	
	public int getNumTripRecords() {
		return numTripRecords;
	}
	
	
	public int[][] getJointParticipants() {
		return (int[][])tripInfo.get(9);
	}
	
	public int[][] getTripHhAutoTripId() {
		return (int[][])odArrayObjects.get(15);
	}
	
	public float[][] getDurations() {
		return (float[][])odArrayObjects.get(16);
	}
	
	
	public int[] getAutoTripsPnums() {
		return (int[])odAutoArrayObjects.get(0);
	}
	
	public int[] getAutoTripsOrigActs() {
		return (int[])odAutoArrayObjects.get(1);
	}
	
	public int[] getAutoTripsDestActs() {
		return (int[])odAutoArrayObjects.get(2);
	}
	
	public float[] getAutoTripsDepart() {
		return (float[])odAutoArrayObjects.get(3);
	}
	
	public float[] getAutoTripsTravelTime() {
		return (float[])odAutoArrayObjects.get(4);
	}
	
	public int[] getAutoTripsOrigTaz() {
		return (int[])odAutoArrayObjects.get(5);
	}
	
	public int[] getAutoTripsDestTaz() {
		return (int[])odAutoArrayObjects.get(6);
	}
	public int[] getAutoTripsPersonTripId() {
		return (int[])odAutoArrayObjects.get(7);
	}
	public float[] getAutoTripsDistance() {
		return (float[])odAutoArrayObjects.get(8);
	}
	
	
	
	
	
	
}
