package stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import align2.Shared;
import align2.Tools;

import dna.Gene;
import dna.Timer;

import fileIO.FileFormat;
import fileIO.ReadWrite;
import structures.ListNum;

/**
 * @author Brian Bushnell
 * @date Feb 13, 2013
 *
 */
public class FastaReadInputStream extends ReadInputStream {
	
	public static void main(String[] args){
		
		int a=20, b=Integer.MAX_VALUE;
		if(args.length>1){a=Integer.parseInt(args[1]);}
		if(args.length>2){b=Integer.parseInt(args[2]);}
		if(args.length>3){MIN_READ_LEN=Integer.parseInt(args[3]);}
		if(args.length>4){TARGET_READ_LEN=Integer.parseInt(args[4]);}
		if(TARGET_READ_LEN<1){
			TARGET_READ_LEN=Integer.MAX_VALUE;
			SPLIT_READS=false;
		}
		
		Timer t=new Timer();
		
		FastaReadInputStream fris=new FastaReadInputStream(args[0], false, false, false, Shared.READ_BUFFER_MAX_DATA);
		Read r=fris.next();
		int i=0;
		
		while(r!=null){
			if(i<a){System.out.println(r.toText(false));}
			r=fris.next();
			if(++i>=a){break;}
		}
		while(r!=null && i++<b){r=fris.next();}
		t.stop();
		System.out.println("Time: \t"+t);
	}
	
	public static ArrayList<Read> toReads(String fname){
		if(fname==null){return null;}
		ArrayList<Read> list=new ArrayList<Read>();

		/* Start an input stream */
		FileFormat ff=FileFormat.testInput(fname, FileFormat.FASTA, null, false, true);
		ConcurrentReadInputStream cris=ConcurrentReadInputStream.getReadInputStream(-1L, false, ff, null);
		cris.start(); //4567
		ListNum<Read> ln=cris.nextList();
		ArrayList<Read> reads=(ln!=null ? ln.list : null);

		/* Iterate through read lists from the input stream */
		while(reads!=null && reads.size()>0){
			list.addAll(reads);
			
			/* Dispose of the old list and fetch a new one */
			cris.returnList(ln.id, ln.list.isEmpty());
			ln=cris.nextList();
			reads=(ln!=null ? ln.list : null);
		}
		/* Cleanup */
		cris.returnList(ln.id, ln.list.isEmpty());
//		errorState|=ReadWrite.closeStream(cris);
		
		return list;
	}
	
	public FastaReadInputStream(String fname, boolean interleaved_, boolean amino_, boolean allowSubprocess_, long maxdata){
		this(FileFormat.testInput(fname, FileFormat.FASTA, FileFormat.FASTA, 0, allowSubprocess_, false, false), interleaved_, amino_, maxdata);
	}
	
	public FastaReadInputStream(FileFormat ff, boolean interleaved_, boolean amino_, long maxdata){
		name=ff.name();
		amino=amino_;
		flag=(amino ? Read.AAMASK : 0);
		
		if(!fileIO.FileFormat.hasFastaExtension(name) && !name.startsWith("stdin")){
			System.err.println("Warning: Did not find expected fasta file extension for filename "+name);
		}
		
		interleaved=interleaved_;
		allowSubprocess=ff.allowSubprocess();
		minLen=MIN_READ_LEN;
		maxLen=(SPLIT_READS ? (TARGET_READ_LEN>0 ? TARGET_READ_LEN : Integer.MAX_VALUE) : Integer.MAX_VALUE);
		MAX_DATA=maxdata>0 ? maxdata : Shared.READ_BUFFER_MAX_DATA;
		
		ins=open();
		
		assert(settingsOK());
	}
	
	@Override
	public Read next() {
		if(!hasMore()){
			if(verbose){System.err.println("hasMore() returned false;  currentList="+
					(currentList==null ? null : currentList.size())+", nextReadIndex="+nextReadIndex+", consumed="+consumed);}
			return null;
		}
		Read r=currentList.set(nextReadIndex, null);
		nextReadIndex++;
		consumed++;
		return r;
	}
	
	@Override
	public ArrayList<Read> nextList() {
		if(nextReadIndex!=0){throw new RuntimeException("'next' should not be used when doing blockwise access.");}
		if(currentList==null || nextReadIndex>=currentList.size()){
			boolean b=fillList();
		}
		ArrayList<Read> list=currentList;
		currentList=null;
		if(list==null || list.isEmpty()){
			list=null;
		}else{
			consumed+=list.size();
		}
		return list;
	}
	
	@Override
	public boolean hasMore() {
		if(currentList==null || nextReadIndex>=currentList.size()){
			if(open){
				fillList();
			}else{
//				assert(generated>0) : "Was the file empty?";
			}
		}
		return (currentList!=null && nextReadIndex<currentList.size());
	}
	
	@Override
	public void restart() {
		if(ins!=null){close();}
		assert(ins==null);
//		generated=0;
		consumed=0;
		nextReadIndex=0;
		nextReadID=0;
		currentList=null;
		header=null;
		bstart=0;
		bstop=0;
		
		currentSection=0;
		if(ins==null){
			ins=open();
		}else{
			assert(false) : "is should be null";
		}
	}
	
	@Override
	public final boolean close(){
		if(!open){return false;}
		open=false;
		assert(ins!=null);
		
		try {
			if(ins!=System.in){
				errorState|=ReadWrite.finishReading(ins, name, allowSubprocess);
			}
		} catch (Exception e) {
			System.err.println("Some error occured: "+e);
			errorState=true;
		}
		
		ins=null;
		return false;
	}
	
	@Override
	public boolean paired() {return interleaved;}
	
	@Override
	public void start() {}
	
	private final boolean fillList(){
//		assert(open);
		if(!open){
			currentList=null;
			return false;
		}
		assert(currentList==null || nextReadIndex>=currentList.size());
		nextReadIndex=0;
		currentList=new ArrayList<Read>(BUF_LEN);
		
		if(header==null){
			header=nextHeader();
			if(header==null){
				currentList=null;
				return false;
			}
		}
		long len=0;
		for(int i=0; i<BUF_LEN && len<MAX_DATA; i++){
			Read r=generateRead(0);
			if(r==null){break;}
			currentList.add(r);
			len+=r.length();
			if(interleaved){
				Read r2=generateRead(1);
				if(r2==null){break;}
				len+=r2.length();
				r.mate=r2;
				r2.mate=r;
			}
			nextReadID++;
			if(verbose){System.err.println("Generated a read; i="+i+", BUF_LEN="+BUF_LEN);}
//			if(i==1){assert(false) : r.numericID+", "+r.mate.numericID;}
		}
		
		return currentList.size()>0;
	}
	
	private final Read generateRead(int pairnum){
		if(verbose){System.err.println("Called generateRead(); bstart="+bstart+", bstop="+bstop+", currentSection="+currentSection+", header="+header);}
		assert(header!=null) : "Null header for fasta read - input file may be corrupt: "+name;
		if(bstart<bstop && buffer[bstart]==carrot){
			header=nextHeader();
			currentSection=0;
		}
		assert(header!=null);
		byte[] bases=nextBases();
		
		currentSection++;
		while(bases==null){
//			if(!open){return null;} //Should not be needed...
			header=nextHeader();
			if(header==null){
				close();
				return null;
			}
			bases=nextBases();
			currentSection=1;
		}
		assert(bases!=null);
		assert(bases.length>0);
		
		byte[] quals=null;
		if(FAKE_QUALITY){
			quals=new byte[bases.length];
			Arrays.fill(quals, (byte)(Shared.FAKE_QUAL));
		}
//		String hd=((currentSection==1 && !hitmax) ? header : header+"_"+currentSection);
		String hd=((!FORCE_SECTION_NAME && currentSection==1 && bases.length<=maxLen) ? header : header+"_part_"+currentSection);
//		assert(false) : FORCE_SECTION_NAME+", "+(currentSection==1)+", "+(bases.length<=maxLen)+", "+bases.length+", "+maxLen;
		assert(currentSection==1 || bases.length>0) : "id="+hd+", section="+currentSection+", len="+bases.length+"\n"+new String(bases);
		Read r=null;
		if(FASTQ.PARSE_CUSTOM){
			if(header!=null && header.indexOf('_')>0){
				String temp=header;
				if(temp.endsWith(" /1") || temp.endsWith(" /2")){temp=temp.substring(0, temp.length()-3);}
				String[] answer=temp.split("_");

				if(answer.length>=5){
					try {
						int trueChrom=Gene.toChromosome(answer[1]);
						byte trueStrand=Byte.parseByte(answer[2]);
						int trueLoc=Integer.parseInt(answer[3]);
						int trueStop=Integer.parseInt(answer[4]);
//						r=new Read(bases, trueChrom, trueStrand, trueLoc, trueStop, hd, quals, nextReadID);
						r=new Read(bases, trueChrom, trueLoc, trueStop, hd, quals, nextReadID, (flag|trueStrand));
						r.setSynthetic(true);
					} catch (NumberFormatException e) {
						FASTQ.PARSE_CUSTOM=false;
						System.err.println("Turned off PARSE_CUSTOM because could not parse "+new String(header));
					}
				}else{
					FASTQ.PARSE_CUSTOM=false;
					System.err.println("Turned off PARSE_CUSTOM because answer="+Arrays.toString(answer));
				}
			}else{
				FASTQ.PARSE_CUSTOM=false;
				System.err.println("Turned off PARSE_CUSTOM because header="+header+", index="+header.indexOf('_'));
			}
		}
		if(r==null){
//			r=new Read(bases, (byte)0, (byte)0, 0, 0, hd, quals, nextReadID);
			r=new Read(bases, -1, -1, -1, hd, quals, nextReadID, flag);
		}
		r.setPairnum(pairnum);
		if(verbose){System.err.println("Made read:\t"+(r.length()>1000 ? r.id : r.toString()));}
		return r;
	}
	
	private String nextHeader(){
		if(verbose){System.err.println("Called nextHeader(); bstart="+bstart+"; bstop="+bstop);}
		assert(bstart>=bstop || buffer[bstart]=='>' || buffer[bstart]<=slashr) : bstart+", "+bstop+", '"+(char)buffer[bstart]+"'";
		while(bstart<bstop && buffer[bstart]!='>'){bstart++;}
		int x=bstart;
		assert(bstart>=bstop || buffer[x]=='>') : bstart+", "+bstop+", '"+(char)buffer[x]+"'";
		while(x<bstop && buffer[x]>slashr){x++;}
		if(verbose){System.err.println("A: x="+x);}
		if(x<bstop && (buffer[x]<0x2 || buffer[x]==tab)){ //Handle deprecated 'SOH' symbol and tab
			while(x<bstop && (buffer[x]>slashr || buffer[x]<0x2 || buffer[x]==tab)){
				if(buffer[x]==0x1){buffer[x]=carrot;}
				x++;
			}
		}
		if(verbose){System.err.println("B: x="+x);}
		if(x>=bstop){
			int fb=fillBuffer();
			if(verbose){System.err.println("B: fb="+fb);}
			if(fb<1){
				if(verbose){System.err.println("Returning null from nextHeader()");}
				return null;
			}
			x=0;
			assert(bstart==0 && bstart<bstop && buffer[x]=='>') : "Improperly formatted fasta file; expecting '>' symbol.\n"+
				(buffer[x]=='@' ? "If this is a fastq file, please rename it with a '.fastq' extension.\n" : "")+
					bstart+", "+bstop+", "+(int)buffer[x]+", "+(char)buffer[x]; //Note: This assertion will fire if a fasta file starts with a newline.
			while(x<bstop && buffer[x]>slashr){x++;}
			if(x<bstop && (buffer[x]<0x2 || buffer[x]==tab)){ //Handle deprecated 'SOH' symbol and tab
				while(x<bstop && (buffer[x]>slashr || buffer[x]<0x2 || buffer[x]==tab)){
					if(buffer[x]==0x1){buffer[x]=carrot;}
					x++;
				}
			}
		}
		if(verbose){System.err.println("C: x="+x);}
		assert(x>=bstop || buffer[x]<=slashr);
		
		int start=bstart+1, stop=x;
		if(Shared.TRIM_READ_COMMENTS){
			for(int i=start; i<stop; i++){
				if(Character.isWhitespace(buffer[i])){
					stop=i;
					break;
				}
			}
		}

		String s=stop>start ? new String(buffer, start, stop-start) : "";
//		String s=new String(buffer, bstart+1, x-(bstart+1));
		if(verbose){System.err.println("Fetched header: '"+s+"'");}
		bstart=x+1;
		
		return s;
	}
	
	private byte[] nextBases(){
		if(verbose){System.err.println("Called nextBases()");}
		assert(open) : "Attempting to read from a closed file.  Current header: "+header;
		if(bstart>=bstop){
			int bytes=fillBuffer();
			if(bytes<1 || !open){return null;}
		}
		int x=bstart;
		int bases=0;
		
		if(!(x>=bstop || buffer[x]!='>')){
			if(WARN_IF_NO_SEQUENCE){
				synchronized(getClass()){
					System.err.println("Warning: A fasta header with no sequence was encountered.");
					WARN_IF_NO_SEQUENCE=false;
				}
			}
			assert(!ABORT_IF_NO_SEQUENCE) : "\n<START>"+new String(buffer, 0, Tools.min(x+1, buffer.length))+"<STOP>\n";
		}
		
//		assert(x>=bstop || buffer[x]!='>') : 
//			"A fasta header with no sequence was encountered.  To discard such headers, please re-run with the -da flag.";
		//"\n<START>"+new String(buffer, 0, Tools.min(x+1, buffer.length))+"<STOP>\n";
		
		while(x<bstop && bases<maxLen && buffer[x]!='>'){
			while(x<bstop && bases<maxLen && buffer[x]!='>'){
				if(buffer[x]>slashr){bases++;}
				x++;
			}
			assert(x==bstop || buffer[x]=='>' || bases==maxLen);
			if(x==bstop && bases<maxLen){
				int fb=fillBuffer();
				if(fb<1){
					x=bstop;
					if(verbose){System.err.println("Broke loop when fb="+fb+"; bstart="+bstart+", bstop="+bstop);}
					break;
				}
				x=bstart;
				bases=0;
			}
		}
		
		if(bases<minLen){
			assert(open) : "Attempting to read from a closed file.  Current header: "+header;
			bstart=x;
			if(verbose){System.err.println("Fetched "+bases+" bases; returning null.  bstart="+bstart+", bstop="+bstop/*+"\n"+new String(buffer)*/);}
			return null;
		}
		
		byte[] r=new byte[bases];
		
//		if(Read.TO_UPPER_CASE){
//			for(int i=bstart, j=0; j<bases; i++){
//				assert(i<x);
//				byte b=buffer[i];
//				//			if(verbose){System.err.println("grabbed base "+(char)b+" = "+b);}
//				if(b>slashr){
//					r[j]=(b<91 ? b : (byte)(b-32));//Convert to upper case
//					//				if(verbose){System.err.println("set to base "+(char)r[j]+" = "+r[j]);}
//					j++;
//				}
//			}
//		}else{
		for(int i=bstart, j=0; j<bases; i++){
			assert(i<x);
			byte b=buffer[i];
			if(b>slashr){
				r[j]=b;
				j++;
			}
		}
//		}
		
		if(verbose){System.err.println("Fetched "+bases+" bases, open="+open+":\n'"+(r.length>1000 ? "*LONG*" : new String(r))+"'");}
		
		bstart=x;
		return r;
	}
	
	/** Fills buffer.  Ensures that result will extend to the next caret or EOF.  Returns number of bytes filled. */
	private final int fillBuffer(){
		assert(open);
		if(!open){return 0;}
		if(verbose){System.err.println("fillBuffer() : bstart="+bstart+", bstop="+bstop);}
		if(bstart<bstop){ //Shift end bytes to beginning
			if(bstart>0){
//				assert(bstart>0) : bstart+", "+bstop+", "+new String(buffer);
				int extra=bstop-bstart;
				for(int i=0; i<extra; i++, bstart++){
					buffer[i]=buffer[bstart];
				}
				bstop=extra;
			}
		}else{
			bstop=0;
		}
		if(verbose){System.err.println("After shift : bstart="+bstart+", bstop="+bstop);}

//		assert(bstart>0 || buffer[0]=='>') : "bstart="+bstart+", bstop="+bstop+", buffer[0]='"+(char)buffer[0]+"'";
//		assert(bstart<=bstop) : "bstart="+bstart+", bstop="+bstop+", buffer[0]='"+(char)buffer[0]+"'";
		
		bstart=0;
		
		int len=bstop;
		int r=-1;
		int sum=0;
		while(len==bstop){//hit end of input without encountering a caret
			if(bstop==buffer.length){
				buffer=Arrays.copyOf(buffer, buffer.length*2);
				if(verbose){System.err.println("Resized to "+buffer.length);}
			}
			try {
				r=ins.read(buffer, bstop, buffer.length-bstop);
			} catch (IOException e) {e.printStackTrace();}
			//if(verbose){System.err.println("r="+r);}
			if(r>0){
				sum+=r;
				bstop=bstop+r;
				if(bstop>0 && len==0){len=1;}
				while(len<bstop && buffer[len]!=carrot){len++;}
			}else{
				len=bstop;
				break;
			}
		}
		
		//Skip ';'-delimited comments
		if(header==null && bstop>bstart && buffer[bstart]==';'){
			if(sum==0){return sum;}
			int lastsemi=bstart;
			assert(nextReadID==0);
			assert(bstart==0);
			while(bstop>bstart && buffer[bstart]==';'){
				while(bstop>bstart && (buffer[bstart]>slashr || buffer[bstart]<0x2 || buffer[bstart]==tab)){bstart++;}
				while(bstop>bstart && buffer[bstart]<=slashr){bstart++;}
			}
			if(bstart>=bstop){ //Overflowed buffer with comments; recur
				bstart=lastsemi;
				return fillBuffer();
			}
		}
		
		assert(r==-1 || buffer[len]=='>');
		if(verbose){System.err.println("After filling: bstart="+bstart+", bstop="+bstop+", len="+len+", r="+r+", sum="+sum);}
		return sum;
	}
	
	private final InputStream open(){
		if(open){
			throw new RuntimeException("Attempt to open already-opened fasta file "+name);
		}
		open=true;
		ins=ReadWrite.getInputStream(name, true, allowSubprocess);
		bstart=0;
		bstop=0;
//		lasteol=-1;
		return ins;
	}
	
	public boolean isOpen(){return open;}
	
	public static final boolean settingsOK(){
		if(MIN_READ_LEN>=Integer.MAX_VALUE-1){
			throw new RuntimeException("Minimum FASTA read length is too long: "+MIN_READ_LEN);
		}
		if(MIN_READ_LEN<1){
			throw new RuntimeException("Minimum FASTA read length is too short: "+MIN_READ_LEN);
		}
		if(SPLIT_READS){
			if(TARGET_READ_LEN<1){
				throw new RuntimeException("Target FASTA read length is too short: "+TARGET_READ_LEN);
			}
			if(MIN_READ_LEN>TARGET_READ_LEN){
				throw new RuntimeException("Minimum FASTA read length is longer than maximum read length: "+MIN_READ_LEN+">"+TARGET_READ_LEN);
			}
		}
		if(MIN_READ_LEN>=Integer.MAX_VALUE-1 || MIN_READ_LEN<1){return false;}
		if(SPLIT_READS && (TARGET_READ_LEN<1 || MIN_READ_LEN>TARGET_READ_LEN)){return false;}
		return true;
	}
	
	public final String name;
	
	private ArrayList<Read> currentList=null;
	private String header=null;

	private boolean open=false;
//	private boolean hitmax=false; //Indicates that the current 'read' has more pieces to come 
	private byte[] buffer=new byte[16384];
	private int bstart=0, bstop=0;
	public InputStream ins;
	
	private long consumed=0;
	private long nextReadID=0;
	private int nextReadIndex=0;
	private int currentSection=0;

	public final boolean allowSubprocess;
	public final boolean interleaved;
	public final boolean amino;
	public final int flag;
	private final int BUF_LEN=Shared.READ_BUFFER_LENGTH;
	private final long MAX_DATA;
	private final int maxLen, minLen;
	
	
	public static boolean verbose=false;
	private final static byte slashr='\r', slashn='\n', carrot='>', space=' ', tab='\t';
	
	public static boolean SPLIT_READS=false;
	public static int TARGET_READ_LEN=500;
	public static int MIN_READ_LEN=1;//40;
	public static boolean FAKE_QUALITY=false;
	public static boolean FORCE_SECTION_NAME=false;
	public static boolean WARN_IF_NO_SEQUENCE=true;
	public static boolean ABORT_IF_NO_SEQUENCE=false;
	
}
