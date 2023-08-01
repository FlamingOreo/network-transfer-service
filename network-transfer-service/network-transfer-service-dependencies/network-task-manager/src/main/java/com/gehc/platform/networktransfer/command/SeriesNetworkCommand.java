package com.gehc.platform.networktransfer.command;

import java.util.logging.Logger;

import com.ge.med.ct.wfplat.utilities.ifc.command.AbstractCommand;
import com.ge.med.ct.wfplat.utilities.ifc.command.Command;
import com.ge.med.ct.wfplat.utilities.ifc.command.ExecutionException;
import com.ge.med.terra.tap.dm.DMObject;
import com.ge.med.terra.tap.dm.DMSession;
import com.gehc.platform.networktransfer.database.DBSession;
import com.gehc.platform.networktransfer.database.QueryLocalDB;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;

public class SeriesNetworkCommand extends AbstractCommand implements Command{
	
	private static final Logger LOG = Logger.getLogger(SeriesNetworkCommand.class
			.getName());
	
	public SeriesNetworkCommand() {
		super(NetworkTransferConstants.NETWORK_COMMAND_SERIES);
	}

	@Override
	public void execute(String... args) throws ExecutionException {
		DMSession jobNWSes = DBSession.getInstance().getRemoteDBSession(args[0], NetworkTransferConstants.NETWORKS);	
		DMObject dmObject = QueryLocalDB.getInstance().querySeries(args[1]);
		if(dmObject!=null){
			if (args.length > 2 && args[2] != null && !args[2].isEmpty()) {
				LOG.severe("Sending series network job with " + args[2] +" priority");
				jobNWSes.send("JobParam;jobPriority=" + args[2]);
			} else {
				LOG.severe("Sending series network job without priority");
			}
			jobNWSes.save(dmObject);	
		} else {
			LOG.severe(NetworkTransferConstants.NETWORK_COMMAND_SERIES + ": is not able to save as querySeries return null");
		}
	}


}
