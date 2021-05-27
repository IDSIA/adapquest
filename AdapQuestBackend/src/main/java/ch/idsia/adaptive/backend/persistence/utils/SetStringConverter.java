package ch.idsia.adaptive.backend.persistence.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    26.11.2020 13:40
 */
public class SetStringConverter implements AttributeConverter<Set<String>, String> {

	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Set<String> meta) {
		try {
			return om.writeValueAsString(meta);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Set<String> convertToEntityAttribute(String dbData) {
		try {
			TypeReference<Set<String>> t = new TypeReference<>() {
			};
			return om.readValue(dbData, t);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
