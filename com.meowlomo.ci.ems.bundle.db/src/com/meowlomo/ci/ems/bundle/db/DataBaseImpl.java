/**
 * 
 */
package com.meowlomo.ci.ems.bundle.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanIntrospector;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.XMLBuilderParameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.ObjectUtils.Null;
import org.json.JSONObject;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;
import com.meowlomo.ci.ems.bundle.interfaces.Instruction;
import com.meowlomo.ci.ems.bundle.utils.ContextConstant;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;
import com.meowlomo.ci.ems.bundle.utils.SGLogger;
import com.meowlomo.ci.ems.bundle.utils.StringUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author tester
 *
 */
public class DataBaseImpl implements IDataSource {
	private static final Logger logger = LoggerFactory.getLogger(DataBaseImpl.class);
	//TODO 拷贝代码的一致性
	
	private static Map<String, DataSource> _dataSources = null;
	private static Map<String, Dictionary<String, String>> _dataSourceProperties = null;
	private XMLConfiguration _xmlConfig;
	private String dsName;
	private static boolean _inited = false;
	
	static int maxPoolSize = 20;
	static int connectionTimeOut = 30000;
	static int idleTimeOut = 600000;
	static int maxLifeTime = 1800000;
	static boolean autoCommit = true;
	static String poolNamePrefix = "tem";
	
	static{
		_dataSources = new HashMap<String, DataSource>();
		_dataSourceProperties = new HashMap<String, Dictionary<String, String>>();
	}
	
	protected DataBaseImpl() {

	}
	
	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IDataSource#inited()
	 */
	@Override
	public boolean inited() {
		return _inited && _dataSources.size() > 0;
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IDataSource#init(java.lang.String)
	 */
	@Override
	public void init(String configPath) {
		cleartDataSource();
		if (configPath.toLowerCase().endsWith(".xml")){
			initDataSource(configPath);
		}
	}

	@Override
	public void init(URI configUri) {
		initDataSource(configUri);
	}
	
	@Override
	public void init(File file) {
		initDataSource(file);
	}

	/* (non-Javadoc)
	 * @see com.meowlomo.ci.beaver.bundle.interfaces.IDataSource#getDataSourceList()
	 */
	@Override
	public Map<String, DataSource> getDataSourceList() {
		return _dataSources;
	}
	
	@Override
	public DataSource getDataSource(String dbName) {
		if (null != _dataSources && _dataSources.containsKey(dbName)){
			return _dataSources.get(dbName);
		}else{
			return null;
		}
	}
	
	@Override
	public String getDataSourceInfo(String dbName) {
		String theDbName = dbName;
		DataSource db = getDataSource(dbName);
		Dictionary<String, String> properties = null;
		if (null != db) {
			properties = _dataSourceProperties.get(dbName);
		} else {
			try {
				JSONObject tmp = new JSONObject(dbName);
				String tmpDbName = tmp.getString("dsName");
				properties = _dataSourceProperties.get(tmpDbName);
				theDbName = tmpDbName;
			}catch (Exception e) {
				properties = null;
			}finally {
				if (null == properties && !StringUtil.nullOrEmpty(dsName)) {
					properties = _dataSourceProperties.get(dsName);
					theDbName = dsName;
				}
			}
		}
		
		// TODO 多db时如何返回 
		
		return null == properties ? null : genDataSourceInfo(theDbName, properties);
	}

	private String genDataSourceInfo(String dbName, Dictionary<String, String> properties) {
		JSONUtil.beginJSONObject("DataBaseImpl.getDataSourceInfo");
		JSONUtil.addJSONField("DataBaseImpl.getDataSourceInfo", String.format("数据库%s 驱动类名", dbName), properties.get("dataSourceClassName"));
		JSONUtil.addJSONField("DataBaseImpl.getDataSourceInfo", String.format("数据库%s 地址", dbName), properties.get("url"));
		JSONUtil.addJSONField("DataBaseImpl.getDataSourceInfo", String.format("数据库%s 用户名", dbName), properties.get("username"));
		JSONUtil.addJSONField("DataBaseImpl.getDataSourceInfo", String.format("数据库%s Element名", dbName), dbName);
		
		return JSONUtil.endJSONObject("DataBaseImpl.getDataSourceInfo", true);
	}

	private void initDataSource(URI xmlUri){
		cleartDataSource();
		File _file = new File(xmlUri);
		initDataSourceFromFile(_file, xmlUri.toString());
	}
	
	private void initDataSource(String xmlFilePath){
		cleartDataSource();
		File _file = new File(xmlFilePath);
		initDataSourceFromFile(_file, xmlFilePath);
	}
	
	private void initDataSource(File is){
		cleartDataSource();
		initDataSourceFromFile(is, "");
	}
	
	private void initDataSourceFromFile(File file, String xmlFilePath){
		InputStream is = null;
		try {
			logger.info("dbconfigfile" + (file.canRead() ? " can read !" : " can`t read !"));
			innerInitFromFile(file, xmlFilePath);
			
		} catch (Exception e) {
			logger.info("dbconfigfile exception occured! IDataSource interface should not be available.");
			e.printStackTrace();
			_inited = false;
		}
	}
	
	private void innerInitFromFile(File file, String xmlFilePath){
		try{
			logger.info("begin innerInitFromFile function ");
			Class biClass = BeanIntrospector.class;
			Class dbClass = DynaBean.class;
			
			Parameters params = new Parameters();
			logger.info("innerInitFromFile class OK. ");
			XMLBuilderParameters xmlBuilderParams = params.xml();
			xmlBuilderParams.setFile(file);
			xmlBuilderParams.setValidating(true);
			FileBasedConfigurationBuilder<XMLConfiguration> builder =
				    new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
				    .configure(xmlBuilderParams);
			
			FileHandler fh = builder.getFileHandler();
			fh.getClass().getName();
			_xmlConfig = builder.getConfiguration();
			if (null == _xmlConfig)
				throw new MeowlomoBundleDataBaseException(String.format("Can`t init DB config from file: %s", file.getName()));
			
			List<HierarchicalConfiguration<ImmutableNode>> subConfs = _xmlConfig.configurationsAt(String.format("database"));
			if (!subConfs.isEmpty()){
				for(HierarchicalConfiguration<ImmutableNode> subConf : subConfs){
					String dbName = subConf.getString("name");
					DataSource dbDS = buildDataSource(subConf);
					_dataSources.put(dbName, dbDS);
				}
				_inited = true;
			}
		} catch (Exception e) {
			logger.info("exception occured:{}", e);
			_inited = false;
		}
	}

	private void printInputStream(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();   
		String line = null;
		try{
			while((line = reader.readLine()) != null){
				logger.info(line);
				sb.append(line + "/n");
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try{
				is.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private DataSource buildDataSource(HierarchicalConfiguration<ImmutableNode> dbNode){
		List<fieldGetter> fields = new ArrayList<fieldGetter>();
		
		fields.add(new fieldGetter<String>("setDataSourceClassName", 1, "Data Source Class Name", "data_source_class_name", "getString"));
		fields.add(new fieldGetter<String>("addDataSourceProperty", 1, "JDBC URL", "url", "getString"));
		fields.add(new fieldGetter<String>("setUsername", 1, "username", "username", "getString"));
		fields.add(new fieldGetter<String>("setPassword", 1, "password", "password", "getString"));
		fields.add(new fieldGetter<Integer>("setMaximumPoolSize", 2, "max pool size", "max_pool_size", "getInt", 50));
		fields.add(new fieldGetter<Long>("setConnectionTimeout", 4, "connection timeout", "connection_timeout", "getLong", (long) 300000));
		fields.add(new fieldGetter<Long>("setIdleTimeout", 4, "idle timeout", "idle_timeout", "getLong", (long) 600000));
		fields.add(new fieldGetter<Long>("setMaxLifetime", 4, "max life time", "max_life_time", "getLong", (long) 1800000));
		fields.add(new fieldGetter<Boolean>("setAutoCommit", 3, "auto commit", "auto_commit", "getBoolean", true));
		fields.add(new fieldGetter<String>("setPoolName", 1, "pool name", "pool_name", "getString", "temConnectionPool"));
		
		HikariConfig hikariConfig = new HikariConfig();
		HikariDataSource ds = null;
		try {
			for(fieldGetter t : fields){
				t.doGet(dbNode);
				t.setDbConfigValue(hikariConfig);
			}
			ds = new HikariDataSource(hikariConfig);
		}catch(Exception e){
			logger.error("IDataSource init failed.", e);
			ds = null;
		}
		
		return ds;
    }

	private static void cleartDataSource(){
		_inited = false;
		Iterator<Entry<String,DataSource>> iter = _dataSources.entrySet().iterator(); 
		
		while (iter.hasNext()) {
			Entry<String,DataSource> entry = iter.next();
			DataSource ds = entry.getValue();
			if (ds instanceof HikariDataSource){
				HikariDataSource hkDs = (HikariDataSource)ds;
				System.err.println("DataSource [" + entry.getKey() + "] will be closed.Then remove it.");
				hkDs.close();
			}
		}
		_dataSources.clear();
	}

	public class fieldGetter<T>{
		static final String _staticPrefix = "Database config ";
		T _obj;
		String _configMethod = "";
		int _configMethodType = 1;//TODO 1 string,2 integer, 3 boolean,   ---- why just java call the "bool" with the name "boolean" or a wrapped class "Boolean" 
		String _loggerPrefix = "";
		String _field = "";
		String _methodName = "";
		T _configValue = null;
		T _defaultValue = null;
		
		
		public fieldGetter(String configMethod, int configMethodType, String loggerPrefix, String field, String methodName, T defaultValue){
			_configMethod = configMethod;
			_configMethodType = (configMethodType % 4);
			_loggerPrefix = loggerPrefix + ":";;
			_field = field;
			_methodName = methodName;
			_defaultValue = defaultValue;
		}
		
		public fieldGetter(String configMethod, int configMethodType, String loggerPrefix, String field, String methodName){
			_configMethod = configMethod;
			_configMethodType = (configMethodType % 4);
			_loggerPrefix = loggerPrefix + ":";
			_field = field;
			_methodName = methodName;
		}
		
		public Object getValue(HierarchicalConfiguration<ImmutableNode> subNode){
			switch(_methodName){
			case "getLong":
				return null == _defaultValue ? subNode.getInt(_field) : subNode.getLong(_field, (long)_defaultValue);
			case "getInt":
				return null == _defaultValue ? subNode.getInt(_field) : subNode.getInt(_field, (int)_defaultValue);
			case "getString":
				return null == _defaultValue ? subNode.getString(_field) : subNode.getString(_field, (String)_defaultValue);
			case "getBoolean":
				return null == _defaultValue ? subNode.getBoolean(_field) : subNode.getBoolean(_field, (boolean)_defaultValue);
			default:
				return null;
			}
		}
		
		public void setDbConfigValue(HikariConfig config) throws NoSuchMethodException, SecurityException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			Method configMethod = null;
			if ("addDataSourceProperty" == _configMethod){
				configMethod = config.getClass().getMethod(_configMethod, String.class, Object.class);
				configMethod.invoke(config, _field, _configValue);
			}else{
				configMethod = config.getClass().getMethod(_configMethod, getTheClass(_configMethodType));
				configMethod.invoke(config, _configValue);
			}
		}
	
		@SuppressWarnings("rawtypes")
		private Class getTheClass(int type){
			switch(type){
			case 1:
				return String.class;
			case 2:
				return int.class;
			case 3:
				return boolean.class;
			case 0:
			case 4:
				return long.class;
			default:
				return String.class;
			}
		}
		
		@SuppressWarnings("unchecked")
		public void doGet(HierarchicalConfiguration<ImmutableNode> subNode){
			_configValue = (T) getValue(subNode);
			logger.info(getLogInfo(subNode));
		}
		
		public String getLogInfo(HierarchicalConfiguration<ImmutableNode> subNode){
			return _staticPrefix + _loggerPrefix + getValue(subNode);
		}
	}

	@Override
	public void innerInit() {
		BaseBundleActivator bba = BaseBundleActivator.getBundleActivator("com.meowlomo.ci.ems.bundle.db");
		EventAdmin eventAdmin = bba.getEventAdmin();
		logger.info(" begin init db bundle by inner xml config.");
		if (null != eventAdmin) {
			logger.info(" begin init db bundle by using event admin service.");
			Dictionary<String, Object> msg = new Hashtable<String, Object>();
			Event reportGeneratedEvent = new Event("com/meowlomo/bundle/db/directinit", msg);
			eventAdmin.postEvent(reportGeneratedEvent);
			return;
		}else{
			logger.info(" init db bundle failed .beacause the event admin is null.");
		}
	}
	
	@Override
	public void innerAddDataSource(String url, String user, String pwd, String className, String name) {
		BaseBundleActivator bba = BaseBundleActivator.getBundleActivator("com.meowlomo.ci.ems.bundle.db");
		EventAdmin eventAdmin = bba.getEventAdmin();
		logger.info(" begin add datasource config :" + name + " directly.");
		if (null != eventAdmin) {
			Dictionary<String, String> msg = new Hashtable<String, String>();
			msg.put("dataSourceClassName", className);
			msg.put("url", url);
			msg.put("username", user);
			msg.put("password", pwd);
			msg.put("name", name);			
			Event reportGeneratedEvent = new Event("com/meowlomo/bundle/db/directadd", msg);
			
			logger.info("try to add datasource config :" + name + " directly. use event admin service and data is ready.");
			eventAdmin.postEvent(reportGeneratedEvent);
			_dataSourceProperties.put(name, msg);
		}else{
			logger.info(" init db bundle failed .beacause the event admin is null.");
		}
	}

	@Override
	public void addDataSource(String host, String port, String user, String pwd, String databaseType, String name, String databaseName) throws MeowlomoBundleDataBaseException {
		
		checkParams(host, port, user, pwd, databaseType, name, databaseName);
		removeSameNameDataSource(name);
		
		HikariConfig hikariConfig = new HikariConfig();
		
		setDataSourceProperty(hikariConfig, databaseType, databaseName, host, port);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(pwd);
		hikariConfig.setMaximumPoolSize(DataBaseImpl.maxPoolSize);
		hikariConfig.setConnectionTimeout(DataBaseImpl.connectionTimeOut);
		hikariConfig.setIdleTimeout(DataBaseImpl.idleTimeOut);
		hikariConfig.setMaxLifetime(DataBaseImpl.maxLifeTime);
		hikariConfig.setAutoCommit(DataBaseImpl.autoCommit);
		hikariConfig.setPoolName(DataBaseImpl.poolNamePrefix + name);
		
		HikariDataSource ds = new HikariDataSource(hikariConfig);
		_dataSources.put(name, ds);
		if (false == _inited){
			_inited = true;
		}
	}
	
	@Override
	public void addDataSource(String url, String user, String pwd, String className, String name) throws MeowlomoBundleDataBaseException {
		removeSameNameDataSource(name);
		HikariConfig hikariConfig = new HikariConfig();

		setDataSourceProperty(hikariConfig, url, className);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(pwd);
		hikariConfig.setMaximumPoolSize(DataBaseImpl.maxPoolSize);
		hikariConfig.setConnectionTimeout(DataBaseImpl.connectionTimeOut);
		hikariConfig.setIdleTimeout(DataBaseImpl.idleTimeOut);
		hikariConfig.setMaxLifetime(DataBaseImpl.maxLifeTime);
		hikariConfig.setAutoCommit(DataBaseImpl.autoCommit);
		hikariConfig.setPoolName(DataBaseImpl.poolNamePrefix + name);
		
		HikariDataSource ds = new HikariDataSource(hikariConfig);
		_dataSources.put(name, ds);
		if (false == _inited){
			_inited = true;
		}
	}

	private void removeSameNameDataSource(String name) {
		DataSource ds = _dataSources.remove(name);
		if (null != ds){
			if (ds instanceof HikariDataSource){
				System.err.println("DataSource [" + name + "] will be closed.");
				HikariDataSource hkDs = (HikariDataSource)ds;
				hkDs.close();
			}
		}
	}
	
	/**
	 * 
	 * @param host							ip或地址
	 * @param port							端口
	 * @param userName						数据库用户名
	 * @param password						数据库密码
	 * @param databaseProductionTypeName	数据库产品名称,如:mysql,oracle,sqlserver等
	 * @param dbConfigName					数据库配置名,后续使用配置时使用
	 * @param databaseName					数据库名,即与table对应的database的名字						
	 */
	private void checkParams(String host, String port, String userName, String password, String databaseProductionTypeName, String dbConfigName, String databaseName) throws MeowlomoBundleDataBaseException{
		if (StringUtil.nullOrEmpty(host))
			throw new MeowlomoBundleDBConfigEmptyException("数据库地址 为空");
		if (StringUtil.nullOrEmpty(port))
			throw new MeowlomoBundleDBConfigEmptyException("端口 为空");
		if (StringUtil.nullOrEmpty(userName))
			throw new MeowlomoBundleDBConfigEmptyException("用户名 为空");
		if (StringUtil.nullOrEmpty(password))
			throw new MeowlomoBundleDBConfigEmptyException("密码 为空");
		
		if (StringUtil.nullOrEmpty(databaseProductionTypeName))
			throw new MeowlomoBundleDBConfigEmptyException("数据库类型 为空");
		if (StringUtil.nullOrEmpty(dbConfigName))
			throw new MeowlomoBundleDBConfigEmptyException("数据库配置名 为空");
		if (StringUtil.nullOrEmpty(databaseName))
			throw new MeowlomoBundleDBConfigEmptyException("数据库名 为空");
		
		if (false == isHostReachable(host, 3))
			throw new MeowlomoBundleDBHostException("Host :" + host + " ip不通");
		
		if (false == isHostPortConnectable(host, Integer.valueOf(port), 3))
			throw new MeowlomoBundleDBHostException("Host :" + host + ",Port :" + port + " 端口不通");
	}
	
	private boolean isHostReachable(String host, int timeOut) {
        try {
            return InetAddress.getByName(host).isReachable(timeOut * 1000);
        } catch (UnknownHostException e) {
            
        } catch (IOException e) {
            
        }
        return false;
    }
	
	private boolean isHostPortConnectable(String host, int port, int timeOut) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeOut * 1000);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
	
	private void setDataSourceProperty(HikariConfig hikariConfig, String url, String className) throws MeowlomoBundleDBClassException{
		if (className.isEmpty() || url.isEmpty())
			throw new MeowlomoBundleDBClassException("Class name or Url is [empty]");
		
		hikariConfig.setDataSourceClassName(className);
		hikariConfig.addDataSourceProperty("url", url);
	}

	private void setDataSourceProperty(HikariConfig hikariConfig, String databaseType, String databaseName, String host, String port) throws MeowlomoBundleDBClassException{
		String type = databaseType.toLowerCase();
		String className = "";
		String url = "";
		switch(type){
		case "oracle":
			className = "oracle.jdbc.pool.OracleDataSource";
			url = "jdbc:oracle:thin:@%s:%s/%s";
			break;
		case "mysql":
			className = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
			url = "jdbc:mysql://%s:%s/%s?characterEncoding=UTF-8";
			break;
		case "postgresql":
			className = "org.postgresql.ds.PGSimpleDataSource";
			url = "jdbc:postgresql://%s:%s/%s";
			break;
		case "sqlserver":
			className = "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
			url = "jdbc:sqlserver://%s:%s;DatabaseName=%s";
			break;
		default:
			break;
		}
		
		if (StringUtil.nullOrEmpty(className))
			throw new MeowlomoBundleDBClassException("type:" + type + " is not supported. only [oracle, mysql, postgresql, sqlserver] is supported.");
		
		hikariConfig.setDataSourceClassName(className);
		
		url = String.format(url, host, port, databaseName);
		hikariConfig.addDataSourceProperty("url", url);
	}

	@Override
	public void removeDataSource(String name) {
		removeSameNameDataSource(name);
	}

	@Override
	public JSONObject attachData(String data) {
		try {
			JSONObject tmp = new JSONObject(data);
			if (tmp.has("dsName")) {
				dsName = tmp.getString("dsName");
			}
			return tmp;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String step(String instructionJson, List<String> paramsInOrOut) {
		if (false == JSONUtil.isJSONValid(instructionJson))
			return new ExecutionResult(false, "instruction数据的Json格式不正确:" + instructionJson).toString();

		Instruction instruction = Instruction.generate(instructionJson);
		//TODO,同样的语句也应该受option控制
		ExecutionResult er = InstructionOptions.instance().genFromOptionStr(instruction.getOptions());
		if (!er.bOK()) return er.toString();
		
		String dbName = instruction.getElementJson().getString("driverName");
		String sqlClause = InstructionOptions.instance().doWithAllTransfer(instruction.getInput());
		System.err.println("DataBaseImpl.step.sql:" + sqlClause);
		return sqlScriptExecute(dbName, sqlClause, InstructionOptions.instance()).toString();
	}
	
	/*
	 * 执行SQL语句
	 */
	public static ExecutionResult sqlScriptExecute(String dbName, String inputSql, InstructionOptions options) {

		BaseBundleActivator bba = Activator.getBundleActivator("com.meowlomo.ci.ems.bundle.db");
		String sql = inputSql.trim();
		IDataSource ds = null;
		Connection con = null;
		IHttpUtil httpUtil = IHttpUtil.getHttpTool();
		
		JSONObject updateParams = new JSONObject();
		updateParams.put("inputData", inputSql);

		if (null != bba) {
			DataSource targetDS = null;
			try {
				ds = BaseBundleActivator.getTheServiceObject("db", IDataSource.class);
				if (null != ds) {
					if (sql.endsWith(";"))
						sql = sql.substring(0, sql.length() - 1);
					targetDS = getTargetDataSource(dbName, bba, ds);

					if (null != targetDS) {
						con = targetDS.getConnection();
						// multi statement
						if (sql.contains(";")) {
							if (supportBatch(con)) {
								try {
									int[] results = doBatchSqlExecute(con, sql.split(";"));
									if (null != results) {
										return SGLogger.infoER(String.format("      [成功]批量执行SQL语句:%s:结果为:%s.", sql, results));
									} else {
										return SGLogger.error(String.format("      [失败]批量执行SQL语句:%s:结果为null.", sql));
									}
								} catch (SQLException e) {
									e.printStackTrace();
									return SGLogger.error(
											String.format("		[失败]批量执行SQL语句时有异常发生:%s。异常为%s", sql, e.getMessage()));
								}
							} else {
								return SGLogger.error(String.format("      [失败]执行SQL语句:%s:目前暂不支持多语句.", sql));
							}
						}

						Statement st = con.createStatement();
						String lowerCaseSQL = sql.toLowerCase();
						if (!lowerCaseSQL.startsWith("select") && (lowerCaseSQL.contains("insert")
								|| lowerCaseSQL.contains("delete") || lowerCaseSQL.contains("update"))) {
							int nResult = st.executeUpdate(sql);
							updateParams.put("returnValue", String.valueOf(nResult));
							httpUtil.updateInstructionResult(updateParams.toString());
							
							if (nResult >= 0) {
								if (options.existOption(ContextConstant.OPTION_SAVE_TEXT)) {
									String savedKey = options.getValue(ContextConstant.OPTION_SAVE_TEXT);
									String value = String.valueOf(nResult);
									System.err.println("存储sql结果:	" + savedKey + "|" + value);
									options.saveData(savedKey, value);
									System.err.println("DataBase.step.options:" + options.savedDatas());
								}
								return SGLogger.infoER(String.format("		[成功]执行SQL语句:%s,影响:%d条", sql, nResult));
							} else {
								return SGLogger.error(String.format("      [失败]执行SQL语句:%s:结果有误,为:%d条.", sql, nResult));
							}
						} else if(lowerCaseSQL.startsWith("select")) {
							ResultSet selectResult = st.executeQuery(sql);
							ResultSetMetaData metaData = selectResult.getMetaData();
							int colCount = metaData.getColumnCount();
							JSONObject returnObj = new JSONObject();
							if (colCount < 1) {
//								TestCaseProccessor.doUpdateInstructionResult(returnObj.toString());
								updateParams.put("returnValue", "结果列少于1");
								httpUtil.updateInstructionResult(updateParams.toString());
								SGLogger.error("      [失败]执行SQL语句结果的MetaData应该至少有一列");
							} else {
								if(selectResult.next()) {
									//TODO 回传数据
									for(int col = 1; col <= colCount; ++col) {
										String key = metaData.getColumnName(col);
										Object value = selectResult.getObject(col);
										returnObj.put(key, value);
									}
//									TestCaseProccessor.doUpdateInstructionResult(returnObj.toString());
									
									Object firstResult = selectResult.getObject(1);
									updateParams.put("returnValue", null == firstResult ? "null" : firstResult.toString());
									httpUtil.updateInstructionResult(updateParams.toString());

									if (options.existOption(ContextConstant.OPTION_SAVE_TEXT)) {
										if (null != firstResult) {
											String savedKey = options.getValue(ContextConstant.OPTION_SAVE_TEXT);
											
											String value = firstResult.toString();
											System.err.println("存储sql结果:	" + savedKey + "|" + value);
											options.saveData(savedKey, value);
										}
									}
									return SGLogger.infoER(String.format("		[成功]执行SQL语句:	%s,结果:%s,列名:%s", sql, firstResult.toString(), metaData.getColumnName(1)));
								}
								return SGLogger.error(String.format("      [失败]逻辑不正确的SQL语句:%s.", sql));
							}
						}else {
							return SGLogger.error(String.format("      [失败]暂不支持的SQL语句:%s.", sql));
						}
					} else {
						return SGLogger.error(String.format("      [失败]The Data Source Name:%s is null.", dbName));
					}
				} else {
					return SGLogger.error("      [失败]The IDataSource Service is null. OSGi framework should have loaded it.");
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return SGLogger.error(String.format("		[失败]执行SQL语句时有此异常发生:%s。异常为%s", sql, e.getMessage()));
			} catch (Exception e){
				e.printStackTrace();
				return SGLogger.error(String.format("		[失败]执行SQL语句时有异常发生:%s。异常为%s", sql, e.getMessage()));
			} finally {
				if (null != con) {
					try {
						if (!con.isClosed())
							con.close();
					} catch (SQLException e) {
						return SGLogger.infoER(String.format("		[失败]执行SQL语句关闭连接时有异常发生:%s。异常为%s", sql, e.getMessage()));
					}
				}
			}
		}
		return new ExecutionResult(false, "执行SQL,发生未知问题.");
	}
	
	protected static DataSource getTargetDataSource(String dbName, BaseBundleActivator bba, IDataSource ds) {
		DataSource targetDS = ds.getDataSource(dbName);
		System.out.println("the db datasource is " + ds.toString());
		if (null == targetDS) {
			System.err.println("Get data source named [" + dbName + "] failed. try to init data source.");
			// TODO init by task before
			try {
				int nCount = 3;
				while (null == targetDS && nCount-- > 0) {
					Thread.sleep(1 * 1000);
					targetDS = ds.getDataSource(dbName);
					System.err.println("Sleep the " + (3 - nCount) + "st . and target "
							+ (null == targetDS ? "is still null." : "got."));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return targetDS;
	}
	
	protected static boolean supportBatch(Connection con) {
		try {
			// get the meta data for db.
			DatabaseMetaData md = con.getMetaData();
			return md.supportsBatchUpdates();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected static int[] doBatchSqlExecute(Connection con, String[] sqls) throws SQLException {
		if (null == sqls)
			return null;

		System.err.println("doBatchSqlExecute.sqlCount:" + sqls.length);
		System.err.println("doBatchSqlExecute.sqlClauses:" + String.join("|", sqls));
		Statement statement = null;
		int[] result = null;
		try {
			statement = con.createStatement();
			for (int i = 0; i < sqls.length; i++) {
				statement.addBatch(sqls[i].trim());
			}
			// 一次执行多条SQL语句
			result = statement.executeBatch();
		} catch (SQLException e) {
			throw e;
		} finally {
			statement.close();
		}
		
		return result;
	}

	@Override
	public String getExecutionEnvironmentInfo(String info) {
		String dbInfo = null;
		System.err.println("db.getExecutionEnvironmentInfo:" + info);
		if (!StringUtil.nullOrEmpty(info)) {
			dbInfo = getDataSourceInfo(info);
			System.err.println("db.getExecutionEnvironmentInfo.case1:" + dbInfo);
		} else if (!StringUtil.nullOrEmpty(dsName)){
			dbInfo = getDataSourceInfo(dsName);
			System.err.println("db.getExecutionEnvironmentInfo.case2:" + dbInfo);
		}
		
		return null == dbInfo ? "" : dbInfo;
	}
}
