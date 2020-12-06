package mampf;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import mampf.catalog.Item.Domain;

public class Util {
	private static final SecureRandom random = new SecureRandom();
	public static Domain parseDomainEnum(String domain){
		return Domain.valueOf(domain.toUpperCase().replace("-", "_"));
	}

	public static Domain parseDomainName(String domain){
		String formatted = domain.toUpperCase().replace("\s", "_");
		return Domain.valueOf(formatted);
	}

	public static String renderDomainName(String domain){
		if(domain == null || domain.length() == 0){
			return "";
		}
		domain = domain.toLowerCase().replaceAll("(-|_)", " ");
		String[] domainArray = domain.split(" ");
		return Arrays.stream(domainArray)
		.map(e -> e.substring(0, 1).toUpperCase() + e.substring(1))
		.collect(Collectors.joining(" "));
	}
	public static <T extends Enum<?>> T randomEnum(Class<T> _class){
		int enumIdentifier = random.nextInt(_class.getEnumConstants().length);
		return _class.getEnumConstants()[enumIdentifier];
	}

	public static <T> String listToString(List<T> list){
		StringBuilder output = new StringBuilder();
		for(T item : list){
			output.append("\n").append(item.toString());
		}
		return output.toString();
	}
}
