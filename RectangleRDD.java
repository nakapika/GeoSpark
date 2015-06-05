package GeoSpark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import Functions.GridFileMaker;
import Functions.PartitionAssignGridRectangle;
import Functions.RectangleRangeFilter;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class RectangleRDD implements Serializable {

	private JavaRDD<Envelope> rectangleRDD;
	
	public RectangleRDD(JavaRDD<Envelope> rectangleRDD)
	{
		this.setRectangleRDD(rectangleRDD.cache());
	}
	public RectangleRDD(JavaSparkContext spark, String InputLocation)
	{
		this.setRectangleRDD(spark.textFile(InputLocation).map(new Function<String,Envelope>()
			{
			public Envelope call(String s)
			{	
				List<String> input=Arrays.asList(s.split(","));
				 Envelope envelope = new Envelope(Double.parseDouble(input.get(0)),Double.parseDouble(input.get(2)),Double.parseDouble(input.get(1)),Double.parseDouble(input.get(3)));
				 return envelope;
			}
			}).cache());
	}
	public JavaRDD<Envelope> getRectangleRDD() {
		return rectangleRDD;
	}
	public void setRectangleRDD(JavaRDD<Envelope> rectangleRDD) {
		this.rectangleRDD = rectangleRDD;
	}
	public void rePartition(Integer partitions)
	{
		this.rectangleRDD=this.rectangleRDD.repartition(partitions);
	}
	public RectangleRDD SpatialRangeQuery(Envelope envelope,Integer condition)
	{
		JavaRDD<Envelope> result=this.rectangleRDD.filter(new RectangleRangeFilter(envelope,condition));
		return new RectangleRDD(result);
	}
	public RectangleRDD SpatialRangeQuery(Polygon polygon,Integer condition)
	{
		JavaRDD<Envelope> result=this.rectangleRDD.filter(new RectangleRangeFilter(polygon,condition));
		return new RectangleRDD(result);
	}
	public Double[] boundary()
	{
		Double[] boundary = new Double[4];
		Double minLongtitude1=this.rectangleRDD.min(new RectangleXMinComparator()).getMinX();
		Double maxLongtitude1=this.rectangleRDD.max(new RectangleXMinComparator()).getMinX();
		Double minLatitude1=this.rectangleRDD.min(new RectangleYMinComparator()).getMinY();
		Double maxLatitude1=this.rectangleRDD.max(new RectangleYMinComparator()).getMinY();
		Double minLongtitude2=this.rectangleRDD.min(new RectangleXMaxComparator()).getMaxX();
		Double maxLongtitude2=this.rectangleRDD.max(new RectangleXMaxComparator()).getMaxX();
		Double minLatitude2=this.rectangleRDD.min(new RectangleYMaxComparator()).getMaxY();
		Double maxLatitude2=this.rectangleRDD.max(new RectangleYMaxComparator()).getMaxY();
		if(minLongtitude1<minLongtitude2)
		{
			boundary[0]=minLongtitude1;
		}
		else
		{
			boundary[0]=minLongtitude2;
		}
		if(minLatitude1<minLatitude2)
		{
			boundary[1]=minLatitude1;
		}
		else
		{
			boundary[1]=minLatitude2;
		}
		if(maxLongtitude1>maxLongtitude2)
		{
			boundary[2]=maxLongtitude1;
		}
		else
		{
			boundary[2]=maxLongtitude2;
		}
		if(maxLatitude1>maxLatitude2)
		{
			boundary[3]=maxLatitude1;
		}
		else
		{
			boundary[3]=maxLatitude2;
		}
		return boundary;
	}
	public SpatialPairRDD<Envelope,ArrayList<Envelope>> SpatialJoinQuery(RectangleRDD rectangleRDD,Integer Condition,Integer GridNumberHorizontal,Integer GridNumberVertical)
	{
		//Find the border of both of the two datasets---------------
		final Integer condition=Condition;
		//condition=0 means only consider fully contain in query, condition=1 means consider full contain and partial contain(overlap).
	
	/*	Double[] boundaryQueryAreaSet=new Double[4];
		Double[] boundaryTargetSet=new Double[4];
		boundaryQueryAreaSet=rectangleRDD.boundarySeeker();
		boundaryTargetSet=this.boundarySeeker();
		Double minLongtitudeQueryAreaSet=boundaryQueryAreaSet[0];
		Double maxLongtitudeQueryAreaSet=boundaryQueryAreaSet[2];
		Double minLatitudeQueryAreaSet=boundaryQueryAreaSet[1];
		Double maxLatitudeQueryAreaSet=boundaryQueryAreaSet[3];
		Double minLongtitudeTargetSet=boundaryTargetSet[0];
		Double maxLongtitudeTargetSet=boundaryTargetSet[2];
		Double minLatitudeTargetSet=boundaryTargetSet[1];
		Double maxLatitudeTargetSet=boundaryTargetSet[3];	
		System.out.println(boundaryQueryAreaSet[0]);
		System.out.println(boundaryQueryAreaSet[1]);
		System.out.println(boundaryQueryAreaSet[2]);
		System.out.println(boundaryQueryAreaSet[3]);
		System.out.println(boundaryTargetSet[0]);
		System.out.println(boundaryTargetSet[1]);
		System.out.println(boundaryTargetSet[2]);
		System.out.println(boundaryTargetSet[3]);
	*/	
		Double minLongtitudeQueryAreaSet;
		Double maxLongtitudeQueryAreaSet;
		Double minLatitudeQueryAreaSet;
		Double maxLatitudeQueryAreaSet;
		Double minLongtitudeTargetSet;
		Double maxLongtitudeTargetSet;
		Double minLatitudeTargetSet;
		Double maxLatitudeTargetSet;
		Double minLongtitude1QueryAreaSet=rectangleRDD.getRectangleRDD().min(new RectangleXMinComparator()).getMinX();
		Double maxLongtitude1QueryAreaSet=rectangleRDD.getRectangleRDD().max(new RectangleXMinComparator()).getMinX();
		Double minLatitude1QueryAreaSet=rectangleRDD.getRectangleRDD().min(new RectangleYMinComparator()).getMinY();
		Double maxLatitude1QueryAreaSet=rectangleRDD.getRectangleRDD().max(new RectangleYMinComparator()).getMinY();
		Double minLongtitude2QueryAreaSet=rectangleRDD.getRectangleRDD().min(new RectangleXMaxComparator()).getMaxX();
		Double maxLongtitude2QueryAreaSet=rectangleRDD.getRectangleRDD().max(new RectangleXMaxComparator()).getMaxX();
		Double minLatitude2QueryAreaSet=rectangleRDD.getRectangleRDD().min(new RectangleYMaxComparator()).getMaxY();
		Double maxLatitude2QueryAreaSet=rectangleRDD.getRectangleRDD().max(new RectangleYMaxComparator()).getMaxY();
		Double minLongtitude1TargetSet=this.rectangleRDD.min(new RectangleXMinComparator()).getMinX();
		Double maxLongtitude1TargetSet=this.rectangleRDD.max(new RectangleXMinComparator()).getMinX();
		Double minLatitude1TargetSet=this.rectangleRDD.min(new RectangleYMinComparator()).getMinY();
		Double maxLatitude1TargetSet=this.rectangleRDD.max(new RectangleYMinComparator()).getMinY();
		Double minLongtitude2TargetSet=this.rectangleRDD.min(new RectangleXMaxComparator()).getMaxX();
		Double maxLongtitude2TargetSet=this.rectangleRDD.max(new RectangleXMaxComparator()).getMaxX();
		Double minLatitude2TargetSet=this.rectangleRDD.min(new RectangleYMaxComparator()).getMaxY();
		Double maxLatitude2TargetSet=this.rectangleRDD.max(new RectangleYMaxComparator()).getMaxY();
		//QueryAreaSet min/max longitude and latitude
		if(minLongtitude1QueryAreaSet<minLongtitude2QueryAreaSet)
		{
			minLongtitudeQueryAreaSet=minLongtitude1QueryAreaSet;
		}
		else
		{
			minLongtitudeQueryAreaSet=minLongtitude2QueryAreaSet;
		}
		if(maxLongtitude1QueryAreaSet>maxLongtitude2QueryAreaSet)
		{
			maxLongtitudeQueryAreaSet=maxLongtitude1QueryAreaSet;
		}
		else
		{
			maxLongtitudeQueryAreaSet=maxLongtitude2QueryAreaSet;
		}
		if(minLatitude1QueryAreaSet<minLatitude2QueryAreaSet)
		{
			minLatitudeQueryAreaSet=minLatitude1QueryAreaSet;
		}
		else
		{
			minLatitudeQueryAreaSet=minLatitude2QueryAreaSet;
		}
		if(maxLatitude1QueryAreaSet>maxLatitude2QueryAreaSet)
		{
			maxLatitudeQueryAreaSet=maxLatitude1QueryAreaSet;
		}
		else
		{
			maxLatitudeQueryAreaSet=maxLatitude2QueryAreaSet;
		}
		//TargetSet min/max longitude and latitude
		if(minLongtitude1TargetSet<minLongtitude2TargetSet)
		{
			minLongtitudeTargetSet=minLongtitude1TargetSet;
		}
		else
		{
			minLongtitudeTargetSet=minLongtitude2TargetSet;
		}
		if(maxLongtitude1TargetSet>maxLongtitude2TargetSet)
		{
			maxLongtitudeTargetSet=maxLongtitude1TargetSet;
		}
		else
		{
			maxLongtitudeTargetSet=maxLongtitude2TargetSet;
		}
		if(minLatitude1TargetSet<minLatitude2TargetSet)
		{
			minLatitudeTargetSet=minLatitude1TargetSet;
		}
		else
		{
			minLatitudeTargetSet=minLatitude2TargetSet;
		}
		if(maxLatitude1TargetSet>maxLatitude2TargetSet)
		{
			maxLatitudeTargetSet=maxLatitude1TargetSet;
		}
		else
		{
			maxLatitudeTargetSet=maxLatitude2TargetSet;
		}
		//Border found
		Double minLongitude=minLongtitudeTargetSet;
		Double minLatitude=minLatitudeTargetSet;
		Double maxLongitude=maxLongtitudeTargetSet;
		Double maxLatitude=maxLatitudeTargetSet;
		if(minLongitude>minLongtitudeQueryAreaSet)
		{
			minLongitude=minLongtitudeQueryAreaSet;
		}
		if(maxLongitude<maxLongtitudeQueryAreaSet)
		{
			maxLongitude=maxLongtitudeQueryAreaSet;
		}
		if(minLatitude>minLatitudeQueryAreaSet)
		{
			minLatitude=minLatitudeQueryAreaSet;
		}
		if(maxLatitude<maxLatitudeQueryAreaSet)
		{
			maxLatitude=maxLatitudeQueryAreaSet;
		}
		/*Double[] boundaryForBoth={minLongitude,minLatitude,maxLongitude,maxLatitude};
		Tuple2<Integer,Envelope>[] gridFile=new Tuple2[GridNumberHorizontal*GridNumberVertical];
		GridFileMaker GFMaker=new GridFileMaker(boundaryForBoth,GridNumberHorizontal,GridNumberVertical);
		gridFile=GFMaker.GridFile();
		System.out.println(gridFile);*/
//Build Grid file-------------------
		
		Double[] gridHorizontalBorder = new Double[GridNumberHorizontal+1];
		Double[] gridVerticalBorder=new Double[GridNumberVertical+1];
		double LongitudeIncrement=(maxLongitude-minLongitude)/GridNumberHorizontal;
		double LatitudeIncrement=(maxLatitude-minLatitude)/GridNumberVertical;
		System.out.println(maxLongitude);
		System.out.println(minLongitude);
		System.out.println(maxLatitude);
		System.out.println(minLatitude);
		for(int i=0;i<GridNumberHorizontal+1;i++)
		{
			gridHorizontalBorder[i]=minLongitude+LongitudeIncrement*i;
		}
		for(int i=0;i<GridNumberVertical+1;i++)
		{
			gridVerticalBorder[i]=minLatitude+LatitudeIncrement*i;
		}
		//Assign grid ID to both of the two dataset---------------------
		JavaPairRDD<Integer,Envelope> TargetSetWithID=this.rectangleRDD.mapPartitionsToPair(new PartitionAssignGridRectangle(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
		JavaPairRDD<Integer,Envelope> QueryAreaSetWithID=rectangleRDD.getRectangleRDD().mapPartitionsToPair(new PartitionAssignGridRectangle(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
//Join two dataset
		JavaPairRDD<Integer, Tuple2<Iterable<Envelope>, Iterable<Envelope>>> jointSet=QueryAreaSetWithID.cogroup(TargetSetWithID).repartition((QueryAreaSetWithID.partitions().size()+TargetSetWithID.partitions().size())*2);
//Calculate the relation between one point and one query area
		JavaPairRDD<Envelope,Envelope> queryResult=jointSet.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer,Tuple2<Iterable<Envelope>, Iterable<Envelope>>>, Envelope,Envelope>()
				{

			public Iterable<Tuple2<Envelope, Envelope>> call(
					Tuple2<Integer, Tuple2<Iterable<Envelope>, Iterable<Envelope>>> t)
					throws Exception {
				ArrayList<Tuple2<Envelope, Envelope>> QueryAreaAndTarget=new ArrayList();
				Iterator<Envelope> QueryAreaIterator=t._2()._1().iterator();
				
				while(QueryAreaIterator.hasNext())
				{
					Envelope currentQueryArea=QueryAreaIterator.next();
					Iterator<Envelope> TargetIterator=t._2()._2().iterator();
					while(TargetIterator.hasNext())
					{
						Envelope currentTarget=TargetIterator.next();
						if(condition==0){
						if(currentQueryArea.contains(currentTarget))
						{
							QueryAreaAndTarget.add(new Tuple2<Envelope,Envelope>(currentQueryArea,currentTarget));
						}
						}
						else
						{
							if(currentQueryArea.intersects(currentTarget)||currentQueryArea.covers(currentTarget))
							{
								QueryAreaAndTarget.add(new Tuple2<Envelope,Envelope>(currentQueryArea,currentTarget));
							}
						}
					}
				}
				
				return QueryAreaAndTarget;
			}
	
		});
//Delete the duplicate result
		JavaPairRDD<Envelope, Iterable<Envelope>> aggregatedResult=queryResult.groupByKey();
		JavaPairRDD<Envelope,String> refinedResult=aggregatedResult.mapToPair(new PairFunction<Tuple2<Envelope,Iterable<Envelope>>,Envelope,String>()
				{

					public Tuple2<Envelope, String> call(Tuple2<Envelope, Iterable<Envelope>> t)
							{
						Integer commaFlag=0;
						Iterator<Envelope> valueIterator=t._2().iterator();
						String result="";
						while(valueIterator.hasNext())
						{
							Envelope currentTarget=valueIterator.next();
							String currentTargetString=""+currentTarget.getMinX()+","+currentTarget.getMaxX()+","+currentTarget.getMinY()+","+currentTarget.getMaxY();
							if(!result.contains(currentTargetString))
							{
								if(commaFlag==0)
								{
									result=result+currentTargetString;
									commaFlag=1;
								}
								else result=result+","+currentTargetString;
							}
						}
						
						return new Tuple2<Envelope, String>(t._1(),result);
					}
			
				});
		
		//return refinedResult;
		SpatialPairRDD<Envelope,ArrayList<Envelope>> result=new SpatialPairRDD<Envelope,ArrayList<Envelope>>(refinedResult.mapToPair(new PairFunction<Tuple2<Envelope,String>,Envelope,ArrayList<Envelope>>()
				{

			public Tuple2<Envelope, ArrayList<Envelope>> call(Tuple2<Envelope, String> t)
			{
				List<String> resultListString= Arrays.asList(t._2().split(","));
				Iterator<String> targetIterator=resultListString.iterator();
				ArrayList<Envelope> resultList=new ArrayList<Envelope>();
				while(targetIterator.hasNext())
				{
					Envelope currentTarget=new Envelope(Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()));
					resultList.add(currentTarget);
				}
				return new Tuple2<Envelope,ArrayList<Envelope>>(t._1(),resultList);
			}
			
		}));
		return result;
	}
	public SpatialPairRDD<Envelope,ArrayList<Envelope>> SpatialJoinQuery(Integer Condition,Integer GridNumberHorizontal,Integer GridNumberVertical)
	{
		//Find the border of both of the two datasets---------------
		final Integer condition=Condition;
		//condition=0 means only consider fully contain in query, condition=1 means consider full contain and partial contain(overlap).
	
		Double minLongtitudeTargetSet;
		Double maxLongtitudeTargetSet;
		Double minLatitudeTargetSet;
		Double maxLatitudeTargetSet;
		Double minLongtitude1TargetSet=this.rectangleRDD.min(new RectangleXMinComparator()).getMinX();
		Double maxLongtitude1TargetSet=this.rectangleRDD.max(new RectangleXMinComparator()).getMinX();
		Double minLatitude1TargetSet=this.rectangleRDD.min(new RectangleYMinComparator()).getMinY();
		Double maxLatitude1TargetSet=this.rectangleRDD.max(new RectangleYMinComparator()).getMinY();
		Double minLongtitude2TargetSet=this.rectangleRDD.min(new RectangleXMaxComparator()).getMaxX();
		Double maxLongtitude2TargetSet=this.rectangleRDD.max(new RectangleXMaxComparator()).getMaxX();
		Double minLatitude2TargetSet=this.rectangleRDD.min(new RectangleYMaxComparator()).getMaxY();
		Double maxLatitude2TargetSet=this.rectangleRDD.max(new RectangleYMaxComparator()).getMaxY();


		//QueryAreaSet min/max longitude and latitude
		
		//TargetSet min/max longitude and latitude
		if(minLongtitude1TargetSet<minLongtitude2TargetSet)
		{
			minLongtitudeTargetSet=minLongtitude1TargetSet;
		}
		else
		{
			minLongtitudeTargetSet=minLongtitude2TargetSet;
		}
		if(maxLongtitude1TargetSet>maxLongtitude2TargetSet)
		{
			maxLongtitudeTargetSet=maxLongtitude1TargetSet;
		}
		else
		{
			maxLongtitudeTargetSet=maxLongtitude2TargetSet;
		}
		if(minLatitude1TargetSet<minLatitude2TargetSet)
		{
			minLatitudeTargetSet=minLatitude1TargetSet;
		}
		else
		{
			minLatitudeTargetSet=minLatitude2TargetSet;
		}
		if(maxLatitude1TargetSet>maxLatitude2TargetSet)
		{
			maxLatitudeTargetSet=maxLatitude1TargetSet;
		}
		else
		{
			maxLatitudeTargetSet=maxLatitude2TargetSet;
		}
		//Border found
		Double minLongitude=minLongtitudeTargetSet;
		Double minLatitude=minLatitudeTargetSet;
		Double maxLongitude=maxLongtitudeTargetSet;
		Double maxLatitude=maxLatitudeTargetSet;
//Build Grid file-------------------
		Double[] gridHorizontalBorder = new Double[GridNumberHorizontal+1];
		Double[] gridVerticalBorder=new Double[GridNumberVertical+1];
		double LongitudeIncrement=(maxLongitude-minLongitude)/GridNumberHorizontal;
		double LatitudeIncrement=(maxLatitude-minLatitude)/GridNumberVertical;
		System.out.println(maxLongitude);
		System.out.println(minLongitude);
		System.out.println(maxLatitude);
		System.out.println(minLatitude);
		for(int i=0;i<GridNumberHorizontal+1;i++)
		{
			gridHorizontalBorder[i]=minLongitude+LongitudeIncrement*i;
		}
		for(int i=0;i<GridNumberVertical+1;i++)
		{
			gridVerticalBorder[i]=minLatitude+LatitudeIncrement*i;
		}
		//Assign grid ID to both of the two dataset---------------------
		JavaPairRDD<Integer,Envelope> TargetSetWithID=this.rectangleRDD.mapPartitionsToPair(new PartitionAssignGridRectangle(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
		JavaPairRDD<Integer,Envelope> QueryAreaSetWithID=this.rectangleRDD.mapPartitionsToPair(new PartitionAssignGridRectangle(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
//Join two dataset
		JavaPairRDD<Integer, Tuple2<Iterable<Envelope>, Iterable<Envelope>>> jointSet=QueryAreaSetWithID.cogroup(TargetSetWithID).repartition((QueryAreaSetWithID.partitions().size()+TargetSetWithID.partitions().size())*2);
//Calculate the relation between one point and one query area
		JavaPairRDD<Envelope,Envelope> queryResult=jointSet.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer,Tuple2<Iterable<Envelope>, Iterable<Envelope>>>, Envelope,Envelope>()
				{

			public Iterable<Tuple2<Envelope, Envelope>> call(
					Tuple2<Integer, Tuple2<Iterable<Envelope>, Iterable<Envelope>>> t)
					throws Exception {
				ArrayList<Tuple2<Envelope, Envelope>> QueryAreaAndTarget=new ArrayList();
				Iterator<Envelope> QueryAreaIterator=t._2()._1().iterator();
				
				while(QueryAreaIterator.hasNext())
				{
					Envelope currentQueryArea=QueryAreaIterator.next();
					Iterator<Envelope> TargetIterator=t._2()._2().iterator();
					while(TargetIterator.hasNext())
					{
						Envelope currentTarget=TargetIterator.next();
						if(condition==0){
						if(currentQueryArea.contains(currentTarget))
						{
							QueryAreaAndTarget.add(new Tuple2<Envelope,Envelope>(currentQueryArea,currentTarget));
						}
						}
						else
						{
							if(currentQueryArea.intersects(currentTarget))
							{
								QueryAreaAndTarget.add(new Tuple2<Envelope,Envelope>(currentQueryArea,currentTarget));
							}
						}
					}
				}
				
				return QueryAreaAndTarget;
			}
	
		});
//Delete the duplicate result
		JavaPairRDD<Envelope, Iterable<Envelope>> aggregatedResult=queryResult.groupByKey();
		JavaPairRDD<Envelope,String> refinedResult=aggregatedResult.mapToPair(new PairFunction<Tuple2<Envelope,Iterable<Envelope>>,Envelope,String>()
				{

					public Tuple2<Envelope, String> call(Tuple2<Envelope, Iterable<Envelope>> t)
							{
						Integer commaFlag=0;
						Iterator<Envelope> valueIterator=t._2().iterator();
						String result="";
						while(valueIterator.hasNext())
						{
							Envelope currentTarget=valueIterator.next();
							String currentTargetString=""+currentTarget.getMinX()+","+currentTarget.getMaxX()+","+currentTarget.getMinY()+","+currentTarget.getMaxY();
							if(!result.contains(currentTargetString))
							{
								if(commaFlag==0)
								{
									result=result+currentTargetString;
									commaFlag=1;
								}
								else result=result+","+currentTargetString;
							}
						}
						
						return new Tuple2<Envelope, String>(t._1(),result);
					}
			
				});
		
		//return refinedResult;
		SpatialPairRDD<Envelope,ArrayList<Envelope>> result=new SpatialPairRDD<Envelope,ArrayList<Envelope>>(refinedResult.mapToPair(new PairFunction<Tuple2<Envelope,String>,Envelope,ArrayList<Envelope>>()
				{

			public Tuple2<Envelope, ArrayList<Envelope>> call(Tuple2<Envelope, String> t)
			{
				List<String> resultListString= Arrays.asList(t._2().split(","));
				Iterator<String> targetIterator=resultListString.iterator();
				ArrayList<Envelope> resultList=new ArrayList<Envelope>();
				while(targetIterator.hasNext())
				{
					Envelope currentTarget=new Envelope(Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()));
					resultList.add(currentTarget);
				}
				return new Tuple2<Envelope,ArrayList<Envelope>>(t._1(),resultList);
			}
			
		}));
		return result;
	}
	public SpatialPairRDD<Polygon,ArrayList<Envelope>> SpatialJoinQueryWithMBR(PolygonRDD polygonRDD,Integer Condition,Integer GridNumberHorizontal,Integer GridNumberVertical)
	{
		final Integer condition=Condition;
	//Create mapping between polygons and their minimum bounding box
		JavaPairRDD<Envelope,Polygon> polygonRDDwithKey=polygonRDD.getPolygonRDD().mapToPair(new PairFunction<Polygon,Envelope,Polygon>(){
			
			public Tuple2<Envelope,Polygon> call(Polygon s)
			{
				Envelope MBR= s.getEnvelopeInternal();
				return new Tuple2<Envelope,Polygon>(MBR,s);
			}
		}).repartition(polygonRDD.getPolygonRDD().partitions().size()*2);
	//Filter phase
		RectangleRDD rectangleRDD=polygonRDD.MinimumBoundingRectangle();
		SpatialPairRDD<Envelope,ArrayList<Envelope>> filterResultPairRDD=this.SpatialJoinQuery(rectangleRDD, Condition, GridNumberHorizontal, GridNumberVertical);
		JavaPairRDD<Envelope,ArrayList<Envelope>> filterResult=filterResultPairRDD.getSpatialPairRDD();
	//Refine phase
		JavaPairRDD<Envelope, Tuple2<Iterable<Polygon>, Iterable<ArrayList<Envelope>>>> joinSet=polygonRDDwithKey.cogroup(filterResult).repartition((polygonRDDwithKey.partitions().size()+filterResult.partitions().size())*2);
		JavaPairRDD<Polygon,Envelope> RefineResult=joinSet.flatMapToPair(new PairFlatMapFunction<Tuple2<Envelope,Tuple2<Iterable<Polygon>,Iterable<ArrayList<Envelope>>>>,Polygon,Envelope>(){
			//GeometryFactory fact = new GeometryFactory();
			public Iterable<Tuple2<Polygon, Envelope>> call(Tuple2<Envelope, Tuple2<Iterable<Polygon>, Iterable<ArrayList<Envelope>>>> t){
				ArrayList<Tuple2<Polygon, Envelope>> QueryAreaAndTarget=new ArrayList();
				Iterator<Polygon> QueryAreaIterator=t._2()._1().iterator();
				
				while(QueryAreaIterator.hasNext())
				{
					Polygon currentQueryArea=QueryAreaIterator.next();
					Iterator<ArrayList<Envelope>> TargetIteratorOutLoop=t._2()._2().iterator();
					while(TargetIteratorOutLoop.hasNext())
					{
						ArrayList<Envelope> currentTargetOutLoop=TargetIteratorOutLoop.next();
						
						
						Iterator<Envelope> targetIterator=currentTargetOutLoop.iterator();
						while(targetIterator.hasNext()){
						Envelope currentTarget=targetIterator.next();
						ArrayList<Coordinate> coordinatesList = new ArrayList<Coordinate>();
						coordinatesList.add(new Coordinate(currentTarget.getMinX(),currentTarget.getMinY()));
						coordinatesList.add(new Coordinate(currentTarget.getMinX(),currentTarget.getMaxY()));
						coordinatesList.add(new Coordinate(currentTarget.getMaxX(),currentTarget.getMaxY()));
						coordinatesList.add(new Coordinate(currentTarget.getMaxX(),currentTarget.getMinY()));
						coordinatesList.add(new Coordinate(currentTarget.getMinX(),currentTarget.getMinY()));
						Coordinate[] coordinates=new Coordinate[coordinatesList.size()];
						coordinates=coordinatesList.toArray(coordinates);
						GeometryFactory fact = new GeometryFactory();
						LinearRing linear = new GeometryFactory().createLinearRing(coordinates);
						Polygon polygon = new Polygon(linear, null, fact);
						if(condition==0){
						if(currentQueryArea.contains(polygon))
						{
							QueryAreaAndTarget.add(new Tuple2<Polygon,Envelope>(currentQueryArea,currentTarget));
						}
						}
						else
						{
							if(currentQueryArea.intersects(polygon))
							{
								QueryAreaAndTarget.add(new Tuple2<Polygon,Envelope>(currentQueryArea,currentTarget));
							}
						}
						}
					}
				}
				return QueryAreaAndTarget;
			}});
		//Delete the duplicate result
				JavaPairRDD<Polygon, Iterable<Envelope>> aggregatedResult=RefineResult.groupByKey();
				JavaPairRDD<Polygon,String> refinedResult=aggregatedResult.mapToPair(new PairFunction<Tuple2<Polygon,Iterable<Envelope>>,Polygon,String>()
						{

							public Tuple2<Polygon, String> call(Tuple2<Polygon, Iterable<Envelope>> t)
									{
								Integer commaFlag=0;
								Iterator<Envelope> valueIterator=t._2().iterator();
								String result="";
								while(valueIterator.hasNext())
								{
									Envelope currentTarget=valueIterator.next();
									String currentTargetString=""+currentTarget.getMinX()+","+currentTarget.getMaxX()+","+currentTarget.getMinY()+","+currentTarget.getMaxY();
									if(!result.contains(currentTargetString))
									{
										if(commaFlag==0)
										{
											result=result+currentTargetString;
											commaFlag=1;
										}
										else result=result+","+currentTargetString;
									}
								}
								
								return new Tuple2<Polygon, String>(t._1(),result);
							}
					
						});
				//return refinedResult;
				SpatialPairRDD<Polygon,ArrayList<Envelope>> result=new SpatialPairRDD<Polygon,ArrayList<Envelope>>(refinedResult.mapToPair(new PairFunction<Tuple2<Polygon,String>,Polygon,ArrayList<Envelope>>()
				{

					public Tuple2<Polygon, ArrayList<Envelope>> call(Tuple2<Polygon, String> t)
					{
						List<String> resultListString= Arrays.asList(t._2().split(","));
						Iterator<String> targetIterator=resultListString.iterator();
						ArrayList<Envelope> resultList=new ArrayList<Envelope>();
						while(targetIterator.hasNext())
						{
				
							Envelope currentTarget=new Envelope(Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()),Double.parseDouble(targetIterator.next()));
							resultList.add(currentTarget);
						}
						return new Tuple2<Polygon,ArrayList<Envelope>>(t._1(),resultList);
					}
					
				}));
				return result;
	}
}
