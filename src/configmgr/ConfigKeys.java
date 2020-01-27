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
