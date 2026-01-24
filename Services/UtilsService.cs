using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace HrWebRecruitment.Services
{
    public static class UtilsService
    {
        public class BsonDateTimeConverter : JsonConverter
        {
            public override bool CanConvert(Type objectType)
            {
                return objectType == typeof(DateTime) || objectType == typeof(DateTime?);
            }

            public override object ReadJson(JsonReader reader, Type objectType, object existingValue, JsonSerializer serializer)
            {
                if (reader.TokenType == JsonToken.Null)
                {
                    return null;
                }

                // Check if the value is a nested object (the BSON Date format)
                if (reader.TokenType == JsonToken.StartObject)
                {
                    // Read the full BSON Date structure: { "$date": "..." }
                    JObject jsonObject = JObject.Load(reader);
                    JToken dateToken = jsonObject["$date"];

                    if (dateToken != null && dateToken.Type == JTokenType.String)
                    {
                        // Parse the ISO 8601 string value
                        if (DateTime.TryParse(dateToken.ToString(), out DateTime date))
                        {
                            return date;
                        }
                    }
                }
                // Handle cases where the date might be a simple ISO string (less common with ToJson())
                else if (reader.TokenType == JsonToken.String)
                {
                    if (DateTime.TryParse(reader.Value.ToString(), out DateTime date))
                    {
                        return date;
                    }
                }

                // Return default or throw error
                return existingValue;
            }

            public override void WriteJson(JsonWriter writer, object value, JsonSerializer serializer)
            {
                // For writing, we typically don't need the BSON format, just the ISO string.
                // If you need the BSON format for a round trip, you would implement the nested BSON structure here.
                if (value is DateTime dateTime)
                {
                    writer.WriteValue(dateTime.ToString("o")); // Write as ISO 8601 string
                }
            }
        }
    }
}
