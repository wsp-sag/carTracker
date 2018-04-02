package fileProcessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import objectMapping.AbmObjectTranslater;

public class AbmDataReader {

	
    public static Map<Integer, List<List<String>>> getValuesFromCsvFileForHhIdsAndFields( String filename, String hhIdLabel, Map<String,Integer> fieldIndexMap, int minRange, int maxRange, List<String> headerMap) {
    	
    	Map< Integer, List<List<String>> > resultMap = new HashMap< Integer, List<List<String>> >();
            	
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
	
	        
	        
	        // read the field names from the header record to get the field index for the hhIdLabel
	        String line = inStream.readLine();
            StringTokenizer st = new StringTokenizer( line, delimSet );
            
            int index = 0;
            int hhIdIndex = -1;
            while ( st.hasMoreTokens() ) {
            	
            	String name = st.nextToken().trim();
            	
            	if ( name.equalsIgnoreCase( hhIdLabel ) ) {
            		hhIdIndex = index;
            		break;
            	}
            	
            	index++;
            	
            }

            
            List<String> fieldValues = null;
        	List<List<String>> returnValues = null;
            
        	int hhIdValue = -1;
        	int oldHhId = -1;
        	
        	int recnum = 0;
        	
            // read the data records and extract the value for the column names passed in, for the hhId only
            while ( ( line = inStream.readLine() ) != null ) {

            	recnum++;
                st = new StringTokenizer( line, delimSet );
                
	        	hhIdValue = -1;
	        	for ( int i=0; i < hhIdIndex; i++ )
                    st.nextToken();
                hhIdValue = Integer.parseInt( st.nextToken() );
                
                if ( hhIdValue >= minRange ) {

                	if ( hhIdValue >= maxRange )
                		break;
                	

                	Map<Integer,String> valuesMap = new HashMap<>();
                	
                    st = new StringTokenizer( line, delimSet );
                    int fieldIndex = 0;
                    int fieldIndexAll = 0;
                    Integer valuesIndex = null;
                    int stringValueIndex = 0;
                    while ( st.hasMoreTokens() ) {

                    	String fieldString = st.nextToken();
                    	
                    	// skip fields from the file that were not included in the fieldIndexMap
                    	String colName = headerMap.get(stringValueIndex);
                    	
                    	if(fieldIndexMap.containsKey(colName)){
                    		fieldIndex = fieldIndexMap.get(colName);
                    		stringValueIndex++;
                    		fieldIndexAll++;
                    	}
                    	else{
                    		stringValueIndex++;
                    		continue;
                    		
                    	}                   	
                    	
                    	
                    	
                    	String fieldValue = fieldString.trim();
                    	
                    	if ( fieldValue.startsWith("\"") ) {
                    		
                        	if ( ! fieldValue.endsWith("\"") ) {
                        		String tempValue = fieldValue;   
                        		tempValue = st.nextToken().trim();
                            	while ( ! tempValue.endsWith("\"") ) {
                            		fieldValue += "," + tempValue;
                            		tempValue = st.nextToken().trim();
                            	}
                        		fieldValue += "," + tempValue;
                        	}

                        	// finally, remove the beginning "[ " and ending " ]" from the String
                       		fieldValue = fieldValue.substring( 2, fieldValue.length()-2 );

                    	}

                    	valuesMap.put( fieldIndex, fieldValue );
                    	
    	        	}
                	
                	//valuesIndex = headerMap.indexOf(fieldIndexMap.get(fieldIndex));
                    fieldIndexAll++;
                	valuesMap.put( fieldIndexAll, String.valueOf( recnum ) );

                	
                    if ( hhIdValue == oldHhId ) {

                    	fieldValues = new ArrayList<String>();
                    	for ( int i=0; i < valuesMap.size(); i++ )
                    		fieldValues.add( valuesMap.get(i) );
                        returnValues.add( fieldValues);
                        
                    }
                    else {
                    	
                    	if ( oldHhId >= 0 )
                    		resultMap.put( oldHhId, returnValues );
                    	
                    	returnValues = new ArrayList<List<String>>();
                    	fieldValues = new ArrayList<String>();
                    	for ( int i=0; i < valuesMap.size(); i++ )
                    		fieldValues.add( valuesMap.get(i) );
                        returnValues.add( fieldValues);

                    	oldHhId = hhIdValue;
                    	
                    }

                }
                
            }
            
    		resultMap.put( oldHhId, returnValues );
	            
    	}
    	catch (NumberFormatException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	catch (IOException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	
    	
    	return resultMap;
    	
    }

   	
//    public static Map<Integer, List<List<String>>> getValuesFromCsvFileForHhIds( String filename, String hhIdLabel, int minRange, int maxRange ) {
//    	
//    	Map< Integer, List<List<String>> > resultMap = new HashMap< Integer, List<List<String>> >();
//            	
//    	try {
//    		
//	        // open the input stream
//	        String delimSet = ",";
//	        BufferedReader inStream = null;
//	        try {
//	            inStream = new BufferedReader(new FileReader(new File(filename)));
//	        }
//	        catch (FileNotFoundException e) {
//	            e.printStackTrace();
//	            System.exit(-1);
//	        }
//	
//	        
//	        
//	        // read the field names from the header record to get the field index for the hhIdLabel
//	        String line = inStream.readLine();
//            StringTokenizer st = new StringTokenizer( line, delimSet );
//            
//            int index = 0;
//            int hhIdIndex = -1;
//            while ( st.hasMoreTokens() ) {
//            	
//            	String name = st.nextToken().trim();
//            	
//            	if ( name.equalsIgnoreCase( hhIdLabel ) ) {
//            		hhIdIndex = index;
//            		break;
//            	}
//            	
//            	index++;
//            	
//            }
//
//            
//            List<String> fieldValues = null;
//        	List<List<String>> returnValues = null;
//            
//        	int hhIdValue = -1;
//        	int oldHhId = -1;
//        	
//        	int recnum = 0;
//        	
//            // read the data records and extract the value for the column names passed in, for the hhId only
//            while ( ( line = inStream.readLine() ) != null ) {
//
//            	recnum++;
//                st = new StringTokenizer( line, delimSet );
//                
//	        	hhIdValue = -1;
//	        	for ( int i=0; i < hhIdIndex; i++ )
//                    st.nextToken();
//                hhIdValue = Integer.parseInt( st.nextToken() );
//                
//                if ( hhIdValue >= minRange ) {
//
//                	if ( hhIdValue >= maxRange )
//                		break;
//                	
//                	fieldValues = new ArrayList<String>();
//                	
//                    st = new StringTokenizer( line, delimSet );
//                    while ( st.hasMoreTokens() ) {
//
//                    	String fieldValue = st.nextToken().trim();
//                    	
//                    	if ( fieldValue.startsWith("\"") ) {
//                    		
//                        	if ( ! fieldValue.endsWith("\"") ) {
//                        		String tempValue = fieldValue;   
//                        		tempValue = st.nextToken().trim();
//                            	while ( ! tempValue.endsWith("\"") ) {
//                            		fieldValue += "," + tempValue;
//                            		tempValue = st.nextToken().trim();
//                            	}
//                        		fieldValue += "," + tempValue;
//                        	}
//
//                        	// finally, remove the beginning "[ " and ending " ]" from the String
//                       		fieldValue = fieldValue.substring( 2, fieldValue.length()-2 );
//
//                    	}
//
//                    	fieldValues.add( fieldValue );
//                    	
//    	        	}
//                	
//                    fieldValues.add( String.valueOf( recnum ) );
//
//                    if ( hhIdValue == oldHhId ) {
//
//                        returnValues.add( fieldValues);
//                        
//                    }
//                    else {
//                    	
//                    	if ( oldHhId >= 0 )
//                    		resultMap.put( oldHhId, returnValues );
//                    	
//                    	returnValues = new ArrayList<List<String>>();
//                        returnValues.add( fieldValues);
//
//                    	oldHhId = hhIdValue;
//                    	
//                    }
//
//                }
//                
//            }
//            
//    		resultMap.put( oldHhId, returnValues );
//	            
//    	}
//    	catch (NumberFormatException e) {
//        	e.printStackTrace();
//        	System.exit(-1);
//        }
//    	catch (IOException e) {
//        	e.printStackTrace();
//        	System.exit(-1);
//        }
//    	
//    	
//    	return resultMap;
//    	
//    }

    	
    public static List<List<String>> getValuesFromCsvFileForFieldNames( String filename, List<String> fieldNames ) {
    	
    	// assumes idFieldName is the fieldName for an integer field
    	List<List<String>> returnValues = new ArrayList<List<String>>();

    	List<String> fileFieldNames = getFieldNamesFromCsvFile( filename );
    	
    	// get a map of field names and field index
    	List<Integer> fieldIndexList = new ArrayList<Integer>();
    	for ( String name : fieldNames ) {
        	int count = 0;
        	for ( String field : fileFieldNames ) {
        		if ( field.equalsIgnoreCase( name ) ) {
        			fieldIndexList.add( count );
            		break;
        		}
        		count++;
        	}
    	}
    
            	
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
	
	        
	        
	        // skip the field names header record
	        String line = inStream.readLine();

        	Map<Integer, String> indexValueMap = new HashMap<Integer, String>( fieldIndexList.size() );
        	for ( int n : fieldIndexList )
        		indexValueMap.put( n, "" );
	        
            // read the data records and extract the values for the columns desired
            while ( ( line = inStream.readLine() ) != null ) {

            	StringTokenizer st = new StringTokenizer( line, delimSet );
    	        int count = 0;
    	        int numValues = 0;
                while ( st.hasMoreTokens() ) {

                	String fieldString = st.nextToken();
                	
                	// skip fields from the file that were not included in the fieldIndexMap
                	String temp = indexValueMap.get( count );
                	if ( temp == null  ) {
                		count++;
                		continue;
                	}
                	                	
                	String fieldValue = fieldString.trim();
                	
                	if ( fieldValue.startsWith("\"") ) {
                		
                    	if ( ! fieldValue.endsWith("\"") ) {
                    		String tempValue = fieldValue;   
                    		tempValue = st.nextToken().trim();
                        	while ( ! tempValue.endsWith("\"") ) {
                        		fieldValue += "," + tempValue;
                        		tempValue = st.nextToken().trim();
                        	}
                    		fieldValue += "," + tempValue;
                    	}

                    	// finally, remove the beginning "[ " and ending " ]" from the String
                   		fieldValue = fieldValue.substring( 2, fieldValue.length()-2 );

                	}
                	
                	
           			indexValueMap.put( count, fieldValue );
            		numValues++;

            		if ( numValues == fieldNames.size() )
            			break;

                	count++;
	        	}

                
            	List<String> valueList = new ArrayList<String>( numValues );            	
            	for ( int n=0; n < numValues; n++)
            		valueList.add( indexValueMap.get( fieldIndexList.get( n ) ) );
                
                returnValues.add( valueList );
                
            }
            
	            
    	}
    	catch (NumberFormatException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	catch (IOException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	
    	
    	return returnValues;
    	
    }

    	
    public static List<String> getFieldNamesFromCsvFile( String filename ) {
    	
    	List<String> returnValues = new ArrayList<String>();

            	
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
	
	        
	        
	        // read the field names from the header record to get the field index for the hhIdLabel
	        String line = inStream.readLine();
            StringTokenizer st = new StringTokenizer( line, delimSet );
            
            while ( st.hasMoreTokens() ) {
            	String name = st.nextToken().trim();
            	returnValues.add( name );
            }
	            
    	}
    	catch (NumberFormatException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	catch (IOException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
    	
    	
    	return returnValues;
    	
    }

    	
}
