package dk.statsbiblioteket.newspaper.roundtripapprover;


import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.Event;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;


import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class RoundtripApproverComponentTest {

    /**
     * Verify that the expected external DOMS calls are made by this component. We have a batch with roundtrip
     * numbers 1,2,4,5 which has been approved in mfpak. Roundtrips 1,2,4 have been flagged for manual QA. We
     * process the roundtrips sequentially. A failure is added to the result collector for each of roundtrips 1,2,5 .
     * Also roundtrip 5 adds a Manually_stopped event.
     * @throws Exception
     */
    @Test(groups = "integrationTest")
    public void testdoWorkOnItem() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        String batchId = getRandomBatchId();
        Batch rt1 = new Batch(batchId, 1);
        Batch rt2 = new Batch(batchId, 2);
        Batch rt4 = new Batch(batchId, 4);
        Batch rt5 = new Batch(batchId, 5);
        List<Batch>  roundtrips = Arrays.asList(new Batch[]{rt1,rt2,rt4,rt5});

        String dataReceived = "Data_Received";
               String manualQAFlagged = "Manual_QA_Flagged";
               String mfpakApproved = "Approved";
        Event dataReceivedEvent = new Event();
        dataReceivedEvent.setEventID(dataReceived);
        Event manualQAEvent = new Event();
        manualQAEvent.setEventID(manualQAFlagged);
        Event mfpakApprovedEvent = new Event();
        mfpakApprovedEvent.setEventID(mfpakApproved);

        List<Event> allEvents = Arrays.asList(dataReceivedEvent, manualQAEvent, mfpakApprovedEvent);
        List<Event> notFlaggedEvents = Arrays.asList(dataReceivedEvent, mfpakApprovedEvent);

        rt1.setEventList(allEvents);
        rt2.setEventList(allEvents);
        rt4.setEventList(allEvents);
        rt5.setEventList(notFlaggedEvents);

        NewspaperDomsEventStorage domsEventStorage = mock(NewspaperDomsEventStorage.class);
        when(domsEventStorage.getAllRoundTrips(anyString())).thenReturn(roundtrips);


        ResultCollector resultCollector = new ResultCollector("foo", "bar", null);
        RoundtripApproverComponent component = new RoundtripApproverComponent(props, domsEventStorage);
        component.doWorkOnItem(rt1, resultCollector);
        verify(domsEventStorage).getAllRoundTrips(batchId);
        assertEquals(resultCollector.toReport().split("exception").length, 2);  //1 exception
        component.doWorkOnItem(rt2, resultCollector);
        assertEquals(resultCollector.toReport().split("exception").length, 3);  //2 exceptions
        component.doWorkOnItem(rt4, resultCollector);
        assertEquals(resultCollector.toReport().split("exception").length, 3);  //no new exceptions
        verify(domsEventStorage, never()).addEventToItem(any(Batch.class), anyString(), any(Date.class), anyString(), anyString(), anyBoolean());
        component.doWorkOnItem(rt5, resultCollector);
        assertEquals(resultCollector.toReport().split("exception").length, 4);  //3 exceptions
        //Verify that an event has been added. This is the Manually_stopped event.
        verify(domsEventStorage).addEventToItem(eq(rt5), anyString(), any(Date.class), anyString(), eq("Manually_stopped"), eq(true));
    }

    private String getRandomBatchId() {
        return "4000220" + Math.round(Math.random() * 100000);
    }
}
