package directedevolution;

public class GapBasedOligoModel extends OligoModel implements Comparable{

    private final Gap gap;

    private boolean shouldBeComplemented;

     GapBasedOligoModel(CharSequence charSequence, int startIndex, int startOverlapSize,int endOverlapSize, Gap gap,  boolean shouldBeComplemented) {
        super(charSequence, startIndex, startOverlapSize, endOverlapSize, OligoModel.ProblemCode.NO_PROBLEM);
        this.shouldBeComplemented=shouldBeComplemented;
        this.gap=gap;
    }

     GapBasedOligoModel getGapBasedOligoModelWithoutGaps(){
        String slimmedCharSequence = getCharSequence().toString().replace("-", "");
         return new GapBasedOligoModel(slimmedCharSequence, getStartIndex(),getStartOverlapSize(),
                 getEndOverlapSize()-(getCharSequence().length()-slimmedCharSequence.length()),gap, shouldBeComplemented);//todo startoverlap size might be wrong
    }

    @Override
    public boolean equals(Object aThat) {
        if (this == aThat) return true;
        if (!(aThat instanceof OligoModel)) return false;
        OligoModel that = (OligoModel) aThat;
        return this.getCharSequence().equals(that.getCharSequence()) &&
                (this.getStartIndex() == that.getStartIndex()) &&
                (this.getStartOverlapSize() == that.getStartOverlapSize()) &&
                (this.getEndOverlapSize() == that.getEndOverlapSize()) &&
                (this.getProblemCode().equals(that.getProblemCode()));
    }

    @Override public int hashCode() {
        int result = HashCodeUtil.SEED;
        result = HashCodeUtil.hash( result, getStartIndex());
        result = HashCodeUtil.hash( result, gap.getSequenceIndex() );
        return result;
    }

    /**
     * @param aThat is a non-null GapBasedOligoModel.
     *
     * @throws NullPointerException if aThat is null.
     * @throws ClassCastException if aThat is not an GapBasedOligoModel object.
     */
    @Override
    public int compareTo(Object aThat) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        //this optimization is usually worthwhile, and can always be added
        if ( this == aThat ) return EQUAL;

        final GapBasedOligoModel that = (GapBasedOligoModel)aThat;

        //primitive numbers follow this form
        if (this.getStartIndex() < that.getStartIndex()) return BEFORE;
        if (this.getStartIndex() > that.getStartIndex()) return AFTER;
        if (this.gap.getSequenceIndex() < that.gap.getSequenceIndex()) return BEFORE;
        if (this.gap.getSequenceIndex() > that.gap.getSequenceIndex()) return AFTER;

        //all comparisons have yielded equality
        //verify that compareTo is consistent with equals (optional)
       // assert this.equals(that) : "compareTo inconsistent with equals.";
        return EQUAL;
    }


    public boolean isShouldBeComplemented() {
        return shouldBeComplemented;
    }

}
