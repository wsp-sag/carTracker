package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;


public class Parsing implements Serializable {
    
	private static final long versionNumber = 1L;
	
	private static final long serialVersionUID = versionNumber;

	public Parsing() {
	}

	
	public static String getOneDimensionalIntArrayExportString( int[] intArray ) {
		
		String exportString = "[";
		if( intArray.length > 0 ) {
			exportString += intArray[0];
			for ( int j=1; j < intArray.length; j++ )
				exportString += "," + intArray[j];
		}
		exportString += "]";
		
		return exportString;
		
	}

	
	public static String getOneDimensionalShortArrayExportString( short[] intArray ) {
		
		String exportString = "[";
		if( intArray.length > 0 ) {
			exportString += intArray[0];
			for ( int j=1; j < intArray.length; j++ )
				exportString += "," + intArray[j];
		}
		exportString += "]";
		
		return exportString;
		
	}

	
	public static float[] getOneDimensionalFloatArrayValuesFromExportString( String exportString ) {
		
		List<Float> valueList = new ArrayList<Float>();
		
	    StringTokenizer st = new StringTokenizer( exportString, "," );	    
	    while ( st.hasMoreTokens() ) {
		   	String stringValue = st.nextToken().trim();
		   	float floatValue = Float.parseFloat( stringValue );
		   	valueList.add( floatValue );
	    }

	    float[] returnArray = new float[valueList.size()];
	    for ( int i=0; i < returnArray.length; i++ )
	    	returnArray[i] = valueList.get( i );
	    
	    return returnArray;
	    
	}

	
	public static int[] getOneDimensionalIntArrayValuesFromExportString( String exportString ) {
		
		List<Integer> valueList = new ArrayList<Integer>();
		
	    StringTokenizer st = new StringTokenizer( exportString, "," );	    
	    while ( st.hasMoreTokens() ) {
		   	String stringValue = st.nextToken().trim();
		   	int intValue = Integer.parseInt( stringValue );
		   	valueList.add( intValue );
	    }

	    int[] returnArray = new int[valueList.size()];
	    for ( int i=0; i < returnArray.length; i++ )
	    	returnArray[i] = valueList.get( i );
	    
	    return returnArray;
	    
	}

	
	public static String getTwoDimensionalIntArrayExportString( int[][] intArray ) {
		
		String exportString = "[[";
		if( intArray[0] == null ) {
			exportString += "*";
		}
		else if( intArray[0].length > 0 ) {
			exportString += intArray[0][0];
			for ( int j=1; j < intArray[0].length; j++ )
				exportString += "," + intArray[0][j];
		}
		exportString += "]";
		
		for ( int i=1; i < intArray.length; i++ ) {
			exportString += "," + "[";
			if( intArray[i] == null ) {
				exportString += "*";
			}
			else if( intArray[i].length > 0 ) {
				exportString += intArray[i][0];
				for ( int j=1; j < intArray[i].length; j++ )
					exportString += "," + intArray[i][j];
			}
			exportString += "]";
		}
		exportString += "]";

		return exportString;
		
	}
	
	public static int[][] getTwoDimensionalIntArrayValuesFromExportString( String exportString ) {
		
		List<int[]> valueList = new ArrayList<int[]>();
		
	   	int leftBracketIndex = exportString.indexOf( '[' );	   	
	   	while ( leftBracketIndex >= 0 ) {
		   	int rightBracketIndex = exportString.indexOf( ']', leftBracketIndex );	   	
		   	String tempString = exportString.substring( leftBracketIndex+1, rightBracketIndex );
		   	int[] values = null;
		   	if ( ! tempString.equals( "*" ) )
		   		values = Parsing.getOneDimensionalIntArrayValuesFromExportString( tempString );
		   	valueList.add( values );
		   	
		   	leftBracketIndex = exportString.indexOf( '[', rightBracketIndex );
	   	}
		
	    int[][] returnArray = new int[valueList.size()][];
	    for ( int i=0; i < returnArray.length; i++ )
	    	returnArray[i] = valueList.get( i );
	    
	    return returnArray;
	    
	}

    public static int[] getIntegerArrayFromPropertyMap( Map<String, String> rbMap, String key )
    {

        int[] returnArray;
        String valueList = rbMap.get(key);
        if (valueList != null)
        {

            TreeSet<Integer> valueSet = new TreeSet<Integer>();

            StringTokenizer valueTokenizer = new StringTokenizer(valueList, ",");
            while(valueTokenizer.hasMoreTokens())
            {
                String listValue = valueTokenizer.nextToken();
                int intValue = Integer.parseInt(listValue.trim());
                valueSet.add(intValue);
            }

            returnArray = new int[valueSet.size()];
            int i = 0;
            for (int v : valueSet)
                returnArray[i++] = v;

        } else
        {
            System.out.println( "property file key: " + key + " missing.  No integer value can be determined.");
            throw new RuntimeException();
        }

        return returnArray;

    }

    public static String[] getStringArrayFromPropertyMap(HashMap<String, String> rbMap, String key)
    {

        String[] returnArray;
        String valueListString = rbMap.get(key);
        if (valueListString != null)
        {

            List<String> valueList = new ArrayList<String>();

            StringTokenizer valueTokenizer = new StringTokenizer(valueListString, ",");
            while(valueTokenizer.hasMoreTokens()) {
                String listValue = valueTokenizer.nextToken();
                valueList.add( listValue );
            }

            returnArray = new String[valueList.size()];
            int i = 0;
            for (String v : valueList)
                returnArray[i++] = v;

        } else
        {
            System.out.println( "property file key: " + key + " missing.  No String values can be determined.");
            throw new RuntimeException();
        }

        return returnArray;

    }

    public static String getIntArrayAsString( int[] values ) {
    	List<Integer> valueList = new ArrayList<Integer>(values.length);
    	for ( int value : values )
    		valueList.add( value );
    	return getIntListAsString( valueList );
    }
    
    public static  String getIntListAsString( List<Integer> intList ) {

    	String result = "[ ";

		if( intList == null ) {
			result = "[ ]";
		}
		else {
	    	for ( int value : intList ) {
	    		if ( result.length() > "[ ".length() )
	    			result += ",";
	    		result += String.valueOf( value );    		
	    	}
			if ( result.length() > "[ ".length() )
				result += " ]";
			else
				result = "[ ]";
		}
		
		return result;
    }
    
public static String[] getStringArrayFromCsvString( String csvString ) {
    	
    	String[] results = null;
    	
        List<String> resultList = new ArrayList<String>(); 
        
        String delimSet = ",";
        StringTokenizer st = new StringTokenizer( csvString, delimSet );
        
        int k = 0;
        int columnIndex = -1;
        while ( st.hasMoreTokens() ) {
        	String value = st.nextToken().trim();
        	resultList.add( value );
        }

        
    	results = new String[resultList.size()];
    	for ( int i=0; i < results.length; i++ )
    		results[i] = resultList.get( i );
	        	
    	return results;
    	
    }
public static int[] getIntArrayFromCsvFile( String filename, String fieldName ) {
	
	int[] results = null;
	
	try {
		
        // open the input stream
        String delimSet = ",";
        BufferedReader inStream = null;
        try {
            inStream = new BufferedReader(new FileReader(new File(filename)));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        
        
        List<Integer> resultList = new ArrayList<Integer>(); 

        
        // read the header record and get the column index that matches the fieldName passed in
        String line = inStream.readLine();
        StringTokenizer st = new StringTokenizer( line, delimSet );
        
        int k = 0;
        int columnIndex = -1;
        while ( st.hasMoreTokens() ) {
        	String name = st.nextToken().trim();
    		if ( fieldName.equalsIgnoreCase( name ) ) {
    			columnIndex = k;
    			break;
    		}
        	k++;
        }

        
        // read the data records and extract the value for the determined column index
        while ( ( line = inStream.readLine() ) != null ) {

            st = new StringTokenizer( line, delimSet );
            
        	int value = -1;
        	for ( int i=0; i <= columnIndex; i++ )
                value = Integer.parseInt(st.nextToken());
        	resultList.add( value );

        }
            

    	results = new int[resultList.size()];
    	for ( int i=0; i < results.length; i++ )
    		results[i] = resultList.get( i );
        	
        
	} catch (NumberFormatException e) {
    	e.printStackTrace();
    	System.exit(-1);
    } catch (IOException e) {
    	e.printStackTrace();
    	System.exit(-1);
    }
	
	
	return results;
	
}


public static double[] getDoubleArrayFromCsvFile( String filename, String fieldName ) {
	
	double[] results = null;
	
	try {
		
        // open the input stream
        String delimSet = ",";
        BufferedReader inStream = null;
        try {
            inStream = new BufferedReader(new FileReader(new File(filename)));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        
        
        List<Double> resultList = new ArrayList<Double>(); 

        
        // read the header record and get the column index that matches the fieldName passed in
        String line = inStream.readLine();
        StringTokenizer st = new StringTokenizer( line, delimSet );
        
        int k = 0;
        int columnIndex = -1;
        while ( st.hasMoreTokens() ) {
        	String name = st.nextToken().trim();
    		if ( fieldName.equalsIgnoreCase( name ) ) {
    			columnIndex = k;
    			break;
    		}
        	k++;
        }

        
        // read the data records and extract the value for the determined column index
        while ( ( line = inStream.readLine() ) != null ) {

            st = new StringTokenizer( line, delimSet );
            
        	double value = -1;
        	for ( int i=0; i <= columnIndex; i++ )
                value = Double.parseDouble(st.nextToken());
        	
        	resultList.add( value );

        }
            

    	results = new double[resultList.size()];
    	for ( int i=0; i < results.length; i++ )
    		results[i] = resultList.get( i );
        	
        
	} catch (NumberFormatException e) {
    	e.printStackTrace();
    	System.exit(-1);
    } catch (IOException e) {
    	e.printStackTrace();
    	System.exit(-1);
    }
	
	
	return results;
	
}

    

}
    

