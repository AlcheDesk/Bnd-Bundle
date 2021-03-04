package com.meowlomo.ci.ems.bundle.expression;

import java.math.BigDecimal;
import java.util.List;

import org.json.JSONObject;

import com.meowlomo.ci.ems.bundle.interfaces.BaseBundleActivator;
import com.meowlomo.ci.ems.bundle.interfaces.ExecutionResult;
import com.meowlomo.ci.ems.bundle.interfaces.IExpression;
import com.meowlomo.ci.ems.bundle.interfaces.IHttpUtil;
import com.meowlomo.ci.ems.bundle.interfaces.Instruction;
import com.meowlomo.ci.ems.bundle.utils.InstructionOptions;
//import com.udojava.evalex.Expression;
import com.meowlomo.ci.ems.bundle.utils.JSONUtil;

public class ExpressionImpl implements IExpression {
	IHttpUtil http = null;
	
	private void getHttpTool() {
		if (null == http) {
			http = IHttpUtil.getHttpTool();
		}
	}

	@Override
	public String step(String instructionJson, List<String> paramsInOrOut) {
		System.err.println("[Expression Test Execution Here]");
		getHttpTool();
		JSONObject instruction = new JSONObject(instructionJson);
		System.err.println(instruction.toString());

		Instruction expressionIns = Instruction.generate(instructionJson);

		boolean pass = false;
		
		if (!isStandSingleton()) {
			//TODO
		}
		
		System.err.println("Expression.step.input.origin:" + expressionIns.getInput());
		String input = InstructionOptions.instance().doWithSavedData(expressionIns.getInput());
		System.err.println("Expression.step.input.real:" + input);
		System.err.println("Expression.step.options:" + InstructionOptions.instance().savedDatas());
		
		JSONObject methodParams = new JSONObject();
		methodParams.put("inputData", input);
		http.updateInstructionResult(methodParams.toString());
		
		StringBuilder sb = new StringBuilder();
		sb.append(genMsg(expressionIns, input));
		if (input.isEmpty()) {
			pass = true;
			sb.append(", input为空,直接成功");
		} else if (expressionIns.getInstructionType().equals("MathExpressionProcessor")) {
			try {
				// TODO
				BigDecimal result = BigDecimal.ONE;// new Expression(input).eval();
				if (BigDecimal.ONE == result) {
					pass = true;
					sb.append(" 结果为真");
				} else {
					sb.append(" 结果为:" + result);
				}
			} catch (Exception e) {
				sb.append("发生异常:" + e.getMessage());
			}
		} else {
			String[] hands = input.split("=");
			if (null != hands && 2 == hands.length) {
				if (hands[0].equals(hands[1])) {
					pass = true;
					sb.append(" 比对成功");
				} else {
					sb.append(" 比对为不等");
				}
			} else {
				sb.append(",格式不对应为等式,使用 '='");
			}
		}
		ExecutionResult er = new ExecutionResult(pass, sb.toString());
		return er.toString();
	}

	private String genMsg(Instruction in, String input) {
		return String.format("指令 [%s] 类型 [%s] 算式 [%s] 真实算式[%s]", in.getIndex(), in.getInstructionType(), in.getInput(), input);
	}

	@Override
	public String getExecutionEnvironmentInfo(String info) {
		JSONUtil.beginJSONObject("Expression");
		JSONUtil.addJSONField("Expression", "EvalEx lib version", "2.1");
		return JSONUtil.endJSONObject("Expression", true);
	}
}
