package objectMapping;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fileProcessing.AbmDataStore;
import objects.AbmDataVehicleIdMap;
import objects.Household;


public class HhObjectMapper implements HhObjectMapperIf {

	private AbmDataStore dataStore;
	private Map<Integer, Float> experiencedVehicleTravelTimesMap;
	private Map<Integer, Float> tripAdjDepMap;
	private Map<String, String> propertyMap;
	private Map<Long, Double> minActDurMap;	
	
	public HhObjectMapper( Map<String, String> propertyMap, AbmDataStore dataStore, Map<Integer, Float> experiencedVehicleTravelTimesMap, Map<Integer, Float> tripAdjDepMap, Map<Long, Double> minActDurMap ) {
		this.dataStore = dataStore;
		this.experiencedVehicleTravelTimesMap = experiencedVehicleTravelTimesMap;
		this.tripAdjDepMap = tripAdjDepMap;
		this.propertyMap = propertyMap;
		this.minActDurMap = minActDurMap;
	}
	
	
	@Override
	public Household createHouseholdObjectFromFiles(HashMap<String, String> propertyMap, int hhid, AbmDataVehicleIdMap abmDataVehicleIdMap, boolean debugging ) {
		
		AbmObjectTranslater abmData = new AbmObjectTranslater( propertyMap, dataStore, experiencedVehicleTravelTimesMap, tripAdjDepMap, abmDataVehicleIdMap, hhid, debugging );
		

    	int[] personTypes = abmData.getPersonTypeArray( hhid );
    	int[] usualCarIds = abmData.getUsualCarIdArray( hhid );
    	int numHhMembers = personTypes.length - 1;
    	int numAutos = abmData.getNumAutos(hhid);
    	int homeMaz = abmData.getHomeMaz(hhid);
    	int ifAvHh= abmData.getIfAvHousehold(hhid);
    	int[] numTrips = abmData.getPersonTripCount( hhid );
    	
    	int[][] participants = abmData.getJointParticipants();
    	int numHhJointTrips = participants.length - 1;

    	int[] numParticipants = new int[ numHhJointTrips+1 ];
    	for ( int i=1; i < participants.length; i++ )
    		numParticipants[i] = participants[i].length - 1;
    	
    	int[] numJointTrips = new int[ numHhMembers+1 ];
    	int[][] jointTrips = abmData.getPersonJointTripIndices();
    	for ( int i=1; i < jointTrips.length; i++ ) {
   			if ( jointTrips[i] != null ) {
   	    		for ( int j=1; j < jointTrips[i].length; j++ ) {
        			if ( jointTrips[i][j] > 0 )
        				numJointTrips[i]++;
    			}
    		}
    	}
    	
    	
    	
    	int[][] orgActivityType = abmData.getTripOrigActivities();
    	int[][] destActivityType = abmData.getTripDestActivities();
    	
    	int[][] origMazs = abmData.getTripOrigMazs();
    	int[][] destMazs = abmData.getTripDestMazs();
    	
    	float[][] tripPlannedDeparture = abmData.getTripDeparts();
    	float[][] tripPlannedTime = abmData.getTripTravelTimes();
    	
    	int[][] tripModes = abmData.getTripModes();
    	int[][] tripRecNums = abmData.getTripRecNums();
    	int[][] tripVehIds = abmData.getTripVehIds();
    	int[][] assignedTripModes = abmData.getAssignedTripModes();
    	int[][] linkedToIds = abmData.getLinkedToIds();
    	int[][] jointDriverPnums = abmData.getJointDriverPnums();
    	
    	float[][] valueOfTime = null;
    	float[][] tripDistance = abmData.getDistances();
    	float[][] actvityDurations = abmData.getDurations();
    	int[] numCompletedTrips = new int[ numHhMembers+1 ];
    	
    	
    	int[][] uniqueChronologicalIds = abmData.getPersonTripIndices();
    	int[][] personTripRecordIds = abmData.getTripFileRecordIndices();
    	
    	int[][] tripHhAutoTripId = abmData.getTripHhAutoTripId();
    	
    	int[] autoTripPnum  = null;
    	int[] autoOrigPurp =null;
    	int[] autoDestPurp = null;
    	float[] autoTripDepart = null;
    	float[] autoTripTravelTime =null;
    	int[] autoTripOrigTaz = null;
    	int[] autoTripDestTaz = null;
    	int[] autoTripPersonTripId = null;
    	float[] autoTripDistance = null;
	
	
    	if(abmData.getNumberOfAutoTrips()>0){
	    	autoTripPnum = abmData.getAutoTripsPnums();
	    	autoOrigPurp = abmData.getAutoTripsOrigActs();
	    	autoDestPurp = abmData.getAutoTripsDestActs();
	    	autoTripDepart = abmData.getAutoTripsDepart();
	    	autoTripTravelTime = abmData.getAutoTripsTravelTime();
	    	autoTripOrigTaz = abmData.getAutoTripsOrigTaz();
	    	autoTripDestTaz = abmData.getAutoTripsDestTaz();
	    	autoTripPersonTripId = abmData.getAutoTripsPersonTripId();
	    	autoTripDistance = abmData.getAutoTripsDistance();
    	}
    	
    	Household hh = HouseholdFactory.getInstance().createHousehold(propertyMap, hhid, uniqueChronologicalIds, numHhMembers, personTypes, usualCarIds,numTrips, numHhJointTrips, numParticipants, participants,
        		numJointTrips, jointTrips, orgActivityType, destActivityType, origMazs, destMazs, tripModes, numCompletedTrips, tripPlannedDeparture, tripPlannedTime, tripPlannedTime,
        		tripDistance, valueOfTime, tripRecNums, assignedTripModes, tripVehIds, linkedToIds, jointDriverPnums, minActDurMap, numAutos,
        		tripHhAutoTripId,autoTripPnum,autoOrigPurp,autoDestPurp,autoTripDepart,autoTripTravelTime,autoTripOrigTaz,autoTripDestTaz,
        		autoTripPersonTripId,autoTripDistance, homeMaz,abmData.getNumberOfAutoTrips(),ifAvHh, actvityDurations);
        	
    	hh.setNumIndivTripRecords( abmData.getNumTripRecords() );
    	hh.setNumJointTripRecords( numHhJointTrips );
    	hh.setPersonTripRecordIds( personTripRecordIds );
    	
		return hh;		

	}

}
