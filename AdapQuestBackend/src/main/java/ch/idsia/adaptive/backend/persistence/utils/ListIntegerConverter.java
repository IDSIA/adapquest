package ch.idsia.adaptive.backend.persistence.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    14.10.2021 15:51
 */
public class ListIntegerConverter implements AttributeConverter<List<Integer>, String> {

	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<Integer> meta) {
		if (meta.isEmpty())
			return "";
		return meta.stream().map(Object::toString).collect(Collectors.joining(","));
	}

	@Override
	public List<Integer> convertToEntityAttribute(String dbData) {
		if (dbData.isEmpty())
			return new ArrayList<>();
		return Arrays.stream(dbData.split(",")).map(Integer::parseInt).collect(Collectors.toList());
	}
}
