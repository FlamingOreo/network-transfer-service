package com.gehc.platform.networktransfer.manager.api;

import java.util.List;

import com.gehc.platform.networktransfer.manager.exception.NetworkTransferJobNotFoundException;
import com.gehc.platform.networktransfer.manager.exception.NetworkTransferJobStateNotSupportedException;
import com.gehc.platform.networktransferservice.model.NetworkTransferJob;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.TransferState;

public interface NetworkTaskManager {

	/**
	 * Delete a network transfer job by ID
	 * 
	 * @param id the ID of the job to delete
	 */
	void delete(String id) throws NetworkTransferJobNotFoundException;

	/**
	 * Submit a new network transfer request
	 * 
	 * @param req the request to submit
	 */
	NetworkTransferJob submit(NetworkTransferRequest req);

	/**
	 * Update the state of a network transfer job
	 * 
	 * @param id    the id of the job to update
	 * @param state the state to set the job to
	 */
	void updateState(String id, TransferState state) throws NetworkTransferJobStateNotSupportedException;

	/**
	 * Retrieve a job by ID
	 * 
	 * @param id the id of the job to update
	 * @throws NetworkTransferJobNotFoundException
	 */
	NetworkTransferJob find(String id) throws NetworkTransferJobNotFoundException;

	/**
	 * Get all network transfer jobs
	 * 
	 * @return list of all network transfer jobs
	 */
	List<NetworkTransferJob> findAll();

	/**
	 * Re-try a given job by ID
	 * 
	 * @param id the id of the job to retry
	 */
	void retry(String id);
}
