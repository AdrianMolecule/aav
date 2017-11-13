package directedevolution;

import com.biomatters.geneious.publicapi.documents.sequence.SequenceCharSequence;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.logging.Logger;

import static directedevolution.OligoUtils.complementDNANucleotides;

/**
 * --------------------------------
 * ===---------=========
 */
public class OligoModel {
    private CharSequence charSequence;
    private int startIndex;
    private int startOverlapSize;
    private int endOverlapSize;
    //todo maybe add a startIndexWithoutOverlap or overlapLength
    private ProblemCode problemCode = ProblemCode.NO_PROBLEM;

    public OligoModel(CharSequence charSequence, int startIndex, int startOverlapSize, int endOverlapSize, ProblemCode problemCode) {
        constructorHelper(startIndex, startOverlapSize, endOverlapSize, problemCode);
        this.charSequence = charSequence;
    }

    public OligoModel(ImmutableList<OligoModel> oligos) {
        constructorHelper(oligos.get(0).getStartIndex(), oligos.get(0).getStartOverlapSize(),
                oligos.get(oligos.size() - 1).getEndOverlapSize(), OligoModel.ProblemCode.NO_PROBLEM);
        charSequence = concatenateOligos(oligos);
    }

    private void constructorHelper(int startIndex, int startOverlapSize, int endOverlapSize, ProblemCode problemCode) {
        this.setStartIndex(startIndex);
        this.setStartOverlapSize(startOverlapSize);
        this.setEndOverlapSize(endOverlapSize);
        this.problemCode = problemCode;
    }

    public CharSequence getCharSequence() {
        return charSequence;
    }

    public OligoModel(CharSequence charSequence, int startIndex, int startOverlapSize, ProblemCode problem) {
        this(charSequence, startIndex, startOverlapSize, -1, problem);//-1 of endOverLap means incomplete model
    }

    SequenceCharSequence getPaddedCharSequence(int neededLength) {
        return OligoUtils.padEnds(getStartIndex(), charSequence, neededLength);
    }

    SequenceCharSequence getPaddedComplementedCharSequence(int neededLength) {
        return OligoUtils.padEnds(getStartIndex(), complementDNANucleotides(charSequence), neededLength);
    }

    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public String toString() {
        return "OligoModel{" +
                "charSequence=" + charSequence +
                ", startIndex=" + getStartIndex() +
                ", getEndIndexPlusOne=" + getEndIndexPlusOne() +
                ", length=" + charSequence.length() + ", startOverlapSize=" + getStartOverlapSize() + ", endOverlapSize=" + getEndOverlapSize() +
                ", problemCode=" + problemCode +
                '}';
    }

    int getSize() {
        return charSequence.length();
    }

    public OligoModel setEndOverlapSize(int endOverlap) {
        this.endOverlapSize = endOverlap;
        return this;
    }

    public int getStartOverlapSize() {
        return startOverlapSize;
    }

    public int getEndOverlapSize() {
        return endOverlapSize;
    }

    public int getEndIndexPlusOne() {
        return startIndex + getSize();
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setStartOverlapSize(int startOverlapSize) {
        this.startOverlapSize = startOverlapSize;
    }

    public ProblemCode getProblemCode() {
        return problemCode;
    }

    public enum ProblemCode {
        NO_PROBLEM,
        TOO_LONG;
    }

    private CharSequence concatenateOligos(List<OligoModel> oligos) {
        {
            if (oligos.size() == 1) {
                return oligos.get(0).getCharSequence();
            }
            if (oligos != null) {
                StringBuilder stringBuilder = new StringBuilder(oligos.get(0).charSequence);
                for (int i = 1; i < oligos.size(); i++) {
                    OligoModel oligoModelCurrent = oligos.get(i);
                    stringBuilder.append(oligoModelCurrent.charSequence.subSequence(oligoModelCurrent.getStartOverlapSize(), oligoModelCurrent.charSequence.length()));
                }
                return stringBuilder;
            } else {
                Logger.getLogger(getClass().getName()).warning("concatenateSequences call with oligos is Empty");
                return null;
            }
        }
    }
}
