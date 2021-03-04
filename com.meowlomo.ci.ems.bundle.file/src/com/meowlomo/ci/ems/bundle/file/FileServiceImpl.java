package com.meowlomo.ci.ems.bundle.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.IFileService;
import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;

public class FileServiceImpl implements IFileService {

	private static Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
	
	private boolean inited = false;
	private SMBFileService fileService = null;
		
	String hostname = "";
	String username = "";
	String password = "";
	String domain = "";
	String fileServicePrefix = "";
	String sharename = "";
	
	@Override
    public boolean inited(){
    	return inited;
    }
	
	@Override
    public boolean init(String hostname, String username, String password, String serverDomain, String sharename){
    	if (inited) fileService.close();
    	
    	fileService = new SMBFileService(hostname, username, password, serverDomain, sharename);
    	inited = fileService.inited();
        return inited;
    }

	@Override
	public boolean create(Path path, byte[] fileContent, boolean overwrite) {
		return inited ? fileService.create(path, fileContent, overwrite) : false;
	}

	@Override
	public boolean appendToFile(Path path, byte[] fileContent) {
		return inited ? fileService.appendToFile(path, fileContent) : false;
	}

	@Override
	public byte[] readFile(Path path) {
		return inited ? fileService.readFile(path) : null;
	}

	@Override
	public boolean createDirectory(Path path) {
		return inited ? fileService.createDirectory(path) : false;
	}

	@Override
	public boolean isReadable(Path path) {
		return inited ? fileService.isReadable(path) : false;
	}

	@Override
	public boolean isWritable(Path path) {
		return inited ? fileService.isWritable(path) : false;
	}

	@Override
	public boolean exist(Path path) {
		return inited ? fileService.exist(path) : false;
	}

	@Override
	public long getFreeSpace() {
		return inited ? fileService.getFreeSpace() : 0;
	}

	@Override
	public boolean rename(Path originalPath, String newname, boolean overwrite) {
		return inited ? fileService.rename(originalPath, newname, overwrite) : false;
	}

	@Override
	public boolean delete(Path path, boolean recursive) {
		return inited ? fileService.delete(path, recursive) : false;
	}

	@Override
	public List<Path> listFile(Path path, String searchPattern) {
		return inited ? fileService.listFile(path, searchPattern) : null;
	}

	@Override
	public boolean isFile(Path path) {
		return inited ? fileService.isFile(path) : false;
	}

	@Override
	public boolean folderExists(Path path) {
		return inited ? fileService.folderExists(path) : false;
	}

	@Override
	public boolean fileExists(Path path) {
		return inited ? fileService.fileExists(path) : false;
	}

	@Override
	public boolean accessiable() {
		return inited ? fileService.accessiable() : false;
	}


	@Override
	public String remoteFileServer(String info) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean clearState() {
		//TODO
		if (inited) fileService.close();
		
		inited = false;
		fileService = null;
		hostname = "";
		username = "";
		password = "";
		domain = "";
		
		//TODO for copy execution.log, while the path is destroyed.
//		fileServicePrefix = "";
		sharename = "";
		FileUtilConstant.reset();
		return IFileService.super.clearState();
	}

	@Override
	public JSONObject attachData(String data) {
		// TODO Auto-generated method stub
		JSONObject param = IFileService.super.attachData(data);
		
		JSONObject parameters = param.getJSONObject("parameters");
		JSONObject remoteParameters = parameters.optJSONObject("remoteFileServer");
		System.err.println("FileService attachData:" + data);
		if (null != remoteParameters) {
			hostname = remoteParameters.getString("hostname").trim();
			username = remoteParameters.getString("username").trim();
			password = remoteParameters.getString("password").trim();
			domain = remoteParameters.optString("domain");	
			if (domain != null && domain.isEmpty()) // TODO
				domain = null;
			else
				domain = domain.trim();
			
			fileServicePrefix = remoteParameters.getString("path").trim();
			if (null != fileServicePrefix && !fileServicePrefix.isEmpty()) {
				if (fileServicePrefix.startsWith("\\") || fileServicePrefix.startsWith("/"))
					fileServicePrefix = fileServicePrefix.substring(1);
				if (fileServicePrefix.length() > 0
						&& !(fileServicePrefix.endsWith("\\") || fileServicePrefix.endsWith("/")))
					fileServicePrefix = fileServicePrefix + "/";
			}
			
			sharename = remoteParameters.getString("sharename").trim();
	
			logger.info(String.format("smb configuration: %s,%s,%s,%s,%s", hostname, username, password, domain,
					sharename));
			
			logger.error("FileService attachData init begin");
			logger.error("hostname:" + hostname);
			logger.error("username" + username);
			logger.error("password" + password);
			logger.error("domain" + domain);
			logger.error("sharename" + sharename);
			
			init(hostname, username, password, domain, sharename);
			logger.error("FileService attachData init end");
		}
		return param;
	}

	@Override
	public String remotePath(boolean bVideoFile) {
		String remotePath = fileServicePrefix + FileUtilConstant.REMOTE_TEST_CASE_RESULT_FOLDER;
		remotePath += bVideoFile ? "video/" : FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER;

		return remotePath;
	}

	@Override
	public String compress(String srcPath, String destPath) throws IOException {
		return ZipCompressUtil.compress(srcPath, destPath, false);
	}
}
