package hiddengene;

import com.biomatters.geneious.publicapi.documents.sequence.NucleotideGraph;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.plugin.GeneiousGraphics2D;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.plugin.SequenceGraph;
import jebl.util.ProgressListener;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Frame Conservation
 */
@SuppressWarnings("NumericCastThatLosesPrecision")
class FrameLogoSequenceGraph extends SequenceGraph {
    private Frame frame;
    private List<CharSequence> sequences = null;
    private FrameMaxes frameMaxes;
    FrameGraphOptions frameGraphOptions;
    private static final Color GRADIENT_BASE_COLOR = Color.RED;
    private static final int LEFT_INDENT = 3;
    AffineTransform FONT_AT = new AffineTransform();

    double THETA = 90.0 * (java.lang.Math.PI / 180.0);

    public FrameLogoSequenceGraph(SequenceDocument.Alphabet alphabet, Frame frame) {
        this.frame = frame;
        FONT_AT.rotate(THETA);
    }

    @Override
    public void setResidues(List<CharSequence> sequences, List<NucleotideGraph> nucleotideGraphs, boolean ignoreEndGaps) {
        this.sequences = sequences;
        performBackgroundCalculations(null);
    }

    @Override
    public void draw(GeneiousGraphics2D graphics, int startResidue, int endResidue, int startX, int startY, int endX, int endY, double averageResidueWidth, int previousSectionWidth, int nextSectionWidth, int previousSectionResidueCount, int nextSectionResidueCount) {
        //Y is downwards so 2 is above 10 ////todo leading is a vertical thing but it seems to work well in terms of range
        int numberOfSequences = sequences.size();
        float residueWidth = (endX - startX) / (endResidue - startResidue + 1);
        for (int residueNumber = startResidue; residueNumber <= endResidue; residueNumber++) {
            int bottomNumber = frameMaxes.getMax(residueNumber, frame);
            for (int residueIndex = startResidue; residueIndex <= endResidue; residueIndex++) {
                int residueStartX=startX+(int)(residueWidth*(residueNumber-startResidue));
                int residueEndX=(int)(residueStartX+residueWidth);
                int topNumber = frameMaxes.getMax(residueNumber, Frame.ZERO);
                Boolean displayBaseFrame = frameGraphOptions.getDisplayBaseFrame();
                double rectangleFactor;
                if (displayBaseFrame) {
                    rectangleFactor = (((double) bottomNumber + (double) topNumber) / (2.0 * (double) numberOfSequences));
                } else {
                    rectangleFactor = ((double) bottomNumber / (double) numberOfSequences);
                }
                drawRectanglesAndStrings(graphics, residueStartX, startY, residueEndX, endY, numberOfSequences, topNumber, bottomNumber, rectangleFactor);
            }
        }
    }

    private void drawRectanglesAndStrings(GeneiousGraphics2D graphics, int residueStartX, int startY, int residueEndX, int endY, int numberOfSequences, int topNumber, int bottomNumber, double rectangleFactor) {
        Color color;
        if (frameGraphOptions.getDisplayAsGradient()) {
            color = new Color(Math.max((int) ((double) GRADIENT_BASE_COLOR.getRed() * rectangleFactor), 0),
                    Math.max((int) ((double) GRADIENT_BASE_COLOR.getGreen() * rectangleFactor), 0),
                    Math.max((int) ((double) GRADIENT_BASE_COLOR.getBlue() * rectangleFactor), 0),
                    GRADIENT_BASE_COLOR.getAlpha());
        } else if (frameGraphOptions.getDisplayAsPlainColor()) {
            color = Color.CYAN;
        } else {
            if (bottomNumber > topNumber) {
                color = Color.RED;
            } else if (bottomNumber == topNumber) {
                color = Color.PINK;
            } else {
                color = Color.LIGHT_GRAY;
            }
        }
        graphics.setColor(color);
        double heightFraction;
        if (frameGraphOptions.getDisplayBaseFrame()) {
            heightFraction = (((double) (topNumber + bottomNumber) / (2.0 * (double) numberOfSequences)));
        } else {
            heightFraction = (((double) bottomNumber / ((double) numberOfSequences)));
        }
        int topY = (int) ((double) endY - (double) (endY - startY) * heightFraction);
        graphics.fillRect(residueStartX, topY, residueEndX, endY);
        ////////////LABELS
        graphics.setColor(Color.BLACK);
        Graphics2D realGraphics = graphics.getRealGraphics();
        if (frameGraphOptions.getDisplayBaseFrame()) {
            if (frameGraphOptions.getDisplayAsNumber()) {
                FontMetrics fontMetrics = realGraphics.getFontMetrics();
                graphics.drawString("" + topNumber, residueStartX + fontMetrics.getLeading(), endY - fontMetrics.getHeight() + 4);
                graphics.drawString("" + bottomNumber, residueStartX + fontMetrics.getLeading(), endY);
            } else {//percentage
                Font theFont = realGraphics.getFont();
                FontMetrics fontMetrics = realGraphics.getFontMetrics();
                realGraphics.setFont(theFont.deriveFont(FONT_AT));
                realGraphics.drawString("" + (int) ((((double) bottomNumber + (double) topNumber) / (2.0 * (double) numberOfSequences)) * 100.0), residueStartX + LEFT_INDENT, endY - 3 * fontMetrics.getHeight());
                realGraphics.drawString("" + (int) (((double) bottomNumber + (double) bottomNumber) / (2.0 * (double) numberOfSequences) * 100.0), residueStartX + LEFT_INDENT, endY - (int) (1.2 * fontMetrics.getHeight()));
                realGraphics.setFont(theFont);
            }
        } else {
            if (frameGraphOptions.getDisplayAsNumber()) {
                FontMetrics fontMetrics = realGraphics.getFontMetrics();
                graphics.drawString("" + bottomNumber, residueStartX + LEFT_INDENT, endY - fontMetrics.getLeading());//todo leading is a vertical thing but it seems to work well in terms of range
            } else {//percentage
                Font theFont = realGraphics.getFont();
                FontMetrics fontMetrics = realGraphics.getFontMetrics();
                realGraphics.setFont(theFont.deriveFont(FONT_AT));
                realGraphics.drawString("" + (int) (rectangleFactor * 100), residueStartX + LEFT_INDENT, endY - (2 * fontMetrics.getHeight()));
                realGraphics.setFont(theFont);
            }
        }
    }

    private void verticalFont(Graphics2D g2D, int startX, int endY, double factor, FontMetrics fontMetrics) {
        //todo leading is a vertical thing but it seems to work well in terms of range
        // put the original font back
    }

    @Override
    public Location getDefaultLocation() {
        return Location.ABOVE_RESIDUES;
    }

    @Override
    public int getDefaultHeight() {
        return 2 * super.getDefaultHeight();
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
        frameMaxes = FrameMaxes.getInstance(true, sequences);
    }

    /*
            //DDD
            // DDD
            //  DDD
            //RRR   <-
            // RRR  <-
            //  RRR <-
             */
    public enum Frame {
        ZERO, ONE, TWO, REVERSE0, REVERSE1, REVERSE2;

        public String toDisplayString() {// when display count from 1 not from zero like in the code
            switch (this) {
                case ZERO:
                    return "Frame 1";
                case ONE:
                    return "Frame 2";
                case TWO:
                    return "Frame 3";
                case REVERSE0:
                    return "Reverse Frame 1";
                case REVERSE1:
                    return "Reverse Frame 2";
                case REVERSE2:
                    return "Reverse Frame 3";
                default:
                    return "Unknown";
            }
        }
    }

    @Override
    public String getName() {
        String bottom = "";
        switch (frame) {
            case ONE:
                bottom = "2";
                break;
            case TWO:
                bottom = "3";
                break;
            case REVERSE0:
                bottom = "reverse 1";
                break;
            case REVERSE1:
                bottom = "reverse 2";
                break;
            case REVERSE2:
                bottom = "reverse 3";
                break;
            default:
                bottom = "ERROR";
        }
        String frameTotal = frameMaxes.getFrameTotal(frame);
        if (((FrameGraphOptions) getOptions()).getDisplayBaseFrame()) {
            return "Frame 1 & " + bottom + " Conservation (" + frameTotal + ')';
        } else {
            return "Frame " + bottom + " Conservation (" + frameTotal + ')';
        }
    }
}
