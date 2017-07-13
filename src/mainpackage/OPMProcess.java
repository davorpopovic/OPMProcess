package mainpackage;

import com.steadystate.css.util.Output;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by nick on 11/07/17.
 */
public class OPMProcess {


    public static String procLine(String line) {

        String ret = "";

        //broken down name entries that
        //will replace col. 'EmployeeName'
        String lastName = "";
        String firstName = "";
        String middleName = "";

        // the 'EmployeeName' column
        String fullName = line.split("[|]")[37 - 5];

        //don't process further! ... bad entry
        if (fullName.contains("NAME WITHHELD")) {
            lastName = "";
            firstName = "";
            middleName = "";
        } else {
            // full name broken down in a list of
            // three elements: {fname, lname, middle}
            String[] fullname_list = fullName.split(" ");

            //all three entries filled: FULL NAME
            if (fullname_list.length == 3) {
                lastName = fullname_list[0];
                firstName = fullname_list[1];
                middleName = fullname_list[2];
            }

            //only two entries filled: NO MIDDLE NAME
            else if (fullname_list.length == 2) {
                lastName = fullname_list[0];
                firstName = fullname_list[1];
                middleName = "";
            }

            //should never happen: NO NAME AT ALL
            else {
                lastName = "NO NAME (Should never happen)";
                firstName = "NO NAME (Should never happen)";
                middleName = "NO NAME (Should never happen)";
            }
            //replace original 'EmployeeName' entry with "<last>|<first>|<middle>" entry
            //and rename new line entry as newLine
            String newLine = line.replace(fullName, lastName + "|" + firstName + "|" + middleName + "|");

            /*System.out.println(newLine.split("[|]").length);*/

            ret = "\"" + newLine.replaceAll("[|]", "\",\"") + "\"";

        }

        return ret;

    }

    public static void procData(String InputFile, String OutputFile, String FieldSeparator,
                                String EmailModelFile, String AgencyField1, String AgencyField2) throws IOException {

        //reader for InputFile
        BufferedReader InputFile_reader = new BufferedReader(new FileReader(InputFile));

        //Writer for OutputFile
        FileWriter OutputFile_writer = new FileWriter(OutputFile);


        //write header to OutputFile
        OutputFile_writer.write("\"Agency\",\"AgencySub-element\",\"OccupationTitle\",\"OccupationalCategory\",\"PayBasis\",\"Grade\",\"PayPlan\",\"BasicPay\",\"ServiceCompDate\",\"SupervisoryStatus\",\"Tenure\",\"TypeofAppointment\",\"WorkSchedule\",\"AsofDate\",\"BargainingUnit\",\"FLSA\",\"FunctionalClass\",\"PayRateDeterminant\",\"PersonnelOfficeID\",\"PositionOccupied\",\"Step/Rate\",\"VeteranStatus\",\"AdjustedBasicPay\",\"TotalPay\",\"CSA\",\"LocalityAdjustment\",\"LocalityPayArea\",\"CBSA\",\"SupervisoryDifferential\",\"SpecialPayTableId\",\"RatingofRecordPattern\",\"RatingofRecordPeriod\",\"LastName\",\"FirstName\",\"MiddleName\",\"City\",\"State\",\"County\",\"Country\"");
        OutputFile_writer.write("\n");

        //line read - storage
        String line;

        Hashtable table = new Hashtable();
        String key = "";
        int value = 0;

        // (1) read InputFile line by line and write to OutputFile
        // (2) build hash table and format the key entry as: <"abc|def">
        while ((line = InputFile_reader.readLine()) != null) {

            //(1) process line and write it to output destination
            if (procLine(line) != "") {
                OutputFile_writer.write(procLine(line));
                OutputFile_writer.write("\n");
            }

            // (2) BUILD HASH TABLE
            int second_pos = StringUtils.ordinalIndexOf(line,FieldSeparator, 2);
            key = line.substring(0, second_pos);

            //key does not exist in table: ADD IT
            if(!table.containsKey(key)){
                table.put(key, value);
                value ++;
            }
        }

        //flush the OutputFile writer content and close it
        OutputFile_writer.flush();
        OutputFile_writer.close();

        //close the InputFile reader
        InputFile_reader.close();


        /* Write HASH TABLE contents to EmailModelFile*/

        //writer for <EMAIL MODEL FILE>
        FileWriter EmailModelFile_writer = new FileWriter(EmailModelFile);
        Enumeration k_enum = table.keys();

        EmailModelFile_writer.write("\"Agency\",\"Agency Sub-element\",\"Email model\",\"URL\"");
        EmailModelFile_writer.write("\n");

        while(k_enum.hasMoreElements()) {
            String entry = (String) k_enum.nextElement();
            String polished_entry = "\"" + entry.replaceAll("[|]", "\",\"") + "\"" + ",\"\",\"\"";

            EmailModelFile_writer.write(polished_entry);
            EmailModelFile_writer.write("\n");
        }
        //flush the EmailModelFile writer content and close it
        EmailModelFile_writer.flush();
        EmailModelFile_writer.close();


    }

    public static void main(String[] args) throws IOException {

        String InputFile = "/home/nick/IdeaProjects/OPMProcess/src/OPMProcess/samplenew.txt";
        String OutputFile = "/home/nick/IdeaProjects/OPMProcess/src/mainpackage/samplenew_out.txt";
        String FieldSeparatorChar = "|";
        String EmailModelFile = "/home/nick/IdeaProjects/OPMProcess/src/mainpackage/samplenew_emailmodel_out.txt";
        String AgencyField1 = "Agency";
        String AgencyField2 = "Agency Sub-element";

        // process the input data file
        // generates: (1) double-quoted csv file with <|> replaced with <,>
        //                and eliminates records containing <NAME WITHHELD>
        //            (2) double-quoted csv file with unique (Agency, Agency Sub-element)
        //                entries
        procData(InputFile, OutputFile, FieldSeparatorChar, EmailModelFile, AgencyField1, AgencyField2);

    }
}
