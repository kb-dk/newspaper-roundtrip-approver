package dk.statsbiblioteket.newspaper.roundtripapprover;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.sbutil.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.Event;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class RoundtripApproverComponentTestIT {

    private final Properties props = new Properties();
    private String batchId = "1337";
    private NewspaperDomsEventStorage domsEventStorage;

    @BeforeMethod(alwaysRun = true)
    private void loadConfiguration() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        props.load(new FileInputStream(pathToProperties));
        purgeBatch();
    }
    
    @AfterMethod
    public void purgeBatch() throws IOException, JAXBException, PIDGeneratorException, BackendInvalidCredsException, BackendMethodFailedException, BackendInvalidResourceException {
        Credentials creds = new Credentials(props.getProperty(ConfigConstants.DOMS_USERNAME), props.getProperty(ConfigConstants.DOMS_PASSWORD));
        EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(
                creds, props.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""), "", null);
        NewspaperDomsEventStorageFactory factory = new NewspaperDomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
        domsEventStorage = factory.createDomsEventStorage();
        String[] objectsToDelete = {"path:B1337", "path:B1337-RT1", "path:B1337-RT2", "path:B1337-RT4", "path:B1337-RT5"};
        for (String object: objectsToDelete) {
            List<String> founds = fedora.findObjectFromDCIdentifier(object);
            if (founds != null && !founds.isEmpty()) {
                fedora.deleteObject(founds.get(0), "");
            }
        }
    }

    /**
        * Create a test setup with a batch with round trips 1,2,4,5 where
        * 1,2,4 are flagged for manual QA and 5 is not.
        * Set all as mfpak approved then call the component on each in turn.
        * Numbers 1,2 and should be set as "Approved=failed"
        * Number 4 as "Approved" and
        * Number 5 as "Manually Stopped" and "Approved=failed"
        *
        * @throws Exception
        */
       @Test(groups = {"externalTest", "integrationTest"})
       public void testdoWorkOnItemIT() throws Exception {
           String dataReceived = "Data_Received";
           String manualQAFlagged = "Manual_QA_Flagged";
           String mfpakApproved = "Approved";
           String details = "Details here";
           Batch batch1 = new Batch(batchId,1);
           Batch batch4 = new Batch(batchId, 4);
           Batch batch5 = new Batch(batchId, 5);
           Batch batch2 = new Batch(batchId, 2);

           domsEventStorage.appendEventToItem(batch1, "agent", new Date(), details, dataReceived, true);
           domsEventStorage.appendEventToItem(batch1, "agent", new Date(), details, manualQAFlagged, true);
           domsEventStorage.appendEventToItem(batch1, "agent", new Date(), details, mfpakApproved, true);
           domsEventStorage.appendEventToItem(batch4, "agent", new Date(), details, dataReceived, true);
           domsEventStorage.appendEventToItem(batch4, "agent", new Date(), details, manualQAFlagged, true);
           domsEventStorage.appendEventToItem(batch4, "agent", new Date(), details, mfpakApproved, true);
           domsEventStorage.appendEventToItem(batch2, "agent", new Date(), details, dataReceived, true);
           domsEventStorage.appendEventToItem(batch2, "agent", new Date(), details, manualQAFlagged, true);
           domsEventStorage.appendEventToItem(batch2, "agent", new Date(), details, mfpakApproved, true);
           domsEventStorage.appendEventToItem(batch5, "agent", new Date(), details, dataReceived, true);
           domsEventStorage.appendEventToItem(batch5, "agent", new Date(), details, mfpakApproved, true);

           Batch batch = domsEventStorage.getItemFromFullID(batch1.getFullID());
           ResultCollector resultCollector = new ResultCollector("foo", "bar", null);
           RoundtripApproverComponent component = new RoundtripApproverComponent(props, domsEventStorage);
           component.doWorkOnItem(batch, resultCollector);
           assertFalse(resultCollector.isSuccess());
           batch = domsEventStorage.getItemFromFullID(batch2.getFullID());
           resultCollector = new ResultCollector("foo", "bar", null);
           component.doWorkOnItem(batch, resultCollector);
           assertFalse(resultCollector.isSuccess());
           batch = domsEventStorage.getItemFromFullID(batch4.getFullID());
           resultCollector = new ResultCollector("foo", "bar", null);
           component.doWorkOnItem(batch, resultCollector);
           assertTrue(resultCollector.isSuccess());
           batch = domsEventStorage.getItemFromFullID(batch5.getFullID());
           resultCollector = new ResultCollector("foo", "bar", null);
           component.doWorkOnItem(batch, resultCollector);
           assertFalse(resultCollector.isSuccess());
           Batch roundtrip5 = domsEventStorage.getItemFromFullID(batch5.getFullID());
           boolean isStopped = false;
           for (Event event: roundtrip5.getEventList()) {
               if (event.getEventID().equals("Manually_stopped")) {
                   isStopped = true;
               }
           }
           assertFalse(resultCollector.isSuccess());
           assertTrue(isStopped, "Should have found a Manually_Stopped event.");
       }

}
