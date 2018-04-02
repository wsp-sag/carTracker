package accessibility;

import java.io.Serializable;
import java.util.HashMap;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.MatrixType;


public class SharedDistanceMatrixData implements Serializable
{

  private static final long serialVersionUID = 1L;

  private static SharedDistanceMatrixData objInstance = null;

  public static final String MATRIX_SERVER_HOST_ADDRESS_KEY = "matrix.server.host.address";
  public static final String MATRIX_SERVER_HOST_PORT_KEY = "matrix.server.host.port";
  private static final String OP_SOV_DIST_FILE_NAME_KEY = "op.sov.dist.file"; 
  private static final String OP_SOV_DIST_TABLE_NAME_KEY = "op.sov.dist.table"; 
  private static final String OP_SOV_DIST = "op.sov.dist.table";
  private static final String OP_SOV_TIME_FILE_NAME_KEY = "op.sov.time.file"; 
  private static final String OP_SOV_TIME_TABLE_NAME_KEY = "op.sov.time.table"; 
  private static final String OP_SOV_TIME = "op.sov.time.table"; 
  private static final String PK_SOV_DIST_FILE_NAME_KEY = "pk.sov.dist.file"; 
  private static final String PK_SOV_DIST_TABLE_NAME_KEY = "pk.sov.dist.table"; 
  private static final String PK_SOV_DIST = "pk.sov.dist.table"; 
  private static final String PK_SOV_TIME_FILE_NAME_KEY = "pk.sov.time.file"; 
  private static final String PK_SOV_TIME_TABLE_NAME_KEY = "pk.sov.time.table"; 
  private static final String PK_SOV_TIME = "pk.sov.time.table"; 

  private transient MatrixDataHandlerIf matrixHandler;
  private transient MatrixDataManager mdm;	
  private final HashMap<String, String> propertyMap;
  private final GeographyManager geogManager;


  // these arrays are shared by multiple tasks in a distributed computing environment
  private float[][] opDistFromTaz;
  private float[][] opDistToTaz;
  private float[][] opTimeFromTaz;
  private float[][] opTimeToTaz;

  private float[][] pkDistFromTaz;
  private float[][] pkDistToTaz;
  private float[][] pkTimeFromTaz;
  private float[][] pkTimeToTaz;
  

  private SharedDistanceMatrixData( HashMap<String, String> propertyMap, GeographyManager geogManager ) {

    this.propertyMap = propertyMap;
    this.geogManager = geogManager;

    String matrixServerHost = propertyMap.get( MATRIX_SERVER_HOST_ADDRESS_KEY ) ;
    String matrixServerPort = propertyMap.get( MATRIX_SERVER_HOST_PORT_KEY );
    String matrixType = "tpplus";

    if ( matrixServerHost != null ) {
      MatrixDataServerRmi ms = new MatrixDataServerRmi( matrixServerHost, Integer.parseInt( matrixServerPort ), MatrixDataServer.MATRIX_DATA_SERVER_NAME );
      matrixHandler = new MatrixDataRemoteHandler( ms, MatrixType.TRANSCAD );
    }
    else {
      MatrixDataServer matrixDataServer = new MatrixDataServer();
      matrixHandler = new MatrixDataLocalHandler( matrixDataServer, MatrixType.TRANSCAD );
    }
    
    String distFilename = propertyMap.get( OP_SOV_DIST_FILE_NAME_KEY ) ;
    String distTablename = propertyMap.get( OP_SOV_DIST_TABLE_NAME_KEY ) ;
    String timeFilename = propertyMap.get( OP_SOV_TIME_FILE_NAME_KEY ) ;
    String timeTablename = propertyMap.get( OP_SOV_TIME_TABLE_NAME_KEY ) ;
    matrixHandler.registerMatrix( OP_SOV_DIST, distFilename, matrixType, distTablename );
    matrixHandler.registerMatrix( OP_SOV_TIME, timeFilename, matrixType, timeTablename );

    int maxTaz = geogManager.getMaxTaz();

    if(geogManager.getMaxExternalStation()>maxTaz)
      maxTaz = geogManager.getMaxExternalStation();

    opDistFromTaz = new float[ maxTaz+1 ][];
    opDistToTaz = new float[ maxTaz+1 ][];
    opTimeFromTaz = new float[ maxTaz+1 ][];
    opTimeToTaz = new float[ maxTaz+1 ][];

    distFilename = propertyMap.get( PK_SOV_DIST_FILE_NAME_KEY ) ;
    distTablename = propertyMap.get( PK_SOV_DIST_TABLE_NAME_KEY ) ;
    timeFilename = propertyMap.get( PK_SOV_TIME_FILE_NAME_KEY ) ;
    timeTablename = propertyMap.get( PK_SOV_TIME_TABLE_NAME_KEY ) ;
    matrixHandler.registerMatrix( PK_SOV_DIST, distFilename, matrixType, distTablename );
    matrixHandler.registerMatrix( PK_SOV_TIME, timeFilename, matrixType, timeTablename );

    //MatrixDataManager mdm = MatrixDataManager.getInstance();
    //mdm.setMatrixDataServerObject( ms );

    pkDistFromTaz = new float[ maxTaz+1 ][];
    pkDistToTaz = new float[ maxTaz+1 ][];
    pkTimeFromTaz = new float[ maxTaz+1 ][];
    pkTimeToTaz = new float[ maxTaz+1 ][];

  }


  public static synchronized SharedDistanceMatrixData getInstance( HashMap<String, String> propertyMap, GeographyManager geogManager )
  {
    if (objInstance == null) {
      objInstance = new SharedDistanceMatrixData( propertyMap, geogManager );
      return objInstance;
    }
    else {
      return objInstance;
    }
  }    

  public float[] getOffpeakDistanceFromTaz( int anchorTaz ) {
    if ( opDistFromTaz[anchorTaz] == null )
      opDistFromTaz[anchorTaz] = matrixHandler.getMatrixRow( OP_SOV_DIST, anchorTaz );        		
    return opDistFromTaz[anchorTaz];		
  }

  public float[] getOffpeakDistanceToTaz( int anchorTaz ) {
    if ( opDistToTaz[anchorTaz] == null )
      opDistToTaz[anchorTaz] = matrixHandler.getMatrixColumn( OP_SOV_DIST, anchorTaz );        		
    return opDistToTaz[anchorTaz];		
  }

  public float[] getOffpeakTimeFromTaz( int anchorTaz ) {
    if ( opTimeFromTaz[anchorTaz] == null )
      opTimeFromTaz[anchorTaz] = matrixHandler.getMatrixRow( OP_SOV_TIME, anchorTaz );        		
    return opTimeFromTaz[anchorTaz];		
  }

  public float[] getOffpeakTimeToTaz( int anchorTaz ) {
    if ( opTimeToTaz[anchorTaz] == null )
      opTimeToTaz[anchorTaz] = matrixHandler.getMatrixColumn( OP_SOV_TIME, anchorTaz );        		
    return opTimeToTaz[anchorTaz];		
  }

  public float[] getPeakDistanceFromTaz( int anchorTaz ) {
    if ( pkDistFromTaz[anchorTaz] == null )
      pkDistFromTaz[anchorTaz] = matrixHandler.getMatrixRow( PK_SOV_DIST, anchorTaz );        		
    return pkDistFromTaz[anchorTaz];		
  }

  public float[] getPeakDistanceToTaz( int anchorTaz ) {
    if ( pkDistToTaz[anchorTaz] == null )
      pkDistToTaz[anchorTaz] = matrixHandler.getMatrixColumn( PK_SOV_DIST, anchorTaz );        		
    return pkDistToTaz[anchorTaz];		
  }

  public float[] getPeakTimeFromTaz( int anchorTaz ) {
    if ( pkTimeFromTaz[anchorTaz] == null )
      pkTimeFromTaz[anchorTaz] = matrixHandler.getMatrixRow( PK_SOV_TIME, anchorTaz );        		
    return pkTimeFromTaz[anchorTaz];		
  }

  public float[] getPeakTimeToTaz( int anchorTaz ) {
    if ( pkTimeToTaz[anchorTaz] == null )
      pkTimeToTaz[anchorTaz] = matrixHandler.getMatrixColumn( PK_SOV_TIME, anchorTaz );        		
    return pkTimeToTaz[anchorTaz];		
  }

}
