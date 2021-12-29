package objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Person implements Serializable {

	private static final long serialVersionUID = 3270421995780495240L;
	
	private final int id;
	private final int personType;
	private List<Integer> tripIds;
	private List<Integer> autoDriverTripIds;
	private final int usualCarId;
	
	public Person( int id, int personType, int usualCarId ) {
		this.id = id;
		this.personType = personType;
		this.usualCarId= usualCarId;
		tripIds = new ArrayList<Integer>();
		autoDriverTripIds = new ArrayList<Integer>();
	}
	
	public void addTrip( int id ) {
		tripIds.add( id );
	}
	
	public void addAutoDriverTrip( int id ) {
		autoDriverTripIds.add( id );
	}
	
	
	public int getId() {
		return id;
	}
	
	public int getPersonType() {
		return personType;
	}
	
	public int getUsualCarId() {
		return usualCarId;
	}
	
	
	public List<Integer> getTripIds() {
		return tripIds;
	}
	
	public List<Integer> getAutoDriverTripIds() {
		return autoDriverTripIds;
	}
	
}
