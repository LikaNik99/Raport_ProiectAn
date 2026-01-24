namespace HrWebRecruitment.Models
{
    public class MongoDbSeedModel
    {
        public List<Candidat> Candidats { get; set; }
        public List<Dictionary> Dictionary { get; set; }
        public List<Employee> Employees { get; set; }
        public List<Hiring> Hirings { get; set; }
        public List<Log> Logs { get; set; }
        public List<User> Users { get; set; }
        public List<Vacancy> Vacancies { get; set; }
    }
}
