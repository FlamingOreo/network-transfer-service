package com.gehc.platform.networktransfer.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ge.med.ct.wfplat.utilities.ifc.command.AbstractCommand;
import com.ge.med.ct.wfplat.utilities.ifc.command.Command;
import com.ge.med.ct.wfplat.utilities.ifc.command.ExecutionException;
import com.ge.med.terra.tap.dm.DMObject;
import com.ge.med.terra.tap.dm.DMSession;
import com.gehc.platform.networktransfer.database.DBSession;
import com.gehc.platform.networktransfer.database.QueryLocalDB;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;

public class ImageNetworkCommand extends AbstractCommand implements Command{
	
	//Map of uniqueID and list of DMObjects
		private Map<String, List<DMObject>> compositeObjectsMap = new HashMap<String, List<DMObject>>();
		private static final Logger LOG = Logger.getLogger(ImageNetworkCommand.class
				.getName());
		
		public ImageNetworkCommand() {
			super(NetworkTransferConstants.NETWORK_COMMAND_IMAGE);
		}

		@Override
		public void execute(String... args) throws ExecutionException {
			String uniqueID = args[3];
			DMSession jobNWSes = DBSession.getInstance().getRemoteDBSession(args[0], NetworkTransferConstants.NETWORKS);
			
			if(!compositeObjectsMap.containsKey(uniqueID)) {
				compositeObjectsMap.put(uniqueID, new ArrayList<DMObject>());
			}
			
			try {
				// Allow for null imageID to trigger processing of existing images without adding an additional image.
				// Useful when series structure changes after images are processed.
				String imageID = args[1];
				if (imageID != null) {
					DMObject queryImage = QueryLocalDB.getInstance().queryImage(imageID);
					compositeObjectsMap.get(uniqueID).add(queryImage);
				}
			} finally {		
				if(args[2].equals("true")) {	
					if (args.length > 4 && args[4] != null && !args[4].isEmpty()) {
						LOG.severe("Sending image network job with " + args[4] +" priority");
						jobNWSes.send("JobParam;jobPriority=" + args[4]);
					} else{
						LOG.severe("Sending image network job without priority");
					}
					jobNWSes.save((DMObject[]) compositeObjectsMap.get(uniqueID).toArray(new DMObject[compositeObjectsMap.get(uniqueID).size()]));
					compositeObjectsMap.get(uniqueID).clear();
					compositeObjectsMap.remove(uniqueID);
				}
			}
		}


}
