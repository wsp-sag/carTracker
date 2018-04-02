package utility;

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
    
}
    

