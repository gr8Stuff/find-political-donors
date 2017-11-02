
import java.io.*;

//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * FindPoliticalDonors : Class that processes contributions from donors. 
 *              Agnostic of the source of the contribution records(if it is streaming from a web app or from a file)
 *              Publishes the running median of contributions/Recipients broken down by ZipCodes and Comprehensive summary broken out by contributions 
 *              and the transaction date, to output files(that was specified earlier)
 * Returns : void              
 */

public class FindPoliticalDonors {
	BufferedWriter m_outZ=null,m_outD=null;
	HashMapList<String,String> mapByZip=null;
	HashMapList<String,Date> mapByDates =null;
	
	public FindPoliticalDonors() {
		mapByZip = new HashMapList<>(true);
		mapByDates = new HashMapList<>(false);
		
	}
	
	
    /*
     * addContrib : Takes in a string, regarding one campaign contribution(structure of this record as detailed at, 
     * 			    http://classic.fec.gov/finance/disclosure/metadata/DataDictionaryContributionsbyIndividuals.shtml). 
     *              Agnostic of the source of the string(if it is streaming from a web app or from a file)
     *              Publishes the running median and total transactions and contributions 
     *              to the output file(that was set earlier)
     * Returns : void              
     */
    public void addContrib(String line) {
    		
    		// Structure of the input contribution
    		// ID(0) AMNTD_IND(1) RPT_TP TXN_PGI IMAGE_NUM TXN_TP ENTITY NAME CITY STATE ZIP(10) EMP(11) OCC(12) TXN_DT(13) TXN_AMT(14)
    		// OTHER(15) TRAN_ID(16) FILE_NUM(17)
    	    // MEMO_CD(18) MEMO_TXT(19) SUB_ID(20)
    	
    		String[] contrib = line.split("\\|");
    		System.out.println(Arrays.toString(contrib));
    		System.out.println(contrib.length);
    		
    		//1. Rule 1: Check if other_id is empty
    		if(!contrib[15].isEmpty()) return;
    		
    		//2. Rule 2: Check if CMTE_ID and TXN_AMT are valid and non-empty
    		if(contrib[0].isEmpty() || contrib[14].isEmpty()) return;
    		
    		
    		String cmteId = contrib[0];
    		double contribAmt = Double.parseDouble(contrib[14]);
    		
    		//3. Rule 3: ZipCode is not empty or less than 5 digits in length
    		String zip= contrib[10].substring(0,5);
    		if(!zip.isEmpty() && zip.length() >= 5) {
    			contrib[10] = zip;
        		mapByZip.put(cmteId, zip,contribAmt);
        		
        		/*Each line of this file should contain these fields:
        			* recipient of the contribution (or `CMTE_ID` from the input file)
        			* 5-digit zip code of the contributor (or the first five characters of the `ZIP_CODE` field from the input file)
        			* running median of contributions received by recipient from the contributor's zip code streamed in so far. Median calculations should be rounded to the whole dollar (drop anything below $.50 and round anything from $.50 and up to the next dollar) 
        			* total number of transactions received by recipient from the contributor's zip code streamed in so far
        			* total amount of contributions received by recipient from the contributor's zip code streamed in so far
        		*/
        		
        		/*System.out.print(cmteId +"---*"+zip+"---*"+"\n");
        		System.out.print(mapByZip.getMedianContributionsByZip(cmteId,zip)+"---*" +
        				mapByZip.getNumberOfContributionsByZip(cmteId, zip)+"---*"+mapByZip.getTotalContributionsByZip(cmteId, zip) );	
        		System.out.print("\n");    
        		*/
        		recordContribution(cmteId,zip, mapByZip.getMedianContributionsByKey(cmteId,zip),
        			mapByZip.getNumberOfContributionsByKey(cmteId, zip),
        			mapByZip.getTotalContributionsByKey(cmteId, zip));
        			
    		}
    		// Record the data grouped by transaction date
    		// 1. Rule 1 for MapByDates: Check if txn_date is valid
    		Date txnDt=null;
    		if( contrib[13].isEmpty() || (txnDt=toDate(contrib[13])) ==null ) return;
    		mapByDates.put(cmteId,txnDt,contribAmt);
    		System.out.print(cmteId +"---*"+txnDt.toString()+"---*"+"---*"+mapByDates.getNumberOfContributionsByKey(cmteId, txnDt)+"---*"+mapByDates.getTotalContributionsByKey(cmteId, txnDt) );	
    		System.out.print("\n");    
    }
    
    /*
     * Convert String to Date
     * Input : Takes a String
     * Tests if the date is valid
     * Output : Return the String in Date structure
     *  
     */
    Date toDate(String date) {
    		SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
    
        // declare and initialize testDate variable, this is what will hold
        // our converted string        
        Date testDate = null;
        // we will now try to parse the string into date form
        try
        {
          testDate = sdf.parse(date);
        }
        // if the format of the string provided doesn't match the format we 
        // declared in SimpleDateFormat() we will get an exception
        catch (ParseException e)
        {
          //"the date provided is in an invalid date format.";
          return null;
        }
        // dateformat.parse will accept any date as long as it's in the format
        // you defined, it simply rolls dates over, for example, december 32 
        // becomes jan 1 and december 0 becomes november 30
        // This statement will make sure that once the string 
        // has been checked for proper formatting that the date is still the 
        // date that was entered, if it's not, we assume that the date is invalid
        if (!sdf.format(testDate).equals(date)) 
        {
          //"The date provided is invalid.";
          return null;
        }
        // if we make it to here without getting an error it is assumed that
        // the date was a valid one and that it's in the proper format
        return testDate;
    }
    
    /*
     * Convert Date to String
     * Input : Takes a Date
     * 
     * Output : Return the String in the specified format
     *  
     */
    String dateToString(Date date) {
    		SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
    		//to convert Date to String, use format method of SimpleDateFormat class.
        String strDate = sdf.format(date);
        //System.out.println("Date converted to String: " + strDate);
        return strDate;
           
    }
    
    
    public void recordByDates() {
    		
    		// Get the contributions sorted in alphabetical order of recipients
    		SortedSet<String> rcpntIds = mapByDates.sortedKeySet();
    		
    		// Using Collections.sort() to get the chronological order of the contributions made to each Recipient in the alphabetically sorted key set.  
    		for (String id : rcpntIds) { 
    			// For each recepientID,
    			SortedSet<Date> txnDts = mapByDates.getSorted(id);
    			for(Date date:txnDts) {
    				try {
						m_outD.write(id + '|' + dateToString(date) + '|' + mapByDates.getMedianContributionsByKey(id,date) + '|'
								+ mapByDates.getNumberOfContributionsByKey(id, date) + '|' 
								+ mapByDates.getTotalContributionsByKey(id, date));
						m_outD.newLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
    			}
    		}
    }

	/*
	 * Record the contribution including the running median of contributions, dates etc in the file handler provided 
	 */
    void recordContribution(String id,String zip,long median,int tot, long amount) {
    		try {
				m_outZ.write(id + '|' + zip+'|'+median+'|'+tot+'|'+amount);
				m_outZ.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }
	
    public void setOutByZip(BufferedWriter out) {
		m_outZ = out;
    }
    
    public void setOutByDates(BufferedWriter out) {
    		m_outD = out;
    }
    
    // main 
    /*
     * main:entry point for the application
     * Arguments : fully qualified input file name 
     *             fully qualified output file name
     * Returns : none
     * Creates: given Output file name or by default create output/medianvals_by_zip.txt  
     */
    public static void main(String[] args) {
    	
    	    // Process arguments
    		String inPath=null;
    		String outByZipPath=null;
    		String outByDtPath=null;
    		String workPath;
    		workPath = System.getProperty("user.dir");
    		
    		// Input file name and output specified as an argument
    		// If not provided, use the default as itcont.txt.

    		if (args.length > 0) {
    			System.out.print("Args: ");
    			for (String s: args) {
    		           System.out.print(s+ "  ");         
    		    }
    			System.out.print("\n");
    			inPath = args[0];
    			outByZipPath = args[1];
    			outByDtPath = args[2];
    	    }
    		else {
    			System.out.println("Usage: FindPoliticalDonors inputFile  medianvals_by_zipFilepath  medianvals_by_dateFilepath");
    			System.out.println("Default args when no userargs are given: " + workPath + "/input/itcont.txt as input");		
    		}

    		FindPoliticalDonors donors = new FindPoliticalDonors();
    		// Input file name and output specified as an argument
    		// If not provided, use the default as itcont.txt.
    		
    		if(inPath == null || inPath.isEmpty()) {
    			inPath = workPath + "/input/itcont.txt";
    		}
    		
    		if(outByZipPath == null || outByZipPath.isEmpty()) {
    			outByZipPath = workPath+"/output/medianvals_by_zip.txt";
    		}
        		
    		if( outByDtPath == null || outByDtPath.isEmpty()) {
    			outByDtPath = workPath+"/output/medianvals_by_date.txt";
    		}
    		
    		BufferedReader br=null;
    		BufferedWriter outZ=null,outD=null;
		FileInputStream inStream = null;
		FileOutputStream outZip = null,outDate=null;
    		try {
				inStream = new FileInputStream(inPath);
				outZip = new FileOutputStream(outByZipPath);
				outDate = new FileOutputStream(outByDtPath);
						
				br = new BufferedReader(new InputStreamReader(inStream));
				outZ = new BufferedWriter(new OutputStreamWriter(outZip));
				outD = new BufferedWriter(new OutputStreamWriter(outDate));
				donors.setOutByZip(outZ);
				donors.setOutByDates(outD);
				String line = br.readLine();
				while (line != null) {
				    		donors.addContrib(line);
				    		//System.out.println(line);
				    		line = br.readLine();
				}
				donors.recordByDates();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if( outZ != null)
						try {
							outZ.close();
							if (outD != null)
	    							outD.close();
				
							if (br != null)
		    						br.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    				
			}
    }
}