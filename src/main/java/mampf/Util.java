package mampf;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import mampf.catalog.Item;
import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;

public class Util {

	private Util() {
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static final List<Item.Category> infinity = List.of(Category.BUFFET, Category.DECORATION,
			Category.DINNER_EVENT, Category.FOOD, Category.SPECIAL_OFFERS);

	//private static final SecureRandom random = new SecureRandom();
	public static <T> int compareCategories(T a, T b) {
		for (int i = 0; i < (java.lang.Math.max(a.toString().length(), b.toString().length())); i++) {
			int dif = a.toString().getBytes()[i] - b.toString().getBytes()[i];
			if (dif != 0) {
				return dif;
			}
		}
		return 0;
	}

	public static Domain parseDomainEnum(String domain) {
		return Domain.valueOf(domain.toUpperCase().replace("-", "_"));
	}

	public static Domain parseDomainName(String domain) {
		String formatted = domain.toUpperCase().replace("\\s", "_");
		return Domain.valueOf(formatted);
	}

	public static String renderDomainName(String domain) {
		if (domain == null || domain.length() == 0) {
			return "";
		}
		domain = domain.toLowerCase().replaceAll("(-|_)", " ");
		String[] domainArray = domain.split(" ");
		return Arrays.stream(domainArray)
				.map(e -> e.substring(0, 1).toUpperCase() + e.substring(1))
				.collect(Collectors.joining(" "));
	}
	//public static <T extends Enum<?>> T randomEnum(Class<T> _class){
	//	int enumIdentifier = random.nextInt(_class.getEnumConstants().length);
	//	return _class.getEnumConstants()[enumIdentifier];
	//}

	public static <T> String listToString(List<T> list) {
		StringBuilder output = new StringBuilder();
		for (T item : list) {
			output.append("\n").append(item.toString());
		}
		return output.toString();
	}
}
