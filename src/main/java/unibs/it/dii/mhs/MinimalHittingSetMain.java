package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import unibs.it.dii.mhs.builder.Director;
import unibs.it.dii.mhs.builder.MinimalHittingSetFacadeBuilder;
import unibs.it.dii.utility.*;

/**
 *
 */
public class MinimalHittingSetMain {

    public static void main(String[] args) throws Exception {
        final Args arguments = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(arguments)
                .build();

        try {
            jc.parse(args);
        } catch (ParameterException pe) {
            System.err.println(pe.getMessage());
            jc.usage();
            System.exit(0);
        }

        MinimalHittingSetMain main = new MinimalHittingSetMain();

        main.run(arguments, jc);

    }

    public void run(Args arguments, JCommander jc) throws Exception {
        // Help call by [-h|--help] argument
        if (arguments.isHelp()) {
            jc.usage();
            System.exit(255);
        }

        if (invalidArgumentsCondition(arguments)) {
            System.err.println("Please choose only one argument between -in or -dir");
            jc.usage();
            System.exit(200);
        }

        // -----------------------------------------------------------------------------

        Director director = new Director(arguments);
        MinimalHittingSetFacadeBuilder builder = new MinimalHittingSetFacadeBuilder();

        director.buildMinimalHittingSetFacade(builder);

        MinimalHittingSetFacade minimalHittingSet = builder.getMHS();

        minimalHittingSet.find();

        // -----------------------------------------------------------------------------

    }

    /**
     * Method to check if only one mode is selected between manual or automatic.
     *
     * @param args the arguments parsed by JCommander
     * @return true if the arguments passed are invalid
     */
    private boolean invalidArgumentsCondition(Args args) {
        return (args.isManualMode() && args.isAutomaticMode()) || (!args.isManualMode() && !args.isAutomaticMode());
    }
}