package mx.iteso.desi.cloud.hw2;

import java.io.IOException;
import mx.iteso.desi.cloud.*;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;

public class GeocodeMapper extends Mapper<LongWritable, Text, Text, GeocodeWritable> {

  /* TODO: Your mapper code here */
    // Set comparison points as constants
	private final static double[] PHL_COORDS = {39.88, -75.25};
	private final static double[] HOU_COORDS = {29.97, -95.35};
	private final static double[] SEA_COORDS = {47.45, -122.30};
	private final static double[] GDL_COORDS = {20.67, -103.40};
	private final static double[] MTY_COORDS = {25.65, -100.45};
	private final static double[][] ALL_COORDS = {PHL_COORDS, HOU_COORDS, SEA_COORDS, GDL_COORDS,MTY_COORDS};
        
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        
        
 // Check input
	  if(value == null) return;
	  String line = value.toString();
	  Triple triple = GeocodeDriver.parseTriple(line);
	  if(triple == null) return;
	  
	  // Get triple values
	  String dbpediaKey = triple.getSubject();
	  String predicate = triple.getPredicate();
	  String object = triple.getObject();
	  GeocodeWritable urlGeocodeWritable = new GeocodeWritable();
	  
	  // Check for type of triple
	  if(predicate.equals("http://xmlns.com/foaf/0.1/depiction")) {
		  // Triple represents an image
		  urlGeocodeWritable.set(
				  new Text(object), 
				  new DoubleWritable(0), 
				  new DoubleWritable(0)
				  );
	  }
	  else if(predicate.equals("http://www.georss.org/georss/point")) {
		  // Triple represents a geocode, check for relation to cities
		  Double[] coords = GeocodeDriver.parseCoordinates(object);
		  if(coords == null) return;
		  Geocode g = new Geocode(null, coords[0], coords[1]);
		  if(!isRelatedToCities(g)) return;
		  urlGeocodeWritable.set(
				  new Text("GEOCODE"),
				  new DoubleWritable(coords[0]), 
				  new DoubleWritable(coords[1])
				  );
	  }
	  else {
		  // Triple represents neither
		  return;
	  }

	  // Emit if value represented an image or a geocode within 5km of the cities
	  context.write(new Text(dbpediaKey), urlGeocodeWritable);	
  }
  
  // Helper function to compare a geocode g's coordinates to an array of coordinates
  private static boolean isRelatedToCities(Geocode g) {
	  for (int i = 0; i < ALL_COORDS.length; i++) {
		  double latTo = ALL_COORDS[i][0];
		  double lonTo = ALL_COORDS[i][1];
		  double distInMeters = g.getHaversineDistance(latTo, lonTo);
		  if(distInMeters <= 5000)
			  return true;
	  }
	  return false;
  }
}
