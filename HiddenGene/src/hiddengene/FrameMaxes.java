package hiddengene;

import com.google.common.collect.Lists;
import jebl.evolution.sequences.Codons;

import java.util.List;

public class FrameMaxes {
    private static FrameMaxes instance;

    public static FrameMaxes getInstance(boolean refresh, List<CharSequence> sequences) {
        if (instance != null && !refresh) {
            return instance;
        } else {
            instance = new FrameMaxes(sequences);
            return instance;
        }
    }

    private List<List<Integer>> allFrameData = null;

    private FrameMaxes(List<CharSequence> sequences) {
        List<Integer> frame0 = Lists.newArrayList();//DDD
        List<Integer> frame1 = Lists.newArrayList();// DDD
        List<Integer> frame2 = Lists.newArrayList();//  DDD
        List<Integer> frameR0 = Lists.newArrayList();//RRR   <-
        List<Integer> frameR1 = Lists.newArrayList();// RRR  <-
        List<Integer> frameR2 = Lists.newArrayList();//  RRR <-
        //first score all codons
        for (int nucleotideIndex = 0; nucleotideIndex < sequences.get(0).length(); nucleotideIndex += 3) {
            MaxBag frame0CodonMax = new MaxBag();
            MaxBag frame1CodonMax = new MaxBag();
            MaxBag frame2CodonMax = new MaxBag();
            MaxBag frameR0CodonMax = new MaxBag();
            MaxBag frameR1CodonMax = new MaxBag();
            MaxBag frameR2CodonMax = new MaxBag();
            for (CharSequence sequence : sequences) {
                if (nucleotideIndex + 3  < sequences.get(0).length()) {
                    frame0CodonMax.add(Codons.getState(direct(nucleotideIndex, sequence)));
                    frameR0CodonMax.add(Codons.getState(reverse(nucleotideIndex, sequence)));
                }else{
                    frame0CodonMax.add(Codons.GAP_STATE);
                    frameR0CodonMax.add(Codons.GAP_STATE);
                }
                if (nucleotideIndex + 3 + 1 < sequences.get(0).length()) {//todo this
                    frame1CodonMax.add(Codons.getState(direct(nucleotideIndex + 1, sequence)));
                    frameR2CodonMax.add(Codons.getState(reverse(nucleotideIndex + 1, sequence)));
                } else {
                    frame1CodonMax.add(Codons.GAP_STATE);
                    frameR2CodonMax.add(Codons.GAP_STATE);
                }
                if (nucleotideIndex + 3 + 2 < sequences.get(0).length()) {
                    frame2CodonMax.add(Codons.getState(direct(nucleotideIndex + 2, sequence)));
                    frameR1CodonMax.add(Codons.getState(reverse(nucleotideIndex + 2, sequence)));
                } else {
                    frame2CodonMax.add(Codons.GAP_STATE);
                    frameR1CodonMax.add(Codons.GAP_STATE);
                }
            }
            frame0.add(frame0CodonMax.getMax());
            frame1.add(frame1CodonMax.getMax());
            frame2.add(frame2CodonMax.getMax());
            frameR0.add(frameR0CodonMax.getMax());
            frameR1.add(frameR1CodonMax.getMax());
            frameR2.add(frameR2CodonMax.getMax());
        }
        allFrameData = Lists.newArrayList();
        allFrameData.add(frame0);
        allFrameData.add(frame1);
        allFrameData.add(frame2);
        allFrameData.add(frameR0);
        allFrameData.add(frameR1);
        allFrameData.add(frameR2);
    }

    public int getMax(int nucleotideIndex, FrameLogoSequenceGraph.Frame frame) {
        int max = 0;
        int codonIndex = new Double(nucleotideIndex / 3.0).intValue();
        int reminder = nucleotideIndex % 3;
        switch (frame) {
            case ZERO:
            case REVERSE0:
                max = allFrameData.get(frame.ordinal()).get(codonIndex);
                break;
            case ONE:
            case REVERSE2:
                if (reminder == 1 || reminder == 2) {
                    max = allFrameData.get(frame.ordinal()).get(codonIndex);
                } else if (codonIndex > 0) {
                    max = allFrameData.get(frame.ordinal()).get(codonIndex - 1);
                }//else is 0
                break;
            case TWO:
            case REVERSE1:
                if (reminder == 2) {
                    max = allFrameData.get(frame.ordinal()).get(codonIndex);
                } else if (codonIndex > 0) {
                    max = allFrameData.get(frame.ordinal()).get(codonIndex - 1);
                }
                //else is 0
                break;
            default:
                max = 0;
        }
        return max;
    }

    public String getFrameTotal(FrameLogoSequenceGraph.Frame frame) {
        int total = allFrameData.get(frame.ordinal()).stream().mapToInt(Number::intValue).sum();
        int zero = allFrameData.get(FrameLogoSequenceGraph.Frame.ZERO.ordinal()).stream().mapToInt(Number::intValue).sum();
        return "" + total + (total >= zero ? ">=" : "<") + zero;
    }

    private String reverse(int nucleotideIndex, CharSequence sequence) {
        return new StringBuffer(sequence.subSequence(nucleotideIndex, nucleotideIndex + 3).toString()).reverse().toString();
    }

    private String direct(int nucleotideIndex, CharSequence sequence) {
        return sequence.subSequence(nucleotideIndex, nucleotideIndex + 3).toString();
    }
}
