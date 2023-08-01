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

/**
 * 
 * @author dp104662
 *
 */
public class ExamNetworkCommand extends AbstractCommand implements Command{
	private static final Logger LOG = Logger.getLogger(ExamNetworkCommand.class
			.getName());
	public ExamNetworkCommand() {
		super(NetworkTransferConstants.ARCHIVE_COMMAND_EXAM);
	}
	@Override
	public void execute(String... args) throws ExecutionException {
		DMSession jobNWSes = DBSession.getInstance().getRemoteDBSession(args[0], NetworkTransferConstants.REMOTE_ARCHIVES);	
		DMObject dmObject = QueryLocalDB.getInstance().queryExam(args[1]);
		if(dmObject!=null){
			jobNWSes.save(dmObject);
		}else{
			LOG.severe(NetworkTransferConstants.ARCHIVE_COMMAND_EXAM + ": is not able to save as queryExams return null");
		}
	}


}
