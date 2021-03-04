package com.meowlomo.ci.ems.bundle.db;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.sql.DataSource;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.IDataSource;
import com.meowlomo.ci.ems.bundle.interfaces.MeowlomoBundleBaseException;

public class DBEventHandler implements EventHandler {
	private static final Logger logger = LoggerFactory.getLogger(DBEventHandler.class);
	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		String topic = event.getTopic();
		if ("com/meowlomo/bundle/db/directinit" == topic){
			logger.info(" Received db msg: com/meowlomo/bundle/db/directinit");			
			IDataSource ds = BaseBundleActivator.getTheServiceObject("db", IDataSource.class);
			if (null == ds){
				logger.info(" IDataSource Service Object is null");
			}
			else if (!ds.inited()){
				logger.info(" db bundle begin init by inner dbconfig.xml");
				ds.init("/dbconfig.xml");
				logger.info(" db bundle inited by inner dbconfig.xml");
			}else{
				logger.info(" db bundle has been inited.");
			}
		}
		else if ("com/meowlomo/bundle/db/init" == topic){
			String filePath = (String)event.getProperty("path");
			URI uri = (URI)event.getProperty("uri");
			
			IDataSource ds = BaseBundleActivator.getTheServiceObject("db", IDataSource.class);
			if (!ds.inited()) {
				if (null != filePath && !filePath.isEmpty()) {
					ds.init(filePath);
					logger.info(String.format("  db bundle init by file:%s", filePath));
				} else {
					ds.init(uri);
					logger.info(String.format("  db bundle init by URI:%s", uri));
				}
			}
		}else if ("com/meowlomo/bundle/db/directadd" == topic){
			String className = (String)event.getProperty("dataSourceClassName");
			String url = (String)event.getProperty("url");
			String username = (String)event.getProperty("username");
			String password = (String)event.getProperty("password");
			String name = (String)event.getProperty("name");
			
			logger.error("add database name :" + name);
			try {
				IDataSource ds = BaseBundleActivator.getTheServiceObject("db", IDataSource.class);
				if (null != ds){
					logger.info("开始添加DataSource");
					logger.info("the db datasource is " + ds.toString());
					ds.addDataSource(url, username, password, className, name);
					logger.info("结束添加DataSource");
					logger.info("开始尝试获取DataSource");
					DataSource target = ds.getDataSource(name);
					logger.info("结束获取DataSource");
					if (null == target){
						logger.info("Target Data Source [" + name +"] get failed after adding action.");
					}else{
						logger.info("成功获取DataSource");
					}
				}else{
					logger.error("db activator is null. event process ended.");
				}
			} catch (MeowlomoBundleBaseException e) {
				logger.error("Excpeiton occured while adding datasource. Type is :" + e.getCause());
				e.printStackTrace();
			}
		}
	}
}
