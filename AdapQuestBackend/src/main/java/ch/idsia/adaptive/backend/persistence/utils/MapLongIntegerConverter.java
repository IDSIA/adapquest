package ch.idsia.adaptive.backend.persistence.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.util.Map;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    26.11.2020 13:40
 */
public class MapLongIntegerConverter implements AttributeConverter<Map<Long, Integer>, String> {

	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<Long, Integer> meta) {
		try {
			return om.writeValueAsString(meta);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Map<Long, Integer> convertToEntityAttribute(String dbData) {
		try {
			TypeReference<Map<Long, Integer>> t = new TypeReference<>() {
			};
			return om.readValue(dbData, t);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
