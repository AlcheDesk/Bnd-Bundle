package com.meowlomo.ci.ems.bundle.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.http.client.config.RequestConfig;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil.CompositeRequestResult;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator.ValidateResult;
import com.meowlomo.ci.ems.bundle.interfaces.IWebDriver;

public class Activator extends BaseBundleActivator implements CommandProvider {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	private static RequestConfig requestConfig = null;

	public Activator() {
		super(logger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		setContext(context);
		context.registerService(CommandProvider.class.getName(), this, null);

		System.out.println("Hello World in Meowlomo Bundle Console!!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		stopMass(context);
		System.out.println("Goodbye World in Console!!");
	}

	public String getHelp() {
		// TODO Auto-generated method stub
		return "\tsay – say what you input\n";
	}

	private static String readFileContent(String filePath) throws IOException {
		// 对一串字符进行操作
		StringBuffer fileData = new StringBuffer();
		//
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[4096];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		// 缓冲区使用完必须关掉
		reader.close();
		return fileData.toString();
	}

	private void innerProcess(ServiceReference<?> serviceRef, String repoPath, String excelPath) {
		IWebDriver iwb = (IWebDriver) _context.getService(serviceRef);
		File repoFile = new File(repoPath);
		File excelFile = new File(excelPath);
		System.out.println(excelFile.exists());

		// JSONArray excelContent = null;
		JSONObject excelContent = null;
		boolean bUseInnerJSONString = true;
		String jsonTask = "";
		if (bUseInnerJSONString) {

			try {
				jsonTask = readFileContent("D:\\commandToBundle.txt");
				JSONObject tmp = new JSONObject(jsonTask);
				jsonTask = tmp.toString();
			} catch (IOException e) {
				jsonTask = "";
				e.printStackTrace();
			}
			if (jsonTask.isEmpty())
				return;
		}
		boolean bUseEvent = true;
		if (bUseEvent) {
			Dictionary<String, Object> msg = new Hashtable<String, Object>();
			msg.put("params", jsonTask);
			EventAdmin eventAdmin = getEventAdmin();
			Event reportGeneratedEvent = new Event("com/meowlomo/bundle/webdriver/dotest", msg);
			eventAdmin.postEvent(reportGeneratedEvent);
		} else {
			String strTestResult = iwb.doTestProcess(jsonTask);
			System.out.println(strTestResult);
		}
	}

	public void _t(CommandInterpreter ci) {
		
		Activator.getBundleActivator("com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator");
		BaseBundleActivator bba = Activator.getBundleActivator("com.meowlomo.ci.ems.bundle.curl");
	
		ISchemaValidator schemaValidator = BaseBundleActivator.getTheServiceObject("curl", ISchemaValidator.class);
		if (null == schemaValidator)
			schemaValidator = BaseBundleActivator.getTheServiceObject("httpclient", ISchemaValidator.class);
		if (null != schemaValidator) {
			try {
				String json = readFileContent("D://fstab-good.json");
				String schema = readFileContent("D://fstab.json");
				ValidateResult vr = schemaValidator.validateJSONSchema(json, schema);
	//				ValidateResult vr = ValidateResult.fromString(vrStr);
			System.out.println(vr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void _utf(CommandInterpreter ci) {
		IHttpUtil http = null;
		if (null == http) {
			try {
				BaseBundleActivator bba = Activator.getBundleActivator("com.meowlomo.ci.ems.bundle.curl");
				if (null == bba){
					bba = Activator.getBundleActivator("com.meowlomo.ci.ems.bundle.httpclient");
				}
				// TODO
				if (null != bba) {
					http = bba.getServiceObject(IHttpUtil.class);
				}
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		}
		if (null != http) {
			String url = "http://10.0.100.185:8080/EMS/rest/agent/llog";
			String params = "{\"中国人\":\"美国人\"}";

			IHttpUtil.MethodType methodType = IHttpUtil.MethodType.POST;
			http.request(url, params, methodType);
		}
	}

	public void _run(CommandInterpreter ci) {
		ServiceReference<?> serviceRef = _context.getServiceReference(IWebDriver.class.getName());
		if (null == serviceRef)
			System.out.println("null web driver object");
		else {
			innerProcess(serviceRef, "F:\\_testcase\\repoTable.xml", "F:\\_testcase\\testcaseTable.xlsm");
		}
	}

	public void _geek(CommandInterpreter ci) {
		String command = ci.nextArgument();
		ServiceReference<?> serviceRef = _context.getServiceReference(IHttpUtil.class.getName());
		if (null == serviceRef)
			System.out.println("null http util object");
		else {
			IHttpUtil iHu = (IHttpUtil) _context.getService(serviceRef);
			
			JSONObject paramsHeadersObj = new JSONObject();
			paramsHeadersObj.put("Cache-Control", "no-cache");
			paramsHeadersObj.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)");
			paramsHeadersObj.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
			JSONArray paramsHeader = new JSONArray();
			paramsHeader.put(paramsHeadersObj);

			JSONObject paramsBody = new JSONObject();
			JSONArray paramsBodyArray = new JSONArray();
	
			if (command.equalsIgnoreCase("get")){
				paramsBody.put("ids", "1,2,3,4");
				paramsBodyArray.put(paramsBody);
				System.out.println(paramsBodyArray.toString());
				
				String url = "http://10.0.100.211:8080/api/cq?ids=1,2,3,4";
				String result = iHu.request(url, "", IHttpUtil.MethodType.GET);System.out.println(result);
//			    String result2 = iHu.requestHeader(url, paramsHeadersObj.toString(), paramsBodyArray.toString(), IHttpUtil.MethodType.GET);System.out.println(result2);
			}else if (command.equalsIgnoreCase("post")){
				paramsHeadersObj.put("Content-Type", "application/json");
				
				paramsBodyArray.put(UUID.randomUUID().toString());
				paramsBodyArray.put(UUID.randomUUID().toString());
				System.out.println(paramsBodyArray.toString());
				String url = "http://10.0.100.211:8080/api/cq/1/tasks";
				System.out.println(paramsHeadersObj.toString());
				CompositeRequestResult result2 = iHu.requestHeader(url, "{\"Content-Type\":\"application/json\"}", paramsBodyArray.toString(), IHttpUtil.MethodType.POST);
//				CompositeRequestResult result2 = CompositeRequestResult.fromString(result2Str);
				System.out.println(result2);
				
			}else if (command.equalsIgnoreCase("getcookie")){		
				String url = "http://atm-mid.oo/api/test/amOK";
				CompositeRequestResult result = iHu.requestHeader(url, "{}", "", IHttpUtil.MethodType.GET);
//				CompositeRequestResult result = CompositeRequestResult.fromString(resultStr);
				System.out.println(result);
//			    String result2 = iHu.requestHeader(url, paramsHeadersObj.toString(), paramsBodyArray.toString(), IHttpUtil.MethodType.GET);System.out.println(result2);
			}else if (command.equalsIgnoreCase("img")){
				String url = "https://cn.bing.com/az/hprichbg/rb/XmasTreeRoad_ZH-CN11556502034_1920x1080.jpg";
				CompositeRequestResult result = iHu.requestHeader(url, "{}", "", IHttpUtil.MethodType.GET);
//				CompositeRequestResult result = CompositeRequestResult.fromString(resultStr);
				System.out.println(result);
				
			}else if (command.equalsIgnoreCase("bd")){
				JSONArray codesArr = new JSONArray();
				codesArr.put(2013);
				codesArr.put("3410");
				
				System.out.println(String.format("%s is array", codesArr));
				System.out.println(String.join(",", codesArr.toList().toArray(new String[0])));
				
				String url = "http://www.baidu.com";
				CompositeRequestResult result = iHu.requestHeader(url, paramsHeadersObj.toString(), "", IHttpUtil.MethodType.GET);
//				CompositeRequestResult result = CompositeRequestResult.fromString(resultStr);
				System.out.println(result);
				System.out.println(result.contentHeader);
			}
		}
	}
	
	public void _go(CommandInterpreter ci) {
		// String command = ci.nextArgument();
		ServiceReference<?> serviceRef = _context.getServiceReference(IWebDriver.class.getName());
		if (null == serviceRef)
			System.out.println("null web driver object");
		else {
			innerProcess(serviceRef, "F:\\_testcase\\repo1.xml", "F:\\_testcase\\testcase1.xlsm");
		}
	}

	private void runDB() {
//		IDataSource ds = null;
//		logger.info("this is my host.");
//		try {
//			ds = getServiceObject(IDataSource.class);
//			EventAdmin eventAdmin = getEventAdmin();
//			if (null != eventAdmin) {
//				URL fileURL = FileLocator.toFileURL(getContext().getBundle().getEntry("dbconfig.xml"));
//				logger.info(getContext().getBundle().getEntry("dbconfig.xml").getFile());
//
//				Dictionary<String, Object> msg = new Hashtable<String, Object>();
//				URL url2 = getContext().getBundle().getEntry("dbconfig.xml");
//				String urlPath = getContext().getBundle().getResource("/").getPath();
//				// msg.put("path",
//				// "C:\\workspace\\com.meowlomo.ci.beavor.bundle.db\\dbconfig.xml");
//
//				// URI uri = new URL(StringUtil.encode(fileURL.toString(),
//				// "UTF-8")).toURI();
//				msg.put("path", "/dbconfig.xml");
//				Event reportGeneratedEvent = new Event("com/meowlomo/bundle/db/init", msg);
//				eventAdmin.postEvent(reportGeneratedEvent);
//				return;
//			}
//
//			if (!ds.inited()) {
//				// URL url1 =
//				// Activator.class.getResource("META-INF/MANIFEST.MF");
//				// URL url2 = Activator.class.getResource("dbconfig.xml");
//				// File dataFile =
//				// getContext().getBundle().getDataFile("utils.ssa");
//				// String dataFileStr = dataFile.getAbsolutePath();
//				//
//				// String url1 =
//				// getContext().getBundle().getEntry("META-INF/MANIFEST.MF").toURI().getPath();
//				// URL url2 = getContext().getBundle().getEntry("dbconfig.xml");
//				// String urlPath =
//				// getContext().getBundle().getResource("/").getPath();
//				//
//				// logger.info(Thread.currentThread().getContextClassLoader().getResource("").getPath());
//				// logger.info(url1.toString());
//				// logger.info(url2.toURI().toString());
//				// logger.info(url2.toString());
//				//
//				//// File tttt = new File(url2.toURI());
//				// File ttwf2 = new File("C:/Program
//				// Files/eclipse/../../workspace/com.meowlomo.ci.beavor.bundle.usedb/dbconfig.xml");
//				//
//				// URL fileURL =
//				// FileLocator.toFileURL(getContext().getBundle().getEntry("dbconfig.xml"));
//				// logger.info(fileURL.toString());
//				//
//				//// File ttwf2232 = new File(encode(fileURL.toString(),
//				// "UTF-8"));
//				// InputStream is = new URL(encode(fileURL.toString(),
//				// "UTF-8")).openStream();
//				// printFileContent(is);
//				//
//				// File tmpFile = new File(fileURL.toURI());
//				// logger.info(fileURL.toURI().getPath().toString());
//				// logger.info(FileLocator.toFileURL(fileURL).getPath().toString());
//				// URI uri = FileLocator.toFileURL(fileURL).toURI();
//				// logger.info(uri.toString());
//
//				URL fileURL = FileLocator.toFileURL(getContext().getBundle().getEntry("dbconfig.xml"));
//				logger.info(getContext().getBundle().getEntry("dbconfig.xml").getFile());
//				logger.info(fileURL.toString());
//				// InputStream is = new
//				// URL(StringUtil.encode(fileURL.toString(),
//				// "UTF-8")).openStream();
//				// URI uri = new URL(StringUtil.encode(fileURL.toString(),
//				// "UTF-8")).toURI();
//				// ds.init(uri);
//				// ds.init("dbconfig.xml");
//				// ds.init("C:/workspace/com.meowlomo.ci.beavor.bundle.usedb/dbconfig.xml");
//				// ds.init("C:\\workspace\\com.meowlomo.ci.beavor.bundle.db\\dbconfig.xml");
//				ds.init("D:\\workspace\\eclipse\\cq.bundle.console\\dbconfig.xml");
//			}
//			if (ds.inited()) {
//				DataSource dss = ds.getDataSource("oracleAoTain");
//				logger.info("get oracle ok.{}", dss.toString());
//			}
//		} catch (InstantiationException | IllegalAccessException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		logger.info(ds.toString());
	}

	public void _runDB(CommandInterpreter ci) {
		runDB();
	}

	// demonstrate how to use the db bundle
	public void _useDB(CommandInterpreter ci) {
		IDataSource ds = null;
		logger.info("this is my host.");
		Connection con = null;
		try {
			String dbName = ci.nextArgument();
			ds = getServiceObject(IDataSource.class);

			String sql = "";
			if (dbName.equalsIgnoreCase("mysql1"))// "tencent_vps_mysql";
				sql = "select count(1) as rows from bundle.logging_event;";
			else if (dbName.equalsIgnoreCase("postgresql1"))
				sql = "select count(1) as totalemployee, sum(salary) as rows from cqtest;";
			// else if (dbName.equalsIgnoreCase("oracle1"))
			// sql = "SELECT COUNT(ARCHITECTURE) AS rows FROM TEM_WORKERS";
			else if (dbName.equalsIgnoreCase("sqlserver1"))
				sql = "select count(distinct(LastName)) as rows from testdb.dbo.Persons;";
			else if (dbName.equalsIgnoreCase("oracleAoTain"))
				// sql = "insert into DAMS_AT_TTSS (name,address,code)
				// values('andrew', '深圳', 1234)";
				sql = "insert all into PUSH_PLATFORM.DOWN_ICP_HCJG_JRHC (WZID,WZBAXH,YM,JXIP,BBISP,HCJG,HCSJ,JRID,ISPID,REMARK,DEAL_FLAG,STATUS,DEAL_TIME,DEAL_REMARK,SHENGID,SHIID,DEAL_USERID) values (2001,'meowlomo_JRHC8','www.meowlomo.com',3232260090,1060,1,'2017-10-26 14:30:30',1008,1060,'meowlomoTest',1,3,to_date('26-11月-17','DD-MON-RR'),'批量未处理',440000,445200,892) into PUSH_PLATFORM.DOWN_ICP_HCJG_JRHC (WZID,WZBAXH,YM,JXIP,BBISP,HCJG,HCSJ,JRID,ISPID,REMARK,DEAL_FLAG,STATUS,DEAL_TIME,DEAL_REMARK,SHENGID,SHIID,DEAL_USERID) values (2002,'meowlomo_JRHC8','www.meowlomo.com   ',3232260090,1060,1,'2017-10-26 14:30:30',1008,1060,'meowlomoTest',1,3,to_date('26-11月-17','DD-MON-RR'),'批量未处理',440000,445200,892) into PUSH_PLATFORM.DOWN_ICP_HCJG_JRHC (WZID,WZBAXH,YM,JXIP,BBISP,HCJG,HCSJ,JRID,ISPID,REMARK,DEAL_FLAG,STATUS,DEAL_TIME,DEAL_REMARK,SHENGID,SHIID,DEAL_USERID) values (2003,'meowlomo_JRHC8','www.meowlomo.com   ',3232260090,1060,1,'2017-10-26 14:30:30',1008,1060,'meowlomoTest',2,3,to_date('26-11月-17','DD-MON-RR'),'批量未处理',440000,445200,892) into PUSH_PLATFORM.DOWN_ICP_HCJG_JRHC (WZID,WZBAXH,YM,JXIP,BBISP,HCJG,HCSJ,JRID,ISPID,REMARK,DEAL_FLAG,STATUS,DEAL_TIME,DEAL_REMARK,SHENGID,SHIID,DEAL_USERID) values (2003,'meowlomo_JRHC8','www.meowlomo.com   ',3232260090,1060,1,'2017-10-26 14:30:30',1008,1060,'meowlomoTest',2,3,to_date('26-11月-17','DD-MON-RR'),'批量未处理',440000,445200,892) select 1 from dual";
			else if (dbName.equalsIgnoreCase("oracle1"))
				sql = String.format("Insert into AT_TEST (name,address,code) values ('andrew', '深圳', %d)",
						System.currentTimeMillis());
			if (sql.isEmpty()) {
				logger.info("db name doesn`t match.");
				return;
			}

			DataSource dsService = ds.getDataSource(dbName);
			con = dsService.getConnection();
			Statement st = con.createStatement();

			if (sql.toLowerCase().contains("insert")) {
				System.out.println(st.executeUpdate(sql));

			} else {
				ResultSet rs = st.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();

				int columnCount = rsmd.getColumnCount();
				boolean bInserted = rs.rowInserted();
				rs.last();
				int rowCount = rs.getRow();
				for (int i = 1; i <= columnCount; ++i) {
					logger.info(rsmd.getColumnName(i));
				}
				if (rs.next()) {
					// int rowcount = rs.getInt("arow");
					int rowcount = rs.getInt(1);
					logger.info(String.format("The row count of target is %d", rowcount));
				}
			}
		} catch (InstantiationException | IllegalAccessException | SQLException e) {
			logger.error("using db error:", e);
		} finally {
			if (null != con)
				try {
					con.close();
				} catch (SQLException e) {
					logger.error("close sql connection error:", e);
				}
		}
	}

	public EventAdmin getEventAdmin() {
		if (null == _context)
			return null;

		ServiceReference<EventAdmin> ref = _context.getServiceReference(EventAdmin.class);
		if (null == ref)
			return null;

		EventAdmin eventAdmin = _context.getService(ref);
		return eventAdmin;
	}
}
