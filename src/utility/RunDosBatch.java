package utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class RunDosBatch {

    private Process process;

    
    public static void main(String[] args) {

        RunDosBatch obj = new RunDosBatch();
        obj.runBatch( "x:/jim/projects/mag/isam", "runIsam.bat" );

    }

    public void runBatch( String directoryName, String batchFileName ) {
        
        ProcessBuilder pb = new ProcessBuilder( Arrays.asList(new String[] {"c:\\windows\\system32\\cmd.exe", "/C", directoryName + "/" + batchFileName} ) );

        final File outputFile = new File(String.format("console_isam_output_%tY%<tm%<td_%<tH%<tM%<tS.txt", System.currentTimeMillis()));
        		
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);

        try {
            process = pb.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            int exitStatus = process.waitFor();
            System.out.println( batchFileName + " exitStatus = " + exitStatus );
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
}
