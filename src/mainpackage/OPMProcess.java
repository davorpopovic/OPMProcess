package mainpackage;

import com.steadystate.css.util.Output;

import java.io.*;
import java.lang.reflect.Field;

/**
 * Created by nick on 11/07/17.
 */
public class OPMProcess {


    public static String procLine(String line) {

        String ret = "";

        String lastName = "";
        String firstName = "";
        String middleName = "";

        //length is 37!
        String fullName = line.split("[|]")[37-5];

        //don't process further!
        if(fullName.contains("NAME WITHHELD")){
            lastName = "";
            firstName = "";
            middleName = "";
        }
        else{
            String[] fullname_list = fullName.split(" ");

            //full name
            if (fullname_list.length == 3){
                lastName = fullname_list[0];
                firstName = fullname_list[1];
                middleName = fullname_list[2];
            }

            //no middle name
            else if(fullname_list.length == 2){
                lastName = fullname_list[0];
                firstName = fullname_list[1];
                middleName = "";
            }

            //no name - should never happen
            else{
                lastName = "NO NAME (Should never happen)";
                firstName = "NO NAME (Should never happen)";
                middleName = "NO NAME (Should never happen)";
            }
            //replace original fullname entry with "<last>|<first>|<middle>" entry
            //and rename new line entry as newLine
            String newLine = line.replace(fullName, lastName + "|" + firstName + "|" + middleName + "|");

            System.out.println(newLine.split("[|]").length);

            ret = "\"" + newLine.replaceAll("[|]","\",\"") + "\"";

        }

        return ret;

    }

    public static void procData(String InputFile, String OutputFile, String FieldSeparator) throws IOException {

        //reader
        BufferedReader br = new BufferedReader(new FileReader(InputFile));

        //writer
        FileWriter fw = new FileWriter(OutputFile);

        //write header
        fw.write("\"Agency\",\"AgencySub-element\",\"OccupationTitle\",\"OccupationalCategory\",\"PayBasis\",\"Grade\",\"PayPlan\",\"BasicPay\",\"ServiceCompDate\",\"SupervisoryStatus\",\"Tenure\",\"TypeofAppointment\",\"WorkSchedule\",\"AsofDate\",\"BargainingUnit\",\"FLSA\",\"FunctionalClass\",\"PayRateDeterminant\",\"PersonnelOfficeID\",\"PositionOccupied\",\"Step/Rate\",\"VeteranStatus\",\"AdjustedBasicPay\",\"TotalPay\",\"CSA\",\"LocalityAdjustment\",\"LocalityPayArea\",\"CBSA\",\"SupervisoryDifferential\",\"SpecialPayTableId\",\"RatingofRecordPattern\",\"RatingofRecordPeriod\",\"LastName\",\"FirstName\",\"MiddleName\",\"City\",\"State\",\"County\",\"Country\"");
        fw.write("\n");

        //read line by line
        String line;
        while ((line = br.readLine()) != null) {
            //System.out.println(line);

            //process line and write it to output destination
            if (procLine(line) != ""){
                fw.write(procLine(line));
                fw.write("\n");
            }
        }

        //flush the Writer content and close it
        fw.flush();
        fw.close();

        //close the Reader
        br.close();


    }

    public static void main(String[] args) throws IOException {

        String InputFile = "/home/nick/IdeaProjects/OPMProcess/src/OPMProcess/samplenew.txt";
        String OutputFile = "/home/nick/IdeaProjects/OPMProcess/src/mainpackage/samplenew_out.txt";
        String FieldSeparatorChar = "|";

        procData(InputFile, OutputFile, FieldSeparatorChar);


    }
}
