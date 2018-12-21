package accessibility;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;

/**
 * @author Jim Hicks
 *
 * Class for managing matrix data in a remote process and accessed by UECs using RMI.
 */
public class MatrixDataServer implements MatrixDataServerIf {

  protected transient Logger logger = Logger.getLogger(MatrixDataServer.class);

  private static final String VERSION = "1";

  // These are used if the server is started manually by running this class's main().  If so, these must be defined consistently with
  // any class that acts as a client to the server, i.e. the client must know the address and port as well.
  private static final String        MATRIX_DATA_SERVER_ADDRESS = "matrix.server.host.address";
  private static final String        MATRIX_DATA_SERVER_PORT    = "matrix.server.host.port";
  public static final String         MATRIX_DATA_SERVER_NAME    = MatrixDataServer.class.getCanonicalName();

  private static final boolean SAVE_MATRIX_DATA_IN_MAP = true;


  //private MatrixIO32BitJvm ioVm32Bit = null; 

  private HashMap<String, DataEntry> matrixEntryMap;
  private HashMap<String, Matrix> matrixMap;
  private Map<String, MatrixInfo> matrixRegistry;

  private boolean useMatrixMap;

  //private static boolean is64bit = true;

  public MatrixDataServer() {

    // start the 32 bit JVM used specifically for running matrix io classes
    //ioVm32Bit = MatrixIO32BitJvm.getInstance();

    // create the HashMap objects to keep track of matrix data read by the server
    matrixMap = new HashMap<String, Matrix>();
    matrixEntryMap = new HashMap<String, DataEntry>();
    matrixRegistry = new HashMap<String, MatrixInfo>();

  }




  public String testRemote()
  {
    logger.info("testRemote() called." );
    return String.format("testRemote() method in %s called.", this.getClass().getCanonicalName() );
  }

  public String testRemote( String remoteObjectName )
  {
    logger.info("testRemote() called by remote process: " + remoteObjectName + "." );
    return String.format("testRemote() method in %s called by %s.", this.getClass().getCanonicalName(), remoteObjectName);
  }

  public void listMatrixRegistry() {

    logger.info( String.format("%5s %20s %20s %20s %-128s", "n", "matrixIdentifier", "type", "tableName", "fileName" ) );

    int i = 0;
    for ( String key : matrixRegistry.keySet() ) {
      MatrixInfo info = matrixRegistry.get( key );
      logger.info( String.format("%5d %20s %20s %20s %-128s", i, key, info.getMatrixType(), info.getTableName(), info.getFileName() ) );
      i++;
    }

  }

  public synchronized void registerMatrix ( String matrixIdentifier, String fileName, String matrixType, String tableName ) {

    try {

      MatrixInfo info = new MatrixInfo( fileName, matrixType, tableName );
      System.out.println( "Matrix Identifier: " +  matrixIdentifier );
      if ( ! matrixRegistry.containsKey( matrixIdentifier ) ) {
        logger.info( "creating matrix registry entry for: " + matrixIdentifier );
        System.out.println( "Matrix Identifier: " +  matrixIdentifier );
        matrixRegistry.put( matrixIdentifier, info );
        Matrix m = loadMatrix( info );
        cacheMatrix ( matrixIdentifier, m );
      }

    }
    catch ( Exception e ) {
      logger.error( "exception caught in MatrixDataServer registering matrix." );
      logger.error( "     matrixIdentifier = " + matrixIdentifier );
      logger.error( "             fileName = " + fileName );
      logger.error( "           matrixType = " + matrixType );
      logger.error( "            tableName = " + tableName );
      logger.error( "", e );
    }

  }

  private synchronized void cacheMatrix ( String matrixIdentifier, Matrix m ) {
    matrixMap.put( matrixIdentifier, m );
  }

  private synchronized boolean checkRegistry( String matrixIdentifier ) {
    return matrixRegistry.containsKey( matrixIdentifier );
  }
  /**
   * Return the Matrix object identified by the DataEntry argument.  If it exists in the cache,
   * return that object.  If it does not yet exist, read the Matrix data, store in the cache, and return it.
   *
   * @param matrixEntry is an object with details about the name and location of a matrix read by a UEC object.
   * @return a Matrix object from the cached set.
   */

  public synchronized Matrix getMatrix( DataEntry matrixEntry ) {

    Matrix m;

    String name = matrixEntry.name;

    if ( matrixEntryMap.containsKey( name ) ) {
      m = matrixMap.get( name );
    }
    else {
      m = readMatrix( matrixEntry );

      if ( useMatrixMap ) {
        matrixMap.put ( name, m );
        matrixEntryMap.put( name, matrixEntry );
      }
    }

    return m;
  }

  public float[] getMatrixColumn( String matrixIdentifier, int tableCol ) {

    float[] values = null;

    if ( checkRegistry( matrixIdentifier ) ) {
      Matrix m = matrixMap.get( matrixIdentifier );
      int maxRowValue = m.getInternalNumbers().length;
      values = new float[maxRowValue+1];
      float[][] matrixValues = m.getValues();
      int colIndex = m.getInternalColumnNumber(tableCol);
      for ( int i=0; i < matrixValues.length; i++ ) {
        int rowValue = m.getExternalNumber(i);
        values[rowValue] = matrixValues[i][colIndex];
      }
    }
    else {
      throw new RuntimeException( "unregistered matrix: " + matrixIdentifier );
    }

    return values;
  }

  public float[] getMatrixRow( String matrixIdentifier, int tableRow ) {

    float[] values = null;

    if ( checkRegistry( matrixIdentifier ) ) {
      Matrix m = matrixMap.get( matrixIdentifier );
      float[][] matrixValues = m.getValues();
      int rowIndex = m.getInternalNumber(tableRow);
      float[] tempValues = matrixValues[rowIndex];
      int maxRowValue = m.getInternalNumbers().length;
      values = new float[maxRowValue+1];
      for ( int i=0; i < tempValues.length; i++ ) {
        int colValue = m.getExternalNumber(i);
        values[colValue] = tempValues[i];
      }
    }
    else {
      throw new RuntimeException( "unregistered matrix: " + matrixIdentifier );
    }

    return values;
  }
  /*
   * Read a matrix into memory.  Store a Matrix object in the matrixMap HashMap, keyed by unique matrixIdentifier
   * @param fileName is the name of the file containing the matrix to be read
   * @param matrixType is the matrix type of the matrix to be read
   * @param tableName is the matrix table name of the matrix to be read
   * @return a Matrix object
   */

  private Matrix loadMatrix( MatrixInfo info ) { 

    String fileName = info.getFileName();
    String matrixType = info.getMatrixType();
    String tableName = info.getTableName();

    Matrix matrix;

    if ( matrixType.equalsIgnoreCase("emme2") ) {
      MatrixReader mr = MatrixReader.createReader( MatrixType.EMME2, new File(fileName) );
      matrix = mr.readMatrix( tableName );
    }
    else if ( matrixType.equalsIgnoreCase("binary") ) {
      MatrixReader mr = MatrixReader.createReader( MatrixType.BINARY, new File(fileName) );
      matrix = mr.readMatrix();
    }
    else if ( matrixType.equalsIgnoreCase("zip") || matrixType.equalsIgnoreCase("zmx") ) {
      MatrixReader mr = MatrixReader.createReader( MatrixType.ZIP, new File(fileName) );
      matrix = mr.readMatrix();
    }
    else if ( matrixType.equalsIgnoreCase("tpplus") ) {
      MatrixReader mr = MatrixReader.createReader( MatrixType.TPPLUS, new File(fileName) );
      matrix = mr.readMatrix( tableName );
    }
    else if ( matrixType.equalsIgnoreCase("transcad") ) {
      MatrixReader mr = MatrixReader.createReader( MatrixType.TRANSCAD, new File(fileName) );
      matrix = mr.readMatrix( tableName );
    }
    else {
      throw new RuntimeException( "unsupported matrix type: " + matrixType );
    }


    return matrix;

  }

  /**
   * Utility method to write a set of matrices to disk.
   * 
   * @param fileName The file name to write to.
   * @param m  An array of matrices
   */
  public void writeMatrixFile(String fileName, Matrix[] m, String[] names, MatrixType mt) {

    File outFile = new File(fileName); 
    MatrixWriter writer = MatrixWriter.createWriter( mt, outFile ); 

    writer.writeMatrices(names, m);

  }

  public void clear() {
    matrixMap.clear();
    matrixEntryMap.clear();
  }


  public void setUseMatrixMap( boolean useMap ) {
    useMatrixMap = useMap;
  }


  /*
   * Read a matrix.
   *
   * @param matrixEntry a DataEntry describing the matrix to read
   * @return a Matrix
   */
  private Matrix readMatrix(DataEntry matrixEntry) {

    Matrix matrix;
    String fileName = matrixEntry.fileName;

    if (matrixEntry.format.equalsIgnoreCase("emme2")) {
      MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2, new File(fileName));
      matrix = mr.readMatrix(matrixEntry.matrixName);
    } else if (matrixEntry.format.equalsIgnoreCase("binary")) {
      MatrixReader mr = MatrixReader.createReader(MatrixType.BINARY, new File(fileName));
      matrix = mr.readMatrix();
    } else if (matrixEntry.format.equalsIgnoreCase("zip") || matrixEntry.format.equalsIgnoreCase("zmx")) {
      MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, new File(fileName));
      matrix = mr.readMatrix();
    } else if (matrixEntry.format.equalsIgnoreCase("tpplus")) {
      MatrixReader mr = MatrixReader.createReader(MatrixType.TPPLUS, new File(fileName));
      matrix = mr.readMatrix(matrixEntry.matrixName);
    } else if (matrixEntry.format.equalsIgnoreCase("transcad")) {
      MatrixReader mr = MatrixReader.createReader(MatrixType.TRANSCAD, new File(fileName));
      matrix = mr.readMatrix(matrixEntry.matrixName);
    } else {
      throw new RuntimeException("unsupported matrix type: " + matrixEntry.format);
    }


    //Use token name from control file for matrix name (not name from underlying matrix)
    matrix.setName(matrixEntry.name);

    return matrix;
  }



  public void start32BitMatrixIoServer( MatrixType mType ) {

    /*        // start the matrix I/O server process
        ioVm32Bit.startJVM32();

        // establish that matrix reader and writer classes will use the RMI versions for TPPLUS format matrices
        ioVm32Bit.startMatrixDataServer( mType );
        logger.info ("matrix data server 32 bit process started.");
     */
    logger.info ("matrix data server 32 bit process is no longer supported.");
  }

  public void stop32BitMatrixIoServer() {

    /*        // stop the matrix I/O server process
        ioVm32Bit.stopMatrixDataServer();

        // close the JVM in which the RMI reader/writer classes were running
        ioVm32Bit.stopJVM32();
        logger.info ("matrix data server 32 bit process stopped.");
     */      
    logger.info ("matrix data server 32 bit process is no longer supported.");
  }



  //    private static void usage( String[] args ) {
  //        logger.error( String.format( "improper arguments." ) );
  //        if (args.length == 0 ) {
  //            logger.error( String.format( "no properties file specified." ) );
  //            logger.error( String.format( "a properties file base name (without .properties extension) must be specified as the first argument." ) );
  //        }
  //        else if (args.length >= 1 ) {
  //            logger.error( String.format( "improper properties file specified." ) );
  //            logger.error( String.format( "a properties file base name (without .properties extension) must be specified as the first argument." ) );
  //        }
  //    }

  @SuppressWarnings("unused")
  public static void main(String args[]) throws Exception {

    ResourceBundle rb = null;
    MatrixDataServer matrixServer = null;


    // check that a properties file is specified
    if ( args.length >= 1 ) {
      // Instantiate MatrixDataManger with the given properties file 
      rb = ResourceBundle.getBundle( args[0] );		
      matrixServer = new MatrixDataServer();
    }
    else {
      System.out.println( "\ninvalid number of command line arguments." );
      System.out.println( "1 argument with the basename of the properties file must be specified." );
      System.exit(-1);
    }

    String serverAddress = rb.getString(MATRIX_DATA_SERVER_ADDRESS);
    int serverPort = Integer.valueOf( rb.getString(MATRIX_DATA_SERVER_PORT));
    String className = MATRIX_DATA_SERVER_NAME;

    String region = rb.getString("region");

    String serverLabel = region + " MatrixDataServer";

    System.out.println( "MatrixDataServer using ip:" + serverAddress + ", port:" + serverPort );




    /*
        try {

            // create the concrete data server object
            matrixServer.start32BitMatrixIoServer( MatrixType.TPPLUS );

        }
        catch ( RuntimeException e ) {
            matrixServer.stop32BitMatrixIoServer();
            logger.error ( "RuntimeException caught in com.pb.models.ctramp.MatrixDataServer.main() -- exiting.", e );
        }
     */


    // bind this concrete object with the cajo library objects for managing RMI
    boolean serverWaiting = true;
    int count = 0;
    while ( serverWaiting ) {
      try {
        Remote.config( serverAddress, serverPort, null, 0 );
        ItemServer.bind( matrixServer, className );
        serverWaiting = false;
      }
      catch ( Exception e ) {
        TimeUnit.SECONDS.sleep(1);
        e.printStackTrace();
        System.out.println( "try number" + count++ );
      }
      if ( count == 3 ) {
        throw new RuntimeException();
      }
    }


    // log that the server started

    System.out.println( String.format("%s version %s started on: %s:%d", serverLabel, VERSION, serverAddress, serverPort) );
    String doneFilePath = rb.getString("check.servers.started.path");
    PrintWriter writer = new PrintWriter(doneFilePath+"/matrixServer_done.txt", "UTF-8");

    /* TODO: delete test code
        GeographyManager geogManager = GeographyManager.getInstance();
        geogManager.setupGeographyManager( rb );
        MatrixInfo matrx = matrixServer.new MatrixInfo( rb.getString( "pk.sov.dist.file" ), "tpplus", "DISTANCE" );

        matrixServer.loadMatrix(matrx);
        matrixServer.registerMatrix("DISTANCE", rb.getString( "pk.sov.dist.file" ), "tpplus", "DISTANCE");

        float[] test = matrixServer.getMatrixRow( "DISTANCE", 12 );
        System.out.println( String.format("matrix data server is successfully set up") );
     */
  }
  class LoadTask implements Runnable {

    private String matrixIdentifier;
    private MatrixInfo info;    	

    public LoadTask( String matrixIdentifier, MatrixInfo info ) {
      this.matrixIdentifier = matrixIdentifier;
      this.info = info;
    }

    public void run() {
      Matrix m = loadMatrix( info );
      cacheMatrix( matrixIdentifier, m );

    }

  }

  class MatrixInfo {

    private final String fileName;
    private final String matrixType;
    private final String tableName;

    private MatrixInfo( String fileName, String matrixType, String tableName ) {
      this.fileName = fileName;
      this.matrixType = matrixType;
      this.tableName = tableName;
    }

    private String getFileName() {
      return fileName;
    }

    private String getMatrixType() {
      return matrixType;
    }

    private String getTableName() {
      return tableName;
    }

  }

  @Override
  public void writeMatrixFile(String arg0, Matrix[] arg1) {
    throw new RuntimeException("\"writeMatrixFile(String fileName, Matrix[] m)\" Method Not Implemented.");
  }

}