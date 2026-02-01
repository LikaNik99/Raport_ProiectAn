using Microsoft.Extensions.Configuration;

namespace Core.Utils
{
    public class ConfigurationHelper
    {
        public static string? GetConnectionString(string name)
        {
            var configuration = new ConfigurationBuilder()
                .SetBasePath(Directory.GetCurrentDirectory())
                .AddJsonFile("appsettings.json")
                .AddEnvironmentVariables()
                .Build();

            return configuration.GetConnectionString(name);
        }
    }

}