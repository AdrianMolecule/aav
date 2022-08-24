package dumboptim;

import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;

public class DumbOptimPlugin extends GeneiousPlugin {
    private final String HELP = "  Dumb Optim\n\n" +
            "---";

    public String getAuthors() {
        return "Adrian F";
    }

    public String getDescription() {
        return " todo";
    }

    public DocumentOperation[] getDocumentOperations() {
        return new DocumentOperation[]{new DumbOptimOperation()};
    }

    public String getHelp() {
        return HELP;
    }

    /**
     * The maximum API version is checked only against the major version number.
     * Thus, if getMaximumApiVersion() returns 4, the plugin will work with API
     * versions 4.1, 4.2, 4.3 and any other version 4.x, but not version 5.0
     * or above.
     */
    public int getMaximumApiVersion() {
        return 4;
    }

    /**
     * The minimum API version is checked against the entire API version string.
     * If the minimum API version is 4.0, the plugin will not work with API
     * versions 3.9 or below.
     */
    public String getMinimumApiVersion() {
        return "4.0";
    }

    public String getName() {
        return "Dumb Optim";
    }

    public String getVersion() {
        return "2.3.2";
    }

    @Override
    public String getEmailAddressForCrashes() {
        return "tada@specyal.com";
    }
}
