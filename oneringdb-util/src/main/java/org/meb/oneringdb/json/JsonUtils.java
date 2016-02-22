package org.meb.oneringdb.json;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.module.SimpleModule;
import org.meb.oneringdb.db.model.CardBase;
import org.meb.oneringdb.db.model.CardLang;
import org.meb.oneringdb.db.model.CardSetBase;
import org.meb.oneringdb.db.model.CardSetLang;
import org.meb.oneringdb.db.model.CardSetType;
import org.meb.oneringdb.db.model.CardType;
import org.meb.oneringdb.db.model.CycleBase;
import org.meb.oneringdb.db.model.CycleLang;
import org.meb.oneringdb.db.model.DomainBase;
import org.meb.oneringdb.db.model.DomainLang;
import org.meb.oneringdb.db.model.EncounterSetBase;
import org.meb.oneringdb.db.model.EncounterSetLang;
import org.meb.oneringdb.db.model.ScenEnstLink;
import org.meb.oneringdb.db.model.ScenarioBase;
import org.meb.oneringdb.db.model.ScenarioLang;
import org.meb.oneringdb.db.model.Sphere;

public class JsonUtils {

	private JsonUtils() {

	}

	public static JsonGenerator createJsonGenerator(Writer writer) throws IOException {
		return new JsonFactory(createJsonObjectMapper(new ObjectMapper())).createJsonGenerator(writer)
				.useDefaultPrettyPrinter();
	}

	public static JsonGenerator createJsonGenerator(OutputStream stream) throws IOException {
		return createJsonGenerator(new OutputStreamWriter(stream));
	}

	public static JsonParser createJsonParser(Reader reader) throws IOException {
		return new JsonFactory(createJsonObjectMapper(new ObjectMapper())).createJsonParser(reader);
	}

	public static JsonParser createJsonParser(InputStream stream) throws IOException {
		return createJsonParser(new InputStreamReader(stream));
	}

	public static ObjectMapper createJsonObjectMapper(ObjectMapper mapper) {
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
		SerializationConfig config = mapper.getSerializationConfig();
		config.addMixInAnnotations(DomainLang.class, JsonMixIn_StandardLang.class);
		config.addMixInAnnotations(DomainBase.class, JsonMixIn_StandardBase.class);
		config.addMixInAnnotations(CardSetBase.class, JsonMixIn_StandardBase.class);
		config.addMixInAnnotations(CardSetLang.class, JsonMixIn_StandardLang.class);
		config.addMixInAnnotations(EncounterSetBase.class, JsonMixIn_EncounterSetBase.class);
		config.addMixInAnnotations(EncounterSetLang.class, JsonMixIn_StandardLang.class);
		config.addMixInAnnotations(ScenarioBase.class, JsonMixIn_ScenarioBase.class);
		config.addMixInAnnotations(ScenarioLang.class, JsonMixIn_StandardLang.class);
		config.addMixInAnnotations(ScenEnstLink.class, JsonMixIn_ScenarioBase.class);
		config.addMixInAnnotations(CardBase.class, JsonMixIn_CardBase.class);
		config.addMixInAnnotations(CardLang.class, JsonMixIn_CardLang.class);
		config.addMixInAnnotations(CycleBase.class, JsonMixIn_CycleBase.class);
		config.addMixInAnnotations(CycleLang.class, JsonMixIn_CycleLang.class);

		SimpleModule module = new SimpleModule("ConquestModule", Version.unknownVersion());
		module.addDeserializer(CardSetType.class, new CardSetTypeDeserializer());
		module.addSerializer(CardSetType.class, new CardSetTypeSerializer());
		module.addDeserializer(CardType.class, new CardTypeDeserializer());
		module.addSerializer(CardType.class, new CardTypeSerializer());
		module.addDeserializer(Sphere.class, new SphereDeserializer());
		module.addSerializer(Sphere.class, new SphereSerializer());
		module.addSerializer(ResourceBundle.class, new JsonSerializer<ResourceBundle>() {

			@Override
			public void serialize(ResourceBundle value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jgen.writeStartObject();
				Set<String> keys = value.keySet();
				for (String key : keys) {
					jgen.writeFieldName(key);
					jgen.writeString(value.getString(key));
				}
				jgen.writeEndObject();
			}
		});

		mapper.registerModule(module);

		return mapper;
	}

	public static void write(List<?> data, OutputStream stream) throws JsonProcessingException, IOException {
		Writer writer = new OutputStreamWriter(stream);
		createJsonGenerator(writer).writeObject(data);
		writer.flush();
	}

	public static void write(List<?> data, String fileName) throws JsonProcessingException, IOException {
		FileOutputStream stream = new FileOutputStream(fileName);
		write(data, stream);
		stream.close();
	}

	public static <T> T[] read(InputStream stream, Class<T[]> clazz) throws IOException {
		return createJsonParser(stream).readValueAs(clazz);
	}

	public static <T> T[] read(String fileName, Class<T[]> clazz) throws IOException {
		FileInputStream stream = new FileInputStream(fileName);
		T[] result = createJsonParser(stream).readValueAs(clazz);
		stream.close();
		return result;
	}
}
