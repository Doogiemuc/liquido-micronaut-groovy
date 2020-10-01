package org.doogie.polls

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.bson.types.ObjectId

import javax.inject.Singleton

/**
 * Helper bean to serialize mongoDB org.bson.types.ObjectId into Strings and back
 */
@Singleton   // not groovy.lang.Singleton !!!
class MongoObjectIdSerializer extends JsonSerializer<ObjectId> {

	@Override
	void serialize(ObjectId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeString(value.toHexString())
	}
}
