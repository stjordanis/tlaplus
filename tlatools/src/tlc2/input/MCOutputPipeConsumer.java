package tlc2.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import tlc2.output.EC;
import tlc2.output.MP;
import util.TLAConstants;

/**
 * This class exists due to a request for the TraceExplorer CL application to be able to consume the output from TLC
 * 	piped into TraceExplorer.  To that extent, this class will:
 * 
 * 	. read from a stream (for piping, this is System.in)
 *	. ensure that it is reading tool messages (first line received will be a message of type EC.TLC_VERSION)
 *			and fail appropriately if not
 *	. look for the first TLAConstants.LoggingAtoms.PARSING_FILE line in order to derive spec name and source directory
 *	. continue to read and wait and read and ... until an EC.TLC_FINISHED message is received - writing the content
 *			to a temp file.
 */
public class MCOutputPipeConsumer extends AbstractMCOutputConsumer {
	public interface ConsumerLifespanListener {
		void consumptionFoundSourceDirectoryAndSpecName(final MCOutputPipeConsumer consumer);
	}
	
	private String specName;
	private File sourceDirectory;
	
	private MCOutputMessage tlcVersionMessage;

	private final InputStream sourceStream;
	private final ConsumerLifespanListener listener;
	
	/**
	 * @param inputStream the stream containing the tool output
	 */
	public MCOutputPipeConsumer(final InputStream inputStream, final ConsumerLifespanListener lifespanListener) {
		sourceStream = inputStream;
		listener = lifespanListener;
	}
	
	/**
	 * This will not return until all output has been read and the output has correctly ended (or if there is a read
	 * 	error, or if the output is not proper tool message output.)
	 * 
	 * @param returnAllMessages if true, all consumed messages will be returned
	 * @return null or a {@link List} of {@link MCOutputMessage} instances of all messages consumed
	 * @throws Exception
	 */
	public List<MCOutputMessage> consumeOutput(final boolean returnAllMessages) throws IOException {
		final ArrayList<MCOutputMessage> encounteredMessages = returnAllMessages ? new ArrayList<>() : null;
		
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(sourceStream))) {
			MCOutputMessage message;

			while ((message = parseChunk(br)) != null) {
				if (returnAllMessages) {
					encounteredMessages.add(message);
				}

				if (message.getType() == MP.ERROR) {
					consumeErrorMessageAndStates(br, message);
				} else if (message.getCode() == EC.TLC_VERSION) {
					tlcVersionMessage = message;
				} else if (message.getCode() == EC.TLC_FINISHED) {
					break;
				}
			}
		} catch (final IOException ioe) {
			if (outputHadNoToolMessages()) {
				// Either we threw this from handleUnknownReadLine(String), or the output was abortive.
				return encounteredMessages;
			}
			
			throw ioe;
		}
		
		return encounteredMessages;
	}
	
	public boolean outputHadNoToolMessages() {
		return (tlcVersionMessage == null);
	}
	
	public String getSpecName() {
		return specName;
	}
	
	public File getSourceDirectory() {
		return sourceDirectory;
	}
	
	@Override
	protected void handleUnknownReadLine(final String line) throws IOException {
		if ((specName == null) && line.startsWith(TLAConstants.LoggingAtoms.PARSING_FILE)) {
			if (tlcVersionMessage == null) {
				throw new IOException("Output does not appear to be generated by TLC run with the '-tool' flag.");
			}
			
			final String wholePath = line.substring(TLAConstants.LoggingAtoms.PARSING_FILE.length() + 1);
			final File tlaFile = new File(wholePath);
			
			sourceDirectory = tlaFile.getParentFile();
			
			final String tlaFilename = tlaFile.getName();
			final int extensionDotIndex = tlaFilename.lastIndexOf('.');
			specName = tlaFilename.substring(0, extensionDotIndex);
			
			if (listener != null) {
				listener.consumptionFoundSourceDirectoryAndSpecName(this);
			}
		}
	}	
}
