package com.sigis.tlc.rfid.tests;
import java.util.ArrayList;
import java.util.List;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.generated.enumerations.*;
import org.llrp.ltk.generated.interfaces.AccessCommandOpSpec;
import org.llrp.ltk.generated.interfaces.AccessCommandOpSpecResult;
import org.llrp.ltk.generated.messages.*;
import org.llrp.ltk.generated.parameters.*;
import org.llrp.ltk.net.*;
import org.llrp.ltk.types.*;
  
public class HelloJavaLtk implements LLRPEndpoint
{
    private LLRPConnection reader;
    private static final int TIMEOUT_MS = 10000;
    private static final int ROSPEC_ID = 123;
    @SuppressWarnings("unused")
	private static final String TARGET_EPC = "372d055bfca26487b78f47011078";
    @SuppressWarnings("unused")
	private static final String TAG_MASK =   "FFFFFFFFFFFFFFFFFFFFFFFFFFFF";
    private static final int WRITE_ACCESSSPEC_ID = 555;
    private static final int READ_ACCESSSPEC_ID = 444;
    private static final int WRITE_OPSPEC_ID = 2121;
    private static final int READ_OPSPEC_ID = 1212;
      
    // Build the ROSpec.
    // An ROSpec specifies start and stop triggers,
    // tag report fields, antennas, etc.
    public ROSpec buildROSpec()
    {
        System.out.println("Building the ROSpec.");
          
        // Create a Reader Operation Spec (ROSpec).
        ROSpec roSpec = new ROSpec();
          
        roSpec.setPriority(new UnsignedByte(0));
        roSpec.setCurrentState(new ROSpecState(ROSpecState.Disabled));
        roSpec.setROSpecID(new UnsignedInteger(ROSPEC_ID));
          
        // Set up the ROBoundarySpec
        // This defines the start and stop triggers.
        ROBoundarySpec roBoundarySpec = new ROBoundarySpec();
          
        // Set the start trigger to null.
        // This means the ROSpec will start as soon as it is enabled.
        ROSpecStartTrigger startTrig = new ROSpecStartTrigger();
        startTrig.setROSpecStartTriggerType
            (new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));
        roBoundarySpec.setROSpecStartTrigger(startTrig);
          
        // Set the stop trigger is null. This means the ROSpec
        // will keep running until an STOP_ROSPEC message is sent.
        ROSpecStopTrigger stopTrig = new ROSpecStopTrigger();
        stopTrig.setDurationTriggerValue(new UnsignedInteger(0));
        stopTrig.setROSpecStopTriggerType
            (new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
        roBoundarySpec.setROSpecStopTrigger(stopTrig);
          
        roSpec.setROBoundarySpec(roBoundarySpec);
          
        // Add an Antenna Inventory Spec (AISpec).
        AISpec aispec = new AISpec();
          
        // Set the AI stop trigger to null. This means that
        // the AI spec will run until the ROSpec stops.
        AISpecStopTrigger aiStopTrigger = new AISpecStopTrigger();
        aiStopTrigger.setAISpecStopTriggerType
            (new AISpecStopTriggerType(AISpecStopTriggerType.Null));
        aiStopTrigger.setDurationTrigger(new UnsignedInteger(0));
        aispec.setAISpecStopTrigger(aiStopTrigger);
          
        // Select which antenna ports we want to use.
        // Setting this property to zero means all antenna ports.
        UnsignedShortArray antennaIDs = new UnsignedShortArray();
        antennaIDs.add(new UnsignedShort(0));
        aispec.setAntennaIDs(antennaIDs);
          
        // Tell the reader that we're reading Gen2 tags.
        InventoryParameterSpec inventoryParam = new InventoryParameterSpec();
        inventoryParam.setProtocolID
            (new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));
        inventoryParam.setInventoryParameterSpecID(new UnsignedShort(1));
        aispec.addToInventoryParameterSpecList(inventoryParam);
          
        roSpec.addToSpecParameterList(aispec);
          
        // Specify what type of tag reports we want
        // to receive and when we want to receive them.
        ROReportSpec roReportSpec = new ROReportSpec();
        // Receive a report every time a tag is read.
        roReportSpec.setROReportTrigger(new ROReportTriggerType
            (ROReportTriggerType.Upon_N_Tags_Or_End_Of_ROSpec));
        roReportSpec.setN(new UnsignedShort(1));
        TagReportContentSelector reportContent =
            new TagReportContentSelector();
        // Select which fields we want in the report.
        reportContent.setEnableAccessSpecID(new Bit(0));
        reportContent.setEnableAntennaID(new Bit(0));
        reportContent.setEnableChannelIndex(new Bit(0));
        reportContent.setEnableFirstSeenTimestamp(new Bit(0));
        reportContent.setEnableInventoryParameterSpecID(new Bit(0));
        reportContent.setEnableLastSeenTimestamp(new Bit(1));
        reportContent.setEnablePeakRSSI(new Bit(0));
        reportContent.setEnableROSpecID(new Bit(0));
        reportContent.setEnableSpecIndex(new Bit(0));
        reportContent.setEnableTagSeenCount(new Bit(0));
        roReportSpec.setTagReportContentSelector(reportContent);
        roSpec.setROReportSpec(roReportSpec);
          
        return roSpec;
    }
      
    // Add the ROSpec to the reader.
    public void addROSpec()
    {
        ADD_ROSPEC_RESPONSE response;
          
        ROSpec roSpec = buildROSpec();
        System.out.println("Adding the ROSpec.");
        try
        {
            ADD_ROSPEC roSpecMsg = new ADD_ROSPEC();
            roSpecMsg.setROSpec(roSpec);
            response = (ADD_ROSPEC_RESPONSE)
                reader.transact(roSpecMsg, TIMEOUT_MS);
            System.out.println(response.toXMLString());
              
            // Check if the we successfully added the ROSpec.
            StatusCode status = response.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success")))
            {
                System.out.println
                    ("Successfully added ROSpec.");
            }
            else
            {
                System.out.println("Error adding ROSpec.");
                System.exit(1);
            }
        }
        catch (Exception e)
        {
            System.out.println("Error adding ROSpec.");
            e.printStackTrace();
        }
    }
      
    // Enable the ROSpec.
    public void enableROSpec()
    {
        ENABLE_ROSPEC_RESPONSE response;
          
        System.out.println("Enabling the ROSpec.");
        ENABLE_ROSPEC enable = new ENABLE_ROSPEC();
        enable.setROSpecID(new UnsignedInteger(ROSPEC_ID));
        try
        {
            response = (ENABLE_ROSPEC_RESPONSE)
                reader.transact(enable, TIMEOUT_MS);
            System.out.println(response.toXMLString());
        }
        catch (Exception e)
        {
            System.out.println("Error enabling ROSpec.");
            e.printStackTrace();
        }
    }
      
    // Start the ROSpec.
    public void startROSpec()
    {
        START_ROSPEC_RESPONSE response;
        System.out.println("Starting the ROSpec.");
        START_ROSPEC start = new START_ROSPEC();
        start.setROSpecID(new UnsignedInteger(ROSPEC_ID));
        try
        {
            response = (START_ROSPEC_RESPONSE)
                reader.transact(start, TIMEOUT_MS);
            System.out.println(response.toXMLString());
        }
        catch (Exception e)
        {
            System.out.println("Error deleting ROSpec.");
            e.printStackTrace();
        }
    }
      
    // Delete all ROSpecs from the reader.
    public void deleteROSpecs()
    {
        DELETE_ROSPEC_RESPONSE response;
          
        System.out.println("Deleting all ROSpecs.");
        DELETE_ROSPEC del = new DELETE_ROSPEC();
        // Use zero as the ROSpec ID.
        // This means delete all ROSpecs.
        del.setROSpecID(new UnsignedInteger(0));
        try
        {
            response = (DELETE_ROSPEC_RESPONSE)
                reader.transact(del, TIMEOUT_MS);
            System.out.println(response.toXMLString());
        }
        catch (Exception e)
        {
            System.out.println("Error deleting ROSpec.");
            e.printStackTrace();
        }
    }
      
    // This function gets called asynchronously
    // when a tag report is available.
    public void messageReceived(LLRPMessage message)
    {
    	
    	System.out.println("nuevo mensaje");
    	
    	try {
			System.out.println(message.toXMLString());
		} catch (InvalidLLRPMessageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	 if (message.getTypeNum() == RO_ACCESS_REPORT.TYPENUM)
    	    {
    	        // The message received is an Access Report.
    	        RO_ACCESS_REPORT report = (RO_ACCESS_REPORT) message;
    	        // Get a list of the tags read.
    	        List <TagReportData> tags = report.getTagReportDataList();
    	        // Loop through the list and get the EPC of each tag.
    	        for (TagReportData tag : tags)
    	        {
    	            System.out.println(tag.getEPCParameter());
    	            System.out.println(tag.getLastSeenTimestampUTC());
    	            List <AccessCommandOpSpecResult> ops =
    	                tag.getAccessCommandOpSpecResultList();
    	            // See if any operations were performed on
    	            // this tag (read, write, kill).
    	            // If so, print out the details.
    	            for (AccessCommandOpSpecResult op : ops)
    	            {
    	                System.out.println(op.toString());
    	            }
    	        }
    	    }
    }
      
    // This function gets called asynchronously
    // when an error occurs.
    public void errorOccured(String s)
    {
        System.out.println("An error occurred: " + s);
    }
      
    // Connect to the reader
    public void connect(String hostname)
    {
        // Create the reader object.
        reader = new LLRPConnector(this, hostname);
          
        // Try connecting to the reader.
        try
        {
            System.out.println("Connecting to the reader.");
                ((LLRPConnector) reader).connect();
        }
        catch (LLRPConnectionAttemptFailedException e1)
        {
            e1.printStackTrace();
            System.exit(1);
        }
    }
      
    // Disconnect from the reader
    public void disconnect()
    {
        ((LLRPConnector) reader).disconnect();
    }
      
    // Connect to the reader, setup the ROSpec
    // and run it.
    public void run(String hostname)
    {
        connect(hostname);
        deleteROSpecs();
        addROSpec();
        enableROSpec();
        startROSpec();
        
        
        deleteAccessSpecs();
        addAccessSpec(READ_ACCESSSPEC_ID);
        enableAccessSpec(READ_ACCESSSPEC_ID);
        
        
    }
      
    // Cleanup. Delete all ROSpecs
    // and disconnect from the reader.
    public void stop()
    {
        deleteROSpecs();
        disconnect();
    }
    
 // Enable the AccessSpec
    public void enableAccessSpec(int accessSpecID)
    {
        ENABLE_ACCESSSPEC_RESPONSE response;
      
        System.out.println("Enabling the AccessSpec.");
        ENABLE_ACCESSSPEC enable = new ENABLE_ACCESSSPEC();
        enable.setAccessSpecID(new UnsignedInteger(accessSpecID));
        try
        {
            response = (ENABLE_ACCESSSPEC_RESPONSE)
            reader.transact(enable, TIMEOUT_MS);
            System.out.println(response.toXMLString());
        }
        catch (Exception e)
        {
            System.out.println("Error enabling AccessSpec.");
            e.printStackTrace();
        }
    }
      
    // Delete all AccessSpecs from the reader
    public void deleteAccessSpecs()
    {
        DELETE_ACCESSSPEC_RESPONSE response;
      
        System.out.println("Deleting all AccessSpecs.");
        DELETE_ACCESSSPEC del = new DELETE_ACCESSSPEC();
        // Use zero as the ROSpec ID.
        // This means delete all AccessSpecs.
        del.setAccessSpecID(new UnsignedInteger(0));
        try
        {
            response = (DELETE_ACCESSSPEC_RESPONSE)
            reader.transact(del, TIMEOUT_MS);
            System.out.println(response.toXMLString());
        }
        catch (Exception e)
        {
            System.out.println("Error deleting AccessSpec.");
            e.printStackTrace();
        }
    }
      
    // Create a OpSpec that reads from user memory
    public C1G2Read buildReadOpSpec()
    {
        // Create a new OpSpec.
        // This specifies what operation we want to perform on the
        // tags that match the specifications above.
        // In this case, we want to read from the tag.
        C1G2Read opSpec = new C1G2Read();
        // Set the OpSpecID to a unique number.
        opSpec.setOpSpecID(new UnsignedShort(READ_OPSPEC_ID));
        opSpec.setAccessPassword(new UnsignedInteger(0));
        // For this demo, we'll read from user memory (bank 3).
        TwoBitField opMemBank = new TwoBitField();
        // Set bits 0 and 1 (bank 3 in binary).
        opMemBank.clear(0);
        opMemBank.set(1);
        opSpec.setMB(opMemBank);
        // We'll read from the base of this memory bank (0x00).
        opSpec.setWordPointer(new UnsignedShort(0x00));
        // Read two words.
        opSpec.setWordCount(new UnsignedShort(6));
        return opSpec;
    }
      
    // Create a OpSpec that writes to user memory
    public C1G2Write buildWriteOpSpec()
    {
        // Create a new OpSpec.
        // This specifies what operation we want to perform on the
        // tags that match the specifications above.
        // In this case, we want to write to the tag.
        C1G2Write opSpec = new C1G2Write();
        // Set the OpSpecID to a unique number.
        opSpec.setOpSpecID(new UnsignedShort(WRITE_OPSPEC_ID));
        opSpec.setAccessPassword(new UnsignedInteger(0));
        // For this demo, we'll write to user memory (bank 3).
        TwoBitField opMemBank = new TwoBitField();
        // Set bits 0 and 1 (bank 3 in binary).
        opMemBank.set(0);
        opMemBank.set(1);
        opSpec.setMB(opMemBank);
        // We'll write to the base of this memory bank (0x00).
        opSpec.setWordPointer(new UnsignedShort(0x00));
        UnsignedShortArray_HEX writeData =
        new UnsignedShortArray_HEX();
        // We'll write 8 bytes or two words.
        writeData.add(new UnsignedShort (0xAABB));
        writeData.add(new UnsignedShort (0xCCDD));
        opSpec.setWriteData(writeData);
      
        return opSpec;
    }
      
    // Create an AccessSpec.
    // It will contain our two OpSpecs (read and write).
    public AccessSpec buildAccessSpec(int accessSpecID)
    {
        System.out.println("Building the AccessSpec.");
      
        AccessSpec accessSpec = new AccessSpec();
      
        accessSpec.setAccessSpecID(new UnsignedInteger(accessSpecID));
      
        // Set ROSpec ID to zero.
        // This means that the AccessSpec will apply to all ROSpecs.
        accessSpec.setROSpecID(new UnsignedInteger(0));
        // Antenna ID of zero means all antennas.
        accessSpec.setAntennaID(new UnsignedShort(0));
        accessSpec.setProtocolID(
            new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));
        // AccessSpecs must be disabled when you add them.
        accessSpec.setCurrentState(
            new AccessSpecState(AccessSpecState.Disabled));
        AccessSpecStopTrigger stopTrigger = new AccessSpecStopTrigger();
        // Stop after the operating has been performed a certain number of times.
        // That number is specified by the Operation_Count parameter.
        stopTrigger.setAccessSpecStopTrigger
            (new AccessSpecStopTriggerType(
            AccessSpecStopTriggerType.Operation_Count));
        // OperationCountValue indicate the number of times this Spec is
        // executed before it is deleted. If set to 0, this is equivalent
        // to no stop trigger defined.
        stopTrigger.setOperationCountValue(new UnsignedShort(0));
        accessSpec.setAccessSpecStopTrigger(stopTrigger);
      
        // Create a new AccessCommand.
        // We use this to specify which tags we want to operate on.
        AccessCommand accessCommand = new AccessCommand();
      
        // Create a new tag spec.
        C1G2TagSpec tagSpec = new C1G2TagSpec();
        C1G2TargetTag targetTag = new C1G2TargetTag();
        targetTag.setMatch(new Bit(1));
        // We want to check memory bank 1 (the EPC memory bank).
        TwoBitField memBank = new TwoBitField();
        // Clear bit 0 and set bit 1 (bank 1 in binary).
        
       /**
        * TODO 
        */
       
        memBank.clear(0);
        memBank.set(1);
       
        
        targetTag.setMB(memBank);
        // The EPC data starts at offset 0x20.
        // Start reading or writing from there.
        targetTag.setPointer(new UnsignedShort(0x20));
        
        // This is the mask we'll use to compare the EPC.
        // We want to match all bits of the EPC, so all mask bits are set.
        //BitArray_HEX tagMask = new BitArray_HEX(TAG_MASK);
        BitArray_HEX tagMask = new BitArray_HEX();
        targetTag.setTagMask(tagMask);
        
        // We only only to operate on tags with this EPC.
        //BitArray_HEX tagData = new BitArray_HEX(TARGET_EPC);
        BitArray_HEX tagData = new BitArray_HEX();
        targetTag.setTagData(tagData);
      
        // Add a list of target tags to the tag spec.
        List <C1G2TargetTag> targetTagList =
            new ArrayList<C1G2TargetTag>();
        targetTagList.add(targetTag);
        tagSpec.setC1G2TargetTagList(targetTagList);
      
        // Add the tag spec to the access command.
        accessCommand.setAirProtocolTagSpec(tagSpec);
      
        // A list to hold the op specs for this access command.
        List <AccessCommandOpSpec> opSpecList =
            new ArrayList<AccessCommandOpSpec>();
      
        // Are we reading or writing to the tag?
        // Add the appropriate op spec to the op spec list.
        if (accessSpecID == WRITE_ACCESSSPEC_ID)
        {
            opSpecList.add(buildWriteOpSpec());
        }
        else
        {
            opSpecList.add(buildReadOpSpec());
        }
      
        accessCommand.setAccessCommandOpSpecList(opSpecList);
      
        // Add access command to access spec.
        accessSpec.setAccessCommand(accessCommand);
      
        // Add an AccessReportSpec.
        // We want to get notification when the operation occurs.
        // Tell the reader to sent it to us with the ROSpec.
        AccessReportSpec reportSpec = new AccessReportSpec();
        reportSpec.setAccessReportTrigger
            (new AccessReportTriggerType(
            AccessReportTriggerType.Whenever_ROReport_Is_Generated));
      
        return accessSpec;
    }
      
    // Add the AccessSpec to the reader.
    public void addAccessSpec(int accessSpecID)
    {
        ADD_ACCESSSPEC_RESPONSE response;
      
        AccessSpec accessSpec = buildAccessSpec(accessSpecID);
        System.out.println("Adding the AccessSpec.");
        try
        {
            ADD_ACCESSSPEC accessSpecMsg = new ADD_ACCESSSPEC();
            accessSpecMsg.setAccessSpec(accessSpec);
            response = (ADD_ACCESSSPEC_RESPONSE)
                reader.transact(accessSpecMsg, TIMEOUT_MS);
            System.out.println(response.toXMLString());
      
            // Check if the we successfully added the AccessSpec.
            StatusCode status = response.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode("M_Success")))
            {
                System.out.println("Successfully added AccessSpec.");
            }
            else
            {
                System.out.println("Error adding AccessSpec.");
                System.exit(1);
            }
        }
        catch (Exception e)
        {
            System.out.println("Error adding AccessSpec.");
            e.printStackTrace();
        }
    }
}