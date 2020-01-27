package configmgr;


/*
 * Author : Joel Wiki
 * Date : 28/10/2019
 * Date of last review : 07/11/2019
 * Company : Wyres
 * Function : This class help to manage the memory content
 * 
 */

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MemoryManager {

	private static int MEMMAX = 8192; //in specs of STM32L151CC, EEPROM's size is 8KB 
	int baseEeprom; //8080000 simplified by 80000
	int nbTags; //numbers of keys of wyres object 

	private String layout; //define schema of data
	private String keysSection; // OFFSET OF SECTION OF KEYS 
	private String valuesSection ; //OFFSET OF VALUES,  

	private int nextValueOff;
	private int nextKeyOffset; //next avaible offset 

	////TODO CREER DES VARIABLES LOCALE 

	private HashMap<String,String> memContent =new HashMap<String,String>(); //contains all data of txt file
	private HashMap<String,ConfigKeys> kV= new HashMap<String,ConfigKeys>(); //each keys his value 


	private final Logger log = LoggerFactory.getLogger(MemoryManager.class);


	public MemoryManager() {
	}


	
	public void extractLayout(int baseEeprom, HashMap<String, String> memContent) throws Exception { // 


		String keysSection;
		String valuesSections;
		
		int tags;
		String line="";

		for(int i=0;i<=15;i++) {
			line+=memContent.get(adressToString(baseEeprom+i));
		}


		setLayout(line);
		setBaseEeprom(baseEeprom);
		

		if(line.length()<=0) {
			throw new Exception("Error : line is empty");
		}


		tags= Integer.parseInt(line.substring(0,2),16);

		if(Integer.valueOf(line.substring(2,4),16)!=tags) { //check if the number of keys are correct else exception
			throw new Exception("Error : number of the keys are not the same");
		}

		setNbTags(tags);
		
		keysSection=adressToString(baseEeprom+Integer.parseInt(line.substring(6, 8).concat(line.substring(4,6)),16));

		setKeysSection(keysSection);

		valuesSections=adressToString(baseEeprom+Integer.parseInt(line.substring(10, 12).concat(line.substring(8, 10)),16));
		
		setValuesSection(valuesSections);

	}

	public void addTags(ArrayList<String> aList) throws Exception { //method to check if the keys exists, if exists do an update

		for(int i = 0; i<aList.size();i++) {

			if(isValid(aList.get(i))) {

				String tag,lenght,value;

				String val = aList.get(i);

				String[] tlv = val.split(":",3);

				tag = tlv[0];
				lenght=tlv[1];
				value=tlv[2];

				if(kV.containsKey(tag)) { //key already exist

					
					System.out.println("Update key : " +tag);

					ConfigKeys ck = kV.get(tag); //create a	 new object ConfigKeys and get object associated with tag 

					int lMemory = Integer.parseInt(ck.getLenght(),16) *2; //get lenght of info already stored in memory 
						

					
					if(lMemory!=ck.getvalue().length()) {
						log.error("Error : value of the key : " + tag + " is not the same lenght of recorded");
						throw new Exception("Error : value of the key : " + tag + " is not the same lenght of recorded");
					}
					
					if(value.length()!=ck.getvalue().length()) {
						log.error("Error : value of the key : " + tag + " is not the same lenght of recorded");
						throw new Exception("Error : value of the key : " + tag + " is not the same lenght of recorded");
					}
					
					
					

					ck.setValue(value);

					changeMemContent(ck);
					//this.nextOff(ck); //update next Offset available
					//this.nbTags++;

				}else { //new key 

					System.out.println("New key : " +tag);
					ConfigKeys ck = new ConfigKeys();

					ck.setKeys(tag);
					ck.setLenght(padVal(lenght,2));		
					ck.setOffset(padVal(adressToString(getNextValuesOffset()),4));
					ck.setValue(value);

					kV.put(tag, ck);
					this.nbTags++;
					changeMemContent(ck);
					this.nextOff(ck); //update next Offset available

				}

			}else {

				throw new Exception("Error : TLV is not valid");
			}
		}

	}


	private boolean isValid(String values) {

		String[] tlv = values.split(":", 3);

		if(tlv.length!=3) {
			log.error("Error syntax of TLV is not valid");
			return false;
		}


		if(!tlv[0].matches("^[0-9a-fA-F]{4,4}$")){
			log.error("Error tag is not in correct format : lenght of the keys must be 4 and numbers between 0 and ");
			return false;
		}

		//one byte
		if(!tlv[1].matches("^[0-9a-fA-F]{1,2}$")){
			log.error("Error lenght must be in correct format : must be between 0 and 99");
			return false;
		}


		

		if(!tlv[2].matches("^.*[0-9a-fA-F]$")) {
			log.error("Error value is not in hexadecimal format");
			return false;
		}


		if((tlv[2].length() % 2 !=0)) {
			log.error("Error value must be a multiple of 2 (byte)");
			return false;
		}

		int lenValue = Integer.parseInt(tlv[1],16); 


		if(lenValue*2!=tlv[2].length()) {
			log.error("Error : lenght registered isn't match with value's lenght");
			return false;
		}

		return true;
	}

	private void changeMemContent(ConfigKeys ck) { //this method is made to replace into memory 


		HashMap<String,String> memContent = getMemoryContent(); //getMemContent

		int nextKeyOffset = getNextKeyOffset(); 
		int baseEeprom = getBaseEeprom();
		int lenght = Integer.parseInt(ck.getLenght(),16) *2; 
		int addressToRead = Integer.valueOf(baseEeprom+Integer.valueOf(ck.getoffset(),16));
		int nbTags = getNbTags();
		String value = ck.getvalue();


		String[] data = new String[lenght]; //1 byte per entry, for exemple 38B8EBE00000FFFF it's 8 bytes 
		int max = data.length/2; 


		for(int i=0;i<max;i++) {
			data[i]=value.substring(i*2,(i*2)+2); //putting changed data into array
		}


		max = data.length/2 ;

		
		String[] content = new String[5];

		content[0] = ck.getKeys().substring(2, 4);
		content[1] = ck.getKeys().substring(0, 2);
		content[2] = padVal(ck.getLenght(),2);
		content[3] = (ck.getoffset().substring(2,4));
		content[4] = ck.getoffset().substring(0, 2);





		for(int i = 0 ; i<5;i++) { //to write change of keys section 
			memContent.put(adressToString(nextKeyOffset+i), content[i].toUpperCase());
		}

		for(int i = 0;i<max;i++) {
			memContent.put(adressToString(addressToRead+i), data[i]); //writing data into values
		}


		//update nb keys in memContent 
		memContent.put(adressToString(baseEeprom),padVal(Integer.toHexString(nbTags),2)); //pad 
		memContent.put(adressToString(baseEeprom+1),padVal(Integer.toHexString(nbTags),2));


		setMemContent(memContent); //update object 
	}

	public void extractKeys(HashMap<String, String> memContent) throws Exception { 

		setMemContent(memContent);

		int nbKeys = getNbTags();
		String keysSection = getKeysSection();

		String[] values = new String[nbKeys];
		String line="";



		int max = getNbTags()*5; //5 bytes for each tag TLV



		for(int i=0;i<=max;i++) {
			line+=memContent.get((adressToString(Integer.parseInt(keysSection,16)+i)));
		}


		for(int i=0;i<nbKeys;i++){ //for each keys, get all infos : lenght, value, offset in memory and  
			values[i]=line.substring(i*10, (i*10)+10);
			addConfigKey(values[i]);	
		}
	}

	private void addConfigKey(String values) throws Exception { 

		String valuesSection = getValuesSection();
		HashMap<String,String> memContent = getMemoryContent();

		ConfigKeys ck= new ConfigKeys();

		ck.setKeys(values.substring(2, 4).concat(values.substring(0,2))); 
		ck.setLenght(values.substring(4,6)); //Longueur maximal 
		ck.setOffset(values.substring(8, 10).concat(values.substring(6,8))); //BUG, a revoir 	
		nextOff(ck);
						//log.info("Keys : "+ck.getKeys());
						//log.info("lenght : "+ck.getLenght());
						//log.info("offset : "+ck.getoffset());

		int lenghtOffset = ck.getoffset().length();

		String offset = ck.getoffset();

		String preAddress =  valuesSection.substring(0,valuesSection.length()- lenghtOffset);


		String addressToRead= adressToString(Integer.valueOf(preAddress+(offset.substring(0, lenghtOffset)),16)).toLowerCase(); // Method adress converter 

		int address  = Integer.parseInt(addressToRead, 16); //parse to int value to convert into string in future 


		int max = Integer.valueOf(ck.getLenght(),16); //ENTIER 

		String value="";


		for(int i=0; i<max;i++) {
			value+=memContent.get(adressToString(address+i)); 
		}

		ck.setValue(value);

		this.kV.put(ck.getKeys(), ck); //putting into hash map 


	}

	
	public void createConfig(ArrayList<String> tagsList) {

		setBaseEeprom(0x80000);
		setKeysSection("80010");
		setValuesSection("803FD");
		setNextValueOffset(Integer.parseInt("3FD",16));
		setNextkeyOffset(Integer.parseInt(getKeysSection(),16));

		int baseEeprom = getBaseEeprom(); 
		HashMap<String,String> memContent = new HashMap<String,String>();


		//fill a hashmap with values at 00;

		for(int i=0;i<MEMMAX;i++) { 
			memContent.put(adressToString(baseEeprom+i), "00");
		}

		String nb = Integer.toString(getNbTags()); //0 

		if(tagsList.size()<10) { //to avoid a half byte 
			nb = padVal(Integer.toString(tagsList.size()),2);
		}else {
			nb =Integer.toString(tagsList.size());
		}

		String layout= nb+nb+"1000FD0300000000000000000000"; //adding 0 to the end to match correctly with memory


		for(int i= 0 ; i<16;i++) {
			memContent.put(adressToString(baseEeprom+i), layout.substring(i*2,(i*2+2)));
		}

		setMemContent(memContent);
		//adding tag 
		try {
			addTags(tagsList);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	
	private void nextOff(ConfigKeys ck) { //this method was create to define where is the next avaible offset for a new t:l:v and for value

		int offset = Integer.parseInt(ck.getoffset(),16) + Integer.parseInt(ck.getLenght(),16);
		int nextValueOff = getNextValuesOffset();


		if(nextValueOff<offset) {
			setNextValueOffset(offset);
			
		}
		setNextkeyOffset(Integer.parseInt(getKeysSection(),16)+(getNbTags()*5)); //set nextKeyOffset to 
	}

	
	///////////////////////////GETTERS/////////////////////////

	public HashMap<String, ConfigKeys> getKV() {
		return this.kV;
	}

	public HashMap<String, String> getMemoryContent() {
		return this.memContent;
	}

	public String getLayout() {
		return this.layout;
	}

	public String getKeysSection() {
		return this.keysSection;
	}

	public String getValuesSection() {
		return this.valuesSection;
	}

	private int getNbTags() {
		return this.nbTags;
	}

	private int getBaseEeprom() {
		return this.baseEeprom;
	}

	private int getNextKeyOffset() {
		return this.nextKeyOffset;
	}

	private int getNextValuesOffset() {
		return this.nextValueOff;
	}



	//////////////////////////////SETTERS/////////////////////////////////////


	private void setBaseEeprom(int baseEeprom) {
		this.baseEeprom=baseEeprom;
	}

	private void setNextValueOffset(int nextValueOffset) {
		this.nextValueOff=nextValueOffset;
	}

	private void setNextkeyOffset(int nextValueOffset) {

		this.nextKeyOffset=nextValueOffset;
	}

	private void setNbTags(int tags) {
		this.nbTags=tags;		
	}
	private void setLayout(String layout) {
		this.layout=layout;
	}

	public void setKeysSection(String keysSection) {
		this.keysSection=keysSection;
	}

	public void setValuesSection(String valuesSection) {
		this.valuesSection=valuesSection;
	}

	public void setMemContent(HashMap<String,String> memContent) {
		this.memContent=memContent;
	}


	///////////////////////////////////UTILS////////////////////////////////

	public String adressToString(int address) { //convert a int adress into Hex String
		String value = Integer.toHexString(address);
		return value;

	}

	public String padVal(String value,int size) { //pad value 
		value = StringUtils.leftPad(value, size,"0");
		return value;
	}


}


