package crossover;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideGraph;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.GeneiousGraphics2D;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.plugin.SequenceGraph;
import jebl.util.ProgressListener;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static crossover.FrameGraphOptions.COLORS;

/**
 * crossover
 */
@SuppressWarnings("NumericCastThatLosesPrecision")
class CrossoverSequenceGraph extends SequenceGraph {
    private CrossoverModel crossoverModel;
    FrameGraphOptions frameGraphOptions;
    private static final Color GRADIENT_BASE_COLOR = Color.RED;
    final static double THETA = 90.0 * (java.lang.Math.PI / 180.0);
    private final Color BACKGROUND_COLOR = Color.WHITE;
    List<SequenceDocument> realSequences;

    static {
        AffineTransform FONT_AT = new AffineTransform();
        FONT_AT.rotate(THETA);
    }

    AnnotatedPluginDocument annotatedPluginDocument;
    final static double VERTICAL_COVERAGE_OF_BAND = 0.8;
    boolean validGraph = true;
    boolean chimeraIsFirst;

    public CrossoverSequenceGraph(SequenceDocument.Alphabet alphabet) {
        System.out.println("Old constructor called");
    }

    public CrossoverSequenceGraph(AnnotatedPluginDocument annotatedPluginDocument, boolean chimeraIsFirst) {
        System.out.println("in CrossoverSequenceGraph good constructor and isChimeraFirst:" + chimeraIsFirst);
        try {
            this.chimeraIsFirst = chimeraIsFirst;
            realSequences = ((DefaultAlignmentDocument) (annotatedPluginDocument.getDocument())).getSequences();
        } catch (DocumentOperationException e) {
            e.printStackTrace();
        }
        getOptions();//make sure options are initialized
        performBackgroundCalculations(null);
        System.out.println("setResidues called");
    }

    @Override
    public void setResidues(List<CharSequence> sequences, List<NucleotideGraph> nucleotideGraphs, boolean ignoreEndGaps) {
        System.out.println("setResidues called");
        //todo assert sequences.size>2
        if (sequences.size() < 3) {
            Dialogs.showMessageDialog("You need to have one resulting sequence and at least two potential sources.");
            validGraph = false;
            return;
        }
        performBackgroundCalculations(null);
    }

    @Override
    public void draw(GeneiousGraphics2D graphics, int startResidue, int endResidue, int startX, int startY, int endX, int endY, double averageResidueWidth, int previousSectionWidth, int nextSectionWidth, int previousSectionResidueCount, int nextSectionResidueCount) {
        if (!validGraph) {
            return;
        }
        List<CharSequence> parentalSequences = crossoverModel.getParents();
        Color crossoverLineColor = frameGraphOptions.getCrossoverLineColor();
        Color mutantLineColor = frameGraphOptions.getMutantLineColor();
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(startX, startY, endX, endY);
        int numberOfParentalSequence = parentalSequences.size();
        double sequenceBandHeigth = (((double) endY - (double) startY) / (double) (numberOfParentalSequence + 1));
        float residueWidth = (endX - startX) / (endResidue - startResidue + 1);
        for (int nucleotideIndex = startResidue; nucleotideIndex < crossoverModel.getChimeraSequenceSize(); nucleotideIndex++) {
            int residueStartX = startX + (int) (residueWidth * (nucleotideIndex - startResidue));
            int residueEndX = (int) (residueStartX + residueWidth);
            CrossoverModel.Column column = crossoverModel.get(nucleotideIndex);
            if (column.isMutant()) { //draw Mutation
                graphics.setColor(mutantLineColor);
                graphics.fillRect(residueStartX, startY + 2, (int) residueWidth, 2);//Bars
            }
            boolean firstInGroup = false;
            int firstYInGroup = 0;//top one too
            for (int parentalSequenceNumber = 0; parentalSequenceNumber < numberOfParentalSequence; parentalSequenceNumber++) {
                CrossoverModel.SequenceData sequenceData = column.get(parentalSequenceNumber);
                //draw rectangles
                List<Color> sequenceColors = frameGraphOptions.getSequenceColors();
                graphics.setColor(sequenceColors.get(parentalSequenceNumber % COLORS.size()));
                int verticalOffsetFromStartY = (int) ((double) (parentalSequenceNumber + 1) * sequenceBandHeigth);
                double matchProportion = sequenceData.getMatchProportion();
                if (matchProportion > 0) {
                    int topY = startY + verticalOffsetFromStartY - (int) Math.ceil(VERTICAL_COVERAGE_OF_BAND / 2.0 * sequenceBandHeigth * matchProportion);
                    graphics.fillRect(residueStartX + 1, topY, (int) residueWidth, (int) Math.ceil(VERTICAL_COVERAGE_OF_BAND * sequenceBandHeigth * matchProportion));//+1 for the little gap
                }
                //Source horizontal Lines
                if (sequenceData.isSource()) {
                    graphics.setColor(crossoverLineColor);
                    //draw LINE
                    graphics.fillRect(residueStartX, startY + verticalOffsetFromStartY - 1, residueEndX - residueStartX, 2);
                    //vertical lines part for Sources
                    CrossoverModel.Length lastSourceLength = sequenceData.getLastNonOverlappingSource();
                    if (sequenceData.isFirstInSource() && lastSourceLength != null) {
                        int topY = startY + ((int) ((double) (lastSourceLength.getSequenceIndex() + 1) * sequenceBandHeigth)) - 1;
                        if (topY > (startY + verticalOffsetFromStartY - 1)) {
                            graphics.fillRect(residueStartX, (startY + verticalOffsetFromStartY - 1), 1, topY - (startY + verticalOffsetFromStartY - 1) + 2);//line  from lastSequence to the current one
                        } else {
                            graphics.fillRect(residueStartX, topY, 1, (startY + verticalOffsetFromStartY) - topY);//DOWNWARDS line from lastSequence to the current one
                        }
                    }
                    //Vertical lines for groups
                    if (sequenceData.isGroup()) {
                        if (!firstInGroup) {
                            firstInGroup = true;
                            firstYInGroup = startY + verticalOffsetFromStartY;
                        } else if (sequenceData.isLastInGroupOnlyAfterCheckingForGroup()) {
                            if (sequenceData.isFirstInSource()) {//starting line
                                graphics.fillRect(residueStartX, firstYInGroup, 1, (startY + verticalOffsetFromStartY) - firstYInGroup);//DOWNWARDS line from lastSequence to the current one
                            }
                            if (sequenceData.isLastInSource()) {
                                graphics.fillRect(residueStartX + (int) residueWidth, firstYInGroup, 1, (startY + verticalOffsetFromStartY) - firstYInGroup);//DOWNWARDS line from lastSequence to the current one
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Location getDefaultLocation() {
        return Location.ABOVE_RESIDUES;
    }

    @Override
    public int getDefaultHeight() {
        return 4 * super.getDefaultHeight();
    }

    @Override
    public Options getOptions() {
        if (frameGraphOptions == null) {
            frameGraphOptions = new FrameGraphOptions();
        }
        return frameGraphOptions;
    }

    @Override
    public void performBackgroundCalculations(ProgressListener progressListener) {
        super.performBackgroundCalculations(progressListener);
        System.out.println("performBackgroundCalculations called");
        //todo check progress listener for cancellation
        //todo get the last param     private boolean chimeraGapsArePartOfLength=true; from options
        //maybe chimeraGapsArePartOfLength should be assigned statically or maybe better passed to sequence
        crossoverModel = CrossoverModel.getInstance(true, realSequences, chimeraIsFirst);
    }

    @Override
    public String getName() {
        return "Crossover";
    }

    @Override
    public String getLegendName() {
        return "Crossover";
    }

    @Override
    public void drawScaleBar(GeneiousGraphics2D graphics, int startX, int startY, int endX, int endY) {
        List<SequenceDocument> parentsAsFullNucleotideSequences = crossoverModel.getParentsAsFullNucleotideSequences();
        double sequenceBandHeigth = (((double) endY - (double) startY) / (double) (parentsAsFullNucleotideSequences.size() + 1));//todo move this as a field
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(startX, startY, endX, endY);
        AtomicInteger sequenceNumber = new AtomicInteger(0);
        graphics.setColor(frameGraphOptions.getMutantLineColor());
        graphics.drawString("Mutations", startX + 1, (int) ((double) startY + 10));
        parentsAsFullNucleotideSequences.forEach(s -> {
            graphics.setColor(frameGraphOptions.getSequenceColors().get(sequenceNumber.get() % frameGraphOptions.getSequenceColors().size()));
            graphics.drawString(s.getName(), startX + 1, (int) ((double) (startY + 3) + sequenceBandHeigth * (double) (sequenceNumber.get() + 1)));
            sequenceNumber.incrementAndGet();
        });
    }

    @Override
    public int getScaleBarWidth() {
        return 80;
    }
}
