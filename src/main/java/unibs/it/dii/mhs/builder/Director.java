package unibs.it.dii.mhs.builder;

import unibs.it.dii.utility.Args;

public class Director {

    private static final int MILLISECONDS = 1000;

    Args arguments;

    public Director(Args arguments) {
        this.arguments = arguments;
    }

    /**
     * Method to build the MinimalHittingSetFacade object.
     * @param builder the interface of the builder that allows to create the target object
     */
    public void buildMinimalHittingSetFacade(FacadeBuilder builder) {
        builder.setPreProcessing(arguments.isPreProcessing());
        builder.setVerbosity(arguments.isVerbose());
        builder.setInputPath(arguments.getInputPath());
        builder.setOutputPath(arguments.getOutputPath());
        builder.setInputDirectoryPath(arguments.getDirectoryPath());
        builder.setTimeout(getMillis(arguments.getTimeout()));
        builder.setAutomaticMode(arguments.isAutomaticMode());
    }

    /**
     * Method to change the time unit from seconds to milliseconds.
     *
     * @param time a time value in seconds
     * @return the time value in milliseconds
     */
    private long getMillis(long time) {
        return time * MILLISECONDS;
    }
}
