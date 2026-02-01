namespace Core.DBModel
{
    public class EntityService
    {
        public int Id { get; set; }
        public string Name { get; set; } = null!;
        public int CategoryId { get; set; }
        public string CategoryName { get; set; } = null!;
        public string ImageFileName { get; set; } = null!;

    }
}
