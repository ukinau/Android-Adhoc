package android.tether.system;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidTetherCommon {

	public static String extractMatchString(String regex, String target) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(target);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}
}
