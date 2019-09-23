package accessibility;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;


public class GeographyManager implements Serializable
{

	private static final long versionNumber = 1L;
	private static final long serialVersionUID = versionNumber;


    //Commented, we do not have MAG and PAG anymore. GV 02/02/2015
	public static final GeographyManager INSTANCE = new GeographyManager();

	private static final String POPULATION_DATA_FILE_PATH_KEY 					= "socec.data.file.name";

	// geography correspondence file structure
	public static final String GEOGRAPHY_DATABASE_TABLE_PROPERTY_KEY 			= "geog.manager.mazControls.file";
	public static final String GEOGRAPHY_DATABASE_MAZ_FIELD_NAME_PROPERTY_KEY 	= "geog.manager.mazFieldName";
	public static final String GEOGRAPHY_DATABASE_TAZ_FIELD_NAME_PROPERTY_KEY 	= "geog.manager.tazFieldName";
	public static final String GEOGRAPHY_DATABASE_PUMA_FIELD_NAME_PROPERTY_KEY 	= "geog.manager.pumaFieldName";
	public static final String GEOGRAPHY_DATABASE_META_FIELD_NAME_PROPERTY_KEY 	= "geog.manager.metaFieldName";


	// school district correspondence file structure
	public static final String SCHOOL_DISTRICT_FILE_NAME_KEY 				= "geog.manager.school.district.file";
	public static final String SCHOOL_DISTRICT_DATABASE_TABLE_MAZ_FIELD_KEY = "geog.manager.school.district.maz.fieldname";
	public static final String SCHOOL_DISTRICT_DATABASE_TABLE_ES_ID_FIELD_KEY = "geog.manager.school.district.es.id.fieldname";
	public static final String SCHOOL_DISTRICT_DATABASE_TABLE_MS_ID_FIELD_KEY = "geog.manager.school.district.ms.id.fieldname";
	public static final String SCHOOL_DISTRICT_DATABASE_TABLE_HS_ID_FIELD_KEY = "geog.manager.school.district.hs.id.fieldname";
	//public static final String SCHOOL_DISTRICT_DATABASE_TABLE_ES_ATTENDANCE_ID_FIELD_KEY = "geog.manager.school.district.es.attend.id.fieldname";
	//public static final String SCHOOL_DISTRICT_DATABASE_TABLE_MS_ATTENDANCE_ID_FIELD_KEY = "geog.manager.school.district.ms.attend.id.fieldname";
	//public static final String SCHOOL_DISTRICT_DATABASE_TABLE_HS_ATTENDANCE_ID_FIELD_KEY = "geog.manager.school.district.hs.attend.id.fieldname";

	//External station list
	public static final String GEOGRAPHY_MANAGER_EXTERNAL_STATION_FILE = "external.stations.filepath";
	public static final String GEOGRAPHY_MANAGER_EXTERNAL_STATION_FIELD = "external.stations.fieldName";


	private int[] mazElemSchoolDistricts;
	private int[] mazMidSchoolDistricts;
	private int[] mazHighSchoolDistricts;
	private int[] mazElemSchoolAttendanceDistricts;
	private int[] mazMidSchoolAttendanceDistricts;
	private int[] mazHighSchoolAttendanceDistricts;
	private HashMap<Integer,TreeSet<Integer>> elemSchoolDistrictMazsMap;
	private HashMap<Integer,TreeSet<Integer>> midSchoolDistrictMazsMap;
	private HashMap<Integer,TreeSet<Integer>> highSchoolDistrictMazsMap;
	private HashMap<Integer,TreeSet<Integer>> elemSchoolAttendanceDistrictMazsMap;
	private HashMap<Integer,TreeSet<Integer>> midSchoolAttendanceDistrictMazsMap;
	private HashMap<Integer,TreeSet<Integer>> highSchoolAttendanceDistrictMazsMap;

    private int maxMaz = 0;
    private int maxTaz = 0;
    private int maxMeta = 0;
    private int maxPuma = 0;
    private int maxSchReg = 0;
    private int maxRegion = 0;
    private int maxExternalStation = 0;
    // given an MAZ, get the associated TAZ, Meta zone, or puma
    private int[] mazTazValues;
    private int[] mazMetaValues;
    private int[] mazPumaValues;
    private int[] mazSchRegValues;

    // unique values of MAZ, TAZ, Meta zone, and pumas
    private int[] mazValues;
    private int[] tazValues;
    private int[] metaValues;
    private int[] pumaValues;
    private int[] schoolRegionValues;
    private int[] externalStationsValues;

    // given a value, e.g. TAZ, get the index value into the tazValues array.  tazValues[tazIndex[TAZ]] = TAZ
    private int[] mazIndex;
    private int[] tazIndex;
    private int[] metaIndex;
    private int[] pumaIndex;
    private int[] schRegIndex;

    private int[] tazMetaCorresp;
    private int[] pumaMetaCorresp;

    private int[] tazMazList;

    private List<Integer>[] tazMazsList;
    private List<Integer>[] pumaTazsList;
    private List<Integer>[] metaTazsList;
    private List<Integer>[] schoolRegionTazsList;

    private ArrayList<Integer> campusTazList;
    private boolean isInitialized = false;
    private boolean ifExternalStationsIncluded = false;

    private int[] mazRegionCorrespTable;
    private GeographyManager() {
    }


	public static GeographyManager getInstance() {
		return INSTANCE;
	}



    public void setupGeographyManager( ResourceBundle rb ) {

    	String inputFilePath = rb.getString(POPULATION_DATA_FILE_PATH_KEY);

    	String mazControlsTableName = rb.getString( GEOGRAPHY_DATABASE_TABLE_PROPERTY_KEY );
    	String mazFieldName = rb.getString( GEOGRAPHY_DATABASE_MAZ_FIELD_NAME_PROPERTY_KEY );
    	String tazFieldName = rb.getString( GEOGRAPHY_DATABASE_TAZ_FIELD_NAME_PROPERTY_KEY );
    	String pumaFieldName = rb.getString( GEOGRAPHY_DATABASE_PUMA_FIELD_NAME_PROPERTY_KEY );
    	String metaFieldName = rb.getString( GEOGRAPHY_DATABASE_META_FIELD_NAME_PROPERTY_KEY );


    		
    		
    	createGeogCorrespondence( mazControlsTableName,
								  metaFieldName,
								  pumaFieldName,
								  tazFieldName,
								  mazFieldName );


		ifExternalStationsIncluded = Boolean.valueOf(rb.getString("include.external.stations"));
		if(ifExternalStationsIncluded)
    	{
    		String externalSationFileName = rb.getString( GEOGRAPHY_MANAGER_EXTERNAL_STATION_FILE );
        	String stationsFieldName = rb.getString( GEOGRAPHY_MANAGER_EXTERNAL_STATION_FIELD );
        	createExternalStationList( externalSationFileName,
					  stationsFieldName);
    	}
    }

    private TableDataSet getTableData( String fileName ) {

    	TableDataSet table;
    	Logger logger = Logger.getLogger( GeographyManager.class );
       try{
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet( "," + reader.getDelimSet() );
            table = reader.readFile(new File( fileName ));
        }
        catch(Exception e){
            logger.fatal( String.format( "Exception occurred reading geography data file: %s into TableDataSet object.", fileName ) );
            throw new RuntimeException(e);
        }

        return table;
    }

    public synchronized void createExternalStationList( String geoFileName,
    		String stationsFieldName ){


    	// get the values for the external
         TableDataSet geoXwalkTable = getTableData( geoFileName );
         int numValues = geoXwalkTable.getRowCount();	// number of rows in the data table

         externalStationsValues = new int[numValues];
         for ( int i=0; i<numValues; i++ ) {

        	 externalStationsValues[i]= (int) geoXwalkTable.getValueAt(i+1, stationsFieldName);
         }

         ArrayList<Integer> externalValuesList = new ArrayList<Integer>();

         for ( int i=0; i < externalStationsValues.length; i++ ) {

         	int externalStation = externalStationsValues[i];

         	if ( externalStation > maxExternalStation )
         		maxExternalStation = externalStation;

         	if ( ! externalValuesList.contains( externalStation ) )
         		externalValuesList.add( externalStation );

         }

         // create array of unique taz values
         externalStationsValues = new int[externalValuesList.size()];
         for ( int i=0; i < externalStationsValues.length; i++ )
        	 externalStationsValues[i] = externalValuesList.get( i );


    }

  
    public synchronized void createGeogCorrespondence( String geoFileName,
    		String metaFieldName, String pumaFieldName, String tazFieldName, String mazFieldName ) {

        if ( isInitialized )
        	return;

    	// get the values for the MAZ, TAZ, and META fields
        TableDataSet geoXwalkTable = getTableData( geoFileName );
        int numValues = geoXwalkTable.getRowCount();	// number of rows in the data table

        // allocate separate arrays to hold the geographic field values
        mazValues = new int[numValues];
        mazTazValues = new int[numValues];
        mazMetaValues = new int[numValues];
        mazPumaValues = new int[numValues];

        // get the maximum MAZ, TAZ, META, and PUMA geography values - used for dimensioning indexing arrays on geographic fields
        for ( int i=0; i<numValues; i++ ) {

        	mazValues[i]= (int) geoXwalkTable.getValueAt(i+1, mazFieldName);
        	mazTazValues[i] = (int) geoXwalkTable.getValueAt(i+1, tazFieldName);
        	mazMetaValues[i] = (int) geoXwalkTable.getValueAt(i+1, metaFieldName);
        	mazPumaValues[i] = (int) geoXwalkTable.getValueAt(i+1, pumaFieldName);
        }


        ArrayList<Integer> tazValuesList = new ArrayList<Integer>();
        ArrayList<Integer> metaValuesList = new ArrayList<Integer>();
        ArrayList<Integer> pumaValuesList = new ArrayList<Integer>();


        // get the maximum MAZ, TAZ, META, and PUMA geography values - used for dimensioning indexing arrays on geographic fields
        for ( int i=0; i < mazValues.length; i++ ) {

        	int maz = mazValues[i];
        	int taz = mazTazValues[i];
        	int meta = mazMetaValues[i];
        	int puma = mazPumaValues[i];


        	if ( maz > maxMaz )
        		maxMaz = maz;
        	if ( taz > maxTaz )
        		maxTaz = taz;
        	if ( meta > maxMeta )
        		maxMeta = meta;
        	if ( puma > maxPuma )
        		maxPuma = puma;

        	if ( ! tazValuesList.contains( taz ) )
        		tazValuesList.add( taz );

        	if ( ! metaValuesList.contains( meta ) )
        		metaValuesList.add( meta );

        	if ( ! pumaValuesList.contains( puma ) )
        		pumaValuesList.add( puma );

        }


        // create array of unique taz values
        tazValues = new int[tazValuesList.size()];
        for ( int i=0; i < tazValues.length; i++ )
      		tazValues[i] = tazValuesList.get( i );

        // create array of unique meta zone values
        metaValues = new int[metaValuesList.size()];
        for ( int i=0; i < metaValues.length; i++ )
        	metaValues[i] = metaValuesList.get( i );

        // create array of unique puma values
        pumaValues = new int[pumaValuesList.size()];
        for ( int i=0; i < pumaValues.length; i++ )
        	pumaValues[i] = pumaValuesList.get( i );


        // Allocate space for the mazIndex array. This array holds the 0-based array index for the mazValues array.
        // This relationship holds:  MAZ == mazValues[mazIndex[MAZ]].
        mazIndex = new int[maxMaz+1];
        Arrays.fill( mazIndex, -1 );

        // Allocate space for the tazIndex array. This array holds the 0-based array index for the tazValues array.
        // This relationship holds:  TAZ == tazValues[tazIndex[TAZ]].
        tazIndex = new int[maxTaz+1];
        Arrays.fill( tazIndex, -1 );

        // Allocate space for the metaIndex array. This array holds the 0-based array index for the metaValues array.
        // This relationship holds:  META == metaValues[metaIndex[META]].
        metaIndex = new int[maxMeta+1];
        Arrays.fill( metaIndex, -1 );

        // Allocate space for the tazIndex array. This array holds the 0-based array index for the tazValues array.
        // This relationship holds:  PUMA == pumaValues[pumaIndex[PUMA]].
        pumaIndex = new int[maxPuma+1];
        Arrays.fill( pumaIndex, -1 );



        // Allocate space for the TAZ to META correspondence array. This array holds the META geog value in which the TAZ is located.
        tazMetaCorresp = new int[maxTaz+1];
        Arrays.fill( tazMetaCorresp, -1 );

        pumaMetaCorresp = new int[maxPuma+1];
        Arrays.fill( pumaMetaCorresp, -1 );




        // Allocate space for an ArrayLists of MAZs for each TAZ.
        tazMazsList = new ArrayList[maxTaz+1];

        // Allocate space for an ArrayList of Meta zones for each PUMA geog.
        pumaTazsList = new ArrayList[maxPuma+1];

        // Allocate space for an ArrayList of TAZs for each Meta geog.
        metaTazsList = new ArrayList[maxMeta+1];



        // populate the correspondence values
        for ( int i=0; i < mazValues.length; i++ ) {

        	int maz = mazValues[i];
        	int taz = mazTazValues[i];
        	int meta = mazMetaValues[i];
        	int puma = mazPumaValues[i];

        	mazIndex[maz] = i;

        	if ( tazIndex[taz] < 0 ) {
        		for ( int k=0; k < tazValues.length; k++ ) {
        			if ( taz == tazValues[k] ) {
        				tazIndex[taz] = k;
        				break;
        			}
        		}
        	}

        	if ( metaIndex[meta] < 0 ) {
        		for ( int k=0; k < metaValues.length; k++ ) {
        			if ( meta == metaValues[k] ) {
        				metaIndex[meta] = k;
        				break;
        			}
        		}
        	}

        	if ( pumaIndex[puma] < 0 ) {
        		for ( int k=0; k < pumaValues.length; k++ ) {
        			if ( puma == pumaValues[k] ) {
        				pumaIndex[puma] = k;
        				break;
        			}
        		}
        	}


            tazMetaCorresp[taz] = meta;
            pumaMetaCorresp[puma] = meta;


        	if ( tazMazsList[taz] == null ) {
        		tazMazsList[taz] = new ArrayList<Integer>();
        	}
    		tazMazsList[taz].add( maz );


        	if ( pumaTazsList[puma] == null ) {
        		pumaTazsList[puma] = new ArrayList<Integer>();
        	}
        	if ( ! pumaTazsList[puma].contains(taz) )
        		pumaTazsList[puma].add( taz );


        	if ( metaTazsList[meta] == null ) {
        		metaTazsList[meta] = new ArrayList<Integer>();
        	}
        	if ( ! metaTazsList[meta].contains(taz) )
        		metaTazsList[meta].add( taz );
        }


        isInitialized = true;

    }

   

    public boolean getIsInitialized() {
    	return isInitialized;
    }


    public int[] getMazIndices() {
    	return mazIndex;
    }

    public int[] getTazIndices() {
    	return tazIndex;
    }

    public int[] getPumaIndices() {
    	return pumaIndex;
    }

    public int[] getMetaIndices() {
    	return metaIndex;
    }

    public int[] getMazValues() {
    	return mazValues;
    }

    public int[] getTazValues() {
    	return tazValues;
    }

    public int[] getTazValuesInSchoolRegion(int reg) {
    	List<Integer> tazsForSchoolRegion = schoolRegionTazsList[reg];

    	int[] result =  new int[tazsForSchoolRegion.size()];

    	for (int i = 0; i<tazsForSchoolRegion.size(); i++)
    		result[i] = tazsForSchoolRegion.get(i);

    	return result;
    }

    public int[] getMetaValues() {
    	return metaValues;
    }

    public int[] getPumaValues() {
    	return pumaValues;
    }

    public List<Integer>[] getTazMazsList() {
    	return tazMazsList;
    }

    public List<Integer>[] getPumaTazsList() {
    	return pumaTazsList;
    }

    /*
     * Given the TAZ ID, returns a list of corresponding MAZ IDs
     */
    public List<Integer> getMazsInTaz( int taz ) {
    	return tazMazsList[taz];
    }

    /*
     * Given an array of TAZ IDs, returns a list of all corresponding MAZ IDs
     */
	public int[] getMazsInTazs(int[] tazs) {
		List<Integer> mazList = new ArrayList<Integer>();

		for (int taz: tazs) {
			mazList.addAll(tazMazsList[taz]);
		}

		// convert array list of Integer to array of int
		int[] mazArray = new int[mazList.size()];
		for (int i = 0; i < mazList.size(); i++)
			mazArray[i] = mazList.get(i);

		return mazArray;
	}

    public int[] getPumaMetaCorresp() {
    	return pumaMetaCorresp;
    }

    public int[] getMazMetaValues() {
    	return mazMetaValues;
    }

    public int[] getExternalStations() {
    	return externalStationsValues;
    }

    public int[] getMazPumaValues() {
    	return mazPumaValues;
    }

	public int getMazTazValue( int maz ) {
		int retValue = -1;

		if(ifExternalStationsIncluded &&  ArrayUtils.contains(externalStationsValues,maz))
			retValue =  maz;
		else
			{
				int index = mazIndex[maz];
				retValue = mazTazValues[index];

			}
		return retValue;
	}

    public int getMazPumaValue( int maz ) {
    	int index = mazIndex[maz];
    	return mazPumaValues[index];
    }

    //Commented by GV 02/02/2015

    public int getMaxMaz() {
    	return maxMaz;
    }

    public int getMaxTaz() {
    	return maxTaz;
    }

    public int getMaxPuma() {
    	return maxPuma;
    }

    public int getMaxRegion(){
    	return maxRegion;
    }

    public int getMaxSchRegion(){
    	return maxSchReg;
    }

    public int getMaxExternalStation(){
    	return maxExternalStation;
    }

    public int getRegionForOutput(int maz){
    	int result = -1;
    	result =  mazRegionCorrespTable[maz];
    	return result;
    }

 

}
