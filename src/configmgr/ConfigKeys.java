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

public class ConfigKeys { 
	private String key,offset,value;  
	private int lenght;

	public ConfigKeys() { //faire une verification de la cohérence des donnees 
		
	}
	
	
	public void setLenght(String lenght) {
		this.lenght=Integer.parseInt(lenght,16);
	}
	
	public void setKeys(String key) {
		this.key=key;
	}
	
	public void setOffset(String offset) {
		this.offset=offset;
	}
	public void setValue(String value) {
		this.value=value;
	}
	
	public String getLenght() {
		return Integer.toString(this.lenght,16);
	}
	
	public String getKeys() {
		return this.key;
	}
	
	public String getoffset() {
		return this.offset;
	}
	
	public String getvalue() {
		return this.value;
	}
}
