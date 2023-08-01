package com.gehc.platform.networktransfer.manager.core;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.med.ct.wfplat.frameworks.ipc.AsynchronousIPCTransportHandler;
import com.ge.med.ct.wfplat.frameworks.ipc.exception.IPCException;
import com.gehc.platform.networktransferservice.model.NetworkTransferJob;
import com.gehc.platform.networktransferservice.model.TransferState;

/**
 * 
 * @author dp104662
 *
 */
@Component
public class DDSCommunicationHandler {
	private static final Logger LOG = Logger.getLogger(DDSCommunicationHandler.class.getName());
	private int numDDSSuccessReponse = 0;

	private enum DDSCommunicationState {
		UP, RESETTING, DOWN
	}

	private AtomicReference<DDSCommunicationState> currentState = new AtomicReference<DDSCommunicationState>(
			DDSCommunicationState.DOWN);
	private AsynchronousIPCTransportHandler networkTransferSubmitResponseWriterTransportHandler;

	/**
	 * Method to submit progress response for the transfer and submission job
	 * 
	 * @param response
	 */
	public void submitResponse(NetworkTransferJob response) {
		synchronized (this) {
			if (DDSCommunicationState.UP.equals(currentState.get())) {
				++numDDSSuccessReponse;
				try {
					networkTransferSubmitResponseWriterTransportHandler.send(response);
					TransferState transferStatus = response.getStatus();

					if (TransferState.COMPLETED.equals(transferStatus)) {
						networkTransferSubmitResponseWriterTransportHandler.unregisterInstance(response.getJobKey());
					}

					if (TransferState.COMPLETED.equals(transferStatus) || TransferState.FAILED.equals(transferStatus)) {
						LOG.severe("@@@TRACK == Number of DDS response== :" + numDDSSuccessReponse + " with status: "
								+ transferStatus);
					}
				} catch (IPCException e) {
					LOG.log(Level.SEVERE, e.getMessage(), e);
				}
			} else {
				LOG.log(Level.SEVERE, "@@@DDS communication is currently down");
			}
		}
	}

}
