// Copyright (C) 2017 Alset Consulting Ltd.

package testing_package;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Processes database records scrapped from URL.
 * Runs in a separate thread.
 */
public class DbgAbProcessor extends Thread {

    private PrintWriter os = null;
    private PrintWriter osd = null;
    private BufferedReader is = null;

    private String firstName = "";
    private String lastName = "";
    private String titule = "";
    private String title = "";

    private String addressStreetDept = "";
    private String addressStreet0Dept = "";
    private String addressCityDept = "";
    private String addressProvDept = "";
    private String addressPCodeDept = "";

    private String addressStreet = "";
    private String addressStreet0 = "";
    private String addressCity = "";
    private String addressProv = "";
    private String addressPCode = "";

    private String telephone = "";
    private String fax = "";
    private String email = "";

    private String baseUrl = "";

    private int treeTimeout = 0;
    private int recordTimeout = 0;
    private int connectionTimeout = 0;

    private boolean consoleOutput = true;

    private int pageCount = 0;
    private int recordCount = 0;

    private String programMode = "";

    private static final char TMPCHAR = 0x1c;

    private int recursionLevel = 0;

    private Hashtable levelIds = null;

    private String deptAddressString;

    private String ministry = "";

    private String browser = "";


    /**
     * Constructor.
     *
     * @param outputFile Output file name
     * @param url        Base URL for database record
     * @param startIndex Starting index for building final URL
     * @param endIndex   Ending index for building final URL
     */
    ///////////////////////////////////////////////////////////////////////////////////
    public DbgAbProcessor(String outputFile,
                          String outputMapFile,
                          String url,
                          String treeTimeout,
                          String recordTimout,
                          String connectionTimeout,
                          boolean co,
                          String pMode,
                          String minAbrev,
                          String browser)
    ///////////////////////////////////////////////////////////////////////////////////
    {
        this.ministry = minAbrev;
        programMode = pMode;
        baseUrl = url;
        this.treeTimeout = (new Integer(treeTimeout)).intValue();
        this.recordTimeout = (new Integer(recordTimeout)).intValue();
        this.connectionTimeout = (new Integer(connectionTimeout)).intValue();
        this.browser = browser;
        consoleOutput = co;

        levelIds = new Hashtable(5000);

        try {
            if (programMode.equals("MAP") || programMode.equals("MAP+RECORD")) {
                // create output map file for writing
                os = new PrintWriter(new FileWriter(outputMapFile));
            }

            if (programMode.equals("RECORD") || programMode.equals("MAP+RECORD")) {
                // create output data file for writing
                osd = new PrintWriter(new FileWriter(outputFile));

                // write headers to file...
                osd.println("\"" + "recordCount" + "\"," +
                        "\"" + "recordId" + "\"," +
                        "\"" + "pageId" + "\"," +
                        "\"" + "timeStamp" + "\"," +
                        "\"" + "departmentTree" + "\"," +
                        "\"" + "level1Dept" + "\"," +
                        "\"" + "lowestDep" + "\"," +
                        "\"" + "firstName" + "\"," +
                        "\"" + "lastName" + "\"," +
                        "\"" + "title" + "\"," +
                        "\"" + "jobTitle" + "\"," +
                        "\"" + "address1Dept" + "\"," +
                        "\"" + "addressDept2" + "\"," +
                        "\"" + "addressCityDept" + "\"," +
                        "\"" + "addressProvDept" + "\"," +
                        "\"" + "addressPostalCodeDept" + "\"," +
                        "\"" + "address1" + "\"," +
                        "\"" + "address2" + "\"," +
                        "\"" + "addressCity" + "\"," +
                        "\"" + "addressProv" + "\"," +
                        "\"" + "addressPostalCode" + "\"," +
                        "\"" + "telephone" + "\"," +
                        "\"" + "fax" + "\"," +
                        "\"" + "email" + "\"");

                osd.flush();
            }

            if (programMode.equals("RECORD")) {
                // open input map file for reading
                is = new BufferedReader(new FileReader(outputMapFile));
            }
        } catch (IOException e) {
            System.out.print("Error while createing export file: " + e);
        } // end try

        pageCount = 0;
        recordCount = 0;
    }

    /**
     * Main processing loop.
     */
    ///////////////////////////////////////////////////////////////////////////////////
    public void run()
    ///////////////////////////////////////////////////////////////////////////////////
    {

        if (programMode.equals("RECORD")) {
            // read existing map file and process records
            readMapFile();
        } else {
            String sumLevel = "";
            // just create map file, and, optionally process records at the same time ("MAP" and "MAP+RECORD" modes)
            String currentUrl = baseUrl + "/includes/directorysearch/dsp_browse_ministry_hierarchy.cfm?item=1";
       
	        /*  
            // FIRST METHOD GETTING A WEB PAGE (COULD CONTAIN JAVASCRIPT)
	        BrowserVersion bv = null;
	  		
	  		if( browser.equals("CHROME"))
	  		{
	  		   bv = BrowserVersion.CHROME;
	  		}
	  		else if( browser.equals("EXPLORER"))
	  		{
	  		   bv = BrowserVersion.INTERNET_EXPLORER;
	  		}
	  		else if( browser.equals("FIREFOX"))
	  		{
	  		   bv = BrowserVersion.FIREFOX_45;
	  		}
	  		else if( browser.equals("EDGE"))
	  		{
	  		   bv = BrowserVersion.EDGE;
	  		}
	  		else
	  		{
	  			bv = BrowserVersion.CHROME; // default
	  		}
	  		
	  		System.out.println("==========================================================================================");
	  		System.out.println(bv.getUserAgent());
	  		System.out.println("==========================================================================================");
	  		  		  		
	  		// process top level departments first
	  		
	  		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
	        WebClient webClient = new WebClient(bv);  
	        String url = currentUrl;
	        System.out.println("run(): Loading top level departments page: " + url);
	        HtmlPage page = null;
	        try {
		  			page = webClient.getPage(url);
		  	    } catch (FailingHttpStatusCodeException e) {
		  			e.printStackTrace();
		  	    } catch (MalformedURLException e) {
		  			e.printStackTrace();
		        } catch (IOException e) {
		  			e.printStackTrace();
		  	}
	  		
	        //webClient.waitForBackgroundJavaScript(30 * 1000);
	        String pageAsXml = page.asXml();
	        System.out.println(pageAsXml);  // page in XML format
	          
	        webClient.close();
	              
	        System.out.println("******************************************************************************************");
	        String[] parts = pageAsXml.split("\n");
	        int len = parts.length;
	        for ( int i = 0; i < len; i++)
	        {
	            System.out.println("line=" + parts[i]);
	        }
	        */ 
	          
	          
	          
	       /*   
	       // SECOND METHOD GETTING A WEB PAGE (FASTER)
	       String url = currentUrl;
	       Document doc = null;
	       System.out.println("run(): Loading top level departments page: " + url);
	       
	       try 
	       {
	            doc = Jsoup.connect(url).get(); 
	       }
	       catch (IOException e) {
	           
	       }
	       
	       String page = doc.html();
	         
	       System.out.println("******************************************************************************************");
	       String[] parts = page.split("\n");
	       int len = parts.length;
	       for ( int i = 0; i < len; i++)
	       {
	           System.out.println("line=" + parts[i]);
	       }
	       */

            processFirstPage(currentUrl, sumLevel);

            // testing:
            //processPage(baseUrl + "includes/directorysearch/GOABROWSE.cfm?Ministry=" + "SRD", "ZZZ" , "" );
            //processPage("http://www.gov.ab.ca/home/includes/DirectorySearch/goaBrowse.cfm?Ministry=SRD&levelID=46411" + "LEG", "ZZZ" , "" );

        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    private void readMapFile()
    ///////////////////////////////////////////////////////////////////////////////////
    {
        String line = null;
        String recordId = null;
        String pageId = null;
        String ministry = null;
        String sumLevel = null;
        String additionalRecord = null;
        String deptAddress = null;
        String nameSurname = null;

        int k = -1;
        int k1 = -1;
        int k2 = -1;
        int k3 = -1;
        int k4 = -1;
        int k5 = -1;

        try {
            while ((line = is.readLine()) != null) {
                k = -1;
                k = line.indexOf("?");
                k1 = line.indexOf("?", k + 1);
                k2 = line.indexOf("?", k1 + 1);
                k3 = line.indexOf("?", k2 + 1);
                k4 = line.indexOf("?", k3 + 1);
                k5 = line.indexOf("?", k4 + 1);
                if (k > -1) {
                    recordCount++;

                    sumLevel = line.substring(0, k);
                    recordId = line.substring(k + 1, k1);
                    pageId = line.substring(k1 + 1, k2);
                    ministry = line.substring(k2 + 1, k3);
                    additionalRecord = line.substring(k3 + 1, k4);
                    deptAddress = line.substring(k4 + 1);
                    nameSurname = line.substring(k5 + 1);

                    if (additionalRecord.charAt(0) == 'Y') {
                        processAdditionalRecord(ministry, recordId, pageId, baseUrl + "includes/directorysearch/goabrowse.cfm?ministry=" + ministry + "&LevelID=" + pageId + "&userid=" + recordId + "#" + recordId, sumLevel, deptAddress);
                    } else {
                        processRecord(ministry, recordId, pageId, nameSurname, baseUrl + "includes/directorysearch/goabrowse.cfm?ministry=" + ministry + "&LevelID=" + pageId + "&userid=" + recordId + "#" + recordId, sumLevel, deptAddress);
                    }
                } else {
                    System.out.println("******WARNING******: readMapFile(): invalid record format ! (record='" + line + "')");
                }

            }
        } catch (IOException e) {
        }
    }

    /**
     * Removes everything between '<' and '>' characters (including these characters) from downloaded
     * web page
     *
     * @param line ASCII web page
     * @return Web page without HTML tags.
     */
    ///////////////////////////////////////////////////////////////////////////////////
    private String removeTags(String line)
    ///////////////////////////////////////////////////////////////////////////////////
    {
        StringBuffer tempLine = new StringBuffer(" ");
        int len = line.length();
        int i = 0;
        // nigel bug: int startIndex = 0;
        int startIndex = -1;

        int k = line.indexOf("<");
        if (k < 0) {
            return line;
        }

        for (i = 0; i < len; i++) {
            if (line.charAt(i) == '<' && i > 0) {
                tempLine.append(" ").append(line.substring(startIndex + 1, i));
            }
            if (line.charAt(i) == '>') {
                startIndex = i;
            }
        } // end for

        return tempLine.toString();
    }

    ///////////////////////////////////////////////////////////////////////////////////
    private String removeTagsExceptBR(String line)
    ///////////////////////////////////////////////////////////////////////////////////
    {
        StringBuffer tempLine = new StringBuffer(" ");
        int len = line.length();
        int i = 0;

        int startIndex = -1;

        int k = line.indexOf("<");
        if (k < 0) {
            return line;
        }

        for (i = 0; i < len; i++) {
            if (line.charAt(i) == '<' && i > 0) {
                if (len > (i + 3) &&
                        (line.charAt(i + 1) == 'B' || line.charAt(i + 1) == 'b') &&
                        (line.charAt(i + 2) == 'R' || line.charAt(i + 2) == 'r') &&
                        (line.charAt(i + 3) == '>')
                        ) {
                    tempLine.append(" ").append(line.substring(startIndex + 1, i));
                    tempLine.append(" <BR>");
                    i = i + 3;
                    startIndex = i;
                } else {
                    tempLine.append(" ").append(line.substring(startIndex + 1, i));
                }
            }
            if (line.charAt(i) == '>') {
                startIndex = i;
            }
        } // end for

        return tempLine.toString();
    }

    private void processFirstPage(String pageUrl, String sumLevel) {
        Vector itemList = new Vector(500);
        Vector levelList = new Vector(500);
        boolean inStreamClosed = true;

        recursionLevel++;

        try {
            Thread.sleep(this.treeTimeout);
        } catch (InterruptedException e) {
            System.out.println("ProcessPage():  InterruptedException in sleep");
        }

        pageCount++;
        System.out.println(pageCount + " ****** new page, recursion level =" + recursionLevel + ", " + pageUrl + "*****");

        boolean startChecking = false;

        try {
            Document doc = Jsoup.connect(pageUrl).get();
            String htmlPage = doc.html();
            String[] parts = htmlPage.split("\n");
            int partsLen = parts.length;

            String line = "";
            int k = -1;

            // read record's web page - only the lines we might be interested in
            for (int i = 0; i < partsLen; i++) {
                line = parts[i];
                line = line.replace("&amp;", "&");
                //System.out.println("----------------------------------\nline="+ line);

                if (startChecking == false) {
                    k = -1;
                    k = line.indexOf("<!-- OUTPUT Organizational Units -->");
                    if (k > -1) {
                        startChecking = true;
                    }

                    continue;
                }

                k = -1;
                k = line.indexOf("&varExpandID=-1\" id=\"");
                if (k > -1) {
                    int k1 = line.indexOf("&Ministry=");

                    // new tree branch
                    int ind1 = k;
                    int ind2 = k1 + 10;
                    String val = line.substring(ind2, ind1);

                    String level = removeTags(line).trim();

                    // filter through input parameter if existent ( if not, do not filter )
                    // ministry can contain multiple department ids separated by spaces
                    if (ministry.length() > 0) {
                        StringTokenizer st = new StringTokenizer(ministry);

                        while (st.hasMoreTokens()) {
                            String tmpMinistry = st.nextToken();
                            if (tmpMinistry.equals(val)) {
                                itemList.addElement(val);
                                levelList.addElement(level);
                            }
                        }

                    } else {
                        itemList.addElement(val);
                        levelList.addElement(level);
                    }
                    continue;
                } else {
                    k = -1;
                    k = line.indexOf("Back to Search Page");
                    if (k > -1) {
                        break;
                    }
                    continue;
                }

            } // end while

            int jj = 0;
            int vectorSize = itemList.size();
            for (jj = 0; jj < vectorSize; jj++) {
                String deptAddress = "";

                System.out.println((baseUrl + "includes/directorysearch/GOABROWSE.cfm?Ministry=" + (String) itemList.elementAt(jj) + "   level=" + (String) levelList.elementAt(jj)));
                processPage(baseUrl + "includes/directorysearch/GOABROWSE.cfm?Ministry=" + (String) itemList.elementAt(jj), (String) levelList.elementAt(jj), deptAddress);
                // break; // for testing
            }
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("***** processFirstPage(): timeout, process again..... ********");

            processFirstPage(pageUrl, sumLevel);
        } finally {

        }
    }

    private void processPage(String pageUrl, String sumLevel, String deptAddress) {
        Vector itemList = new Vector(500);
        Vector levelList = new Vector(500);
        boolean inStreamClosed = true;

        recursionLevel++;

        try {
            Thread.sleep(this.treeTimeout);
        } catch (InterruptedException e) {
            System.out.println("ProcessPage():  InterruptedException in sleep");
        }

        pageCount++;
        System.out.println(pageCount + " ****** new page, recursion level =" + recursionLevel + ", " + pageUrl + "*****");
        BufferedReader inStream = null;
        boolean startChecking = false;
        boolean processingTree = false;
        boolean processingEmployee = false;

        try {
            Document doc = Jsoup.connect(pageUrl).get();
            String htmlPage = doc.html();
            String[] parts = htmlPage.split("\n");
            int partsLen = parts.length;

            String line = "";

            boolean firstPass = false;

            Vector htmlLines = new Vector(10000);
            for (int i = 0; i < partsLen; i++) {
                line = parts[i];
                line = line.replace("&amp;", "&");
                //System.out.println("Second Level line="+ line);
                htmlLines.addElement(line);
            }

            // read record's web page - only the lines we might be interested in
            int jhtml = 0;
            int htmlLinesSize = htmlLines.size();
            for (jhtml = 0; jhtml < htmlLinesSize; jhtml++) {
                line = (String) htmlLines.elementAt(jhtml);

                int k = -1;
                int k1 = -1;
                if (startChecking == false) {
                    k = line.indexOf("* START HTML TREE *");
                    k1 = line.indexOf("* START Employee Listing *");
                    if (k > -1) {
                        // start checking
                        startChecking = true;
                        processingTree = true;
                    }
                    if (k1 > -1) {
                        // start checking
                        startChecking = true;
                        processingEmployee = true;
                    }
                    continue;
                }

                k = -1;
                k = line.indexOf("* END HTML TREE *");
                if (k > -1) {
                    // stop checking
                    startChecking = false;
                    processingTree = false;
                    continue;
                }

                k = -1;
                k = line.indexOf("magglass-ai.gif");
                if (k > -1) {
                    // stop checking - exit loop
                    processingEmployee = false;
                    break;
                }

                if (processingTree == true) {
                    k = -1;
                    k1 = -1;

                    k = line.indexOf("&Ministry=");
                    k1 = line.indexOf("levelID=");

                    if (k > -1 && k1 > -1) {
                        int ind1 = k + 10;
                        int ind2 = line.indexOf("\"", ind1);
                        int ind3 = line.indexOf("&", ind1);

                        if (ind1 >= 0 && ind2 >= 0 && ind3 >= 0) {

                        } else {
                            continue;
                        }

                        String val = line.substring(ind1, ind2);
                        String ministry = line.substring(ind1, ind3);

                        ind1 = k1 + 8;
                        ind2 = line.indexOf("\"", ind1);
                        String levelId = line.substring(ind1, ind2);

                        String levelIdsKey = ministry + levelId;
                        String levelIdsValue = (String) levelIds.get(levelIdsKey);
                        if (levelIdsValue == null) {
                            // level not found, process it and add to hash
                            levelIds.put(levelIdsKey, levelIdsKey);

                            String level = removeTags(line).trim();
                            itemList.addElement(val);
                            levelList.addElement((sumLevel + TMPCHAR + level));
                        } else {
                            // building found in hash, do not process
                        }

                        continue;
                    } else {
                        continue;
                    }

                } // end processing tree

                if (processingEmployee == true) {
                    k = -1;
                    k = line.toLowerCase().indexOf(("HREF=\"goaBrowse.cfm?txtSearch=&Ministry=").toLowerCase());
                    if (k > -1) {
                        int ind1 = k + 40;
                        int ind3 = line.indexOf("&", ind1);
                        String ministry = line.substring(ind1, ind3);

                        k = line.indexOf("LevelID=", ind3);
                        ind1 = k + 8;
                        ind3 = line.indexOf("&", ind1);
                        String levelId = line.substring(ind1, ind3);

                        k = line.indexOf("userid=", ind3);
                        ind1 = k + 7;
                        ind3 = line.indexOf("#", ind1);
                        String userId = line.substring(ind1, ind3);

                        String nameSurname = removeTags(line).trim();

                        System.out.println("====>>>LEVEL:  sumLevel=" + sumLevel);
                        System.out.println("====>>>RECORD: min=" + ministry + " level=" + levelId + " userid=" + userId + " name=" + nameSurname);

                        recordCount++;
                        if (consoleOutput) {
                            //System.out.println( "====>>>LEVEL:  sumLevel=" + sumLevel );
                            System.out.println("====>>>RECORD: " + recordCount + " min=" + ministry + " level=" + levelId + " userid=" + userId + " name=" + nameSurname);
                        }
                        os.println(sumLevel + "?" + userId + "?" + levelId + "?" + ministry + "?N" + "?" + deptAddress + "?" + nameSurname);
                        os.flush();
                        if (programMode.equals("MAP+RECORD")) {
                            processRecord(ministry, userId, levelId, nameSurname, baseUrl + "includes/directorysearch/goaBrowse.cfm?txtSearch=&Ministry=" + ministry + "&LevelID=" + levelId + "&userid=" + userId + "#" + userId, sumLevel, deptAddress);
                        }

                    } else {

                    }   // end if <A name=

                } // end processing employee


            }   // end for


            int jj = 0;
            int vectorSize = itemList.size();
            for (jj = 0; jj < vectorSize; jj++) {
                System.out.println(("NEW LEVEL:  " + baseUrl + "includes/directorysearch/GOABROWSE.cfm?Ministry=" + (String) itemList.elementAt(jj) + "   level=" + (String) levelList.elementAt(jj)));
                processPage(baseUrl + "includes/directorysearch/GOABROWSE.cfm?Ministry=" + (String) itemList.elementAt(jj), (String) levelList.elementAt(jj), deptAddress);
                //break; // for testing
            }

            recursionLevel--;
            System.gc();


        } catch (StringIndexOutOfBoundsException eob) {
            eob.printStackTrace();
            System.out.println("     ***** String index out of range, skipping this page... ********");

            recursionLevel--;
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("***** processPage():  timeout, process again..... ********");
            recursionLevel--;
            pageCount--;
            processPage(pageUrl, sumLevel, deptAddress);
        } finally {

        }
    }

    private void processRecord(String ministry, String recordId, String pageId, String nameSurname, String pageUrl, String sumLevel, String deptAddress) {
        firstName = "";
        lastName = "";
        titule = "";
        title = "";
        addressStreet = "";
        addressStreet0 = "";
        addressCity = "";
        addressProv = "";
        addressPCode = "";
        addressStreetDept = "";
        addressStreet0Dept = "";
        addressCityDept = "";
        addressProvDept = "";
        addressPCodeDept = "";
        telephone = "";
        fax = "";
        email = "";

        int lineCount = 0;
        boolean pCodeFound = false;
        boolean lastNameExists = false;
        Vector lineList = new Vector(100);

        try {
            Thread.sleep(this.recordTimeout);
        } catch (InterruptedException e) {
            System.out.println("ProcessRecord():  InterruptedException in sleep");
        }

        System.out.println("     new record " + pageUrl + " sum level=" + sumLevel);
        String webPage = new String("");
        BufferedReader inStream = null;
        boolean startChecking = false;

        try {
            Document doc = Jsoup.connect(pageUrl).get();
            String htmlPage = doc.html();
            String[] parts = htmlPage.split("\n");
            int partsLen = parts.length;

            String line = "";

            boolean processingEmployee = false;

            Vector htmlLines = new Vector(10000);
            for (int i = 0; i < partsLen; i++) {
                line = parts[i];
                line = line.replace("&amp;", "&");
                htmlLines.addElement(line);
                //if(recordCount == 4) System.out.println("recordCount=" + recordCount + "line=" + line);
            }

            String tmpSumLevel = (sumLevel.substring(0)).replace(TMPCHAR, '?');
            String level1Dept = "";
            String lowestDept = "";

            // extract the first level of department description out of tmpSumLevel
            int k1 = tmpSumLevel.indexOf("?");
            if (k1 > -1) {
                level1Dept = tmpSumLevel.substring(0, k1);
            } else {
                level1Dept = tmpSumLevel;
            }

            // extract the last (high) level of department description out of tmpSumLevel
            k1 = tmpSumLevel.lastIndexOf("?");
            if (k1 > -1) {
                lowestDept = tmpSumLevel.substring(k1 + 1);
            } else {
                lowestDept = tmpSumLevel;
            }

            // read record's web page - only the lines we might be interested in
            int jhtml = 0;
            int htmlLinesSize = htmlLines.size();
            for (jhtml = 0; jhtml < htmlLinesSize; jhtml++) {
                line = (String) htmlLines.elementAt(jhtml);

                // example of data
                // <td> <a name="17372"></a> <b>Wanner, Robert, Hon.</b><br> Speaker <br> Office of the Speaker <p> Legislative Branch <br> 325 Legislature Building<br> 10800 - 97 Avenue<br> Edmonton, AB<br> T5K 2B6<br><br> <b>Phone:</b> 780 427-2464 <br> <b>Fax:</b> 780 422-9553 <br> <b>E-mail:</b> <font face="Arial, Helvetica, sans-serif" size="2"> <a href="mailto:robert.wanner@assembly.ab.ca">robert.wanner@assembly.ab.ca</a> </font><br> <style type="text/css">

                int k = -1;
                if (startChecking == false) {
                    k = line.indexOf("<!-- Display records-->");
                    if (k > -1) {
                        // start checking
                        startChecking = true;
                    }
                    continue;
                }

                k = line.toUpperCase().indexOf(("</TD>").toUpperCase());
                if (k > -1) {
                    // stop checking - exit loop
                    break;
                }

                k = line.toUpperCase().indexOf(("<A HREF=\"mailto:").toUpperCase());
                int z = line.toUpperCase().indexOf(recordId.toUpperCase());
                if (k > -1 || z > -1) {
                    int ind1;

                    System.out.println("*** DATA RECORD whole line ***=" + line);

                    if (k > -1) {
                        ind1 = k + 16;
                        k = line.toUpperCase().indexOf(("\"").toUpperCase(), ind1);
                        if (k > -1) {
                            email = line.substring(ind1, k).trim();
                        }
                    } else {
                        email = "";
                    }

                    int endOfAddressIndex = -1;

                    // we already got email at this point but will start searching for end of address
                    // get start of email search pattern
                    k = -1;
                    k = line.toUpperCase().indexOf(("<b>E-mail:</b>").toUpperCase());
                    if (k > -1) {
                        endOfAddressIndex = k - 1; // one character BEFORE start of email search pattern
                    }

                    // get fax number
                    k = -1;
                    k = line.toUpperCase().indexOf(("<b>Fax:</b>").toUpperCase());
                    if (k > -1) {
                        ind1 = k + 11;
                        k = line.toUpperCase().indexOf(("<br>").toUpperCase(), ind1);
                        if (k > -1) {
                            fax = line.substring(ind1, k).trim();

                            //endOfAddressIndex = ind1; // will probably later be overwritten with some value closer to the actual end of address
                            endOfAddressIndex = ind1 - 11 - 1; // one character BEFORE start of fax search pattern, will probably later be overwritten with some value closer to the actual end of address
                        }
                    }

                    // get phone number
                    k = -1;
                    k = line.toUpperCase().indexOf(("<b>Phone:</b>").toUpperCase());
                    if (k > -1) {
                        ind1 = k + 13;
                        k = line.toUpperCase().indexOf(("<br>").toUpperCase(), ind1);
                        if (k > -1) {
                            telephone = line.substring(ind1, k).trim();

                            //endOfAddressIndex = ind1; // will probably later be overwritten with some value closer to the actual end of address
                            endOfAddressIndex = ind1 - 13 - 1; // one character BEFORE start of phone search pattern, will probably later be overwritten with some value closer to the actual end of address
                        }
                    }

                    // extract name, first name, titule (if it exists)
                    String tmp = nameSurname;

                    k = -1;
                    k = tmp.indexOf(",");
                    if (k > -1) {
                        if (tmp.length() >= (k + 2 + 1)) {
                            firstName = tmp.substring(k + 2);

                            if (firstName.length() > 0) {
                                // possibly separate the title (example: The Hon. or MLA )
                                int n = -1;
                                n = firstName.indexOf(" ");
                                int n1 = firstName.indexOf(",");
                                if (n > -1 || n1 > -1) {
                                    if (n1 > -1) {
                                        n = n1;
                                    }

                                    if (firstName.length() > n + 1 + 1) {
                                        titule = firstName.substring(n + 1);
                                    }
                                    firstName = firstName.substring(0, n);
                                }
                            }
                        }
                        lastName = tmp.substring(0, k);
                        lastNameExists = true;
                    }

                    if (firstName.length() == 0 && lastName.length() == 0 && titule.length() == 0) {
                        // leave all other fields empty
                    } else {

                        // find title
                        k = line.toUpperCase().indexOf(nameSurname.toUpperCase());
                        if (k > -1) {
                            ind1 = k + nameSurname.length() + 8; // <B>Wanner, Robert, Hon.</B><BR> Speaker <BR>
                            int ind2 = line.toUpperCase().indexOf(("<br>").toUpperCase(), ind1);
                            title = removeTags(line.substring(ind1, ind2).trim()).trim();
                        }

                        // find the part that contains address0 and address1

                        int startOfAddressIndex = -1;
                        k = -1;
                        k = line.toUpperCase().indexOf(level1Dept.toUpperCase());
                        if (k > -1) {
                            k1 = -1;
                            k1 = line.toUpperCase().indexOf(("<br>").toUpperCase(), k);
                            if (k1 > -1) {
                                startOfAddressIndex = k1 + 4; // start of address information
                            } else {
                                startOfAddressIndex = k; // start of level1Dept
                            }
                        } else {
                            startOfAddressIndex = 0; // start of line
                        }

                        addressPCode = containsPCode(line);


                        k = line.toUpperCase().indexOf((", AB<br>").toUpperCase());
                        if (k > -1) {
                            endOfAddressIndex = k; // end of address information
                            addressProv = "AB";

                            tmp = line.substring(0, k);

                            z = tmp.toUpperCase().lastIndexOf(("<br>").toUpperCase());

                            addressCity = tmp.substring(z + 5, k);

                            endOfAddressIndex -= addressCity.length(); // if there is a city, the end of address is before it
                        }

                        if (startOfAddressIndex > -1 && endOfAddressIndex > -1) {
                            String rawAddress = line.substring(startOfAddressIndex, endOfAddressIndex);
                            System.out.println("*** DATA RECORD: address part ***=" + rawAddress);

                            String[] addrParts = rawAddress.split("<br>");
                            int addrLen = addrParts.length;

                            if (addrParts[addrLen - 1].trim().length() == 0) {
                                addrLen -= 1; // there is nothing after last <br>
                            }

                            if (addrLen == 0) {
                                addressStreet0 = removeTags(rawAddress);
                            } else if (addrLen == 1) {
                                addressStreet0 = removeTags(addrParts[0]);
                            } else if (addrLen == 2) {
                                addressStreet0 = removeTags(addrParts[0]);
                                addressStreet = removeTags(addrParts[1]);
                            } else if (addrLen == 3) {
                                addressStreet0 = removeTags(addrParts[0]) + ", " + removeTags(addrParts[1]);
                                addressStreet = removeTags(addrParts[2]);
                            } else if (addrLen >= 4) {
                                addressStreet0 = removeTags(addrParts[0]) + ", " + removeTags(addrParts[1]);
                                addressStreet = removeTags(addrParts[2]) + ", " + removeTags(addrParts[3]);
                            }

                            level1Dept = removeTags(level1Dept).trim();
                            lowestDept = removeTags(lowestDept).trim();

                            // remove other data from address fields ( if fetched in error )

                            int m1 = -1;
                            if ((firstName.trim()).length() > 0) {
                                m1 = addressStreet.indexOf(firstName.trim());
                                if (m1 > -1) {
                                    if ((m1 + (firstName.trim()).length() < addressStreet.length()) && addressStreet.charAt(m1 + (firstName.trim()).length()) != ' ') {
                                        m1 = -1;  // firstName is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int m2 = -1;
                            if ((lastName.trim()).length() > 0) {
                                m2 = addressStreet.indexOf(lastName.trim());
                                if (m2 > -1) {
                                    if ((m2 + (lastName.trim()).length() < addressStreet.length()) && addressStreet.charAt(m2 + (lastName.trim()).length()) != ' ') {
                                        m2 = -1;  // lastName is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int m3 = -1;
                            if ((title.trim()).length() > 0) {
                                m3 = addressStreet.indexOf(title.trim());
                                if (m3 > -1) {
                                    if ((m3 + (title.trim()).length() < addressStreet.length()) && addressStreet.charAt(m3 + (title.trim()).length()) != ' ') {
                                        m3 = -1;  // title is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int m4 = -1;
                            if ((level1Dept.trim()).length() > 0) {
                                m4 = addressStreet.indexOf(level1Dept.trim());
                                if (m4 > -1) {
                                    if ((m4 + (level1Dept.trim()).length() < addressStreet.length()) && addressStreet.charAt(m4 + (level1Dept.trim()).length()) != ' ') {
                                        m4 = -1;  // level1Dept is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int m5 = -1;
                            if ((lowestDept.trim()).length() > 0) {
                                m5 = addressStreet.indexOf(lowestDept.trim());
                                if (m5 > -1) {
                                    if ((m5 + (lowestDept.trim()).length() < addressStreet.length()) && addressStreet.charAt(m5 + (lowestDept.trim()).length()) != ' ') {
                                        m5 = -1;  // lowestDept is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            if ((m1 > -1 && m2 > -1) || m3 > -1 || m4 > -1 || m5 > -1) {
                                addressStreet = "";
                            }
                            //////////////////////////////////////////////////////////////////

                            int n1 = -1;
                            if ((firstName.trim()).length() > 0) {
                                n1 = addressStreet0.indexOf(firstName.trim());
                                if (n1 > -1) {
                                    if ((n1 + (firstName.trim()).length() < addressStreet0.length()) && addressStreet0.charAt(n1 + (firstName.trim()).length()) != ' ') {
                                        n1 = -1;  // firstName is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int n2 = -1;
                            if ((lastName.trim()).length() > 0) {
                                n2 = addressStreet0.indexOf(lastName.trim());
                                if (n2 > -1) {
                                    if ((n2 + (lastName.trim()).length() < addressStreet0.length()) && addressStreet0.charAt(n2 + (lastName.trim()).length()) != ' ') {
                                        n2 = -1;  // lastName is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int n3 = -1;
                            if ((title.trim()).length() > 0) {
                                n3 = addressStreet0.indexOf(title.trim());
                                if (n3 > -1) {
                                    if ((n3 + (title.trim()).length() < addressStreet0.length()) && addressStreet0.charAt(n3 + (title.trim()).length()) != ' ') {
                                        n3 = -1;  // title is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int n4 = -1;
                            if ((level1Dept.trim()).length() > 0) {
                                n4 = addressStreet0.indexOf(level1Dept.trim());
                                if (n4 > -1) {
                                    if ((n4 + (level1Dept.trim()).length() < addressStreet0.length()) && addressStreet0.charAt(n4 + (level1Dept.trim()).length()) != ' ') {
                                        n4 = -1;  // level1Dept is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            int n5 = -1;
                            if ((lowestDept.trim()).length() > 0) {
                                n5 = addressStreet0.indexOf(lowestDept.trim());
                                if (n5 > -1) {
                                    if ((n5 + (lowestDept.trim()).length() < addressStreet0.length()) && addressStreet0.charAt(n5 + (lowestDept.trim()).length()) != ' ') {
                                        n5 = -1;  // lowestDept is part of some longer word, keep content of addressStreet
                                    }
                                }
                            }

                            if ((n1 > -1 && n2 > -1) || n3 > -1 || n4 > -1 || n5 > -1) {
                                addressStreet0 = "";
                            }
                            ;
                        }
                    }

                    continue;
                }

            } // end for

            if (consoleOutput) {
                System.out.println("fname    =" + firstName);
                System.out.println("lname    =" + lastName);
                System.out.println("title    =" + titule);
                System.out.println("job title=" + title);
                System.out.println("sumLevel =" + tmpSumLevel);
                System.out.println("level1   =" + level1Dept);
                System.out.println("lowest   =" + lowestDept);
                System.out.println("adr str0Dept =" + addressStreet0Dept);
                System.out.println("adr strDept  =" + addressStreetDept);
                System.out.println("adr cityDept =" + addressCityDept);
                System.out.println("adr provDept =" + addressProvDept);
                System.out.println("adr pcodeDept=" + addressPCodeDept);
                System.out.println("adr str0 =" + addressStreet0);
                System.out.println("adr str  =" + addressStreet);
                System.out.println("adr city =" + addressCity);
                System.out.println("adr prov =" + addressProv);
                System.out.println("adr pcode=" + addressPCode);
                System.out.println("phone    =" + telephone);
                System.out.println("fax      =" + fax);
                System.out.println("email    =" + email);
                System.out.println("------------------------------------------------------------");
            }

            // write extracted data to file...
            osd.println("\"" + recordCount + "\"," +
                    "\"" + recordId + "\"," +
                    "\"" + pageId + "\"," +
                    "\"" + getTimeStamp() + "\"," +
                    "\"" + tmpSumLevel.trim() + "\"," +
                    "\"" + level1Dept.trim() + "\"," +
                    "\"" + lowestDept.trim() + "\"," +
                    "\"" + firstName.trim() + "\"," +
                    "\"" + lastName.trim() + "\"," +
                    "\"" + (titule.replace('"', '\'')).trim() + "\"," +
                    "\"" + (title.replace('"', '\'')).trim() + "\"," +
                    "\"" + addressStreet0Dept.trim() + "\"," +
                    "\"" + addressStreetDept.trim() + "\"," +
                    "\"" + addressCityDept.trim() + "\"," +
                    "\"" + addressProvDept.trim() + "\"," +
                    "\"" + addressPCodeDept.trim() + "\"," +
                    "\"" + addressStreet0.trim() + "\"," +
                    "\"" + addressStreet.trim() + "\"," +
                    "\"" + addressCity.trim() + "\"," +
                    "\"" + addressProv.trim() + "\"," +
                    "\"" + addressPCode.trim() + "\"," +
                    "\"" + (telephone.replace(' ', '-')).trim() + "\"," +
                    "\"" + (fax.replace(' ', '-')).trim() + "\"," +
                    "\"" + email.trim() + "\"");

            osd.flush();


        } catch (StringIndexOutOfBoundsException eob) {
            eob.printStackTrace();
            System.out.println("     ***** String index out of range, skipping this record... ********");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("     ***** record timeout, process again..... ********");
            processRecord(ministry, recordId, pageId, nameSurname, pageUrl, sumLevel, deptAddress);
        }

    }

    private void processAdditionalRecord(String ministry, String recordId, String pageId, String pageUrl, String sumLevel, String deptAddress) {
        firstName = "";
        lastName = "";
        titule = "";
        title = "";
        addressStreetDept = "";
        addressStreet0Dept = "";
        addressCityDept = "";
        addressProvDept = "";
        addressPCodeDept = "";
        addressStreet = "";
        addressStreet0 = "";
        addressCity = "";
        addressProv = "";
        addressPCode = "";
        telephone = "";
        fax = "";
        email = "";

        int lineCount = 0;
        boolean pCodeFound = false;
        boolean lastNameExists = false;
        Vector lineList = new Vector(100);

        boolean inStreamClosed = true;

        try {
            Thread.sleep(this.recordTimeout);
        } catch (InterruptedException e) {
            System.out.println("ProcessAdditionalRecord():  InterruptedException in sleep");
        }

        System.out.println("     new additional record " + pageUrl + " sum level=" + sumLevel);
        String webPage = new String("");
        BufferedReader inStream = null;
        boolean startChecking = false;

        try {
            Document doc = Jsoup.connect(pageUrl).get();
            String htmlPage = doc.html();
            String[] parts = htmlPage.split("\n");
            int partsLen = parts.length;

            String line = "";

            boolean processingEmployee = false;

            Vector htmlLines = new Vector(10000);
            //while ( (line = inStream.readLine()) != null)
            for (int i = 0; i < partsLen; i++) {
                line = parts[i];
                line = line.replace("&amp;", "&");
                htmlLines.addElement(line);
            }

            // read record's web page - only the lines we might be interested in
            int jhtml = 0;
            int htmlLinesSize = htmlLines.size();
            for (jhtml = 0; jhtml < htmlLinesSize; jhtml++) {
                line = (String) htmlLines.elementAt(jhtml);

                int k = -1;
                if (startChecking == false) {
                    k = line.indexOf("* START Employee Listing *");
                    if (k > -1) {
                        // start checking
                        startChecking = true;
                    }
                    continue;
                }

                k = line.indexOf("* END EMPLOYEE *");
                if (k > -1) {
                    // stop checking - exit loop
                    break;
                }

                if (processingEmployee == true) {
                    // check for end of data
                    k = -1;
                    k = line.indexOf("</tr>");
                    if (k > -1) {
                        // end of useful data
                        // write to file and console HERE !
                        break;
                    }
                }

                k = -1;
                k = line.indexOf("<B><A class=\"cclink\" NAME=\"");
                if (k > -1) {
                    int k1 = k + 27;
                    int k2 = line.indexOf("\"", k1);
                    String userId = line.substring(k1, k2);
                    System.out.println("recId=" + recordId + " userID=" + userId);
                    if (recordId.equals(userId)) {
                        System.out.println(" !!! Match !!!");
                        processingEmployee = true;

                        // skip one line
                        //line = inStream.readLine();
                        jhtml++;

                        //line = inStream.readLine();
                        jhtml++;
                        line = (String) htmlLines.elementAt(jhtml);

                        // extract name, first name, titule (if it exists)
                        String tmp = removeSpaces(removeTags(line));


                        k = -1;
                        k = tmp.indexOf(",");
                        if (k > -1) {
                            if (tmp.length() >= (k + 2 + 1)) {
                                firstName = tmp.substring(k + 2);

                                if (firstName.length() > 0) {
                                    // possibly separate the title (example: The Hon. or MLA )
                                    int n = -1;
                                    n = firstName.indexOf(" ");
                                    int n1 = firstName.indexOf(",");
                                    if (n > -1 || n1 > -1) {
                                        if (n1 > -1) {
                                            n = n1;
                                        }

                                        if (firstName.length() > n + 1 + 1) {
                                            titule = firstName.substring(n + 1);
                                        }
                                        firstName = firstName.substring(0, n);
                                    }
                                }
                            }
                            lastName = tmp.substring(1, k);
                            lastNameExists = true;
                        }

                        // extract job title

                        // skip seven lines
                        jhtml += 7;

                        // job title should be here - for now we don't extract it

                        jhtml++;
                        line = (String) htmlLines.elementAt(jhtml);
                        String line_d = removeSpaces(removeTagsExceptBR(deptAddress)).trim();
                        System.out.println("len=" + line_d.length() + " (ADDITIONAL REC)DEPT.ADDRESS: " + line_d);
                        System.out.println("len=" + (line.trim()).length() + " (ADDITIONAL REC)ADDRESS     : " + line);

                        // check employee address first

                        // check how menu lines do we have ?
                        int brCount = 0;
                        int startInd = 0;
                        String line1 = "";
                        String line2 = "";
                        String line3 = "";
                        while (true) {
                            k = -1;
                            k = line.indexOf("<BR>", startInd);
                            if (k > -1) {
                                brCount++;
                                if (brCount == 1) {
                                    // city and pcode could be on second line
                                    line1 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line1);
                                    if (tmp1.length() > 0) {
                                        addressPCode = tmp1;
                                        int kt = line1.indexOf(addressPCode);
                                        addressCity = line1.substring(0, kt).trim();
                                    } else {
                                        addressStreet0 = line1;
                                    }
                                }
                                if (brCount == 2) {
                                    // city and pcode could be on second line
                                    line2 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line2);
                                    if (tmp1.length() > 0) {
                                        addressPCode = tmp1;
                                        int kt = line2.indexOf(addressPCode);
                                        addressCity = line2.substring(0, kt).trim();
                                    } else {
                                        addressStreet = line2;
                                    }
                                }
                                if (brCount == 3) {
                                    // only city and pcode can be on third line
                                    line3 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line3);
                                    if (tmp1.length() > 0) {
                                        addressPCode = tmp1;
                                        int kt = line3.indexOf(addressPCode);
                                        addressCity = line3.substring(0, kt).trim();
                                    }
                                }
                                startInd = k + 4;
                            } else {
                                break;
                            }
                        } // end while

                        line1 = line1.trim();
                        line2 = line2.trim();
                        line3 = line3.trim();

                        System.out.println(line1.length() + " line1: '" + line1 + "'");
                        System.out.println(line2.length() + " line2: '" + line2 + "'");
                        System.out.println(line3.length() + " line3: '" + line3 + "'");

                        if (line1.length() > 0 && line2.length() == 0 && line3.length() == 0) {
                            // city is on line 1
                            addressCity = line1;
                            addressPCode = "";
                            addressStreet0 = "";
                            addressStreet = "";
                        }
                        if (line2.length() > 0 && line1.length() == 0 && line3.length() == 0) {
                            // city is on line 1
                            addressCity = line2;
                            addressPCode = "";
                            addressStreet0 = "";
                            addressStreet = "";
                        }

                        // province should be Alberta everywhere ?
                        addressProv = "Alberta";

                        // check dept. address
                        line = line_d;

                        // check how menu lines do we have ?
                        brCount = 0;
                        startInd = 0;
                        line1 = "";
                        line2 = "";
                        line3 = "";
                        while (true) {
                            k = -1;
                            k = line.indexOf("<BR>", startInd);
                            if (k > -1) {
                                brCount++;
                                if (brCount == 1) {
                                    // city and pcode could be on second line
                                    line1 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line1);
                                    if (tmp1.length() > 0) {
                                        addressPCodeDept = tmp1;
                                        int kt = line1.indexOf(addressPCodeDept);
                                        addressCityDept = line1.substring(0, kt).trim();
                                    } else {
                                        addressStreet0Dept = line1;
                                    }
                                }
                                if (brCount == 2) {
                                    // city and pcode could be on second line
                                    line2 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line2);
                                    if (tmp1.length() > 0) {
                                        addressPCodeDept = tmp1;
                                        int kt = line2.indexOf(addressPCodeDept);
                                        addressCityDept = line2.substring(0, kt).trim();
                                    } else {
                                        addressStreetDept = line2;
                                    }
                                }
                                if (brCount == 3) {
                                    // only city and pcode can be on third line
                                    line3 = line.substring(startInd, k).trim();
                                    String tmp1 = containsPCode(line3);
                                    if (tmp1.length() > 0) {
                                        addressPCodeDept = tmp1;
                                        int kt = line3.indexOf(addressPCodeDept);
                                        addressCityDept = line3.substring(0, kt).trim();
                                    }
                                }
                                startInd = k + 4;
                            } else {
                                break;
                            }
                        } // end while

                        line1 = line1.trim();
                        line2 = line2.trim();
                        line3 = line3.trim();

                        System.out.println(line1.length() + " line1Dept: '" + line1 + "'");
                        System.out.println(line2.length() + " line2Dept: '" + line2 + "'");
                        System.out.println(line3.length() + " line3Dept: '" + line3 + "'");

                        if (line1.length() > 0 && line2.length() == 0 && line3.length() == 0) {
                            // city is on line 1
                            addressCityDept = line1;
                            addressPCodeDept = "";
                            addressStreet0Dept = "";
                            addressStreetDept = "";
                        }
                        if (line2.length() > 0 && line1.length() == 0 && line3.length() == 0) {
                            // city is on line 1
                            addressCityDept = line2;
                            addressPCodeDept = "";
                            addressStreet0Dept = "";
                            addressStreetDept = "";
                        }

                        // province should be Alberta everywhere ?
                        addressProvDept = "Alberta";

                        continue;
                    } else {
                        continue;
                    }
                }

                k = -1;
                k = line.indexOf("<B>Fax:");
                if (k > -1) {
                    String tmp = removeTags(line).trim();
                    if (tmp.length() > 6) {
                        // this is porbably number
                        fax = tmp;
                    } else {
                        // pick up everything until we hit  <BR>
                        tmp = "";
                        while (true) {
                            //line = inStream.readLine();
                            jhtml++;
                            line = (String) htmlLines.elementAt(jhtml);
                            String tmp1 = removeTags(line).trim();
                            if (tmp1.length() > 0) {
                                tmp = tmp + tmp1;
                            }

                            k = -1;
                            k = line.indexOf("<BR>");
                            if (k > -1) {
                                fax = tmp;
                                break;
                            }

                        }// end while
                    }
                }


                // get phone number
                k = -1;
                k = line.indexOf("<TD nowrap >");
                if (k > -1) {
                    // pick up everything until we hit  </TD>
                    String tmp = "";
                    while (true) {
                        jhtml++;
                        line = (String) htmlLines.elementAt(jhtml);
                        //line = inStream.readLine();
                        k = -1;
                        k = line.indexOf("</TD>");
                        if (k > -1) {
                            telephone = tmp;
                            break;
                        } else {
                            String tmp1 = removeTags(line).trim();
                            if (tmp1.length() > 0) {
                                tmp = tmp + tmp1;
                            }
                        }

                    }
                }

                String tmp = removeSpaces(removeTags(line));
                k = -1;
                k = tmp.indexOf("Email:");
                if (k > -1) {
                    if (tmp.length() <= 7) continue;
                    email = tmp.substring(k + 7);
                    continue;
                }

            } // end for

            String tmpSumLevel = (sumLevel.substring(0)).replace(TMPCHAR, '?');
            String level1Dept = "";
            String lowestDept = "";

            // extract the first level of department description out of tmpSumLevel
            int k1 = tmpSumLevel.indexOf("?");
            if (k1 > -1) {
                level1Dept = tmpSumLevel.substring(0, k1);
            } else {
                level1Dept = tmpSumLevel;
            }

            // extract the last (high) level of department description out of tmpSumLevel
            k1 = tmpSumLevel.lastIndexOf("?");
            if (k1 > -1) {
                lowestDept = tmpSumLevel.substring(k1 + 1);
            } else {
                lowestDept = tmpSumLevel;
            }

            if (consoleOutput) {
                System.out.println("ADDITIONAL RECORD:");
                System.out.println("fname    =" + firstName);
                System.out.println("lname    =" + lastName);
                System.out.println("title    =" + titule);
                System.out.println("job title=" + title);
                System.out.println("sumLevel =" + tmpSumLevel);
                System.out.println("level1   =" + level1Dept);
                System.out.println("lowest   =" + lowestDept);
                System.out.println("adr str0Dept =" + addressStreet0Dept);
                System.out.println("adr strDept  =" + addressStreetDept);
                System.out.println("adr cityDept =" + addressCityDept);
                System.out.println("adr provDept =" + addressProvDept);
                System.out.println("adr pcodeDept=" + addressPCodeDept);
                System.out.println("adr str0 =" + addressStreet0);
                System.out.println("adr str  =" + addressStreet);
                System.out.println("adr city =" + addressCity);
                System.out.println("adr prov =" + addressProv);
                System.out.println("adr pcode=" + addressPCode);
                System.out.println("phone    =" + telephone);
                System.out.println("fax      =" + fax);
                System.out.println("email    =" + email);
                System.out.println("------------------------------------------------------------");
            }

            // write extracted data to file...
            osd.println("\"" + recordCount + "\"," +
                    "\"" + recordId + "\"," +
                    "\"" + pageId + "\"," +
                    "\"" + getTimeStamp() + "\"," +
                    "\"" + tmpSumLevel.trim() + "\"," +
                    "\"" + level1Dept.trim() + "\"," +
                    "\"" + lowestDept.trim() + "\"," +
                    "\"" + firstName.trim() + "\"," +
                    "\"" + lastName.trim() + "\"," +
                    "\"" + (titule.replace('"', '\'')).trim() + "\"," +
                    "\"" + (title.replace('"', '\'')).trim() + "\"," +
                    "\"" + addressStreet0Dept.trim() + "\"," +
                    "\"" + addressStreetDept.trim() + "\"," +
                    "\"" + addressCityDept.trim() + "\"," +
                    "\"" + addressProvDept.trim() + "\"," +
                    "\"" + addressPCodeDept.trim() + "\"," +
                    "\"" + addressStreet0.trim() + "\"," +
                    "\"" + addressStreet.trim() + "\"," +
                    "\"" + addressCity.trim() + "\"," +
                    "\"" + addressProv.trim() + "\"," +
                    "\"" + addressPCode.trim() + "\"," +
                    "\"" + (telephone.replace(' ', '-')).trim() + "\"," +
                    "\"" + (fax.replace(' ', '-')).trim() + "\"," +
                    "\"" + email.trim() + "\"");

            osd.flush();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("     ***** record timeout, process again..... ********");
            processAdditionalRecord(ministry, recordId, pageId, pageUrl, sumLevel, deptAddress);
        }

    }

    private String containsPCode(String line) {
        String tmp = "";
        int len = line.length();
        int i = 0;

        if (len < 7) return ("");

        for (i = 0; i < len - 6; i++) {
            tmp = line.substring(i, i + 7);
            if (!Character.isDigit(tmp.charAt(0)) &&
                    Character.isDigit(tmp.charAt(1)) &&
                    !Character.isDigit(tmp.charAt(2)) &&
                    Character.isSpace(tmp.charAt(3)) &&
                    Character.isDigit(tmp.charAt(4)) &&
                    !Character.isDigit(tmp.charAt(5)) &&
                    Character.isDigit(tmp.charAt(6))) {
                return tmp;
            }
        }

        return "";

    }

    /**
     * Returns time stamp (current sytem time) in format YYYY-MM-DD
     *
     * @return time stamp
     */
    ///////////////////////////////////////////////////////////////////////////////////
    private String getTimeStamp()
    ///////////////////////////////////////////////////////////////////////////////////
    {
        //create system generated date/time stamp
        Calendar now = Calendar.getInstance();

        int month = (1 + now.get(Calendar.MONTH));
        int day = now.get(Calendar.DAY_OF_MONTH);
        //int hour      = now.get(Calendar.HOUR_OF_DAY);
        //int minute    = now.get(Calendar.MINUTE);
        //int sec       = now.get(Calendar.SECOND);

        String smonth = "";
        String sday = "";
        //String shour  = "";
        //String smin   = "";
        //String ssec   = "";

        smonth = "" + month;
        if (month < 10) smonth = "0" + smonth;

        sday = "" + day;
        if (day < 10) sday = "0" + sday;

        //shour = "" + hour;
        //if(hour < 10)   shour  = "0" + shour;

        //smin = "" + minute;
        //if(minute < 10) smin   = "0" + smin;

        //ssec = "" + sec;
        //if(sec < 10)    ssec   = "0" + ssec;

        return (now.get(Calendar.YEAR) + "-" + smonth + "-" + sday /*+ shour + smin + ssec + "00"*/);
    }

    private String removeSpaces(String tmp) {
        String line = tmp.replace('&', ' ');
        line = line.replace(';', ' ');

        StringTokenizer st = new StringTokenizer(line);
        String token = "";

        String nextToken = "";
        while (st.hasMoreTokens()) {
            nextToken = st.nextToken();
            if (!nextToken.equals("nbsp")) token += (" " + nextToken);
        }
        return token;

    }
/*
    private static class TimeoutHttpClient extends HttpClient {

        private Socket socket;


        public TimeoutHttpClient(URL url, String str, int i) throws IOException {
            super(url, str, i);
        }

        public static HttpClient New(URL url_1) throws IOException {

        	HttpClient httpc = (HttpClient)kac.get(url_1);

            if(httpc == null) httpc = new TimeoutHttpClient(url_1, null, -1);
            else httpc = HttpClient.New(url_1);

            return httpc;

        }

        public void setTimeout(int millis) throws SocketException {
            if(socket != null) socket.setSoTimeout(millis);
        }

        public int getTimeout() throws SocketException {
            if(socket != null) return socket.getSoTimeout();
            else return -1;
        }

        protected Socket doConnect(String host, int port) throws IOException, UnknownHostException {

            socket = new Socket(host, port);

            socket.setSoTimeout(TimeoutHttpURLConnection.TIMEOUT);

            return socket;
        }
    }
*/
/*
    private static class TimeoutHttpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {

        public static int TIMEOUT = 30000;

        public TimeoutHttpURLConnection(URL url, String proxy, int proxyPort) throws IOException {
            super(url, proxy, proxyPort);
        }

        public TimeoutHttpURLConnection(URL url) throws IOException {
            //this(url, null, -1);
            this(url, "alberta.ca", 80);
            this.setRequestProperty("Accept","image/gif, image/x-xbitmap, image/jpeg, application/msword, application/vnd.ms-excel ");
            this.setRequestProperty("Accept-Language","en-us");
            this.setRequestProperty("Accept-Encoding","gzip, deflate");
            this.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 5.5; Windows NT \"");
        }

        protected HttpClient getNewClient(URL url) throws IOException {
            return new TimeoutHttpClient(url, null, -1);
        }

        protected HttpClient getProxiedClient(URL url, String s, int i) throws IOException {
            return new TimeoutHttpClient(url, s, i);
        }

        public void connect() throws IOException {

            if(connected) return;

            Properties prop = System.getProperties();
            String set = (String)prop.get("proxySet");

            if(set != null && set.equalsIgnoreCase("true")) {

               String host = (String)prop.get("proxyHost");
               int port = -1;

               try {
                   port = Integer.parseInt((String)prop.get("proxyPort"));
               } catch(Exception e) { }

               if(host != null && port != -1) http = new TimeoutHttpClient(url, host, port);
              else http = TimeoutHttpClient.New(url);

            } else http = TimeoutHttpClient.New(url);

            if(http instanceof TimeoutHttpClient) ((TimeoutHttpClient)http).setTimeout(TIMEOUT);

           ps = (PrintStream)http.getOutputStream();

           connected = true;
       }
    }

    private static class TimeoutHttpStreamHandler extends URLStreamHandler {

        public TimeoutHttpStreamHandler() {
            super();
        }

        public URLConnection openConnection(URL url) throws IOException {
            return new TimeoutHttpURLConnection(url);
        }
    }
*/

}

