package objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objectMapping.AbmObjectTranslater;

public class AbmDataVehicleIdMap implements Serializable {

	private static final long serialVersionUID = 3727062689580194048L;
	
	public static final int HHID_KEY_INDEX = 0;
	public static final int PNUM_KEY_INDEX = 1;
	public static final int TRIPID_KEY_INDEX = 2;
	
	public static final long HHID_MULTIPLIER = (long)Math.pow( 10, 7 );
	public static final long PNUM_MULTIPLIER = (long)Math.pow( 10, 4 );
	
	private Map<Long, Integer> abmKeyVehicleId;
	private Map<Integer, int[]> vehicleIdAbmKeys;
	
	
	
	public AbmDataVehicleIdMap( Map<String, String> propertyMap, Map<Integer, List<List<String>>> tripRecordsMap, Map<Integer,Integer> fieldIndexMap ) {
		
		abmKeyVehicleId = new HashMap<Long, Integer>();
		vehicleIdAbmKeys = new HashMap<Integer, int[]>();
		
		int tripPnumFieldIndex = Integer.valueOf( propertyMap.get( AbmObjectTranslater.TRIP_PNUM_FIELD_KEY ) );
		int tripIdFieldIndex = Integer.valueOf( propertyMap.get( AbmObjectTranslater.TRIP_ID_FIELD_KEY ) );
		
		int vehId = 1;
		for ( int hhid : tripRecordsMap.keySet() ) {
			
			List<List<String>> tripFileValues = tripRecordsMap.get(hhid);
			if ( tripFileValues == null )
				continue;
			
			for ( List<String> values : tripFileValues ) {
				
				int pnum = Integer.valueOf( values.get( fieldIndexMap.get(tripPnumFieldIndex) ) );
				int tripid = Integer.valueOf( values.get( fieldIndexMap.get(tripIdFieldIndex) ) );
				long abmIndex = getAbmDataKey( hhid, pnum, tripid );
				
				abmKeyVehicleId.put( abmIndex, vehId );
				vehicleIdAbmKeys.put( vehId, new int[] { hhid, pnum, tripid } );
				
				vehId++;
			}			
			
		}
		
	}
	
	public long getAbmDataKey( int hhid, int pnum, int tripid ) {
		long abmDataKey = (hhid * HHID_MULTIPLIER) + (pnum * PNUM_MULTIPLIER) + tripid;
		return abmDataKey;
	}

	public int getVehicleId ( int hhid, int pnum, int tripid ) {
		long abmIndex = getAbmDataKey( hhid, pnum, tripid );
		return abmKeyVehicleId.get( abmIndex );
	}

	public int[] getAbmTripKeyData ( int vehicleId ) {
		return vehicleIdAbmKeys.get( vehicleId );
	}

}
