package assemble;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import kmer.AbstractKmerTable;
import kmer.AbstractKmerTableSet;
import kmer.HashArray1D;
import kmer.HashForest;
import kmer.KmerNode;
import kmer.KmerTableSet;

import ukmer.Kmer;
import ukmer.KmerTableSetU;

import stream.ByteBuilder;
import stream.FastaReadInputStream;
import stream.Read;
import structures.IntList;
import structures.LongList;
import align2.ReadLengthComparator;
import align2.ReadStats;
import align2.Shared;
import align2.Tools;
import dna.AminoAcid;
import dna.Parser;
import dna.Timer;
import fileIO.ByteFile;
import fileIO.ByteStreamWriter;
import fileIO.ReadWrite;
import fileIO.FileFormat;


/**
 * Assembles kmers into a concise representation.
 * @author Brian Bushnell
 * @date May 15, 2015
 *
 */
public class KmerCompressor {
	
	/**
	 * Code entrance from the command line.
	 * @param args Command line arguments
	 */
	public static void main(String[] args){
		
		args=Parser.parseConfig(args);
		if(Parser.parseHelp(args, true)){
			printOptions();
			System.exit(0);
		}
		
		Timer t=new Timer(), t2=new Timer();
		t.start();
		t2.start();

		final KmerCompressor wog=new KmerCompressor(args, true);
		t2.stop();
		outstream.println("Initialization Time:      \t"+t2);
		
		///And run it
		wog.process(t);
	}
	
	public static final int preparseK(String[] args){
		int k=31;
		for(int i=0; i<args.length; i++){
			final String arg=args[i];
			String[] split=arg.split("=");
			String a=split[0].toLowerCase();
			String b=split.length>1 ? split[1] : null;
			if("null".equalsIgnoreCase(b)){b=null;}
			while(a.charAt(0)=='-' && (a.indexOf('.')<0 || i>1 || !new File(a).exists())){a=a.substring(1);}
			
			if(a.equals("k")){
				k=Integer.parseInt(b);
			}
		}
		return Kmer.getMult(k)*Kmer.getK(k);
	}
	
	/**
	 * Display usage information.
	 */
	protected static final void printOptions(){
		outstream.println("Syntax:\nTODO");
	}
	
	/**
	 * Constructor.
	 * @param args Command line arguments
	 */
	public KmerCompressor(String[] args, boolean setDefaults){
		System.err.println("Executing "+getClass().getName()+" "+Arrays.toString(args)+"\n");
		k=preparseK(args);
		
		if(setDefaults){
			/* Set global defaults */
			ReadWrite.ZIPLEVEL=8;
			ReadWrite.USE_UNPIGZ=true;
			ReadWrite.USE_PIGZ=true;
			FastaReadInputStream.SPLIT_READS=false;
			if(!ByteFile.FORCE_MODE_BF1 && !ByteFile.FORCE_MODE_BF2 && Shared.threads()>2){
				ByteFile.FORCE_MODE_BF2=true;
			}
			AbstractKmerTableSet.defaultMinprob=0.5;
		}
		
		/* Initialize local variables with defaults */
		Parser parser=new Parser();
		ArrayList<String> in1=new ArrayList<String>();
		ArrayList<String> in2=new ArrayList<String>();
		int fuse_=0;
		
		{
			boolean b=false;
			assert(b=true);
			EA=b;
		}
		
		/* Parse arguments */
		for(int i=0; i<args.length; i++){

			final String arg=args[i];
			String[] split=arg.split("=");
			String a=split[0].toLowerCase();
			String b=split.length>1 ? split[1] : null;
			if("null".equalsIgnoreCase(b)){b=null;}
			while(a.charAt(0)=='-' && (a.indexOf('.')<0 || i>1 || !new File(a).exists())){a=a.substring(1);}
			
			if(Parser.isJavaFlag(arg)){
				//jvm argument; do nothing
			}else if(Parser.parseCommonStatic(arg, a, b)){
				//do nothing
			}else if(Parser.parseZip(arg, a, b)){
				//do nothing
			}else if(Parser.parseQuality(arg, a, b)){
				//do nothing
			}else if(Parser.parseFasta(arg, a, b)){
				//do nothing
			}else if(parser.parseInterleaved(arg, a, b)){
				//do nothing
			}else if(parser.parseTrim(arg, a, b)){
				//do nothing
			}else if(a.equals("in") || a.equals("in1")){
				in1.clear();
				if(b!=null){
					String[] s=b.split(",");
					for(String ss : s){
						in1.add(ss);
					}
				}
			}else if(a.equals("in2")){
				in2.clear();
				if(b!=null){
					String[] s=b.split(",");
					for(String ss : s){
						in2.add(ss);
					}
				}
			}else if(a.equals("out") || a.equals("contigs")){
				outContigs=b;
			}else if(a.equals("append") || a.equals("app")){
				append=ReadStats.append=Tools.parseBoolean(b);
			}else if(a.equals("overwrite") || a.equals("ow")){
				overwrite=Tools.parseBoolean(b);
			}else if(a.equals("fuse")){
				if(b==null || Character.isLetter(b.charAt(0))){
					fuse_=Tools.parseBoolean(b) ? 100000 : 0;
				}else{
					fuse_=Integer.parseInt(b);
				}
			}else if(a.equals("showstats") || a.equals("stats")){
				showStats=Tools.parseBoolean(b);
			}else if(a.equals("mincount") || a.equals("mincov") || a.equals("mindepth") || a.equals("min")){
				minCount=(int)Tools.parseKMG(b);
			}else if(a.equals("maxcount") || a.equals("maxcov") || a.equals("maxdepth") || a.equals("max")){
				maxCount=(int)Tools.parseKMG(b);
			}else if(a.equals("requiresamecount") || a.equals("rsc") || a.equals("rsd")){
				REQUIRE_SAME_COUNT=Tools.parseBoolean(b);
			}else if(a.equals("threads") || a.equals("t")){
				Shared.setThreads(b);
			}else if(a.equals("buildthreads") || a.equals("bthreads") || a.equals("bt")){
				if(b.equalsIgnoreCase("auto")){
					BUILD_THREADS=Shared.threads();
				}else{
					BUILD_THREADS=Integer.parseInt(b);
				}
			}else if(a.equals("showspeed") || a.equals("ss")){
				showSpeed=Tools.parseBoolean(b);
			}else if(a.equals("verbose")){
//				assert(false) : "Verbose flag is currently static final; must be recompiled to change.";
				verbose=Tools.parseBoolean(b);
			}else if(a.equals("verbose2")){
//				assert(false) : "Verbose flag is currently static final; must be recompiled to change.";
				verbose2=Tools.parseBoolean(b);
			}else if(a.equals("ilb") || a.equals("ignoreleftbranches") || a.equals("ignoreleftjunctions") || a.equals("ibb") || a.equals("ignorebackbranches")){
				extendThroughLeftJunctions=Tools.parseBoolean(b);
			}else if(a.equals("rcomp")){
				doRcomp=Tools.parseBoolean(b);
			}
			
			else if(KmerTableSetU.isValidArgument(a)){
				//Do nothing
			}else{
				throw new RuntimeException("Unknown parameter "+args[i]);
			}
		}
		
		fuse=fuse_;
		LOAD_THREADS=Shared.threads();
		
		{//Process parser fields
			Parser.processQuality();
		}
		
		/* Adjust I/O settings and filenames */
		
		assert(FastaReadInputStream.settingsOK());

		nextTable=new AtomicInteger[1];
		nextVictims=new AtomicInteger[1];
		for(int i=0; i<1; i++){
			nextTable[i]=new AtomicInteger(0);
			nextVictims[i]=new AtomicInteger(0);
		}

		if(!Tools.testOutputFiles(overwrite, append, false, outContigs)){
			throw new RuntimeException("\nCan't write to some output files; overwrite="+overwrite+"\n");
		}
		assert(LOAD_THREADS>0);
		outstream.println("Using "+LOAD_THREADS+" threads.");
		
		
		final int bytesPerKmer;
		{
			int mult=12+k; //worst case for no assembly;
			if(true){mult+=4;}
			bytesPerKmer=mult;
		}
		
		tables=new KmerTableSet(args, bytesPerKmer);
		k2=tables.k2;
	}

	
	/*--------------------------------------------------------------*/
	/*----------------         Outer Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	
	public final void process(Timer t){
		
		/* Count kmers */
		process2();
		
		/* Stop timer and calculate speed statistics */
		t.stop();
		
		
		if(showSpeed){

			//Format with k or m suffixes
			String rpstring=(readsIn<100000 ? ""+readsIn : readsIn<100000000 ? (readsIn/1000)+"k" : (readsIn/1000000)+"m");
			String bpstring=(basesIn<100000 ? ""+basesIn : basesIn<100000000 ? (basesIn/1000)+"k" : (basesIn/1000000)+"m");

			while(rpstring.length()<8){rpstring=" "+rpstring;}
			while(bpstring.length()<8){bpstring=" "+bpstring;}
			
			outstream.println("\nTotal Time:               \t"+t);
		}
		
		if(showStats && outContigs!=null && FileFormat.isFasta(ReadWrite.rawExtension(outContigs))){
			outstream.println();
			jgi.AssemblyStats2.main(new String[] {"in="+outContigs});
		}
		
		/* Throw an exception if errors were detected */
		if(errorState){
			throw new RuntimeException(getClass().getSimpleName()+" terminated in an error state; the output may be corrupt.");
		}
	}
	
	public long loadKmers(Timer t){
		tables.process(t);
		return tables.kmersLoaded;
	}
	
	public final void process2(){
		
		/* Start phase timer */
		Timer t=new Timer();
		
		/* Fill tables with kmers */
		outstream.println("\nLoading kmers.\n");
		loadKmers(t);
		
		t.stop();
//		outstream.println("Input:                      \t"+tables.readsIn+" reads \t\t"+tables.basesIn+" bases.");
//		outstream.println("Unique Kmers:               \t"+tables.kmersLoaded);
//		outstream.println("Load Time:                  \t"+t);
		
		
		t.start();
		
		{
			/* Build contigs */
			outstream.println("\nBuilding contigs.\n");
			buildContigs();
			
			if(DISPLAY_PROGRESS){
				outstream.println("\nAfter building contigs:");
				Shared.printMemory();
				outstream.println();
			}
			
			t.stop();
			
			if(readsIn>0){outstream.println("Input:                      \t"+readsIn+" reads \t\t"+basesIn+" bases.");}
			outstream.println("Bases generated:            \t"+basesBuilt);
			outstream.println("Contigs generated:          \t"+contigsBuilt);
			outstream.println("Longest contig:             \t"+longestContig);
			outstream.println("Contig-building time:       \t"+t);
		}
	}
	
	
	/*--------------------------------------------------------------*/
	/*----------------         Inner Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	/**
	 * Build contigs.
	 */
	private final void buildContigs(){
		
		allContigs=new ArrayList<Read>();

		tables.initializeOwnership();
		
		/* Create ProcessThreads */
		ArrayList<AbstractBuildThread> alpt=new ArrayList<AbstractBuildThread>(BUILD_THREADS);
		for(int i=0; i<BUILD_THREADS; i++){alpt.add(makeBuildThread(i));}
		for(AbstractBuildThread pt : alpt){pt.start();}
		
		/* Wait for threads to die, and gather statistics */
		for(AbstractBuildThread pt : alpt){
			while(pt.getState()!=Thread.State.TERMINATED){
				try {
					pt.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for(Read contig : pt.contigs){
				allContigs.add(contig);
				contigsBuilt++;
				basesBuilt+=contig.length();
				longestContig=Tools.max(longestContig, contig.length());
			}
			
			readsIn+=pt.readsInT;
			basesIn+=pt.basesInT;
			lowqReads+=pt.lowqReadsT;
			lowqBases+=pt.lowqBasesT;
		}
		
		if(outContigs!=null){
			FileFormat ff=FileFormat.testOutput(outContigs, FileFormat.FA, 0, 0, true, overwrite, append, false);
//			ConcurrentReadOutputStream ros=ConcurrentReadOutputStream.getStream(ff, null, null, null, 4, null, false);
//			ros.start();
			ByteStreamWriter bsw=new ByteStreamWriter(ff);
			bsw.start();
			if(allContigs!=null){
//				Collections.sort(allContigs, ReadComparatorID.comparator);
				Collections.sort(allContigs, ReadLengthComparator.comparator);
				fuse(allContigs, fuse);
				for(int i=0; i<allContigs.size(); i++){
					Read r=allContigs.get(i);
					bsw.println(r);
				}
			}
			errorState|=bsw.poisonAndWait();
		}
	}
	
	private static void fuse(ArrayList<Read> contigs, int fuse){
		if(fuse<2){return;}
		ArrayList<Read> temp=new ArrayList<Read>();
		ByteBuilder bb=new ByteBuilder();
		int num=0;
		for(int i=0; i<contigs.size(); i++){
			Read r=contigs.set(i, null);
			if(bb.length()>0){bb.append('N');}
			bb.append(r.bases);
			if(bb.length()>=fuse){
				Read fused=new Read(bb.toBytes(), -1, -1, -1, ""+num, null, num, 0);
				num++;
				temp.add(fused);
				bb.clear();
			}
		}
		if(bb.length()>0){
			Read fused=new Read(bb.toBytes(), -1, -1, -1, ""+num, null, num, 0);
			temp.add(fused);
			bb.clear();
		}
		contigs.clear();
		contigs.addAll(temp);
		temp=null;
	}
	
	/*--------------------------------------------------------------*/
	/*----------------         Inner Classes        ----------------*/
	/*--------------------------------------------------------------*/
	
	/*--------------------------------------------------------------*/
	/*----------------          BuildThread         ----------------*/
	/*--------------------------------------------------------------*/
	
	BuildThread makeBuildThread(int id){
		return new BuildThread(id);
	}
	
	/**
	 * Builds contigs. 
	 */
	private class BuildThread extends AbstractBuildThread{
		
		public BuildThread(int id_){
			super(id_, Tadpole.contigMode, null);
		}
		
		@Override
		public void run(){
			//Build from kmers
			
			//Final pass
			while(processNextTable(nextTable[0])){}
			while(processNextVictims(nextVictims[0])){}
		}
		
		private boolean processNextTable(AtomicInteger aint){
			final int tnum=aint.getAndAdd(1);
			if(tnum>=tables.ways){return false;}
			final HashArray1D table=tables.getTable(tnum);
			if(verbose && id==0){System.err.println("Processing table "+tnum+", size "+table.size());}
			final int max=table.arrayLength();
			for(int cell=0; cell<max; cell++){
				int x=processCell(table, cell);
			}
			return true;
		}
		
		private boolean processNextVictims(AtomicInteger aint){
			final int tnum=aint.getAndAdd(1);
			if(tnum>=tables.ways){return false;}
			final HashArray1D table=tables.getTable(tnum);
			final HashForest forest=table.victims();
			if(verbose && id==0){System.err.println("Processing forest "+tnum+", size "+forest.size());}
			final int max=forest.arrayLength();
			for(int cell=0; cell<max; cell++){
				KmerNode kn=forest.getNode(cell);
				int x=traverseKmerNode(kn);
			}
			return true;
		}
		
		private int processCell(HashArray1D table, int cell){
			int count=table.readCellValue(cell);
			if(count<minCount || count>maxCount){return 0;}
			
			long key=table.getKmer(cell);

			if(verbose){outstream.println("id="+id+" processing cell "+cell+"; \tkmer="+key+"\t"+toText(key));}
			int owner=table.getCellOwner(cell);
			if(verbose){outstream.println("Owner is initially "+owner);}
			if(owner>-1){return 0;}
			owner=table.setOwner(key, id, cell);
			if(verbose){outstream.println("Owner is now "+owner);}
			if(owner!=id){return 0;}
			return processKmer(key);
		}
		
		private int traverseKmerNode(KmerNode kn){
			int sum=0;
			if(kn!=null){
				sum+=processKmerNode(kn);
				if(kn.left()!=null){
					sum+=traverseKmerNode(kn.left());
				}
				if(kn.right()!=null){
					sum+=traverseKmerNode(kn.right());
				}
			}
			return sum;
		}
		
		private int processKmerNode(KmerNode kn){
			final long key=kn.pivot();
			final int count=kn.getValue(key);
			if(count<minCount || count>maxCount){return 0;}

			if(verbose){outstream.println("id="+id+" processing KmerNode; \tkmer="+key+"\t"+toText(key));}
			int owner=kn.getOwner(key);
			if(verbose){outstream.println("Owner is initially "+owner);}
			if(owner>-1){return 0;}
			owner=kn.setOwner(key, id);
			if(verbose){outstream.println("Owner is now "+owner);}
			if(owner!=id){return 0;}
			return processKmer(key);
		}
		
		/** Returns length of new contig */
		private int processKmer(long key){
			byte[] contig=makeContig(key, builderT, true);
			if(contig!=null){
				final long num=contigNum.incrementAndGet();
				final String id;
				if(REQUIRE_SAME_COUNT){
					id="n"+num+",c="+tables.getCount(key);
				}else{
					id=Long.toString(num);
				}
				
				Read r=new Read(contig, -1, -1, -1, id, null, num, 0);
				contigs.add(r);
				if(verbose){System.err.println("Added "+contig.length);}
				return contig.length;
			}else{
				if(verbose){System.err.println("Created null contig.");}
			}
			return 0;
		}
		
		/** From kmers */
		private byte[] makeContig(final long key, final ByteBuilder bb, boolean alreadyClaimed){
			builderT.setLength(0);
			builderT.appendKmer(key, k);
			if(verbose){outstream.println("Filled builder: "+builderT);}
			
			final int initialLength=bb.length();
			assert(initialLength==k);
			if(initialLength<k){return null;}
//			System.err.print("A");
			
			{
				boolean success=(alreadyClaimed || claim(key, id));
				if(verbose){System.err.println("Thread "+id+" checking owner after setting: "+findOwner(bb, id));}
				if(!success){
					assert(bb.length()==k);
					//				release(bb, id); //no need to release
					return null;
				}
			}
//			System.err.print("B");
			if(verbose  /*|| true*/){System.err.println("Thread "+id+" building contig; initial length "+bb.length());}
			if(verbose){System.err.println("Extending to right.");}
			{
				final int status=extendToRight(bb, rightCounts, id);
				
				if(status==DEAD_END){
					//do nothing
				}else if(status==TOO_LONG){
					//do nothing
				}else if(status==BAD_SEED){
					if(bb.length()<=k){
						release(key, id);
						return null;
					}
				}else{
					throw new RuntimeException("Bad return value: "+status);
				}
			}
//			System.err.print("C");
			
			bb.reverseComplementInPlace();
			if(verbose  /*|| true*/){System.err.println("Extending rcomp to right; current length "+bb.length());}
			{
				final int status;
				if(doRcomp){
					status=extendToRight(bb, rightCounts, id);
				}else{
					status=extendToRight_RcompOnly(bb, rightCounts, id);
				}
				
				if(status==DEAD_END){
					//do nothing
				}else if(status==TOO_LONG){
					//do nothing
				}else if(status==BAD_SEED){
					if(bb.length()<=k){
						release(key, id);
						return null;
					}
				}else{
					throw new RuntimeException("Bad return value: "+status);
				}
			}
//			System.err.print("D");

			if(verbose  /*|| true*/){System.err.println("A: Final length for thread "+id+": "+bb.length());}
			
			//TODO: Success only if this thread actually owns some kmer in the contig.  And trim unowned terminal kmers.
			
			if(bb.length()>=k){
				bb.reverseComplementInPlace();
				return bb.toBytes();
			}
			if(verbose  /*|| true*/){System.err.println("A: Contig was too short for "+id+": "+bb.length());}
//			assert(false) : bb.length()+", "+initialLength+", "+minExtension+", "+minContigLen;
//			System.err.print("F");
			return null;
		}
		
	}
	
	/*--------------------------------------------------------------*/
	/*----------------       Extension Methods      ----------------*/
	/*--------------------------------------------------------------*/
	
	
	/**
	 * Extend these bases into a contig.
	 * Stops at both left and right junctions.
	 * Claims ownership.
	 */
	public int extendToRight(final ByteBuilder bb, final int[] rightCounts, final int id){
		if(bb.length()<k){return BAD_SEED;}
		final int shift=2*k;
		final int shift2=shift-2;
		final long mask=~((-1L)<<shift);
		long kmer=0;
		long rkmer=0;
		int len=0;
		
		/* Loop through the bases, maintaining a forward and reverse kmer via bitshifts, to get the rightmost kmer */
		{
			final int bblen=bb.length();
			final byte[] bases=bb.array;
			for(int i=bblen-k; i<bblen; i++){
				final byte b=bases[i];
				final long x=AminoAcid.baseToNumber[b];
				final long x2=AminoAcid.baseToComplementNumber[b];
				kmer=((kmer<<2)|x)&mask;
				rkmer=(rkmer>>>2)|(x2<<shift2);
				if(x<0){
					len=0;
					kmer=rkmer=0;
				}else{len++;}
				if(verbose){outstream.println("A: Scanning i="+i+", len="+len+", kmer="+kmer+", rkmer="+rkmer+"\t"+new String(bases, Tools.max(0, i-k2), Tools.min(i+1, k)));}
			}
		}
		
		if(len<k){
			if(verbose){outstream.println("Returning BAD_SEED 1");}
			return BAD_SEED;
		}
		else{assert(len==k);}
		
		/* Now the trailing kmer has been initialized. */
		
		long key=toValue(kmer, rkmer);
		HashArray1D table=tables.getTableForKey(key);
		int count=table.getValue(key);
		if(count<minCount || count>maxCount){
			if(verbose){
				outstream.println("Returning because count was too low: "+count);
				outstream.println("Returning BAD_SEED 2");
			}
			return BAD_SEED;
		}
		
		int owner=table.getOwner(key);
		if(verbose){outstream.println("Owner: "+owner);}
		if(owner>-1 && owner!=id){
			if(verbose){outstream.println("Returning BAD_SEED 3");}
			return BAD_SEED;
		}
		
		owner=table.setOwner(key, id);
		if(verbose){outstream.println("A. Owner is now "+id+" for key "+key);}
		if(owner!=id){
			if(verbose){
				outstream.println("Returning early because owner was "+owner+" for thread "+id+".");
				outstream.println("Returning BAD_SEED 4");
			}
			return BAD_SEED;
		}
		
		final int maxLen=Tools.max(100000, bb.length()+90000);
		
		while(bb.length()<maxLen){
			
			fillRightCounts(kmer, rkmer, rightCounts, mask, shift2);
			int selected=-1;
			for(int i=0; i<4; i++){
				final int count2=rightCounts[i];
				if(count2>=minCount && count2<=maxCount && (!REQUIRE_SAME_COUNT || count2==count)){
					final long y=i;
					final long y2=AminoAcid.numberToComplement[i];
					final long kmer2=((kmer<<2)|(long)y)&mask;
					final long rkmer2=(rkmer>>>2)|(y2<<shift2);
					final long key2=toValue(kmer2, rkmer2);
					HashArray1D table2=tables.getTableForKey(key2);
					if(table2.getOwner(key2)<0){
						if(table2.setOwner(key2, id)==id){
							selected=i;
							kmer=kmer2;
							rkmer=rkmer2;
							key=key2;
							count=count2;
							final byte b=AminoAcid.numberToBase[selected];
							bb.append(b);
							break;
						}
					}
				}
			}
			
			if(verbose){
				outstream.println("kmer: "+toText(kmer)+", "+toText(rkmer));
				outstream.println("Counts: "+count+", "+Arrays.toString(rightCounts));
			}
			
			if(selected<0){
				if(verbose){outstream.println("Returning DEAD_END");}
				return DEAD_END;
			}//TODO: Explore on failure
		}
		if(verbose){
			outstream.println("Current contig length: "+bb.length()+"\nReturning TOO_LONG");
		}
//		assert(owner!=id) : owner+"!="+id+"; maxLen="+maxLen+"; len="+bb.length();
		return TOO_LONG;
	}
	
	
	/**
	 * Extend these bases into a contig.
	 * Stops at both left and right junctions.
	 * Claims ownership.
	 */
	public int extendToRight_RcompOnly(final ByteBuilder bb, final int[] rightCounts, final int id){
		if(bb.length()<k){return BAD_SEED;}
		final int shift=2*k;
		final int shift2=shift-2;
		final long mask=~((-1L)<<shift);
		long kmer=0;
		long rkmer=0;
		int len=0;
		
		/* Loop through the bases, maintaining a forward and reverse kmer via bitshifts, to get the rightmost kmer */
		{
			final int bblen=bb.length();
			final byte[] bases=bb.array;
			for(int i=bblen-k; i<bblen; i++){
				final byte b=bases[i];
				final long x=AminoAcid.baseToNumber[b];
				final long x2=AminoAcid.baseToComplementNumber[b];
				kmer=((kmer<<2)|x)&mask;
				rkmer=(rkmer>>>2)|(x2<<shift2);
				if(x<0){
					len=0;
					kmer=rkmer=0;
				}else{len++;}
				if(verbose){outstream.println("A: Scanning i="+i+", len="+len+", kmer="+kmer+", rkmer="+rkmer+"\t"+new String(bases, Tools.max(0, i-k2), Tools.min(i+1, k)));}
			}
		}
		
		if(len<k){
			if(verbose){outstream.println("Returning BAD_SEED 1");}
			return BAD_SEED;
		}
		else{assert(len==k);}
		
		/* Now the trailing kmer has been initialized. */
		
		long key=rkmer;
		HashArray1D table=tables.getTableForKey(key);
		int count=table.getValue(key);
		if(count<minCount || count>maxCount){
			if(verbose){
				outstream.println("Returning because count was too low: "+count);
				outstream.println("Returning BAD_SEED 2");
			}
			return BAD_SEED;
		}
		
		int owner=table.getOwner(key);
		if(verbose){outstream.println("Owner: "+owner);}
		if(owner>-1 && owner!=id){
			if(verbose){outstream.println("Returning BAD_SEED 3");}
			return BAD_SEED;
		}
		
		owner=table.setOwner(key, id);
		if(verbose){outstream.println("A. Owner is now "+id+" for key "+key);}
		if(owner!=id){
			if(verbose){
				outstream.println("Returning early because owner was "+owner+" for thread "+id+".");
				outstream.println("Returning BAD_SEED 4");
			}
			return BAD_SEED;
		}
		
		final int maxLen=Tools.max(100000, bb.length()+90000);
		
		while(bb.length()<maxLen){
			
			fillRightCountsRcompOnly(kmer, rkmer, rightCounts, mask, shift2);
			int selected=-1;
			for(int i=0; i<4; i++){
				final int count2=rightCounts[i];
				if(count2>=minCount && count2<=maxCount && (!REQUIRE_SAME_COUNT || count2==count)){
					final long y=i;
					final long y2=AminoAcid.numberToComplement[i];
					final long kmer2=((kmer<<2)|(long)y)&mask;
					final long rkmer2=(rkmer>>>2)|(y2<<shift2);
					final long key2=rkmer2;
					HashArray1D table2=tables.getTableForKey(key2);
					if(table2.getOwner(key2)<0){
						if(table2.setOwner(key2, id)==id){
							selected=i;
							kmer=kmer2;
							rkmer=rkmer2;
							key=key2;
							count=count2;
							final byte b=AminoAcid.numberToBase[selected];
							bb.append(b);
							break;
						}
					}
				}
			}
			
			if(verbose){
				outstream.println("kmer: "+toText(kmer)+", "+toText(rkmer));
				outstream.println("Counts: "+count+", "+Arrays.toString(rightCounts));
			}
			
			if(selected<0){
				if(verbose){outstream.println("Returning DEAD_END");}
				return DEAD_END;
			}//TODO: Explore on failure
		}
		if(verbose){
			outstream.println("Current contig length: "+bb.length()+"\nReturning TOO_LONG");
		}
//		assert(owner!=id) : owner+"!="+id+"; maxLen="+maxLen+"; len="+bb.length();
		return TOO_LONG;
	}
	
	
	/*--------------------------------------------------------------*/
	/*----------------        Helper Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	/** Currently unused */
	protected final Kmer getKmer(byte[] bases, int loc, Kmer kmer){
		kmer.clear();
		for(int i=loc, lim=loc+kmer.k; i<lim; i++){
			byte b=bases[i];
			int x=AminoAcid.baseToNumber[b];
			if(x<0){return null;}
			kmer.addRightNumeric(x);
		}
		assert(kmer.len==kmer.k);
		return kmer;
	}
	
	/*--------------------------------------------------------------*/
	/*----------------        Recall Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	private final long rcomp(long kmer){return AminoAcid.reverseComplementBinaryFast(kmer, k);}
	private final long toValue(long kmer, long rkmer){return tables.toValue(kmer, rkmer);}
	public final int getCount(long kmer, long rkmer){return tables.getCount(kmer, rkmer);}
	private final boolean claim(long kmer, int id){return claim(kmer, rcomp(kmer), id);}
	private final boolean claim(long kmer, long rkmer, int id){return tables.claim(kmer, rkmer, id);}
	private final int findOwner(ByteBuilder bb, int id){return tables.findOwner(bb, id);}
	private final void release(long key, int id){tables.release(key, id);}
	private final int fillRightCounts(long kmer, long rkmer, int[] counts, long mask, int shift2){return tables.fillRightCounts(kmer, rkmer, counts, mask, shift2);}
	private final int fillRightCountsRcompOnly(long kmer, long rkmer, int[] counts, long mask, int shift2){return tables.fillRightCountsRcompOnly(kmer, rkmer, counts, mask, shift2);}
	private final StringBuilder toText(long kmer){return AbstractKmerTable.toText(kmer, k);}
	
	/*--------------------------------------------------------------*/
	/*----------------            Fields            ----------------*/
	/*--------------------------------------------------------------*/

	public final KmerTableSet tables;
	
	/** Normal kmer length */
	private final int k;
	/** k-1; used in some expressions */
	private final int k2;

	private ArrayList<Read> allContigs;
	private long contigsBuilt=0;
	private long basesBuilt=0;
	private long longestContig=0;
	
	protected boolean extendThroughLeftJunctions=true;

	private int minCount=1;
	private int maxCount=Integer.MAX_VALUE;
	
	/** Only extend to kmers with the same count as this kmer */
	private boolean REQUIRE_SAME_COUNT=false;
	
	public boolean showStats=true;
	
	/** Has this class encountered errors while processing? */
	public boolean errorState=false;
	
	/** Contig output file */
	private String outContigs=null;
	
	long readsIn=0;
	long basesIn=0;
	long readsOut=0;
	long basesOut=0;
	long lowqReads=0;
	long lowqBases=0;
	
	/*--------------------------------------------------------------*/
	/*----------------       ThreadLocal Temps      ----------------*/
	/*--------------------------------------------------------------*/
	
	protected final void initializeThreadLocals(){
		if(localRightCounts.get()!=null){return;}
		localRightCounts.set(new int[4]);
		localLongList.set(new LongList());
		localIntList.set(new IntList());
		localByteBuilder.set(new ByteBuilder());
		localBitSet.set(new BitSet(300));
		localKmer.set(new Kmer(k));
	}
	
	protected ThreadLocal<int[]> localRightCounts=new ThreadLocal<int[]>();
	protected ThreadLocal<LongList> localLongList=new ThreadLocal<LongList>();
	protected ThreadLocal<IntList> localIntList=new ThreadLocal<IntList>();
	protected ThreadLocal<ByteBuilder> localByteBuilder=new ThreadLocal<ByteBuilder>();
	protected ThreadLocal<BitSet> localBitSet=new ThreadLocal<BitSet>();
	protected ThreadLocal<Kmer> localKmer=new ThreadLocal<Kmer>();
	
	/*--------------------------------------------------------------*/
	/*----------------       Final Primitives       ----------------*/
	/*--------------------------------------------------------------*/
	
	/** True iff java was launched with the -ea' flag */
	private final boolean EA;
	
	/** For numbering contigs */
	final AtomicLong contigNum=new AtomicLong(0);
	
	/** For controlling access to tables for contig-building */
	final AtomicInteger nextTable[];
	
	/** For controlling access to victim buffers for contig-building */
	final AtomicInteger nextVictims[];
	
	final int fuse;
	
	/*--------------------------------------------------------------*/
	/*----------------         Static Fields        ----------------*/
	/*--------------------------------------------------------------*/
	
	/** Print messages to this stream */
	protected static PrintStream outstream=System.err;
	/** Permission to overwrite existing files */
	public static boolean overwrite=false;
	/** Permission to append to existing files */
	public static boolean append=false;
	/** Print speed statistics upon completion */
	public static boolean showSpeed=true;
	/** Display progress messages such as memory usage */
	public static boolean DISPLAY_PROGRESS=true;
	/** Verbose messages */
	public static boolean verbose=false;
	/** Debugging verbose messages */
	public static boolean verbose2=false;
	/** Reverse-complement */
	public static boolean doRcomp=true;
	/** Number of load threads */
	public static int LOAD_THREADS=Shared.threads();
	/** Number of build threads */
	public static int BUILD_THREADS=1;
	
	/** Explore codes */
	public static final int KEEP_GOING=0, DEAD_END=1, TOO_SHORT=2, TOO_LONG=3, TOO_DEEP=4;
	
	/** Extend codes */
	public static final int BAD_SEED=12;
	
	public static final int STATUS_UNEXPLORED=0, STATUS_EXPLORED=1, STATUS_REMOVE=2, STATUS_KEEP=3;
	
}
