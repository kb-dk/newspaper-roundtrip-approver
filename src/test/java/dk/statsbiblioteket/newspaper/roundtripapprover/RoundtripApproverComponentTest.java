package dk.statsbiblioteket.newspaper.roundtripapprover;


import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.Event;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class RoundtripApproverComponentTest {

    /**
     * Create a test setup with a batch with round trips 1,2,4,5 where
     * 1,2,4 are flagged for manual QA and 5 is not.
     * Set all as mfpak approved then call the component on each in turn.
     * Numbers 1,2 and should be set as "Approved=failed"
     * Number 4 as "Approved" and
     * Number 5 as "Manually Stopped"
     *
     * @throws Exception
     */
    @Test(groups = {"externalTest"})
    public void testDoWorkOnBatch() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        DomsEventStorageFactory factory = new DomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        DomsEventStorage domsEventStorage = factory.createDomsEventStorage();

        String batchId = getRandomBatchId();
        String dataReceived = "Data_Received";
        String manualQAFlagged = "Manual_QA_Flagged";
        String mfpakApproved = "Approved";
        String details = "Details here";

        domsEventStorage.addEventToBatch(batchId, 1, "agent", new Date(), details, dataReceived, true);
        domsEventStorage.addEventToBatch(batchId, 1, "agent", new Date(), details, manualQAFlagged, true);
        domsEventStorage.addEventToBatch(batchId, 1, "agent", new Date(), details, mfpakApproved, true);
        domsEventStorage.addEventToBatch(batchId, 4, "agent", new Date(), details, dataReceived, true);
        domsEventStorage.addEventToBatch(batchId, 4, "agent", new Date(), details, manualQAFlagged, true);
        domsEventStorage.addEventToBatch(batchId, 4, "agent", new Date(), details, mfpakApproved, true);
        domsEventStorage.addEventToBatch(batchId, 2, "agent", new Date(), details, dataReceived, true);
        domsEventStorage.addEventToBatch(batchId, 2, "agent", new Date(), details, manualQAFlagged, true);
        domsEventStorage.addEventToBatch(batchId, 2, "agent", new Date(), details, mfpakApproved, true);
        domsEventStorage.addEventToBatch(batchId, 5, "agent", new Date(), details, dataReceived, true);
        domsEventStorage.addEventToBatch(batchId, 5, "agent", new Date(), details, mfpakApproved, true);

        Batch batch = domsEventStorage.getBatch(batchId, 1);
        ResultCollector resultCollector = new ResultCollector("foo", "bar", null);
        RoundtripApproverComponent component = new RoundtripApproverComponent(props);
        component.doWorkOnBatch(batch, resultCollector);
        assertFalse(resultCollector.isSuccess());
        batch = domsEventStorage.getBatch(batchId, 2);
        resultCollector = new ResultCollector("foo", "bar", null);
        component.doWorkOnBatch(batch, resultCollector);
        assertFalse(resultCollector.isSuccess());
        batch = domsEventStorage.getBatch(batchId, 4);
        resultCollector = new ResultCollector("foo", "bar", null);
        component.doWorkOnBatch(batch, resultCollector);
        assertTrue(resultCollector.isSuccess());
        batch = domsEventStorage.getBatch(batchId, 5);
        resultCollector = new ResultCollector("foo", "bar", null);
        component.doWorkOnBatch(batch, resultCollector);
        assertFalse(resultCollector.isSuccess());
        Batch roundtrip5 = domsEventStorage.getBatch(batchId, 5);
        boolean isStopped = false;
        for (Event event: roundtrip5.getEventList()) {
             if (event.getEventID().equals("Manually_Stopped")) {
                 isStopped = true;
             }
        }
        assertTrue(isStopped, "Should have found a Manually_Stopped event.");
    }

    private String getRandomBatchId() {
        return "4000220252" + Math.round(Math.random() * 100);
    }
}
