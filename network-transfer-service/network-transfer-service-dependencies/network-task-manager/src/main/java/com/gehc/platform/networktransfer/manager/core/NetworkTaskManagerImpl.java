package com.gehc.platform.networktransfer.manager.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ge.med.ct.wfplat.frameworks.ipc.AsynchronousIPCTransportHandler;
import com.ge.med.ct.wfplat.frameworks.ipc.listener.IPCListener;
import com.ge.med.ct.wfplat.system.rawimage.api.ImageStatusDomainObject;
import com.ge.med.ct.wfplat.system.systemifc.reconjobqueue.ReconJobQueue;
import com.ge.med.terra.tap.dm.DMJob;
import com.ge.med.terra.tap.dm.DMSession;
import com.gehc.platform.networktransfer.database.DBSession;
import com.gehc.platform.networktransfer.manager.api.NetworkTaskManager;
import com.gehc.platform.networktransfer.manager.api.TaskController;
import com.gehc.platform.networktransfer.manager.exception.NetworkTransferJobNotFoundException;
import com.gehc.platform.networktransfer.manager.exception.NetworkTransferJobStateNotSupportedException;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;
import com.gehc.platform.networktransferservice.model.JobKey;
import com.gehc.platform.networktransferservice.model.NetworkTransferJob;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.SeriesData;
import com.gehc.platform.networktransferservice.model.TransferState;

@Component
public class NetworkTaskManagerImpl implements NetworkTaskManager {

	@Autowired
	@Qualifier("ImageInstallReaderTransportHandler")
	private AsynchronousIPCTransportHandler imageInstalledTransportHandler;

	@Autowired
	@Qualifier("ReconJobQueueTransportrHandler")
	private AsynchronousIPCTransportHandler reconJobQueueTransportHandler;

	@Autowired
	private DDSCommunicationHandler ddsCommunicationHandler;

	@Autowired
	private TaskControllerFactory factory;
	private Map<String, TaskController> controllerMap = new ConcurrentHashMap<>();
	private DMSession dmSession;
	
	public NetworkTaskManagerImpl() {
		dmSession = DBSession.getInstance().getLocalDBSession();
	}

	@Override
	public void delete(String id) throws NetworkTransferJobNotFoundException {


	}

	@Override
	public NetworkTransferJob submit(NetworkTransferRequest req) {

		// 1. Cleanup existing controller for request if one exists
		if (controllerMap.containsKey(req.getJobKey().getId())) {
			TaskController oldController = controllerMap.get(req.getJobKey().getId());
			oldController.destroy();
			controllerMap.remove(req.getJobKey().getId());
		}
		
		// 2. Create new controller using factory class
		TaskController controller = factory.createController(req, 0);
		controllerMap.put(req.getJobKey().getId(), controller);

		// 3. Create new NetworkJobResponse
		NetworkTransferJob networkTransferJob = new NetworkTransferJob();
		networkTransferJob.setJobKey(req.getJobKey());
		networkTransferJob.setPercentComplete(BigDecimal.ZERO);
		networkTransferJob.setStatus(TransferState.PENDING);
		Iterator<SeriesData> it = req.getExamData().getSeriesData().iterator();
		networkTransferJob.setType(it.next().getType());

		// 4. Create AutoStoreSubmitResponseTaskDomainObject and publish to DDS
		ddsCommunicationHandler.submitResponse(networkTransferJob);
		
		// 5. Return object from step 3
		return networkTransferJob;

	}

	@Override
	public void updateState(String id, TransferState state) throws NetworkTransferJobStateNotSupportedException {


	}

	@Override
	public NetworkTransferJob find(String id) throws NetworkTransferJobNotFoundException {
		List<NetworkTransferJob> jobs = findAll();
		for (NetworkTransferJob job : jobs) {
			if (job.getJobKey().getId().equals(id)) {
				return job;
			}
		}
		
		throw new NetworkTransferJobNotFoundException("Could not find job with the ID " + id);
	}

	@Override
	public List<NetworkTransferJob> findAll() {
		List<NetworkTransferJob> results = new ArrayList<>();
		DMJob[] jobs = dmSession.getJobs();
		for (DMJob job : jobs) {
			NetworkTransferJob result = new NetworkTransferJob();
			JobKey jobKey = new JobKey();
			jobKey.setHostname(job.getProperty("target").toString());
			jobKey.setId("" + job.getJobHandle());
			jobKey.setSessionKey(job.getProperty("series_uid").toString());
			result.setJobKey(jobKey);
			result.setPercentComplete(BigDecimal.ZERO); // TODO - fix me with correct percentage calculation
			result.setStatus(NetworkTransferConstants.JOBSTATEMAP.get(Integer.parseInt(job.getProperty("job_status").toString())));
			result.setType(job.getProperty("content_level").toString());
			
			results.add(result);
		}
		
		return results;
	}

	@Override
	public void retry(String id) {
		// TODO Auto-generated method stub

	}

	private IPCListener<ImageStatusDomainObject> imageStatusListener = new IPCListener<ImageStatusDomainObject>() {

		@Override
		public void handleIPCEvent(ImageStatusDomainObject domainObject) {

		}
	};

	private IPCListener<ReconJobQueue> reconJobQueueListener = new IPCListener<ReconJobQueue>() {

		@Override
		public void handleIPCEvent(ReconJobQueue domainObject) {
			

		}
	};

	// https://gitlab-gxp.cloud.health.ge.com/CTSD/odplatform/-/blob/master/vobs/wfplat/system/autostoremanager/src/main/java/com/ge/med/ct/wfplat/system/autostoremanager/manager/NWTaskManagerImpl.java#L288

}
