/**
 * 
 */
package com.meowlomo.ci.ems.bundle.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.IFileUtil;
import com.meowlomo.ci.ems.bundle.utils.FileUtilConstant;

/**
 * @author tester
 *
 */
public class FileUtilImpl implements IFileUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtilImpl.class);
	
	private static String _baseDirectory = "";
	private static boolean _baseSetted = false;
	long instructionRunId;
	long instructionId;
	long logicalOrderIndex;
	
	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#registerWorkspace(java.lang.String)
	 */
	@Override
	public boolean registerWorkspace(String workspaceName){
		if (workspaceName.isEmpty() || _baseSetted) return false;
		
		//todo support other URI type
		File tmp = new File(workspaceName);
		if (tmp.exists()){
			if (tmp.isDirectory()){
				_baseDirectory = transferSplash(workspaceName);
				if (!_baseDirectory.endsWith("/"))
					_baseDirectory += "/";
				
				_baseSetted = true;
			}
		}
		return _baseSetted;
	}
	
	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#getFile(java.lang.String)
	 */
	@Override
	public File getFile(String fileName) {
		// TODO Auto-generated method stub
		File tmp = new File(getAbsolutePath(fileName));
		return tmp;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#newFile(java.lang.String)
	 */
	@Override
	public boolean newFile(String fileName) {
		// TODO Auto-generated method stub
		try{
			File tmp = new File(getAbsolutePath(fileName));
			if (!tmp.exists())
				return tmp.createNewFile();
			else if (tmp.isFile())
				return tmp.canWrite();
		}catch(Exception e){
			
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#canRead(java.lang.String)
	 */
	@Override
	public boolean canRead(String fileName) {
		// TODO Auto-generated method stub
		File tmp = new File(getAbsolutePath(fileName));
		if (!tmp.exists() || tmp.isDirectory())
			return false;
		else if (tmp.isFile())
			return tmp.canRead();
		return false;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#newDirectory(java.lang.String)
	 */
	@Override
	public boolean newDirectory(String path) {
		// TODO Auto-generated method stub
		if (!_baseSetted) return false;
		
		File tmp = new File(getAbsolutePath(path));
		if (tmp.exists())
			return true;
		
		return tmp.mkdirs();
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#getInputStream(java.lang.String)
	 */
	@Override
	public InputStream getInputStream(String fileName) throws FileNotFoundException{
		// TODO Auto-generated method stub
		File tmp = new File(getAbsolutePath(fileName));
		if (!tmp.exists())
			return null;
		
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(tmp);
		}catch(FileNotFoundException e){
			throw e;
		}
		
		return fis;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IFileUtil#getOutputStream(java.lang.String)
	 */
	@Override
	public OutputStream getOutputStream(String fileName) throws IOException{
		// TODO Auto-generated method stub
		File tmp = new File(getAbsolutePath(fileName));
		if (!tmp.exists())
		{
			try {
				tmp.createNewFile();
			} catch (IOException e) {
				throw e;
			}
		}
		else if (tmp.isDirectory()){
			throw new IOException("a directory given where we need a file.");
		}
		
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(tmp);
		}catch(FileNotFoundException e){
			throw e;
		}
		
		return fos;
	}

	private String transferSplash(String target){
		return target.replaceAll("\\\\", "/");
	}
	
	private String getAbsolutePath(String fileName){
		String subFileName = transferSplash(fileName);
		
		if (!_baseSetted) return subFileName;
	
		if (subFileName.startsWith("/")){
			subFileName = subFileName.substring(1);
		}
		return _baseDirectory + subFileName;
	}
	

	@Override
	public void attachInstructionRunData(String info) {
		JSONObject infoObj = new JSONObject(info);
		logger.info("[attachInstructionRunData]: {}", info);
		//参见vmc的Instruction类的asString的字段
		this.instructionRunId = infoObj.optLong("instructionRunId");
		this.instructionId = infoObj.optLong("id");
		FileUtilConstant.EXCEL_ROW_NUMBER = this.logicalOrderIndex = infoObj.optLong("index");
		
		if (0 == this.logicalOrderIndex)
			FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER = infoObj.optString("index") + "/";
		else
			FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER = logicalOrderIndex + "/";
		
		FileUtilConstant.remoteScreenShootFileName = String.format("_%s_%s_", infoObj.getString("target"), infoObj.getString("action"));
		
		//TODO
		FileUtilConstant.INSTRUCTION = infoObj.optString("elementId");
		if (0 == FileUtilConstant.EXCEL_ROW_NUMBER)
			FileUtilConstant.EXCEL_ROW_NUMBER_STRING = infoObj.optString("index");
		else
			FileUtilConstant.EXCEL_ROW_NUMBER_STRING = String.format("%1$04d", FileUtilConstant.EXCEL_ROW_NUMBER);
		
		//TODO delete
		System.err.println("IFileUtil attachInstructionRunData begin");
		System.err.println(FileUtilConstant.REMOTE_INSTRUCTION_RESULT_FOLDER);
		System.err.println(FileUtilConstant.remoteScreenShootFileName);
		System.err.println(FileUtilConstant.INSTRUCTION);
		System.err.println(FileUtilConstant.EXCEL_ROW_NUMBER);
		System.err.println(FileUtilConstant.EXCEL_ROW_NUMBER_STRING);
		System.err.println("IFileUtil attachInstructionRunData end");
	}
	
	@Override
	public JSONObject attachData(String data) {
		// TODO Auto-generated method stub
		JSONObject superObj = IFileUtil.super.attachData(data);
			
		System.err.println("[FileUtilImpl attachData]" + superObj.toString());
		
		// the path for the test case
		FileUtilConstant.TEST_CASE_RESULT_FOLDER = FileUtilConstant.genTestcaseResultFolder(superObj);
		FileUtilConstant.REMOTE_TEST_CASE_RESULT_FOLDER = localState.runId + "/";
		// final path for the log for this test case
		String logFolder = superObj.getString("logFolder");
		FileUtilConstant.LOG_FOLDER = logFolder + FileUtilConstant.TEST_CASE_RESULT_FOLDER;
		
		//TODO delete
		System.err.println("IFileUtil attachData begin");
		System.err.println(FileUtilConstant.TEST_CASE_RESULT_FOLDER);
		System.err.println(FileUtilConstant.REMOTE_TEST_CASE_RESULT_FOLDER);
		System.err.println(FileUtilConstant.LOG_FOLDER);
		System.err.println("IFileUtil attachData end");

		return superObj;
	}
	
//	public static void writeToFileServer(String screenShootNumber, File srcFile, boolean bVideoFile) throws IOException {
//		String fileName = bVideoFile ? srcFile.getName() : String.format("%04d%s%s.jpg", remoteScreenShootIndex, remoteScreenShootFileName,
//				ActionCommon.screenFileNameTail);
//		boolean bUseNewName = true;
//		if (bUseNewName) {
//			if (ActionCommon.instructionResultId > 0 && null != fileService && fileService.inited()
//					&& null != srcFile) {
//
//				// log/勿删-Andrew测试勿删除_/ ==> log/
//				String remotePath = ActionCommon.fileServicePrefix + ContextConstant.REMOTE_TEST_CASE_RESULT_FOLDER
//						+ ContextConstant.REMOTE_INSTRUCTION_RESULT_FOLDER;
//				if (bVideoFile)
//					remotePath = ActionCommon.fileServicePrefix + ContextConstant.REMOTE_TEST_CASE_RESULT_FOLDER + "video/";
//				
//				Path remotePathName = Paths.get(remotePath + fileName);
//				byte[] picContent = Files.readAllBytes(srcFile.toPath());
//
//				fileService.create(remotePathName, picContent, false);
//				byte[] content = fileService.exist(remotePathName) ? fileService.readFile(remotePathName) : null;
//
//				if (null != content && null != picContent && (content.length == picContent.length
//						|| Arrays.equals(picContent, content)
//						|| (content.length > 0.8 * picContent.length && content.length < 1.1 * picContent.length))) {
//					// back to atm
//					// report it.
//					if (null != ActionCommon.addStepFileLog && null != ActionCommon.httpUtil) {
//						String url = ActionCommon.addStepFileLog.optString("url");
//						String methodType = ActionCommon.addStepFileLog.optString("method");
//
//						JSONObject fileInfo = new JSONObject();
//						fileInfo.put("name", fileName + ".jpg");
//						fileInfo.put("uri", remotePathName.toString());
//						if (bVideoFile){
//							fileInfo.put("type", "Video");
//							fileInfo.put("runId", ActionCommon.runId);
//						}else{
//							fileInfo.put("type", "Screenshot");
//							fileInfo.put("instructionResultId", ActionCommon.instructionResultId);
//						}
//						JSONArray fileArray = new JSONArray();
//						fileArray.put(fileInfo);
//
//						ActionCommon.httpUtil.request(url, fileArray.toString(),
//								MethodType.valueOf(methodType.toUpperCase()));
//					} else
//						System.out.println("Report file to atm failed.");
//				} else {
//					System.out.println("Report file to atm failed.");
//				}
//			}
//		} else if (ActionCommon.instructionResultId > 0 && null != fileService && fileService.inited()
//				&& null != srcFile) {
//			String remotePath = ContextConstant.TEST_CASE_RESULT_FOLDER;
//			if (remotePath.startsWith("/")) {
//				remotePath = remotePath.substring(1);
//			} else if (remotePath.startsWith("\\")) {
//				remotePath = remotePath.substring(2);
//			}
//
//			Path path = Paths
//					.get(remotePath + screenShootNumber + fileName + "_" + System.currentTimeMillis() + ".jpg");
//			byte[] picContent = Files.readAllBytes(srcFile.toPath());
//
//			fileService.create(path, picContent, false);
//			byte[] content = fileService.exist(path) ? fileService.readFile(path) : null;
//
//			if (null != content && null != picContent && (content.length == picContent.length
//					|| Arrays.equals(picContent, content)
//					|| (content.length > 0.8 * picContent.length && content.length < 1.1 * picContent.length))) {
//				// back to atm
//				// report it.
//				if (null != ActionCommon.addStepFileLog && null != ActionCommon.httpUtil) {
//					String url = ActionCommon.addStepFileLog.optString("url");
//					String methodType = ActionCommon.addStepFileLog.optString("method");
//
//					JSONObject file = new JSONObject();
//					file.put("name", fileName + ".jpg");
//					file.put("uri", path.toString());
//					file.put("type", "Screenshot");
//					file.put("instructionResultId", ActionCommon.instructionResultId);
//					JSONArray fileArray = new JSONArray();
//					fileArray.put(file);
//
//					ActionCommon.httpUtil.request(url, fileArray.toString(),
//							MethodType.valueOf(methodType.toUpperCase()));
//				} else
//					System.out.println("Report file to atm failed.");
//			} else {
//				System.out.println("Report file to atm failed.");
//			}
//		}
//	}
}
