import java.util.*;

import javax.xml.soap.Text;

import java.io.*;
import java.text.*;

public class Main {
	
	//Constants
	static final int IS_NONE = 0;
	static final int IS_INSTRUCTION_NO_OPERAND = 1;
	static final int IS_INSTRUCTION_WITH_OPERAND = 2;
	static final int IS_ADDRESS_WITH_VALUE = 3;
	static final int IS_ADDRESS_WITH_INSTRUCTION = 4;
	static final int IS_VALUE_WITHOUT_ADDRESS = 5;
	static final int IS_NOT_A_NUMBER = -999;
	static final String splitter = "\t|\\ "; //Either a tabulation, or a space
	static Scanner sc = new Scanner(System.in);
	static BufferedReader br;
	
	//Marie Variables
	static Vector<String> Code = new Vector<>();
	static Vector<Integer> M = new Vector<>();
	static Vector<String> Variables = new Vector<>();
	static Vector<String> VariablesM = new Vector<>();
	static String IR;
	static String LineCounter = "0";
	static String MAR;
	static String MBR;
	static int org = 0; //Initial address (ORIGIN)
	static int MCounter = 0; //Memory Counter
	static int AC;
	static int PC;
	static int input;
	static int output;
	
	
	public static void main(String[] args){
		
		final String filePath = "E:/Documents/MyMarieSimTest.txt";
		br = null;
		String line = "";
		
		try {
			br = new BufferedReader(new FileReader(filePath));
			while( (line = br.readLine()) != null ){
				read(line);
				PC++;
				if(Code.size()>0)
					System.out.println(Code.lastElement());
			}
			
			for(PC = 0; PC < Code.size(); PC++){
				ControlUnit(Code.get(PC));//Calling Control Unit to decode the instruction
			}
		}catch (FileNotFoundException e) {
			System.out.println("Error opening the file " + e.getMessage());
		}catch(IOException e){
			System.out.println("Error reading the file " + e.getMessage());
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("Error closing the file " + e.getMessage());
				}
			}
		}
	}
	
	private static void read(String line) {
//		String[] code = line.split(splitter);
		
		if(line.length()<1 || line.charAt(0) == '/'){
			return;
		}
		
		
		String[] code = new String[4];
		//Attempting to adapt to multiple kinds of tabulations (double tabs, whitespace and tab and such)
		char a, b;
		int temp = 0;
		int count = 0;
		boolean beg = false;
		for(int i = 0; i < line.length(); i ++){
			a = line.charAt(i);
			
			if( (line.charAt(0) == ' ' || line.charAt(0) == '\t') && count == 0 && i==0){
				code[count] = "";
				count++;
			}
			
			if(a == '/'){
				break;
			}
			
			if(count>2){
				break;
			}
							
			if( !(a==' ' || a=='\t') && !beg){
				beg = true;
				temp = i;
				if(i+1 == line.length()){
					code[count] = String.valueOf(a);
					count++;
				}
			}else if( (a==' ' || a=='\t') && beg){
				beg = false;
				code[count] = line.substring(temp, i);
				count++;
				temp = i;
			}else if( (a==' ' || a=='\t') && !beg){
				temp = i;
			}else if(i+1 == line.length() && beg){
				code[count] = line.substring(temp, i+1);
				count++;
			}
		}
		if(code[0].equals("End,")){
			System.out.println();
		}
		if(code[2] == null){
			code[2] = "\t";
		}
		
		line = (code[0].equals("")?"\t":code[0] + "\t")+ code[1] + (code[2].equals("")?"": "\t"+ code[2]);
		if(line.equals("\t"))
			return;
		if(code[1] == null)
			return;
//		code[0] = code[0].trim();
//		code[1] = code[1].trim();
//		code[2] = code[2].trim();
//		if(code.length>4){
//			code = line.split("\t\t| \t|"+splitter);
//		}
//		if(code[0].contains("/")){
//			return;
//		}
//		if(code.length<2){
//			return;
//		}
		
		
		
//		System.out.println(code[1]);
		boolean flag = false;
		//This flag will be used to know which Kind of Marie Program it is... Examples:
		//1st kind of program: Line number is not given (ORG is used)
		//2nd kind of program: Line number is given (and ORG is not used)
		if(Code.size() == 0 && code[1].toLowerCase().equals("org")){
			org = Integer.valueOf(code[2]);
		}
		if(Code.size() == 0){
			LineCounter = String.valueOf(org);
		}
		try{
			if(!code[0].equals("") && code[0].contains(",")){
				String[] var = code[0].split(",");
				saveVar(line);
			}else if(code[1].toLowerCase().contains("hex") || 
					code[1].toLowerCase().contains("dec")){
				saveVar(line);
			}
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println("Please remove the extra space in line " + (PC+1));
			System.out.println(line);
			e.printStackTrace();
		}catch(Exception e){
			System.out.println(line + code[1]);
			e.printStackTrace();
		}
		
		if(!flag){
			try {
				if(!line.toLowerCase().contains("org")){//Decrement
					Code.add(String.valueOf(LineCounter) + " " + line);
//						LineCounter = DecToHex(HexToDec(LineCounter)-1);
					//Increment
					LineCounter = DecToHex(HexToDec(LineCounter)+1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			Code.add(line);
		}
	}
	
	private static void saveVar(String line) {
		String[] code = line.split(splitter);
		String varName = code[0].split(",")[0];
		if(GetConstantCode(line, 0) == IS_ADDRESS_WITH_VALUE){
			int value = getValue(code[1], code[2]);
			save(varName, value);
			saveVariables(varName);
		}else if(GetConstantCode(line, 0) == IS_VALUE_WITHOUT_ADDRESS){
			int value = getValue(code[1], code[2]);
			save(LineCounter, value);
		}else if(GetConstantCode(line, 0) 
				== IS_ADDRESS_WITH_INSTRUCTION){
			saveVariables(varName);
		}
	}


	private static void saveVariables(String line){
		String variable = line + "=" + LineCounter;
		Variables.add(variable);
	}
	
	private static void saveVariablesWithValue(String line){
		String variable = line + "=" + MCounter;
		VariablesM.add(variable);
		save(getAddressOfVariable(line));
	}
	
	private static int getValueOfVariableM(String var){
		int index = getAddressOfVariableInMemory(var);
		return M.get(index);
	}
	
	private static void setValueOfVariableM(String var, int newValue){
		int index = getAddressOfVariableInMemory(var);
		M.set(index, newValue);
	}
	
	private static int getAddressOfVariableInMemory(String var){
		int index = 0;
		for(int i = 0; i < VariablesM.size(); i++){
			try{
				if(VariablesM.get(i).contains(var)){
					index = Integer.valueOf(VariablesM.get(i).split("=")[1]);
				}
			}catch(NullPointerException e){
				System.out.println("Error reading the code... Please check"
						+ " that there are no blank spaces in the code");
				System.out.println("Error in Line: " + (PC + 1));
				System.out.println(Code.get(PC));
				Halt();
			}
		}
		return index;
	}
	
	//Returns the address of the instruction given a variable
	private static int getAddressOfVariable(String var){
		for(int i = 0; i < Variables.size(); i++){
			String[] instr = Variables.get(i).split("=");
			if(instr[0].equals(var)){
//				int address = Integer.valueOf(instr[1]) - org;
				int address = 0;
				try {
					address = HexToDec(instr[1]) - HexToDec(String.valueOf(org));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return address;
			}
		}
		return IS_NOT_A_NUMBER;
	}
	
	//Given a variable, return the string with the corresponding instruction
	private static String getInstructionOfVariable(String var){
		var = var.split(splitter)[1].split(",")[0];
		for(int i = 0; i < Variables.size(); i++){
			String[] instr = Variables.get(i).split("=");
			if(instr[0].equals(var)
					&& GetConstantCode(Integer.valueOf(instr[1]))
					== IS_ADDRESS_WITH_INSTRUCTION) 
			{
				int index = 0;
				try {
					 index = HexToDec(instr[1]) - HexToDec(String.valueOf(org));
				} catch (Exception e) {
					e.printStackTrace();
				}
				String[] line = Code.get(index).split(splitter);
				return line[2];
			}
		}
		return "Undefined";
	}
	
	//Given the index (Starting from 0) return the LineCounter (Address)
	//equivalent.
	private static String getAddressOfCode(int index){
		return Code.get(index).split(splitter)[0];
	}
	
	//Given the index (Starting from 0) return the Variable (Address)
	private static String getVarAddressOfCode(int index){
		return Code.get(index).split(splitter)[1];
	}
	
	//Given the index (Starting from 0) return the Instruction (String)
	private static String getInstructionOfCode(int index){
		return Code.get(index).split(splitter)[2];
	}
	
	//Given the index(Starting from 0) return the Variable after the instruction
	private static String getValueOfCode(int index){
		return Code.get(index).split(splitter)[3];
	}
	
	//Given the index(Starting from 0) return the value of the 
	//variable of the instruction
	private static int getDeepValueOfCode(int index){
		return getAddressOfVariable(getValueOfCode(index));
	}
	
	//Gets the decimal value of the given number (Can be decimal or hex)
	private static int getValue(String regex, String value) {
		regex = regex.toLowerCase();
		switch(regex){
		case "dec":
			try{
				return Integer.valueOf(value);
			}catch (NumberFormatException e){
				System.out.println("Error compiling... please check the code"
						+ "is tabulated properly (no blank spaces)");
				System.out.println("Error in Line: " + (PC + 1));
				System.out.println(Code.get(PC));
				Halt();
			}
		case "hex":
			try {
				return HexToDec(value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return IS_NOT_A_NUMBER;//Not a DEC or HEX
	}
	
	private static String getValueHex(String regex, String value) {
		regex = regex.toLowerCase();
		switch(regex){
		case "dec":
			return DecToHex(Integer.parseInt(value));
		case "hex":
			return value;
		}
		return null;//Not a DEC or HEX
	}

	//Converts a hexadecimal number to decimal given a string.
	private static int HexToDec(String value) throws Exception{
		int result = 0;
		value = value.toLowerCase();
		for(int i = 0; i < value.length(); i++){
			result+=(HexDecEquivalence(value.charAt(i)) * Math.pow(16, value.length()-i-1));
		}
		return result;
	}
	
	//Has the Hex equivalence in Decimal
	private static int HexDecEquivalence(char hex) throws Exception{
		int res;
		switch(hex){
		case 'a':
			return 10;
		case 'b':
			return 11;
		case 'c':
			return 12;
		case 'd':
			return 13;
		case 'e':
			return 14;
		case 'f':
			return 15;
		}
		res = Character.getNumericValue(hex);
		if(res == -1)//If it is not a number...
			throw new Exception();
		return res;
	}
	
	public static String DecToHex(int dec){
	    String digits = "0123456789ABCDEF";
	    if (dec <= 0) //Usually we won't have to deal with negative numbers
	    	return "0";
	    int base = 16;
	    String hex = "";
	    while (dec > 0) {
	        int digit = dec % base;
	        hex = digits.charAt(digit) + hex; 
	        dec = dec / base;
	    }
	    return hex;
	}
	
	//Returns the type of instruction it is in a constant code
	private static int GetConstantCode(String line){
		String[] code = line.split(splitter);
		if(code[1].contains(",")){
			if(getValue(code[2], code[3])!=IS_NOT_A_NUMBER){
				return IS_ADDRESS_WITH_VALUE;
			}else{
				return IS_ADDRESS_WITH_INSTRUCTION;
			}
		}else if(code[1].equals("") && (code[2].toLowerCase().contains("hex") || 
				code[2].toLowerCase().contains("dec")) ){
			return IS_VALUE_WITHOUT_ADDRESS;
		}else if(!code[2].equals("")){
			if(code.length>3 && code[3].equals("")){
				return IS_INSTRUCTION_NO_OPERAND;
			}else{
				return IS_INSTRUCTION_WITH_OPERAND;
			}
		}
		return IS_NONE;
	}
	
	//This will do the same as the previous but will use code[0] instead of
	//code[0] since there will not be line numbers in the lines
	private static int GetConstantCode(String line, int initial){
		String[] code = line.split(splitter);
		
		if(code[0].contains(",")){
			if(code.length > 2 && getValue(code[1], code[2])!=IS_NOT_A_NUMBER){
				return IS_ADDRESS_WITH_VALUE;
			}else{
				return IS_ADDRESS_WITH_INSTRUCTION;
			}
		}else if(code[0].equals("") && (code[1].toLowerCase().contains("hex") || 
				code[1].toLowerCase().contains("dec")) ){
			return IS_VALUE_WITHOUT_ADDRESS;
		}else if(!code[1].equals("")){
			if(code.length>3 && code[2].equals("")){
				return IS_INSTRUCTION_NO_OPERAND;
			}else{
				return IS_INSTRUCTION_WITH_OPERAND;
			}
		}
		return IS_NONE;
	}
	
	//Gets the constant code of an instruction given the line number 
	//(From ORG)
	private static int GetConstantCode(int line){
		return GetConstantCode(Code.get(line-org));
	}
	
	//Function to know if a given string is a number (or even a hexadecimal)
	private static boolean isNumber(String line){
		try{
			double d = Double.parseDouble(line);
		}catch(Exception e){
			try{
				int decimal = HexToDec(line);
				return true;
			}catch(Exception exc){
				return false;
			}
		}
		return true;
	}

	//Gets a line of code, and decrypts it.
	private static void ControlUnit(String line){
		try{
			String[] splitted = line.split(splitter);
			String address = "";
			if(splitted.length>3)
				address = (splitted[3].equals("")?null:splitted[3]);
			else
				address = splitted[2];
			IR = getInstructionOfCode(PC);
			MAR = address;
			PerformInstruction();
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println(line);
			System.out.println("Please remove the extra line in the line " + (PC+1) + " " + e.getMessage());
		}
	}
	
	//Given a string, it knows which function to use.
	private static void PerformInstruction() {
		IR = IR.toLowerCase();
		switch(IR){
		case "load":
			Load(MAR);
			break;
		case "loadi":
			LoadI(MAR);
			break;
		case "input":
			Input();
			break;
		case "output":
			Output();
			break;
		case "store":
			Store(MAR);
			break;
		case "add":
			Add(MAR);
			break;
		case "addi":
			AddI(MAR);
			break;
		case "subt":
			Subt(MAR);
			break;
		case "clear":
			Clear();
			break;
		case "jump":
			Jump(MAR);
			break;
		case "jumpi":
			JumpI(MAR);
			break;
		case "jns":
			JnS(MAR);
			break;
		case "skipcond":
			Skipcond(MAR);
			break;
		case "halt":
			Halt();
			break;
		case "org":
			//Already handled in the beginning
			break;
		default:
			System.out.println("Invalid instruction at line " + (PC + 1) + ": " + IR );
			System.out.println(Code.get(PC));
			System.out.println("address = " + MAR);
			System.exit(-1);
		}
		
	}
	
	//Saves a decimal value in memory.
	private static void save(int decValue){
		M.add(decValue);
	}
	
	private static void save(String var, int value){
		save(value);
		VariablesM.add(var+"="+MCounter);
		MCounter++;
	}
	
	//Second method using the Registers
	//Copy the PC to the MAR
	private static void PCtoMAR(){
		MAR = getAddressOfCode(PC);
	}
	
	//Marie Functions
	private static void Load(String var){
		AC = getValueOfVariableM(var);
	}
	
	private static void LoadI(String var){
		AC = IndirectAdd(var);
	}
	
	private static void Input(){
		System.out.println("Enter a number");
		input = sc.nextInt();
		AC = input;
	}
	
	private static void Output(){
		System.out.println(AC);
	}
	
	private static void Jump(String var){
		PC = getAddressOfVariable(var) - 1; //Minus 1 since one it finishes the current task,
		//it will ad one to the PC anyways;
	}
	
	private static void JumpI(String var) {
		PC = getValueOfVariableM(var);
	}
	
	private static void JnS(String var){
		setValueOfVariableM(var, PC);
		PC = getAddressOfVariable(var);
		//It is supposed to be +1 but the for loop adds one already
	}
	
	private static void Store(String var){
		setValueOfVariableM(var, AC);
	}
	
	private static void Add(String var){
		AC = AC + getValueOfVariableM(var);
	}
	
	private static void AddI(String var){
		AC = AC + IndirectAdd(var);
	}
	
	//Given a variable, use its value to get the value in memory
	private static int IndirectAdd(String var){
//		int address = getAddressOfCode(Integer.parseInt(var));
		String address = DecToHex(getValueOfVariableM(var));
		return getValueOfVariableM(address);
	}
	
	private static void Skipcond(String cond){
		switch(cond){
		case "000":
			if(AC<0)
				PC++;
			break;
		case "400":
			if(AC == 0)
				PC++;
			break;
		case "800":
			if(AC > 0)
				PC++;
			break;
		default:
			System.out.println("Invalid condition for SkipCond " + cond);
			try{
				br.close();
			}catch (Exception e){
				System.out.println("Error closing the file " + e.getMessage());
			}
			sc.close();
			System.exit(-2);
		}
	}
	
	private static void Subt(String var){
		AC = AC - getValueOfVariableM(var);
	}
	
	private static void Clear(){
		AC = 0;
	}
		
	private static void Halt(){
		System.out.print("End of program, AC = ");
		Output();
		try{
			br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		sc.close();
		System.exit(1);
	}
}
