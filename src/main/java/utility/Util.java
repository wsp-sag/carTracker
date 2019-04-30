package utility;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class Util {

	// define variables to describe start and end of simulation - 3:00 AM
	public static final int START_HOUR = 3;
	public static final int START_MINUTES = 0;
	public static final int END_HOUR = 27;
	public static final int END_MINUTES = 0;
	
	public static int getMinutesFromStart( String time ) {
		int colonIndex = time.indexOf( ':' );
		int hr = Integer.parseInt( time.substring( 0, colonIndex ) );
		int min = Integer.parseInt( time.substring( colonIndex+1 ) );
		int minutes = ( hr - START_HOUR )*60 + ( min - START_MINUTES );
		return minutes;
	}
	
	public static String getHourMinuteStringFromMinutes( float minutes ) {
		int hr = (int)( minutes/60.0 );
		int min = (int)(minutes - hr*60);
		String time = hr + ":";
		time += ( min < 10 ? "0" + min : min );
		return time;
	}
	
	public static String getHourMinuteSecondStringFromMinutes( double minutes, double offset ) {
		minutes += offset;
		return getHourMinuteSecondStringFromMinutes( minutes );
	}
	
	public static String getHourMinuteSecondStringFromMinutes( double minutes ) {
		int hr = (int)( minutes/60.0 );
		int min = (int)( minutes - hr*60 );
		int sec = (int)( (minutes - hr*60 - min)*60.0 );
		
		String time = hr + ":";
		time += ( min < 10 ? "0" + min : min );
		time += ":" + ( sec < 10 ? "0" + sec : sec );
		return time;
	}
	
	public static int getActivityIndex( String activity ) {
		int colonIndex = activity.indexOf( '_' );
		int index = Integer.parseInt( activity.substring( 0, colonIndex ) );
		return index;
	}
	
    public static void writeArrayDataToCsv( String fileName, String rowLabel, List<String> rowNames, List<String> colNames, double[][] values ) {

        try {
    		
    		PrintWriter outStream = new PrintWriter( new BufferedWriter( new FileWriter( fileName ) ) );
    	
            String header = "i," + rowLabel;
            for ( String name : colNames )
            	header += "," + name;
            outStream.println( header );            
    	
        	for ( int i=0; i < rowNames.size(); i++ ) {
        		String outputRecord = (i+1) + "," + rowNames.get(i);
            	for ( int j=0; j < colNames.size(); j++ ) {
            		outputRecord +=
            			"," + values[i][j]
            		;
            	}
	            outStream.println( outputRecord );        		
        	}
        	
	        outStream.close();

    	}
    	catch (IOException e) {
    		System.out.println("IO Exception writing output file: " + fileName );
    		e.printStackTrace();
    	}
    	
    }
    
        	
}
