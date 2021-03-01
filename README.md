This repository have been archived and exists for historical purposes. 
No updates or futher development will go into this repository. The content can be used as is but no support will be given. 

---

newspaper-roundtrip-approver
============================

This component determines whether a newspaper batch round-trip should be approved.

The logic is as follows:

When an MFPak_Approved event is registered, it triggers the component for every known round trip for the given batch.
Only batches for which a ManualQA_Flagged event exists can be approved.

So, if this is the most recent roundtrip with ManualQA_Flagged set, then it is set Approved=true.
Earlier roundtrips are set Approved=false.
Later roundtrips are set to Manually Stopped.

Configuration
-------------
The component uses only standard configuration parameters. Event parameters should be set as follows:

     autonomous.pastSuccessfulEvents=MFPak_Approved
     autonomous.oldEvents=
     autonomous.futureEvents=Approved
