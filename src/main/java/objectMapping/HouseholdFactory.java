package objectMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import appLayer.CarAllocatorMain;
import objects.AbmDataVehicleIdMap;
import objects.AutoTrip;
import objects.Household;
import objects.Person;
import objects.Trip;
import utility.Util;

public class HouseholdFactory {

	private static HouseholdFactory INSTANCE;

	private List<int[]> personTripList;
	private List<Trip> tripList;
	private List<AutoTrip> autoTripList;
	private HouseholdFactory() {
		personTripList = new ArrayList<int[]>();
		tripList = new ArrayList<Trip>();
		autoTripList = new ArrayList<AutoTrip>();
	}
	
	
	public static synchronized HouseholdFactory getInstance() {
		if ( INSTANCE == null )
			return new HouseholdFactory();
		else
			return INSTANCE;
	}
	
	
	public Household createHousehold( HashMap<String, String> propertyMap,int hhid, int hidAcrossSample, int[][] uniqueChronologicalIds, int numHhMembers, int[] personTypes, int[][] usualCarIds, int[] numTrips, int numHhJointTrips, int[] numParticipants, int[][] participants,
			int[] numJointTrips, int[][] jointTrips, int[][] orgActivityType, int[][] destActivityType, int[][] origMazs, int[][] destMazs, int[][] tripModes, int[] numCompletedTrips,
			float[][] tripPlannedDeparture, float[][] tripPlannedTime, float[][] tripExpectedTime, float[][] tripDistance, float[][] valueOfTime, int[][] tripRecNums, int[][] assignedTripModes,
			int[][] tripVehIds, int[][] linkedToIds, int[][] jointDriverPnums, Map<Long, Double> minActDurMap, int numAutos, int[][]tripHhAutoTripId , int[] autoTripPnum,
			int[] autoOrigPurp,int[] autoDestPurp,float[] autoTripDepart,float[] autoTripTravelTime,
			int[] autoTripOrigTaz,int[] autoTripDestTaz,int[] autoTripPersonTripId,float[] autoTripDistance, int homeMaz, int numAutoTrips, int ifAvHh, float[][] actvityDurations, int[] autoModes, float[] autoVot) {

		Map<Integer, int[][]> jointParties = new HashMap<Integer, int[][]>();
		int[][] uniqueTripIds = createUniqueTripIds( numHhMembers, numTrips );		

		for ( int m=1; m <= numHhMembers; m++ ) {
			
			for ( int i=1; i <= numTrips[m]; i++ ) {
			
				int[] linkedTripIds = getIndivTripIdsLinkedToJointTripId( m, jointTrips[m] == null ? 0 : jointTrips[m][i], numHhMembers, numTrips, jointTrips, uniqueTripIds );
				if ( linkedTripIds.length > 0 && !jointParties.containsKey( jointTrips[m][i] ) ) {
					int[][] party = getJointParty( m, uniqueTripIds[m][i], linkedTripIds );		
					jointParties.put( jointTrips[m][i], party );
				}
				
				double minActDur = Math.max(Float.parseFloat(propertyMap.get("min.activity.duration")),actvityDurations[m][i]);
				
				
				if ( minActDurMap != null ) {
					long key = hhid*AbmDataVehicleIdMap.HHID_MULTIPLIER + m*AbmDataVehicleIdMap.PNUM_MULTIPLIER + uniqueChronologicalIds[m][i];
                	minActDur = minActDurMap.get( key );
				}
				 
				
				Trip trip = new Trip(  uniqueTripIds[m][i], m, uniqueChronologicalIds[m][i], i, jointTrips[m] == null ? 0 : jointTrips[m][i], orgActivityType[m][i], 
						destActivityType[m][i], origMazs[m][i], destMazs[m][i], tripModes[m][i], linkedTripIds, tripPlannedDeparture[m][i], 
						tripPlannedDeparture[m][i] + tripPlannedTime[m][i], tripPlannedTime[m][i], tripExpectedTime[m][i], tripDistance[m][i], 
						valueOfTime[m][i], tripRecNums[m][i], assignedTripModes[m][i], tripVehIds[m][i], linkedToIds[m][i], jointDriverPnums[m][i], minActDur  );
				trip.setHhAutoTripId(tripHhAutoTripId[m][i]);
				tripList.add( trip );
				
			}
			
		}
		
		if(numAutoTrips>0){
			
			for ( int i=1; i < autoTripPnum.length; i++ ) {
				double minActDur = Float.parseFloat(propertyMap.get("min.activity.duration"));
				
				AutoTrip aTrip = new AutoTrip(i,autoTripPnum[i],autoTripPersonTripId[i],autoOrigPurp [i],autoDestPurp [i],autoTripOrigTaz [i],
						autoTripDestTaz [i],autoTripDepart [i],autoTripTravelTime [i],autoTripDistance[i],minActDur,autoVot[i], autoModes[i]);
				autoTripList.add(aTrip);
			}
		}
		Household hh = new Household( hhid, hidAcrossSample, personTypes,usualCarIds, tripList, jointParties, numAutos,autoTripList, homeMaz,numAutoTrips,ifAvHh );
		
		Person[] persons = hh.getPersons();
		
		for ( int[] personTrip : personTripList ) {
			int m = personTrip[0];
			int i = personTrip[1];
			persons[m].addTrip( i );
		}
		
		return hh;
		
	}
	
	
	private int[] getIndivTripIdsLinkedToJointTripId( int persNum, int jointTripId, int numHhMembers, int[] numTrips, int[][] jointTrips, int[][] uniqueTripIds ) {

		List<Integer> linkedTripIds = new ArrayList<Integer>();
		
		if ( jointTripId > 0 ) {
			
			for ( int m=1; m <= numHhMembers; m++ ) {
				
				if ( m == persNum )
					continue;
				
				for ( int i=1; i <= numTrips[m]; i++ ) {
					if ( jointTrips[m][i] == jointTripId )
						linkedTripIds.add( uniqueTripIds[m][i]+1 );
				}
				
			}
			
		}

		
		int[] returnArray = new int[linkedTripIds.size()];
		for ( int i=0; i < linkedTripIds.size(); i++ )
			returnArray[i] = linkedTripIds.get(i); 
		
		return returnArray;
		
	}
	
	
	private int[][] createUniqueTripIds( int numHhMembers, int[] numTrips ) {
		
		int[][] tripIds = new int[numHhMembers+1][];
			
		int tripId = 0;
		for ( int m=1; m <= numHhMembers; m++ ) {
		
			tripIds[m] = new int[numTrips[m]+1];
			
			for ( int i=1; i <= numTrips[m]; i++ ) {
				
				tripIds[m][i] = tripId;
				
				int[] persTrip = new int[2];
				persTrip[0] = m;
				persTrip[1] = tripId;
				personTripList.add( persTrip );
				
				tripId++;
			}
			
		}
		
		return tripIds;
		
	}
	
	
	private int[][] getJointParty( int pers, int trip, int[] linkedTripIds ) {
		
		int[][] party = new int[linkedTripIds.length+1][linkedTripIds.length+1];

		party[0][0] = pers;
		party[0][1] = trip;
		
		for ( int n=0; n < linkedTripIds.length; n++ ) {
			int m = personTripList.get( linkedTripIds[n]-1 )[0];
			int i = personTripList.get( linkedTripIds[n]-1 )[1];
			party[n+1][0] = m;
			party[n+1][1] = i;
		}
		
		return party;
		
	}
	
	
}
