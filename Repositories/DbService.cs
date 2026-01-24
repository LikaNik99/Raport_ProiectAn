using Microsoft.AspNetCore.Cryptography.KeyDerivation;
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Serializers;
using MongoDB.Bson.Serialization;
using MongoDB.Driver;
using MongoDB.Driver.Linq;
using PasswordManager.Models;
using PasswordManager.Models.Config;
using System.Globalization;
using System.Security.Cryptography;

namespace PasswordManager.Repositories
{
    public class DbService(IConfiguration config, IMongoClient mongoClient, MongoDbConfigModel dbConfig)
    {
        private IMongoDatabase _mongoDatabase = mongoClient.GetDatabase(config["MongoDb:DatabaseName"]);
        private IMongoCollection<BsonDocument> _usersCollection;
        private IMongoCollection<PasswordModel> _passwordsCollection;
        private IMongoCollection<BsonDocument> _requestsCollection;
        public DbService(IConfiguration config, IMongoClient mongoClient, IOptions<MongoDbConfigModel> dbConfig) : this(config, mongoClient, dbConfig.Value)
        {
            InitDatabaseAsync().Wait();
        }

        public async Task InitDatabaseAsync()
        {
            try
            {
                // Creează collections dacă nu există
                var collections = await _mongoDatabase.ListCollections().ToListAsync();
                var collectionNames = collections.Select(c => c["name"].AsString).ToList();

                if (!collectionNames.Contains("users"))
                {
                    await _mongoDatabase.CreateCollectionAsync("users");
                    Console.WriteLine("✓ Collection \"users\" creată");
                }

                if (!collectionNames.Contains("passwords"))
                {
                    await _mongoDatabase.CreateCollectionAsync("passwords");
                    Console.WriteLine("✓ Collection \"passwords\" creată");
                }

                if (!collectionNames.Contains("requests"))
                {
                    await _mongoDatabase.CreateCollectionAsync("requests");
                    Console.WriteLine("✓ Collection \"requests\" creată");
                }

                _usersCollection = _mongoDatabase.GetCollection<BsonDocument>("users");
                _passwordsCollection = _mongoDatabase.GetCollection<PasswordModel>("passwords");
                _requestsCollection = _mongoDatabase.GetCollection<BsonDocument>("requests");

                // Creează indexuri
                await _usersCollection.Indexes.CreateOneAsync(
                    new CreateIndexModel<BsonDocument>(Builders<BsonDocument>.IndexKeys.Ascending("email"),
                    new CreateIndexOptions { Unique = true }));

                Console.WriteLine("✓ Index pentru users.email creat");

                await _passwordsCollection.Indexes.CreateOneAsync(
                    new CreateIndexModel<PasswordModel>(Builders<PasswordModel>.IndexKeys.Ascending("userId")));

                Console.WriteLine("✓ Index pentru passwords.userId creat");

                await _requestsCollection.Indexes.CreateOneAsync(
                    new CreateIndexModel<BsonDocument>(Builders<BsonDocument>.IndexKeys.Ascending("userId")));

                await _requestsCollection.Indexes.CreateOneAsync(
                    new CreateIndexModel<BsonDocument>(Builders<BsonDocument>.IndexKeys.Ascending("status")));

                Console.WriteLine("✓ Indexuri pentru requests create");

                // Creează userul admin
                var adminExists = await _usersCollection.Find(Builders<BsonDocument>.Filter.Eq("email", "admin")).FirstOrDefaultAsync();

                if (adminExists == null)
                {

                    // Generate a 128-bit salt using a sequence of
                    // cryptographically strong random bytes.
                    byte[] salt = RandomNumberGenerator.GetBytes(128 / 8); // divide by 8 to convert bits to bytes
                    Console.WriteLine($"Salt: {Convert.ToBase64String(salt)}");

                    // derive a 256-bit subkey (use HMACSHA256 with 100,000 iterations)
                    string hashedPassword = Convert.ToBase64String(KeyDerivation.Pbkdf2(
                        password: "admin",
                        salt: salt,
                        prf: KeyDerivationPrf.HMACSHA256,
                        iterationCount: 100000,
                        numBytesRequested: 256 / 8));

                    Console.WriteLine($"Hashed: {hashedPassword}");

                    var adminDoc = new BsonDocument
                        {
                            { "email", "admin" },
                            { "password", hashedPassword },
                            { "role", "admin" },
                            { "createdAt", DateTime.UtcNow }
                        };
                    await _usersCollection.InsertOneAsync(adminDoc);
                    Console.WriteLine("✓ User admin creat (email: admin, password: admin)");
                }
                else
                {
                    Console.WriteLine("ℹ User admin există deja");
                }

                // Adaugă date demo
                var passwordCount = await _passwordsCollection.CountDocumentsAsync(Builders<PasswordModel>.Filter.Empty);
                if (passwordCount == 0)
                {
                    var demoPasswords = new[]
                    {
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Camera Video Etajul 1",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "admin@example.com",
                            Parola = "demo_password_123",
                            SaitWeb = "https://gmail.com",
                            Detalii = "Cont personal Gmail",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        },
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Camera Video Etajul 1",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "admin_cam1",
                            Parola = "camera_secure_pass_01",
                            SaitWeb = "http://192.168.1.10",
                            Detalii = "Acces cameră supraveghere etaj 1",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        },
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Camera Video Etajul 2",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "admin_cam2",
                            Parola = "camera_secure_pass_02",
                            SaitWeb = "http://192.168.1.11",
                            Detalii = "Acces cameră supraveghere etaj 2",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        },
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Parola Admin Else",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "admin_else",
                            Parola = "else_admin_pass",
                            SaitWeb = "https://else.fcim.utm.md/",
                            Detalii = "Acces aplicație internă Else",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        },
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Parola Laptop ServerRoom",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "server_admin",
                            Parola = "serverroom_laptop_pass",
                            SaitWeb = "local",
                            Detalii = "Laptop dedicat administrare servere",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        },
                        new PasswordModel
                        {
                            UserId = "admin",
                            Denumire = "Panou Control Server",
                            Proprietar = "Admin",
                            Tip = "Conectare",
                            Username = "root_admin",
                            Parola = "server_control_pass",
                            SaitWeb = "https://server-control.local",
                            Detalii = "Acces panou principal control server",
                            Istoric = new List<HistoryEntry>
                            {
                                new HistoryEntry { Actiune = "creat", Data = DateTime.UtcNow }
                            },
                            CreatedDate = DateTime.UtcNow,
                            LastEdited = DateTime.UtcNow
                        }
                    };
                    await _passwordsCollection.InsertManyAsync(demoPasswords);
                    Console.WriteLine("✓ Date demo adăugate (5 parole)");
                }

                Console.WriteLine("\n✅ Baza de date inițializată cu succes!");
                Console.WriteLine("\nPoți accesa aplicația:");
                Console.WriteLine("  Frontend: http://localhost:5000");
                Console.WriteLine("  API: http://localhost:3000");
                Console.WriteLine("  Credențiale: admin / admin");
            }
            catch (Exception ex)
            {
                Console.WriteLine("❌ Eroare: " + ex.Message);
                Environment.Exit(1);
            }
        }

        public async Task<string> GetPasswordsAsync(string objectId)
        {
            // Start with the IQueryable interface, which is the foundation for LINQ operations
            IQueryable<PasswordModel> query = _passwordsCollection.AsQueryable();
            var passwords = await _passwordsCollection.Find(_ => true).ToListAsync();
            // Check if a specific objectId filter is required
            if (!string.IsNullOrEmpty(objectId))
            {
                // --- 1. Validate and Parse ObjectId ---

                ObjectId mongoId;
                // Use standard C# TryParse for validation
                bool validId = ObjectId.TryParse(objectId, out mongoId);

                if (!validId)
                {
                    // Handle invalid ObjectId case as before
                    return "[]";
                }

                // --- 2. Apply Filter using LINQ 'where' clause ---
                // The MongoDB driver translates 'p => p.Id == mongoId' into a Bson Document filter: { "_id": ObjectId(...) }
                query = query.Where(p => p.Id == mongoId.ToString());
            }

            // --- 3. Execute the Query and Retrieve Results ---

            // Use the MongoQueryable.ToListAsync() extension method to execute the query asynchronously
            var list = await query.ToListAsync();

            // --- 4. Serialize to JSON ---
            // The serialization logic remains the same as it handles the final output format.
            var bsonList = list.Select(x => x.ToBsonDocument()).ToList();
            return bsonList.ToJson();
        }

        public async Task<string> DeletePasswordsAsync(string? id)
        {
            if (string.IsNullOrEmpty(id))
                return "Invalid id";

            // Convert string to ObjectId
            if (!ObjectId.TryParse(id, out ObjectId objectId))
                return "Invalid ObjectId format";

            // Build filter
            var filter = Builders<PasswordModel>.Filter.Eq("_id", objectId);

            // Delete the document
            var result = await _passwordsCollection.DeleteOneAsync(filter);

            // Return a message
            if (result.DeletedCount > 0)
                return $"Password with id {id} deleted successfully.";
            else
                return $"Password with id {id} not found.";
        }

        public async Task<string> AddPasswordAsync(PasswordModel passwordData)
        {
            passwordData.Id = ObjectId.GenerateNewId().ToString();
            await _passwordsCollection.InsertOneAsync(passwordData);
            return passwordData.ToJson();
        }

        public async Task<string> UpdatePasswordAsync(string id, PasswordModel updates)
        {
            // Ensure the ID string is a valid ObjectId
            if (!ObjectId.TryParse(id, out var objectId))
            {
                return $"{{ \"success\": false, \"message\": \"Invalid ID format: {id}.\" }}";
            }

            // 1. Define the filter to find the document by its unique MongoDB _id
            // This is the correct way to filter by the primary key.
            var filter = Builders<PasswordModel>.Filter.Eq(p => p.Id, id);
            var collection = _passwordsCollection.Database
                .GetCollection<PasswordModel>(_passwordsCollection.CollectionNamespace.CollectionName);
            // Get the current document from MongoDB
            var current = await collection.Find(filter).FirstOrDefaultAsync();

            if (current == null)
            {
                return $"{{ \"success\": false, \"message\": \"No password found with ID {id}.\" }}";
            }
            // Update current object with non-null values from updates
            foreach (var property in typeof(PasswordModel).GetProperties())
            {
                var value = property.GetValue(updates);
                if (value != null && value != "" && property.Name != nameof(PasswordModel.Id))
                {
                    property.SetValue(current, value);
                }
            }

            // Replace the document in MongoDB
            var replaceResult = await collection.ReplaceOneAsync(filter, current);

            if (replaceResult.ModifiedCount > 0)
            {
                return $"{{ \"success\": true, \"message\": \"Request with ID {id} updated successfully.\" }}";
            }
            else
            {
                return $"{{ \"success\": false, \"message\": \"No request found with ID {id}.\" }}";
            }
        }

        // Add the missing method definition for DeletePasswordByIdAsync  
        public async Task<string> DeletePasswordByIdAsync(string id)
        {
            var filter = Builders<PasswordModel>.Filter.Eq("_id", new ObjectId(id));
            var result = await _passwordsCollection.DeleteOneAsync(filter);
            return result.DeletedCount > 0
                ? $"{{\"success\": true, \"message\": \"Password with id {id} deleted.\"}}"
                : $"{{\"success\": false, \"message\": \"Password with id {id} not found.\"}}";
        }

        // Add the missing method definition for GetRequestsAsync  
        public async Task<string> GetRequestsAsync(string? userId, string? status)
        {
            var filterBuilder = Builders<BsonDocument>.Filter;
            var filters = new List<FilterDefinition<BsonDocument>>
            {
                filterBuilder.Ne("status", "Aprobat"),  // Exclude approved
                filterBuilder.Ne("status", "Respins")  // Exclude approved
            };

            if (!string.IsNullOrEmpty(userId))
            {
                filters.Add(filterBuilder.Eq("userId", userId));
            }

            if (!string.IsNullOrEmpty(status))
            {
                filters.Add(filterBuilder.Eq("status", status));
            }

            var filter = filters.Count > 0 ? filterBuilder.And(filters) : FilterDefinition<BsonDocument>.Empty;
            var requests = await _requestsCollection.Find(filter).ToListAsync();

            return requests.ToJson();
        }

        // Add the missing method definition for AddRequestAsync
        public async Task<string> AddRequestAsync(RequestModel requestModel)
        {
            requestModel.Id = ObjectId.GenerateNewId().ToString();
            var document = new BsonDocument(requestModel.ToBsonDocument());
            await _requestsCollection.InsertOneAsync(document);
            return document.ToJson();
        }

        private static object? ConvertJsonElement(System.Text.Json.JsonElement element)
        {
            switch (element.ValueKind)
            {
                case System.Text.Json.JsonValueKind.Object:
                    var dict = new Dictionary<string, object?>();
                    foreach (var prop in element.EnumerateObject())
                    {
                        dict[prop.Name] = ConvertJsonElement(prop.Value);
                    }
                    return dict;
                case System.Text.Json.JsonValueKind.Array:
                    var list = new List<object?>();
                    foreach (var item in element.EnumerateArray())
                    {
                        list.Add(ConvertJsonElement(item));
                    }
                    return list;
                case System.Text.Json.JsonValueKind.String:
                    return element.GetString();
                case System.Text.Json.JsonValueKind.Number:
                    if (element.TryGetInt64(out var l)) return l;
                    if (element.TryGetDouble(out var d)) return d;
                    return element.GetRawText();
                case System.Text.Json.JsonValueKind.True:
                    return true;
                case System.Text.Json.JsonValueKind.False:
                    return false;
                case System.Text.Json.JsonValueKind.Null:
                case System.Text.Json.JsonValueKind.Undefined:
                    return null;
                default:
                    return element.GetRawText();
            }
        }

        // Other existing methods...
        public async Task<string> UpdateRequestAsync(string id, RequestModel updates)
        {
            // Get the current document from MongoDB
            var filter = Builders<RequestModel>.Filter.Eq(x => x.Id, id);
            var collection = _requestsCollection.Database
                .GetCollection<RequestModel>(_requestsCollection.CollectionNamespace.CollectionName);
            var current = await collection.Find(filter).FirstOrDefaultAsync();

            if (current == null)
            {
                return $"{{ \"success\": false, \"message\": \"No request found with ID {id}.\" }}";
            }

            // Update current object with non-null values from updates
            foreach (var property in typeof(RequestModel).GetProperties())
            {
                var value = property.GetValue(updates);
                if (value != null && value != "" && property.Name != nameof(RequestModel.Id))
                {
                    property.SetValue(current, value);
                }
            }

            // Always update Data to now
            current.Data = DateTime.UtcNow;

            // Replace the document in MongoDB
            var replaceResult = await collection.ReplaceOneAsync(filter, current);

            if (replaceResult.ModifiedCount > 0)
            {
                return $"{{ \"success\": true, \"message\": \"Request with ID {id} updated successfully.\" }}";
            }
            else
            {
                return $"{{ \"success\": false, \"message\": \"No request found with ID {id}.\" }}";
            }
        }

        public async Task<string> DeleteRequestByIdAsync(string id)
        {
            var filter = Builders<BsonDocument>.Filter.Eq("_id", id);
            var result = await _requestsCollection.DeleteOneAsync(filter);

            if (result.DeletedCount > 0)
            {
                return $"{{\"success\": true, \"message\": \"Request with id {id} deleted successfully.\"}}";
            }
            else
            {
                return $"{{\"success\": false, \"message\": \"Request with id {id} not found.\"}}";

            }
        }

        public class CustomDateTimeSerializer : SerializerBase<DateTime>
        {
            // The format string for "12/13/2025 02:23:15"
            private const string DateFormat = "MM/dd/yyyy HH:mm:ss";

            public override DateTime Deserialize(BsonDeserializationContext context, BsonDeserializationArgs args)
            {
                // Read the value as a string from the BSON document
                var dateString = context.Reader.ReadString();

                // Explicitly parse the string using the known format and InvariantCulture
                return DateTime.ParseExact(
                    dateString,
                    DateFormat,
                    CultureInfo.InvariantCulture, // Use InvariantCulture for consistent parsing
                    DateTimeStyles.None
                );
            }

            public override void Serialize(BsonSerializationContext context, BsonSerializationArgs args, DateTime value)
            {
                // When writing back to MongoDB, serialize it in the exact same string format
                var dateString = value.ToString(DateFormat, CultureInfo.InvariantCulture);
                context.Writer.WriteString(dateString);
            }
        }
    }
}
