namespace PasswordManager.Models.Config
{
    public class MongoDbConfigModel
    {
        public string UserName { get; set; }
        public string Password { get; set; }
        public string HostName { get; set; }
        public string Port { get; set; }
        public string DatabaseName { get; set; }
        public string ConnectionString { get => $"mongodb://{UserName}:{Password}@{HostName}:{Port}"; }

        // mongodb://admin:secret@localhost:27017"
    }
}
