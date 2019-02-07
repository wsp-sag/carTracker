package objects;

import java.io.Serializable;

public class AutoTrip implements Serializable {
	
	private static final long serialVersionUID = -7599335722527474930L;
	
	private final int id;
	private final int pnum;
	private final int hhTripId;
	private final int origAct;
	private final int destAct;
	private final int origMaz;
	private final int destMaz;
	private final float schedDepart;
	private final float schedTime;
	private final float distance;
	private final double minActivityDuartion;
	private final int mode;
	private final float vot;
	
	
	public AutoTrip( int id, int pnum, int indivId, int origAct, int destAct,
			int origMaz, int destMaz, float depTime,  float plannedTravelTime, 
			float distance, double minActivityDuartion, float vot, int mode ) {
		this.id = id;
		this.pnum = pnum;
		this.hhTripId = indivId;
		this.origAct = origAct;
		this.destAct = destAct;
		this.origMaz = origMaz;
		this.destMaz = destMaz;
		this.schedDepart = depTime;
		this.schedTime = plannedTravelTime;
		this.distance = distance;
		this.minActivityDuartion = minActivityDuartion;
		this.vot = vot;
		this.mode = mode;
	}


	public int getId() {
		return id;
	}
	
	public int getPnum() {
		return pnum;
	}

	
	
	public int getOrigAct() {
		return origAct;
	}
	
	public int getDestAct() {
		return destAct;
	}
	
	public int getOrigMaz() {
		return origMaz;
	}
	
	public int getDestMaz() {
		return destMaz;
	}
	
	
	public float getSchedDepart() {
		return schedDepart;
	}
	

	public float getSchedTime() {
		return schedTime;
	}
	
	
	public double getMinActivityDuartion() {
		return minActivityDuartion;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public int getHhTripId(){
		return hhTripId;
	}
	public int getMode(){
		return mode;
	}
	
	public float getValueOfTime(){
		return vot;
	}
}
