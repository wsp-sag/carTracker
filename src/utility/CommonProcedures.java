package utility;



// This clas contains the procedures used by several models
public class CommonProcedures {
	
	//Convert interval into minutes
	public static float convertIntervalToMinutes( int interval ) {
		float minutes = ( interval - 0.5f ) * 15;
		return minutes;
	}
	
	public static float convertIntervalToMinutesForDepartFinalTripSequence( int interval ) {
		float minutes = ( interval-1) * 15 +.01f;
		return minutes;
	}
	
	public static float convertIntervalToMinutesForArriveFinalTripSequence( int interval ) {
		float minutes = ( interval ) * 15 - .01f;
		return minutes;
	}
	
	
	
	
	
	//Convert minutes into intervals
	public static int convertMinutesToInterval( float minutes ) {
		int interval = (int)( ( minutes / 15 ) + 0.99999 );
		return Math.min( 96, interval );
	}
	

}
