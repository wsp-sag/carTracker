/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package fileProcessing;



import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads an Excel parameters file.
 *
 */

public class ParameterReader {

	private static ParameterReader INSTANCE;
	
    // define fixed table dimensions and parameter sets
    private static final int OBJECTIVE_FUNCTION_TAB = 0;
    private static final int RIGHT_HAND_SIDE_TAB = 1;
    
    private static final int MAX_PERSON_TYPE = 8;
    private static final int MAX_PURPOSE_INDEX = 9;
    
    public static final String UNSAT_DEM = "UnSat";
    public static final String D_EARLY_FOR = "DepEarlyForAct";
    public static final String D_LATE_FOR = "DepLateForAct";
    public static final String D_EARLY_FROM = "DepEarlyFromAct";
    public static final String D_LATE_FROM = "DepLateFromAct";
    
    private static final String[] OF_PARAM_NAMES = { 
    	UNSAT_DEM,
    	D_EARLY_FOR,
    	D_LATE_FOR,
    	D_EARLY_FROM,
    	D_LATE_FROM
    };

    public static final String S_SHORT_2 = "DurShort2";
    public static final String S_SHORT_3 = "DurShort3";
    public static final String S_SHORT_4 = "DurShort4";
    public static final String S_LONG_2 = "DurLong2";
    public static final String S_LONG_3 = "DurLong3";
    public static final String S_EARLY_2 = "DepEarly2";
    public static final String S_LATE_2 = "DepLate2";
    public static final String Q_EARLY_2 = "ArrEarly2";
    public static final String Q_LATE_2 = "ArrLate2";

    private static final String[] RHS_PARAM_NAMES = { 
        S_SHORT_2,
        S_SHORT_3,
        S_SHORT_4,
        S_LONG_2,
        S_LONG_3,
        S_EARLY_2,
        S_LATE_2,
        Q_EARLY_2,
        Q_LATE_2
    };
            
    private Map< String, float[][] > objectiveFunctionParameters;
    private Map< String, float[][] > rightHandSideParameters;


    
    private ParameterReader() {
    }
    
    public static ParameterReader getInstance() {
    	if ( INSTANCE == null )
    		INSTANCE = new ParameterReader();
    	return INSTANCE;
    }


    public void readParameters ( String fileName ) {

    	objectiveFunctionParameters = new HashMap< String, float[][] >();
    	rightHandSideParameters = new HashMap< String, float[][] >();
    	
        try {
        	
            WorkbookSettings ws = new WorkbookSettings();
            ws.setGCDisabled( true );
            Workbook workbook = Workbook.getWorkbook( new File(fileName), ws );
            
            readObjectiveFunctionParameters( workbook );
            //readRightHandSideParameters( workbook );
            
        }
        catch (Throwable t) {
            System.out.println( "error reading parameters from: " + fileName );
            t.printStackTrace();
            System.exit(-1);
        }

    }



    /**
     * Read the entries for the objective function parameters.
     */
    private void readObjectiveFunctionParameters( Workbook workbook ) {

        Sheet sheet = workbook.getSheet( OBJECTIVE_FUNCTION_TAB );
        
        int startRow = 5 - 1;
        int startCol = 3 - 1 - 1;
        
        for ( String name : OF_PARAM_NAMES ) {
        	objectiveFunctionParameters.put( name, readValuesFromWorksheet( startRow, startCol, sheet ) );
        	startRow += ( MAX_PURPOSE_INDEX + 3);
        }

    }


    /**
     * Read the entries for the objective function parameters.
     */
    private void readRightHandSideParameters( Workbook workbook ) {

        Sheet sheet = workbook.getSheet( RIGHT_HAND_SIDE_TAB );
        
        int startRow = 5 - 1;
        int startCol = 3 - 1 - 1;
        
        for ( String name : RHS_PARAM_NAMES ) {
        	rightHandSideParameters.put( name, readValuesFromWorksheet( startRow, startCol, sheet ) );
        	startRow += ( MAX_PURPOSE_INDEX + 3);
        }

    }


    private float[][] readValuesFromWorksheet( int startRow, int startCol, Sheet sheet ) {

        Cell cell;
    	
        float[][] values = new float[MAX_PURPOSE_INDEX+1][MAX_PERSON_TYPE+1];
        
        for ( int i=0; i <= MAX_PURPOSE_INDEX; i++ ) {

        	int row = startRow + i;
        	
        	for ( int j=1; j <= MAX_PERSON_TYPE; j++ ) {
        		
        		int col = startCol + j;
        		
                cell = sheet.getCell( col, row );
                String coeffString = cell.getContents().trim();
                values[i][j] = Float.parseFloat( coeffString );

        	}

        }

        return values;
        
    }
    
    
    public float[][] getOfParameters( String parameterSetName ) {
    	return objectiveFunctionParameters.get( parameterSetName );
    }

    public float[][] getRhsParameters( String parameterSetName ) {
    	return rightHandSideParameters.get( parameterSetName );
    }

}
