package pacbio;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import stream.ConcurrentGenericReadInputStream;
import stream.ConcurrentLegacyReadInputStream;
import stream.ConcurrentReadInputStream;
import stream.FASTQ;
import stream.FastaReadInputStream;
import stream.Read;
import stream.SequentialReadInputStream;
import structures.ListNum;
import dna.Data;
import dna.Parser;

import fileIO.FileFormat;
import fileIO.ReadWrite;
import fileIO.TextStreamWriter;
import align2.ReadStats;
import align2.Tools;

/**
 * @author Brian Bushnell
 * @date Dec 7, 2012
 *
 */
public class MergeReadsAndGenome {
	
	
	public static void main(String[] args){
		int genome=-1;
		String in[]=null;
		String out=null;
		long reads=-1;
		int readlen=300;
		boolean overwrite=false;
		boolean append=false;
		int sequentialOverlap=5;
		boolean sequentialStrandAlt=true;
		ReadWrite.ZIPLEVEL=2;
		
		FastaReadInputStream.TARGET_READ_LEN=250;
		FastaReadInputStream.SPLIT_READS=(FastaReadInputStream.TARGET_READ_LEN>0);
		
		
		for(int i=0; i<args.length; i++){
			final String arg=args[i];
			final String[] split=arg.split("=");
			String a=split[0].toLowerCase();
			String b=(split.length>1 ? split[1] : "true");

			if(Parser.isJavaFlag(arg)){
				//jvm argument; do nothing
			}else if(Parser.parseCommonStatic(arg, a, b)){
				//do nothing
			}else if(Parser.parseZip(arg, a, b)){
				//do nothing
			}else if(Parser.parseFasta(arg, a, b)){
				//do nothing
			}else if(a.equals("in")){
				if("null".equalsIgnoreCase(b)){
					//do nothing
				}else{
					in=b.split(",");
				}
			}else if(a.equals("out")){
				out=b;
			}else if(a.equals("build") || a.equals("genome")){
				genome=Integer.parseInt(b);
			}else if(a.equals("append") || a.equals("app")){
				append=ReadStats.append=Tools.parseBoolean(b);
			}else if(a.equals("overwrite") || a.equals("ow")){
				overwrite=Tools.parseBoolean(b);
				System.out.println("Set overwrite to "+overwrite);
			}else if(a.equals("reads")){
				reads=Tools.parseKMG(b);
			}else if(a.equals("readlen") || a.equals("length") || a.equals("len")){
				readlen=Integer.parseInt(b);
			}else if(a.equals("sequentialoverlap")){
				sequentialOverlap=Integer.parseInt(b);
			}else if(a.equals("sequentialstrandalt")){
				sequentialStrandAlt=Tools.parseBoolean(b);
			}else if(a.equals("verbose")){
				verbose=Tools.parseBoolean(b);
			}else{
				System.err.println("Unknown parameter "+split[i]);
				assert(false);
			}
		}
		
		assert(FastaReadInputStream.settingsOK());
		if(in!=null){
			File a=new File(out);
			for(String s : in){
				File b=new File(s);
				if(a.equals(b)){throw new RuntimeException("Input file may not equal output file: "+a.toString());}
			}
		}
		assert(out!=null);
		
		TextStreamWriter tsw=new TextStreamWriter(out, overwrite, false, false);
		tsw.start();
		
		long id=0;
		
		if(genome>=0){
			Data.setGenome(genome);
			SequentialReadInputStream.UNLOAD=true;
//			SequentialReadInputStream.verbose=true;
			SequentialReadInputStream ris=new SequentialReadInputStream(reads, readlen, Tools.max(50, readlen/2), sequentialOverlap, sequentialStrandAlt);
			ConcurrentLegacyReadInputStream cris=new ConcurrentLegacyReadInputStream(ris, reads);
			cris.start();
			id=appendReads(cris, tsw, id);
			ReadWrite.closeStream(cris);
		}
		
		if(in!=null){
			for(String s : in){
				final ConcurrentReadInputStream cris;
				{
					FileFormat ff1=FileFormat.testInput(s, FileFormat.FASTQ, null, true, false);
					cris=ConcurrentReadInputStream.getReadInputStream(-1, true, ff1, null);
					if(verbose){System.err.println("Started cris");}
					cris.start(); //4567
				}
				id=appendReads(cris, tsw, id);
				ReadWrite.closeStream(cris);
			}
		}
		
		tsw.poison();
		tsw.waitForFinish();
	}
	
	public static long appendReads(ConcurrentReadInputStream cris, TextStreamWriter tsw, long id){
		ListNum<Read> ln=cris.nextList();
		ArrayList<Read> reads=(ln!=null ? ln.list : null);
		while(reads!=null && reads.size()>0){

			for(Read r : reads){
				Read b=r.mate;
				Read a=correctRead(r, id);
				if(a!=null){
					tsw.println(a);
					id++;
				}
				b=correctRead(b, id);
				if(b!=null){
					tsw.println(b);
					id++;
				}
			}
			
			cris.returnList(ln.id, ln.list.isEmpty());
			//System.err.println("fetching list");
			ln=cris.nextList();
			reads=(ln!=null ? ln.list : null);
		}
		if(verbose){System.err.println("Finished reading");}
		cris.returnList(ln.id, ln.list.isEmpty());
		if(verbose){System.err.println("Returned list");}
		return id;
	}
	
	public static Read correctRead(Read r, long id){
		if(r==null){return null;}
		r.numericID=id;
		r.id=""+id;
		if(r.chrom<1){return r;}
		
		int startN=0;
		int stopN=r.length()-1;
		while(startN<r.length() && r.bases[startN]=='N'){startN++;}
		while(stopN>0 && r.bases[stopN]=='N'){stopN--;}
		if(startN>0 || stopN<r.length()-1){
			if(r.length()-startN-stopN<50){return null;}
			r.bases=Arrays.copyOfRange(r.bases, startN, stopN+1);
			if(r.quality!=null){r.quality=Arrays.copyOfRange(r.quality, startN, stopN+1);}
		}
		return r;
	}
	
	public static boolean verbose=false;
	
}
