/**
 * 
 */
package com.meowlomo.ci.ems.bundle.interfaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author 陈琪
 *
 */
public interface IFileUtil extends IBundleStateClearable, IBundleStepStateClearable{
	
	boolean registerWorkspace(String workspaceName);
	
	File getFile(String fileName);
	boolean newFile(String fileName);
	boolean canRead(String fileName);
	boolean newDirectory(String path);
	
	InputStream getInputStream(String fileName) throws FileNotFoundException;
	OutputStream getOutputStream(String fileName) throws IOException;
}
