/**
 * Copyright 2020 Wyres
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
*/


package configmgr;

/*
 * Author : Joel Wiki
 * Date : 30/10/2019
 * Date of last review : 07/09/2019
 * Company : Wyres
 * Function : This class help to manage export and read file
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryParser { //memory parser 

	private int BASEEEPROM = Integer.parseInt("80000",16); //see specs of wyres devices it's 8080000 but symplify at 80000  

	private int MEMMAX = 8192; // in specs of STM32L151CC, EEPROM's size is 8KB 

	private String fInput; //File Input 
	private static String fOutput; //File Output


	private HashMap<String,String> memContent =new HashMap<>(); //contains all data of txt file 

	final Logger log = LoggerFactory.getLogger(MemoryParser.class);

	public MemoryParser() {
	}

	public MemoryParser(String fInput) throws Exception {		
		setFileInput(fInput);
		hexToTxt();
		readFile();
		
	}

	


	public void hexToTxt() { //to transform Hex File to txt File.. OK
		File f= new File(getFInput());
		String fOutput=f.getParent().concat("\\temp.txt");

		setFileOutput(fOutput);
		Process p = null ;
		try {
			p = Runtime.getRuntime().exec("srecord-1.63-win32\\srec_cat.exe -disable-sequence-warning "+getFInput()+" -Intel -o "+fOutput+" -HEX_DUMP");

			p.waitFor(5000,TimeUnit.MILLISECONDS); //wait 10 seconds before falling in exception

		} catch (Exception e) {
			log.error("Error during execution of command hexToTxt");
		}finally {
			p.destroy();
			
			f = new File(fOutput);
			if(f.exists()) {
				//log.info(f.delete()? "Delete temp.txt OK" : "Delete temp.txt NOK");
			}
		}
	}


	private void readFile() throws Exception { //we read 16 address at once and put content into a HashMap OK...
		this.hexToTxt();
		FileReader fReader = null;
		BufferedReader bReader = null;

		HashMap<String,String> memContent = new HashMap<>(); //new hashmap

		try {
			fReader = new FileReader(this.fOutput);
			bReader = new BufferedReader(fReader);

			String line;
			String address;



			while((line = bReader.readLine())!=null) {

				String[] part1 = line.split("#");
				String[] part2 =part1[0].split(":",2);


				address = part2[0].substring(3); //to simplify
				String preAddress = address.substring(0, address.length()-1);

				String values = part2[1].replace(" ", "");
				if(address.contains("80000")) {
					setBaseEeprom(toHex(address));
				}



				String[] data= new String[16]; //hexa 


				for(int i=0;i<=15;i++) {
					data[i]=values.substring(i*2,(i*2)+2);
					memContent.put(preAddress.concat(getAdress(i)).toLowerCase(),data[i]); //putting all data of file into HashMap, 	
					//GET CONCAT	
				}

			}

			if(memContent.size()!=MEMMAX) {
				log.error("Error reading file ");
				throw new Exception("Error : file is not equals as memory'size");
			}



		} catch (FileNotFoundException e) {
			log.error("Error : File not found");
		} catch (IOException e) {
			log.error("Error : cannot read file");
		}finally {
			setMemoryContent(memContent);
			fReader.close();
			bReader.close();
		}


	}



	public void exportTxt(String fOutput) throws IOException {	


		HashMap<String,String> memContent = getMemoryContent();
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(fOutput));

			int start = getBaseEEprom();
			int max = MEMMAX/16; //512

			String line="";

			for(int i = 0;i<max;i++) { //BASEEEPROM a la place de i;
				line="";
				for(int j=0;j<16;j++ ) {
					if(memContent.containsKey(getAdress(start+j))) { //DO THIS TO AVOID A NullPointer Exception (BUG ??)
						line+=memContent.get(getAdress(start+j)).concat(" ");
					}

				}
				w.write("000".concat(getAdress(start).toUpperCase())+": "+line+"\n");
				start+=16;
			}

		} catch (IOException e) {
			log.info("Error : Error during writing operation");
		}finally {
			w.close();
			File f = new File(fOutput);
			log.info(f.exists() ? "Export txt OK" : " Export txt NOK"); //print if file exist

		}
	}

	public void exportCSV(String fOutput, HashMap<String, ConfigKeys> hashMap) throws IOException {

		File f = new File(fOutput);
		BufferedWriter w = null;

		try {
			w = new BufferedWriter(new FileWriter(fOutput));

			w.write("key;lenght;offset;value\n");

			if(f.canWrite()) {
				for(String key : hashMap.keySet()) {
					ConfigKeys ck = hashMap.get(key);
					w.write(ck.getKeys()+";"+ck.getLenght()+";"+ck.getoffset()+";"+ck.getvalue()+"\n"); //concat
				}
				log.info(f.exists() ? "dump config OK" : " dump config NOK"); //print info if file was created 
			}else {
				log.error("Error : Cannot write into the file, maybe open ? ");
			}
		} catch (IOException e) {
			log.info("Error : error during writing operation" );
		}finally {
			w.close();

		}
	}


	public void exportHex(String fOutput) throws IOException { //this method need to be call when txt file is generated 

		File f = new File(fOutput);

		if(f.exists()) { //delete file if exists
			log.info(f.delete()? "Delete old file ".concat(fOutput)+ " OK ": "Error when delete old file".concat(fOutput));
		}

		String fileTxt = f.getParent().concat("\\deviceConfig.txt"); //to Create a temp txt file //VARIABLE STATIC
		exportTxt(fileTxt); //export to txt 


		Process p = null;

		try {

			p = Runtime.getRuntime().exec("srecord-1.63-win32\\srec_cat.exe "+fileTxt +" -HEX_DUMP -o "+fOutput +" -Intel"); //DEFINIR A UN SEUL ENDROIT

			p.waitFor(5000,TimeUnit.MILLISECONDS); //wait 10 seconds before falling in exception
			
			p = Runtime.getRuntime().exec("srecord-1.63-win32\\srec_cat.exe "+fOutput+" -Intel -offset 0x8000000 -o "+fOutput+" -Intel"); // set offset to 
			p.waitFor(5000, TimeUnit.MILLISECONDS);
			

		} catch (IOException e) {
			log.error("Error when executing command of exportHex");
		} catch (InterruptedException e) {
			log.error("Error process takes to long");
		}finally {

			p.destroy();
			log.info(f.exists() ? "Export Hex OK" : " Export Hex NOK");

			f = new File(fileTxt); //cleaning old txt file when 

			log.info("Delete temp file...");
			log.info(f.delete()? "Delete old temp file  ".concat(fileTxt)+ " OK ": "Error when delete old temp file".concat(fileTxt));
		}

	}
	
	/////////////////////////////////SETTERS////////////////////////////////////////////////////////////////

	private void setBaseEeprom(int baseEeprom) {
		this.BASEEEPROM=baseEeprom;

	}

	public void setMemoryContent(HashMap<String, String> memContent) {
		this.memContent=memContent;
	}

	private void setFileInput(String fInput) {
		this.fInput=fInput;
	}

	private void setFileOutput(String fOutput) {
		this.fOutput=fOutput;
	}


	/////////////////////////////////////////GETTERS////////////////////////////////////////////////////////////
	public int getBaseEEprom() {
		return this.BASEEEPROM;
	}

	public HashMap<String, String> getMemoryContent() {
		return this.memContent;
	}

	public String getFOutput() {
		return this.fOutput;
	}

	public String getFInput() {
		return this.fInput;
	}


	////////////////////////////////////UTILS///////////////////////////////////////////////////////////////////
	public String getAdress(int address) { //convert a int adress into Hex String
		return Integer.toHexString(address);


	}

	public int toHex(String  value) {
		return Integer.parseInt(value, 16);
	}
}
