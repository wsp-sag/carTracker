package utility;



// This clas contains the procedures used by several models
public class CommonProcedures {
	
	//Convert interval into minutes
	public static float convertIntervalToMinutes( int interval ) {
		float minutes = ( interval - 0.5f ) * Constants.TOD_INTERVAL_IN_MINUTES;
		return minutes;
	}
	
	public static float convertIntervalToMinutesForDepartFinalTripSequence( int interval ) {
		float minutes = ( interval-1) * Constants.TOD_INTERVAL_IN_MINUTES +.01f;
		return minutes;
	}
	
	public static float convertIntervalToMinutesForArriveFinalTripSequence( int interval ) {
		float minutes = ( interval ) * Constants.TOD_INTERVAL_IN_MINUTES - .01f;
		return minutes;
	}
	
	
	// MAG Specific
	public static int getInterval( int hourFrom3Am, int minute ) {
		
		int hour = hourFrom3Am + 3;
		int hr = 0;
		if ( hour > 24 )
			hr = Constants.NUM_TOD_INTERVALS;		
		else if ( hour < 4 )
			hr = 1;		
		else
			hr = hour - 4;		
		int interval = hr*Constants.NUM_TOD_INTERVALS_PER_HOUR + ( minute < Constants.TOD_INTERVAL_IN_MINUTES ? 0 : 1 );
		
		return Math.max( 1, Math.min( Constants.NUM_TOD_INTERVALS, interval ) );
	}
	
	//Convert minutes into intervals
	public static int convertMinutesToInterval( float minutes ) {
		int interval = (int)( ( minutes / Constants.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
		return Math.min( 96, interval );
	}
	

}
