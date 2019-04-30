package objects;

import java.io.Serializable;

public class Trip implements Serializable {
	
	private static final long serialVersionUID = -7599335722527474930L;
	
	private final int id;
	private final int pnum;
	private final int uniqueTripId;
	private final int indivTripId;
	private final int jointTripId;
	private final int origAct;
	private final int destAct;
	private final int origMaz;
	private final int destMaz;
	private final int mode;
	private final int[] linkedTripIds;
	private final float schedDepart;
	private final float schedArrive;
	private final float schedTime;
	private final float simulatedTime;
	private final float distance;
	private final float valueOfTime;
	private final int tripRecNum;
	private final int assignedMode;
	private int tripVehId;
	private final int linkedToId;
	private final int jointDriverPnum;
	private final double minActivityDuartion;
	private int hhAutoTripId;
	
	public Trip( int id, int pnum, int uniqueId, int indivId, int jointId, int origAct, int destAct, int origMaz, int destMaz, int mode, int[] linkedTripIds, float depTime, float arrTime, float plannedTravelTime, float simulatedTravelTime, float distance, float vot, int tripRecNum, 
			int assignedMode, int tripVehId, int linkedToId, int jointDriverPnum, double minActivityDuartion ) {
		this.id = id;
		this.pnum = pnum;
		this.uniqueTripId = uniqueId;
		this.indivTripId = indivId;
		this.jointTripId = jointId;
		this.origAct = origAct;
		this.destAct = destAct;
		this.origMaz = origMaz;
		this.destMaz = destMaz;
		this.mode = mode;
		this.linkedTripIds = linkedTripIds;
		this.schedDepart = depTime;
		this.schedArrive = arrTime;
		this.schedTime = plannedTravelTime;
		this.simulatedTime = simulatedTravelTime;
		this.distance = distance;
		this.valueOfTime = vot;
		this.tripRecNum = tripRecNum;
		this.assignedMode = assignedMode;
		this.tripVehId = tripVehId;
		this.linkedToId = linkedToId;
		this.jointDriverPnum = jointDriverPnum;
		this.minActivityDuartion = minActivityDuartion;
	}


	public int getId() {
		return id;
	}
	
	public int getPnum() {
		return pnum;
	}
	
	public int getIndivTripId() {
		return indivTripId;
	}
	
	public int getUniqueTripId() {
		return uniqueTripId;
	}
	
	public int getJointTripId() {
		return jointTripId;
	}
	
	public int[] getLinkedTripIds() {
		return linkedTripIds;
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
	
	public int getMode() {
		return mode;
	}
	
	public float getSchedDepart() {
		return schedDepart;
	}
	
	public float getSchedArrive() {
		return schedArrive;
	}
	
	public float getSchedTime() {
		return schedTime;
	}
	
	public float getSimulatedTime() {
		return simulatedTime;
	}
	
	public double getMinActivityDuartion() {
		return minActivityDuartion;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public float getValueOfTime() {
		return valueOfTime;
	}
	
	public int getVehId() {
		return tripVehId;
	}
	
	public int getTripRecNum() {
		return tripRecNum;
	}

	public int getAssignedMode() {
		return assignedMode;
	}

	public int getLinkedToId() {
		return linkedToId;
	}
	
	public int getJointDriverPnum() {
		return jointDriverPnum;
	}
	
	public void setHhAutoTripId(int id){
		this.hhAutoTripId = id;
	}
	
	
	public int getHhAutoTripId(){
		return hhAutoTripId;
	}
	
	public void setAllocatedAutoId(int autoId){
		tripVehId = autoId;
	}

}
