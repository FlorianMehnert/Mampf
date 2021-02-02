package mampf;


import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

import mampf.catalog.Item.Domain;

public class Util {

	private Util() {
	}

	public static <T> int compareTwoToStrings(T a, T b) {
		int minLength = java.lang.Math.min(a.toString().length(), b.toString().length()) - 1;
		int maxLength = java.lang.Math.max(a.toString().length(), b.toString().length()) - 1;

		for (int i = 0; i < minLength; i++) {
			int dif = a.toString().getBytes()[i] - b.toString().getBytes()[i];
			if (dif != 0) {
				return dif;
			}
		}
		int compAmount = 0;
		if (minLength < maxLength) {
			if (a.toString().length() > b.toString().length()) {
				compAmount = 1;
			} else {
				compAmount = -1;
			}
		}
		return compAmount;
	}

	public static Domain parseDomainEnum(String domain) {
		if(domain.equals("")){
			return null;
		}
		try {
			return Domain.valueOf(domain.toUpperCase().replace("-", "_"));
		}catch (IllegalArgumentException iae){
			return null;
		}

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


	public static <T> String listToString(List<T> list) {
		StringBuilder output = new StringBuilder();
		for (T item : list) {
			output.append("\n").append(item.toString());
		}
		return output.toString();
	}
}
