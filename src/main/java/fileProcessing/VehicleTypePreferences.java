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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;


/**
 * Reads an Excel vehicle type preferences file.
 */
public class VehicleTypePreferences implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// define fixed table dimensions and parameter sets
    private static final int PREFERENCES_TAB = 0;
    
    private static final int NUM_PURPOSES = 9;
    private static final int NUM_MODES = 3;
    private static final int NUM_PERSON_TYPES = 8;

    private static final String[] FUEL_TYPE_NAMES = { "", "no veh", "gd", "hyb", "ev" };
    private static final String[] BODY_TYPE_NAMES = { "", "mc", "lev", "car", "suv", "van", "ldt" };
    
    private List<Integer> category = new ArrayList<>();
    private List<String> fuelType = new ArrayList<>();
    private List<String> bodyType = new ArrayList<>();
    private List<String> vehSize = new ArrayList<>();
    private List<Float> usualDriver = new ArrayList<>();
    private List<List<Float>> purposePref = new ArrayList<>();
    private List<List<Float>> modePref = new ArrayList<>();
    private List<List<Float>> persTypePref = new ArrayList<>();
    private List<Float> distPref = new ArrayList<>();
    private List<Float> operatingCost = new ArrayList<>();
	
    private Map<String,Map<String,Integer>> indexLookup = new HashMap<>();
    
            


    public float getUsualDriverDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return usualDriver.get(index);
    }

    public List<Float> getPurposeDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return purposePref.get(index);
    }
    
    public List<Float> getModeDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return modePref.get(index);
    }
    
    public List<Float> getDrvPersTypePrefDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return persTypePref.get(index);
    }
    
    public float getDistanceSquaredDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return distPref.get(index);
    }

    public float getOperatingCostDisutil( int fuelTypeIndex, int bodyTypeIndex ) {
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return operatingCost.get(index);
    }

    public int getCategory( int fuelTypeIndex, int bodyTypeIndex ) {
    	int dummy = 0;
    	if ( fuelTypeIndex < 0 || bodyTypeIndex < 0 )
    		dummy = 1;
    	int index = indexLookup.get(FUEL_TYPE_NAMES[fuelTypeIndex]).get(BODY_TYPE_NAMES[bodyTypeIndex]);
    	return category.get(index);
    }

    public int getCategory( int index ) {
    	return category.get(index);
    }

    public String getFuelType( int index ) {
    	return fuelType.get(index);
    }

    public String getBodyType( int index ) {
    	return bodyType.get(index);
    }

    public String getVehSize( int index ) {
    	return vehSize.get(index);
    }

    public List<Integer> getCategories() {
    	return category;    	
    }
    
    public List<String> getFuelTypes() {
    	return fuelType.stream().distinct().collect(Collectors.toList());				
    }
    
    public List<String>  getBodyTypes() {
    	return bodyType.stream().distinct().collect(Collectors.toList());	    					
    }
    
    public List<String> getVehSizes() {
    	return vehSize.stream().distinct().collect(Collectors.toList());	    					
    }
    
    
    public void readPreferences ( String fileName ) {

        try {
        	
            WorkbookSettings ws = new WorkbookSettings();
            ws.setGCDisabled( true );
            Workbook workbook = Workbook.getWorkbook( new File(fileName), ws );
            
            readValuesFromWorksheet( workbook );
            
        }
        catch (Throwable t) {
            System.out.println( "error reading parameters from: " + fileName );
            t.printStackTrace();
            System.exit(-1);
        }

    }


    private void readValuesFromWorksheet( Workbook workbook ) {

        Logger logger = Logger.getLogger(VehicleTypePreferences.class);
    	
        Sheet sheet = workbook.getSheet( PREFERENCES_TAB );
        Cell cell;
        
        // row index 0 refers to the header row, so start getting values at row 1.
		int index = 0;
        int row = 1;
		int col = 0;

        // an empty cell in column 0 for any row ends the while loop
		while ( true ) {

	        // get attribute values for the row starting with column index 0
			
			// category value
            cell = sheet.getCell( col, row );
            String cellContent = cell.getContents().trim();
            category.add( cellContent.length() == 0 ? 0 : Integer.valueOf( cellContent ) );
            col++;

            // body type
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            bodyType.add( cellContent );
            col++;

            // fuel type
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            fuelType.add( cellContent );
            col++;

            // update indexLookup to be able to lookup the index for all the Lists by fuelType,bodyType combination.
            Map<String,Integer> tempMap = indexLookup.get( fuelType.get(index) );
            if ( tempMap == null ) {
            	tempMap = new HashMap<>();
            	indexLookup.put( fuelType.get(index), tempMap );
            }
            tempMap.put( bodyType.get(index), index );
            
            // veh size
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            vehSize.add( cellContent );
            col++;

            // usual driver
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            usualDriver.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
            col++;
            
            // purposes
            List<Float> tempList = new ArrayList<>();
            tempList.add(0f);
            for ( int i=0; i < NUM_PURPOSES; i++ ) {
                cell = sheet.getCell( col, row );
                cellContent = cell.getContents().trim();
                tempList.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
                col++;
            }
            purposePref.add(tempList);

            // modes
            tempList = new ArrayList<>();
            for ( int i=0; i < NUM_MODES; i++ ) {
                cell = sheet.getCell( col, row );
                cellContent = cell.getContents().trim();
                tempList.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
                col++;
            }
            modePref.add(tempList);

            // driver person types
            tempList = new ArrayList<>();
            for ( int i=0; i < NUM_PERSON_TYPES; i++ ) {
                cell = sheet.getCell( col, row );
                cellContent = cell.getContents().trim();
                tempList.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
                col++;
            }
            persTypePref.add(tempList);
            
            // distance
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            distPref.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
            
            // operating cost
            cell = sheet.getCell( col, row );
            cellContent = cell.getContents().trim();
            operatingCost.add( cellContent.length() == 0 ? 0 : Float.valueOf(cellContent) );
            
            row++;
            col = 0;
            if ( sheet.getCell( col, row ).getContents().trim().length() == 0 )
            	break;

            index++;
            
        }

		logger.info( "vehicle type preferences read for " + category.size() + " categories." );
		logger.info( "distinct fuel types: " + getFuelTypes().toString() );
		logger.info( "distinct body types: " + getBodyTypes().toString() );
		logger.info( "distinct veh sizes: " + getVehSizes().toString() );
		logger.info( "" );
    }
    
    
}
