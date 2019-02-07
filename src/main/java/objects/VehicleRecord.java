package objects;

import java.io.Serializable;

public class VehicleRecord implements Serializable {

	private static final long serialVersionUID = 628477863592027723L;
	
	private static final int IOC = 3;
	private static final int INTDE = 1;
	private static final int INFO = 0;
	private static final String RIBF = "0.2000";
	private static final String COMP = "0.00";
	private static final int EVAC = 0;
	private static final int TFLAG = 0;
	private static final String PARRTIME = "0.0";
	private static final int TP = 0;
	private static final String INIGAS = "0.0";
	private static final String ACTDUR = "0.00";
	
	private final int hhId;
	private final int pnum;
	private final int uniqueTripId;

	private int onode;
	
	private final int vehId;
	private final int usec;
	private final int dsec;
	private float stime;
	private final int vehClass;
	private final int vehType;
	private final int tripMode;
	private final int origMaz;
	private final int destMaz;
	private final double initPos;
	private final float vot;
	
	
	public VehicleRecord( int vehId, int usec, int dsec, float stime, int vehClass, int vehType, int tripMode, int origMaz, int destMaz, double initPos, float vot, int hhid, int pnum, int tripid ) {
		this.vehId = vehId;
		this.usec = usec;
		this.dsec = dsec;
		this.stime = stime;
		this.vehClass = vehClass;
		this.vehType = vehType;
		this.tripMode = tripMode;
		this.origMaz = origMaz;
		this.destMaz = destMaz;
		this.initPos = initPos;
		this.vot = vot;
		
		this.hhId = hhid;
		this.pnum = pnum;
		this.uniqueTripId = tripid;
	}

	public static String[] getVehicleDatHeaderPair( int numVehs ) {
		String record1 = String.format( "%12d%12d%s", numVehs, 1, "    # of vehicles in the file, Max # of STOPs" );
		String record2 = String.format( "%s", "        #   usec   dsec   stime vehcls vehtype ioc #ONode #IntDe info ribf    comp   izone Evac InitPos    VoT  tFlag pArrTime TP IniGas" );
		return new String[]{ record1, record2 };
	}

	//#FORMAT(i9,2i7,f8.2,6i6,2f8.4,2i6,f12.8,f8.2,I5,f7.1,I5,f5.1)
	
	public String[] getVehicleDatRecordPair( float depTime ) {
		String record1 = String.format( "%9d%7d%7d%8.1f%6d%6d%6d%6d%6d%6d%8s%8s%6d%6d%12.8f%8.2f%5d%7s%5d%5s", vehId, usec, dsec, depTime, vehClass, vehType, IOC, onode, INTDE, INFO, RIBF, COMP, origMaz, EVAC, initPos, Math.max(0.1f, vot), TFLAG, PARRTIME, TP, INIGAS );
		String record2 = String.format( "%12d%7s", destMaz, ACTDUR );
		return new String[]{ record1, record2 };
	}

	
	public int getVehId() {
		return vehId;
	}
	
	public int getVehType() {
		return vehType;
	}
	
	public int getTripMode() {
		return tripMode;
	}
	
	public int getHhId() {
		return hhId;
	}
	
	public int getPnum() {
		return pnum;
	}
	
	public int getUniqueTripId() {
		return uniqueTripId;
	}
	
	public void setStartTime( float time ) {
		stime = time;
	}

	public float getStartTime() {
		return stime;
	}

	public void setNumPathNodes( int numNodes ) {
		onode = numNodes;
	}
	
	public static int compareByStimeThenVehId( VehicleRecord v1, VehicleRecord v2 ) {
		if ( ( getSortValue(v1) - getSortValue(v2) < 0.0 ) )
			return -1;
		else if ( ( getSortValue(v1) - getSortValue(v2) > 0.0 ) )
			return 1;
		else 
			return 0;
	}
	
	private static long getSortValue( VehicleRecord v ) {
		return ((long)( ((int)( v.getStartTime()*10 )) * 1.0e10 )) + v.getVehId();
	}

	
}
