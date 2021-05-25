package appLayer;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import accessibility.GeographyManager;
import accessibility.SharedDistanceMatrixData;
import accessibility.SocioEconomicDataManager;
import objects.HouseholdCarAllocation;
import utility.ConstantsIf;
import utility.Parsing;

public interface WriteCarAllocationOutputFilesIf {

	public static final String MODEL_REGION_PROPERTY = "region";
	public static final String SCENARIO_NAME_PROPERTY = "scenario.name";
	public static final String SEPARATE_CAV_TRIP_TABLES_KEY = "separate.cav.trip.matrices";
	
    public static final int TRIP_REPOSITIONING_PURPOSE = 20;
    
	public static final String OUTPUT_TRIP_TABLES_KEY = "output.trip.matrices";
	public static final String OUTPUT_TRIP_TABLE_FORMAT_KEY = "output.trip.matrices.format";
	public static final String OUTPUT_TRIP_TABLE_CSV_FILENAME_KEY = "output.trip.matrices.csv.file";
	public static final String MODE_TABLE_NAMES_KEY = "output.trip.matrix.mode.tables";
	public static final String AUTO_MODE_TABLE_NAMES_KEY = "output.trip.matrix.auto.mode.tables";
	
	public static final String TRIP_MATRIX_EA_FILE_KEY = "output.trip.matrix.early";
	public static final String TRIP_MATRIX_AM_FILE_KEY = "output.trip.matrix.am";
	public static final String TRIP_MATRIX_MD_FILE_KEY = "output.trip.matrix.midday";
	public static final String TRIP_MATRIX_PM_FILE_KEY = "output.trip.matrix.pm";
	public static final String TRIP_MATRIX_EV_FILE_KEY = "output.trip.matrix.late";
	
	public static final String NUM_OUTPUT_PERIODS_KEY = "number.periods";
	public static final String EA_PERIOD_START_KEY = "early.period.start.interval";
	public static final String EA_PERIOD_END_KEY = "early.period.end.interval";
	public static final String AM_PERIOD_START_KEY = "am.period.start.interval";
	public static final String AM_PERIOD_END_KEY = "am.period.end.interval";
	public static final String MD_PERIOD_START_KEY = "midday.period.start.interval";
	public static final String MD_PERIOD_END_KEY = "midday.period.end.interval";
	public static final String PM_PERIOD_START_KEY = "pm.period.start.interval";
	public static final String PM_PERIOD_END_KEY = "pm.period.end.interval";
	public static final String EV_PERIOD_START_KEY = "late.period.start.interval";
	public static final String EV_PERIOD_END_KEY = "late.period.end.interval";
	

	public static final String NUM_USER_CLASS_KEY = "num.user.classes";



	public void writeCarAllocationOutputFile( Logger logger, HashMap<String, String> propertyMap, 
			    	String outputTripListFilename, String outputDisaggregateCarUseFileName, String outputProbCarChangeFileName,
			    	String outputVehTypePurposeSummaryFileName, String outputVehTypePersTypeSummaryFileName, String outputVehTypeDistanceSummaryFileName,
			    	List<HouseholdCarAllocation> hhCarAllocationResultsList, GeographyManager geogManager,
			    	SharedDistanceMatrixData sharedDistanceObject, SocioEconomicDataManager socec,
			    	ConstantsIf constants);
	

    default int getTripTablePeriod(int departureInterval, int[][] periodIntervals){
    	
    	int nextPeriodIndex = 1;
    	while ( departureInterval >= periodIntervals[nextPeriodIndex][0] ) {
    		nextPeriodIndex++;
    		if ( nextPeriodIndex == 5 )
    			break;
    	}
    	
    	return (nextPeriodIndex - 1);
    }


    default float[][] getTripTableOrderedByExternalTazValues( int[] externalTazOrder, float[][] tripTable, int[] tazValues ) {
    	
    	float[][] orderedTripTable = new float[tazValues.length][tazValues.length];
    	
    	for ( int i=0; i < tazValues.length; i++ ) {
    		int k = externalTazOrder[i];
			for ( int j=0; j < tazValues.length; j++ ) {
				int m = externalTazOrder[j];				
				orderedTripTable[i][j] = tripTable[k][m]; 
			}
    	}
    	
    	return orderedTripTable;
    }


    default float[][] getTripTableOrderedByExternalTazIndices( int[] externalTazOrder, float[][] tripTable, int[] tazIndices ) {
    	
    	float[][] orderedTripTable = new float[tazIndices.length][tazIndices.length];
    	for ( int i=0; i < tazIndices.length; i++ ) {
    		if(tazIndices[i]>=0){
    			int k = externalTazOrder[tazIndices[i]];
    			for ( int j=0; j < tazIndices.length; j++ ) {
    				if(tazIndices[j]>=0){
    				int m = externalTazOrder[tazIndices[j]];
    				orderedTripTable[i-1][j-1] = tripTable[k][m]; 
    				}
    			}
    		}
    	}    	
    	return orderedTripTable;
    }


    default double[] getProbabilityOfChangingCarOwnership(String probFileName,String hhidName,String probField) {    	
    	
    	int[] hidWoSampleValues = Parsing.getIntArrayFromCsvFile( probFileName, hhidName );
    	double[] probChange = Parsing.getDoubleArrayFromCsvFile( probFileName, probField );
    	
    	int maxHidWoSample = 0;
    	for ( int i=0; i < hidWoSampleValues.length; i++  ){
    		if ( (int)hidWoSampleValues[i] > maxHidWoSample )
    			maxHidWoSample = (int)hidWoSampleValues[i];
    	}
    	double[] hhProbChangeCarOwnership = new double[maxHidWoSample+1];
    	
    	for ( int i=0; i < hidWoSampleValues.length; i++ ) {
    		int hid =(int)hidWoSampleValues[i];
    		double prob = probChange[i];
    		
    		hhProbChangeCarOwnership[hid] = prob;
    	}
    		    	
    	return hhProbChangeCarOwnership;
    	
    }

    
	default String insertBeforeExtension( String filename, String insertString ) {	
	  	int dotIndex = filename.indexOf(".");
	  	String extension = filename.substring(dotIndex+1);
	  	return filename.substring(0, dotIndex) + insertString + "." + extension;
	}

}
