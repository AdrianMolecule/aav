package directedevolution;


import com.biomatters.geneious.publicapi.documents.URN;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import jebl.evolution.sequences.Sequence;

import java.util.Date;

public class OptimizedNucleotideSequence extends DefaultNucleotideSequence {
    public OptimizedNucleotideSequence() {
        super();
    }

    public OptimizedNucleotideSequence(String s, String s1, CharSequence charSequence, Date date) {
        super(s, s1, charSequence, date);
    }

    public OptimizedNucleotideSequence(String s, String s1, CharSequence charSequence, Date date, URN urn) {
        super(s, s1, charSequence, date, urn);
    }

    public OptimizedNucleotideSequence(NucleotideSequenceDocument nucleotideSequenceDocument, Date date) {
        super(nucleotideSequenceDocument, date);
    }

    public OptimizedNucleotideSequence(Sequence sequence) {
        super(sequence);
    }

    public OptimizedNucleotideSequence(String s, CharSequence charSequence) {
        super(s, charSequence);
    }
}
