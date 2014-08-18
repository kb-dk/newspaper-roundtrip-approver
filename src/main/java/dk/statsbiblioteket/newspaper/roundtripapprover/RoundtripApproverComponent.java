package dk.statsbiblioteket.newspaper.roundtripapprover;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.TreeProcessorAbstractRunnableComponent;

import java.util.Properties;

/**
 *
 */
public class RoundtripApproverComponent extends TreeProcessorAbstractRunnableComponent {

    public static String APPROVED_EVENT = "Approved";

    protected RoundtripApproverComponent(Properties properties) {
        super(properties);
    }

    @Override
    public String getEventID() {
        return APPROVED_EVENT;
    }

    @Override
    public void doWorkOnBatch(Batch batch, ResultCollector resultCollector) throws Exception {

    }
}
