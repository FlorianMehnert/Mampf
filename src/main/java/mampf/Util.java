package mampf;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;

import mampf.catalog.Item.Domain;

public class Util {
	private static final SecureRandom random = new SecureRandom();

	public static Domain parseDomainEnum(String domain){
		return Domain.valueOf(domain.toUpperCase().replace("-", "_"));
	}
	public static String renderDomainName(String domain){
		domain = domain.toLowerCase().replaceAll("(-|_)", " ");
		String[] domainArray = domain.split(" ");
		return Arrays.asList(domainArray)
		.stream()
		.map(e -> e.substring(0, 1).toUpperCase() + e.substring(1))
		.collect(Collectors.joining(" "));
	}
	public static <T extends Enum<?>> T randomEnum(Class<T> _class){
		int enumIdentifier = random.nextInt(_class.getEnumConstants().length);
		return _class.getEnumConstants()[enumIdentifier];
	}
}
