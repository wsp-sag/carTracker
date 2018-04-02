package objectMapping;

import java.util.HashMap;

import objects.AbmDataVehicleIdMap;
import objects.Household;

@FunctionalInterface
public interface HhObjectMapperIf {
	public Household createHouseholdObjectFromFiles( HashMap<String, String> propertyMap,int hhid, AbmDataVehicleIdMap abmDataVehicleIdMap, boolean debugFlag );
}
