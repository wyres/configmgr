/*
 * Author : Joel Wiki
 * Date : 22/10/2019
 * Date of last review : 07/11/2019
 * Company : Wyres
 * Function : This class will help to read a mynewt device 
 * 
 */

package configmgr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigMgr {

	final Logger log = LoggerFactory.getLogger(ConfigMgr.class);


	public static void main(String[] args) throws Exception {

		String fileInput = "";
		String outputFile = "";
		String outputCsv = "";


		boolean dumpConfig = false;
		boolean addConfig = false; 
		boolean input = false;
		boolean output = false; 


		ArrayList<String> tagsList = new ArrayList<>();

		for(int i = 0; i< args.length; i++) {

			if(args[i].equalsIgnoreCase("-i")) {		
				try {
					i++;
					fileInput=args[i];
				}catch (ArrayIndexOutOfBoundsException e) {
					throw new Exception("Error : argument for -i is empty");
				}
				
				File f = new File(fileInput);

				if(f.exists()) {
					if(f.canRead()&&f.canWrite()) {
						if(fileInput.endsWith(".hex")) {
							input=true;
						}else {
							throw new Exception("Error : file is not a .hex file");
						}
					}else {
						throw new Exception("Error : File cannot be read or write");
					}
				}else {
					throw new Exception("Error : File not exists");
				}

			}else if(args[i].equalsIgnoreCase("--hexOutput")) { //need to be hex or bin 
				i++;
				outputFile=args[i];

				//TODO IS OUTUT HASOUTPUT
				output=true;

			}else if(args[i].equalsIgnoreCase("--dumpConfig")) { //dump the config of the file in csv 
				i++;
				try {
					outputCsv= args[i];
					dumpConfig=true;
				}catch (ArrayIndexOutOfBoundsException e) {
					throw new Exception("Error : argument for --dumpConfig is empty");
				}

			}else if(args[i].equalsIgnoreCase("--addConfig")) { //add config of tlv 
				addConfig=true;
				i++;
				try {
					tagsList.add(args[i]);
				}catch (ArrayIndexOutOfBoundsException e) {
					throw new Exception("Error : argument for -addConfig is empty");
				}

			
			}else if(args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help")) {
				printHelp();
			}else {
				printHelp();
			}
		}

		//SWITCH CASE 
		
		MemoryManager memManag= new MemoryManager();
		MemoryParser mP = new MemoryParser();
		
		
	
		if(input&&dumpConfig) {
			mP = new MemoryParser(fileInput);
			
			memManag.extractLayout(mP.getBaseEEprom(),mP.getMemoryContent());
			
			memManag.extractKeys(mP.getMemoryContent());
			mP.exportCSV(outputCsv,memManag.getKV());
			

		}else if(input&&output&&addConfig) {

			mP = new MemoryParser(fileInput); 

			memManag.extractLayout(mP.getBaseEEprom(),mP.getMemoryContent());
			memManag.extractKeys(mP.getMemoryContent());
			
			memManag.addTags(tagsList);
			
			mP.setMemoryContent(memManag.getMemoryContent());
			mP.exportHex(outputFile);

		}else if(!input&&output&&addConfig) {
			memManag.createConfig(tagsList);

			mP.setMemoryContent(memManag.getMemoryContent());
			
			File f = new File(outputFile);
			String textFile = f.getParent().concat("deviceConfig.txt"); //deviceConfig.hex
			
			mP.exportTxt(textFile);
			mP.exportHex(outputFile);
			
			System.out.println(new File(textFile).delete()? "Delete deviceConfig.txt OK" :"Delete deviceConfig.txt OK");
		
			
			
			
		}else{ //print help and command

			printHelp();

		}

	}

	private static void printHelp() {
		
		MavenXpp3Reader reader = new MavenXpp3Reader();
		
		try {
			Model model = reader.read(new FileReader("pom.xml")); //read 
			
			System.out.println(model.getName()+" "+model.getVersion()); 
			//System.out
			System.out.println("Description : This Program was created to be able of create, modify a Wyres config file \n, you can also dump the file config at csv format\n");
			
			System.out.println("Args :\n-i : this argument is to specify a input file\n--hexOutput : to specify a file in hex in output of program\n--addConfig : to add a TlV in this format t:L:V\n--dumpConfig : this argument is for export a config file to a CSV file\n -h or --help : to print this help");
			
			System.out.println("Commands :\n-i 'pathtoFileInput' --dumpConfig 'filetoexport.csv : to export at csv file format +\n-i 'pathtofileinput  --hexOutput 'hexFiletoExport.hex' --addconfig 0101:04=FFFFFFFF -addConfig N  : to modify a already existing config \n--hexOutput 'hexFiletoExport.hex' --addconfig 0101:04=FFFFFFFF -addConfig N : to create a new config with new t:l:v \n " );
		
			
			//preference pour un fichier sans espace , exemple avec gestion des espaces
		
			System.out.println("Example :\n-i C:\\Files\\Config.hex --dumpConfig C:\\Files\\dump.csv\n-i C:\\Files\\Config.hex --hexOutput C:\\Files\\Configmodif.hex --addConfig 0104:04=11223344\n--hexOutput C:\\Files\\newConfig.hex --addConfig 0404:01=01\n");
			System.out.println("Warning : "
					+ "If a Key already exists in a file, the keys which replace them must be equals to the lenght");
			
			
		} catch (FileNotFoundException e) {
			System.out.println("Error pom.xml not found");
		} catch (IOException e) {
			System.out.println("IO Error during processing pom.xml");
		} catch (XmlPullParserException e) {
			System.out.println("Error during parsing pom.xml");
		}
		
	}
}
