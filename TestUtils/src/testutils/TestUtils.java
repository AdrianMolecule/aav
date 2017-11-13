package testutils;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.plugin.PluginUtilities;
import com.biomatters.geneious.publicapi.utilities.ProgressInputStream;
import jebl.util.BasicProgressListener;
import org.junit.Assert;

import javax.annotation.Nonnull;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestUtils {

    public static final String RELATIVE_TEST_DATA_DIR = "testdata/";

    //compare  the sequences and the annotations
     public static void compareDocuments(File oldDocPath, @Nonnull File newDocPath) throws Exception {
        List<AnnotatedPluginDocument> oldDoc = PluginUtilities.importDocuments(oldDocPath, null);
        List<AnnotatedPluginDocument> newDoc = PluginUtilities.importDocuments(newDocPath, null);
        for (int docIndex = 0; docIndex < oldDoc.size(); docIndex++) {
            AnnotatedPluginDocument oldDocPart = oldDoc.get(docIndex);
            AnnotatedPluginDocument newDocPart = newDoc.get(docIndex);
            DefaultAlignmentDocument oldDocumentOrCrash = ((DefaultAlignmentDocument) oldDocPart.getDocumentOrCrash());
            DefaultAlignmentDocument newDocumentOrCrash = ((DefaultAlignmentDocument) newDocPart.getDocumentOrCrash());
            System.out.println("Old doc name:" + oldDocumentOrCrash.getName());
            System.out.println("New doc name:" + newDocumentOrCrash.getName());
            Assert.assertEquals(oldDocumentOrCrash.getSequences().size(), newDocumentOrCrash.getSequences().size());
            for (int sec = 0; sec < oldDocumentOrCrash.getSequences().size(); sec++) {
                Assert.assertEquals(" Sequence index:" + sec + " named:" + oldDocumentOrCrash.getSequences().get(sec).getName() + " for old one",
                        oldDocumentOrCrash.getSequences().get(sec).getSequenceString(), newDocumentOrCrash.getSequences().get(sec).getSequenceString());
                Assert.assertEquals(" Sequence annotations:" + sec + " named:" + oldDocumentOrCrash.getSequences().get(sec).getName() + " for old one",
                        oldDocumentOrCrash.getSequences().get(sec).getSequenceAnnotations(), newDocumentOrCrash.getSequences().get(sec).getSequenceAnnotations());
            }
        }
    }

    public static List<DefaultNucleotideSequence> loadFileSequences(File file) throws Exception {
        List<DefaultNucleotideSequence> sequenceList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ProgressInputStream(new BasicProgressListener(), file)))) {
            String line;
            StringBuilder currentSequence = new StringBuilder();
            String currentName = "";
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;//--------------->
                }
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith(">")) {
                    if (currentSequence.length() > 0) {
                        sequenceList.add(new DefaultNucleotideSequence(currentName, currentSequence.toString()));
                        currentSequence = new StringBuilder();
                    }
                    currentName = line.substring(1);
                } else {
                    currentSequence.append(line);
                }
            }
            if (currentSequence.length() > 0) {
                sequenceList.add(new DefaultNucleotideSequence(currentName, currentSequence.toString()));
            }
        }
        return sequenceList;
    }

    static   private List<Integer> findDiffIndexes(String s1, String s2) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < s1.length() && i < s2.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    public static int findFirstDiffIndex(String s1, String s2) {
        List<Integer> indexes = findDiffIndexes(s1, s2);
        if (indexes.isEmpty()) return -1;
        else return indexes.get(0);
    }

    private static <F> F getPrivateField(String fieldName, Object targetObject)
            throws NoSuchFieldException, IllegalAccessException {
        Field field =
                targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (F) field.get(targetObject);
    }

    private static void setPrivateField(String fieldName, Object targetObject, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field =
                targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObject, value);
    }

    public static <T> T callPrivateMethod(String aMethod, Options targetObject, Object arg) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Method method = targetObject.getClass().getDeclaredMethod(aMethod, arg.getClass());
        method.setAccessible(true);
        return (T) method.invoke(targetObject, arg);
    }

    public static Options getDefaultOptions(Options options) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        MutablePicoContainer pico = new DefaultPicoContainer(new SetterInjection());
//        pico.addComponent(DirectedEvolutionOperationOptions.class);
//        DirectedEvolutionOperationOptions options = pico.getComponent(DirectedEvolutionOperationOptions.class);
        //ActionEvent dummyEvent = mockery.mock(ActionEvent.class);
        ActionEvent actionEvent = new ActionEvent("", 0, null);
        callPrivateMethod("resetOptions", options, actionEvent);
        //options.resetOptions(dummyEvent);
//        DirectedEvolutionOperationOptions mockedOptions = mockery.mock(DirectedEvolutionOperationOptions.class);
//        mockedOptions.setValue("useOptions",new Boolean(false));
//        mockedOptions.("useOptions",new Boolean(false));
//        generateOligosOperation.setOptions(mockedOptions);
        return options;
    }
}
