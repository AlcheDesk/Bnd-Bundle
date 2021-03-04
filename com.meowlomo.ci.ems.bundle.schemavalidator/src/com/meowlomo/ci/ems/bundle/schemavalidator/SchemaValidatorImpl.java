package com.meowlomo.ci.ems.bundle.schemavalidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.ISchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.examples.Example1;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class SchemaValidatorImpl implements ISchemaValidator {
	private static final Logger logger = LoggerFactory.getLogger(SchemaValidatorImpl.class);
	private BaseBundleActivator activator = null;
	
//	private static String readFileContent(String filePath) throws IOException {
//		// 对一串字符进行操作
//		StringBuffer fileData = new StringBuffer();
//		//
//		BufferedReader reader = new BufferedReader(new FileReader(filePath));
//		char[] buf = new char[4096];
//		int numRead = 0;
//		while ((numRead = reader.read(buf)) != -1) {
//			String readData = String.valueOf(buf, 0, numRead);
//			fileData.append(readData);
//		}
//		// 缓冲区使用完必须关掉
//		reader.close();
//		return fileData.toString();
//	}
	
	public SchemaValidatorImpl(BaseBundleActivator bba){
		activator = bba;
	}
	
	protected SchemaValidatorImpl() {
		
	}
	
	@Override
	public String doTestProcess(String jsonTask, List<String> paramsInOut) {
		return new ExecutionResult(true, "OK").toString();
	}

	@Override
	public void notifyTimeout() {
		// TODO Auto-generated method stub
	}

	@Override
	public ValidateResult validateJSONSchema(String json, String schema) {
//		ObjectNode objNode = JsonNodeFactory.instance.objectNode();	arrayNode();
		ValidateResult vrResult = new ValidateResult();
		try {
			JsonNode content = JsonLoader.fromString(json);
			JsonNode schemaContent = JsonLoader.fromString(schema);
			final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
	        final JsonSchema jsonSchema = factory.getJsonSchema(schemaContent);

	        ProcessingReport report = jsonSchema.validate(content);
	        String errors = report.toString();
	        String[] errorArray = errors.split("\n");
	        List<String> errorInfoList = Arrays.asList(errorArray);
	        Set<String> errorSet = errorInfoList.stream()
	        						.filter((String line) -> line.contains("error:"))
	        						.collect(Collectors.toSet());

	        vrResult.ok = report.isSuccess();
	        if (!vrResult.ok)
	        	vrResult.msg = (1 == errorSet.size() ? errorSet.toArray()[0].toString() : errorSet.toString());
	        
		} catch (IOException | ProcessingException e) {
			vrResult.ok = false;
			vrResult.msg = e.getMessage();
			e.printStackTrace();
		}
		return vrResult;
		//ValidateResult fromString
//		return vrResult.toString();
	}

	@Override
	public ValidateResult validateXMLSchema(String json, String schema) {
		// TODO Auto-generated method stub
		return null;
	}
}
