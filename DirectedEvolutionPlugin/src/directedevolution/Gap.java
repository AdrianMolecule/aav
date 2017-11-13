package directedevolution;

import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;

import javax.annotation.Nonnull;
import java.util.Objects;

public class Gap //implements Comparable{
{
    private final int startIndex;
    private final DefaultNucleotideSequence sequence;
    private final int sequenceIndex;
    private int size;

    public Gap(int startIndex, int size, @Nonnull DefaultNucleotideSequence sequence, int sequenceIndex) {
        this.startIndex = startIndex;
        this.size = size;
        this.sequence = sequence;
        this.sequenceIndex = sequenceIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getSize() {
        return size;
    }

    public DefaultNucleotideSequence getSequence() {
        return sequence;
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object aThat) {
        if (this == aThat) return true;
        if (!(aThat instanceof Gap)) return false;
        Gap that = (Gap) aThat;
        return ((this.getStartIndex() == that.getStartIndex()) &&
                (this.getSize() == that.getSize()) &&
                (this.getSequenceIndex() == that.getSequenceIndex()) &&
                (this.getSequence() == that.getSequence()));
    }

    @Override
    public int hashCode() {
        //todo use
        //if(true) return Objects.hash(getStartIndex(), getSize(),getSequenceIndex(),getSequence());
        int result = HashCodeUtil.SEED;
        result = HashCodeUtil.hash(result, getStartIndex());
        result = HashCodeUtil.hash(result, getSize());
        result = HashCodeUtil.hash(result, getSequenceIndex());
        result = HashCodeUtil.hash(result, getSequence());
        return result;
    }
//    /**
//     * @param aThat is a non-null GapBasedOligoModel.
//     *
//     * @throws NullPointerException if aThat is null.
//     * @throws ClassCastException if aThat is not an GapBasedOligoModel object.
//     */
//    @Override
//    public int compareTo(Object aThat) {
//        final int BEFORE = -1;
//        final int EQUAL = 0;
//        final int AFTER = 1;
//
//        //this optimization is usually worthwhile, and can always be added
//        if ( this == aThat ) return EQUAL;
//
//        final Gap that = (Gap)aThat;
//
//        //primitive numbers follow this form
//        if (this.getStartIndex() < that.getStartIndex()) return BEFORE;
//        if (this.getStartIndex() > that.getStartIndex()) return AFTER;
//        if (this.getSequenceIndex() < that.getSequenceIndex()) return BEFORE;
//        if (this.getSequenceIndex() > that.getSequenceIndex()) return AFTER;
//
//        //all comparisons have yielded equality
//        //verify that compareTo is consistent with equals (optional)
//        // assert this.equals(that) : "compareTo inconsistent with equals.";
//        return EQUAL;
//    }
//}

    @Override
    public String toString() {
        return "Gap{" +
                "startIndex=" + startIndex +
                ", size=" + size +
                ", sequence=" + sequence.getSequenceString() +
                ", sequenceIndex=" + sequenceIndex +
                '}';
    }

    public int geEndIndexPlusOne() {
        return startIndex + getSize();
    }
}
