package intermediateCodeTranslator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;

public class Translator {
	private ArrayList<String> code = new ArrayList<>();
	private Integer top = 0;// 栈指针
	private Stack<Integer> stack = new Stack<>();// 数据栈
	private HashMap<String, function> funcTable = new HashMap<>();// 函数表
	private HashMap<String, variable> varTable = new HashMap<>();// 变量表
	private HashMap<String, Integer> labelTable = new HashMap<>();// 标签表（处理控制流）
	private Integer currentLine = 0;// 代码区当前要执行的下一条指令位置
	private returnInfo globalRetInfo = new returnInfo(code.size(), 0, varTable);

	public void run(String arg, function f, returnInfo retInfo) {
		if (arg.startsWith("var")) {
			doVar(arg, f);
		}
		if (arg.startsWith("push")) {
			if (!doPush(arg, f)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 4) + " 未赋初值");
			}
		}
		if (arg.startsWith("pop")) {
			if (!doPop(arg, f)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 4) + " 未赋初值");
			}
		}
		if (arg.startsWith("add")) {
			if (!doAdd(arg)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 3) + " 未赋初值");
			}
		}
		if (arg.startsWith("sub")) {
			if (!doSub(arg)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 3) + " 未赋初值");
			}
		}
		if (arg.startsWith("mul")) {
			if (!doMul(arg)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 3) + " 未赋初值");
			}
		}
		if (arg.startsWith("div")) {
			if (!doDiv(arg)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 3) + " 未赋初值");
			}
		}
		if (arg.startsWith("jmp")) {
			if (!doJmp(arg)) {
				System.out.println("error");
			}
		}
		if (arg.startsWith("jz")) {
			if (!doJz(arg)) {
				System.out.println("error");
			}
		}

		if (arg.startsWith("$")) {
			if (!doFunc(arg, f.localVariableTable)) {
				System.out.println("error");
			}
//			System.out.println("dofunc success");
		}
		if (arg.startsWith("print")) {
			if (!doPrint(arg)) {
				System.out.println("error at: " + stack.size() + " " + arg.substring(0, 3) + " 未赋初值");
			}
		}
		if (arg.startsWith("ret")) {
			if (!doRet(arg, f, retInfo)) {
				System.out.println("error");
			}
//			System.out.println("return success");

		}
	}
	public boolean doVar(String arg, function f) {
		arg = arg.substring(4);
		String[] vars = arg.split(", ");
		for (String s : vars) {
			variable v = new variable();
			v.varName = s;
			v.value = null;
			v.location = top;
			f.localVariableTable.put(s, v);
			stack.push(v.value);
			top++;
		}
		return true;
	}

	public boolean doPush(String arg, function f) {
		arg = arg.substring(5);
		Pattern pattern = Pattern.compile("[0-9]*");
		if (pattern.matcher(arg).matches()) {// 数字or id
			stack.push(Integer.parseInt(arg));
			top++;
		} else {
			variable v = f.localVariableTable.get(arg);
			if (v.value != null) {
				stack.push(v.value);
				top++;
			} else {
				return false;
			}

		}
//		System.out.println("after push of the stack :" + stack);
		return true;
	}

	public boolean doPop(String arg, function f) {

		if (arg.length() > 4) {
			arg = arg.substring(4);
			variable v = f.localVariableTable.get(arg);
			if ((v.value = stack.peek()) == null) {
				return false;
			}
			v.value = stack.pop();
			top--;
			stack.set(v.location, v.value);
//			System.out.println("after pop of the stack :" + stack);
		} else {
			stack.pop();
		}
		return true;
	}

	public boolean doAdd(String arg) {
		if (!stack.isEmpty()) {
			int t1 = stack.pop();
			if (!stack.isEmpty()) {
				int t2 = stack.pop();
				stack.push((t1 + t2));
			}
		}
		top--;
//		System.out.println("after add of the stack :" + stack);
		return true;
	}

	public boolean doSub(String arg) {
		if (!stack.isEmpty()) {
			int t1 = stack.pop();
			if (!stack.isEmpty()) {
				int t2 = stack.pop();
				stack.push(t2 - t1);
			}
		}
		top--;
		return true;
	}

	public boolean doMul(String arg) {
		if (!stack.isEmpty()) {
			int t1 = stack.pop();
			if (!stack.isEmpty()) {
				int t2 = stack.pop();
				stack.push(t2 * t1);
			}
		}
		top--;
		return true;
	}

	public boolean doDiv(String arg) {
		if (!stack.isEmpty()) {
			int t1 = stack.pop();
			if (!stack.isEmpty()) {
				int t2 = stack.pop();
				stack.push(t2 / t1);
			}
		}
		top--;
		return true;
	}

	public boolean doJmp(String arg) {
		arg = arg.split(" ")[1];
		currentLine = labelTable.get(arg);
		return true;
	}

	public boolean doJz(String arg) {
		arg = arg.split(" ")[1];
		if (stack.pop() == 0) {
			currentLine = labelTable.get(arg);
		} else {
			currentLine++;
		}

		return true;
	}

	public boolean doRet(String arg, function f, returnInfo retInfo) {
		Integer returnValue;
		arg = arg.replace("ret", "").trim();
		if (arg.length() == 0) {
			returnValue = null;
		} else {
			Pattern pattern = Pattern.compile("[0-9]*");
			if (pattern.matcher(arg).matches()) {// 数字or id
				returnValue = Integer.parseInt(arg);
			} else {
				variable v = f.localVariableTable.get(arg);
				if (v != null && v.value != null) {
					returnValue = v.value;
				} else {
					returnValue = stack.pop();
					top--;
				}

			}

		}
		while (!stack.isEmpty() && (stack.peek() == null || stack.peek() != Integer.MAX_VALUE)) {
			stack.pop();
			top--;
		}
		if (!stack.isEmpty()) {
			stack.pop();
			top--;
		}
		Integer num = retInfo.paramNum;
		while (num-- > 0) {
			stack.pop();
			top--;
		}
//		System.out.println("in ret::++");
//		System.out.println(stack);
//		System.out.println(top);
//		System.out.println("in ret");
		stack.push(returnValue);
		top++;
		if(f.funcName.equals("main")) {
			code.add(currentLine, "exit");
		}
		return true;
	}

	public boolean doFunc(String arg, HashMap<String, variable> currentVariableTable) {
		returnInfo retInfo = null;
		function f = funcTable.get(arg.replace("$", ""));
		if (f == null) {
			return false;
		}
		Integer callLoaction = currentLine;
		currentLine = f.entryLocation;// 进入函数
		currentLine++;// arg
		int codeLength = code.size();
		while (!code.get(currentLine).equals("end_func") && currentLine != codeLength) {
			arg = code.get(currentLine);
//			System.out.println(arg);
			if (arg.startsWith("arg")) {
				arg = arg.substring(4);
				String[] args = arg.split(", ");
				for (int i = args.length - 1; i >= 0; i--) {
					variable v = new variable();
					v.varName = args[i];
					v.location = top - 1;
					v.value = stack.pop();
					top--;
					f.localVariableTable.put(args[i], v);
				}
//				System.out.println(f);
				for (String s : args) {
					stack.push(f.localVariableTable.get(s).value);
					top++;
				}
			}
			if (retInfo == null) {
				retInfo = new returnInfo();
				retInfo.nextLocation = callLoaction;
				retInfo.varTable = currentVariableTable;
				retInfo.paramNum = f.localVariableTable.size();
				stack.push(Integer.MAX_VALUE);// 将返回信息压入栈中，表示调用该函数处的位置
				top++;
			}
			run(arg, f, retInfo);
//			System.out.println(stack);
//			System.out.println(top);
			currentLine++;
		}
		currentLine = retInfo.nextLocation;
//		System.out.println("cccline:" + currentLine);
//		System.out.println(stack);
//		System.out.println(top);
		f.localVariableTable.clear();
		return true;
	}

	public boolean doPrint(String arg) {
		arg = arg.substring(6);
		String[] args = arg.substring(1, arg.length() - 1).split("%d, ");
		args[0] += "%d";
		String out = "";
		for (int i = args.length - 1; i > 0; i--) {
			out += ", " + args[i].replace("%d", stack.pop().toString());
		}
		out = args[0].replace("%d", stack.pop().toString()) + out;
		System.out.println(out);
		return true;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Translator tl = new Translator();
		String filePath = "C:\\Users\\吴凡\\Desktop\\output.txt";
		try {
			FileInputStream fin = new FileInputStream(filePath);
			InputStreamReader reader = new InputStreamReader(fin);
			BufferedReader buffReader = new BufferedReader(reader);
			String arg = "";
			while ((arg = buffReader.readLine()) != null) {
//				System.out.println(arg);
				if (arg.length() > 0) {
					arg = arg.trim();
					tl.code.add(arg);
					if (arg.charAt(0) == '_') {
						tl.labelTable.put(arg.substring(0, arg.indexOf(":")), tl.currentLine);
					}
					if (arg.startsWith("function")) {
						arg = arg.substring(9).replace("@", "").replaceAll(":", "");
						function f = new function();
						f.funcName = arg;
						f.entryLocation = tl.currentLine;
						tl.funcTable.put(arg, f);
					}
					tl.currentLine++;
				}

			}
			tl.funcTable.get("main").localVariableTable = tl.varTable;
//			System.out.println(tl.funcTable);
			buffReader.close();
			tl.currentLine = 0;
			int codeLength = tl.code.size();
			while (tl.currentLine != codeLength) {
				arg = tl.code.get(tl.currentLine);
//				System.out.println(arg);
				tl.run(arg, tl.funcTable.get("main"), tl.globalRetInfo);
				tl.currentLine++;
//				System.out.println("main:" + tl.stack);
//				System.out.println("main:" + tl.top);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

class function {
	@Override
	public String toString() {
		return "function [funcName=" + funcName + ", returnType=" + returnType + ", entryLocation=" + entryLocation
				+ ", localVariableTable=" + localVariableTable + "]";
	}

	String funcName = "";
	String returnType = "";
	Integer entryLocation = null;
	HashMap<String, variable> localVariableTable = new HashMap<>();// 局部变量表

}

class variable {
	@Override
	public String toString() {
		return "variable [varName=" + varName + ", value=" + value + ", location=" + location + "]";
	}

	String varName = "";
	Integer value = 0;
	Integer location = 0;
}

class returnInfo {
	Integer nextLocation;
	Integer paramNum;
	HashMap<String, variable> varTable;// 调用者的变量表

	public returnInfo() {

	}

	public returnInfo(Integer nextLocation, Integer paramNum, HashMap<String, variable> varTable) {
		this.nextLocation = nextLocation;
		this.paramNum = paramNum;
		this.varTable = varTable;
	}

}
