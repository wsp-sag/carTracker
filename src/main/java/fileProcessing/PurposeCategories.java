package fileProcessing;

import java.util.ArrayList;
import java.util.List;

public enum PurposeCategories {
	
	HOME          ( "home",                             0 ),
	WORK          ( "work",                             1 ),
	UNIVERSITY    ( "university",                       2 ),
	SCHOOL        ( "school",                           3 ),
	ESCORT        ( "escort",                           4 ),
	ESCORT_SCHOOL ( "school escort",                   41 ),
	ESCORT_PE     ( "school pure escort",             411 ),
	ESCORT_RS     ( "school ride sharing",            412 ),
	ESCORT_OTHER  ( "non-school escort",               42 ),
	SHOP          ( "shop",                             5 ),
	MAINTENANCE   ( "maintenance",                      6 ),
	MAINTENANCE_HH   ( "HH maintenance",                      61 ),
	MAINTENANCE_PER   ( "personal maintenance",                      62 ),
	EAT_OUT		  ( "eat out",                          7 ),
	EAT_BREAKFAST ( "eat out breakfast",               71 ),
	EAT_LUNCH     ( "eat out lunch",                   72 ),
	EAT_DINNER    ( "eat out dinner",                  73 ),
	VISITING	  ( "visiting", 	                    8 ),
	DISCRETIONARY ( "discretionary",                    9 ),
	SPECIAL_EVENT ( "special event",                   10 ),
	AT_WORK       ( "at work",                         11 ),
	AT_WORK_BUS   ( "at work business",                12 ),
	AT_WORK_LUN   ( "at work lunch",                   13 ),
	AT_WORK_OTH   ( "at work other",                   14 ),
	BUSINESS      ( "business",                        15 ),
	CAMPUS_BUSINESS ( "campusBusiness",                16 );
	
	private final String label;
	private final int index;
	
	
	
	PurposeCategories( String label, int index ) {
		this.label = label;
		this.index = index;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static PurposeCategories getPurpose( int index ) {
		PurposeCategories returnPurpose = null;
		for ( PurposeCategories purpose : PurposeCategories.values() ) {
			if ( purpose.getIndex() == index ) {
				returnPurpose = purpose;
				break;
			}
		}
		return returnPurpose;
	}

	public static int getMaxPurposeIndex() {
		int maxIndex = 0;
		for ( PurposeCategories purpose : PurposeCategories.values() ) {
			if ( purpose.getIndex() > maxIndex )
				maxIndex = purpose.getIndex();
		}
		return maxIndex;
	}

	public static List<Integer> getPurposeIndices() {
		List<Integer> purposeIndices = new ArrayList<>();
		for ( PurposeCategories purpose : PurposeCategories.values() )
			purposeIndices.add( purpose.getIndex() );
		return purposeIndices;
	}

}
