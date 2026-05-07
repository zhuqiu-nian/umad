/**
 * edu.utexas.mobios.util.CLARA 2004.05.16
 *
 * Copyright Information:
 *
 * Change Log:
 * 2004.05.16: Created by Rui Mao
 * 2004.06.16: Add the general purpose version, by Rui Mao
 * 2004.08.24: Changed to use DNAEditDistanceMetric instead of mPAM250ExtendedAminoAcidsMetric, by Willard
 */
 
package algorithms.pivotselection;

//import edu.utexas.mobios.type.DoubleVector;

import db.type.IndexObject;
import db.type.Pair;
import db.type.Sequence;
import metric.Metric;
import metric.SequenceFragmentMetric;

import java.util.List;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;



/**
 * This is a msd algorithm, i.e., select centers from given dataset so that the sum of cluster radii is minimized.
 * for detail see: Kaufman, Rousseeuw, Finding groups in data, an introduction to cluster analysis.
 * There are two versions, one is general purpose, the other is for sequence fragments. The algorithms are the same, but just the 
 * interface are different. The differences are:
 *  1. the metric: general purpose one uses Metric and sequence fragment one uses SequenceFragmentMetric
 *  2. The dataset: gernal purposes one uses an array of Object, and the sequence fragment one uses an array of long
 * @author Rui Mao
 * @version 2004.06.16
 */

public class CLARAHelper
{
	private static int sampleSize = 50;
	private static final int timeK =2 ;            // each same size is sampleSize + timeK * k
	private static int sampleNumber = 20;  //number of runs of pam, i.e., number of samples to be drawn and find centers
	private static final int minDataMagnitude = 3;  //if the data set size is less than this many times of sample size, run pam directly.
	private static final int maxSampleCounter = 100; //if after maxSampleCounter*sampleSize random sampling, 
	                                                                                 //still can not get enough sample, the dataset possibly does not contain enough number of different points, '
	                                                                                 //then use linear scan to find different points.

//----------------------------------------- sequence fragment code--------------------------------------------------------------------------------//

	private static final long BASE = 2* ((long)Integer.MAX_VALUE + 1);
        
//-------------------------------------------------- general purpose code ----------------------------------------------------------------------//
	/**
	 * select k centers using CLARA algorithm, the method to be called from out side. This is the general purpose version
	 * if source data point number is smaller than center number, the source data array will be returned (no copy)
	 * if the source array do not contail enough different points, all different points will be returned, and the remain centers will be duplicate of the first one.
	 * @param metric the metric to compute distance
	 * @param data the dataset, an array of Object
	 * @param k number of centers to select
	 * @return an array of centers
	 */
	public static IndexObject[] selectCenter (Metric metric, IndexObject [] data, final int k)
	{
		if (data.length <= k)
			return data;
					
		IndexObject [] best=null; //current best set of centers
		if ( data.length <= (sampleSize + timeK*k) * minDataMagnitude)
			best = PAM(metric, data, k); //call pam directly, the lenght of return array could smaller than k if no enough different points
		else
		{	
			double bestCost = Double.POSITIVE_INFINITY; //cost of best centers, the sum of cluster radii
			double cost =0;
			IndexObject [] center = null; //current centers
		
			IndexObject [] sample = null; //new long [sampleSize + timeK * k];
		
			for (int i=0; i< sampleNumber ;i ++)
			{
				//sampling
				if (i==0)        //first round, select sample of size sampleSize + timeK*k
					sample = sampling(metric, data, sampleSize + timeK*k, new IndexObject [0]);
				else             //not first round select same of size sampleSize + (timeK-1)*k, and include the current best centers in sample
				{
					IndexObject [] temp = sampling(metric, data, sampleSize + (timeK-1)*k, best );
					sample = new IndexObject [ temp.length + best.length];
					System.arraycopy(temp, 0, sample, 0, temp.length);
					System.arraycopy(best,0, sample, temp.length, best.length);
				}
				
				if (sample.length<= k)
				{
					best = sample;
					break;
				}
			
				center = PAM(metric, sample, k);
			
				//System.out.print("one pam done. ");
			
				cost = sumRadii(metric, data, center);
			
				if (cost < bestCost)
				{
					bestCost = cost;
					best = center;
				}
			}
			
		}
		
		if (best.length<k)
		{
			IndexObject [] temp = new IndexObject[k];
			System.arraycopy(best, 0, temp, 0, best.length);
			for (int i=best.length; i< k; i++)
				temp[i] = best[0];
			
			best = temp;
		}
		
		return best;
	}
	
	/**
	 * select k centers using PAM algorithm, the general purpose version
	 * @param metric the metric to compute distance
	 * @param data the dataset, the elements will be reordered, its length should be larger than k
	 * @param k number of centers to select
	 * @return an array of centers
	 */
	public static IndexObject [] PAM (Metric metric, IndexObject [] data, final int k)
	{
		final int size = data.length;
		
		//--------------- temp variables------------------------------------------------------------------
		double dist =0, dist1=0; // a distance ,used as temp variable
		double minDistance = 0; //the min distance from a point to all centers
		int minCenter = 0;            //the center id with the min distance to the  point
		double maxDistance =0; //the max distance from a center to all points
		int maxPoint =0;             // the point id with the max distance to the center
		//------------------------------------------------------------------------------------------------------
		
		//all pair of distances among elements of data, symmetric, store the lower triangle,
		//for pair data[i], data[j], j<i, the corresponding element is distance[i*(i-1)/2 + j]
		double [] distance = new double[size*(size-1)/2];
		for (int i=1; i<size; i++)
			for (int j=0; j<i; j++)
				distance[ i*(i-1)/2 +j ] = metric.getDistance( data[i], data[j] );
		
		Random r = new Random();
		
		int [] centerIndex = new int[k]; //indexes of current centers in data
		int [] farthest = new int [k]; //indexes of the farthest point in the cluster
		
		boolean [] isCenter = new boolean[size]; //whether the element is a center
		int [] clusterID = new int [size]; // to which cluster the element belongs
		
		//initialization, select random centers, set isCenter, clusterID, and radius
		for (int i=0; i<size; i++)
			isCenter[i] = false;
		
		int counter =0; //counter of random selection.
		//select random centers	
		for (int i=0; i<k; i++)
		{
			centerIndex[i] = r.nextInt(size);
			counter =0;
			while ((isCenter[ centerIndex[i] ] || sameAsCenter(distance, centerIndex[i], centerIndex, 0, i-1)) && counter < maxSampleCounter*( 1+ i ) )
			{
				centerIndex[i] = r.nextInt(size);
				counter ++;
			}
			
			if (counter >= maxSampleCounter* (i+1))  //try linear scan to find different point
			{
				for (int j=0; j<size; j++) //linear scan
				{
					centerIndex[i] =j;
					if ( !isCenter[ centerIndex[i] ] && ! sameAsCenter(distance, centerIndex[i], centerIndex, 0, i-1)  )
					{
						i++;
						if (i>=k)
							break;
					}
				}
				
				//check whether enough points are got
				if (i<k)  //no enough point
				{
					IndexObject [] temp = new IndexObject [i];
					for (int j=0; j<i; j++)
						temp[j] = data[ centerIndex[j] ];
					return temp;
				}
				else  //enough points are selected as centers
				{
					for (int j=0;j<k; j++)
					{
						isCenter[ centerIndex[j] ] = true;
						clusterID[ centerIndex[j] ] = j;
					}
					break;
					
				}
			}
				
			isCenter[ centerIndex[i] ] = true;
			clusterID[ centerIndex[i] ] = i;
		} 
		
		for (int i=0; i<k; i++)
			farthest[i] = centerIndex[i];
		
		//set clusterID and radius
		for (int i=0; i<size; i++)
		{
			if (isCenter[i] )  //don't need to process a center
				continue;
			
			minDistance = Double.POSITIVE_INFINITY; //the min distance to a center
			minCenter = 0;                                                         //the center id with the min distance to current point
			for (int j=0; j<k; j++)
			{
				dist = getDistance(distance, i,centerIndex[j] );
				if (dist < minDistance)
				{
					minDistance = dist;
					minCenter = j;
				}
			}
			
			clusterID[i] = minCenter;
			dist = getDistance(distance, centerIndex[minCenter] , farthest[minCenter]);
			                                                                                       
			if (dist < minDistance)
				farthest[minCenter] = i;
				
		}
			
		//iteration	
		double minCost = -1; //min cost of replacing a center by a non-center
		double cost =0;
		int replaceCenter=0; //the indexes of the pair of center and point with the min cost
		int replacePoint = 0;
		int [] replaceFarthest = null;// the farthest array with min cost
		int [] replaceClusterID = null; //the clusterid array with the min cost
		int [] replaceCenterIndex = null;
		//boolean [] replaceIsCenter = null;
		int [] tempFarthest = null;  //copy of the farthest, to be modified in iteration
		int [] tempClusterID = null; //copy of the cluster id array, to be modified in iteration
		int [] tempCenterIndex = null;
		//boolean [] tempIsCenter = null;
		 
		while(minCost <0)
		{
			minCost = 0;
			for (int i=0; i<size; i++)  //i is the non-center data to be selected as center
			{
				//don't need to process a point which is already a center, or has zero distance to a center
				if (isCenter[i] || sameAsCenter(metric, data, i, centerIndex, 0, k))
					continue;
				
				for (int j=0; j<k;j++)  //j is the center to be replaced by i
				{
					tempFarthest = (int []) farthest.clone();
					tempFarthest[j] = i;
					tempClusterID = (int []) clusterID.clone();
					tempCenterIndex = (int[]) centerIndex.clone();
					tempCenterIndex[j] =i;
					//tempIsCenter = (boolean []) isCenter.clone();
					//tempIsCenter[i] = true;
					//tempIsCenter[ centerIndex[j] ] = false;
					
					/*System.out.print("temp centerindex:");
					for (int m=0; m< k ; m++)
						System.out.print(tempCenterIndex[m] + ", ");
					System.out.println();*/

					//deal with point i itself
					if (tempClusterID[i] != j) //point i is not in the cluster to be removed, remove i from its original cluster
					{
						if ( tempFarthest[ tempClusterID[i] ] == i)  // if i is the farthest in its original cluster, re-calculate the radius
						{
							maxDistance = Double.NEGATIVE_INFINITY;
							maxPoint = 0;
							for (int t=0; t<size; t++)
							{
								//only process points belonging to the cluster
								if ( (tempClusterID[t] != tempClusterID[i]) || (t==i) )
									continue;
								
								dist = getDistance(distance, t, tempCenterIndex[ tempClusterID[i] ]);
								if (dist > maxDistance)
								{
									maxDistance = dist;
									tempFarthest[ tempClusterID[i] ] = t;
								}
							}
						}
						
						tempClusterID[i] = j;
					} 
							
					//for each datapoint, check cost
					for (int t=0; t<size; t++)
					{
						//if data[t] is a center and is not the center to be replaced, don't need to process it
						if ( (isCenter[t] & t!= centerIndex[j]) || (t==i) )
							continue;
						
						if (tempClusterID[t] == j)  //process a point in the cluster to be replaced
						{
							minDistance = Double.POSITIVE_INFINITY; //the min distance to a center
							minCenter = 0;                                                         //the center id with the min distance to current point
							for (int m=0; m<k; m++)
							{
								dist = getDistance(distance, t, tempCenterIndex[m] );
								if (dist < minDistance)
								{
									minDistance = dist;
									minCenter = m;
								}
							}
			
							tempClusterID[t] = minCenter;
							dist = getDistance(distance, tempCenterIndex[minCenter] , tempFarthest[minCenter]);
			                                                                                       
							if (dist < minDistance)
								tempFarthest[minCenter] = t;
						}
						
						else                           //process a point belonging to other clusters
						{
							dist = getDistance(distance, tempCenterIndex[ tempClusterID[t] ], t );  //distance to original parent center
							dist1 = getDistance(distance, i,t);                         //distance to the new center
							
							if (dist > dist1)  //this point should be moved to the new cluster
							{
								dist = getDistance(distance, i, tempFarthest[j]);  //radius of the new cluster
								if (dist1 > dist)
									tempFarthest[j] = t;
									
								//check whether the original cluster is affected
								if ( tempFarthest[ tempClusterID[t] ]== t )
								{
									//re-calculate the radius of original cluster
									maxDistance = Double.NEGATIVE_INFINITY;
									maxPoint = 0;
									for (int m=0; m<size; m++)
									{
										//only process points belonging to the cluster
										if ( (tempClusterID[m] != tempClusterID[t]) ||  (m==t) )
											continue;
								
										dist = getDistance(distance, m, tempCenterIndex[ tempClusterID[t] ]);
										if (dist > maxDistance)
										{
											maxDistance = dist;
											tempFarthest[ tempClusterID[t] ] = m;
										}
									}
								}
							
								tempClusterID[t] = j;
							}
						}
						
					} //end of for each point, check cost
					
					//now the temp arrays are set, we can compute the cost
					cost =0;
					for (int t=0; t<k; t++)
						if( t!= j)
						{
							if (tempFarthest[t] != farthest[t])
								cost += getDistance(distance, centerIndex[t], tempFarthest[t]) - getDistance(distance, centerIndex[t], farthest[t]);
						}
						else
							cost += getDistance(distance, i, tempFarthest[j]) - getDistance(distance, centerIndex[j], farthest[j]);
							
					if (cost < minCost)
					{
						replaceClusterID = tempClusterID;
						replaceFarthest = tempFarthest;
						replaceCenterIndex = tempCenterIndex;
				
						/*System.out.print("replace centerindex:");
						for (int m=0; m< k ; m++)
							System.out.print(replaceCenterIndex[m] + ", ");
						System.out.println();*/
						
						minCost = cost;
					}
					
				} //end of loop for each cluster
				
			}//end of loop for each data point
			
			//now, a new min cost is found
			if (minCost < 0)
			{
				clusterID = replaceClusterID;
				centerIndex = replaceCenterIndex;
				
				/*System.out.print("final replace centerindex:");
				for (int i=0; i< k ; i++)
					System.out.print(replaceCenterIndex[i] + ", ");
				System.out.println();*/
				
				farthest = replaceFarthest;
				
				for (int i=0; i<size;i++)
					isCenter[i] = false;
				for (int i=0;i<k; i++)
					isCenter[ centerIndex[i] ] = true;
			}
			
		}//end of while loop
		
/*		System.out.print("pam centerindex:");
		for (int i=0; i< k ; i++)
			System.out.print(centerIndex[i] + ", ");
		System.out.println();*/
				

		IndexObject [] center = new IndexObject[k];
		for (int i=0; i<k; i++)
			center[i] = data[ centerIndex[i] ];
		
		return center;
	}
									
							
	
	/**
	 * Random sampling from an array, general purpose version
	 * @param metric used to compute distance
	 * @param data the source array, its length should be much larger that the sample size
	 * @param size sample size
	 * @param oldCenter centers that has already been selected before the sampling, the new sample should not be identical the old centers
	 * @return the sample array, whose size can be smaller than required sample size if no enough different points
	 */
	public static IndexObject [] sampling (Metric metric, IndexObject[] data, int size, IndexObject [] oldCenter)
	{
		Random r = new Random();
		boolean repeat = false;
		final int dataSize = data.length;
		int counter = 0; //counter of random selection
		
		int [] index = new int[size];
		
		int i=0;
		for ( i=0; i< size; i++ )
		{
			repeat = true;
			counter =0;
			while (repeat && (counter < maxSampleCounter*(i + oldCenter.length+ 1) ) )
			{
				counter ++;
				index[i] = r.nextInt(dataSize);
				repeat = false;
				for (int j=0; j<i; j++)
					if ( ( index[j] == index[i] ) || ( metric.getDistance( data[ index[j] ], data[ index[i] ] ) ==0 ))
					{
						repeat = true;
						break;
					}
				
				repeat = sameAsCenter(metric, data[ index[i] ], oldCenter);
			}
			
			if (counter >= maxSampleCounter*(i + oldCenter.length+ 1))
				break;
		}
			
		if (i<size)  //if no enough sample, that means counter is exceeded, then use linear scan to find different points
		{
			for (int t=0; t< data.length; t++)
			{
				index[i] = t;
				if ( ! sameAsCenter(metric, data[ index[i] ], oldCenter) )
				{
					repeat = false;
					for (int j=0; j<i; j++)
						if ( ( index[j] == index[i] ) || ( metric.getDistance( data[ index[j] ], data[ index[i] ] ) ==0 ))
						{
							repeat = true;
							break;
						}
							
					if (!repeat)
					{
						i++;
						if (i>= size )
							break; 
					}
				}
			}
		} 
		
		IndexObject [] sample = new IndexObject [i];
		for (int j=0; j<i;j++)
			sample [j] = data[ index[j] ];
		
		return sample;
	}
			
	/**
	 * compute the sum of radii of clusters for given dataset and centers, general purpose version
	 * @param metric the metric to compute distance
	 * @param data the source data set
	 * @param center the centers
	 * @return the sum of radii of clusters
	 */
	private static double sumRadii (Metric metric, IndexObject [] data, IndexObject [] center)
	{
		return sumRadii(metric, data, center, false);
	}
	
	private static double sumRadii (Metric metric, IndexObject[] data, IndexObject[] center, boolean print)
	{
		final int k = center.length;
		int [] clusterSize = null; 
		if (print)
		{
			clusterSize = new int [k];
			for (int i=0; i<k; i++)
				clusterSize[i] =0;
		} 
		double [] radius = new double[k];
		for (int i=0; i<k; i++)
			radius[i] =0;
		
		//--------------- temp variables------------------------------------------------------------------
		double dist =0; // a distance ,used as temp variable
		double minDistance = 0; //the min distance from a point to all centers
		int minCenter = 0;            //the center id with the min distance to the  point
		//------------------------------------------------------------------------------------------------------

		//set clusterID and radius
		for (int i=0; i<data.length; i++)
		{
			minDistance = Double.POSITIVE_INFINITY; //the min distance to a center
			minCenter = 0;                                                         //the center id with the min distance to current point
			for (int j=0; j<k; j++)
			{
				dist = metric.getDistance(data[i], center[j] );
				if (dist < minDistance)
				{
					minDistance = dist;
					minCenter = j;
				}
			}
			
			if (print) clusterSize[minCenter] ++;
			
			if (minDistance > radius[ minCenter])
				radius[minCenter] = minDistance;
		}
		
		dist =0; 
		for (int i=0; i< k; i++)
			dist += radius[i];
			
		if(print)
		{
			System.out.print("cluster size: "); 
			for (int i=0; i<k; i++)
				System.out.print(clusterSize[i] + ", ");
			System.out.println("  sum of radii= "+ dist);
		}
		
		return dist;
	}
	
	
	/**
	 * check whether a point has a zero distance to a center. Use a Metric to compute distance, general purpose version
	 * @param metric used to compute distance
	 * @param data the data point
	 * @param center the array containing the centers
	 * @param offset the offset of the first center in the array
	 * @param k number of centers
	 * @return true if the data point has zero distance to a center
	 */
	private static boolean sameAsCenter (Metric metric, IndexObject data,  IndexObject [] center)
	{
		boolean repeat = false;
		for (int i= 0; i< center.length; i++)
			if (metric.getDistance(data, center[i] ) == 0)
			{
				repeat = true;
				break;
			}
		
		return repeat;
	}

	/**
	 * check whether a point has a zero distance to a center. Use a Metric to compute distance, general purpose version
	 * data point and centers are represented by indexes
	 * @param metric used to compute distance
	 * @param source the data point
	 * @param center the array containing the centers
	 * @param offset the offset of the first center in the array
	 * @param k number of centers
	 * @return true if the data point has zero distance to a center
	 */
	private static boolean sameAsCenter (Metric metric, IndexObject[] source, int index, int [] centerIndex, int offset, int k)
	{
		boolean repeat = false;
		for (int i= offset; i< offset+k; i++)
			if (metric.getDistance(source[index], source [ centerIndex[i] ] ) == 0)
			{
				repeat = true;
				break;
			}
		
		return repeat;
	}
	
//----------------------------common code--------------------------------------------------------------------//	
	
	/**
	 * distance stores the lower triangle part of a symmetric distance matrix, give i, j, return the corresponding distance of element i, and element j
	 * @param distance contains the lower triangle part the a symmetric distance matrix
	 * @param i 
	 * @param j
	 * @return the distance between element i and element j
	 */
	private static double getDistance(double[] distance, int i, int j)
	{
		if (i==j)
			return 0;
			
		return (i<j)? distance[ j*(j-1)/2 +i ]: distance[ i*(i-1)/2 + j];
	}

	/**
	 * Check whether a point has a zero distance to a center. pair-wise distances is already computed and stored.
	 * data point and centers are represented by their indexes in the source data array.
	 * @param distance a double array of already computed distances, the lower triangle part of the symmetric distance matrix
	 * @param index index of the point to test
	 * @param centerIndex indexes of centers
	 * @return true if the distance to one center is 0
	 */
	private static boolean sameAsCenter(double [] distance, int index, int[] centerIndex, int first, int last)
	{
		boolean repeat = false;
		for (int i=first; i<= last; i++)
			if (getDistance(distance, index, centerIndex[i]) == 0)
			{
				repeat = true;
				break;
			}
		
		return repeat;
	}


}