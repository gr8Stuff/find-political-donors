# Submission for the Insight find-political-donors Challenge 
1. [Introduction](README.md#introduction)
2. [Approach](README.md#challenge-summary)
3. [Details of implementation](README.md#details-of-implementation)
4. [Input file](README.md#input-file)
5. [Output files](README.md#output-files)
6. [Example](README.md#example)
7. [Writing clean, scalable and well-tested code](README.md#writing-clean-scalable-and-well-tested-code)
8. [Repo directory structure](README.md#repo-directory-structure)
9. [Testing your directory structure and output format](README.md#testing-your-directory-structure-and-output-format)
10. [Instructions to submit your solution](README.md#instructions-to-submit-your-solution)
11. [FAQ](README.md#faq)

# Introduction
As a data engineer working for political consultants,we’ve been asked to help identify possible donors for a variety of upcoming election campaigns. 

The Federal Election Commission regularly publishes campaign contributions and while we don’t want to pull specific donors from those files — because using that information for fundraising or commercial purposes is illegal — we want to identify the areas (zip codes) that may be fertile ground for soliciting future donations for similar candidates. 

Because the donations may come from specific events (e.g., high-dollar fundraising dinners) but aren’t marked as such in the data, we also want to identify the time periods that are particularly lucrative so that an analyst might later correlate them to specific fundraising events.
For this challenge, we're have to take as input a file that lists campaign contributions by individual donors and distill it into two output files:

1. `medianvals_by_zip.txt`: contains a calculated running median, total dollar amount and total number of contributions by recipient and zip code

2. `medianvals_by_date.txt`: has the calculated median, total dollar amount and total number of contributions by recipient and date.


# Approach

The data can flow in from different sources such as a stream from a webapp or from a file. Similarly, the output can flow to a queue or a process that feeds into a front-end process rather than to a file. As a result, the main data processing layer that will identify donor data has to be separated from the inflow and outflow of the data pipeline has to be source and destination agnostic. 
 The code is divided into following layers,
1. Init Layer : This is the main initialization code that will:

   a. Validate the arguments that are passed into the program. This layer will default the non-mandatory arguments and 
      exit with appropriate errors if there are insufficient/invalid arguments. 
      
   b. Process the input source(s) and output destination(s) paths and names. The input source must exist in the specified location
      with appropriate  permissions. Output directories must be writable with permissions to create output files. 
      
   c. Create the appropriate readers and writers for the input and output. This can then be seemlessly adapted for 
      different  formats/types of input sources and output destinations.
      
2. Data Processing Layer : This is the main processor that will receive one contribution and record the contribution based on the rules    identified. 
3. Summary Output : This is the final layer that will perform any overall computations or summaries.
4. Clean up : This is the code where any open files are closed so the program exits gracefully. 

# Details of Implementation

1. Language of implementation : Chose Java to implement this as it is easy to integrate different input and output formats using 
   an adapter-like Pattern and feed to the main data processor class.  Java can also scale for large data files/streams.
   
2. The FindPoliticalDonors class represents the main body of the processor. The interactions between the processor and the user input
   and output are channeled through the static main() method, the entry to the application.
   
3. main() : Processes user arguments, initializes the File readers and output File writers as appropriate. 
   Instantiate the FindPoliticalDonors class. 
   FindPoliticalDonors provides Setter Methods that the main() uses to pass File Handles for 
   writing  outputs. 
   main() reads each line from the input file, mimicking streaming input and calls the addContrib() method of 
   the FindPoliticalDonors class to processes and record each incoming contribution and running medians in the  medianvals_by_zip.txt 
   At the end, main() calls the recordByDates() method of the FindPoliticalDonors class to create a chronological summary 
   of contributions, broken down by recipients.  
   
4. The processor method addContrib() of the FindPoliticalDonors class parses the incoming record and validates the incoming 
   contribution based on the specified business rules. This rejects records that fail criteria such as invalid OTHER_ID
   ( other_id is non-empty) or CMTE_ID is empty( Recipient is not specified) and other such rules. 
   
5. To support the contribution processing, broken down by recipients( each Flier ID represents a recipient), FindPoliticalDonors 
   uses two HashMap-based classes mapByZip and mapByDates.
   These two classes are based on a HashMapList class. HashMapList class represents a HashMap of HashMaps and is used for the two 
   different use cases of this project. 
   
   In order to support the two different cases and also to provide extensibility for future scenarios, HashMapList class is 
   designed as a generic class with two type parameters to represents a HashMap of HashMaps.
   
   mapByZip: In the first scenario, given a CMTE_ID(String type), it should be possible to group and retrieve the summary 
   of contributions by Zip Code(String Type) for each individual recipient. 
   A HashMapList<String,String> can be used for this. 
   mapByDates: In the second scenario, given a CMTE_ID(String type), it should be possible to get the chronological summary 
   of contributions grouping the contributions that were collected on the same day.  
   A HashMapList<String,Date> can be used for this. 
   The HashMapList class is useful to hold contributions broken down by recipients and further organized by zip codes.
   The HashMapList class is also used to classify recipient-wise contributions grouped by transaction date. 
   
 ### Performance optimizations
 
 The HashMapList class supports two modes, sorted and unsorted. 
 The sorted option is used for mapByZip class to compute the running median contributions raised from each of the Zip Codes
 corresponding to the contributions received for a recipient. 
 Under this option, the contributions are always stored in sorted fashion to enable computing the median contributions as 
 each contribution is received. 
 For this purpose, the contributions for a zip code are stored in a dynamic array list and it is relatively easy to 
 add/reorder contributions. 
 The mapByDates class does not require that each contribution is inserted in the sorted order as this is used to print a summary 
 after processing all the records.
 In this case, the HashMapList uses an array of doubles to store the contributions corresponding to a given transaction date. 
 The amount corresponding to  each record is appended in the order processed. 
 As this outputs the contributions distibuted over a sorted collection of recipients, in a chronological order, the dates 
 of contribution are used as the key of the secondary HashMap. This is stored as Date as it is far more easier to validate, compare and
 sort  Dates as compared to Strings. The Dates are finally converted to String only for the report.
 After all records are read in,the java Arrays.sort() method is called on the array before the median is computed. 
 The array can hold upto 40 contributions for the same date, but if this size is exceeded,  the array is incremented in 
 chunks of 40 elements. 
 
 #### Big O considerations
 The array-based read/write is an O(1) operation and superior to ArrayList, both in terms of performance as well as size. 
 The sort is run just once on the HashMap object of mapByDates as compared to the mapByZip in which the contributions have to always 
 be stored in sorted manner so the median can be computed for incoming contributions. 
   
   
## Input file

The Federal Election Commission provides data files stretching back years and is [regularly updated](http://classic.fec.gov/finance/disclosure/ftpdet.shtml)

For the purposes of this challenge, we’re interested in individual contributions. While you're welcome to run your program using the data files found at the FEC's website, you should not assume that we'll be testing your program on any of those data files or that the lines will be in the same order as what can be found in those files. Our test data files, however, will conform to the data dictionary [as described by the FEC](http://classic.fec.gov/finance/disclosure/metadata/DataDictionaryContributionsbyIndividuals.shtml).

Also, while there are many fields in the file that may be interesting, below are the ones that you’ll need to complete this challenge:

* `CMTE_ID`: identifies the flier, which for our purposes is the recipient of this contribution
* `ZIP_CODE`:  zip code of the contributor (we only want the first five digits/characters)
* `TRANSACTION_DT`: date of the transaction
* `TRANSACTION_AMT`: amount of the transaction
* `OTHER_ID`: a field that denotes whether contribution came from a person or an entity 

### Input file considerations

Here are some considerations to keep in mind:
1. Because we are only interested in individual contributions, we only want records that have the field, `OTHER_ID`, set to empty. If the `OTHER_ID` field contains any other value, ignore the entire record and don't include it in any calculation
2. If `TRANSACTION_DT` is an invalid date (e.g., empty, malformed), you should still take the record into consideration when outputting the results of `medianvals_by_zip.txt` but completely ignore the record when calculating values for `medianvals_by_date.txt`
3. While the data dictionary has the `ZIP_CODE` occupying nine characters, for the purposes of the challenge, we only consider the first five characters of the field as the zip code
4. If `ZIP_CODE` is an invalid zipcode (i.e., empty, fewer than five digits), you should still take the record into consideration when outputting the results of `medianvals_by_date.txt` but completely ignore the record when calculating values for `medianvals_by_zip.txt`
5. If any lines in the input file contains empty cells in the `CMTE_ID` or `TRANSACTION_AMT` fields, you should ignore and skip the record and not take it into consideration when making any calculations for the output files
6. Except for the considerations noted above with respect to `CMTE_ID`, `ZIP_CODE`, `TRANSACTION_DT`, `TRANSACTION_AMT`, `OTHER_ID`, data in any of the other fields (whether the data is valid, malformed, or empty) should not affect your processing. That is, as long as the four previously noted considerations apply, you should process the record as if it was a valid, newly arriving transaction. (For instance, campaigns sometimes retransmit transactions as amendments, however, for the purposes of this challenge, you can ignore that distinction and treat all of the lines as if they were new)
7. For the purposes of this challenge, you can assume the input file follows the data dictionary noted by the FEC for the 2015-current election years
8. The transactions noted in the input file are not in any particular order, and in fact, can be out of order chronologically

## Output files

For the two output files that your program will create, the fields on each line should be separated by a `|`

**`medianvals_by_zip.txt`**

The first output file `medianvals_by_zip.txt` should contain the same number of lines or records as the input data file minus any records that were ignored as a result of the 'Input file considerations.'

Each line of this file should contain these fields:
* recipient of the contribution (or `CMTE_ID` from the input file)
* 5-digit zip code of the contributor (or the first five characters of the `ZIP_CODE` field from the input file)
* running median of contributions received by recipient from the contributor's zip code streamed in so far. Median calculations should be rounded to the whole dollar (drop anything below $.50 and round anything from $.50 and up to the next dollar) 
* total number of transactions received by recipient from the contributor's zip code streamed in so far
* total amount of contributions received by recipient from the contributor's zip code streamed in so far

When creating this output file, you can choose to process the input data file line by line, in small batches or all at once depending on which method you believe to be the best given the challenge description. However, when calculating the running median, total number of transactions and total amount of contributions, you should only take into account the input data that has streamed in so far -- in other words, from the top of the input file to the current line. See the below example for more guidance.

**`medianvals_by_date.txt`**

Each line of this file should contain these fields:
* recipeint of the contribution (or `CMTE_ID` from the input file)
* date of the contribution (or `TRANSACTION_DT` from the input file)
* median of contributions received by recipient on that date. Median calculations should be rounded to the whole dollar (drop anything below $.50 and round anything from $.50 and up to the next dollar) 
* total number of transactions received by recipient on that date
* total amount of contributions received by recipient on that date

This second output file does not depend on the order of the input file, and in fact should be sorted alphabetical by recipient and then chronologically by date.


## Breakdown of Tests
1. test_1
   The Sample provided.
2. test_2
   Some variation of test_1 adding more zip codes and transaction dates.
3. test_3
   This test tests the behaviour of the program for large number of contributions for the same recipient(CMTE_ID), from the same 
   zip  code on the same date. The idea is to simulate a large number of transactions(> 50) for the same bucket(cmte_id, zipcode 
   and cmte_id,txn_date) and check that the median values and amount are computed right. 
4. test_4
   This tests for error conditions.
   - Other_ID not empty
   - Empty transaction amount
   - Invalid transaction amount(non numeric).
   - Empty CMTE_ID
   - Empty Zip Code
   - Zip Code < 5 digits 
   - Non-numeric zip code
   - Empty transaction date
   - Invalid transaction date

5. test_5
   Testing performance for very large combination of amounts and contributions.
   - number of contributions is high(> 500) 
   - amount of total contributions exceeds 1 Billion(> size of MAX_INTEGER)

## Instructions to run program

Usage : FindPoliticalDonors pathtoInputFile pathtoMedianValsByZip pathToMedianValsByDate

In addition to the source code, the top-most directory of your repo must include the `input` and `output` directories, and a shell script named `run.sh` that compiles and runs the program(s) that implement the required features.
Dependencies and run instructions (if any) in your `README`.

run.sh compiles and runs the java program FindPoliticalDonors with the arguments as shown below.  

##Contents of run.sh
javac -d classes ./src/*.java
java -classpath ./classes FindPoliticalDonors ./input/itcont.txt ./output/medianvals_by_zip.txt ./output/medianvals_by_date.txt


## Repo directory structure

The directory structure for your repo should look like this:

    ├── README.md 
    ├── run.sh
    ├── src
    │   └── FindPoliticalDonors.java
    |   └── HashMapList.java
    |___classes
    |   └──FindPoliticalDonors.class
    |   └──HashMapList$TableOfContribs.class
    |   └──HashMapList.class
    ├── input
    │   └── itcont.txt
    ├── output
    |   └── medianvals_by_zip.txt
    |   └── medianvals_by_date.txt
    ├── insight_testsuite
        └── run_tests.sh
        └── tests
            └── test_1
            |   ├── input
            |   │   └── itcont.txt
            |   |__ output
            |   │   └── medianvals_by_zip.txt
            |   |__ └── medianvals_by_date.txt
            ├── your-own-test
                ├── input
                │   └── your-own-input.txt
                |── output
                    └── medianvals_by_zip.txt
                    └── medianvals_by_date.txt

**Don't fork this repo*, and don't use this `README` instead of your own. The content of `src` does not need to be a single file called `find_political_donors.py`, which is only an example. Instead, you should include your own source files and give them expressive names.

## Testing your directory structure and output format

To make sure that your code has the correct directory structure and the format of the output files are correct, we have included a test script called `run_tests.sh` in the `insight_testsuite` folder.

The tests are stored simply as text files under the `insight_testsuite/tests` folder. Each test should have a separate folder with an `input` folder for `itcont.txt` and an `output` folder for output corresponding to that test.

You can run the test with the following command from within the `insight_testsuite` folder:

    insight_testsuite~$ ./run_tests.sh 

On a failed test, the output of `run_tests.sh` should look like:

    [FAIL]: test_1
    [Thu Mar 30 16:28:01 PDT 2017] 0 of 1 tests passed

On success:

    [PASS]: test_1
    [Thu Mar 30 16:25:57 PDT 2017] 1 of 1 tests passed



One test has been provided as a way to check your formatting and simulate how we will be running tests when you submit your solution. We urge you to write your own additional tests. `test_1` is only intended to alert you if the directory structure or the output for this test is incorrect.

Your submission must pass at least the provided test in order to pass the coding challenge.

## Instructions to submit your solution
* To submit your entry please use the link you received in your coding challenge invite email
* You will only be able to submit through the link one time 
* Do NOT attach a file - we will not admit solutions which are attached files 
* Use the submission box to enter the link to your github repo or bitbucket ONLY
* Link to the specific repo for this project, not your general profile
* Put any comments in the README inside your project repo, not in the submission box
* We are unable to accept coding challenges that are emailed to us 


### Github link
You should submit the URL for the top-level root of your repository. For example, this repo would be submitted by copying the URL `https://github.com/InsightDataScience/find-political-donors` into the appropriate field on the application. **Do NOT try to submit your coding challenge using a pull request**, which would make your source code publicly available.

### What should be in the input directory?
You can put any text file you want in the directory since our testing suite will replace it. Indeed, using your own input files would be quite useful for testing. The file size limit on Github is 100 MB so you won't be able to include the larger sample input files in your `input` directory.
