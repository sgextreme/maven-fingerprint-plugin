package com.st.plugin.fingerprint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Pattern SCRIPT_PATTERN = Pattern.compile("(\")([^\\s]*?\\.js)(\")");
		//Matcher m = SCRIPT_PATTERN.matcher(data);
		
		String content = "require.config({\r\n" + 
		"	//urlArgs: 'cacheBuster=' + Math.random(),\r\n" + 
		"    paths: {\r\n" + 
		"        'angular-route': '../lib/angular-route',\r\n" + 
		"        'angular-cookies': '../lib/angular-cookies',\r\n" + 
		"        'angularAMD': '../lib/angularAMD',\r\n" + 
		"        'angular': '../lib/angular',\r\n" + 
		"        'angular-animate': '../lib/angular-animate',\r\n" + 
		"        'ngload': '../lib/ngload',\r\n" + 
		"        'ui-bootstrap-tpls': '../lib/ui-bootstrap-tpls',\r\n" + 
		"        'ng-tags-input': '../lib/modules/ng-tags-input',\r\n" + 
		"        'jquery' : '../lib/jquery',\r\n" + 
		"    },\r\n" + 
		"    shim: {\r\n" + 
		"        'angularAMD': ['angular'],\r\n" + 
		"        'ngload': ['angularAMD'],\r\n" + 
		"        'ui-bootstrap-tpls': ['angular'],\r\n" + 
		"        'ng-tags-input': ['angular'],\r\n" + 
		"        'angular-route': ['angular'],\r\n" + 
		"        'angular-cookies': ['angular'],\r\n" + 
		"        'angular-animate': ['angular'],\r\n" + 
		"    	'login/LoginController': ['jquery'],\r\n" + 
		"    	'MainController': ['angular'],\r\n" + 
		"    	'directives/DateValidationDirective': ['jquery'],\r\n" + 
		"    	'directives/OutFocusClickDirective': ['jquery']\r\n" + 
		"    },\r\n" + 
		"    waitSeconds: 120,\r\n" + 
		"    deps: ['app']\r\n" + 
		"});\r\n"; 
		
//		String replaced = "<link rel=\"stylesheet\" href=\"${pageContext.request.contextPath}/resources/css/style.css\" />"
//				.replaceAll(Pattern.quote("${")+"[\\S]+"+Pattern.quote("}"), "");
		
//		Pattern p = Pattern.compile("lib/angular");
//		System.out.println(Pattern.matches("lib/angular", content));
//		Matcher m = p.matcher(content);
//		System.out.println(m.find());
//		m.
		String replaced = content.replaceFirst("lib/angular',", "lib/32e32323/angular").replaceFirst("lib/angularAMD"+Pattern.quote("$"), "lib/d4332323/angularAMD");
		System.out.println(replaced);
	}

}
