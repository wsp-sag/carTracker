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


import jxl.format.CellFormat;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import java.io.File;
import java.io.IOException;

/**
 * Reads an Excel parameters file.
 *
 */

public class WorksheetWriter {

	private static WorksheetWriter INSTANCE;
	
	private WritableWorkbook workbook;
	private WritableSheet sheet;
	private Sheet oldSheet;

    
    private WorksheetWriter() {
    }
    
    // create an instance for a specific filename
    public static WorksheetWriter getInstance( String newFileName, String oldFileName ) {
    	
    	if ( INSTANCE == null ) {
    		
    		INSTANCE = new WorksheetWriter();

            try {
            	
            	Workbook oldWb = Workbook.getWorkbook( new File(oldFileName) );
                INSTANCE.oldSheet = oldWb.getSheet( 0 );
                
                INSTANCE.workbook = Workbook.createWorkbook( new File(newFileName), oldWb );
                INSTANCE.sheet = INSTANCE.workbook.createSheet( "NewResult", 0 );

                for ( int i=0; i < INSTANCE.oldSheet.getRows(); i++ )
                    for ( int j=0; j < INSTANCE.oldSheet.getColumns(); j++ )
                    	INSTANCE.writeValue ( i, j, INSTANCE.oldSheet.getCell( j, i ).getContents() );
                
            }
            catch (Throwable t) {
                System.out.println( "error copying existing workbook: " + oldFileName + " to new workbook: " + newFileName );
                t.printStackTrace();
                System.exit(-1);
            }

    	}

    	return INSTANCE;
    }

    


    public void writeValue ( int row, int col, String stringValue ) {

        Label label = new Label( col, row, stringValue );
        WritableCell cell = (WritableCell)label;
        CellFormat format = oldSheet.getCell( col, row ).getCellFormat();

        if ( format == null )
        	return;
    	        
        cell.setCellFormat( format );
        
        try {
			sheet.addCell(cell);
		}
        catch (RowsExceededException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (NullPointerException e) {
			e.printStackTrace();
        }
        
    }
    
    
    
    public void closeWorkbook () {
        
        try {
        	workbook.write();
        	workbook.close();
		}
        catch (WriteException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    
    
    
    public static void main( String[] args ) throws IOException, RowsExceededException, WriteException, BiffException {
 
    	String oldName = "C:/Users/Jim/Documents/projects/cmap/ScheduleAdjustment_Output.xls";
    	String newName = "C:/Users/Jim/Documents/projects/cmap/ScheduleAdjustment_Result.xls";

    	WorksheetWriter writer = getInstance( newName, oldName );
    	writer.writeValue( 4, 3, "5:00" );
    	writer.closeWorkbook();

    }
    
}
