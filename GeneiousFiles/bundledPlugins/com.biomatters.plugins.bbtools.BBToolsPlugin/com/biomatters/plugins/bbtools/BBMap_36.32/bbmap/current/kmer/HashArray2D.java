package kmer;

import java.util.ArrayList;
import java.util.Arrays;

import align2.Tools;

/**
 * Stores kmers in a long[] and values in an int[][], with a victim cache.
 * @author Brian Bushnell
 * @date Nov 7, 2014
 *
 */
public final class HashArray2D extends HashArray {
	
	/*--------------------------------------------------------------*/
	/*----------------        Initialization        ----------------*/
	/*--------------------------------------------------------------*/
	
	public HashArray2D(int initialSize, boolean autoResize_){
		super(initialSize, autoResize_, true);
		values=allocInt2D(prime+extra);
	}
	
	/*--------------------------------------------------------------*/
	/*----------------        Public Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	@Deprecated
	@Override
	public int increment(final long kmer){
		throw new RuntimeException("Unsupported.");
	}
	
	@Deprecated
	@Override
	public int incrementAndReturnNumCreated(final long kmer){
		throw new RuntimeException("Unsupported.");
	}
	
	/*--------------------------------------------------------------*/
	/*----------------      Nonpublic Methods       ----------------*/
	/*--------------------------------------------------------------*/
	
	@Override
	protected final int readCellValue(int cell) {
		int[] set=values[cell];
		return set==null ? 0 : set[0];
	}
	
	@Override
	protected final int[] readCellValues(int cell, int[] singleton) {
		return values[cell];
	}
	
	/** Returns number of values added */
	protected final void insertValue(final long kmer, final int v, final int cell){
		assert(array[cell]==kmer);
		if(values[cell]==null){
			values[cell]=new int[] {v, NOT_PRESENT};
			return;
		}
		int[] set=values[cell];
		assert(set!=null);
		
		for(int i=0; i<set.length; i++){
			if(set[i]==v){return;}
			else if(set[i]<0){set[i]=v;return;}
		}
		final int oldSize=set.length;
		final int newSize=(int)Tools.min(Integer.MAX_VALUE, oldSize*2L);
		assert(newSize>set.length) : "Overflow.";
		set=Arrays.copyOf(set, newSize);
		set[oldSize]=v;
		Arrays.fill(set, oldSize+1, newSize, NOT_PRESENT);
		values[cell]=set;
	}
	
	/** Returns number of values added */
	protected final void insertValue(final long kmer, final int[] vals, final int cell){
		assert(array[cell]==kmer);
		if(values[cell]==null){
			values[cell]=vals;
		}else{
			for(int v : vals){
				if(v<0){break;}
				insertValue(kmer, v, cell);
			}
		}
	}
	
	/*--------------------------------------------------------------*/
	/*----------------   Resizing and Rebalancing   ----------------*/
	/*--------------------------------------------------------------*/
	
	@Override
	public final boolean canRebalance() {return false;}
	
	@Override
	protected synchronized void resize(){
//		assert(false);
//		System.err.println("Resizing from "+prime+"; load="+(size*1f/prime));
		if(prime>=maxPrime){
			sizeLimit=0xFFFFFFFFFFFFL;
			return;
		}
		
		final long oldSize=size, oldVSize=victims.size;
		final long totalSize=oldSize+oldVSize;
		
		final long maxAllowedByLoadFactor=(long)(totalSize*minLoadMult);
		final long minAllowedByLoadFactor=(long)(totalSize*maxLoadMult);

//		sizeLimit=Tools.min((long)(maxLoadFactor*prime), maxPrime);
		
		assert(maxAllowedByLoadFactor>=minAllowedByLoadFactor);
		if(maxAllowedByLoadFactor<prime){
			sizeLimit=(long)(maxLoadFactor*prime);
			return;
		}
		
		long x=10+(long)(prime*resizeMult);
		x=Tools.max(x, minAllowedByLoadFactor);
		x=Tools.min(x, maxAllowedByLoadFactor);
		
		int prime2=(int)Tools.min(maxPrime, Primes.primeAtLeast(x));
		
		if(prime2<=prime){
			sizeLimit=(long)(maxLoadFactor*prime);
			assert(prime2==prime) : "Resizing to smaller array? "+totalSize+", "+prime+", "+x;
			return;
		}
//		System.err.println("Resizing from "+prime+" to "+prime2+"; size="+size);
		
		prime=prime2;
//		System.err.println("Resized to "+prime+"; load="+(size*1f/prime));
		long[] oldk=array;
		int[][] oldc=values;
		KmerNode[] oldv=victims.array;
		array=allocLong1D(prime2+extra);
		Arrays.fill(array, NOT_PRESENT);
		values=allocInt2D(prime2+extra);
		ArrayList<KmerNode> list=new ArrayList<KmerNode>((int)(victims.size)); //Can fail if more than Integer.MAX_VALUE
		for(int i=0; i<oldv.length; i++){
			if(oldv[i]!=null){oldv[i].traverseInfix(list);}
		}
		Arrays.fill(oldv, null);
		victims.size=0;
		size=0;
		sizeLimit=Long.MAX_VALUE;
		
		final int[] singleton=new int[] {NOT_PRESENT};
		
		for(int i=0; i<oldk.length; i++){
			if(oldk[i]>NOT_PRESENT){
//				assert(!contains(oldk[i]));
				set(oldk[i], oldc[i]);
//				assert(contains(oldk[i]));
//				assert(Tools.equals(getValues(oldk[i], singleton), oldc[i]));
			}
		}
		
		for(KmerNode n : list){
			if(n.pivot>NOT_PRESENT){
//				assert(!contains(n.pivot));
				set(n.pivot, n.values(singleton));
//				assert(contains(n.pivot));
//				assert(Tools.equals(getValues(n.pivot, singleton), n.values(singleton)));
			}
		}
		
		assert(oldSize+oldVSize==size+victims.size) : oldSize+", "+oldVSize+" -> "+size+", "+victims.size;
		
		sizeLimit=(long)(maxLoadFactor*prime);
	}
	
	@Deprecated
	@Override
	public void rebalance(){
		throw new RuntimeException("Unimplemented.");
	}
	
	@Deprecated
	@Override
	public long regenerate(final int limit){
		assert(false) : "This is not tested or intended for use.";
		long sum=0;
		assert(owners==null) : "Clear ownership before regeneration.";
		for(int pos=0; pos<values.length; pos++){
			final long key=array[pos];
			if(key>=0){
				final int[] value=values[pos];
				values[pos]=null;
				array[pos]=NOT_PRESENT;
				size--;
				if(value!=null){
					assert(value[0]>0);
					set(key, value);
				}else{
					sum++;
				}
			}
		}
		
		ArrayList<KmerNode> nodes=victims.toList();
		victims.clear();
		for(KmerNode node : nodes){
			set(node.pivot, node.values(null));//TODO: Probably unsafe or unwise.  Should test for singletons, etc.
		}
		
		return sum;
	}
	
	/*--------------------------------------------------------------*/
	/*----------------            Fields            ----------------*/
	/*--------------------------------------------------------------*/
	
	private int[][] values;
	

	
}
