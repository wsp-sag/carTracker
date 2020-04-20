package utility;


// This clas contains the procedures used by several models
public class CommonProcedures {
	
	//Convert interval into minutes
	public static float convertIntervalToMinutes( int interval, ConstantsIf constants ) {
		float minutes = ( interval - 0.5f ) * constants.getTodIntervalMinutes();
		return minutes;
	}
	
	public static float convertIntervalToMinutesForDepartFinalTripSequence( int interval, ConstantsIf constants ) {
		float minutes = ( interval-1) * constants.getTodIntervalMinutes() +.01f;
		return minutes;
	}
	
	public static float convertIntervalToMinutesForArriveFinalTripSequence( int interval, ConstantsIf constants ) {
		float minutes = ( interval ) * constants.getTodIntervalMinutes() - .01f;
		return minutes;
	}
	
	
	// MAG Specific
	public static int getInterval( int hourFrom3Am, int minute, ConstantsIf constants ) {
		
		int hour = hourFrom3Am + 3;
		int hr = 0;
		if ( hour > 24 )
			hr = constants.getNumTodIntervals();		
		else if ( hour < 4 )
			hr = 1;		
		else
			hr = hour - 4;		
		int interval = hr*constants.getNumTodIntervalsPerHour() + ( minute < constants.getTodIntervalMinutes() ? 0 : 1 );
		
		return Math.max( 1, Math.min( constants.getNumTodIntervals(), interval ) );
	}
	
	//Convert minutes into intervals
	public static int convertMinutesToInterval( float minutes, ConstantsIf constants ) {
		int interval = (int)( ( minutes / constants.getTodIntervalMinutes() ) + 0.99999 );
		return Math.min( 96, interval );
	}
	

}
