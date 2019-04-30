package accessibility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


import com.pb.common.datafile.TableDataSet;

public class SocioEconomicDataManager implements Serializable {

	private static final long versionNumber = 1L;	
	private static final long serialVersionUID = versionNumber;
	
	public static final SocioEconomicDataManager INSTANCE = new SocioEconomicDataManager();

	private TableDataSet tds;
	private Map<String, int[]> intDataFieldsMap = new HashMap<String, int[]>();
	private Map<String, double[]> doubleDataFieldsMap = new HashMap<String, double[]>();
	private boolean dataTableIsLoaded;
	private int[] mazValues;
	private int maxMazValue=0;
	
    private SocioEconomicDataManager() {
    }

    
	public static SocioEconomicDataManager getInstance() {
		return INSTANCE;
	}

	
	public void loadDataFromCsvFile( String fileName, String mazValueField ) {
		
		tds = TableDataSet.readFile( fileName );
		
		// process the mazValues and determine the maximum value for use in dimensioning arrays
		mazValues = tds.getColumnAsInt( mazValueField );
		for ( int maz : mazValues )
			if ( maz > maxMazValue )
				maxMazValue = maz;
		
		dataTableIsLoaded = true;
	}
	
	
	public boolean getDataTableIsLoaded() {
		return dataTableIsLoaded;
	}

	
	public int[] getIntFieldByMazValue( String fieldName ) {
		
		int[] returnArray = null;
		
		// if the column for this field is already stored in the data map, return it
		if ( intDataFieldsMap.containsKey( fieldName ) ) {
			
			returnArray = intDataFieldsMap.get( fieldName );
			
		}
		// otherwise, get the column values, save them in an array indexed by maz value, and store it in the map
		else {

			returnArray = new int[maxMazValue+1];

			int[] values = tds.getColumnAsInt( fieldName );
			for ( int i=0; i < mazValues.length; i++ ) {
				int maz = mazValues[i];
				returnArray[maz] = values[i];
			}
			
			intDataFieldsMap.put( fieldName, returnArray );
			
		}
		
		return returnArray;
	}

	
	public double[] getDoubleFieldByMazValue( String fieldName ) {
		
		double[] returnArray = null;
		
		// if the column for this field is already stored in the data map, return it
		if ( doubleDataFieldsMap.containsKey( fieldName ) ) {
			
			returnArray = doubleDataFieldsMap.get( fieldName );
			
		}
		// otherwise, get the column values, save them in an array indexed by maz value, and store it in the map
		else {

			returnArray = new double[maxMazValue+1];

			double[] values = tds.getColumnAsDouble( fieldName );
			for ( int i=0; i < mazValues.length; i++ ) {
				int maz = mazValues[i];
				returnArray[maz] = values[i];
			}
			
			doubleDataFieldsMap.put( fieldName, returnArray );
			
		}
		
		return returnArray;
	}
	
}
