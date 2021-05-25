package objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Household implements Serializable {

	private static final long serialVersionUID = 7574608842260501309L;

	private final int id;
	private final int hhsize;
	private final int numAutos;
	private final int homeMaz;
	private final int hidAcrossSample;
	private int numIndivTripRecords;
	private int numJointTripRecords;
	private int[][] personTripRecordIds;
	
	private final Person[] persons;
	
	private Map<Integer, int[][]> jointParties;
	private List<Trip> trips;
	private List<AutoTrip> autoTrips;
	private int numAutoTrips;
	private int ifAvHh;
	private int[] hhCarTypes;
	
	public Household( int hhid, int hidAcrossSample, int[] persTypes,int[][] usualCarIds, List<Trip> trips, Map<Integer, int[][]> jointParties, int numAutos, List<AutoTrip> autoTrips, int homeMaz, int numAutoTrips, int ifAvHh) {
		
		id = hhid;
		hhsize = persTypes.length - 1;
		persons = new Person[hhsize + 1];
		this.hidAcrossSample = hidAcrossSample;
		this.trips = trips;
		this.jointParties = jointParties;
		this.numAutos = numAutos;
		this.autoTrips = autoTrips;
		this.homeMaz = homeMaz;
		this.numAutoTrips = numAutoTrips;
		this.ifAvHh = ifAvHh;

		for ( int i=1; i < persTypes.length; i++ )
			persons[i] = new Person( i, persTypes[i],usualCarIds[i] );
		
			
	}
	
	public int getId() {
		return id;
	}
	
	public int getHidAcrossSample(){
		return hidAcrossSample;
	}
	public Person[] getPersons() {
		return persons;
	}
	
	public int getNumJointTrips() {
		return jointParties.size();
	}
	
	public Map<Integer, int[][]> getJointParties() {
		return jointParties;
	}
	
	public List<Trip> getTrips() {
		return trips;
	}
	
	public void sethhCarTypes( int[] hhCarTypes ) {
		this.hhCarTypes = hhCarTypes;
	}
	public void setNumIndivTripRecords( int trips ) {
		numIndivTripRecords = trips;
	}
	
	
	public int getNumIndivTripRecords() {
		return numIndivTripRecords;
	}
	

	public void setNumJointTripRecords( int trips ) {
		numJointTripRecords = trips;
	}
	
	
	public int getNumJointTripRecords() {
		return numJointTripRecords;
	}

	public void setPersonTripRecordIds( int[][] personTripRecordIds ) {
		this.personTripRecordIds = personTripRecordIds;
	}

	public int[][] getPersonTripRecordIds() {
		return personTripRecordIds;
	}
	
	public int getNumAutos(){
		return numAutos;
	}

	public List<AutoTrip> getAutoTrips(){
		return autoTrips;
	}
	
	public int getHomeMaz(){
		return homeMaz;
	}
	public int getNumAutoTrips(){
		return numAutoTrips;
	}
	public int getIfAvHousehold(){
		return ifAvHh;
	}
	public int[] getHhCarTypes() {
		return hhCarTypes;
	}
}
