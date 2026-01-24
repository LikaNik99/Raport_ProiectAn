using HrWebRecruitment.Models;
using HrWebRecruitment.Models.Config;
using Microsoft.Extensions.Options;
using Microsoft.Extensions.Primitives;
using MongoDB.Bson;
using MongoDB.Bson.Serialization;
using MongoDB.Driver;
using Newtonsoft.Json;
using System.Collections;
using System.Dynamic;
using System.Reflection;
using System.Text.Json;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace HrWebRecruitment.Services
{
    public class DbService(IMongoClient mongoClient, MongoDbConfigModel dbConfig, IConfiguration configuration, ILogger<DbService> logger)
    {
        private IMongoDatabase _database = mongoClient.GetDatabase(dbConfig.DatabaseName);
        private readonly IMongoCollection<Vacancy> _vacancyCollection;
        private readonly IMongoCollection<User> _usersCollection;
        private readonly IMongoCollection<Candidat> _candidatsCollection;
        private readonly IMongoCollection<Hiring> _hiringCollection;
        private readonly IMongoCollection<Dictionary> _dictionariesCollection;
        private readonly IMongoCollection<Employee> _employeesCollection;

        public DbService(IMongoClient mongoClient, IOptions<MongoDbConfigModel> dbConfig, IConfiguration configuration, ILogger<DbService> logger) : this(mongoClient, dbConfig.Value, configuration, logger)
        {
            Initialize().Wait();
            _vacancyCollection = _database.GetCollection<Vacancy>("Vacancies");
            _usersCollection = _database.GetCollection<User>("Users");
            _candidatsCollection = _database.GetCollection<Candidat>("Candidats");
            _hiringCollection = _database.GetCollection<Hiring>("Hirings");
            _dictionariesCollection = _database.GetCollection<Dictionary>("Dictionary");
            _employeesCollection = _database.GetCollection<Employee>("Employees");
        }

        public async Task Initialize()
        {
            var seedSection = configuration.GetSection("MongoDbSeed");
            if (!seedSection.Exists())
                return;

            foreach (var collectionSection in seedSection.GetChildren())
                CreateCollectionIfNotExists(collectionSection.Key); //Create collections if not exists

            var properties = typeof(MongoDbSeedModel).GetProperties(BindingFlags.Public | BindingFlags.Instance)
            .Where(p => p.PropertyType.IsGenericType && p.PropertyType.GetGenericTypeDefinition() == typeof(List<>));// Get all the properties of MongoDbSeedModel dynamically

            //var properties = typeof(MongoDbSeedModel).GetProperties();

            foreach (var property in properties)
            {
                try
                {
                    // Get the collection name from the property name
                    string collectionName = property.Name;

                    // Get the generic type of the list (e.g., Candidat, Employee)
                    Type listItemType = property.PropertyType.GetGenericArguments()[0];

                    // Get the data from appsettings.json for this property
                    var sectionData = seedSection.GetSection(collectionName);
                    if (!sectionData.Exists())
                    {
                        Console.WriteLine($"No data found for collection '{collectionName}' in appsettings.json.");
                        continue;
                    }

                    // Deserialize the JSON data into a List<T>
                    var listType = typeof(List<>).MakeGenericType(listItemType);
                    var data = sectionData.Get(listType);

                    if (data == null || ((IList)data).Count == 0)
                    {
                        Console.WriteLine($"No items to insert for collection '{collectionName}'.");
                        continue;
                    }

                    // Get the MongoDB collection dynamically
                    var collectionMethod = typeof(IMongoDatabase).GetMethod("GetCollection").MakeGenericMethod(listItemType);
                    var collection = collectionMethod.Invoke(_database, new object[] { collectionName, null });

                    // Insert the data using InsertManyAsync
                    var insertMethod = typeof(IMongoCollection<>)
                        .MakeGenericType(listItemType)
                        .GetMethod("InsertManyAsync", new[] { typeof(IEnumerable<>).MakeGenericType(listItemType), typeof(InsertManyOptions), typeof(CancellationToken) });

                    var insertTask = (Task)insertMethod.Invoke(collection, new object[] { data, null, CancellationToken.None });
                    await insertTask;

                    Console.WriteLine($"Inserted {((IList)data).Count} items into collection '{collectionName}'.");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Error processing collection '{property.Name}': {ex.Message}");
                }
            }
        }

        public async Task<List<Vacancy>> GetVacancies() => await _vacancyCollection.Find(new BsonDocument()).ToListAsync();

        public async Task<User> GetUser(string userName, string password)
        {
            // Create a filter to find a user with the specified userName and password
            var filter = Builders<User>.Filter.Eq(u => u.UserName, userName) &
                         Builders<User>.Filter.Eq(u => u.Password, password);

            // Use FindOneAsync to return the first matching user (or null if no match is found)
            return await _usersCollection.Find(filter).FirstOrDefaultAsync();
        }

        public async Task ApplyAsync(Candidat candidat)
        {
            await _candidatsCollection.InsertOneAsync(candidat);
        }

        public async Task<string> GetHirings()
        {
            try
            {
                // 1. Fetch all static data tables first (Dictionaries, Users, Candidats, Vacancies)
                var dictionaries = await _dictionariesCollection.Find(_ => true).ToListAsync();
                var candidats = await _candidatsCollection.Find(_ => true).ToListAsync();
                var employees = await _employeesCollection.Find(_ => true).ToListAsync();
                var vacancies = await _vacancyCollection.Find(_ => true).ToListAsync();

                // 2. Dynamically find the ID for the status we want to exclude (e.g., "Archived")
                //    NOTE: You must know the exact 'Name' or 'Id' of the status to exclude. 
                //    Assuming the status named "Archived" should be excluded.
                //    If the ID 8 is indeed the archived status, you can skip this step and use 8 directly.

                // --- Dynamic Filter (Recommended) ---
                var archivedStatusId = dictionaries.FirstOrDefault(d => d.Name == "Archived" && d.Type == "Status")?.Id;

                // 3. Fetch Hirings, filtering out the excluded status
                //    If we couldn't find the archived status ID, we default to showing all (or throw/return empty).
                //    If you use LINQ to filter, you must pull all documents first, which is inefficient:
                //    var hirings = await _hiringCollection.Find(_ => true).ToListAsync();
                //    var filteredHirings = hirings.Where(h => h.Status != archivedStatusId).ToList();

                // --- Efficient MongoDB Filter (Preferred for the initial query) ---
                // Assuming the Status field in the Hiring collection is stored as an integer (like the Dictionary Id)
                var filter = Builders<Hiring>.Filter.Ne(h => new ObjectId(h.Status), archivedStatusId);

                // Handle the case where the status was not found (or default to existing behavior if '8' is reliable)
                if (archivedStatusId == null)
                {
                    // With this corrected line:
                    filter = Builders<Hiring>.Filter.Ne(h => h.Status, "8");
                }

                var filteredHirings = await _hiringCollection.Find(filter).ToListAsync();


                // 4. Join data using LINQ
                var result = filteredHirings
                    .Select(hiring =>
                    {
                        // Ensure IDs are converted correctly for matching (e.g., Candidat ID is ObjectId or string)
                        var candidat = candidats.FirstOrDefault(c => c.Id == new ObjectId(hiring.Candidat)); // Assuming Candidat is the correct BSON type
                        var user = employees.FirstOrDefault(u => u.Id == new ObjectId(hiring.Employee));            // Assuming Users is the correct BSON type
                        var statusDict = dictionaries.FirstOrDefault(d => d.Type == "Status" && d.Name == (hiring.Status ?? "New"));
                        var vacancy = vacancies.FirstOrDefault(v => v.Title == hiring.Vacancy);

                        return new
                        {
                            HiringId = hiring.Id,
                            CandidatId = hiring.Candidat,
                            Candidat = candidat != null ? $"{candidat.FirstName} {candidat.LastName}" : "",
                            User = user != null ? $"{user.FirstName} {user.LastName}" : "admin",
                            Status = statusDict?.Name,
                            Vacancy = vacancy?.Title,
                            StatusDate = hiring.StatusDate, // Keep as DateTime for proper JSON serialization
                            Comm = string.IsNullOrEmpty(hiring.Comm) ? "required" : hiring.Comm,
                            CV = candidat?.LinkToCv
                        };
                    })
                    .OrderBy(x => x.HiringId)
                    .ToList();

                // 5. Return JSON
                return JsonConvert.SerializeObject(result);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error while fetching Hiring data. {ErrorMessage}", ex.Message);
                return "[]";
            }
        }

        public async Task<string> GetUsers()
        {
            try
            {
                var users = await _usersCollection.Find(_ => true).ToListAsync();
                var dictionaries = await _dictionariesCollection.Find(d => d.Type == "Position").ToListAsync();

                var result = users
                    .OrderBy(u => u.Id)
                    .Select(u =>
                    {
                        var role = dictionaries.FirstOrDefault(d => d.Id == new ObjectId(u.RoleId));
                        return new
                        {
                            Id = u.Id,
                            Username = u.UserName,
                            Password = u.Password,
                            FirstName = u.FirstName,
                            LastName = u.LastName,
                            Email = u.Email,
                            RoleId = role?.Name,
                            StartDate = u.StartDate,
                            EndDate = u.EndDate
                        };
                    })
                    .ToList();

                return JsonConvert.SerializeObject(result);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving users and roles");
                return null;
            }
        }

        public async Task<string> GetEmployees()
        {
            try
            {
                // Fetch all required collections
                var employees = await _employeesCollection.Find(_ => true).ToListAsync();
                var dictionaries = await _dictionariesCollection.Find(_ => true).ToListAsync();
                var hirings = await _hiringCollection.Find(_ => true).ToListAsync();
                var candidats = await _candidatsCollection.Find(_ => true).ToListAsync();

                var result = employees
                    .OrderBy(e => e.Id)
                    .Select(e =>
                    {
                        var positionDict = dictionaries.FirstOrDefault(d => d.Id == new ObjectId(e.Position));
                        var departmentDict = dictionaries.FirstOrDefault(d => d.Id == new ObjectId(e.Department));
                        var hiring = !string.IsNullOrEmpty(e.Hiring) ? hirings.FirstOrDefault(h => h.Id == new ObjectId(e.Hiring)) : null;
                        var candidat = hiring != null
                            ? candidats.FirstOrDefault(c => c.Id == new ObjectId(hiring.Candidat))
                            : null;

                        return new
                        {
                            Id = e.Id,
                            FirstName = e.FirstName,
                            LastName = e.LastName,
                            Phone = e.PhoneNumber,
                            Email = e.Email,
                            Department = departmentDict?.Name,
                            Position = positionDict?.Name,
                            Hiring = candidat != null ? $"{candidat.FirstName} {candidat.LastName}" : "",
                            StartDate = e.StartDate,
                            EndDate = e.EndDate
                        };
                    })
                    .ToList();

                return JsonConvert.SerializeObject(result);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving employees data.");
                return null;
            }
        }

        public async Task<List<Dictionary>> GetDictionary() => await _dictionariesCollection.Find(_ => true).ToListAsync();
        // Add the missing method definition for GetCandidats  
        public async Task<List<Candidat>> GetCandidats()
        {
            return await _candidatsCollection.Find(_ => true).ToListAsync();
        }


        public async Task<string> GetStatuses()
        {
            try
            {
                var dictionariesCollection = _database.GetCollection<BsonDocument>("Dictionaries");

                var filter = Builders<BsonDocument>.Filter.Eq("Type", "Status");
                var dictionaries = await dictionariesCollection.Find(filter).ToListAsync();

                var statuses = new List<string>();
                foreach (var doc in dictionaries)
                {
                    if (doc.TryGetValue("Name", out var name))
                    {
                        statuses.Add(name.AsString);
                    }
                }

                return JsonConvert.SerializeObject(statuses);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving statuses");
                return null;
            }
        }
        private void CreateCollectionIfNotExists(string collectionName)
        {
            var collectionNames = _database.ListCollectionNames().ToList();
            if (!collectionNames.Contains(collectionName))
            {
                _database.CreateCollection(collectionName);
                Console.WriteLine($"✅ Collection '{collectionName}' created.");
            }
        }
        public async Task AddUserAsync(User newUser)
        {
            if (_usersCollection == null)
            {
                throw new InvalidOperationException("Users collection is not initialized.");
            }
            await _usersCollection.InsertOneAsync(newUser);
        }

        internal async Task AddVacancyAsync(Vacancy newVacancy)
        {

            if (newVacancy == null)
                throw new ArgumentNullException(nameof(newVacancy));

            if (_vacancyCollection == null)
                throw new InvalidOperationException("Vacancy collection is not initialized.");
            newVacancy.LinkId = new Random().Next(640000);
            await _vacancyCollection.InsertOneAsync(newVacancy);
        }

        internal async Task UpdateVacancyAsync(Vacancy editVacancy)
        {
            if (editVacancy == null)
                throw new ArgumentNullException(nameof(editVacancy));

            if (_vacancyCollection == null)
                throw new InvalidOperationException("Vacancy collection is not initialized.");

            var filter = Builders<Vacancy>.Filter.Eq(v => v.Id, editVacancy.Id);
            var update = Builders<Vacancy>.Update
                .Set(v => v.Title, editVacancy.Title)
                .Set(v => v.Description, editVacancy.Description)
                .Set(v => v.StartDate, editVacancy.StartDate)
                .Set(v => v.EndDate, editVacancy.EndDate);

            await _vacancyCollection.UpdateOneAsync(filter, update);
        }

        internal async Task AddEmployeeAsync(Employee newEmployee)
        {
            if (newEmployee == null)
                throw new ArgumentNullException(nameof(newEmployee));

            if (_employeesCollection == null)
                throw new InvalidOperationException("Employees collection is not initialized.");

            await _employeesCollection.InsertOneAsync(newEmployee);
        }

        internal async Task AddOrUpdateHiringAsync(Hiring hiring)
        {
            if (hiring == null)
                throw new ArgumentNullException(nameof(hiring));

            if (_hiringCollection == null)
                throw new InvalidOperationException("Hiring collection is not initialized.");

            // Check if a hiring with the same Id exists
            var filter = Builders<Hiring>.Filter.Eq(h => h.Id, hiring.Id);
            var existingHiring = await _hiringCollection.Find(filter).FirstOrDefaultAsync();

            if (existingHiring == null)
            {
                // No hiring exists with this Id, insert new
                await _hiringCollection.InsertOneAsync(hiring);
            }
            else
            {
                // Hiring exists, update it
                var update = Builders<Hiring>.Update
                    .Set(h => h.Candidat, hiring.Candidat)
                    .Set(h => h.Employee, hiring.Employee)
                    .Set(h => h.Status, hiring.Status)
                    .Set(h => h.Vacancy, hiring.Vacancy)
                    .Set(h => h.StatusDate, hiring.StatusDate)
                    .Set(h => h.Comm, hiring.Comm);

                await _hiringCollection.UpdateOneAsync(filter, update);
            }
        }

        internal async Task UpdateEmployeeAsync(Employee editEmployee)
        {
            if (editEmployee == null)
                throw new ArgumentNullException(nameof(editEmployee));

            if (_employeesCollection == null)
                throw new InvalidOperationException("Employees collection is not initialized.");

            var filter = Builders<Employee>.Filter.Eq(e => e.Id, editEmployee.Id);
            var update = Builders<Employee>.Update
                .Set(e => e.FirstName, editEmployee.FirstName)
                .Set(e => e.LastName, editEmployee.LastName)
                .Set(e => e.PhoneNumber, editEmployee.PhoneNumber)
                .Set(e => e.Email, editEmployee.Email)
                .Set(e => e.Department, editEmployee.Department)
                .Set(e => e.Position, editEmployee.Position)
                .Set(e => e.Hiring, editEmployee.Hiring)
                .Set(e => e.StartDate, editEmployee.StartDate)
                .Set(e => e.EndDate, editEmployee.EndDate);

            await _employeesCollection.UpdateOneAsync(filter, update);
        }

        internal async Task UpdateDictionaryAsync(Dictionary editDictionary)
        {
            if (editDictionary == null)
                throw new ArgumentNullException(nameof(editDictionary));

            if (_dictionariesCollection == null)
                throw new InvalidOperationException("Dictionaries collection is not initialized.");

            var filter = Builders<Dictionary>.Filter.Eq(d => d.Id, editDictionary.Id);
            var update = Builders<Dictionary>.Update
                .Set(d => d.Name, editDictionary.Name)
                .Set(d => d.Type, editDictionary.Type)
                .Set(d => d.Description, editDictionary.Description);

            await _dictionariesCollection.UpdateOneAsync(filter, update);
        }

        internal async Task UpdateUserAsync(User editUser)
        {
            if (editUser == null)
                throw new ArgumentNullException(nameof(editUser));

            if (_usersCollection == null)
                throw new InvalidOperationException("Users collection is not initialized.");

            var filter = Builders<User>.Filter.Eq(u => u.Id, editUser.Id);
            var update = Builders<User>.Update
                .Set(u => u.UserName, editUser.UserName)
                .Set(u => u.Password, editUser.Password)
                .Set(u => u.FirstName, editUser.FirstName)
                .Set(u => u.LastName, editUser.LastName)
                .Set(u => u.Email, editUser.Email)
                .Set(u => u.RoleId, editUser.RoleId)
                .Set(u => u.StartDate, editUser.StartDate)
                .Set(u => u.EndDate, editUser.EndDate);

            await _usersCollection.UpdateOneAsync(filter, update);
        }

        internal async Task DeleteVacancyAsync(StringValues delVacancyId)
        {
            if (_vacancyCollection == null)
                throw new InvalidOperationException("Vacancy collection is not initialized.");

            foreach (var idStr in delVacancyId)
            {
                if (ObjectId.TryParse(idStr, out var objectId))
                {
                    var filter = Builders<Vacancy>.Filter.Eq(v => v.Id, objectId);
                    await _vacancyCollection.DeleteOneAsync(filter);
                }
                else
                {
                    throw new ArgumentException($"Invalid Vacancy Id: {idStr}");
                }
            }
        }

        internal async Task DeleteUserAsync(StringValues delUserId)
        {
            if (_usersCollection == null)
                throw new InvalidOperationException("Users collection is not initialized.");

            foreach (var idStr in delUserId)
            {
                if (ObjectId.TryParse(idStr, out var objectId))
                {
                    var filter = Builders<User>.Filter.Eq(u => u.Id, objectId);
                    await _usersCollection.DeleteOneAsync(filter);
                }
                else
                {
                    throw new ArgumentException($"Invalid User Id: {idStr}");
                }
            }
        }

        internal async Task DeleteEmployeeAsync(StringValues delEmployeeId)
        {
            if (_employeesCollection == null)
                throw new InvalidOperationException("Employees collection is not initialized.");

            foreach (var idStr in delEmployeeId)
            {
                if (ObjectId.TryParse(idStr, out var employeeId))
                {
                    var filter = Builders<Employee>.Filter.Eq(e => e.Id, employeeId);
                    await _employeesCollection.DeleteOneAsync(filter);
                }
                else
                {
                    throw new ArgumentException($"Invalid Employee Id: {idStr}");
                }
            }
        }

        internal async Task DeleteDictionaryAsync(StringValues delDictId)
        {
            if (_dictionariesCollection == null)
                throw new InvalidOperationException("Dictionaries collection is not initialized.");

            foreach (var idStr in delDictId)
            {
                if (ObjectId.TryParse(idStr, out var dictId))
                {
                    var filter = Builders<Dictionary>.Filter.Eq(d => d.Id, dictId);
                    await _dictionariesCollection.DeleteOneAsync(filter);
                }
                else
                {
                    throw new ArgumentException($"Invalid Dictionary Id: {idStr}");
                }
            }
        }

        internal async Task DeleteCandidatAsync(StringValues delCandidatId)
        {
            if (_candidatsCollection == null)
                throw new InvalidOperationException("Candidats collection is not initialized.");

            foreach (var idStr in delCandidatId)
            {
                if (!string.IsNullOrWhiteSpace(idStr))
                {
                    var filter = Builders<Candidat>.Filter.Eq(c => c.Id, new ObjectId(idStr));
                    await _candidatsCollection.DeleteOneAsync(filter);
                }
                else
                {
                    throw new ArgumentException($"Invalid Candidat Id: {idStr}");
                }
            }
        }

        internal async Task AddDictionaryAsync(Dictionary newDictionary)
        {
            if (newDictionary == null)
                throw new ArgumentNullException(nameof(newDictionary));

            if (_dictionariesCollection == null)
                throw new InvalidOperationException("Dictionaries collection is not initialized.");

            await _dictionariesCollection.InsertOneAsync(newDictionary);
        }

        internal async Task UpdateCandidatRecruiter(ObjectId candidatId, string recruiterId)
        {
            if (_candidatsCollection == null)
                throw new InvalidOperationException("Candidats collection is not initialized.");

            // recruiterId can be a single value or multiple values (comma separated or array)

            var filter = Builders<Candidat>.Filter.Eq(c => c.Id, candidatId);
            var update = Builders<Candidat>.Update.Set(c => c.AssignedUserId, recruiterId);

            await _candidatsCollection.UpdateOneAsync(filter, update);
        }

        internal async Task<List<Hiring>>? GetRawHirings()
        {
            try
            {
                var hirings = await _hiringCollection.Find(_ => true).ToListAsync();
                return hirings;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving raw hirings data.");
                return null;
            }
        }

        internal async Task<List<Employee>>? GetRawEmployees()
        {
            try
            {
                var employees = await _employeesCollection.Find(_ => true).ToListAsync();
                return employees;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving raw hirings data.");
                return null;
            }
        }

        internal async Task<List<User>>? GetRawUsers()
        {
            try
            {
                var users = await _usersCollection.Find(_ => true).ToListAsync();
                return users;
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Error retrieving raw hirings data.");
                return null;
            }
        }
    }
}
