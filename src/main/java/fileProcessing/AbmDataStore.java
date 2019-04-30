package fileProcessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;

import objectMapping.AbmObjectTranslater;


public class AbmDataStore {

	public static final int MINUTES_OFFSET_FOR_MINUTES_FROM_3_AM = 180;
	
//	public static final String HH_FILE 	 = "hh_0_99_hhs.csv";
//	public static final String PERS_FILE = "hh_0_99_persons.csv";
//	public static final String TRIP_FILE = "hh_0_99_trips.csv";
//	public static final String HH_FILE 	 = "output_disaggHouseholdList.csv";
//	public static final String PERS_FILE = "output_disaggPersonList.csv";
//	public static final String TRIP_FILE = "output_disaggTripList.csv";
//	public static final String HH_FILE 	 = "output_disaggHouseholdList_hh22.csv";
//	public static final String PERS_FILE = "output_disaggPersonList_hh22.csv";
//	public static final String TRIP_FILE = "output_disaggTripList_hh22.csv";
	
	private String hhIdLabel;
	private String hhFile;
	private String personFile;
	private String tripFile;

	
	private int[] hhIds;
	
	private List<String> hhFieldNames;
	private Map<Integer, List<List<String>>> hhRecords;
	
	private List<String> persFieldNames;
	private Map<Integer, List<List<String>>> persRecords;

	private List<String> tripFieldNames;
	private Map<Integer, List<List<String>>> tripRecords;
	private Map<Integer, List<List<String>>> autoTripRecords;

	private List<String> requiredHhField;
	private List<String> requiredPersonField;
	private List<String> requiredTripField;

	Map<String,Integer> hhFieldIndexMap = new HashMap<>();
	Map<Integer,String> hhIndexFieldMap = new HashMap<>();
	Map<String,Integer> personFieldIndexMap = new HashMap<>();
	Map<Integer,String> personIndexFieldMap = new HashMap<>();
	Map<String,Integer> tripFieldIndexMap = new HashMap<>();
	Map<Integer,String> tripIndexFieldMap = new HashMap<>();
	
	private final String inputFileFolder;

	private int minHhId;
	private int maxHhId;
	
	
	public AbmDataStore( Map <String, String> propertyMap ) {

		inputFileFolder = propertyMap.get( GlobalProperties.ABM_DATA_FOLDER_FILE_KEY.toString() );
		hhIdLabel = propertyMap.get( GlobalProperties.ABM_HHID_LABEL_ID_KEY.toString() );
		hhFile = propertyMap.get( GlobalProperties.ABM_HOUSEHOLD_DATA_FILE_KEY.toString() );
		personFile = propertyMap.get( GlobalProperties.ABM_PERSON_DATA_FILE_KEY.toString() );
		tripFile = propertyMap.get( GlobalProperties.ABM_TRIP_DATA_FILE_KEY.toString() );
		
		
		requiredHhField = Arrays.asList( 
				propertyMap.get( AbmObjectTranslater.HH_NUM_AUTO_FIELD_KEY ),
				propertyMap.get(AbmObjectTranslater.HH_MAZ_KEY),
				propertyMap.get(AbmObjectTranslater.HH_AV_FLAG_KEY),
				propertyMap.get(AbmObjectTranslater.HH_ID_WO_SAMPLE_FIELD_KEY));
		
		requiredPersonField = Arrays.asList( 
			propertyMap.get( AbmObjectTranslater.PERSON_TYPE_FIELD_KEY ),
			propertyMap.get( AbmObjectTranslater.PERSON_USUAL_CAR_ID_FIELD_KEY ));
			
		requiredTripField = Arrays.asList( 
			propertyMap.get( AbmObjectTranslater.TRIP_PNUM_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.TRIP_ORIG_MAZ_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.TRIP_DEST_MAZ_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.TRIP_MODE_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.TRIP_PLANNED_DISTANCE_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.TRIP_PARTY_FIELD_KEY ) ,
			propertyMap.get( AbmObjectTranslater.JOINT_TRIP_ID_FIELD_KEY  ),
			propertyMap.get( AbmObjectTranslater.TRIP_DEPART_MINUTE_FIELD_KEY  ),
			propertyMap.get( AbmObjectTranslater.TRIP_ARRIVE_MINUTE_FIELD_KEY  ),
			propertyMap.get( AbmObjectTranslater.TRIP_ID_FIELD_KEY ),
			propertyMap.get( AbmObjectTranslater.TRIP_ORIG_PURP_FIELD_KEY ),
			 propertyMap.get( AbmObjectTranslater.TRIP_DEST_PURP_FIELD_KEY  ),
			 propertyMap.get(AbmObjectTranslater.TRIP_ACTIVITY_DURATION_KEY),
			  propertyMap.get(AbmObjectTranslater.TRIP_VOT_FIELD_KEY)
		);
		 
		hhRecords = new HashMap<Integer, List<List<String>>>();
		persRecords = new HashMap<Integer, List<List<String>>>();
		tripRecords = new HashMap<Integer, List<List<String>>>();
		autoTripRecords = new HashMap<Integer, List<List<String>>>();
		hhFieldIndexMap = new HashMap<>();
		hhIndexFieldMap = new HashMap<>();
		
		int count = 0;
        for ( int i=0; i < requiredHhField.size(); i++ ) {
        	if ( requiredHhField.get(i) == null )
        		continue;
        	hhFieldIndexMap.put( requiredHhField.get(i), count );
        	hhIndexFieldMap.put( count++, requiredHhField.get(i) );
        }
		
		personFieldIndexMap = new HashMap<>();
		personIndexFieldMap = new HashMap<>();
		count = 0;
        for ( int i=0; i < requiredPersonField.size(); i++ ) {
        	if ( requiredPersonField.get(i) == null )
        		continue;
        	personFieldIndexMap.put( requiredPersonField.get(i), count );
        	personIndexFieldMap.put( count++, requiredPersonField.get(i) );
        }
		
		tripFieldIndexMap = new HashMap<>();
		tripIndexFieldMap = new HashMap<>();
		count = 0;
        for ( int i=0; i < requiredTripField.size(); i++ ) {
        	if ( requiredTripField.get(i) == null  )
        		continue;
        	tripFieldIndexMap.put( requiredTripField.get(i), count );
        	tripIndexFieldMap.put( count++, requiredTripField.get(i) );
        }
		

        
        
		List<List<String>> hhidStringValues = AbmDataReader.getValuesFromCsvFileForFieldNames( inputFileFolder+"/"+hhFile, Arrays.asList( new String[] {"hhid"} ) );		
    	
		List<Integer> tempList = new ArrayList<>();
		for ( List<String> temp : hhidStringValues )
			tempList.add( Integer.valueOf( temp.get(0) ) );
		hhIds = new int[tempList.size()];
		for ( int i=0; i < tempList.size(); i++ )
			hhIds[i] = tempList.get(i);
					
//            OLD_CSVFileReader reader = new OLD_CSVFileReader(); 
//            TableDataSet tds = reader.readFile( new File( inputFileFolder + "/" + hhFile ) );
//            hhIds = tds.getColumnAsInt( hhIdLabel );

		int[] minMax = getMinMaxHhid();
        minHhId = minMax[0];
        maxHhId = minMax[1];
        
	}

	private int[] getMinMaxHhid() {
		int min = 999999999;
		int max = -1;
		for ( int id : hhIds ) {
			if ( id < min )
				min = id;
			if ( id > min )
				max = id;
		}
		return new int[] { min, max };
	}
	
	public int[] getHhIdArray() {
		return hhIds;
	}
	
	
	public int[] getHhIdArrayInRange( int minRange, int maxRange ) {

		List<Integer> idList = new ArrayList<Integer>();
		
		for ( int i=0; i < hhIds.length; i++ ) {
			if ( hhIds[i] >= minRange && hhIds[i] < maxRange )
				idList.add( hhIds[i] );
		}
		
		int[] newHhIds = new int[idList.size()];
		for ( int i=0; i < idList.size(); i++ )
			newHhIds[i] = idList.get(i);
			
		return newHhIds;
	}
	
	
	public int getNumTripRecords( int hhid ) {
		int numTrips = tripRecords.get(hhid) == null ? 0 : tripRecords.get(hhid).size();
		return numTrips;
	}
	
	
	public void populateDataStore( int minRange, int maxRange ) {

		int[] newHhIds = getHhIdArrayInRange( minRange, maxRange );
		
		if ( requiredHhField.size() > 0 ) {
			hhFieldNames = AbmDataReader.getFieldNamesFromCsvFile( inputFileFolder + "/" + hhFile );
			Map<Integer,List<List<String>>> hhMap = AbmDataReader.getValuesFromCsvFileForHhIdsAndFields( inputFileFolder + "/" + hhFile, hhIdLabel, hhFieldIndexMap, minRange, maxRange, hhFieldNames );
			for ( int hhid : newHhIds )
				hhRecords.put( hhid, hhMap.get( hhid ) );
		}

		if ( requiredPersonField.size() > 0 ) {
			persFieldNames = AbmDataReader.getFieldNamesFromCsvFile( inputFileFolder + "/" + personFile );
			Map<Integer,List<List<String>>> persMap = AbmDataReader.getValuesFromCsvFileForHhIdsAndFields( inputFileFolder + "/" + personFile, hhIdLabel, personFieldIndexMap, minRange, maxRange, persFieldNames );
			for ( int hhid : newHhIds )
				persRecords.put( hhid, persMap.get( hhid ) );
		}

		if ( requiredTripField.size() > 0 ) {
			tripFieldNames = AbmDataReader.getFieldNamesFromCsvFile( inputFileFolder + "/" + tripFile );
			Map<Integer,List<List<String>>> indTripMap = AbmDataReader.getValuesFromCsvFileForHhIdsAndFields( inputFileFolder + "/" + tripFile, hhIdLabel, tripFieldIndexMap, minRange, maxRange, tripFieldNames );
			for ( int hhid : newHhIds ){
				tripRecords.put( hhid, indTripMap.get( hhid ) );
				if(indTripMap.get( hhid ) !=null){
					List<List<String>>autoTrips = getAutoTrips(indTripMap.get( hhid ) );
					autoTripRecords.put(hhid,  autoTrips);
				}
			}
		}
		
		
	}
	
	private List<List<String>>  getAutoTrips(List<List<String>> indiTrips){
		List<List<String>> result = new ArrayList<List<String>> ();
		for(List<String> record : indiTrips ){
			String modeValue = record.get( tripFieldIndexMap.get("mode") );
			int mode = Integer.parseInt( modeValue );
			if(mode == AbmObjectTranslater.SOV_MODE || mode == AbmObjectTranslater.HOV2_DR_MODE || mode == AbmObjectTranslater.HOV3_DR_MODE)
				result.add(record);
		}
		
		return result;
		
	}
	public List<List<String>> getHouseholdRecords( int hhid ) {
		return hhRecords.get(hhid);
	}
	
	public List<List<String>> getPersonRecords( int hhid ) {
		return persRecords.get(hhid);
	}
	
	public List<List<String>> getTripRecords( int hhid ) {
		return tripRecords.get(hhid);
	}
	public List<List<String>> getNumAutoTripRecords( int hhid ) {
		return autoTripRecords.get(hhid);
	}
	
	public Map<Integer, List<List<String>>> getTripRecordsMap() {
		return tripRecords;
	}
	
	
	private void logResults( List<String> fieldNames, List<List<String>> fileStrings ) {

		String header = "";
		for ( String name : fieldNames ) {
			if ( header.length() > 0 )
				header += ", ";
			
			header += name;
		}
		System.out.println( header );
		
		
		for ( List<String> values : fileStrings ) {

			String record = "";
			for ( String value : values ) {
				if ( record.length() > 0 )
					record += ", ";
				
				record += value;
			}
			System.out.println( record );
			
		}
		
	}
	
	
	private void logDatastoreRecords( int hhid ) {

		System.out.println( "\n" + inputFileFolder + "/" + hhFile );
		logResults( hhFieldNames, hhRecords.get(hhid) );
		
		System.out.println( "\n" + inputFileFolder + "/" + personFile );
		logResults( persFieldNames, persRecords.get(hhid) );

		System.out.println( "\n" + inputFileFolder + "/" + tripFile );
		logResults( tripFieldNames, tripRecords.get(hhid) );

	}
	
	
	public Map<String,Integer> getHhFieldIndexMap() {
		return hhFieldIndexMap;		
	}
	
	public Map<Integer,String> getHhIndexFieldMap() {
		return hhIndexFieldMap;		
	}
	
	public Map<String,Integer> getPersonFieldIndexMap() {
		return personFieldIndexMap;		
	}
	
	public Map<Integer,String> getPersonIndexFieldMap() {
		return personIndexFieldMap;		
	}
	
	public Map<String,Integer> getTripFieldIndexMap() {
		return tripFieldIndexMap;		
	}
	
	public Map<Integer,String> getTripIndexFieldMap() {
		return tripIndexFieldMap;		
	}

	public int getMinHhId() {
		return minHhId;
	}
	
	public int getMaxHhId() {
		return maxHhId;
	}
	
	
	public static void main( String[] args ) {

		int hhId = 22;
		ResourceBundle rb = null;
		if ( args.length == 1 ) {
			rb = ResourceBundle.getBundle( args[0] );		
		}
		else {
			System.out.println( "\ninvalid number of command line arguments - 1 expected." );
			System.out.println( "\t 1) String argument with the name of the properties file (without the .properties extension)." );
			System.exit(-1);
		}
		Map<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
		AbmDataStore myObject = new AbmDataStore( propertyMap );
		
		// read the csv files into datastore as List<List<String>> objects. 
		myObject.populateDataStore( hhId, hhId+1 );

		// log values in the Datastore to the console
		myObject.logDatastoreRecords( hhId );

	}
	
}
