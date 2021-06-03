package fileProcessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import utility.BinarySearch;
import utility.Parsing;

public class VehicleTypeCategories_Copy {

	private static final String CATEGORY_FIELD_NAME = "category";
	private static final String BODY_TYPE_FIELD_NAME = "bodyType";
	private static final String FUEL_TYPE_FIELD_NAME = "fuelType";
	private static final String TRIP_MODE_FIELD_NAME = "mode";
	private static final String CAR_SIZE_FIELD_NAME = "size";
	private static final String DISUTIL1_FIELD_NAME = "disutil1";
	private static final String DISUTIL2_FIELD_NAME = "disutil2";
	private static final String DISUTIL3_FIELD_NAME = "disutil3";
	private static final String DISUTIL4_FIELD_NAME = "disutil4";
	private static final String VEH_TYPE_PROBS_FIELD_NAME = "vehTypeProb";
	private static final String TNC_VEH_TYPE_PROBS_FIELD_NAME = "tncTypeProb";
	
	public final int MAX_AUTOS_ALLOCATED = 4;

	private static VehicleTypeCategories_Copy INSTANCE;
	private final Map<Integer, List<String>> vehicleTypeDefsMap;
	private final List<String> tableFieldNames;
	private final Map<String,Integer> fieldNameIndices;

	private final double[] cumProbs;
	private final double[] cumTncProbs;
	private final Random rand = new Random(1234567);
	
	
	private VehicleTypeCategories_Copy(String filename) {		
		vehicleTypeDefsMap = Parsing.parseCsvFileFieldsToMap( filename );		
		tableFieldNames = vehicleTypeDefsMap.get(0);
		fieldNameIndices = new HashMap<>();
		for (int i=0; i < tableFieldNames.size(); i++)
			fieldNameIndices.put( tableFieldNames.get(i), i );
		
		int probIndex = fieldNameIndices.get(VEH_TYPE_PROBS_FIELD_NAME);
		cumProbs = new double[vehicleTypeDefsMap.values().size()-1];
		cumProbs[0] = Double.valueOf(vehicleTypeDefsMap.get(1).get(probIndex));
		for ( int id : vehicleTypeDefsMap.keySet() ) {
			if ( id > 1 ) {
				cumProbs[id-1] = cumProbs[id-2] + Double.valueOf(vehicleTypeDefsMap.get(id).get(probIndex));
			}
		}

		int tncProbIndex = fieldNameIndices.get(TNC_VEH_TYPE_PROBS_FIELD_NAME);
		cumTncProbs = new double[vehicleTypeDefsMap.values().size()-1];
		cumTncProbs[0] = Double.valueOf(vehicleTypeDefsMap.get(1).get(tncProbIndex));
		for ( int id : vehicleTypeDefsMap.keySet() ) {
			if ( id > 1 ) {
				cumTncProbs[id-1] = cumTncProbs[id-2] + Double.valueOf(vehicleTypeDefsMap.get(id).get(tncProbIndex));
			}
		}
	}
	
	public static VehicleTypeCategories_Copy createInstance(String filename) {
		if (INSTANCE == null) {
			INSTANCE = new VehicleTypeCategories_Copy(filename);
		}
		return INSTANCE;
	}
	
	public static VehicleTypeCategories_Copy getInstance() {
		return INSTANCE;
	}

	public int getCarType() {
		double rn = rand.nextDouble();
		int n = BinarySearch.binarySearchDouble(cumProbs, rn);
		return n+1;
	}
	
	public int getTncType() {
		double rn = rand.nextDouble();
		int n = BinarySearch.binarySearchDouble(cumTncProbs, rn);
		return n+1;
	}
	
	public String getCarSize( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(CAR_SIZE_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getCategory( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(CATEGORY_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getBodyType( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(BODY_TYPE_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getFuelType( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(FUEL_TYPE_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getTripMode( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(TRIP_MODE_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getDisutil1( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(DISUTIL1_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getDisutil2( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(DISUTIL2_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getDisutil3( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(DISUTIL3_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public String getDisutil4( int carTypeIndex ) {
		int fieldIndex = fieldNameIndices.get(DISUTIL4_FIELD_NAME);
		return vehicleTypeDefsMap.get(carTypeIndex).get(fieldIndex);
	}
	
	public List<Integer> getVehicleTypeIndices() {
		return new ArrayList<>( vehicleTypeDefsMap.keySet() );
	}
	
}
