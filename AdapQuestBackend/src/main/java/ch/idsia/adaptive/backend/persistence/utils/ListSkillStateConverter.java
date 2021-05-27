package ch.idsia.adaptive.backend.persistence.utils;

import ch.idsia.adaptive.backend.persistence.model.SkillState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.util.Collections;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    16.12.2020 14:03
 */
public class ListSkillStateConverter implements AttributeConverter<List<SkillState>, String> {

	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<SkillState> meta) {
		try {
			return om.writeValueAsString(meta);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<SkillState> convertToEntityAttribute(String dbData) {
		try {
			TypeReference<List<SkillState>> t = new TypeReference<>() {
			};
			List<SkillState> list = om.readValue(dbData, t);
			Collections.sort(list);
			return list;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
}