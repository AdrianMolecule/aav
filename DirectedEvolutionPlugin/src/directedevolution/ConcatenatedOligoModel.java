package directedevolution;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConcatenatedOligoModel extends OligoModel {

    private List<OligoModel> oligos;
    boolean shouldBeComplemented;//only for the first oligo

    public ConcatenatedOligoModel(ImmutableList<OligoModel> oligos, boolean shouldBeComplemented) {
        //could make it more memory friendly if we don't keep all the oligos in
        super(oligos);
        this.shouldBeComplemented = shouldBeComplemented;
    }

    public boolean getShouldBeComplemented() {
        return shouldBeComplemented;
    }
}
