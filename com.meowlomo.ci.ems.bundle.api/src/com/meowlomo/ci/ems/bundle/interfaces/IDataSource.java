/**
 * 
 */
package com.meowlomo.ci.ems.bundle.interfaces;

import java.io.File;
import java.net.URI;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author 陈琪
 *
 */
public interface IDataSource extends IInstructionExecutable, IBundleStateClearable{
	public enum DBType{
		ORACEL,MSSERVER,MYSQL,POSTGRESQL
	}
	
	public boolean inited();
	public void init(String configPath);
	public void init(URI configUri);
	public void init(File file);
	public void innerInit();
	
	public void addDataSource(String host, String port, String user, String pwd, String databaseType, String name, String databaseName) throws MeowlomoBundleBaseException;
	public void addDataSource(String url, String user, String pwd, String className, String name) throws MeowlomoBundleBaseException;
	public void innerAddDataSource(String url, String user, String pwd, String className, String name);
	public void removeDataSource(String name);
	
	public Map<String, DataSource> getDataSourceList();
	public DataSource getDataSource(String dbName);
	public String getDataSourceInfo(String dbName);
}
