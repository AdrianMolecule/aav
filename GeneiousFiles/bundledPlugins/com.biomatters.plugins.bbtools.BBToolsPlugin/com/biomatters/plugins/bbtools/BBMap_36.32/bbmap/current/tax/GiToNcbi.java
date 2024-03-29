package tax;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import align2.Tools;
import fileIO.ByteFile;
import fileIO.ReadWrite;

/**
 * @author Brian Bushnell
 * @date Mar 10, 2015
 *
 */
public class GiToNcbi {
	
	public static void main(String[] args){
		ReadWrite.USE_UNPIGZ=true;
		ReadWrite.USE_PIGZ=true;
		ReadWrite.ZIPLEVEL=8;
		initialize(args[0]);
		if(args.length>2){//Run a test
			test(args);
		}else if(args.length==2){//Write array
			ReadWrite.write(array, args[1], true);
		}
	}
	
	public static void test(String[] args){
		System.err.println(getID(1000));
		System.err.println(getID(10000));
		System.err.println(getID(10001));
		System.err.println(getID(10002));
		System.err.println(getID(10003));
		System.err.println(getID(10004));
		System.err.println(getID(10005));
		System.err.println(getID(100000));
		System.err.println(getID(1000000));
		System.err.println(getID(10000000));
		
		TaxTree tree=null;
		if(args.length>1){
			tree=new TaxTree(args[1], args[2]);
		}
		
		System.err.println("Strings:");
		int x;
		x=getID("gi|18104025|emb|AJ427095.1| Ceratitis capitata centromeric or pericentromeric satellite DNA, clone 44");
		System.err.println(x);
		if(tree!=null){
			System.err.println(tree.getNode(x));
			tree.incrementRaw(x, 30);
		}
		x=getID("gi|15982920|gb|AY057568.1| Arabidopsis thaliana AT5g43500/MWF20_22 mRNA, complete cds");
		System.err.println(x);
		if(tree!=null){
			System.err.println(tree.getNode(x));
			tree.incrementRaw(x, 40);
		}
		x=getID("gi|481043749|gb|KC494054.1| Plesiochorus cymbiformis isolate ST05-58 internal transcribed spacer 2, partial sequence");
		System.err.println(x);
		if(tree!=null){
			System.err.println(tree.getNode(x));
			tree.incrementRaw(x, 20);
		}
		
		if(tree!=null){
			tree.percolateUp();
			ArrayList<TaxNode> nodes=tree.gatherNodesAtLeastLimit(35);
			for(TaxNode n : nodes){
				System.err.println(n);
			}
		}
	}
	
	public static int parseGiToNcbi(String s){
		int x=parseGiNumber(s);
		assert(x>=0);
		assert(array!=null) : "To use gi numbers, you must load a gi table.";
//		if(x>=array.length || array[x]<0){x=(int)(Math.random()*array.length);} //Test to make sure array is nonempty.
		if(x>=0 && x<array.length){return array[x];}
		assert(x<array.length) : "The GI number "+x+" is too big.\n"
				+ "Please update the gi table with the latest version from NCBI as per the instructions in gitable.sh.\n"
				+ "To ignore this problem, please run with the -da flag.\n";
		return -1;
	}
	
	public static int parseGiToNcbi(byte[] s){
		int x=parseGiNumber(s);
		if(x>=0){return array[x];}
		return -1;
	}
	
	/** Parse a gi number, or return -1 if formatted incorrectly. */
	static int parseGiNumber(String s){
		if(s==null || s.length()<4){return -1;}
//		System.err.println("a");
		if(s.charAt(0)=='>'){return getID(s.substring(1));}
//		System.err.println("b");
		if(!s.startsWith("gi")){return -1;}
//		System.err.println("c");
		char delimiter='|';
//		System.err.println("d");
		int initial=s.indexOf(delimiter);
//		System.err.println("e");
		if(initial<0){
			delimiter='_';
//			System.err.println("f");
			initial=s.indexOf(delimiter);
//			System.err.println("g");
			if(initial<0){return -1;}
		}
//		System.err.println("h");
		if(!Character.isDigit(s.charAt(initial+1))){return -1;}
//		System.err.println("i");
		
		int number=0;
		for(int i=initial+1; i<s.length(); i++){
			char c=s.charAt(i);
			if(c==delimiter){break;}
			assert(Character.isDigit(c));
			number=(number*10)+(c-'0');
		}
//		System.err.println("j: "+number);
		return number;
	}
	
	/** Parse a ncbi number, or return -1 if formatted incorrectly. */
	static int parseNcbiNumber(String s){
		if(s==null || s.length()<6){return -1;}
		if(s.charAt(0)=='>'){return getID(s.substring(1));}
		if(!s.startsWith("ncbi")){return -1;}
		char delimiter='|';
		int initial=s.indexOf(delimiter);
		if(initial<0){
			delimiter='_';
			initial=s.indexOf(delimiter);
			if(initial<0){return -1;}
		}
		if(!Character.isDigit(s.charAt(initial+1))){return -1;}
		
		int number=0;
		for(int i=initial+1; i<s.length(); i++){
			char c=s.charAt(i);
			if(c==delimiter){break;}
			assert(Character.isDigit(c));
			number=(number*10)+(c-'0');
		}
		return number;
	}
	
	/** Get the taxID from a header starting with a taxID or gi number */
	public static int getID(String s){
		int x=parseGiNumber(s);
		if(x>=0){return array[x];}
		return parseNcbiNumber(s);
	}
	
	/** Parse a gi number, or return -1 if formatted incorrectly. */
	static int parseGiNumber(byte[] s){
		if(s==null || s.length<4){return -1;}
		if(!Tools.startsWith(s, "gi") && !Tools.startsWith(s, ">gi")){return -1;}
		char delimiter='|';
		int initial=Tools.indexOf(s, (byte)delimiter);
		if(initial<0){
			delimiter='_';
			initial=Tools.indexOf(s, (byte)delimiter);
			if(initial<0){return -1;}
		}
		if(!Character.isDigit(s[initial+1])){return -1;}
		
		int number=0;
		for(int i=initial+1; i<s.length; i++){
			byte c=s[i];
			if(c==delimiter){break;}
			assert(Character.isDigit(c));
			number=(number*10)+(c-'0');
		}
		return number;
	}
	
	/** Parse a gi number, or return -1 if formatted incorrectly. */
	static int parseNcbiNumber(byte[] s){
		if(s==null || s.length<4){return -1;}
		if(!Tools.startsWith(s, "ncbi") && !Tools.startsWith(s, ">ncbi")){return -1;}
		char delimiter='|';
		int initial=Tools.indexOf(s, (byte)delimiter);
		if(initial<0){
			delimiter='_';
			initial=Tools.indexOf(s, (byte)delimiter);
			if(initial<0){return -1;}
		}
		if(!Character.isDigit(s[initial+1])){return -1;}
		
		int number=0;
		for(int i=initial+1; i<s.length; i++){
			byte c=s[i];
			if(c==delimiter){break;}
			assert(Character.isDigit(c));
			number=(number*10)+(c-'0');
		}
		return number;
	}
	
	/** Get the taxID from a header starting with a taxID or gi number */
	public static int getID(byte[] s){
		int x=parseGiNumber(s);
		if(x>=0){return array[x];}
		return parseNcbiNumber(s);
	}
	
	/** Get the taxID from a gi number */
	public static int getID(int gi){
		assert(gi>=0) : gi;
		assert(gi<array.length) : gi+", "+array.length;
		return array[gi];
	}
	
	public static void initialize(String fname){
		assert(fname!=null);
		if(fileString==null || !fileString.equals(fname)){
			synchronized(GiToNcbi.class){
				if(!initialized || fileString==null || !fileString.equals(fname)){
					fileString=fname;
					if(fname.contains(".int1d")){
						array=ReadWrite.read(int[].class, fname, true);
					}else{
						array=makeArray(fname);
					}
				}
				initialized=true;
			}
		}
	}
	
	public static boolean isInitialized(){return initialized;}
	
	public static synchronized void unload(){
		array=null;
		fileString=null;
		initialized=false;
	}
	
	private static int[] makeArray(String fnames){
		String[] split;
		if(new File(fnames).exists()){split=new String[] {fnames};}
		else{split=fnames.split(",");}
		
		long max=0;
		for(String s : split){
			max=Tools.max(max, findMaxID(s));
		}
		
		assert(max<Integer.MAX_VALUE) : "Overflow.";
		int[] x=new int[(int)max+1];
		Arrays.fill(x, -1);
		
		long total=0;
		for(String s : split){
			long count=fillArray(s, x);
			total+=count;
		}
		return x;
	}
	
	private static long findMaxID(String fname){
		ByteFile bf=ByteFile.makeByteFile(fname, false, true);
		long count=0, max=0;
		byte[] line=bf.nextLine();
		while(line!=null){
			count++;
			int tab=Tools.indexOf(line, (byte)'\t');
			long gi=Tools.parseLong(line, 0, tab);
			max=Tools.max(max, gi);
			line=bf.nextLine();
		}
		bf.close();
		return max;
	}
	
	private static long fillArray(String fname, int[] x){
		ByteFile bf=ByteFile.makeByteFile(fname, false, true);
		long count=0;
		byte[] line=bf.nextLine();
		while(line!=null){
			count++;
			int tab=Tools.indexOf(line, (byte)'\t');
			int gi=Tools.parseInt(line, 0, tab);
			int ncbi=Tools.parseInt(line, tab+1, line.length);
			assert(x[gi]==-1 || x[gi]==ncbi) : "Contradictory entries for gi "+gi+": "+x[gi]+" -> "+ncbi;
			x[gi]=ncbi;
			line=bf.nextLine();
		}
		if(verbose){System.err.println("Count: "+count);}
		bf.close();
		return count;
	}
	
	private static int[] array;
	private static String fileString;
	
	public static boolean verbose=false;
	private static boolean initialized=false;
}
