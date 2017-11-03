import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/*
 * HashMapList : Hash Table of campaign contributions keyed on FlierID
 * For each FlierID, HashMapList maintains another HashTable of contributions, by Zip Code/Transaction Date.
 * The contributions for a ZipCode are recorded in the TableOfContribs that is an inner class. TableOfContribs
 * maintains a sorted list of contributions, along with the median of contributions, total number of contributions and
 * amount collected for the zip code. 
 */
class HashMapList<T,E> {
	
	class TableOfContribs { // TableOfContribs is an inner class of HashMapList
		ArrayList<Double> contribs;
		Double[] effContribs;
		long median;
		double total;
		boolean toSort;
		int numOfContrib;
		
		public TableOfContribs(boolean toS) {
			median = -1;
			total = 0;
			toSort = toS;
			if(toSort)
				contribs = new ArrayList<Double>();
			else
				effContribs = new Double[50];
			numOfContrib = 0;
		}
		
		void incrementArray(int sz) {
			int newsz = effContribs.length + sz;
			Double arr[] = new Double[newsz];
			int pos=0;
			while(pos < effContribs.length) {
				arr[pos] = effContribs[pos++];
			}
			effContribs = arr;
		}
		
		// Add the new contribution in the right position - the contributions are recorded in ascending order
		public void add(double val) {
			numOfContrib++;
			total += val;
			if(!toSort) {
				if(numOfContrib < effContribs.length)
					effContribs[numOfContrib - 1] = val;
				else {
					//System.out.println(numOfContrib + " " + effContribs.length);
					effContribs[numOfContrib - 1] = val;
					incrementArray(50);
				}
				return;
			}
			int curr = contribs.size();
			if(curr == 0 ) contribs.add(val);
			else {
				int pos = addAt(val,0,curr -1);
				//System.out.println("Insert at: "+pos + " : "+ val + " after  "+contribs.get(0));
				contribs.add(pos, val);
			}
			//System.out.println("len: "+ contribs.size());
			setMedian();
			
		}
		
		void setMedian() {
			int mid = (numOfContrib) % 2;
			int curr = numOfContrib/2;
			if(!toSort) {
				if( mid == 0)
					median =(long)Math.round((effContribs[curr -1] + effContribs[curr])/2);
				else {
					median = (long)Math.round(effContribs[curr]);
				}
			}
			else {
				if( mid == 0)
					median =(long)Math.round((contribs.get(curr -1) + contribs.get(curr))/2);
				else {
					median = (long)Math.round(contribs.get(curr));
				}	
			}			
		}
		
		//determine where a contribution should be inserted
		int addAt(double val,int low,int high) {
			int mid=0;
			while( low <= high) {
				mid =  (low+high)/ 2;
				double middle = contribs.get(mid);
				if(middle > val) {
					high = mid -1;
				} else if(middle < val){
					low = mid+1;
				} else {
					return mid;
				}
			}
			if(low > high)
				return low;
			else return high;
		}
			
		
		// get the median contribution
		public long getMedian() {
			if(!toSort) {
				Arrays.sort(effContribs,0, numOfContrib-1);
				setMedian();
			}
			return median;
		}
		
		// get the total amount of contributions
		public long getTotalContributions() {
			return (long)Math.round(total);
		}
		
		// get the total number of contributions
		public int getNumberOfContributions() {
			return numOfContrib;
		}
	}
	
	//HashMap that stored contributions by FlierId
	private HashMap<T,HashMap<E,TableOfContribs>> map = new HashMap<T, HashMap<E,TableOfContribs>>();
	boolean isSorted = false;
	
	
	public HashMapList(boolean so) {
		isSorted = so;
	}
	
	/*
	 * Insert item into list at key
	 */
	public void put(T flierId, E zipOrdate,double val) {
		HashMap<E, TableOfContribs> innerMap = null;
		if(!map.containsKey(flierId)) {
			innerMap = new HashMap<E,TableOfContribs>();
			map.put(flierId, innerMap);
		}
		innerMap = map.get(flierId);
		if(!innerMap.containsKey(zipOrdate)) {
			innerMap.put(zipOrdate,new TableOfContribs(isSorted));
			map.put(flierId, innerMap);
		}
		innerMap.get(zipOrdate).add(val);
	}
	
	
	/*
	 * Insert list of items at key
	 */
	public void put( T flierId, HashMap<E,TableOfContribs> items) {
		map.put(flierId, items);
	}
	
	/*
	 * Get list of items at key
	 */
	public HashMap<E,TableOfContribs> get( T flierId) {
		return map.get(flierId);
	}
	
	public SortedSet<E> getSorted( T flierId) {
		SortedSet<E> sortedDates = new TreeSet<E>(get(flierId).keySet());
		return sortedDates;
		//Collections.sort(arrayList, new StringDateComparator());
		//return map.get(flierId);
	}
	
	
	
	/*
	 * Check if the hashmaplist contains key
	 */
	public boolean containsKey( T flierId) {
		return map.containsKey(flierId);
	}
	
	/*
	 * Check if list at key contains value
	 */
	public boolean containsKeyValue(T flierId, E zipcode) {
		HashMap<E,TableOfContribs> hm = get(flierId);
		if( hm == null) return false;
		return hm.containsKey(zipcode);
	}
	
	/*
	 * Get the list of keys
	 */
	public Set<T> keySet() {
		return map.keySet();
	}
	
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	public SortedSet<T> sortedKeySet() {
		SortedSet<T> keys = new TreeSet<T>(map.keySet());
		return keys;
	}
	
	// For a given FlierId, find the total number of contributions from the given zip code
	public int getNumberOfContributionsByKey(T flierId,E zipOrdate) {
		return get(flierId).get(zipOrdate).getNumberOfContributions();
	}
	
	// For a given FlierId, find the total amount of contributions from the given zip code
	public long getTotalContributionsByKey(T flierId,E zipOrdate) {
		return get(flierId).get(zipOrdate).getTotalContributions();
	}
	
	// For a given FlierId, find the median of contributions from the given zip code
	public long getMedianContributionsByKey(T flierId,E zipOrdate) {
		return get(flierId).get(zipOrdate).getMedian();
	}
	
	
}

