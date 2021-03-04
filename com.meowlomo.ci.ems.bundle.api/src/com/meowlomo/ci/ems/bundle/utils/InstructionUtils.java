package com.meowlomo.ci.ems.bundle.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

/**
 * The Class InstructionUtils. Utils for signal instruction processing
 */
public class InstructionUtils {

	/**
	 * Decode target object string.
	 *
	 * @param instructionObjectString
	 *            the original instruction object string
	 * @return the map contains project name, application name, section name, object
	 *         name
	 */
	public static Map<String, String> decodeTargetObjectString(String instructionObjectString) {
		Map<String, String> resultMap = new HashMap<String, String>();
		if (instructionObjectString.isEmpty()) {
			return null;
		} else {
			Pattern re = Pattern.compile("^(.+\\.)?(.+)\\.(.+)\\.(.*section\\.)*(.+)\\.(.+)$");
			Matcher m = re.matcher(instructionObjectString.toLowerCase());
			if (m.find()) {
				resultMap.put("projectname", m.group(2).toLowerCase());
				resultMap.put("applicationname", m.group(3).toLowerCase());
				resultMap.put("sectionname", m.group(5).toLowerCase());
				resultMap.put("elementname", m.group(6).toLowerCase());
			}
			return resultMap;
		}
	}

	protected static String convertSavedValues(String input, Map<String, String> savedInstructionValues) {
		if (!savedInstructionValues.isEmpty()) {
			Iterator<Map.Entry<String, String>> it = savedInstructionValues.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> entry = it.next();
				String targetKey = String.format("$(%s)", entry.getKey());
				if (input.contains(targetKey)) {
					input = input.replace(targetKey, entry.getValue());
				}
			}
		}
		return input;
	}

	protected static String convertMultiFuncInput(String input) {
		if (StringUtil.nullOrEmpty(input))
			return input;
		else {
			if (input.contains(ContextConstant.INPUT_CONVERTER_PATTERN_PREFIX)
					&& input.contains(ContextConstant.INPUT_CONVERTER_PATTERN_SUFFIX)) {
				String pattern = "\\u0024\\u007B(((lettermix|letterlowercase|lettercapital|letterintmix|int|double|chinese|specialsymbol|timeCurrent)\\u002Erandom\\u0028\\d*)|(unixtime(InMilliseconds){0,1}\\u0028))\\u0029\\u007D";
				Pattern p = Pattern.compile(pattern);
				Matcher match = p.matcher(input);
				StringBuffer sb = new StringBuffer();

				while (match.find()) {
					String matchedSubString = match.group(0);
					String convert = convertFuncInput(matchedSubString);
					if (matchedSubString.contains("specialsymbol")) {
						convert = convert.replaceAll("\\u0024", "\\\\\\$");
					}
					System.out.println(convert);
					match.appendReplacement(sb, convert);

				}
				match.appendTail(sb);// 从截取点将后面的字符串接上
				String convertedInput = sb.toString();
				System.out.println(convertedInput);
				return convertedInput;
			}
		}
		return input;
	}

	/**
	 * convert the input by function
	 * 
	 * @param input
	 * @return
	 */
	public static String convertFuncInput(String input) {
		do {
			if (StringUtil.nullOrEmpty(input))
				break;
			else {
				// match the ${int.random()}
				if (input.startsWith(ContextConstant.INPUT_CONVERTER_PATTERN_PREFIX)
						&& input.endsWith(ContextConstant.INPUT_CONVERTER_PATTERN_SUFFIX)) {

					if (!input.contains("random") && !input.contains("unixtime"))
						break;

					int prefixLen = ContextConstant.INPUT_CONVERTER_PATTERN_PREFIX.length();
					int suffixLen = ContextConstant.INPUT_CONVERTER_PATTERN_SUFFIX.length();
					if (prefixLen + suffixLen >= input.length())
						break;
					// get the function name like: int.random()
					String functionName = input.substring(prefixLen, input.length() - suffixLen);
					if (functionName.contains("()")) {
						int nLen = Math.abs(new Random().nextInt() % genContentLenLimit(functionName));
						nLen++;
						String content = genContent(functionName, nLen);
						if (content.isEmpty())
							break;
						else
							return content;
					} else if (functionName.contains("(") && functionName.contains(")")) {
						int posL = functionName.indexOf("(");
						int posR = functionName.indexOf(")");

						if (posR > posL && posL > 0) {
							String funcName = functionName.substring(0, posL);
							String number = functionName.substring(posL + 1, posR);
							if (number.matches("[1-9]{1}\\d*") && number.length() <= 5) {
								int nLen = new Integer(number);
								String content = genContent(funcName + "()", nLen);
								if (content.isEmpty())
									break;
								else
									return content;
							} else {
								break;
							}
						} else {
							break;
						}
					}

				} else {
					break;
				}
			}
		} while (false);
		return input;
	}

	protected static String genContent(String funcName, int nLen) {
		switch (funcName) {
		case ContextConstant.INPUT_CONVERTER_PATTERN_INT:
			return getRandomInt(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_DOUBLE:
			return doubleRandom(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_CHINESE:
			return chineseRandom(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_LETTER:
			return getLetterMixed(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_LETTER1:
			return getLittleLetter(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_LETTER2:
			return getCapitalLetter(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_LETTERINTMIX:
			return getLetterIntMixed(nLen);
		case ContextConstant.INPUT_CONVERTER_PATTERN_SPECIALSYMBOL:
			return getSpecialSymbol(nLen);
		case ContextConstant.INPUT_CONVERTER_TIME_CURRENT:
			return getCurrentTime(nLen);
		case ContextConstant.INPUT_CONVERTER_UNIXTIME:
			return getUnixtime();
		case ContextConstant.INPUT_CONVERTER_UNIXTIME_MILLISEC:
			return getUnixtimeInMillisec();
		default:
			return "";
		}
	}
	
	protected static int genContentLenLimit(String funcName) {
		switch (funcName) {
		case ContextConstant.INPUT_CONVERTER_TIME_CURRENT:
			return 0x7FFFFFFF;
		default:
			return 8;
		}
	}

	public static String getLittleLetter(int length) {
		return innerGetAssignedCharByLength("abcdefghijklmnopqrstuvwxyz", length);
	}

	public static String getCapitalLetter(int length) {
		return innerGetAssignedCharByLength("ABCDEFGHIJKLMNOPQRSTUVWXYZ", length);
	}

	public static String getLetterMixed(int length) {
		return innerGetAssignedCharByLength("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", length);
	}

	public static String getLetterIntMixed(int length) {
		return innerGetAssignedCharByLength("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", length);
	}

	public static String getRandomInt(int length) {
		return innerGetAssignedCharByLength("0123456789", length);
	}

	public static String getSpecialSymbol(int length) {
		return innerGetAssignedCharByLength("!@#$%^&*", length);
	}

	protected static String innerGetAssignedCharByLength(String source, int length) {
		if (length <= 0 || StringUtil.nullOrEmpty(source))
			return "";

		String base = source;// ;
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param len
	 * @return
	 */
	public static String doubleRandom(int len) {
		// TODO
		double randomD = new Random().nextDouble();
		return String.format("%f", randomD);
	}
	
	/**
	 * 
	 * @param len
	 * @return
	 */
	public static String chineseRandom(int len) {
		String result = "";
		if (len < 100) {
			try {
				while (len > 0) {
					len--;
					result += chineseCreate();
				}
			} catch (Exception e) {
				result = "";
			}
		} else {
			return "";
		}
		return result;
	}

	/**
	 * 获取系统当前时间字符，格式例如：20180712200315 len不支持输入0,大于14则返回值也只为14位
	 * 
	 * @return
	 */
	public static String getCurrentTime(int len) {
		String result = "";
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		result = df.format(new Date());
		if (len > 0 && len <= 14) {
			result = result.substring(0, len);
		} else {
			result = result.substring(0, 14);
		}
		return result;
	}
	
	/**
	 * 生成unix时间
	 */
	public static String getUnixtime() {
		long milli = System.currentTimeMillis()/1000;
		return String.valueOf(milli);
	}
	
	public static String getUnixtimeInMillisec() {
		return String.valueOf(System.currentTimeMillis());
	}

	public static String chineseCreate() throws Exception {
		String str = null;
		int hightPos, lowPos; // 定义高低位
		Random random = new Random();
		hightPos = (176 + Math.abs(random.nextInt(39)));// 获取高位值
		lowPos = (161 + Math.abs(random.nextInt(93)));// 获取低位值
		byte[] b = new byte[2];
		b[0] = (new Integer(hightPos).byteValue());
		b[1] = (new Integer(lowPos).byteValue());
		str = new String(b, "GBK");// 转成中文
		return str;
	}
}
